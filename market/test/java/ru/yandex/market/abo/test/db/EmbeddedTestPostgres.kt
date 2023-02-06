package ru.yandex.market.abo.test.db

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.postgresql.PGProperty
import javax.sql.DataSource

class EmbeddedTestPostgres : TestPostgres {
    private val pg = EmbeddedPostgres.builder()
        .setConnectConfig(PGProperty.REWRITE_BATCHED_INSERTS.getName(), "true")
        .start()

    override fun getDataSource(): DataSource = pg.postgresDatabase

    override fun getJdbcUrl(): String = pg.getJdbcUrl("postgres", "postgres")

    override fun close() = pg.close()
}
