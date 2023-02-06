package ru.yandex.market.mbo.ydb.config

import com.yandex.ydb.core.auth.AuthProvider
import com.yandex.ydb.core.auth.NopAuthProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import ru.yandex.market.mbo.ydb.client.YdbClient

@TestConfiguration
@EnableConfigurationProperties(TestYdbProperties::class)
open class TestYdbConfig(
    ydbProperties: TestYdbProperties
) : BaseYdbConfig<TestYdbProperties>(ydbProperties) {
    override fun authProvider(): AuthProvider {
        return NopAuthProvider.INSTANCE
    }

    @Bean
    override fun ydbClient(): YdbClient {
        return super.ydbClient()
    }
}
