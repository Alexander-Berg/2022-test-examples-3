package ru.yandex.direct.yav.client

import okhttp3.mockwebserver.MockResponse
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.nio.charset.StandardCharsets.UTF_8

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class YavClientTest {
    @JvmField
    @RegisterExtension
    var mockedYav: MockedYav = MockedYav()

    lateinit var yavClient: YavClient

    @BeforeAll
    fun init() {
        yavClient = mockedYav.createClient()

        val responseBody = readResource(SAMPLE_VERSIONS_RESPONSE_PATH)
        val response = MockResponse().addHeader("Content-Type", "application/json")
            .setBody(responseBody)

        mockedYav.add("GET:/versions/$SAMPLE_SECRET_UUID", response)
    }

    @Test
    fun `test sample response`() {
        val secretValues = yavClient.getSecretValues(SAMPLE_SECRET_UUID)

        val expectedSecrets = mapOf(
            "testfile.txt" to "sdasdsa\n",
            "vfsdasd" to "545",
            "sanya_key" to "123123",
            "asdasd" to "123",
            "asdwww" to "6665",
            "key_3" to "value 3",
            "asd" to "3333"
        )

        Assertions.assertEquals(expectedSecrets, secretValues)
    }

    private fun readResource(path: String) = IOUtils.toString(YavClientTest::class.java.getResourceAsStream(path), UTF_8)

    private companion object {
        const val TVM_TICKET_BODY = "ticketBody"
        const val SAMPLE_VERSIONS_RESPONSE_PATH = "sample_versions_response.json"
        const val SAMPLE_SECRET_UUID = "sec-01f33ak4h4dec51e399nq43jq6"
    }
}
