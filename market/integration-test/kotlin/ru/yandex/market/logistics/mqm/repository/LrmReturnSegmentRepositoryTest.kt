package ru.yandex.market.logistics.mqm.repository

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.enums.lrm.LogisticPointType
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ReturnSegmentStatus
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ShipmentDestinationType
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ShipmentRecipientType
import ru.yandex.market.logistics.mqm.entity.lrm.LrmLogisticPointFields
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnSegmentEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmShipmentFields

class LrmReturnSegmentRepositoryTest : AbstractContextualTest() {
    @Autowired
    private lateinit var repository: LrmReturnSegmentRepository

    @Test
    @DatabaseSetup("/repository/return-segment/before/before_save.xml")
    @ExpectedDatabase(
        "/repository/return-segment/after/after_save.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun save() {
        val returnSegment = LrmReturnSegmentEntity(
            id = 1,
            lrmSegmentId = 1,
            lrmReturnId = 1,
            status = ReturnSegmentStatus.CREATED,
            shipment = LrmShipmentFields(
                destination = LrmShipmentFields.Destination(
                    type = ShipmentDestinationType.SORTING_CENTER,
                    partnerId = 20,
                    logisticPointId = 21,
                    name = "22",
                    returnSegmentId = 23
                ),
                recipient = LrmShipmentFields.Recipient(
                    type = ShipmentRecipientType.DELIVERY_SERVICE,
                    partnerId = 33,
                    name = "ricnorr",
                    courier = LrmShipmentFields.Courier(
                        id = 1,
                        uid = 2,
                        name = "okhttp",
                        carNumber = "car",
                        carDescription = "car?",
                        phoneNumber = "13"
                    )
                )
            ),
            logisticPoint = LrmLogisticPointFields(
                partnerId = 10,
                logisticPointId = 11,
                logisticPointExternalId = "13",
                type = LogisticPointType.DROPOFF
            )
        )
        repository.save(returnSegment)
    }

    @Test
    @DatabaseSetup("/repository/return-segment/before/before_find.xml")
    fun find() {
        val segment = repository.findByExternalBoxIdAndLogisticPointType("ext", LogisticPointType.SORTING_CENTER).first()
        assertSoftly {
            segment.id shouldBe 1
            segment.returnEntity!!.id shouldBe 666
        }
    }

    @Test
    @DatabaseSetup("/repository/return-segment/before/before_find.xml")
    fun findByLrmReturnId() {
        val segments = repository.findAllByLrmReturnId(1)
        assertSoftly {
            segments.size shouldBe 1
            segments[0].id shouldBe 1
        }
    }
}
