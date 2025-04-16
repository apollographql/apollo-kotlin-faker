plugins {
  id("base")
}
buildscript {
  dependencies {
    classpath(libs.kgp)
    classpath("com.apollographql.faker:gradle-plugin")
  }
}
