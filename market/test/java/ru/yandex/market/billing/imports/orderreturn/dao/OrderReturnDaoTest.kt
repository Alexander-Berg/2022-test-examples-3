package ru.yandex.market.billing.imports.orderreturn.dao

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.FunctionalTest
import ru.yandex.market.billing.imports.orderreturn.model.OrderReturn
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus
import ru.yandex.market.common.test.db.DbUnitDataSet
import java.time.OffsetDateTime

/**
 * Тесты для [OrderReturnDao].
 */
internal class OrderReturnDaoTest : FunctionalTest() {
    @Autowired
    private lateinit var orderReturnDao: OrderReturnDao

    @Test
    @DisplayName("Сохраняем одну запись")
    @DbUnitDataSet(after = ["OrderReturnDaoTest.testStore.after.csv"])
    fun testStore() {
        orderReturnDao.store(listOf(
            OrderReturn(
                returnId = 123L,
                orderId = 456L,
                status = ReturnStatus.REFUNDED,
                createdAt = OffsetDateTime.parse("2021-11-20T12:00:00+03").toInstant(),
                statusUpdatedAt = OffsetDateTime.parse("2021-11-20T14:00:00+03").toInstant()
            )
        ))
    }

    @Test
    @DisplayName("Получаем список return_ids, которые нужно игнорировать")
    @DbUnitDataSet(before = ["OrderReturnDaoTest.testGetIgnoredReturnIds.before.csv"])
    fun testGetIgnoredReturnIds() {
        val ignoredReturnIds = orderReturnDao.ignoredReturnIds
        Assertions.assertFalse(ignoredReturnIds.isEmpty())
        Assertions.assertTrue(ignoredReturnIds.contains(123L))
        Assertions.assertTrue(ignoredReturnIds.contains(456L))
    }
}
