package ru.yandex.market.mdm.service.shared

import me.dinowernli.grpc.prometheus.MonitoringServerInterceptor
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource
import ru.yandex.market.mdm.db.config.TestSqlMdmDatasourceConfig
import ru.yandex.market.mdm.service.common_entity.testutils.TestServiceRepositoryConfig
import ru.yandex.market.mdm.service.shared.config.BaseConfiguration
import ru.yandex.passport.tvmauth.TvmClient

@Configuration
@Import(
    TestSqlMdmDatasourceConfig::class,
    TestServiceRepositoryConfig::class,
    BaseConfiguration::class
)
@PropertySource("classpath:test.properties")
open class TestConfig {

    @Bean
    @Primary
    open fun monitoringServerInterceptor(): MonitoringServerInterceptor? {
        return Mockito.mock(MonitoringServerInterceptor::class.java)
    }

    @Bean
    @Primary
    open fun tvmClient(): TvmClient {
        return Mockito.mock(TvmClient::class.java)
    }
}

