package ru.yandex.market.logistics.cte.config

import com.nhaarman.mockitokotlin2.any
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI
import ru.yandex.market.common.mds.s3.client.content.ContentConsumer
import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi
import ru.yandex.market.logistics.cte.monitoring.solomon.SolomonPushClient
import ru.yandex.market.logistics.lom.client.LomClient
import ru.yandex.market.mboc.http.DeliveryParams
import ru.yandex.market.personal.client.api.DefaultPersonalApi
import ru.yandex.market.tanker.client.TankerClient
import ru.yandex.startrek.client.Session
import java.net.URL

@Configuration
open class MockConfiguration {

    @Bean
    open fun deliveryParamsService(): DeliveryParams {
        return Mockito.mock(DeliveryParams::class.java)
    }

    @Bean
    open fun solomonPushClient(): SolomonPushClient {
        return Mockito.mock(SolomonPushClient::class.java)
    }

    @Bean
    open fun startrekSession(): Session {
        return Mockito.mock(Session::class.java)
    }

    @Bean
    open fun tankerClient(): TankerClient {
        return Mockito.mock(TankerClient::class.java)
    }

    @Bean
    open fun yqlJdbcTemplate(): JdbcTemplate {
        return Mockito.mock(JdbcTemplate::class.java)
    }

    @Bean
    open fun ytClusterStage(): String {
        return "local"
    }

    @Bean
    open fun mdsS3Client(): MdsS3Client {
        val client = Mockito.mock(MdsS3Client::class.java)
        Mockito.`when`(client.getUrl(any())).thenAnswer { URL("https://document") }
        Mockito.`when`(client.download(any(), any<ContentConsumer<*>>())).thenAnswer { arguments ->
            val inputStream = "some text".byteInputStream()
            val outputStream = arguments.getArgument(1, StreamCopyContentConsumer::class.java)
            outputStream.consume(inputStream)
        }

        return client
    }

    @Bean
    open fun fulfillmentWorkflowClientApi(): FulfillmentWorkflowClientApi {
        return Mockito.mock(FulfillmentWorkflowClientApi::class.java)
    }

    @Bean("checkouterApi")
    open fun checkouterApi(): CheckouterAPI =
        Mockito.mock(CheckouterAPI::class.java)

    @Bean
    open fun locationFactory(): ResourceLocationFactory {
        val factory = Mockito.mock(ResourceLocationFactory::class.java)
        Mockito.`when`(factory.createLocation(any()))
            .thenReturn(Mockito.mock(ResourceLocation::class.java))

        return factory
    }

    @Bean
    fun mockLomClient(): LomClient {
        return Mockito.mock(LomClient::class.java)
    }

    @Bean
    fun personalMultiTypesRetrieveApi(): DefaultPersonalApi {
        return Mockito.mock(DefaultPersonalApi::class.java)
    }

}
