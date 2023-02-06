package ru.yandex.market.contentmapping.testutils

import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import ru.yandex.market.contentmapping.config.TestConfig

/**
 * @author yuramalinov
 * @created 24.02.2020
 */
@RunWith(SpringRunner::class)
@ActiveProfiles("test")
@ContextConfiguration(
        initializers = [PgTestInitializer::class, MockServerInitializer::class],
        classes = [TestConfig::class]
)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class BaseWebTestClass {
    @Autowired
    protected lateinit var mockMvc: MockMvc
}
