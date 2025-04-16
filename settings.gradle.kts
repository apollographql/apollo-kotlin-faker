pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.mavenCentral()
    it.maven("https://storage.googleapis.com/gradleup/m2")
    it.maven("https://storage.googleapis.com/apollo-previews/m2/")
  }
}

include(
  "gradle-plugin",
  "gradle-tasks",
  "resolver-datafaker",
)

