package ru.yandex.market.logistics.mqm.repository

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.logistics.lom.model.enums.CargoType
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.lom.CancellationOrderRequest
import ru.yandex.market.logistics.mqm.entity.lom.LomItem
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.DeliveryInterval
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.CancellationOrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.DeliveryType
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderTag
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.LocalDate

class LomOrderRepositoryTest : AbstractContextualTest() {

    @Autowired
    lateinit var transactionTemplate: TransactionOperations

    @Autowired
    private lateinit var lomOrderRepository: LomOrderRepository

    @Test
    @DatabaseSetup("/repository/lomOrder/find_order_by_id.xml")
    fun findWithEntitiesById() {
        createOrder { testOrder ->
            transactionTemplate.execute {
                val readOrder = lomOrderRepository.findWithEntitiesById(1)!!
                assertSoftly {
                    readOrder shouldBe testOrder
                    readOrder.waybill shouldContainExactly testOrder.waybill
                    readOrder.cancellationOrderRequests shouldContainExactlyInAnyOrder testOrder.cancellationOrderRequests
                }
            }
        }
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/lomOrder/save_order.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun saveEntities() {
        createOrder { order -> transactionTemplate.execute { lomOrderRepository.save(order) } }
    }

    private fun createOrder(assertion: (LomOrder) -> Unit) {
        val testOrder = joinInOrder(
            listOf(
                WaybillSegment(
                    id = 1,
                    partnerId = 101,
                    partnerType = PartnerType.FULFILLMENT,
                    segmentType = SegmentType.FULFILLMENT,
                    shipment = WaybillShipment(date = LocalDate.of(2021, 5, 11)),
                ),
                WaybillSegment(
                    id = 2,
                    partnerId = 102,
                    partnerType = PartnerType.DELIVERY,
                    segmentType = SegmentType.COURIER,
                    shipment = WaybillShipment(date = LocalDate.of(2021, 5, 11)),
                ),
            )
        ).apply {
            id = 1
            deliveryType = DeliveryType.MOVEMENT
            deliveryInterval = DeliveryInterval(
                deliveryDateMin = LocalDate.of(2022, 6, 17),
                deliveryDateMax = LocalDate.of(2022, 6, 17)
            )
            status = OrderStatus.PROCESSING
            senderEmails = listOf()
            items = listOf(
                LomItem(
                    name = "name",
                    categoryName = "categoryName",
                    cargoTypes = setOf(CargoType.CIS_REQUIRED),
                    instances = listOf(mapOf(Pair("cis", "123")))
                )
            )
            units = listOf()
            orderTags = setOf(OrderTag.B2B_CUSTOMER, OrderTag.DELAYED_RDD_NOTIFICATION)
        }

        testOrder.cancellationOrderRequests = mutableSetOf(
            CancellationOrderRequest(
                id = 1,
                status = CancellationOrderStatus.REQUIRED_SEGMENT_SUCCESS,
                cancellationOrderReason = "DELIVERY_SERVICE_UNDELIVERED",
            ).apply { order = testOrder },
            CancellationOrderRequest(
                id = 2,
                status = CancellationOrderStatus.SUCCESS,
                cancellationOrderReason = "DELIVERY_SERVICE_UNDELIVERED",
            ).apply { order = testOrder },
        )
        assertion.invoke(testOrder)
    }

}
