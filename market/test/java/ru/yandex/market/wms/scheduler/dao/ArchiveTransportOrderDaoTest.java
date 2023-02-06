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

public class ArchiveTransportOrderDaoTest extends SchedulerIntegrationTest {

    @Autowired
    private ArchiveTransportOrderDao dao;

    private static final int SELECTION_TIMEOUT = 10;

    @Test
    @DatabaseSetup(value = "/db/dao/archive/transport-order/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/dao/archive/transport-order/after.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    void deleteTransportOrderTest() {
        List<String> transportOrderKeys = Collections.singletonList("6d809e60-d707-11ea-9550-a9553a7b0571");

        int i = dao.deleteTransportOrder(transportOrderKeys, SELECTION_TIMEOUT);

        Assertions.assertEquals(1, i);
    }
}
