package ru.yandex.market.billing.sortingcenter.pg;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.sortingcenter.model.SCTariffDTO;
import ru.yandex.market.common.test.db.DbUnitDataSet;


class SCTariffDaoTest extends FunctionalTest {

    public static final LocalDate TEST_DATE = LocalDate.of(2021, 12, 20);

    @Autowired
    private SCTariffDao scTariffDao;

    @DbUnitDataSet(before = "SCTariffDaoTest.selectAllTariffsForDate.before.csv")
    @Test
    void selectTariffsForDateTest() {
        List<SCTariffDTO> tariffs = scTariffDao.selectActualTariffs(TEST_DATE);
        Assertions.assertEquals(2, tariffs.size());
    }

    @DbUnitDataSet(before = "SCTariffDaoTest.selectAllTariffsForDate.before.csv")
    @Test
    void selectTariffsAfterUpdateTest() {
        List<SCTariffDTO> tariffs = scTariffDao.selectActualTariffs(TEST_DATE);
        Assertions.assertEquals(2, tariffs.size());
    }
}
