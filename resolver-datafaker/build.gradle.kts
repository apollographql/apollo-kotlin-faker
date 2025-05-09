import com.apollographql.apollo.ast.GQLEnumTypeDefinition
import com.apollographql.apollo.ast.parseAsGQLDocument
import com.gradleup.librarian.gradle.Librarian

plugins {
  id("org.jetbrains.kotlin.jvm")
}

Librarian.module(project)

dependencies {
  api(libs.apollo.execution)
  implementation(libs.datafaker)
  testImplementation(libs.kotlin.test)
}

val codegen = tasks.register("generateTypes", GenerateTypes::class.java) {
  inputSchema.set(file("../gradle-tasks/src/main/resources/fakes.graphqls"))
  outputDirectory.set(layout.buildDirectory.dir("generated/graphql_types"))
}

kotlin.sourceSets.main.get().kotlin.srcDir(codegen)

@CacheableTask
abstract class GenerateTypes: DefaultTask() {
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val inputSchema: RegularFileProperty

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @TaskAction
  fun taskAction() {
    val enumDefinition = inputSchema.get().asFile.parseAsGQLDocument()
      .getOrThrow()
      .definitions
      .filterIsInstance<GQLEnumTypeDefinition>()
      .firstOrNull {
        it.name == "FakeType"
      } ?: error("Cannot find 'FakeType' enum in the fake schema")

    val source = buildString {
      appendLine("""
        /**
         * This file is autogenerated, do not edit manually
         */
        package com.apollographql.faker.datafaker

        internal enum class FakeType {
      """.trimIndent())
      enumDefinition.enumValues.forEach {
        appendLine("    ${it.name},")
      }
      appendLine("}")
    }

    outputDirectory.get().asFile.resolve("com/apollographql/faker/datafaker").apply {
      mkdirs()
      resolve("generated.kt").writeText(source)
    }
  }
}