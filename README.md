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

Apollo Faker is strongly inspired by [graphql-faker](https://github.com/graphql-kit/graphql-faker) and uses similar directives:



## 🌈 Getting started

Put your schema in `src/main/graphql/schema.graphql` and add an `extra.graphqls` file next to it.

Define how to generate fake data using [fakes directives](https://specs.apollo.dev/fakes/v0.0/):  

```graphql
extend schema @link(url: "https://specs.apollo.dev/fakes/v0.0/", import: ["@fakeExamples", "@fakeList", "@fake", "FakeType", "FakeValue"])


extend type User {
  name: String @fake(type: FIRST_NAME)
  birthdate: Date @fake(type: PAST_DATE) 
  avatar: String! @fake(type: AVATAR_URL)  
  company: String @fakeExamples(values: ["Hooli", "Pied Piper"])
  pets: [Pet] @fakeList(minSize: 1, maxSize: 10)
}
```

Add the `com.apollographql.faker` plugin to your build scripts:

```kotlin
plugins {
  id("org.jetbrains.kotlin.jvm").version("$kotlinVersion")
  id("com.apollographql.faker").version("0.0.1")
}

apolloFaker {
  service("service") {
    schemaFiles.from(files("src/main/graphql/"))
    packageName.set("com.example")
  }
}
```

The plugin creates a `${service}ExecutableSchema()` factory function that returns an `ExecutableSchema` instance that you can run your queries against:

```kotlin
val executableSchema = serviceExecutableSchemaBuilder()
val response = executableSchema.execute(
  """
  {
    user {
      name
      birthdate
      avatar
      company
      pets {
        name
      }
    }
  }
  """.toGraphQLRequest())

println(response.data)
```

Response:

```json
{
  "data": {
    "user": {
      "name": "Rodger Ullrich",
      "birthdate": "2000-08-16T14:33:44.400Z",
      "avatar": "https://robohash.org/rnbtefod.png",
      "company": "Pier Piper",
      "pets": [
        { "name": "Russel" },
        { "name": "Bosco" },
        { "name": "Zulma" },
        { "name": "Rocky" }
      ]
    }
  }
}
```
## ✅ Requirements

Apollo Faker is JVM only for now.

* Java 11
* Kotlin Gradle Plugin 1.9.0+
* Gradle 8.0+


