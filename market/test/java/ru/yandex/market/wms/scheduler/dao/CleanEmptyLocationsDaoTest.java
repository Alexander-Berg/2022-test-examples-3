package ru.yandex.market.wms.scheduler.dao;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class CleanEmptyLocationsDaoTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_DELETE_TIMEOUT = 5;
    private static final int DEFAULT_SELECT_TIMEOUT = 30;
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final String LOCATION = "location1";
    private static final long SERIAL_NUMBER = 123;
    private static final int EXPECTED_DELETED_COUNT = 1;

    @Autowired
    private CleanEmptyLocationsDao cleanEmptyLocationsDao;

    @Test
    @DatabaseSetup(value = "/db/dao/loc-for-dao-test/before-with-empty-loc.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/loc-for-dao-test/before-with-empty-loc.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void getEmptyLocationSerialNumberTest() {
        Optional<Long> serialNumber = cleanEmptyLocationsDao.verifyEmptyLocation(LOCATION, DEFAULT_SELECT_TIMEOUT);

        Assertions.assertEquals(SERIAL_NUMBER, serialNumber.get());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/loc-for-dao-test/before-with-empty-loc.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/loc-for-dao-test/after-with-empty-loc.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void deleteLocationBySerialKeyTest() {
        int deletedCount = cleanEmptyLocationsDao
                .deleteLocationBySerialKey(SERIAL_NUMBER, LOCATION, DEFAULT_DELETE_TIMEOUT);

        Assertions.assertEquals(EXPECTED_DELETED_COUNT, deletedCount);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/empty-loc-for-dao-test/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/empty-loc-for-dao-test/before.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void getEmptyLocationsTest() {
        List<String> expectedEmptyLocations = Arrays.asList("location1", "location2", "location3");
        List<String> emptyLocations = cleanEmptyLocationsDao
                .getEmptyLocations(DEFAULT_BATCH_SIZE, DEFAULT_SELECT_TIMEOUT);

        Assertions.assertEquals(expectedEmptyLocations, emptyLocations);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/empty-loc-for-dao-test/before.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(value = "/db/dao/empty-loc-for-dao-test/after-removing.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED)
    void deleteLocationFromEmptyLocTest() {
        cleanEmptyLocationsDao.deleteLocationFromEmptyLoc(LOCATION, DEFAULT_DELETE_TIMEOUT);
    }
}
