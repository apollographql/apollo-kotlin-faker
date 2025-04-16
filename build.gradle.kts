import com.gradleup.librarian.gradle.Librarian

plugins {
    id("base")
}
buildscript {
    dependencies {
        classpath(libs.kgp)
        classpath(libs.ksp)
        classpath(libs.apollo.ast)
        classpath(libs.gratatouille.gradle.plugin)
        classpath(libs.librarian.gradle.plugin)
    }
}

Librarian.root(project)
