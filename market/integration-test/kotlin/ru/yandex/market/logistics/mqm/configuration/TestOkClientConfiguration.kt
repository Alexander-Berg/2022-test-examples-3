package ru.yandex.market.logistics.mqm.configuration

import javax.annotation.Nonnull
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import ru.yandex.market.logistics.mqm.service.ok.OkClient
import ru.yandex.market.logistics.mqm.service.ok.OkClientImpl
import ru.yandex.market.logistics.util.client.ExternalServiceProperties
import ru.yandex.market.logistics.util.client.HttpTemplate
import ru.yandex.market.logistics.util.client.HttpTemplateImpl
import ru.yandex.market.logistics.util.client.TvmTicketProvider

@Configuration
class TestOkClientConfiguration {
    @Bean
    @ConfigurationProperties("ok")
    fun okClientProperties() = ExternalServiceProperties()

    @Bean
    fun okClient(
        okClientProperties: ExternalServiceProperties,
        ticketProvider: TvmTicketProvider,
        restTemplate: RestTemplate,
    ): OkClient {
        val okJsonConverter = createOkJsonConverter()
        val httpTemplate = createHttpTemplate(okClientProperties, ticketProvider, restTemplate)
        return OkClientImpl(httpTemplate, okJsonConverter.objectMapper)
    }

    @Nonnull
    fun createHttpTemplate(
        properties: ExternalServiceProperties,
        tvmTicketProvider: TvmTicketProvider,
        restTemplate: RestTemplate,
    ): HttpTemplate {
        return HttpTemplateImpl(properties.url, restTemplate, tvmTicketProvider)
    }

    @Nonnull
    fun createOkJsonConverter(): MappingJackson2HttpMessageConverter {
        val converter = MappingJackson2HttpMessageConverter()
        converter.objectMapper
            .registerModule(ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        return converter
    }
}
