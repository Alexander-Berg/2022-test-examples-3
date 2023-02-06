package ru.yandex.market.wms.consolidation.modules.consolidation.dao

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.common.spring.IntegrationTest

internal class ConsolidationDaoTest(
        @Autowired private val consolidationDao: ConsolidationDao
) : IntegrationTest() {

    @Test
    @DatabaseSetup("/line-for-putwall/before.xml")
    fun findReadyWavesPutWall() {
        with(consolidationDao.findReadyWavesPutWall("SORT-01")) {
            assertNotNull(this)
            assertFalse(isEmpty())
            assertEquals(2, size)
            assertEquals("123", first().waveKey)
        }
    }

    @Test
    @DatabaseSetup("/scan-uit/sort-locations.xml")
    fun findSortLocationsByOrderKey() {
        with(consolidationDao.findSortLocationsByOrderKey("123")) {
            assertFalse(isEmpty())
            assertEquals(3, size)
        }
    }

    @Test
    @DatabaseSetup("/scan-putwall-cell/before/cell-detail.xml")
    fun findCellDetail() {
        consolidationDao.findCellDetail("SORT-LOC-1")?.let {
            assertNotNull(it)
            assertNotNull(it.cell)
            assertNotNull(it.sortStation)
            assertNotNull(it.dropId)
            assertNotNull(it.orderKey)
            assertFalse(it.isEmpty)
        }
        consolidationDao.findCellDetail("SORT-LOC-2")?.let {
            assertNotNull(it)
            assertNotNull(it.cell)
            assertNotNull(it.sortStation)
            assertNull(it.orderKey)
            assertTrue(it.isEmpty)
        }
    }

    @Test
    @DisplayName("Готовность волны к консолидации: 10%")
    @DatabaseSetup("/wave-readiness/before/before-0.xml")
    fun updateWaveReadinessAsync_10() {
        assertions.assertThat(consolidationDao.percentageOfWaveNearPutWall("WAVE001")).isGreaterThan(10.0)
    }

    @Test
    @DisplayName("Готовность волны к консолидации: 80%")
    @DatabaseSetup("/wave-readiness/before/before-1.xml")
    fun updateWaveReadinessAsync_80() {
        assertions.assertThat(consolidationDao.percentageOfWaveNearPutWall("WAVE001")).isGreaterThan(80.0)
    }

    @Test
    @DisplayName("Готовность волны к консолидации: 100%")
    @DatabaseSetup("/wave-readiness/before/before-2.xml")
    fun updateWaveReadinessAsync_100() {
        assertions.assertThat(consolidationDao.percentageOfWaveNearPutWall("WAVE001")).isEqualTo(100.0)
    }

    @Test
    @DisplayName("Готовность волны к консолидации: 100% (+нонсорт)")
    @DatabaseSetup("/wave-readiness/before/before-3.xml")
    fun updateWaveReadinessAsync_100_NonSort() {
        assertions.assertThat(consolidationDao.percentageOfWaveNearPutWall("WAVE001")).isEqualTo(100.0)
    }
}
