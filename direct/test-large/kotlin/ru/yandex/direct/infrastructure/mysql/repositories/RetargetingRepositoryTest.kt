package ru.yandex.direct.infrastructure.mysql.repositories

import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import ru.yandex.direct.domain.retargeting.retargetingTestSuit
import ru.yandex.direct.infrastructure.mysql.ShardedDatabaseExtension
import ru.yandex.direct.infrastructure.mysql.tables.BidsRetargeting
import ru.yandex.direct.infrastructure.mysql.tables.RetargetingConditions

class RetargetingRepositoryTest : FunSpec({
    val database = install(ShardedDatabaseExtension())

    database.shards.values.forEach { shard ->
        transaction(shard) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(RetargetingConditions)
            SchemaUtils.create(BidsRetargeting)
        }
    }

    include(
        retargetingTestSuit(
            MysqlRetargetingRepository(
                database,
                database.shards[1]!!,
                MysqlRetargetingConditionRepository(database)
            )
        )
    )
})
