package ru.yandex.market.logistics.yard.client.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.test.web.client.MockRestServiceServer
import ru.yandex.market.logistics.util.client.ExternalServiceProperties
import ru.yandex.market.logistics.util.client.HttpTemplate
import ru.yandex.market.logistics.util.client.HttpTemplateBuilder
import ru.yandex.market.logistics.util.client.HttpTemplateImpl
import ru.yandex.market.logistics.yard.client.YardClientApiImpl
import ru.yandex.market.request.trace.Module

@Configuration
@PropertySource("classpath:application-integration-test.properties")
open class YardClientApiTestConfig {

    @Bean
    open fun fulfillmentYardProperties(
        @Value("\${fulfillment-yard.api.host}") host: String
    ): ExternalServiceProperties {
        val properties = ExternalServiceProperties()
        properties.url = host
        return properties
    }

    @Bean
    open fun yardHttpTemplate(yardProperties: ExternalServiceProperties): HttpTemplate {
        return HttpTemplateBuilder
            .create(yardProperties, Module.MARKET_FF_YARD)
            .build()
    }

    @Bean
    open fun yardServiceClient(
        yardHttpTemplate: HttpTemplate
    ): YardClientApiImpl {
        return YardClientApiImpl(yardHttpTemplate)
    }

    @Bean
    open fun mockRestServiceServer(yardClient: YardClientApiImpl): MockRestServiceServer {
        return MockRestServiceServer.createServer((yardClient.httpTemplate as HttpTemplateImpl).restTemplate)
    }
}
