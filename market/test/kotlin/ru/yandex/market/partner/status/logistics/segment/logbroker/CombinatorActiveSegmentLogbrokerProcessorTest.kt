package ru.yandex.market.partner.status.logistics.segment.logbroker

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.logistics.management.entity.logbroker.BusinessWarehouseSnapshotDto
import ru.yandex.market.logistics.management.entity.logbroker.EntityType
import ru.yandex.market.logistics.management.entity.logbroker.EventDto
import ru.yandex.market.logistics.management.entity.type.ExtendedShipmentType
import ru.yandex.market.logistics.management.entity.type.PartnerType
import ru.yandex.market.partner.status.AbstractFunctionalTest
import java.time.Instant

/**
 * Тесты для [CombinatorActiveSegmentLogbrokerProcessor].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class CombinatorActiveSegmentLogbrokerProcessorTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var combinatorActiveSegmentLogbrokerProcessor: CombinatorActiveSegmentLogbrokerProcessor

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DbUnitDataSet(after = ["CombinatorActiveSegmentLogbrokerProcessorTest/empty.after.csv"])
    fun `different type, skip it`() {
        val event = EventDto().apply {
            entityType = EntityType.PARTNER_CHANGE
            eventTimestamp = Instant.now()
            entitySnapshot = objectMapper.nullNode()
        }
        combinatorActiveSegmentLogbrokerProcessor.process(event.toMessage())
    }

    @Test
    @DbUnitDataSet(after = ["CombinatorActiveSegmentLogbrokerProcessorTest/empty.after.csv"])
    fun `warehouse without shipment type`() {
        val event = mockEvent {
            partnerId = 100L
            partnerType = PartnerType.DELIVERY
        }
        combinatorActiveSegmentLogbrokerProcessor.process(event.toMessage())
    }

    @Test
    @DbUnitDataSet(
        before = ["CombinatorActiveSegmentLogbrokerProcessorTest/outdated.before.csv"],
        after = ["CombinatorActiveSegmentLogbrokerProcessorTest/outdated.after.csv"]
    )
    fun `delete warehouse without shipment type`() {
        val event = mockEvent {
            partnerId = 100L
            partnerType = PartnerType.DELIVERY
        }
        combinatorActiveSegmentLogbrokerProcessor.process(event.toMessage())
    }

    @Test
    @DbUnitDataSet(
        after = ["CombinatorActiveSegmentLogbrokerProcessorTest/new.after.csv"]
    )
    fun `new warehouse with shipment type`() {
        val event = mockEvent {
            partnerId = 100L
            partnerType = PartnerType.DELIVERY
            shipmentType = ExtendedShipmentType.WITHDRAW
        }
        combinatorActiveSegmentLogbrokerProcessor.process(event.toMessage())
    }

    @Test
    @DbUnitDataSet(
        before = ["CombinatorActiveSegmentLogbrokerProcessorTest/update.before.csv"],
        after = ["CombinatorActiveSegmentLogbrokerProcessorTest/update.after.csv"]
    )
    fun `update warehouse with shipment type`() {
        val event = mockEvent {
            partnerId = 100L
            partnerType = PartnerType.DELIVERY
            shipmentType = ExtendedShipmentType.WITHDRAW
        }
        combinatorActiveSegmentLogbrokerProcessor.process(event.toMessage())
    }

    private fun mockEvent(builder: BusinessWarehouseSnapshotDto.() -> Unit): EventDto {
        val warehouse = BusinessWarehouseSnapshotDto().apply(builder)
        return EventDto().apply {
            entityType = EntityType.BUSINESS_WAREHOUSE
            eventTimestamp = Instant.now()
            entitySnapshot = warehouse.toSnapshot()
        }
    }

    private fun EventDto.toMessage(): MessageBatch =
        MessageBatch("", 0, listOf(MessageData(objectMapper.writeValueAsBytes(this), 0, null)))

    private fun BusinessWarehouseSnapshotDto.toSnapshot(): JsonNode =
        objectMapper.readTree(objectMapper.writeValueAsString(this))
}
