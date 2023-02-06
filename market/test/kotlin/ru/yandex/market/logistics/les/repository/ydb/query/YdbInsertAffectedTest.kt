package ru.yandex.market.logistics.les.repository.ydb.query

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.les.repository.ydb.description.EventsToEntitiesTableDescription
import ru.yandex.market.ydb.integration.query.YdbModificationQuery

class YdbInsertAffectedTest {

    var table = EventsToEntitiesTableDescription()

    @BeforeEach
    fun before() {
        table = EventsToEntitiesTableDescription()
    }

    @Test
    fun insert() {
        assert(YdbInsertAffected.insert(table, table.eventId, table.entityId).row(1L, 2L).toQuery(), "INSERT")
    }

    @Test
    fun upsert() {
        assert(YdbInsertAffected.upsert(table, table.eventId, table.entityId).row(1L, 2L).toQuery(), "UPSERT")
    }

    @Test
    fun replace() {
        assert(YdbInsertAffected.replace(table, table.eventId, table.entityId).row(1L, 2L).toQuery(), "REPLACE")
    }

    private fun assert(qb: YdbModificationQuery, type: String) {
        assertThat(qb.text()).`as`("$type string is OK").isEqualTo(getQuery(type))
        assertThat(qb.params()).`as`("$type params is OK").hasSize(1)
        assertThat(qb.params().keys).`as`("$type params is OK").contains("\$batch_les_events_to_entities_0")
    }

    private fun getQuery(type: String): String = """
        ${"$"}query = (
            SELECT v.`event_id` AS `event_id`, v.`entity_id` AS `entity_id`
            FROM AS_TABLE(${"$"}batch_les_events_to_entities_0) AS v
            LEFT JOIN `null` AS t ON v.event_id = t.event_id AND v.entity_id = t.entity_id
            WHERE t.event_id IS NULL AND t.entity_id IS NULL
        );
        $type INTO `null` SELECT * FROM ${"$"}query;
        SELECT COUNT(*) FROM ${"$"}query;
    """.trimIndent()
}
