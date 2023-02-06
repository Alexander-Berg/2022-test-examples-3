package ru.yandex.market.logistics.les.admin.controller

import java.time.Instant
import org.apache.commons.codec.digest.DigestUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.Sort
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.base.EntityType
import ru.yandex.market.logistics.les.entity.ydb.OrderEventDao
import ru.yandex.market.logistics.les.repository.ydb.YdbOrderEventRepository
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent

@DisplayName("Получение информации о событиях")
class EventControllerTest : AbstractContextualTest() {

    @MockBean
    private lateinit var repository: YdbOrderEventRepository

    @DisplayName("Получение всех событий")
    @Test
    fun getAllEvents() {
        whenever(
            repository.loadByOrderIdOrSourcePaged(
                lastId = 0,
                pageSize = PageSettings.PAGE_SIZE,
                orderBy = "timestamp",
                direction = Sort.Direction.DESC.name
            )
        )
            .thenReturn(
                listOf(
                    createFirstEvent(),
                    createSecondEvent()
                )
            )

        mockGetRequest(content = "admin/event/get_events.json")
    }

    @DisplayName("Получение всех событий с сортировкой по timestamp")
    @Test
    fun getAllEventsSortedByTimestamp() {
        whenever(
            repository.loadByOrderIdOrSourcePaged(
                lastId = 0,
                pageSize = PageSettings.PAGE_SIZE,
                orderBy = "timestamp",
                direction = Sort.Direction.DESC.name
            )
        )
            .thenReturn(
                listOf(
                    createSecondEvent(),
                    createFirstEvent()
                )
            )

        mockGetRequest(content = "admin/event/get_events_sorted.json", byTimestamp = true)
    }

    @DisplayName("Получение событий по orderId")
    @Test
    fun getEventsByOrderId() {
        whenever(
            repository.loadByOrderIdOrSourcePaged(
                entityId = "49784",
                entityType = EntityType.ORDER.name,
                lastId = 0,
                pageSize = PageSettings.PAGE_SIZE,
                orderBy = "timestamp",
                direction = Sort.Direction.DESC.name
            )
        )
            .thenReturn(listOf(createFirstEvent()))

        mockGetRequest(
            entityId = "49784",
            content = "admin/event/get_by_order_id.json"
        )
    }

    @DisplayName("Получение событий по источнику")
    @Test
    fun getEventsBySource() {
        whenever(
            repository.loadByOrderIdOrSourcePaged(
                source = "sc",
                lastId = 0,
                pageSize = PageSettings.PAGE_SIZE,
                orderBy = "timestamp",
                direction = Sort.Direction.DESC.name
            )
        )
            .thenReturn(listOf(createFirstEvent()))

        mockGetRequest(
            source = "sc",
            content = "admin/event/get_by_order_id.json"
        )
    }

    @DisplayName("Получение события по одному заказу и источнику")
    @Test
    fun getSingleOrderEventByOrderIdAndSource() {
        whenever(
            repository.loadByOrderIdOrSourcePaged(
                entityId = "49784",
                entityType = EntityType.ORDER.name,
                source = "sc",
                lastId = 0,
                pageSize = PageSettings.PAGE_SIZE,
                orderBy = "timestamp",
                direction = Sort.Direction.DESC.name
            )
        )
            .thenReturn(listOf(createFirstEvent()))

        mockGetRequest(
            entityId = "49784",
            source = "sc",
            content = "admin/event/get_by_order_id.json"
        )
    }

    @DisplayName("Получение события не по заказу")
    @Test
    fun getSingleOrderEventByNotOrder() {
        whenever(
            repository.loadByOrderIdOrSourcePaged(
                entityId = "TSLA10",
                entityType = EntityType.VEHICLE.name,
                lastId = 0,
                pageSize = PageSettings.PAGE_SIZE,
                orderBy = "timestamp",
                direction = Sort.Direction.DESC.name
            )
        )
            .thenReturn(listOf(createThirdEvent()))

        mockGetRequest(
            entityId = "TSLA10",
            entityType = EntityType.VEHICLE,
            content = "admin/event/get_not_order_event_by_id.json"
        )
    }

    private fun createFirstEvent(): OrderEventDao {
        val eventId = "138791190"
        val orderId = "49784"
        val source = "sc"
        val eventType = "ORDER_READY_TO_BE_SEND_TO_SO_FF"
        val id = DigestUtils.sha256Hex(eventId + orderId + eventType + source)
        return OrderEventDao(
            id = id,
            eventId = eventId,
            eventType = eventType,
            source = source,
            entityId = orderId,
            entityType = EntityType.ORDER.name,
            timestamp = Instant.parse("2021-10-07T23:42:00.00Z"),
            description = "Изменился статус заказа в Курьерской платформе"
        )
    }

    private fun createSecondEvent(): OrderEventDao {
        val eventId = "138791190"
        val orderId = "67763780"
        val source = "sc"
        val eventType = "ORDER_READY_TO_BE_SEND_TO_SO_FF"
        val id = DigestUtils.sha256Hex(eventId + orderId + eventType + source)
        return OrderEventDao(
            id = id,
            eventId = eventId,
            eventType = eventType,
            source = source,
            entityId = orderId,
            entityType = EntityType.ORDER.name,
            timestamp = Instant.parse("2021-10-07T23:50:00.00Z"),
            description = "Изменился статус заказа в Курьерской платформе"
        )
    }

    private fun createThirdEvent(): OrderEventDao {
        val eventId = "6537364"
        val entityId = "TSLA10"
        val entityType = EntityType.VEHICLE.name
        val source = "courier"
        val eventType = "VEHICLE_FLEW_INTO_SPACE"
        val id = DigestUtils.sha256Hex(eventId + entityId + eventType + source)
        return OrderEventDao(
            id = id,
            eventId = eventId,
            eventType = eventType,
            source = source,
            entityId = entityId,
            entityType = entityType,
            timestamp = Instant.parse("2018-02-06T23:45:00.00Z"),
            description = "Улетела и улетела"
        )
    }

    private fun mockGetRequest(
        entityId: String? = null,
        entityType: EntityType? = null,
        source: String? = null,
        content: String,
        byTimestamp: Boolean = false
    ) {
        val rb = get("/admin/events")
            .param("page", "0")
            .param("size", PageSettings.PAGE_SIZE.toString())
        entityId?.let { rb.param("entityId", it) }
        entityType?.let { rb.param("entityType", it.name) }
        source?.let { rb.param("source", it) }
        if (byTimestamp) {
            rb.param("sort", "timestamp,desc")
        }

        mockMvc.perform(rb)
            .andExpect(status().isOk)
            .andExpect(jsonContent(content, false))
    }
}
