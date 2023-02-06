package ru.yandex.market.billing.imports.orderreturn.dao

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.FunctionalTest
import ru.yandex.market.billing.imports.orderreturn.model.OrderReturnItem
import ru.yandex.market.common.test.db.DbUnitDataSet

/**
 * Тесты для [OrderReturnItemDao].
 */
internal class OrderReturnItemDaoTest : FunctionalTest() {
    @Autowired
    private lateinit var orderReturnItemDao: OrderReturnItemDao

    @Test
    @DisplayName("Сохраняем одну запись")
    @DbUnitDataSet(after = ["OrderReturnItemDaoTest.testStore.after.csv"])
    fun testStore() {
        orderReturnItemDao.store(listOf(
            OrderReturnItem(
                returnItemId = 123L,
                orderId = 456L,
                returnId = 789L,
                orderItemId = 4561L,
                count = 2,
                returnReason = "my awesome return reason"
            )
        ))
    }

    @Test
    @DisplayName("Сохраняем одну запись + обновляем одну запись")
    @DbUnitDataSet(
        before = ["OrderReturnItemDaoTest.testStore.after.csv"],
        after = ["OrderReturnItemDaoTest.testStoreWithUpdate.after.csv"]
    )
    fun testStoreWithUpdate() {
        orderReturnItemDao.store(listOf(
            OrderReturnItem(
                returnItemId = 123L,
                orderId = 456L,
                returnId = 789L,
                orderItemId = 4561L,
                count = 3,
                returnReason = "my awesome return reason 1"
            ),
            OrderReturnItem(
                returnItemId = 124L,
                orderId = 456L,
                returnId = 789L,
                orderItemId = 4562L,
                count = 2,
                returnReason = "my awesome return reason 2"
            )
        ))
    }

    @Test
    @DisplayName("Получаем список return_item_ids, которые нужно игнорировать")
    @DbUnitDataSet(before = ["OrderReturnItemDaoTest.testGetIgnoredReturnItemIds.before.csv"])
    fun testGetIgnoredReturnItemIds() {
        val ignoredReturnIds = orderReturnItemDao.ignoredReturnItemIds
        Assertions.assertFalse(ignoredReturnIds.isEmpty())
        Assertions.assertTrue(ignoredReturnIds.contains(321L))
        Assertions.assertTrue(ignoredReturnIds.contains(654L))
    }

    @Test
    @DisplayName("Получаем список OrderReturnItem")
    @DbUnitDataSet(before = ["OrderReturnItemDaoTest.testGetOrderReturnItems.before.csv"])
    fun testGetOrderReturnItems() {
        val result = orderReturnItemDao.getOrderReturnItems(setOf(789L, 791L))
        Assertions.assertEquals(3, result.size)
        Assertions.assertTrue(result.any { it.returnItemId == 123L })
        Assertions.assertTrue(result.any { it.returnItemId == 126L })
        Assertions.assertTrue(result.any { it.returnItemId == 127L })
    }
}
