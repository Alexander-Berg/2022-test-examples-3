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

public class ArchiveTransportUnitTrackingDaoTest extends SchedulerIntegrationTest {

    @Autowired
    private ArchiveTransportUnitTrackingDao dao;

    private static final int SELECTION_TIMEOUT = 10;

    @Test
    @DatabaseSetup(value = "/db/dao/archive/transport-unit-tracking/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/archive/transport-unit-tracking/after-remove.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED
    )
    void deleteUnitTrackingTest() {
        List<String> unitTrackingKeys = Collections.singletonList("1");

        int i = dao.deleteUnitTracking(unitTrackingKeys, SELECTION_TIMEOUT);

        Assertions.assertEquals(1, i);
    }
}
