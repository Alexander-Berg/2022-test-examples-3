package ru.yandex.market.hrms.core.service.ispring;

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;

class OutstaffIspringServiceTest extends AbstractCoreTest {

    @Autowired
    private OutstaffIspringService outstaffIspringService;

    @Test
    @DbUnitDataSet(before = "OutstaffIspringServiceTest.duplicatesInISpringId.before.csv")
    void getIspringOutstaffToDeactivate() {
        mockClock(LocalDate.of(2021, 10, 15));
        var result = outstaffIspringService.getIspringOutstaffToDeactivate();
        Assertions.assertEquals(1, result.size());
    }
}
