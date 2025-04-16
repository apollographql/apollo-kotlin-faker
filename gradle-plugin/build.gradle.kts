import com.gradleup.librarian.gradle.Librarian

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.google.devtools.ksp")
  id("com.gradleup.gratatouille")
}

Librarian.module(project)

gratatouille {
  pluginMarker("com.apollographql.faker", "default")
  codeGeneration()
}

dependencies {
  compileOnly(libs.gradle.api.min)
  compileOnly(libs.kgp.min)
  gratatouille(project(":gradle-tasks"))
  testImplementation(libs.kotlin.test)
}


