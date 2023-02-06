package ru.yandex.direct.infrastructure.mysql

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.extensions.MountableExtension
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.spec.Spec
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import ru.yandex.direct.infrastructure.mysql.sharding.ShardedPPCDict

class ShardedDatabaseExtension : MountableExtension<Unit, ShardedPPCDict>, AfterSpecListener {
    private val shardCount = 2

    private val mysqlImage = DockerImageName.parse("mysql:5.7")

    private val container = MySQLContainer(mysqlImage)
        .withUsername("root")

    override fun mount(configure: Unit.() -> Unit): ShardedPPCDict {
        container.start()
        initDatabases()

        return ShardedPPCDict(
            ppcdict = Database.connect(createDataSource("ppcdict")),
            shards = (1..shardCount)
                .associateWith { shard -> Database.connect(createDataSource("ppc$shard")) },
        )
    }

    private fun initDatabases() {
        val dataSource = createDataSource(container.databaseName)
        val default = Database.connect(dataSource)
        transaction(default) {
            SchemaUtils.createDatabase("ppcdict")
            (1..shardCount).forEach { shard ->
                SchemaUtils.createDatabase("ppc$shard")
            }
        }
    }

    private fun createDataSource(database: String): HikariDataSource {
        val config = HikariConfig()
        config.jdbcUrl = jdbcUrl(database)
        config.username = container.username
        config.password = container.password
        return HikariDataSource(config)
    }

    private fun jdbcUrl(database: String): String {
        return "jdbc:mysql://${container.host}:${container.getMappedPort(MySQLContainer.MYSQL_PORT)}/$database"
    }

    override suspend fun afterSpec(spec: Spec) {
        container.stop()
    }
}
