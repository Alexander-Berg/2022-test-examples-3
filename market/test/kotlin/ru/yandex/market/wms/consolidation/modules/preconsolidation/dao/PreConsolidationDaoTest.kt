package ru.yandex.market.wms.consolidation.modules.preconsolidation.dao

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dao.implementation.WaveDao
import ru.yandex.market.wms.common.spring.service.SortStationAssignmentDbService

class PreConsolidationDaoTest(
    @Autowired private val dbService: SortStationAssignmentDbService,
    @Autowired private val waveDao: WaveDao,
    @Autowired private val jdbcTemplate: JdbcTemplate
) : IntegrationTest() {
    @BeforeEach
    fun actualiseData() {
        jdbcTemplate.update("update wave set adddate = getdate()")
    }

    @Test
    @DatabaseSetup("/reassign-station/dao/before.xml")
    fun getAlreadyAssignedWaves() {
        val actual = waveDao.getAlreadyAssignedWaves("SORT1")
        assertEquals(setOf("01", "03"), actual)
    }

    @Test
    @DatabaseSetup("/reassign-station/dao/before.xml")
    fun getAlreadyAssignedWavesEmpty() {
        val actual = waveDao.getAlreadyAssignedWaves("SORT2")
        assertTrue(actual.isEmpty())
    }

    @Test
    @DatabaseSetup("/reassign-station/dao/before.xml")
    @ExpectedDatabase("/reassign-station/dao/after.xml", assertionMode = NON_STRICT)
    fun assignStationToWave() {
        val wave = waveDao.findWavesByKeys(listOf("01"))[0]
        dbService.reassignStation(wave, "SORT2", "user")
    }
}
