package ru.yandex.market.wms.consolidation.modules.consolidation.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.yandex.market.wms.common.model.enums.LocationType
import ru.yandex.market.wms.common.model.enums.PickDetailStatus
import ru.yandex.market.wms.common.model.enums.WaveStatus
import java.time.Instant

const val WAVEKEY: String = "WAVE001"

const val PUTWALL_01: String = "S-01"
const val PUTWALL_02: String = "S-02"

const val CONS_LINE_01_1: String = "LINE_01_1"
const val CONS_LINE_01_2: String = "LINE_01_2"
const val CONS_LINE_02_1: String = "LINE_02_1"

internal class WavePicksMapperTest {

    @Test
    fun waveOn1Line1PutWall() {
        val readyWave = WavePicksMapper.toReadyWave(
            WavePicks(
                WAVEKEY, WaveStatus.RELEASED_TO_TASK_MANAGER, Instant.now(), mutableListOf(
                    PicksLocation(PUTWALL_01, CONS_LINE_01_1, LocationType.CONSOLIDATION, 40.0, PickDetailStatus.PICKED),
                    PicksLocation(null, null, LocationType.PICK_TO, 60.0, PickDetailStatus.IN_PROCESS)
                )
            )
        )

        assertNotNull(readyWave!!)
        assertEquals(WAVEKEY, readyWave.waveKey)
        assertEquals(PUTWALL_01, readyWave.putWall)
        assertEquals(CONS_LINE_01_1, readyWave.line)
        assertEquals(40.0, readyWave.readinessPercentage)
    }

    @Test
    fun waveOn2Lines1PutWall() {
        val readyWave = WavePicksMapper.toReadyWave(
            WavePicks(
                WAVEKEY, WaveStatus.RELEASED_TO_TASK_MANAGER, Instant.now(), mutableListOf(
                    PicksLocation(PUTWALL_01, CONS_LINE_01_1, LocationType.CONSOLIDATION, 40.0, PickDetailStatus.PICKED),
                    PicksLocation(PUTWALL_01, CONS_LINE_01_2, LocationType.CONSOLIDATION, 20.0, PickDetailStatus.PICKED),
                    PicksLocation(null, null, LocationType.PICK_TO, 40.0, PickDetailStatus.IN_PROCESS)
                )
            )
        )

        assertNotNull(readyWave!!)
        assertEquals(WAVEKEY, readyWave.waveKey)
        assertEquals(PUTWALL_01, readyWave.putWall)
        assertTrue(readyWave.line == CONS_LINE_01_1 || readyWave.line == CONS_LINE_01_2)
        assertEquals(60.0, readyWave.readinessPercentage)
    }

    @Test
    fun waveOn2Lines2PutWalls() {
        val readyWave = WavePicksMapper.toReadyWave(
            WavePicks(
                WAVEKEY, WaveStatus.RELEASED_TO_TASK_MANAGER, Instant.now(), mutableListOf(
                    PicksLocation(PUTWALL_01, CONS_LINE_01_1, LocationType.CONSOLIDATION, 40.0, PickDetailStatus.PICKED),
                    PicksLocation(PUTWALL_02, CONS_LINE_02_1, LocationType.CONSOLIDATION, 20.0, PickDetailStatus.PICKED),
                    PicksLocation(null, null, LocationType.PICK_TO, 40.0, PickDetailStatus.IN_PROCESS)
                )
            )
        )

        assertNotNull(readyWave!!)
        assertEquals(WAVEKEY, readyWave.waveKey)
        assertTrue(readyWave.putWall == PUTWALL_01 || readyWave.putWall == PUTWALL_02)
        if (readyWave.putWall == PUTWALL_01) {
            assertEquals(readyWave.line, CONS_LINE_01_1)
            assertEquals(40.0, readyWave.readinessPercentage)
        } else {
            assertEquals(readyWave.line, CONS_LINE_02_1)
            assertEquals(20.0, readyWave.readinessPercentage)
        }
    }

