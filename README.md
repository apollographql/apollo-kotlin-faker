<div align="center">

<p>
	<a href="https://www.apollographql.com/"><img src="https://raw.githubusercontent.com/apollographql/apollo-client-devtools/a7147d7db5e29b28224821bf238ba8e3a2fdf904/assets/apollo-wordmark.svg" height="100" alt="Apollo Client"></a>
</p>

[![Discourse](https://img.shields.io/discourse/topics?label=Discourse&server=https%3A%2F%2Fcommunity.apollographql.com&logo=discourse&color=467B95&style=flat-square)](http://community.apollographql.com/new-topic?category=Help&tags=mobile,client)
[![Slack](https://img.shields.io/static/v1?label=kotlinlang&message=apollo-kotlin&color=A97BFF&logo=slack&style=flat-square)](https://app.slack.com/client/T09229ZC6/C01A6KM1SBZ)

[![Maven Central](https://img.shields.io/maven-central/v/com.apollographql.faker/resolver-datafaker?style=flat-square)](https://central.sonatype.com/namespace/com.apollographql.faker)
[![OSS Snapshots](https://img.shields.io/nexus/s/com.apollographql.faker/resolver-datafaker?server=https%3A%2F%2Fs01.oss.sonatype.org&label=oss-snapshots&style=flat-square)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/apollographql/faker/)

</div>

## 🚀 Apollo Kotlin Faker

Apollo Kotlin Faker makes it easy to fake a GraphQL server during the early phases of development or while testing. 

Apollo Faker is strongly inspired by [graphql-faker](https://github.com/graphql-kit/graphql-faker).

## 🌈 Getting started

Add the `com.apollographql.faker` plugin to your build scripts:

```kotlin
plugins {
  id("org.jetbrains.kotlin.jvm").version("$kotlinVersion")
  id("com.apollographql.faker").version("0.0.0")
}

apolloFaker {
  service("service") {
    schemaFiles.from("src/main/graphql/schema.graphqls")
    packageName.set("com.example")
  }
}
```

The plugin creates a `${service}ExecutableSchemaBuilder()` factory function that returns an `ExecutableSchema.Builder` instance that you can configure with a `DataFakerResolver`:

```kotlin
val executableSchema = serviceExecutableSchemaBuilder()
  .resolver(DataFakerResolver())
  .build()

val response = executableSchema.execute()
```



## ⚙️ Custom scalars

Because the coercing of custom scalars is implementation specific, 

## ✅ Requirements


