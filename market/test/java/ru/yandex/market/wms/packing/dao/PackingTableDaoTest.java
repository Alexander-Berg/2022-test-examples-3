package ru.yandex.market.wms.packing.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.packing.LocationsRov;
import ru.yandex.market.wms.packing.LocationsSof;
import ru.yandex.market.wms.packing.pojo.PackingTable;

import static com.github.springtestdbunit.annotation.DatabaseOperation.INSERT;
import static org.assertj.core.api.Assertions.assertThat;

public class PackingTableDaoTest extends IntegrationTest {

    @Autowired
    private PackingTableDao dao;

    @Test
    @DatabaseSetup(value = "/db/locations_setup_rov.xml", type = INSERT)
    void getPackingTablesRov() {
        List<PackingTable> tables = dao.getPackingTables();
        assertThat(tables).containsExactlyInAnyOrder(
                LocationsRov.TABLE_1,
                LocationsRov.TABLE_2,
                LocationsRov.TABLE_3,
                LocationsRov.TABLE_4,
                LocationsRov.NONSORT_TABLE_1,
                LocationsRov.NONSORT_TABLE_2,
                LocationsRov.TABLE_PROMO_1
        );
    }

    @Test
    @DatabaseSetup("/db/locations_setup_sof.xml")
    void getPackingTablesSof() {
        List<PackingTable> tables = dao.getPackingTables();
        assertThat(tables).containsExactlyInAnyOrder(
                LocationsSof.TABLE_1_A,
                LocationsSof.TABLE_1_B,
                LocationsSof.TABLE_2_A,
                LocationsSof.TABLE_2_B,
                LocationsSof.NONSORT_TABLE_1
        );
    }

}
