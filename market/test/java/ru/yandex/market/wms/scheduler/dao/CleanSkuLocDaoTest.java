package ru.yandex.market.wms.scheduler.dao;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class CleanSkuLocDaoTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_BATCH_SIZE = 4;
    private static final int DEFAULT_ARCHIVE_DAYS = 2;
    private static final int DEFAULT_SELECTION_TIMEOUT = 20;
    private static final int DEFAULT_DELETION_TIMEOUT = 4;

    private static final List<Integer> LIST_TO_DELETE = List.of(2, 3, 4);

    @Autowired
    private CleanSkuLocDao cleanSkuLocDao;

    @Test
    @DatabaseSetup(value = "/db/dao/clean/clean-sku-loc/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-sku-loc/before.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void getSerialKeysForDeleteTest() {
        List<Integer> serialKeys = cleanSkuLocDao.getSerialKeys(
                DEFAULT_BATCH_SIZE, DEFAULT_ARCHIVE_DAYS, DEFAULT_SELECTION_TIMEOUT);
        Assertions.assertEquals(serialKeys, LIST_TO_DELETE);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/clean/clean-sku-loc/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-sku-loc/after.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void deleteBySerialKeysTest() {
        List<Integer> serialKeys = cleanSkuLocDao.getSerialKeys(
                DEFAULT_BATCH_SIZE, DEFAULT_ARCHIVE_DAYS, DEFAULT_SELECTION_TIMEOUT);

        int deletedRowsCount = cleanSkuLocDao.deleteSerialKeys(serialKeys, DEFAULT_DELETION_TIMEOUT);
        Assertions.assertEquals(3, deletedRowsCount);
    }
}
