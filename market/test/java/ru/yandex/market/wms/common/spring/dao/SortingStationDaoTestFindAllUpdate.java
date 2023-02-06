package ru.yandex.market.wms.common.spring.dao;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.implementation.SortingStationDao;
import ru.yandex.market.wms.common.spring.dto.AutoStartSortingStationDto;
import ru.yandex.market.wms.common.spring.enums.AutoStartSortingStationMode;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.common.spring.enums.AutoStartSortingStationMode.NON_CONVEYABLE_ORDERS;
import static ru.yandex.market.wms.common.spring.enums.AutoStartSortingStationMode.ORDERS;
import static ru.yandex.market.wms.common.spring.enums.AutoStartSortingStationMode.WITHDRAWALS;

class SortingStationDaoTestFindAllUpdate extends IntegrationTest {

    @Autowired
    protected SortingStationDao dao;

    @Test
    @DatabaseSetups(
            {
                    @DatabaseSetup(value = "/db/dao/sortation-station/data2.xml", connection = "wmwhseConnection"),
            }
    )
    public void test() {
        assertThat(dao.findAll(), is(equalTo(initial())));
        dao.update(updated());
        assertThat(dao.findAll(), is(equalTo(updated())));
    }

    @Test
    @DatabaseSetups(
            {
                    @DatabaseSetup(value = "/db/dao/sortation-station/data2.xml", connection = "wmwhseConnection"),
            }
    )
    public void testOrders() {
        assertThat(dao.findAll(Set.of(ORDERS)), is(equalTo(List.of(initial().get(0)))));
        dao.update(updated());
        assertThat(dao.findAll(Set.of(ORDERS)), is(equalTo(List.of(updated().get(1)))));
    }

    @Test
    @DatabaseSetups(
            {
                    @DatabaseSetup(value = "/db/dao/sortation-station/data2.xml", connection = "wmwhseConnection"),
            }
    )
    public void testWithdrawals() {
        assertThat(dao.findAll(Set.of(WITHDRAWALS)), is(equalTo(List.of(initial().get(1)))));
        dao.update(updated());
        assertThat(dao.findAll(Set.of(WITHDRAWALS)), is(equalTo(List.of(updated().get(2)))));
    }

    @Test
    @DatabaseSetups(
            {
                    @DatabaseSetup(value = "/db/dao/sortation-station/data2.xml", connection = "wmwhseConnection"),
            }
    )
    public void testNonConveyableOrders() {
        assertThat(
                dao.findAll(Set.of(NON_CONVEYABLE_ORDERS)),
                is(equalTo(List.of(initial().get(2))))
        );
        dao.update(updated());
        assertThat(
                dao.findAll(Set.of(NON_CONVEYABLE_ORDERS)),
                is(equalTo(List.of(updated().get(3))))
        );
    }

    @Test
    @DatabaseSetups(
            {
                    @DatabaseSetup(value = "/db/dao/sortation-station/data2.xml", connection = "wmwhseConnection"),
            }
    )
    public void testOff() {
        assertThat(dao.findAll(Set.of(AutoStartSortingStationMode.OFF)), is(equalTo(List.of(initial().get(3)))));
        dao.update(updated());
        assertThat(dao.findAll(Set.of(AutoStartSortingStationMode.OFF)), is(equalTo(List.of(updated().get(0)))));
    }

    static List<AutoStartSortingStationDto> initial() {
        return Arrays.asList(
                AutoStartSortingStationDto.builder()
                        .station("S01")
                        .mode(ORDERS)
                        .build(),

                AutoStartSortingStationDto.builder()
                        .station("S02")
                        .mode(WITHDRAWALS)
                        .build(),

                AutoStartSortingStationDto.builder()
                        .station("S03")
                        .mode(NON_CONVEYABLE_ORDERS)
                        .build(),

                AutoStartSortingStationDto.builder()
                        .station("S04")
                        .mode(AutoStartSortingStationMode.OFF)
                        .build()
        );
    }

    static List<AutoStartSortingStationDto> updated() {
        return Arrays.asList(
                AutoStartSortingStationDto.builder()
                        .station("S01")
                        .mode(AutoStartSortingStationMode.OFF)
                        .build(),

                AutoStartSortingStationDto.builder()
                        .station("S02")
                        .mode(ORDERS)
                        .build(),

                AutoStartSortingStationDto.builder()
                        .station("S03")
                        .mode(WITHDRAWALS)
                        .build(),

                AutoStartSortingStationDto.builder()
                        .station("S04")
                        .mode(NON_CONVEYABLE_ORDERS)
                        .build()
        );
    }
}
