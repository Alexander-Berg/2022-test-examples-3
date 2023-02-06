package ru.yandex.market.mbi.orderservice.tms.service.logbroker.events.mappers

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.helpers.loadTestEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLogisticsEntity
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest

class EventMappersTest : FunctionalTest() {

    @Autowired
    lateinit var checkouterAnnotationObjectMapper: ObjectMapper

    @Test
    fun `FBS single item event`() {
        val event = EventMappersTest::class.loadTestEntity<OrderHistoryEvent>(
            "fbs-event-1.json",
            checkouterAnnotationObjectMapper
        )

        val result = event.toEventEntities()

        assertThat(result).satisfies {
            assertThat(it.orderEntities).hasSize(1)
                .usingElementComparatorIgnoringFields("updatedAt")
                .containsAll(
                    EventMappersTest::class.loadTestEntities<OrderEntity>("expected/fbs-event-1-order-entities.json")
                )
            assertThat(it.lineEntities).hasSize(1)
                .satisfies { line -> assertThat(line[0].orderLinePromo).isNotNull.hasSize(2) }
                .usingElementComparatorIgnoringFields("orderLinePromo")
                .containsAll(
                    EventMappersTest::class.loadTestEntities<OrderLineEntity>("expected/fbs-event-1-line-entities.json")
                )
            assertThat(it.logisticsEntities).hasSize(1)
                .containsAll(
                    EventMappersTest::class.loadTestEntities<OrderLogisticsEntity>("expected/fbs-event-1-logistic-entities.json")
                )
        }
    }

    @Test
    fun `FBY multiple merchant items event`() {
        // FBY-заказ с товарами трех различных мерчантов
        val event = EventMappersTest::class.loadTestEntity<OrderHistoryEvent>(
            "fby-event-1.json",
            checkouterAnnotationObjectMapper
        )

        val result = event.toEventEntities()

        assertThat(result).satisfies {
            assertThat(it.orderEntities).hasSize(3)
                .usingElementComparatorIgnoringFields("updatedAt")
                .containsAll(
                    EventMappersTest::class.loadTestEntities<OrderEntity>("expected/fby-event-1-order-entities.json")
                )
            assertThat(it.lineEntities).hasSize(4)
                .allSatisfy { line -> assertThat(line.orderLinePromo).isNotNull }
                .usingElementComparatorIgnoringFields("orderLinePromo")
                .containsAll(
                    EventMappersTest::class.loadTestEntities<OrderLineEntity>("expected/fby-event-1-line-entities.json")
                )
            assertThat(it.logisticsEntities).hasSize(3)
                .containsAll(
                    EventMappersTest::class.loadTestEntities<OrderLogisticsEntity>("expected/fby-event-1-logistic-entities.json")
                )
        }
    }

    @Test
    fun `DBS order with single item`() {
        val event = EventMappersTest::class.loadTestEntity<OrderHistoryEvent>(
            "dbs-event-1.json",
            checkouterAnnotationObjectMapper
        )

        val result = event.toEventEntities()

        assertThat(result).satisfies {
            assertThat(it.orderEntities).hasSize(1)
                .usingElementComparatorIgnoringFields("updatedAt")
                .containsAll(
                    EventMappersTest::class.loadTestEntities<OrderEntity>("expected/dbs-event-1-order-entities.json")
                )
            assertThat(it.lineEntities).hasSize(1)
                .satisfies { line -> assertThat(line[0].orderLinePromo).isNotNull.hasSize(2) }
                .usingElementComparatorIgnoringFields("orderLinePromo")
                .containsAll(
                    EventMappersTest::class.loadTestEntities<OrderLineEntity>("expected/dbs-event-1-line-entities.json")
                )
            assertThat(it.logisticsEntities).hasSize(1)
                .containsAll(
                    EventMappersTest::class.loadTestEntities<OrderLogisticsEntity>("expected/dbs-event-1-logistic-entities.json")
                )
        }
    }
}
