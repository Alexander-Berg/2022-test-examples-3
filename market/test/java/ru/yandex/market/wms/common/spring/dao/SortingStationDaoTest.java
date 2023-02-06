package ru.yandex.market.wms.common.spring.dao;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.lgw.util.CollectionUtil;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.implementation.SortingStationDao;
import ru.yandex.market.wms.common.spring.enums.AutoStartSortingStationMode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class SortingStationDaoTest extends IntegrationTest {

    @Autowired
    protected SortingStationDao dao;

    @Test
    @DatabaseSetups(
            {
                    @DatabaseSetup(value = "/db/dao/sortation-station/data.xml", connection = "wmwhseConnection"),
            }
    )
    public void findSortingStationCapacities() {
        assertThat(
                dao.findSortingStationCapacities(true, Set.of(AutoStartSortingStationMode.ORDERS)),
                is(equalTo(CollectionUtil.mapOf("S01", 2, "S02", 1)))
        );
    }
}
