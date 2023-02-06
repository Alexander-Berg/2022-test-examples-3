package ru.yandex.direct.infrastructure.mysql.repositories

import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import ru.yandex.direct.domain.retargeting.retargetingConditionTestSuit
import ru.yandex.direct.infrastructure.mysql.ShardedDatabaseExtension
import ru.yandex.direct.infrastructure.mysql.sharding.ShardIncRetCondIds
import ru.yandex.direct.infrastructure.mysql.tables.RetargetingConditions

class RetargetingConditionRepositoryTest : FunSpec({
    val database = install(ShardedDatabaseExtension())

    transaction(database.ppcdict) {
        SchemaUtils.create(ShardIncRetCondIds)
    }

    database.shards.values.forEach { shard ->
        transaction(shard) {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(RetargetingConditions)
        }
    }

    include(retargetingConditionTestSuit(MysqlRetargetingConditionRepository(database)))
})
