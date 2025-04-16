package com.apollographql.faker

import gratatouille.GExtension
import org.gradle.api.Action
import org.gradle.api.Project

@GExtension(pluginId = "com.apollographql.faker")
abstract class ApolloFakerExtension(private val project: Project) {
  private val services = mutableSetOf<String>()
  fun service(name: String, action: Action<ApolloFakerService>) {
    check(!services.contains(name)) {
      "Apollo Faker: service '$name' is already created."
    }

    services.add(name)
    ApolloFakerService(project, name)
      .also(action::execute)
      .registerTasks()
  }
}