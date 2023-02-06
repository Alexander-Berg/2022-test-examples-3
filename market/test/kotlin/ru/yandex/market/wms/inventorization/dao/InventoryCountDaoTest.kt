package ru.yandex.market.wms.inventorization.dao

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.inventorization.entity.InventorizationEvent

class InventoryCountDaoTest : IntegrationTest() {
    @Autowired
    private val inventoryCountDao: InventoryCountDao? = null

    @Test
    @DatabaseSetup("/dao/inventorycount_log/before.xml")
    @ExpectedDatabase(value = "/dao/inventorycount_log/before.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun selectTop() {
        val expected: List<InventorizationEvent> = listOf(
            InventorizationEvent(63599643, "anonymousUser"),
            InventorizationEvent(63599642, "anonymousUser6"),
            InventorizationEvent(63599640, "anonymousUser5"),
            InventorizationEvent(63599639, "anonymousUser4"),
            InventorizationEvent(63599638, "anonymousUser3"),
        )
        val topList = inventoryCountDao!!.getTopKeys(5)
        Assertions.assertNotNull(topList)
        Assertions.assertFalse(topList.isEmpty())
        Assertions.assertEquals(expected.size, topList.size)
        Assertions.assertTrue(expected.containsAll(topList) && topList.containsAll(expected))
    }

    @Test
    @DatabaseSetup("/dao/inventorycount_log/before.xml")
    @ExpectedDatabase(value = "/dao/inventorycount_log/before.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun `test getListWhereKeyGreaterThan`() {
        val expected: List<InventorizationEvent> = listOf(
            InventorizationEvent(63599643, "anonymousUser"),
            InventorizationEvent(63599642, "anonymousUser6"),
            InventorizationEvent(63599640, "anonymousUser5"),
        )
        val topList = inventoryCountDao!!.getListWhereKeyGreaterThan(63599639)
        Assertions.assertNotNull(topList)
        Assertions.assertFalse(topList.isEmpty())
        Assertions.assertEquals(expected.size, topList.size)
        Assertions.assertTrue(expected.containsAll(topList) && topList.containsAll(expected))
    }

}
