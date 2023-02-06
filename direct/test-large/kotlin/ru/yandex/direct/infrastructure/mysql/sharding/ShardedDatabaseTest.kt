package ru.yandex.direct.infrastructure.mysql.sharding

import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import ru.yandex.direct.domain.client.ClientID
import ru.yandex.direct.infrastructure.mysql.ShardedDatabaseExtension

class ShardedDatabaseTest : FunSpec({
    val database = install(ShardedDatabaseExtension())

    transaction(database.ppcdict) {
        SchemaUtils.create(ShardClientIds)
    }

    test("shard is returned for existing clients") {
        transaction(database.ppcdict) {
            ShardClientId.new(ClientID(1)) { shard = 1 }
            ShardClientId.new(ClientID(2)) { shard = 2 }
        }

        database.shard(ClientID(1)) shouldBeSameInstanceAs database.shards[1]
        database.shard(ClientID(2)) shouldBeSameInstanceAs database.shards[2]
    }

    test("a shard should be assigned to an unknown client") {
        database.shard(ClientID(3)).shouldNotBeNull()
    }
})
