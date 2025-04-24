package com.apollographql.faker.datafaker

import com.apollographql.apollo.ast.*
import com.apollographql.apollo.execution.ResolveInfo
import com.apollographql.apollo.execution.Resolver
import net.datafaker.Faker
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import kotlin.random.Random
import java.util.Random as JavaRandom

private val fakeList = "fakeList"

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
        FakeType.ZIP_CODE -> faker.address().zipCode()
        FakeType.CITY -> faker.address().city()
        FakeType.STREET_NAME -> faker.address().streetName()
        FakeType.STREET_ADDRESS -> faker.address().streetAddress()
        FakeType.SECONDARY_ADDRESS -> faker.address().secondaryAddress()
        FakeType.COUNTRY -> faker.address().country()
        FakeType.COUNTRY_CODE -> faker.address().countryCode()
        FakeType.STATE -> faker.address().state()
        FakeType.STATE_ABBR -> faker.address().stateAbbr()
        FakeType.LATITUDE -> faker.address().latitude()
        FakeType.LONGITUDE -> faker.address().longitude()
        FakeType.COLOR_NAME -> faker.color().name()
        FakeType.PRODUCT_NAME -> faker.commerce().productName()
        FakeType.MONEY -> faker.commerce().price()
        FakeType.PRODUCT_MATERIAL -> faker.commerce().material()
        FakeType.COMPANY_NAME -> faker.company().name()
        FakeType.COMPANY_CATCH_PHRASE -> faker.company().catchPhrase()
        FakeType.COMPANY_BS -> faker.company().bs()
        FakeType.DATE -> faker.timeAndDate().between(
          LocalDate.of(2000, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
          LocalDate.of(2030, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
        ).toString()
        FakeType.PAST_DATE -> faker.timeAndDate().past().toString()
        FakeType.FUTURE_DATE -> faker.timeAndDate().future().toString()
        FakeType.CURRENCY_CODE -> faker.money().currencyCode()
        FakeType.CURRENCY_NAME -> faker.money().currency()
        FakeType.CURRENCY_SYMBOL -> faker.money().currencySymbol()
        FakeType.INTERNATIONAL_BANK_ACCOUNT_NUMBER -> faker.finance().iban()
        FakeType.BANK_IDENTIFIER_CODE -> faker.finance().bic()
        FakeType.HACKER_ABBREVIATION -> faker.hacker().abbreviation()
        FakeType.IMAGE_URL -> faker.internet().image()
        FakeType.AVATAR_URL -> faker.avatar().image()
        FakeType.EMAIL -> faker.internet().emailAddress()
        FakeType.URL -> faker.internet().url()
        FakeType.DOMAIN_NAME -> faker.internet().domainName()
        FakeType.IPV4_ADDRESS -> faker.internet().ipV4Address()
        FakeType.IPV6_ADDRESS -> faker.internet().ipV6Address()
        FakeType.USER_AGENT -> faker.internet().userAgent()
        FakeType.COLOR_HEX -> faker.color().hex()
        FakeType.MAC_ADDRESS -> faker.internet().macAddress()
        FakeType.PASSWORD -> faker.internet().password()
        FakeType.LOREM -> faker.lorem().sentence()
        FakeType.FIRST_NAME -> faker.name().firstName()
        FakeType.LAST_NAME -> faker.name().lastName()
        FakeType.FULL_NAME -> faker.name().fullName()
        FakeType.JOB_TITLE -> faker.job().title()
        FakeType.PHONE_NUMBER -> faker.phoneNumber().phoneNumber()
        FakeType.NUMBER -> random.nextInt()
        FakeType.UUID -> faker.internet().uuid()
        FakeType.WORD -> faker.lorem().word()
        FakeType.WORDS -> faker.lorem().words().joinToString(",")
        FakeType.LOCALE -> faker.locality().localeString()
        FakeType.FILENAME -> faker.file().fileName()
        FakeType.MIME_TYPE -> faker.file().mimeType()
        FakeType.FILE_EXTENSION -> faker.file().extension()
        FakeType.SEMVER -> faker.app().version()
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
  return directives.filter { schema.originalDirectiveName(it.name) == "fakeList" }
    .map {
      FakeSize(
        level = it.getIntArgument("level") ?: 0,
        min = it.getIntArgument("minSize")!!,
        max = it.getIntArgument("maxSize")!! + 1
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
    .firstOrNull { schema.originalDirectiveName(it.name) == "fakeExamples" }
    ?.arguments
    ?.firstOrNull { it.name == "values" }
    ?.value
    ?.let {
      it as GQLListValue
      it.values.map { it.coerceToKotlin() }
    }.orEmpty()
}