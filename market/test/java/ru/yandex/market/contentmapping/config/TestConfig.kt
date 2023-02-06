package ru.yandex.market.contentmapping.config

import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import ru.yandex.market.contentmapping.config.utils.TestServiceWrapper

/**
 * @author yuramalinov
 * @created 24.02.2020
 */
@Configuration
@EnableWebSecurity
@Import(
        TestSqlDatasourceConfig::class,
        ProtoServicesMockConfig::class,
        TestAuthConfig::class,
        AppConfig::class,
        TestDaoConfig::class,
        TestControllerConfig::class,
        TestYtConfig::class,
        TestRedisConfig::class,
)
@PropertySource("classpath:test.properties")
open class TestConfig {
    @Bean
    fun testServiceWrapper() = object: TestServiceWrapper {
        override fun <T> spyInTests(service: T): T = Mockito.spy(service)
    }
}