    @Test
    fun waveOn2Lines2PutWallsSorting() {
        val readyWave = WavePicksMapper.toReadyWave(
            WavePicks(
                WAVEKEY, WaveStatus.SORTING, Instant.now(), mutableListOf(
                    PicksLocation(PUTWALL_01, CONS_LINE_01_2, LocationType.SORT, 20.0, PickDetailStatus.PICKED),
                    PicksLocation(PUTWALL_01, CONS_LINE_01_1, LocationType.CONSOLIDATION, 10.0, PickDetailStatus.PICKED),
                    PicksLocation(PUTWALL_02, CONS_LINE_02_1, LocationType.CONSOLIDATION, 30.0, PickDetailStatus.PICKED),
                    PicksLocation(null, null, LocationType.PICK_TO, 40.0, PickDetailStatus.IN_PROCESS)
                )
            )
        )

        assertNotNull(readyWave!!)
        assertEquals(WAVEKEY, readyWave.waveKey)
        assertEquals(PUTWALL_01, readyWave.putWall)
        assertEquals(readyWave.line, CONS_LINE_01_1)
        assertEquals(30.0, readyWave.readinessPercentage)
    }

    @Test
    fun waveOn2Lines2PutWallsWithShorted() {
        val readyWave = WavePicksMapper.toReadyWave(
            WavePicks(
                WAVEKEY, WaveStatus.SORTING, Instant.now(), mutableListOf(
                    PicksLocation(PUTWALL_01, CONS_LINE_01_2, LocationType.SORT, 1.0, PickDetailStatus.PICKED),
                    PicksLocation(PUTWALL_02, CONS_LINE_02_1, LocationType.CONSOLIDATION, 39.0, PickDetailStatus.PICKED),
                    PicksLocation(null, null, LocationType.PICK_TO, 60.0, PickDetailStatus.IN_PROCESS)
                )
            )
        )

        assertNotNull(readyWave!!)
        assertEquals(WAVEKEY, readyWave.waveKey)
        assertEquals(PUTWALL_02, readyWave.putWall)
        assertEquals(readyWave.line, CONS_LINE_02_1)
        assertEquals(39.0, readyWave.readinessPercentage)
    }

    @Test
    fun existsPickDetailsAfterPutWall() {
        val readyWave = WavePicksMapper.toReadyWave(
            WavePicks(
                WAVEKEY, WaveStatus.RELEASED_TO_TASK_MANAGER, Instant.now(), mutableListOf(
                    PicksLocation(PUTWALL_01, CONS_LINE_01_1, LocationType.CONSOLIDATION, 40.0, PickDetailStatus.PICKED),
                    PicksLocation(PUTWALL_01, CONS_LINE_01_2, LocationType.SORT, 20.0, PickDetailStatus.PICKED),
                    PicksLocation(null, null, LocationType.PACK, 10.0, PickDetailStatus.PACKED),
                    PicksLocation(null, null, LocationType.PICK_TO, 30.0, PickDetailStatus.IN_PROCESS)
                )
            )
        )

        assertNotNull(readyWave!!)
        assertEquals(WAVEKEY, readyWave.waveKey)
        assertEquals(WaveStatus.SORTING, readyWave.status)
        assertEquals(PUTWALL_01, readyWave.putWall)
        assertEquals(CONS_LINE_01_1, readyWave.line)
        assertEquals(60.0, readyWave.readinessPercentage)
    }

    @Test
    fun waveWithWrongDataReturnNull() {
        val readyWave = WavePicksMapper.toReadyWave(
            WavePicks(
                WAVEKEY, WaveStatus.SORTING, Instant.now(), mutableListOf(
                    PicksLocation(null, null, LocationType.PICK_TO, 100.0, PickDetailStatus.IN_PROCESS)
                )
            )
        )

        assertNull(readyWave)
    }
}
