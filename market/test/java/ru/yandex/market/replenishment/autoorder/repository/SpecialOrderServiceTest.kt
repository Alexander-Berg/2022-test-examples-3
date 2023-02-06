package ru.yandex.market.replenishment.autoorder.repository

import com.google.common.collect.ImmutableList
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.exception.UserWarningException
import ru.yandex.market.replenishment.autoorder.model.ABC
import ru.yandex.market.replenishment.autoorder.model.SpecialOrderDateType
import ru.yandex.market.replenishment.autoorder.model.SpecialOrderDeepmindStatus
import ru.yandex.market.replenishment.autoorder.model.SupplyRouteType
import ru.yandex.market.replenishment.autoorder.model.WarehouseType
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Assortment
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Department
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.SpecialOrder
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.SpecialOrderItem
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Supplier
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.User
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.Warehouse
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.WarehouseRegion
import ru.yandex.market.replenishment.autoorder.service.special_order.SpecialOrderService
import java.time.LocalDate
import java.time.LocalDateTime

class SpecialOrderServiceTest : FunctionalTest() {
    companion object {
        const val USER_LOGIN_CAN_USE_FAST_TRACK = "UserCanUseFastTrack"
        const val USER_LOGIN_CANNOT_USE_FAST_TRACK = "UserCannotUseFastTrack"
        const val USER_LOGIN_NOT_EXISTS = "NotExists1"
        val NOW_DATE_TIME: LocalDateTime = LocalDateTime.of(2022, 1, 24, 12, 23, 1)
        val NOW_DATE: LocalDate = NOW_DATE_TIME.toLocalDate()
    }

    @Autowired
    private lateinit var specialOrderService: SpecialOrderService

    @Before
    fun setMockedDate() {
        setTestTime(NOW_DATE_TIME)
    }

    @Test
    @DbUnitDataSet(before = ["SpecialOrderServiceTest.testUserCanUseFastTrackForSpecialOrder.before.csv"])
    fun testUserCanUseFastTrackForSpecialOrder_Can() {
        assertTrue(specialOrderService.userCanUseFastTrackForSpecialOrder(USER_LOGIN_CAN_USE_FAST_TRACK))
    }

    @Test
    @DbUnitDataSet(before = ["SpecialOrderServiceTest.testUserCanUseFastTrackForSpecialOrder.before.csv"])
    fun testUserCanUseFastTrackForSpecialOrder_Cant() {
        assertFalse(specialOrderService.userCanUseFastTrackForSpecialOrder(USER_LOGIN_CANNOT_USE_FAST_TRACK))
    }

    @Test
    @DbUnitDataSet(before = ["SpecialOrderServiceTest.testUserCanUseFastTrackForSpecialOrder.before.csv"])
    fun testUserCanUseFastTrackForSpecialOrder_NotExists() {
        assertFalse(specialOrderService.userCanUseFastTrackForSpecialOrder(USER_LOGIN_NOT_EXISTS))
    }

    @Test
    @DbUnitDataSet(before = ["SpecialOrderServiceTest.testResponsibleCheck.before.csv"])
    fun testResponsibleCheck_isBadRequest() {
        val specialOrders = getRightSpecialOrder(null, 16L)
        val e = assertThrows<UserWarningException> {
            specialOrderService.addSpecialOrderBySlowPipe("boris", specialOrders)
        }
        assertEquals(
            "Для поставщика 'supplier1', склада 'Маршрут', типа поставки 'Прямая' БЕЗ группы отсутствуют логистические параметры, номер строки: 1",
            e.message
        )
    }

    @Test
    @DbUnitDataSet(before = ["SpecialOrderServiceTest.testResponsibleCheck.before.csv"])
    fun testLogisticParamCheck_isBadRequest() {
        val specialOrders = getRightSpecialOrder(null, 15L)
        val e = assertThrows<UserWarningException> {
            specialOrderService.addSpecialOrderBySlowPipe("boris", specialOrders)
        }
        assertEquals(
            "Для поставщика 'supplier1', склада 'Маршрут', типа поставки 'Прямая' и группы 'ruchkan' отсутствуют логистические параметры, номер строки: 1",
            e.message
        )
    }

    private fun getRightSpecialOrder(specialOrderId: Long?, msku: Long): ImmutableList<SpecialOrder> {
        val supplier = Supplier(1)
        supplier.rsId = "01"
        val warehouse = Warehouse(145, "Маршрут", WarehouseType.FULFILLMENT, WarehouseRegion(145, "Moscow"))
        val user = User(1, "boris")
        val assortment = Assortment(
            msku, "msku_$msku", 1, 1, ABC.A,
            Department(1, "dept_1"), false, null, null, null
        )
        val items = listOf(
            SpecialOrderItem(
                1, NOW_DATE, 10, 1, SpecialOrderDateType.TODAY, null
            )
        )
        return ImmutableList.of(
            SpecialOrder(
                specialOrderId, user, warehouse, supplier, "01.$msku", assortment, SpecialOrder.OrderType.NEW,
                1.123, 1, items, null, 1, SupplyRouteType.DIRECT,
                SpecialOrderDeepmindStatus.PENDING
            )
        )
    }
}
