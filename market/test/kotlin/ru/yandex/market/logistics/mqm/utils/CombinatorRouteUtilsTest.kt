package ru.yandex.market.logistics.mqm.utils

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.yandex.market.logistics.lom.model.dto.CombinatorPointNode
import ru.yandex.market.logistics.lom.model.dto.CombinatorRoute
import ru.yandex.market.logistics.lom.model.utils.CombinatorRouteUtils
import ru.yandex.market.logistics.mqm.entity.lom.LomOrderCombinatorRoute
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PointType

class CombinatorRouteUtilsTest {

    @Test
    @DisplayName("Успешное создание смещений")
    fun successfulBuildOffsets() {
        val values = calculateShipmentDateOffsets(mockCombinatorRoute())
        val lomValues = calculateShipmentDateOffsetDays(mockCombinatorRouteFromLom())
        values shouldBe lomValues
    }

    @Test
    @DisplayName("Успешное нахождение следующей точки")
    fun getNextPointSuccess() {
        val combinatorRoute = mockCombinatorRoute().route!!
        assertSoftly {
            combinatorRoute.getNextPoint(1).segmentId shouldBe 2
            combinatorRoute.getNextPoint(2).segmentId shouldBe 3
        }
    }

    @Test
    @DisplayName("Попытка получить следующую после последней точку")
    fun getNextPointOnLastPoint() {
        val combinatorRoute = mockCombinatorRoute().route!!
        val exception = assertThrows<NoSuchElementException> { combinatorRoute.getNextPoint(3) }
        exception.message shouldBe "There is no next segment"
    }

    @Test
    @DisplayName("Попытка получить следующую точку с несуществующим в маршруте segmentId")
    fun getNextPointWithUnknownSegmentId() {
        val combinatorRoute = mockCombinatorRoute().route!!
        val exception = assertThrows<IllegalArgumentException> { combinatorRoute.getNextPoint(54321) }
        exception.message shouldBe "Can not find point with segmentId = 54321"
    }

    @Test
    @DisplayName("Успешная попытка получить основную точку по сегменту ЛОМа")
    fun getMainPointSuccess() {
        val combinatorRoute = mockCombinatorRoute().route!!
        val waybillSegment = WaybillSegment(combinatorSegmentIds = listOf(2, 3))
        val mainPoint = combinatorRoute.getMainPoint(waybillSegment)
        assertSoftly {
            mainPoint.segmentId shouldBe 3
            mainPoint.segmentType shouldBe PointType.WAREHOUSE
        }
    }

    private fun calculateShipmentDateOffsetDays(route: CombinatorRoute): Map<Long, Int> {
        val combinatorPointNode = CombinatorRouteUtils.getPointTree(route)
        val shipmentDateOffsetDays = mutableMapOf<Long, Int>()
        collectShipmentDateOffsetDays(combinatorPointNode, shipmentDateOffsetDays)
        return shipmentDateOffsetDays
    }

    private fun collectShipmentDateOffsetDays(
        combinatorPointNode: CombinatorPointNode,
        shipmentDateOffsetDays: MutableMap<Long, Int>,
    ) {
        shipmentDateOffsetDays[combinatorPointNode.point.segmentId] =
            combinatorPointNode.point.shipmentDateOffsetDays ?: 0
        combinatorPointNode.children.forEach { collectShipmentDateOffsetDays(it, shipmentDateOffsetDays) }
    }

    private fun mockCombinatorRoute() =
        LomOrderCombinatorRoute(
            route = LomOrderCombinatorRoute.DeliveryRoute(
                paths = listOf(
                    LomOrderCombinatorRoute.Path(pointFrom = 0, pointTo = 1),
                    LomOrderCombinatorRoute.Path(pointFrom = 1, pointTo = 2),
                ),
                points = listOf(
                    LomOrderCombinatorRoute.Point(segmentId = 1, segmentType = PointType.WAREHOUSE),
                    LomOrderCombinatorRoute.Point(segmentId = 2, segmentType = PointType.MOVEMENT),
                    LomOrderCombinatorRoute.Point(segmentId = 3, segmentType = PointType.WAREHOUSE),
                ),
                shipmentDateOffsets = listOf(
                    LomOrderCombinatorRoute.ShipmentDateOffset(offsetDays = -1, pointIndex = 2)
                )
            )
        )

    private fun mockCombinatorRouteFromLom() =
        CombinatorRoute()
            .setRoute(
                CombinatorRoute.DeliveryRoute()
                    .setPaths(
                        listOf(
                            CombinatorRoute.Path().setPointFrom(0).setPointTo(1),
                            CombinatorRoute.Path().setPointFrom(1).setPointTo(2),
                        )
                    )
                    .setPoints(
                        listOf(
                            CombinatorRoute.Point().setSegmentId(1),
                            CombinatorRoute.Point().setSegmentId(2),
                            CombinatorRoute.Point().setSegmentId(3),
                        )
                    )
                    .setShipmentDateOffsets(
                        listOf(
                            CombinatorRoute.ShipmentDateOffset().setOffsetDays(-1).setPointIndex(2)
                        )
                    )
            )
}
