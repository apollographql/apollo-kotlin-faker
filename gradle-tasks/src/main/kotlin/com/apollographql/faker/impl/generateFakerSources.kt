package com.apollographql.faker.impl

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.ast.ForeignSchema
import com.apollographql.apollo.ast.GQLDefinition
import com.apollographql.apollo.ast.GQLDirectiveDefinition
import com.apollographql.apollo.ast.GQLDocument
import com.apollographql.apollo.ast.GQLNamed
import com.apollographql.apollo.ast.GQLScalarTypeDefinition
import com.apollographql.apollo.ast.GQLSchemaExtension
import com.apollographql.apollo.ast.builtinForeignSchemas
import com.apollographql.apollo.ast.internal.SchemaValidationOptions
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.apollographql.apollo.ast.toUtf8
import com.apollographql.apollo.ast.validateAsSchema
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.withIndent
import gratatouille.GInputFiles
import gratatouille.GOutputDirectory
import gratatouille.GTask
import gratatouille.capitalizeFirstLetter
import okio.buffer
import okio.sink
import java.util.Locale

private object Unused

@OptIn(ApolloExperimental::class)
@GTask()
fun generateFakerSources(
  inputSchemaFiles: GInputFiles,
  packageName: String,
  serviceName: String,
  outputKotlinDir: GOutputDirectory,
  outputResourcesDir: GOutputDirectory
) {
  val definitions = mutableListOf<GQLDefinition>()
  /*
   * We do not merge the schema extensions because it confuses the apollo schema validation
   */
  val schemaExtensions = mutableListOf<GQLSchemaExtension>()
  inputSchemaFiles.flatMap { it.file.parseAsGQLDocument().getOrThrow().definitions }.toMutableList().forEach {
    when (it) {
      is GQLSchemaExtension -> schemaExtensions.add(it)
      else -> definitions.add(it)
    }
  }

  val mergedDefinitions = ExtensionsMerger(definitions, MergeOptions(true, false)).merge().getOrThrow()
  val mergedDocument = GQLDocument(sourceLocation = null, definitions = mergedDefinitions + schemaExtensions)

  val classLoader = Unused::class.java.classLoader
  val fakesDefinitions = classLoader.getResourceAsStream("fakes.graphqls")!!
    .bufferedReader()
    .readText()
    .parseAsGQLDocument()
    .getOrThrow()
    .definitions

  val schema = mergedDocument
    .validateAsSchema(
      SchemaValidationOptions(
        addKotlinLabsDefinitions = false,
        builtinForeignSchemas() + ForeignSchema("fakes", "v0.0", fakesDefinitions)
      )
    ).getOrThrow()

  /*
   * remove the builtin definitions because ExecutableSchema will add its own
   */
  val filteredDefinitions = schema.toGQLDocument().definitions.filter {
    if (it !is GQLNamed) {
      return@filter true
    }

    if (it.name.startsWith("__")) {
      return@filter false
    }

    if (it is GQLDirectiveDefinition && it.name in setOf("skip", "include", "deprecated", "specifiedBy", "defer")) {
      /*
       * There is an interesting question what to do with @oneOf...
       */
      return@filter false
    }
    true
  }
  outputResourcesDir.apply {
    deleteRecursively()
    mkdirs()
    resolve("${serviceName}Schema.graphqls").sink().buffer().use {
      GQLDocument(sourceLocation = null, definitions = filteredDefinitions).toUtf8(it)
    }
  }

  outputKotlinDir.resolve(packageName.replace('.', '/')).apply {
    deleteRecursively()
    mkdirs()
    executableSchemaFileSpec(
      packageName,
      serviceName,
      filteredDefinitions.filterIsInstance<GQLScalarTypeDefinition>()
    ).writeTo(this)
    resolve(packageName.replace('.', '/')).resolve("JsonCoercing.kt").writeText(jsonCoercing(packageName))
  }
}

private fun jsonCoercing(packageName: String): String {
  return """
    package $packageName
    
    import com.apollographql.apollo.api.json.ApolloJsonElement
    import com.apollographql.apollo.ast.*
    import com.apollographql.apollo.execution.Coercing

    internal object JsonCoercing: Coercing<Any?> {
      override fun serialize(internalValue: Any?): ApolloJsonElement {
        return internalValue
      }
    
      override fun deserialize(value: ApolloJsonElement): Any? {
        return value
      }
    
      override fun parseLiteral(value: GQLValue): Any? {
        return value.toAny()
      }
    
      private fun GQLValue.toAny(): Any? {
        return when(this) {
          is GQLBooleanValue -> value
          is GQLEnumValue -> value
          is GQLFloatValue -> value.toDouble()
          is GQLIntValue -> value.toInt()
          is GQLListValue -> values.map { it.toAny() }
          is GQLNullValue -> null
          is GQLObjectValue -> fields.associate { it.name to it.value.toAny() }
          is GQLStringValue -> value
          is GQLVariableValue -> TODO()
        }
      }
    }
  """.trimIndent()
}

private fun executableSchemaFileSpec(
  packageName: String,
  serviceName: String,
  scalarDefinitions: List<GQLScalarTypeDefinition>
): FileSpec {
  val holderName = "${serviceName.capitalizeFirstLetter()}ClassloaderHolder"
  val schemaFileName = "${serviceName}Schema.graphqls"
  return FileSpec.builder(ClassName(packageName, "${serviceName}ExecutableSchema"))
    .addType(
      TypeSpec.objectBuilder(holderName)
        .addModifiers(KModifier.PRIVATE)
        .build()
    )
    .addFunction(
      FunSpec.builder("${serviceName}ExecutableSchema")
        .returns(ClassName("com.apollographql.apollo.execution", "ExecutableSchema"))
        .addModifiers(KModifier.INTERNAL)
        .addCode(
          buildCodeBlock {
            add("val resourceName = %S\n", schemaFileName)
            add("val resource = $holderName::class.java.classLoader.getResourceAsStream(resourceName)\n")
            add("check(resource != null) {\n")
            withIndent {
              add("%S", "Apollo Faker: cannot find resource '${'$'}resourceName'.\n")
            }
            add("}\n")
            add("\n")
            add("return %T()\n", ClassName("com.apollographql.apollo.execution", "ExecutableSchema", "Builder"))
            withIndent {
              add(".schema(resource.bufferedReader().readText())\n")
              add(".resolver(%T())\n", ClassName("com.apollographql.faker.datafaker", "DataFakerResolver"))
              add(".typeResolver {obj, _ ->\n")
              withIndent {
                add("check (obj is Map<*,*>)\n")
                add("obj.get(\"__typename\").toString()\n")
              }
              add("}\n")
              scalarDefinitions.forEach {
                add(".addCoercing(%S, JsonCoercing)\n", it.name)
              }
              add(".build()\n")
            }
          }
        )
        .build()
    )
    .build()
}

private fun String.myCapitalize() =
  replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }