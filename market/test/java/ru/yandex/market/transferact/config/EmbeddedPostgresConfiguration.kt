package ru.yandex.market.transferact.config

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
@EnableAutoConfiguration(exclude = [DataSourceAutoConfiguration::class])
open class EmbeddedPostgresConfiguration {

    @Bean
    open fun embeddedPostgres(): EmbeddedPostgres {
        return EmbeddedPostgres.builder()
            .setServerConfig("unix_socket_directories", "")
            .start();
    }

    @Bean
    @Primary
    open fun dataSource(embeddedPostgres: EmbeddedPostgres): DataSource {
        return ProxyDataSourceBuilder.create(embeddedPostgres.postgresDatabase)
            .name("Batch-Insert-Logger")
            .asJson().countQuery().logQueryToSysOut().build()
    }

}
