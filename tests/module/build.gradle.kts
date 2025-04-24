plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.faker")
}

apolloFaker {
  service("service") {
    packageName.set("com.example")
    schemaFiles.from(files("src/main/graphql"))
  }
}

dependencies {
  implementation("com.apollographql.faker:resolver-datafaker")
  implementation(libs.apollo.execution)
  implementation(libs.truth)
  testImplementation(libs.kotlin.test)
}