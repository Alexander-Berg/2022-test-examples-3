package ru.yandex.market.logistics.les.repository

import com.yandex.ydb.table.values.ListValue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.entity.ydb.EventToEntityDao
import ru.yandex.market.logistics.les.repository.ydb.YdbEventToEntityRepository
import ru.yandex.market.logistics.les.repository.ydb.description.EventsToEntitiesTableDescription.Companion.EVENT_ID_IDX
import ru.yandex.market.ydb.integration.query.YdbSelect
import ru.yandex.market.ydb.integration.utils.ListConverter

class YdbEventToEntityRepositoryTest : AbstractContextualTest() {

    @Autowired
    lateinit var repository: YdbEventToEntityRepository

    @Test
    fun loadByEventIds() {
        val eventIds = listOf(1L, 2L)
        repository.loadByEventIds(eventIds, PAGEABLE.pageSize)

        val capturedSelect = captureYdbSelect()
        val param = capturedSelect.params().entries.find { it.key != "\$limit" }!!

        capturedSelect.text() shouldBe SELECT_ALL_QUERY_PREFIX +
            "view $EVENT_ID_IDX as eventsToEntities " +
            "where (eventsToEntities.event_id IN ${param.key}) " +
            "limit \$limit"

        param.value shouldBe ListValue.of(eventIds[0].toUint64(), eventIds[1].toUint64())
        capturedSelect.limit().limit shouldBe PAGEABLE.pageSize
    }

    @Test
    fun loadByEntityIds() {
        val entityIds = listOf(1L, 2L)
        repository.loadByEntityIds(entityIds, PAGEABLE.pageSize)

        val capturedSelect = captureYdbSelect()
        val param = capturedSelect.params().entries.find { it.key != "\$limit" }!!

        capturedSelect.text() shouldBe SELECT_ALL_QUERY_PREFIX +
            "as eventsToEntities " +
            "where (eventsToEntities.entity_id IN ${param.key}) " +
            "limit \$limit"

        param.value shouldBe ListValue.of(entityIds[0].toUint64(), entityIds[1].toUint64())
        capturedSelect.limit().limit shouldBe PAGEABLE.pageSize
    }

    private fun captureYdbSelect() : YdbSelect {
        val captor = argumentCaptor<YdbSelect>()
        verify(ydbTemplate).selectList(captor.capture(), any(), any<ListConverter<EventToEntityDao>>())
        return captor.firstValue
    }

    companion object {
        private const val SELECT_ALL_QUERY_PREFIX = "select " +
            "eventsToEntities.entity_id as eventsToEntities_entity_id, " +
            "eventsToEntities.event_id as eventsToEntities_event_id " +
            "from `null/les/events_to_entities` "

        private val PAGEABLE = PageRequest.of(0, 20)
    }
}
