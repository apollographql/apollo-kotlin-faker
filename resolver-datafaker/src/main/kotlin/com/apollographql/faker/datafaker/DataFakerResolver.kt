package com.apollographql.faker.datafaker

import com.apollographql.apollo.ast.*
import com.apollographql.apollo.execution.ResolveInfo
import com.apollographql.apollo.execution.Resolver
import net.datafaker.Faker
import java.util.*
import kotlin.random.Random
import java.util.Random as JavaRandom

class DataFakerResolver(private val locale: Locale = Locale.ROOT) : Resolver {
  override suspend fun resolve(resolveInfo: ResolveInfo): Any? {

    val fieldDefinition = resolveInfo.fieldDefinition()
    var examples: List<Any?> = fieldDefinition.findExamples(resolveInfo.schema)
    var fakeType: FakeType? = null
    if (examples.isEmpty()) {
      fakeType = fieldDefinition.findFakeType(resolveInfo.schema)
      if (fakeType == null) {
        val gqlType = fieldDefinition.type.rawType().let { resolveInfo.schema.typeDefinition(it.name) }
        if (gqlType is GQLScalarTypeDefinition) {
          examples = gqlType.findExamples(resolveInfo.schema)
          if (examples.isEmpty()) {
            fakeType = gqlType.findFakeType(resolveInfo.schema)
          }
        }
      }
    }
    val fakeSizes: List<FakeSize> = fieldDefinition.findFakeSizes(resolveInfo.schema)
    return resolveFake(resolveInfo, fieldDefinition.type, examples, fakeType, fakeSizes, 0, 0)
  }

  private fun resolveFake(
    resolveInfo: ResolveInfo,
    type: GQLType,
    examples: List<Any?>,
    fakeType: FakeType?,
    fakeSizes: List<FakeSize>,
    depth: Int,
    index: Int
  ): Any? {
    val actualType = if (type is GQLNonNullType) {
      type.type
    } else {
      type
    }

    val seed = (resolveInfo.path + index).hashCode().toLong()
    val random = Random(seed)
    if (actualType is GQLListType) {
      val fakeSize = fakeSizes.firstOrNull { it.level == depth } ?: FakeSize(0, 1, 5)
      val size = random.nextInt(from = fakeSize.min, until = fakeSize.max)

      return 0.until(size).map { resolveFake(resolveInfo, actualType.type, examples, fakeType, fakeSizes, depth + 1, it) }
    }

    if (examples.isNotEmpty()) {
      return examples.get(random.nextInt(examples.size))
    }

    check(actualType is GQLNamedType)

    if (fakeType != null) {
      val faker = Faker(locale, JavaRandom(seed))
      return when (fakeType) {
        FakeType.zipCode -> faker.address().zipCode()
        FakeType.city -> faker.address().city()
        FakeType.streetName -> faker.address().streetName()
        FakeType.streetAddress -> faker.address().streetAddress()
        FakeType.secondaryAddress -> faker.address().secondaryAddress()
        FakeType.country -> faker.address().country()
        FakeType.countryCode -> faker.address().countryCode()
        FakeType.state -> faker.address().state()
        FakeType.stateAbbr -> faker.address().stateAbbr()
        FakeType.latitude -> faker.address().latitude()
        FakeType.longitude -> faker.address().longitude()
        FakeType.colorName -> faker.color().name()
        FakeType.productName -> faker.commerce().productName()
        FakeType.money -> faker.commerce().price()
        FakeType.productMaterial -> faker.commerce().material()
        FakeType.companyName -> faker.company().name()
        FakeType.companyCatchPhrase -> faker.company().catchPhrase()
        FakeType.companyBS -> faker.company().bs()
        FakeType.date -> faker.timeAndDate().between(
          GregorianCalendar(2000, 1, 1).toInstant(),
          GregorianCalendar(2023, 1, 1).toInstant()
        ).toString()
        FakeType.pastDate -> faker.timeAndDate().past().toString()
        FakeType.futureDate -> faker.timeAndDate().future().toString()
        FakeType.currencyCode -> faker.money().currencyCode()
        FakeType.currencyName -> faker.money().currency()
        FakeType.currencySymbol -> faker.money().currencySymbol()
        FakeType.internationalBankAccountNumber -> faker.finance().iban()
        FakeType.bankIdentifierCode -> faker.finance().bic()
        FakeType.hackerAbbreviation -> faker.hacker().abbreviation()
        FakeType.imageUrl -> faker.internet().image()
        FakeType.avatarUrl -> faker.avatar().image()
        FakeType.email -> faker.internet().emailAddress()
        FakeType.url -> faker.internet().url()
        FakeType.domainName -> faker.internet().domainName()
        FakeType.ipv4Address -> faker.internet().ipV4Address()
        FakeType.ipv6Address -> faker.internet().ipV6Address()
        FakeType.userAgent -> faker.internet().userAgent()
        FakeType.colorHex -> faker.color().hex()
        FakeType.macAddress -> faker.internet().macAddress()
        FakeType.password -> faker.internet().password()
        FakeType.lorem -> faker.lorem().sentence()
        FakeType.firstName -> faker.name().firstName()
        FakeType.lastName -> faker.name().lastName()
        FakeType.fullName -> faker.name().fullName()
        FakeType.jobTitle -> faker.job().title()
        FakeType.phoneNumber -> faker.phoneNumber().phoneNumber()
        FakeType.number -> random.nextInt()
        FakeType.uuid -> faker.internet().uuid()
        FakeType.word -> faker.lorem().word()
        FakeType.words -> faker.lorem().words().joinToString(",")
        FakeType.locale -> faker.locality().localeString()
        FakeType.filename -> faker.file().fileName()
        FakeType.mimeType -> faker.file().mimeType()
        FakeType.fileExtension -> faker.file().extension()
        FakeType.semver -> faker.app().version()
      }
    }

    val typeDefinition = resolveInfo.schema.typeDefinition(actualType.name)
    return when (typeDefinition) {
      is GQLScalarTypeDefinition -> {
        when (actualType.name) {
          "Int" -> random.nextInt(100)
          "Float" -> random.nextDouble(100.0)
          "Boolean" -> random.nextBoolean()
          "String" -> {
            Faker(locale, JavaRandom(resolveInfo.path.hashCode().toLong())).lorem().word()
          }

          "ID" -> random.nextLong().toString()
          else -> {
            error("Apollo: please use use @examples or @fake on the '${actualType.name}' custom scalar.")
          }
        }
      }

      is GQLEnumTypeDefinition -> {
        typeDefinition.enumValues.get(random.nextInt(typeDefinition.enumValues.size)).name
      }

      is GQLInterfaceTypeDefinition,
      is GQLUnionTypeDefinition -> {
        val possibleTypes = resolveInfo.schema.possibleTypes(typeDefinition.name).toList()
        mapOf("__typename" to possibleTypes.get(random.nextInt(possibleTypes.size)))
      }

      is GQLObjectTypeDefinition -> {
        mapOf("__typename" to typeDefinition.name)
      }
      is GQLInputObjectTypeDefinition -> error("Input object in output position")
    }
  }
}


