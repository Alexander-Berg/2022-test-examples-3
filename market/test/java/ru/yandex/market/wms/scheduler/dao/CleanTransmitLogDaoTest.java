package ru.yandex.market.wms.scheduler.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class CleanTransmitLogDaoTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_BATCH_SIZE = 4;
    private static final int DEFAULT_INACTIVITY_DAYS_THRESHOLD = 2;
    private static final int DEFAULT_SELECTION_TIMEOUT = 20;
    private static final int DEFAULT_DELETION_TIMEOUT = 4;

    private static final List<Integer> LIST_TO_DELETE = List.of(2, 3, 4);

    @Autowired
    private CleanTransmitLogDao cleanTransmitLogDao;

    @Test
    @DatabaseSetup(value = "/db/dao/clean/clean-transmit-log/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-transmit-log/before.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void getSerialKeysForDeleteTest() {
        List<Integer> serialKeys = cleanTransmitLogDao.getTransmitLogSerialKeys(
                DEFAULT_BATCH_SIZE, DEFAULT_INACTIVITY_DAYS_THRESHOLD, DEFAULT_SELECTION_TIMEOUT);
        Assertions.assertEquals(LIST_TO_DELETE, serialKeys);
    }
}
