package ru.yandex.market.mapi.db.mock

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.yandex.market.mapi.config.DataSourceConfig
import java.io.Closeable
import java.sql.SQLException
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.math.max

object EmbeddedPostgresFactory {
    private val log: Logger = LoggerFactory.getLogger(EmbeddedPostgresFactory::class.java)

    /**
     * Embedded postres or reciepe.
     * Starts db with url=jdbc:postgresql://localhost:#{embeddedPostgres.getPort()}/postgres
     * User/password = postgres.
     * <p>
     * Uses market.pers.use.pg.recipe environment to decide if recipe should be used (always active in ya.make).
     * <p>
     * Don't forget to close on destroy!
     */
    fun embeddedPostgres(customizer: EmbeddedPostgres.Builder.() -> Unit = {}): Closeable {
        val recipeEnv = System.getenv("market.mapi.use.pg.recipe")
        log.info("Reciple pg flag = $recipeEnv")
        val useRecipe = recipeEnv?.equals("true", true) ?: false

        // initializes embedded/recipe postgres connection
        //market.pers.jdbc.writeUrl=jdbc:postgresql://localhost:#{embeddedPostgres.getPort()}/postgres
        //market.pers.jdbc.username=postgres
        //market.pers.jdbc.password=postgres

        // initializes embedded/recipe postgres connection
        //market.pers.jdbc.writeUrl=jdbc:postgresql://localhost:#{embeddedPostgres.getPort()}/postgres
        //market.pers.jdbc.username=postgres
        //market.pers.jdbc.password=postgres
        return if (useRecipe) {
            log.info("Running tests with postgres recipe configuration")
            return RecipeAwarePostgres()
        } else {
            try {
                println("Running tests with postgres local configuration")
                EmbeddedPostgres.builder().also(customizer).start()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    fun embeddedDatasource(portProvider: Any, props: Map<String, String>): DataSource {
        val getPort = portProvider.javaClass.getMethod("getPort")
        val port = getPort.invoke(portProvider) as Int
        val ds = PGSimpleDataSource()
            .also { dataSource ->
                dataSource.setURL("jdbc:postgresql://localhost:$port/postgres")
                dataSource.databaseName = "postgres"
                dataSource.user = "postgres"
            }

        ds.setURL("jdbc:postgresql://localhost:$port/postgres")
        ds.databaseName = "postgres"
        ds.user = "postgres"

        props.forEach { (propertyKey: String?, propertyValue: String?) ->
            try {
                ds.setProperty(propertyKey, propertyValue)
            } catch (e: SQLException) {
                throw RuntimeException(e)
            }
        }
        return ds
    }

    fun embeddedHikariDatasource(
        portProvider: Any,
        props: Map<String, String>,
        customizer: (HikariConfig) -> Unit = {}
    ): DataSource {
        val getPort = portProvider.javaClass.getMethod("getPort")
        val port = getPort.invoke(portProvider) as Int

        val hikariDataSource = HikariConfig()
            .also { config ->
                config.jdbcUrl = "jdbc:postgresql://localhost:$port/postgres"
                config.username = "postgres"
                config.maximumPoolSize = 10
                // idle connections config
                config.minimumIdle = max(10 / 5, 1)
                config.idleTimeout = TimeUnit.MINUTES.toMillis(DataSourceConfig.MAX_IDLE_TIMEOUT_MINUTES.toLong())
                // other timing configs
                config.connectionTimeout =
                    TimeUnit.SECONDS.toMillis(DataSourceConfig.CONNECTION_TIMEOUT_SECONDS.toLong())
                config.maxLifetime = TimeUnit.HOURS.toMillis(DataSourceConfig.MAX_LIFE_TIME_HOURS.toLong())
                config.connectionTestQuery = "SELECT 1"

                props.forEach { (propertyKey: String?, propertyValue: String?) ->
                    try {
                        config.addDataSourceProperty(propertyKey, propertyValue)
                    } catch (e: SQLException) {
                        throw RuntimeException(e)
                    }
                }
            }
            .also(customizer)
            .let { HikariDataSource(it) }

        return hikariDataSource
    }
}
