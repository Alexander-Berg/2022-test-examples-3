package ru.yandex.market.logistics.mqm.repository

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.converter.lrm.toFBYClaimInfo
import ru.yandex.market.logistics.mqm.entity.enums.DestinationPointType
import ru.yandex.market.logistics.mqm.entity.enums.lrm.LrmReturnReasonType
import ru.yandex.market.logistics.mqm.entity.enums.lrm.LrmReturnSubreason
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ReturnSource
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ReturnStatus
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnBoxEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnDestinationPointFields
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnItemEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnPickupPointFields
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnSegmentEntity
import java.time.LocalDateTime
import java.time.ZoneOffset

class LrmReturnRepositoryTest : AbstractContextualTest() {
    @Autowired
    private lateinit var repository: LrmReturnRepository

    @Test
    @DatabaseSetup("/repository/return/before/before_save.xml")
    @ExpectedDatabase(
        "/repository/return/after/after_save.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun save() {
        val entity = LrmReturnEntity(
            id = 1,
            lrmReturnId = 1,
            orderExternalId  = "1",
            source = ReturnSource.PICKUP_POINT,
            externalId = "1",
            logisticPointFromId = 1,
            pickupPoint = LrmReturnPickupPointFields(
                partnerId = 1,
                externalId = "1",
            ),
            committed = LocalDateTime.of(2021, 5, 11, 3, 40, 51).toInstant(ZoneOffset.UTC),
            destinationPoint = LrmReturnDestinationPointFields(
                type = DestinationPointType.SHOP,
                partnerId = 1,
                shopId = 1
            ),
            status = ReturnStatus.CREATED
        )
        val item =  LrmReturnItemEntity(
            supplierId = 1,
            vendorCode = "vendorCode",
            instances = mapOf("kek" to "lol"),
            returnReason = "returnReason",
            returnSubreason = LrmReturnSubreason.BAD_PACKAGE,
            returnReasonType = LrmReturnReasonType.BAD_QUALITY,
            externalBoxId = "extBox"
        )
        val box = LrmReturnBoxEntity(
            externalId = "box"
        )
        val segment = LrmReturnSegmentEntity(lrmReturnId = 1)
        entity.returnItems = mutableSetOf(item)
        entity.returnBoxes = mutableSetOf(box)
        entity.returnSegments = mutableSetOf(segment)
        repository.save(entity)
    }

    @Test
    @DatabaseSetup("/repository/return/returns.xml")
    fun getOutDatedReturns() {
        val returns =
            repository.findAllOutdatedFbyReturns(listOf(ReturnStatus.CREATED, ReturnStatus.IN_TRANSIT))
        val fbyClaimInfo = returns.map { it.toFBYClaimInfo() }.toList()

        assert(fbyClaimInfo.size == 1)
    }
}
