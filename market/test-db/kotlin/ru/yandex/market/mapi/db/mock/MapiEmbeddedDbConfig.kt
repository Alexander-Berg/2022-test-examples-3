package ru.yandex.market.mapi.db.mock

import liquibase.integration.spring.SpringLiquibase
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.mapi.core.NonEngineConfig
import java.util.Map
import javax.sql.DataSource

@TestConfiguration
@NonEngineConfig
open class MapiEmbeddedDbConfig {

    @Bean(destroyMethod = "close")
    open fun embeddedPostgres(): Any {
        return EmbeddedPostgresFactory.embeddedPostgres()
    }

    @Bean
    open fun embeddedDatasource(): DataSource? {
        //todo revert
        // return EmbeddedPostgresFactory.embeddedDatasource(embeddedPostgres(), Map.of())
        return EmbeddedPostgresFactory.embeddedHikariDatasource(embeddedPostgres(), Map.of())
    }

    @Bean
    open fun pgLiquibase(): SpringLiquibase? {
        val result = SpringLiquibase()
        result.dataSource = pgDataSource()
        result.changeLog = "classpath:liquibase/db-changelog.xml"
        result.setChangeLogParameters(mapOf("is-unit-testing" to "true"))
        return result
    }

    @Bean
    open fun pgDataSource(): DataSource? {
        return embeddedDatasource()
    }

    @Bean
    open fun pgJdbcTemplate(@Qualifier("pgDataSource") dataSource: DataSource): JdbcTemplate {
        return JdbcTemplate(dataSource)
    }


    @Bean
    open fun tmsJdbcTemplate(@Qualifier("pgDataSource") dataSource: DataSource): JdbcTemplate {
        return JdbcTemplate(dataSource)
    }
}