private fun GQLHasDirectives.findFakeType(schema: Schema): FakeType? {
  return directives.firstOrNull { schema.originalDirectiveName(it.name) == "fake" }
    ?.arguments
    ?.first { it.name == "type" }
    ?.value
    ?.let {
      it as GQLEnumValue
      FakeType.valueOf(it.value)
    }
}

private class FakeSize(val level: Int, val min: Int, val max: Int)

private fun GQLDirective.getIntArgument(name: String): Int? {
  return arguments.firstOrNull { it.name == name }
    ?.value
    ?.let {
      it as GQLIntValue
      it.value.toInt()
    }
}

private fun GQLHasDirectives.findFakeSizes(schema: Schema): List<FakeSize> {
  return directives.filter { schema.originalDirectiveName(it.name) == "listSize" }
    .map {
      FakeSize(
        level = it.getIntArgument("level") ?: 0,
        min = it.getIntArgument("min")!!,
        max = it.getIntArgument("max")!! + 1
      )
    }
}

private fun GQLValue.coerceToKotlin(): Any? {
  return when (this) {
    is GQLEnumValue -> value
    is GQLListValue -> values.map { it.coerceToKotlin() }
    is GQLStringValue -> value
    is GQLIntValue -> value.toInt()
    is GQLFloatValue -> value.toFloat()
    is GQLBooleanValue -> value
    is GQLObjectValue -> fields.associate { it.name to it.value.coerceToKotlin() }
    is GQLNullValue -> null
    else -> error("Unsupported value: $this")
  }
}

private fun GQLHasDirectives.findExamples(schema: Schema): List<Any?> {
  return directives
    .firstOrNull { schema.originalDirectiveName(it.name) == "examples" }
    ?.arguments
    ?.firstOrNull { it.name == "values" }
    ?.value
    ?.let {
      it as GQLListValue
      it.values.map { it.coerceToKotlin() }
    }.orEmpty()
}