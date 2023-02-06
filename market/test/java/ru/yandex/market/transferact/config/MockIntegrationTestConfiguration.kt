package ru.yandex.market.transferact.config

import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.yandex.market.transferact.external.SignatureRequestCallbackClient

@Configuration
class MockIntegrationTestConfiguration {

    @Bean
    fun signatureRequestCallbackClient(): SignatureRequestCallbackClient {
        return Mockito.mock(SignatureRequestCallbackClient::class.java)
    }

}
