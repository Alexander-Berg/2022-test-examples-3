package ru.yandex.market.billing.imports.orderreturn.dao

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.FunctionalTest
import ru.yandex.market.billing.imports.orderreturn.model.OrderReturnTrantime
import ru.yandex.market.common.test.db.DbUnitDataSet
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Тесты для [OrderReturnTrantimeDao].
 */
internal class OrderReturnTrantimeDaoTest : FunctionalTest() {
    @Autowired
    private lateinit var orderReturnTrantimeDao: OrderReturnTrantimeDao

    @Test
    @DisplayName("Вставка нового трантайма")
    @DbUnitDataSet(after = ["OrderReturnTrantimeDaoTest.testInsertTrantimeIfNotExists.after.csv"])
    fun testInsertTrantimeIfNotExists() {
        orderReturnTrantimeDao.insertTrantimeIfNotExists(listOf(
            OrderReturnTrantime(123L, OffsetDateTime.parse("2021-11-20T12:00:00+03").toInstant())
        ))
    }

    @Test
    @DisplayName("Вставка нового трантайма + уже есть существующий")
    @DbUnitDataSet(
        before = ["OrderReturnTrantimeDaoTest.testInsertTrantimeIfNotExists.after.csv"],
        after = ["OrderReturnTrantimeDaoTest.testInsertTrantimeIfNotExistsWithExists.after.csv"]
    )
    fun testInsertTrantimeIfNotExistsWithExists() {
        orderReturnTrantimeDao.insertTrantimeIfNotExists(listOf(
            OrderReturnTrantime(123L, OffsetDateTime.parse("2021-11-20T12:00:00+03").toInstant()),
            OrderReturnTrantime(456L, OffsetDateTime.parse("2021-11-21T13:30:00+03").toInstant()),
            OrderReturnTrantime(789L, OffsetDateTime.parse("2021-11-22T14:45:00+03").toInstant())
        ))
    }

    @Test
    @DisplayName("Получить список трантаймов")
    @DbUnitDataSet(before = ["OrderReturnTrantimeDaoTest.testGetOrderReturnTrantimes.before.csv"])
    fun testGetOrderReturnTrantimes() {
        val trantimes = orderReturnTrantimeDao.getOrderReturnTrantimes(
            LocalDate.of(2021, 11, 21)
        )
        Assertions.assertFalse(trantimes.isEmpty())
        Assertions.assertEquals(1, trantimes.size)
        Assertions.assertEquals(456L, trantimes[0].returnId)
    }
}
