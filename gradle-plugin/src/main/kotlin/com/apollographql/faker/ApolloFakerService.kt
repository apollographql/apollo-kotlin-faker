package com.apollographql.faker

import com.apollographql.faker.impl.registerGenerateFakerSourcesTask
import gratatouille.capitalizeFirstLetter
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class ApolloFakerService(private val project: Project, private val serviceName: String) {
  val packageName: Property<String> = project.objects.property(String::class.java)
  val schemaFiles: ConfigurableFileCollection = project.files()

  internal fun registerTasks() {
    val task = project.registerGenerateFakerSourcesTask(
      taskName = "generate${serviceName.capitalizeFirstLetter()}FakerSources",
      inputSchemaFiles = schemaFiles,
      packageName = packageName,
      serviceName = project.provider { serviceName },
    )

    val kotlinExtension = project.extensions.getByName("kotlin") as? KotlinJvmProjectExtension

    check(kotlinExtension != null) {
      "Apollo Faker requires the org.jetbrains.kotlin.jvm plugin"
    }
    kotlinExtension.sourceSets.getByName("main").apply {
      kotlin.srcDir(task.flatMap { it.outputKotlinDir })
      resources.srcDir(task.flatMap { it.outputResourcesDir })
    }
  }
}
