package ru.yandex.market.logistics.mqm.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate
import ru.yandex.market.logistics.mqm.client.configuration.TestClientConfiguration

@ExtendWith(
    SpringExtension::class,
    SoftAssertionsExtension::class,
)
@SpringBootTest(
    classes = [
        TestClientConfiguration::class
    ],
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@TestPropertySource("classpath:integration-test.properties")
abstract class AbstractClientTest {

    @Value("\${mqm.api.url}")
    lateinit var uri: String

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var mqmClient: MqmClient

    @Autowired
    lateinit var clientRestTemplate: RestTemplate

    lateinit var mockServer: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        mockServer = MockRestServiceServer.createServer(clientRestTemplate)
    }
}
