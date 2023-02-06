package ru.yandex.market.logistics.mqm.service.processor.planfact.lrm

import ru.yandex.market.logistics.mqm.entity.enums.lrm.LogisticPointType
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ReturnSegmentStatus
import ru.yandex.market.logistics.mqm.entity.lrm.LrmLogisticPointFields
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnBoxEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnSegmentEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmShipmentFields

abstract class AbstractLrmReturnPlanFactTest {
    fun mockLrmReturn(): LrmReturnEntity {
        val lrmReturn = LrmReturnEntity()
        val segment = LrmReturnSegmentEntity()
        lrmReturn.returnSegments = mutableSetOf(segment)
        return lrmReturn
    }

    fun LrmReturnEntity.withSegment(segment : LrmReturnSegmentEntity) : LrmReturnEntity {
        this.returnSegments.add(segment)
        segment.returnEntity = this
        return this
    }

    fun LrmReturnEntity.withBox(box: LrmReturnBoxEntity): LrmReturnEntity {
        this.returnBoxes.add(box)
        box.returnEntity = this
        return this
    }

    fun mockLrmReturnSegment(type: LogisticPointType, status: ReturnSegmentStatus = ReturnSegmentStatus.IN): LrmReturnSegmentEntity {
        return LrmReturnSegmentEntity(
                status = status,
                logisticPoint = LrmLogisticPointFields(
                        type = type
                ),
                shipment = LrmShipmentFields(
                        destination = LrmShipmentFields.Destination(
                                returnSegmentId = 2
                        )
                ))
    }
}
