package ru.yandex.market.wms.pickbylight.dao

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.wms.pickbylight.configuration.PickByLightIntegrationTest
import ru.yandex.market.wms.pickbylight.model.Station
import ru.yandex.market.wms.pickbylight.vendor.Vendor

@DatabaseSetup("/db/dao/station/station-and-cells.xml")
internal class StationDaoTest : PickByLightIntegrationTest() {

    companion object {
        val S01 = Station("S01", Vendor.AXELOT, "host1", 15003, "0", "1")
        val S02 = Station("S02", Vendor.AXELOT, "host1", 15003, "2", "3")
        val S03 = Station("S03", Vendor.AXELOT, "host2", 15003, "0", "1")
    }

    @Autowired
    private lateinit var stationDao: StationDao

    @Test
    fun getStations() {
        assertThat(stationDao.getStations()).containsExactlyInAnyOrder(S01, S02, S03)
    }

    @Test
    fun getStation() {
        listOf(S01, S02, S03).forEach {
            assertThat(stationDao.getStation(it.stationName)).isEqualTo(it)
        }
    }
}
