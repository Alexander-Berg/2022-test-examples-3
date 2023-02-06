package ru.yandex.market.mdm.metadata.testutils

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.json.JsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import com.jayway.jsonpath.spi.mapper.MappingProvider
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.mdm.lib.testutils.PgTestInitializer
import ru.yandex.market.mdm.metadata.config.TestConfig
import java.util.EnumSet

@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@ContextConfiguration(
    initializers = [PgTestInitializer::class, MockServerInitializer::class],
    classes = [TestConfig::class]
)
@Transactional
@AutoConfigureMockMvc
abstract class BaseAppTestClass {
    init {
        Configuration.setDefaults(object : Configuration.Defaults {
            val jsonProvider = JacksonJsonProvider()
            val mappingProvider = JacksonMappingProvider()

            override fun jsonProvider(): JsonProvider {
                return jsonProvider
            }

            override fun mappingProvider(): MappingProvider {
                return mappingProvider
            }

            override fun options(): MutableSet<Option> {
                return EnumSet.noneOf(Option::class.java)
            }
        })
    }

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var namedJdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    protected fun loadResource(resourceName: String) =
        javaClass.classLoader.getResourceAsStream(resourceName)
            ?: throw IllegalArgumentException("Can't load resource $resourceName")
}
