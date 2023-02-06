package ru.yandex.market.transferact.config

import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi
import ru.yandex.passport.tvmauth.TvmClient

@Configuration
class MockTvmConfiguration {

    @Primary
    @Bean
    fun mockedTvmClientApi() = Mockito.mock(TvmClientApi::class.java)

    @Primary
    @Bean
    fun mockedTvmClient() = Mockito.mock(TvmClient::class.java)

}
