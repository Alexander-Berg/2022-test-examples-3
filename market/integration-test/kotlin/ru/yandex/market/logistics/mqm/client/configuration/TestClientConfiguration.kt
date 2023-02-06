package ru.yandex.market.logistics.mqm.client.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import ru.yandex.market.logistics.mqm.client.MqmClientImpl
import ru.yandex.market.logistics.util.client.ClientUtilsFactory
import ru.yandex.market.logistics.util.client.HttpTemplate
import ru.yandex.market.logistics.util.client.HttpTemplateImpl
import ru.yandex.market.logistics.util.client.TvmTicketProvider
import java.nio.charset.StandardCharsets

@Configuration
class TestClientConfiguration {

    @Bean
    fun mqmClient(httpTemplate: HttpTemplate, objectMapper: ObjectMapper) = MqmClientImpl(httpTemplate, objectMapper)

    @Bean
    fun httpTemplate(
        @Value("\${mqm.api.url}") host: String,
        restTemplate: RestTemplate,
        tvmTicketProvider: TvmTicketProvider
    ) = HttpTemplateImpl(host, restTemplate, tvmTicketProvider)

    @Bean
    fun clientRestTemplate(objectMapper: ObjectMapper) = RestTemplateBuilder()
        .messageConverters(listOf(
            StringHttpMessageConverter(StandardCharsets.UTF_8),
            MappingJackson2HttpMessageConverter(objectMapper)
        ))
        .build()

    @Bean
    fun tvmTicketProvider() = object : TvmTicketProvider {
        override fun provideServiceTicket() = "test-service-ticket"
        override fun provideUserTicket() = "test-user-ticket"
    }

    @Bean
    fun objectMapper() = ClientUtilsFactory
        .getObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}
