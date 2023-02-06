package ru.yandex.travel.orders.services.support.jobs

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.travel.hotels.common.orders.*
import ru.yandex.travel.orders.commons.proto.EServiceType
import ru.yandex.travel.orders.entities.*
import ru.yandex.travel.orders.repository.support.SuccessfulHotelOrderNotificationRepository
import ru.yandex.travel.orders.workflow.hotels.proto.EHotelOrderState
import java.time.Instant
import java.time.LocalDate
import java.util.*
import javax.persistence.EntityManager

@RunWith(SpringRunner::class)
@DataJpaTest
@ActiveProfiles("test")
open class SuccessfulHotelOrderNotificationRepositoryTest {
    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var notifications: SuccessfulHotelOrderNotificationRepository

    @Test
    fun testSelectNewConfirmedOrders() {
        createTestOrder("2007-12-03T10:15:30.00Z", EHotelOrderState.OS_CONFIRMED)
        createTestOrder("2007-12-03T12:15:30.00Z", EHotelOrderState.OS_CONFIRMED)
        createTestOrder("2007-12-03T13:15:30.00Z", EHotelOrderState.OS_WAITING_CONFIRMATION)
        val selected = notifications.selectNewConfirmedOrders(Instant.parse("2007-12-03T11:15:30.00Z"))
        Assertions.assertThat(selected.size).isEqualTo(1)
    }

    private fun createTestOrder(date: String, state: EHotelOrderState, orderItemType: EServiceType = EServiceType.PT_EXPEDIA_HOTEL): HotelOrder {
        val order = HotelOrder()
        order.id = UUID.randomUUID()
        order.state = state
        order.addOrderItem(createTestOrderItem(orderItemType))
        em.persist(order)
        em.flush()
        // overriding the default create_at timestamp
        order.createdAt = Instant.parse(date)
        em.merge(order)
        em.flush()
        return order
    }

    private fun createTestOrderItem(orderItemType: EServiceType): HotelOrderItem {
        val orderItem: HotelOrderItem
        val orderDetails = OrderDetails.builder()
                .checkinDate(LocalDate.parse("2020-09-01"))
                .checkoutDate(LocalDate.parse("2020-09-02"))
                .build()
        val itinerary: HotelItinerary
        when (orderItemType) {
            EServiceType.PT_EXPEDIA_HOTEL -> {
                orderItem = ExpediaOrderItem()
                itinerary = ExpediaHotelItinerary()
                itinerary.setOrderDetails(orderDetails)
                orderItem.itinerary = itinerary
            }
            EServiceType.PT_TRAVELLINE_HOTEL -> {
                orderItem = TravellineOrderItem()
                itinerary = TravellineHotelItinerary()
                itinerary.setOrderDetails(orderDetails)
                orderItem.itinerary = itinerary
            }
            EServiceType.PT_BNOVO_HOTEL -> {
                orderItem = BNovoOrderItem()
                itinerary = BNovoHotelItinerary()
                itinerary.setOrderDetails(orderDetails)
                orderItem.itinerary = itinerary
            }
            else -> throw RuntimeException("Unsupported order item type: $orderItemType")
        }
        return orderItem
    }
}
