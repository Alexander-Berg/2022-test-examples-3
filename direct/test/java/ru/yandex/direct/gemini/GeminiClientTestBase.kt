package ru.yandex.direct.gemini

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.JUnitSoftAssertions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.asynchttp.ParallelFetcherFactory

@RunWith(SpringJUnit4ClassRunner::class)
@ContextConfiguration(classes = [TestingConfiguration::class])
abstract class GeminiClientTestBase {
    private val logger: Logger = LoggerFactory.getLogger(GeminiClientTestBase::class.java)

    companion object {
        const val USER = "user"
    }

    private lateinit var mockWebServer: MockWebServer

    @get:Rule
    val softAssertions = JUnitSoftAssertions()

    @Autowired
    private lateinit var parallelFetcherFactory: ParallelFetcherFactory

    protected lateinit var geminiClient: GeminiClient

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.setDispatcher(dispatcher())
        mockWebServer.start()

        geminiClient = GeminiClient(url(), USER, parallelFetcherFactory)
    }

    protected abstract fun dispatcher(): Dispatcher

    protected open fun url(): String {
        return "http://${mockWebServer.hostName}:${mockWebServer.port}"
    }

    @After
    fun tearDown() {
        try {
            mockWebServer.shutdown()
        } catch (e: Exception) {
            logger.warn("cannot shut down mockWebServer", e)
        }
    }
}
