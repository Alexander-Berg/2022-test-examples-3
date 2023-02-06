package ru.yandex.market.logistics.mqm.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.entity.lom.LomOrderCombinatorRoute
import ru.yandex.market.logistics.mqm.entity.lom.enums.PointType
import ru.yandex.market.logistics.mqm.entity.lom.enums.ServiceCodeName
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.StartrekProcessorTest

class CombinatorRouteServiceImplTest: StartrekProcessorTest() {

    @Autowired
    private lateinit var service: CombinatorRouteService

    @Test
    @DisplayName("Получение точек партнёра")
    @DatabaseSetup("/service/combinator_route_service/combinator_route.xml")
    fun findPartnerPoints() {
        val actualPoints = service.findPartnerPoints(
            orderId = TEST_ORDER_ID,
            partnerId = TEST_PARTNER_ID,
        )
        val point = LomOrderCombinatorRoute.Point(
            ids = LomOrderCombinatorRoute.PointIds(partnerId = TEST_PARTNER_ID),
            services = listOf(LomOrderCombinatorRoute.DeliveryService(id = 1)),
        )
        actualPoints shouldBe listOf(point)
    }

    @Test
    @DisplayName("Получение пустого списка, если нет точек")
    @DatabaseSetup("/service/combinator_route_service/combinator_route_no_points.xml")
    fun findPartnerPointsReturnEmptyIfNoPoints() {
        val actualPoints = service.findPartnerPoints(
            orderId = TEST_ORDER_ID,
            partnerId = TEST_PARTNER_ID,
        )
        actualPoints shouldHaveSize 0
    }

    @Test
    @DisplayName("Получение пустого списка, если нет точек партнёра нужного партнёра")
    @DatabaseSetup("/service/combinator_route_service/combinator_route_no_points_partner.xml")
    fun findPartnerPointsReturnEmptyIfNoPointsForPartner() {
        val actualPoints = service.findPartnerPoints(
            orderId = TEST_ORDER_ID,
            partnerId = TEST_PARTNER_ID,
        )
        actualPoints shouldHaveSize 0
    }

    @Test
    @DisplayName("Получение сервиса партнёра")
    @DatabaseSetup("/service/combinator_route_service/combinator_route_point_with_service.xml")
    fun findService() {
        service.findService(
            orderId = TEST_ORDER_ID,
            partnerId = TEST_PARTNER_ID,
            pointType = PointType.WAREHOUSE,
            serviceType = ServiceCodeName.INBOUND,
        ) shouldBe LomOrderCombinatorRoute.DeliveryService(
            id = 1,
            code = ServiceCodeName.INBOUND,
        )
    }

    @Test
    @DisplayName("Не получается найти сервис, если нет искомой точки в комбинаторном маршруте")
    @DatabaseSetup("/service/combinator_route_service/combinator_route_no_points.xml")
    fun findServiceReturnErrorIfNoPoint() {
        val exception = assertThrows<IllegalStateException> {
            service.findService(
                orderId = TEST_ORDER_ID,
                partnerId = TEST_PARTNER_ID,
                pointType = PointType.WAREHOUSE,
                serviceType = ServiceCodeName.INBOUND,
            )
        }
        exception.message shouldBe "No point with segmentType=WAREHOUSE in route. order=1 partner=1"
    }

    @Test
    @DisplayName("Не получается найти сервис, если более одной искомой точки в комбинаторном маршруте")
    @DatabaseSetup("/service/combinator_route_service/combinator_route_two_same_points.xml")
    fun findServiceReturnErrorIfMoreThanOnePoint() {
        val exception = assertThrows<IllegalStateException> {
            service.findService(
                orderId = TEST_ORDER_ID,
                partnerId = TEST_PARTNER_ID,
                pointType = PointType.WAREHOUSE,
                serviceType = ServiceCodeName.INBOUND,
            )
        }
        exception.message shouldBe "More than one point with segmentType=WAREHOUSE in route. order=1 partner=1"
    }

    @Test
    @DisplayName("Не получается найти сервис, если нет сервиса в комбинаторном маршруте")
    @DatabaseSetup("/service/combinator_route_service/combinator_route_point_with_service.xml")
    fun findServiceReturnErrorIfNoService() {
        val exception = assertThrows<IllegalStateException> {
            service.findService(
                orderId = TEST_ORDER_ID,
                partnerId = TEST_PARTNER_ID,
                pointType = PointType.WAREHOUSE,
                serviceType = ServiceCodeName.CALL_COURIER,
            )
        }
        exception.message shouldBe "No service with code=CALL_COURIER in point. partner=1 segmentId=101"
    }

    @Test
    @DisplayName("Не получается найти сервис, если более одного сервиса в комбинаторном маршруте")
    @DatabaseSetup("/service/combinator_route_service/combinator_route_point_with_two_same_service.xml")
    fun findServiceReturnErrorIfMoreThanOneService() {
        val exception = assertThrows<IllegalStateException> {
            service.findService(
                orderId = TEST_ORDER_ID,
                partnerId = TEST_PARTNER_ID,
                pointType = PointType.WAREHOUSE,
                serviceType = ServiceCodeName.INBOUND,
            )
        }
        exception.message shouldBe "More than one service with code=INBOUND in point. partner=1 segmentId=101"
    }

    companion object {
        const val TEST_ORDER_ID = 1L
        const val TEST_PARTNER_ID = 1L
    }
}
