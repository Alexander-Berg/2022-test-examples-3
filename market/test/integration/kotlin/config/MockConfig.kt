package ru.yandex.market.logistics.calendaring.config

import org.mockito.Mockito
import org.springframework.boot.test.mock.mockito.MockReset.after
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi
import ru.yandex.market.logbroker.consumer.LogbrokerReader
import ru.yandex.market.logistics.calendaring.config.logbroker.LogbrokerEventConsumerProperties
import ru.yandex.market.logistics.calendaring.service.datetime.geobase.GeobaseProviderApi
import ru.yandex.market.logistics.management.client.LMSClient

@Configuration
open class MockConfig {

    @Bean
    open fun lmsClient(): LMSClient? {
        return Mockito.mock(LMSClient::class.java, after())
    }

    @Bean
    open fun geobaseProviderApi(): GeobaseProviderApi {
        return Mockito.mock(GeobaseProviderApi::class.java)
    }

    @Bean
    open fun logbrokerEventConsumerProperties(): LogbrokerEventConsumerProperties {
        return Mockito.mock(LogbrokerEventConsumerProperties::class.java, after())
    }

    @Bean
    open fun logbrokerReader(): LogbrokerReader {
        return Mockito.mock(LogbrokerReader::class.java, after())
    }

    @Bean
    open fun ffwfClientApi(): FulfillmentWorkflowClientApi {
        return Mockito.mock(FulfillmentWorkflowClientApi::class.java, after())
    }

}
