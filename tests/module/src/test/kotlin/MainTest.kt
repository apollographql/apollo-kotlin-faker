import com.apollographql.apollo.api.json.buildJsonString
import com.apollographql.apollo.api.json.writeAny
import com.apollographql.apollo.execution.toGraphQLRequest
import com.example.serviceExecutableSchema
import com.google.common.truth.ExpectFailure.assertThat
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MainTest {
  @Test
  fun test(): Unit = runBlocking {
    serviceExecutableSchema()
      .execute(
      """
        {
          date
          integer
          list
          user {
            name
            address
            avatarUrl
            bio 
            date 
            json
          }
          date
          json
          direction
          nodes {
            __typename
            id
            ... on Product {
              name
            }
            ... on Review {
              text
            }
          }
        }
      """.trimIndent()
        .toGraphQLRequest()
      ).apply {
        assertNull(errors)
        val jsonData = buildJsonString("  ") {
          writeAny(data)
        }
        // Uncomment to update the data
        // File("test-data/data.json").parentFile.mkdirs()
        // File("test-data/data.json").writeText(jsonData)
        println(jsonData)
        Truth.assertThat(File("test-data/data.json").readText()).isEqualTo(jsonData)
        assertEquals(File("test-data/data.json").readText(), jsonData)
      }
  }
}