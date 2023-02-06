package ru.yandex.market.hrms.core.domain.timex.repo;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimexDisasterRepoTest extends AbstractCoreTest {
    @Autowired
    TimexDisasterRepo timexDisasterRepo;

    @Test
    @DbUnitDataSet(before = "TimexDisasterRepoTest.before.csv")
    public void getAll() {
        List<TimexDisasterEntity> all = timexDisasterRepo.findAll();
        assertEquals(1, all.size(), "all");
    }
}
