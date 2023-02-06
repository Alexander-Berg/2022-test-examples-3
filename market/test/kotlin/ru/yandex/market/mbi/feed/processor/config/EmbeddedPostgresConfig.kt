package ru.yandex.market.mbi.feed.processor.config

import de.flapdoodle.embed.process.runtime.Network
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.postgresql.Driver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.core.env.Environment
import org.springframework.core.io.Resource
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.embedded.ConnectionProperties
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseConfigurer
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactoryBean
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import ru.yandex.market.common.postgres.test.PGConfigBuilder
import ru.yandex.market.common.postgres.test.PGEmbeddedDatabase
import ru.yandex.market.common.postgres.test.PGEmbeddedDatasource
import ru.yandex.market.common.test.jdbc.InstrumentedDataSourceFactory
import ru.yandex.market.common.test.spring.DbUnitTestConfig
import ru.yandex.market.common.test.transformer.StringTransformer
import ru.yandex.qatools.embed.postgresql.config.AbstractPostgresConfig
import ru.yandex.qatools.embed.postgresql.config.PostgresConfig
import ru.yandex.qatools.embed.postgresql.distribution.Version
import java.sql.SQLException
import java.util.Objects
import javax.sql.DataSource

private const val PG_LOCAL_PORT = "PG_LOCAL_PORT"
private const val PG_LOCAL_DATABASE = "PG_LOCAL_DATABASE"
private const val PG_LOCAL_USER = "PG_LOCAL_USER"
private const val PG_LOCAL_PASSWORD = "PG_LOCAL_PASSWORD"

@Configuration
@Import(
    EmbeddedPostgresConfig.EmbeddedDbConfig::class,
    EmbeddedPostgresConfig.EnvironmentDbConfig::class
)
internal class EmbeddedPostgresConfig(
    @Autowired(required = false)
    private val pgEmbeddedDatabase: PGEmbeddedDatabase?,
    private val postgresConfig: PostgresConfig
) : DbUnitTestConfig() {

    private val log = KotlinLogging.logger { }

    override fun databaseType(): EmbeddedDatabaseType {
        return EmbeddedDatabaseType.H2 // не используется по факту, но null вернуть нельзя
    }

    override fun databaseResources(): List<Resource> {
        return emptyList()
    }

    override fun createSqlTransformer(): StringTransformer {
        return StringTransformer { it }
    }

    override fun createDataSourceFactory(vararg scripts: Resource): EmbeddedDatabaseFactoryBean {
        val factoryBean = EmbeddedDatabaseFactoryBean()
        factoryBean.setDatabaseConfigurer(object : EmbeddedDatabaseConfigurer {
            override fun configureConnectionProperties(properties: ConnectionProperties, databaseName: String) {
                // prefer config values
                val host = StringUtils.defaultIfBlank(postgresConfig.net().host(), "localhost")
                val port = postgresConfig.net().port()
                val db = StringUtils.defaultIfBlank(postgresConfig.storage().dbName(), databaseName)
                val user = StringUtils.defaultString(postgresConfig.credentials().username())
                val password = StringUtils.defaultString(postgresConfig.credentials().password())

                with(properties) {
                    setDriverClass(Driver::class.java)
                    setUrl("jdbc:postgresql://$host:$port/$db")
                    setUsername(user)
                    setPassword(password)
                }
            }

            override fun shutdown(dataSource: DataSource, databaseName: String) {
                try {
                    dataSource.connection?.close()
                } catch (ex: SQLException) {
                    log.error("Could not close JDBC Connection on shutdown", ex)
                }
            }
        })
        factoryBean.setDataSourceFactory(InstrumentedDataSourceFactory(createSqlTransformer()))
        factoryBean.setDatabaseName("testDataBase" + System.currentTimeMillis())
        return factoryBean
    }

    @Bean("tmsDataSourceWrapped", "postgresDatasource")
    fun postgresDatasource(dataSource: DataSource): DataSource = dataSource

    @Bean
    @Primary
    fun namedParameterJdbcTemplate(@Qualifier("namedParameterJdbcTemplate") namedParameterJdbcTemplate: NamedParameterJdbcTemplate) =
        namedParameterJdbcTemplate

    @Configuration
    @Conditional(
        EnvironmentDbConfig.ActivateCondition::class
    )
    class EnvironmentDbConfig(
        private val environment: Environment
    ) {
        internal class ActivateCondition : Condition {
            override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
                return StringUtils.isNotBlank(context.environment.getProperty(PG_LOCAL_PORT))
            }
        }

        @Bean
        fun postgresConfig(): PostgresConfig {
            return PostgresConfig(
                Version.Main.V11, // stub
                AbstractPostgresConfig.Net(
                    Network.getLocalHost().hostAddress,
                    Objects.requireNonNull(
                        environment.getProperty(PG_LOCAL_PORT),
                        PG_LOCAL_PORT
                    ).toInt()
                ),
                AbstractPostgresConfig.Storage(
                    Objects.requireNonNull(environment.getProperty(PG_LOCAL_DATABASE), PG_LOCAL_DATABASE)
                ),
                AbstractPostgresConfig.Timeout(),
                AbstractPostgresConfig.Credentials(
                    environment.getProperty(PG_LOCAL_USER),
                    environment.getProperty(PG_LOCAL_PASSWORD)
                )
            )
        }
    }

    @Configuration
    @Conditional(
        EmbeddedDbConfig.ActivateCondition::class
    )
    class EmbeddedDbConfig {
        internal class ActivateCondition : Condition {
            override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
                return !EnvironmentDbConfig.ActivateCondition().matches(context, metadata)
            }
        }

        @Bean
        fun postgresConfig(): PostgresConfig {
            return PGConfigBuilder()
                .setVersion(Version.Main.V11)
                .build()
        }

        @Bean("dataSource")
        @DependsOn("pgEmbeddedDatabase")
        fun dataSource(): PGEmbeddedDatasource {
            return PGEmbeddedDatasource(postgresConfig())
        }

        @Bean
        fun pgEmbeddedDatabase(): PGEmbeddedDatabase {
            return PGEmbeddedDatabase(postgresConfig())
        }
    }
}
