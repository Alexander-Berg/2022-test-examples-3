package ru.yandex.market.wms.scheduler.dao;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class ArchiveSorterDimensionControlDaoTest extends SchedulerIntegrationTest {
    @Autowired
    private ArchiveSorterDimensionsControlDao dao;

    private static final int SELECTION_TIMEOUT = 10;

    @Test
    @DatabaseSetup(value = "/db/dao/archive/sorter-dimensions-control/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/archive/sorter-dimensions-control/after-remove.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED
    )
    void deleteSorterDimensionsControlTest() {
        List<String> deleteSorterDimensionsControlIds = Collections.singletonList("1");

        int i = dao.deleteSorterDimensionsControl(deleteSorterDimensionsControlIds, SELECTION_TIMEOUT);

        Assertions.assertEquals(1, i);
    }
}
