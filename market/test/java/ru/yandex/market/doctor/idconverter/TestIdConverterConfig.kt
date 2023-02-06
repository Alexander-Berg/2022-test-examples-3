package ru.yandex.market.doctor.idconverter

import com.nhaarman.mockitokotlin2.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.support.beans

@TestConfiguration
open class TestIdConverterConfig : IdConverterConfig {
    @Bean
    override fun idConverterService(): IdConverterService {
        return mock()
    }
}
