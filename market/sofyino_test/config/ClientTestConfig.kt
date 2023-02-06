package ru.yandex.market.logistics.yard_v2.external.pass_connector.sofyino_test.config

import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPost
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
open class ClientTestConfig(
    @Value("\${sofyino-ffc.client.test.connectTimeout:30000}") val connectTimeout: Int,
    @Value("\${sofyino-ffc.client.test.connectionRequestTimeout:30000}") val connectionRequestTimeout: Int,
    @Value("\${sofyino-ffc.client.test.socketTimeout:30000}") val socketTimeout: Int,
    @Value("\${sofyino-ffc.client.test.url:}") val url: String,
    @Value("\${sofyino-ffc.client.test.authToken:}") val authToken: String
) {

    fun getSofyinoClientConfig(): RequestConfig {
        return RequestConfig.custom()
            .setConnectTimeout(connectTimeout)
            .setConnectionRequestTimeout(connectionRequestTimeout)
            .setSocketTimeout(socketTimeout)
            .build()
    }

    fun getHttpPost(): HttpPost {
        val httpPost = HttpPost(url)
        httpPost.addHeader("Authorization", authToken)

        return httpPost
    }
}
