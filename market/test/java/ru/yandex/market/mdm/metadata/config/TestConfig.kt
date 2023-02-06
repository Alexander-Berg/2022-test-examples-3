package ru.yandex.market.mdm.metadata.config

import me.dinowernli.grpc.prometheus.MonitoringServerInterceptor
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource
import ru.yandex.market.mdm.db.config.TestSqlMdmDatasourceConfig

@Configuration
@Import(
    TestSqlMdmDatasourceConfig::class,
    MdmMetadataConfiguration::class,
    TestMdmMetadataRepositoryConfig::class,
)
@PropertySource("classpath:test.properties")
open class TestConfig {

    @Bean
    @Primary
    open fun monitoringServerInterceptor(): MonitoringServerInterceptor? {
        return Mockito.mock(MonitoringServerInterceptor::class.java)
    }
}
