package ru.yandex.market.mbi.orderservice.tms.config

import de.flapdoodle.embed.process.runtime.Network
import liquibase.integration.spring.SpringLiquibase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import ru.yandex.market.common.postgres.test.PGEmbeddedDatasource
import ru.yandex.market.mbi.helpers.CiOnly
import ru.yandex.market.mbi.helpers.LocalEnvOnly
import ru.yandex.market.mbi.orderservice.common.util.injectedLogger
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig
import ru.yandex.qatools.embed.postgresql.distribution.Version
import javax.sql.DataSource

const val EMBEDDED_DATASOURCE = "pgEmbeddedDatasource"

const val ENV_PORT = "PG_LOCAL_PORT"
const val ENV_DATABASE = "PG_LOCAL_DATABASE"
const val ENV_USER = "PG_LOCAL_USER"
const val ENV_PASSWORD = "PG_LOCAL_PASSWORD"

@Configuration
open class LiquibaseConfig {

    @Bean
    open fun springLiquibase(
        @Value("\${mbi.liquibase.changelogPath}") changelogPath: String,
        @Qualifier(EMBEDDED_DATASOURCE) datasource: DataSource
    ): SpringLiquibase =
        SpringLiquibase().apply {
            dataSource = datasource
            changeLog = changelogPath
        }
}

@Configuration
open class LocalEmbeddedPostgresConfig(@Value("\${mbi.pg.embedded.schema}") val schema: String) {

    @Autowired
    lateinit var environment: Environment

    @Bean
    @CiOnly
    open fun recipePostgresConfig(): PostgresConfig =
        PostgresConfig(
            Version.Main.PRODUCTION,
            AbstractPostgresConfig.Net(
                Network.getLocalHost().hostAddress,
                requireNotNull(environment.getProperty(ENV_PORT, Int::class.java))
            ),
            AbstractPostgresConfig.Storage(
                requireNotNull(environment.getProperty(ENV_DATABASE))
            ),
            AbstractPostgresConfig.Timeout(),
            AbstractPostgresConfig.Credentials(
                environment.getProperty(ENV_USER),
                environment.getProperty(ENV_PASSWORD)
            )
        )

    @Primary
    @Bean(name = [EMBEDDED_DATASOURCE, "dataSource"])
    open fun pgEmbeddedDatasource(
        postgresConfig: PostgresConfig,
        @Autowired(required = false) postgresContainer: PostgreSQLContainer<*>?
    ): PGEmbeddedDatasource =
        PGEmbeddedDatasource(postgresConfig, schema)

    @Configuration
    @LocalEnvOnly
    @Testcontainers
    open class TestContainersPgConfig(
        @Value("\${mbi.pg.embedded.username}") val userName: String,
        @Value("\${mbi.pg.embedded.password}") val passWord: String,
        @Value("\${mbi.pg.embedded.schema}") val schema: String
    ) {

        private val log by injectedLogger()

        @Bean(initMethod = "start")
        open fun postgresContainer(): PostgreSQLContainer<Nothing> {
            return PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:13-alpine")).apply {
                withExposedPorts(5432)
                withLogConsumer(Slf4jLogConsumer(log))
                withUsername(userName)
                withPassword(passWord)
                waitingFor(Wait.forListeningPort())
            }
        }

        @Bean
        @LocalEnvOnly
        open fun localPostgresConfig(postgresContainer: PostgreSQLContainer<*>): PostgresConfig =
            PostgresConfig(
                Version.Main.PRODUCTION,
                AbstractPostgresConfig.Net(
                    Network.getLocalHost().hostAddress,
                    requireNotNull(postgresContainer.firstMappedPort)
                ),
                AbstractPostgresConfig.Storage("postgres"),
                AbstractPostgresConfig.Timeout(),
                AbstractPostgresConfig.Credentials(userName, passWord)
            )
    }
}
