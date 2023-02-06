package ru.yandex.market.wms.pickbylight.dao

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.pickbylight.configuration.PickByLightIntegrationTest
import ru.yandex.market.wms.pickbylight.model.StationCell

@DatabaseSetup("/db/dao/station/station-and-cells.xml")
internal class StationCellDaoTest : PickByLightIntegrationTest() {

    companion object {
        val S01_001 = StationCell("S01", "S01-001", "0001", "1001")
        val S01_002 = StationCell("S01", "S01-002", "0002", "1002")
        val S02_001 = StationCell("S02", "S02-001", "2001", "3001")
        val S02_002 = StationCell("S02", "S02-002", "2002", "3002")
        val S03_001 = StationCell("S03", "S03-001", "0001", "1001")
        val S03_002 = StationCell("S03", "S03-002", "0002", "1002")
    }

    @Autowired
    private lateinit var stationCellDao: StationCellDao

    @Test
    fun getStationCells() {
        assertThat(stationCellDao.getStationCells("S01")).containsExactlyInAnyOrder(S01_001, S01_002)
        assertThat(stationCellDao.getStationCells("S02")).containsExactlyInAnyOrder(S02_001, S02_002)
        assertThat(stationCellDao.getStationCells("S03")).containsExactlyInAnyOrder(S03_001, S03_002)
    }

    @Test
    fun getStationCellsByHostPort() {
        assertThat(stationCellDao.getStationCellsByHostPort("host1", 15003))
            .containsExactlyInAnyOrder(S01_001, S01_002, S02_001, S02_002)

        assertThat(stationCellDao.getStationCellsByHostPort("host2", 15003))
            .containsExactlyInAnyOrder(S03_001, S03_002)
    }
}
