package ru.yandex.market.logistics.cte.base

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.springtestdbunit.DbUnitTestExecutionListener
import com.github.springtestdbunit.annotation.DbUnitConfiguration
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener
import org.springframework.boot.test.mock.mockito.ResetMocksTestExecutionListener
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.util.ResourceUtils
import ru.yandex.market.logistics.test.integration.db.listener.CleanDatabase
import ru.yandex.market.logistics.test.integration.db.listener.ResetDatabaseTestExecutionListener
import java.io.IOException

@WebAppConfiguration
@AutoConfigureMockMvc(addFilters = false)
@SpringBootTest(classes = [SecurityTestConfig::class], webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestExecutionListeners(
    DependencyInjectionTestExecutionListener::class,
    ResetDatabaseTestExecutionListener::class,
    DbUnitTestExecutionListener::class,
    MockitoTestExecutionListener::class,
    ResetMocksTestExecutionListener::class
)
@CleanDatabase
@DbUnitConfiguration(
    databaseConnection = ["dbUnitDatabaseConnection", "dbqueueDatabaseConnection"],
    dataSetLoader = NullableColumnsDataSetLoader::class
)
@TestPropertySource("classpath:test.properties")
abstract class AbstractContextualTest {

    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var  mockMvc: MockMvc

    @BeforeEach
    open fun beforeEach() {
        objectMapper = ObjectMapper()
        objectMapper!!.registerModule(JavaTimeModule())
    }

    @Throws(IOException::class)
    protected open fun <T> readFromJson(filename: String?, typedClass: Class<T>?): T {
        val file = ResourceUtils.getFile(filename)
        return objectMapper!!.readValue(file, typedClass)
    }
}
