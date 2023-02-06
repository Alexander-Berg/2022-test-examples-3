package ru.yandex.market.logistics.les.repository

import com.yandex.ydb.table.values.ListValue
import com.yandex.ydb.table.values.PrimitiveValue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.entity.ydb.EventToEntityDao
import ru.yandex.market.logistics.les.repository.ydb.YdbEventRepository
import ru.yandex.market.logistics.les.repository.ydb.description.EventsTableDescription.Companion.EVENT_ID_TIMESTAMP_IDX
import ru.yandex.market.logistics.les.repository.ydb.description.EventsTableDescription.Companion.EVENT_TYPE_TIMESTAMP_IDX
import ru.yandex.market.logistics.les.repository.ydb.description.EventsTableDescription.Companion.SOURCE_EVENT_TYPE_TIMESTAMP_IDX
import ru.yandex.market.logistics.les.repository.ydb.description.EventsTableDescription.Companion.SOURCE_TIMESTAMP_IDX
import ru.yandex.market.logistics.les.repository.ydb.description.EventsTableDescription.Companion.TIMESTAMP_IDX
import ru.yandex.market.ydb.integration.query.YdbQuery.YdbLimitedQuery
import ru.yandex.market.ydb.integration.utils.ListConverter

class YdbEventRepositoryTest : AbstractContextualTest() {

    @Autowired
    lateinit var repository: YdbEventRepository

    @Test
    fun loadWithoutFilters() {
        repository.loadWithoutFilters(PAGEABLE)

        val capturedSelect = captureYdbSelect()

        capturedSelect.text() shouldBe SELECT_ALL_QUERY_PREFIX +
            "view $TIMESTAMP_IDX as events " +
            "order by events.timestamp desc " +
            "limit \$limit"

        capturedSelect.limit().limit shouldBe PAGEABLE.pageSize
    }

    @Test
    fun loadByIds() {
        val searchIds = listOf(1L, 2L)
        repository.loadByIds(searchIds)

        val capturedSelect = captureYdbSelect()
        val param = capturedSelect.params().entries.first()

        capturedSelect.text() shouldBe SELECT_ALL_QUERY_PREFIX +
            "as events where (events.id IN ${param.key})"

        param.value shouldBe ListValue.of(searchIds[0].toUint64(), searchIds[1].toUint64())
    }

    @Test
    fun loadByEventId() {
        val eventId = "event_id_1"
        repository.loadByEventId(eventId, PAGEABLE)

        val capturedSelect = captureYdbSelect()
        val param = capturedSelect.params().entries.find { it.key != "\$limit" }!!

        capturedSelect.text() shouldBe SELECT_ALL_QUERY_PREFIX +
            "view $EVENT_ID_TIMESTAMP_IDX as events " +
            "where (events.event_id = ${param.key}) " +
            "order by events.event_id desc, events.timestamp desc " +
            "limit \$limit"

        capturedSelect.limit().limit shouldBe PAGEABLE.pageSize
        param.value shouldBe PrimitiveValue.utf8(eventId)
    }

    @Test
    fun loadBySourceAndEventType() {
        val source = "src"
        val eventType = "evTyp"
        repository.loadBySourceAndEventType(source, eventType, PAGEABLE)

        val capturedSelect = captureYdbSelect()
        val params = capturedSelect.params().entries
        val sourceParam = params.find { it.key.contains("source") }!!
        val eventTypeParam = params.find { it.key.contains("event_type") }!!

        capturedSelect.text() shouldBe SELECT_ALL_QUERY_PREFIX +
            "view $SOURCE_EVENT_TYPE_TIMESTAMP_IDX as events " +
            "where ((events.source = ${sourceParam.key}) and " +
            "(events.event_type = ${eventTypeParam.key})) " +
            "order by events.source desc, events.event_type desc, events.timestamp desc " +
            "limit \$limit"

        capturedSelect.limit().limit shouldBe PAGEABLE.pageSize
        sourceParam.value shouldBe PrimitiveValue.utf8(source)
        eventTypeParam.value shouldBe PrimitiveValue.utf8(eventType)
    }

    @Test
    fun loadBySource() {
        val source = "src"
        repository.loadBySource(source, PAGEABLE)

        val capturedSelect = captureYdbSelect()
        val param = capturedSelect.params().entries.find { it.key.contains("source") }!!

        capturedSelect.text() shouldBe SELECT_ALL_QUERY_PREFIX +
            "view $SOURCE_TIMESTAMP_IDX as events " +
            "where (events.source = ${param.key}) " +
            "order by events.source desc, events.timestamp desc " +
            "limit \$limit"

        capturedSelect.limit().limit shouldBe PAGEABLE.pageSize
        param.value shouldBe PrimitiveValue.utf8(source)
    }

    @Test
    fun loadByEventType() {
        val eventType = "evTyp"
        repository.loadByEventType(eventType, PAGEABLE)

        val capturedSelect = captureYdbSelect()
        val param = capturedSelect.params().entries.find { it.key.contains("event_type") }!!

        capturedSelect.text() shouldBe SELECT_ALL_QUERY_PREFIX +
            "view $EVENT_TYPE_TIMESTAMP_IDX as events " +
            "where (events.event_type = ${param.key}) " +
            "order by events.event_type desc, " +
            "events.timestamp desc " +
            "limit \$limit"

        capturedSelect.limit().limit shouldBe PAGEABLE.pageSize
        param.value shouldBe PrimitiveValue.utf8(eventType)
    }

    private fun captureYdbSelect() : YdbLimitedQuery {
        val captor = argumentCaptor<YdbLimitedQuery>()
        verify(ydbTemplate).selectList(captor.capture(), any(), any<ListConverter<EventToEntityDao>>())
        return captor.firstValue
    }

    companion object {
        private const val SELECT_ALL_QUERY_PREFIX = "select " +
            "events.description, " +
            "events.event_id, " +
            "events.event_type, " +
            "events.id, " +
            "events.payload, " +
            "events.source, " +
            "events.timestamp " +
            "from `null/les/events` "

        private val PAGEABLE = PageRequest.of(0, 20)
    }
}
