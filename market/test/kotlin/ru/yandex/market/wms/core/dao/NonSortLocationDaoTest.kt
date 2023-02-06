package ru.yandex.market.wms.core.dao

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType.OVERSIZE
import ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType.SINGLES

class NonSortLocationDaoTest : IntegrationTest() {
    @Autowired
    private val nonSortLocationDao: NonSortLocationDao? = null

    @Test
    @DatabaseSetup("/cons-loc/db/before.xml")
    fun getPackTablesWithConsolidationLocations() {
        //when
        val packTablesWithConsolidationLocations = nonSortLocationDao!!.getPackTablesWithConsolidationLocations()
        //then
        assertNotNull(packTablesWithConsolidationLocations)
        assertFalse(packTablesWithConsolidationLocations.isEmpty())
        assertEquals(3, packTablesWithConsolidationLocations.size)
        packTablesWithConsolidationLocations.forEach {
            assertNotNull(it.table)
            assertNotNull(it.loc)
            assertNotNull(it.types)
            assertEquals("PACK-2", it.table)
            when (it.loc) {
                "CONS-2" -> {
                    assertEquals(2, it.types.size)
                    assertTrue(it.types.contains(SINGLES))
                    assertTrue(it.types.contains(OVERSIZE))
                }
                "CONS-3" -> {
                    assertEquals(1, it.types.size)
                    assertTrue(it.types.contains(OVERSIZE))
                }
                "CONS-4" -> assertTrue(it.types.isEmpty())
            }
        }
    }
}
