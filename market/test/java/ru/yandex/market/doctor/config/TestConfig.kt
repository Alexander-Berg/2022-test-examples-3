package ru.yandex.market.doctor.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.PropertySource
import ru.yandex.market.doctor.proxy.TestServiceProxyConfig
import ru.yandex.market.doctor.session.exporter.TestSessionExporterConfig
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper

@Import(
    AppConfig::class,
    TestYtConfig::class,
    TestServiceProxyConfig::class,
    TestSessionExporterConfig::class,
)
@TestConfiguration
@PropertySource("classpath:test.properties")
open class TestConfig {
    /**
     * Use mock transaction helper in tests to be able to read other transaction's changes
     */
    @Bean
    fun transactionHelper(): TransactionHelper = TransactionHelper.MOCK
}
