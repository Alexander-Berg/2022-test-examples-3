package ru.yandex.market.wms.scheduler.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

class ArchiveWaveDaoTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_BATCH_SIZE = 10000;
    private static final int DEFAULT_SELECT_TIMEOUT = 10;
    private static final int DEFAULT_DAYS_THRESHOLD = 30;
    private static final List<String> LIST_TO_ARCHIVE = List.of("WAVE001", "WAVE002", "WAVE003");

    @Autowired
    private ArchiveWaveDao dao;

    @Test
    @DatabaseSetup(value = "/db/dao/archive/wave/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/archive/wave/before.xml",
            connection = "wmwhseConnection", assertionMode = NON_STRICT_UNORDERED)
    void getKeyListTest() {
        List<String> keyList = dao.getKeyList(DEFAULT_BATCH_SIZE, DEFAULT_DAYS_THRESHOLD, DEFAULT_SELECT_TIMEOUT);
        Assertions.assertEquals(LIST_TO_ARCHIVE, keyList);
    }
}
