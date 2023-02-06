package ru.yandex.market.doctor.proxy

import com.nhaarman.mockitokotlin2.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.beans

@TestConfiguration
class TestServiceProxyConfig {
    @Bean
    fun serviceProxyService(): ServiceProxyService {
        return mock()
    }
}
