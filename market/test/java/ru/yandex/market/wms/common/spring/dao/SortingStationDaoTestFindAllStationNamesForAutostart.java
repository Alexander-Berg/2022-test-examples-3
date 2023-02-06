package ru.yandex.market.wms.common.spring.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.SortStation;
import ru.yandex.market.wms.common.spring.dao.implementation.SortingStationDao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.common.spring.enums.AutoStartSortingStationMode.ORDERS;
import static ru.yandex.market.wms.common.spring.enums.AutoStartSortingStationMode.WITHDRAWALS;

class SortingStationDaoTestFindAllStationNamesForAutostart extends IntegrationTest {

    @Autowired
    protected SortingStationDao dao;

    @Test
    @DatabaseSetups(
            {
                    @DatabaseSetup(value = "/db/dao/sortation-station/data2.xml", connection = "wmwhseConnection"),
            }
    )
    public void test() {
        assertThat(dao.findAllStationNamesForAutostart(ORDERS),
                is(equalTo(List.of(SortStation.builder().sortStation("S01").activeBatchesPerPutwall(2).build()))));
        assertThat(dao.findAllStationNamesForAutostart(WITHDRAWALS),
                is(equalTo(List.of(SortStation.builder().sortStation("S02").activeBatchesPerPutwall(2).build()))));
    }

    @Test
    @DatabaseSetups(
            {
                    @DatabaseSetup(value = "/db/dao/sortation-station/data3.xml", connection = "wmwhseConnection"),
            }
    )
    public void testFindSoringStationInBuildingForAutostart() {
        assertThat(dao.findAllStationNamesForAutostart(ORDERS, 1),
                is(equalTo(List.of(SortStation.builder().sortStation("S01").activeBatchesPerPutwall(2).build(),
                        SortStation.builder().sortStation("S02").activeBatchesPerPutwall(2).build()
                        ))));
        assertThat(dao.findAllStationNamesForAutostart(ORDERS, 2),
                is(equalTo(List.of(SortStation.builder().sortStation("S03").activeBatchesPerPutwall(2).build()))));
    }
}
