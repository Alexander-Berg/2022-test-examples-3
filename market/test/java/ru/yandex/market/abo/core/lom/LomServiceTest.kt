package ru.yandex.market.abo.core.lom

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.util.kotlin.toLocalDateTime
import ru.yandex.market.logistics.lom.client.LomClient
import ru.yandex.market.logistics.lom.model.dto.OrderDto
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto
import ru.yandex.market.logistics.lom.model.enums.PartnerType.DELIVERY
import ru.yandex.market.logistics.lom.model.enums.SegmentType.COURIER
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter
import ru.yandex.market.logistics.lom.model.page.PageResult
import ru.yandex.market.logistics.lom.model.search.Pageable
import java.time.Instant

/**
 * @author zilzilok
 */
class LomServiceTest {
    private val lomClient = mock(LomClient::class.java)
    private val lomService = LomService(lomClient)

    @Test
    fun loadOrderCreationTimeTest() {
        val created = Instant.now()
        val callCourierTime = created.plusSeconds(30)

        whenever(lomClient.searchOrders(buildFilter(), Pageable.unpaged())).thenReturn(
            buildOrder(created, callCourierTime)
        )
        val orderInfo = lomService.loadLomOrderInfo(ORDER_ID, SUPPLIER_ID)

        assertNotNull(orderInfo)
        assertEquals(orderInfo?.lomEstimatedDateTime, callCourierTime.toLocalDateTime())
        assertEquals(orderInfo?.orderCreationDateTime, created.toLocalDateTime())
    }

    private fun buildOrder(created: Instant, callCourierTime: Instant) = PageResult.of(
        listOf(
            OrderDto()
                .setCreated(created)
                .setWaybill(
                    listOf(
                        WaybillSegmentDto.builder()
                            .segmentType(COURIER)
                            .partnerType(DELIVERY)
                            .partnerId(GO_DELIVERY_SERVICE_ID)
                            .callCourierTime(callCourierTime)
                            .build()
                    )
                )
        ), 1, 0, 1
    )

    private fun buildFilter() = OrderSearchFilter.builder()
        .externalIds(setOf(ORDER_ID.toString()))
        .senderIds(setOf(SUPPLIER_ID))
        .build()

    companion object {
        private const val ORDER_ID = 1L
        private const val SUPPLIER_ID = 1L
    }
}

