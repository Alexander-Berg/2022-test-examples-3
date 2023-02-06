package ru.yandex.market.mbi.feed.processor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.market.application.properties.AppPropertyContextInitializer
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.common.test.junit.JupiterDbUnitTest
import ru.yandex.market.common.test.spring.PropertiesDirInitializer
import ru.yandex.market.mbi.feed.processor.config.SpringApplicationConfig

/**
 * Базовый класс для функциональных тестов.
 */
@ExtendWith(SpringExtension::class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [SpringApplicationConfig::class],
    properties = ["file.encoding=UTF-8"]
)
@ActiveProfiles(profiles = ["functionalTest", "development"])
@TestPropertySource(locations = ["classpath:functional-test.properties"])
@DbUnitDataSet // чтобы сбрасывать базу перед каждым тестом
@ContextConfiguration(
    initializers = [
        PropertiesDirInitializer::class,
        AppPropertyContextInitializer::class
    ]
)
internal abstract class FunctionalTest : JupiterDbUnitTest() {

    @LocalServerPort
    private val port = 0

    val baseUrl: String
        get() = "http://localhost:$port"

    @ExperimentalCoroutinesApi
    private val testDispatcher = TestCoroutineDispatcher()

    @Before
    @ExperimentalCoroutinesApi
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    @ExperimentalCoroutinesApi
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
}
