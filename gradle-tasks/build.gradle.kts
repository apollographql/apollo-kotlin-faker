import com.gradleup.librarian.gradle.Librarian

plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.google.devtools.ksp")
  id("com.gradleup.gratatouille")
}

Librarian.module(project)

gratatouille {
  codeGeneration {
    classLoaderIsolation()
  }
}

dependencies {
  implementation(libs.apollo.ast)
  implementation(libs.kotlinpoet)
  testImplementation(libs.kotlin.test)
}


