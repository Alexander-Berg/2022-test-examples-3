package ru.yandex.market.mdm.lib.testutils

import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.json.JsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import com.jayway.jsonpath.spi.mapper.MappingProvider
import org.junit.runner.RunWith
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.mdm.lib.config.TestConfig
import java.util.EnumSet

@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@ContextConfiguration(
    initializers = [PgTestInitializer::class],
    classes = [TestConfig::class]
)
@Transactional
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
}
