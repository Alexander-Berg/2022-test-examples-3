package ru.yandex.market.logistics.calendaring.client.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.test.web.client.MockRestServiceServer
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClient
import ru.yandex.market.logistics.util.client.ExternalServiceProperties
import ru.yandex.market.logistics.util.client.HttpTemplate
import ru.yandex.market.logistics.util.client.HttpTemplateBuilder
import ru.yandex.market.logistics.util.client.HttpTemplateImpl
import ru.yandex.market.request.trace.Module


@Configuration
@PropertySource("classpath:application-integration-test.properties")
open class CalendaringServiceClientTestConfig {

    @Bean
    open fun calendaringServiceProperties(
        @Value("\${calendaring-service.api.host}") host: String
    ): ExternalServiceProperties {
        val properties = ExternalServiceProperties()
        properties.url = host
        return properties
    }

    @Bean
    open fun calendaringHttpTemplate(calendaringServiceProperties: ExternalServiceProperties): HttpTemplate {
        return HttpTemplateBuilder
            .create(calendaringServiceProperties, Module.MARKET_CALENDARING_SERVICE)
            .withUrlEncoded(false)
            .build()
    }

    @Bean
    open fun calendaringServiceClient(
        calendaringHttpTemplate: HttpTemplate
    ): CalendaringServiceClient {
        return CalendaringServiceClient(calendaringHttpTemplate)
    }

    @Bean
    open fun mockRestServiceServer(calendaringServiceClient: CalendaringServiceClient): MockRestServiceServer {
        return MockRestServiceServer.createServer((calendaringServiceClient.httpTemplate as HttpTemplateImpl).restTemplate)
    }


}
