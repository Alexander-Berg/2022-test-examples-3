package ru.yandex.market.wms.packing.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.packing.LocationsRov;


class PackingTableServiceTest extends IntegrationTest {

    @Autowired
    private PackingTableService packingTableService;

    @Test
    @DatabaseSetup("/db/locations_setup_rov.xml")
    void getPackingTables() {
        Assertions.assertThat(packingTableService.getPackingTablesNoCache()).containsExactlyInAnyOrder(
                LocationsRov.TABLE_1,
                LocationsRov.TABLE_2,
                LocationsRov.TABLE_3,
                LocationsRov.TABLE_4,
                LocationsRov.NONSORT_TABLE_1,
                LocationsRov.NONSORT_TABLE_2
        );
    }

    @Test
    @DatabaseSetup("/db/locations_setup_rov.xml")
    @DatabaseSetup("/db/service/packing-table/config-cons-loc.xml")
    void getPackingTablesWithUserSelectsTask() {
        Assertions.assertThat(packingTableService.getPackingTablesNoCache()).containsExactlyInAnyOrder(
                LocationsRov.TABLE_1,
                LocationsRov.TABLE_2,
                LocationsRov.TABLE_3,
                LocationsRov.TABLE_4,
                LocationsRov.NONSORT_TABLE_1,
                LocationsRov.NONSORT_TABLE_2.toBuilder().userSelectsTask(true).build()
        );
    }

    @Test
    @DatabaseSetup("/db/locations_setup_rov.xml")
    @DatabaseSetup("/db/service/packing-table/config-table.xml")
    void getPackingTablesWithUserSelectsTask2() {
        Assertions.assertThat(packingTableService.getPackingTablesNoCache()).containsExactlyInAnyOrder(
                LocationsRov.TABLE_1,
                LocationsRov.TABLE_2,
                LocationsRov.TABLE_3,
                LocationsRov.TABLE_4,
                LocationsRov.NONSORT_TABLE_1,
                LocationsRov.NONSORT_TABLE_2.toBuilder().userSelectsTask(true).build()
        );
    }
}
