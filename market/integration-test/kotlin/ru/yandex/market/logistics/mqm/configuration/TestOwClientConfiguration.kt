package ru.yandex.market.logistics.mqm.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import ru.yandex.market.logistics.mqm.ow.OwClientImpl
import ru.yandex.market.logistics.util.client.HttpTemplateImpl
import ru.yandex.market.logistics.util.client.TvmTicketProvider

@Configuration
class TestOwClientConfiguration {

    @Bean
    fun owClient(
        @Value("\${ow.url}") host: String,
        restTemplate: RestTemplate,
        tvmTicketProvider: TvmTicketProvider,
        @Value("\${ow.enable_call}") enableCall: Boolean,
    ) = OwClientImpl(
        httpTemplate(host, restTemplate, tvmTicketProvider),
        enableCall,
        host,
    )

    fun httpTemplate(
        host: String,
        restTemplate: RestTemplate,
        tvmTicketProvider: TvmTicketProvider
    ) = HttpTemplateImpl(host, restTemplate, tvmTicketProvider)

    @Bean
    fun clientRestTemplate() = RestTemplateBuilder().build()

    @Bean
    fun tvmTicketProvider() = object : TvmTicketProvider {
        override fun provideServiceTicket() = "test-service-ticket"
        override fun provideUserTicket() = "test-user-ticket"
    }
}
