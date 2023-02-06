package ru.yandex.market.contentmapping.testutils

import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.contentmapping.config.TestConfig
import ru.yandex.market.contentmapping.config.TestServicesConfig

/**
 * @author yuramalinov
 * @created 24.02.2020
 */
@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@ContextConfiguration(
        initializers = [PgTestInitializer::class, MockServerInitializer::class],
        classes = [TestConfig::class, TestServicesConfig::class]
)
@Transactional
abstract class BaseAppTestClass {
    @Autowired
    lateinit var context: ConfigurableApplicationContext

    protected fun loadResource(resourceName: String) =
        javaClass.classLoader.getResourceAsStream(resourceName)
            ?: throw IllegalArgumentException("Can't load resource $resourceName")

    // NOTE: final is required here
    final inline fun <reified T> bean(name: String? = null) = lazy {
        if (name != null) {
            context.getBean(name, T::class.java)
        } else {
            context.getBean(T::class.java)
        }
    }
}
