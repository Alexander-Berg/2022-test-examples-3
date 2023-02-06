package ru.yandex.market.wms.scheduler.service.clean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;
import ru.yandex.market.wms.scheduler.dao.CleanEmptyLocationsDao;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CleanEmptyLocationsServiceTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int DEFAULT_DELETE_TIMEOUT = 12;
    private static final int DEFAULT_SELECT_TIMEOUT = 30;
    private static final int DEFAULT_SLEEP_TIME = 10;
    private static final int DEFAULT_MAX_EXECUTION_TIME = 5_000;
    private static final String LOCATION_1 = "location1";
    private static final String LOCATION_2 = "location2";
    private static final long SERIAL_NUMBER_1 = 1;
    private static final long SERIAL_NUMBER_2 = 2;

    @InjectMocks
    private CleanEmptyLocationsService cleanEmptyLocationsService;

    @Mock
    private CleanEmptyLocationsDao cleanEmptyLocationsDao;

    @Mock
    private DbConfigService dbConfigService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        setUpMocks();
    }

    @Test
    void executeForOneLocationTest() throws InterruptedException {
        when(cleanEmptyLocationsDao.getEmptyLocations(anyInt(), anyInt()))
                .thenReturn(Collections.singletonList(LOCATION_1));
        when(cleanEmptyLocationsDao.verifyEmptyLocation(eq(LOCATION_1), anyInt()))
                .thenReturn(Optional.of(SERIAL_NUMBER_1));
        when(cleanEmptyLocationsDao.deleteLocationBySerialKey(eq(SERIAL_NUMBER_1), eq(LOCATION_1), anyInt()))
                .thenReturn(1);

        String expectedResult = String.format("Deleted empty location from wmwhse1.LOC. " +
                "Deleted locations count %d", 1);
        String result = cleanEmptyLocationsService.execute();

        Assertions.assertEquals(expectedResult, result);
        verifyDbConfigServiceMock();
        verifyCleanEmptyLocationsDaoMock(1, 1, 1,
                0, 0);
    }

    @Test
    void executeForOneLocationWithoutSerialKeysTest() throws InterruptedException {
        when(cleanEmptyLocationsDao.getEmptyLocations(anyInt(), anyInt()))
                .thenReturn(Collections.singletonList(LOCATION_1));
        when(cleanEmptyLocationsDao.verifyEmptyLocation(eq(LOCATION_1), anyInt()))
                .thenReturn(Optional.empty());

        String expectedResult = String.format("Deleted empty location from wmwhse1.LOC. " +
                "Deleted locations count %d", 0);
        String result = cleanEmptyLocationsService.execute();

        Assertions.assertEquals(expectedResult, result);
        verifyDbConfigServiceMock();
        verifyCleanEmptyLocationsDaoMock(1, 1, 0,
                0, 1);
    }

    @Test
    void executeForSomeLocationsTest() throws InterruptedException {
        when(cleanEmptyLocationsDao.getEmptyLocations(anyInt(), anyInt()))
                .thenReturn(Arrays.asList(LOCATION_1, LOCATION_2));
        when(cleanEmptyLocationsDao.verifyEmptyLocation(eq(LOCATION_1), anyInt()))
                .thenReturn(Optional.of(SERIAL_NUMBER_1));
        when(cleanEmptyLocationsDao.verifyEmptyLocation(eq(LOCATION_2), anyInt()))
                .thenReturn(Optional.of(SERIAL_NUMBER_2));
        when(cleanEmptyLocationsDao.deleteLocationBySerialKey(eq(SERIAL_NUMBER_1), eq(LOCATION_1), anyInt()))
                .thenReturn(1);
        when(cleanEmptyLocationsDao.deleteLocationBySerialKey(eq(SERIAL_NUMBER_2), eq(LOCATION_2), anyInt()))
                .thenReturn(1);

        String expectedResult = String.format("Deleted empty location from wmwhse1.LOC. " +
                "Deleted locations count %d", 2);
        String result = cleanEmptyLocationsService.execute();

        Assertions.assertEquals(expectedResult, result);
        verifyDbConfigServiceMock();
        verifyCleanEmptyLocationsDaoMock(1, 2, 2,
                0, 0);
    }

    @Test
    void executeForEmptyLocationsTest() throws InterruptedException {
        when(cleanEmptyLocationsDao.getEmptyLocations(anyInt(), anyInt()))
                .thenReturn(new ArrayList<>());

        String expectedResult = String.format("Deleted empty location from wmwhse1.LOC. " +
                "Deleted locations count %d", 0);
        String result = cleanEmptyLocationsService.execute();

        Assertions.assertEquals(expectedResult, result);
        verifyDbConfigServiceMock();
        verifyCleanEmptyLocationsDaoMock(1, 0, 0,
                0, 0);
    }

    private void setUpMocks() {
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_EMPTY_LOC_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_SLEEP_TIME);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_EMPTY_LOC_EXEC_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_MAX_EXECUTION_TIME);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_EMPTY_LOC_SELECT_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_SELECT_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_EMPTY_LOC_DELETE_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_DELETE_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_EMPTY_LOC_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_BATCH_SIZE);
    }

    private void verifyDbConfigServiceMock() {
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("DEL_EMPTY_LOC_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("DEL_EMPTY_LOC_EXEC_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("DEL_EMPTY_LOC_SELECT_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("DEL_EMPTY_LOC_DELETE_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("DEL_EMPTY_LOC_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
    }

    private void verifyCleanEmptyLocationsDaoMock(
            int getEmptyLocationsTimes,
            int verifyEmptyLocationTimes,
            int deleteLocationBySerialKeyTimes,
            int deleteLocationFromEmptyLocTimes,
            int markLocationNotDeletedTimes
    ) {
        verify(cleanEmptyLocationsDao, times(getEmptyLocationsTimes)).getEmptyLocations(anyInt(), anyInt());
        verify(cleanEmptyLocationsDao, times(verifyEmptyLocationTimes)).verifyEmptyLocation(anyString(), anyInt());
        verify(cleanEmptyLocationsDao, times(deleteLocationBySerialKeyTimes))
                .deleteLocationBySerialKey(anyLong(), anyString(), anyInt());
        verify(cleanEmptyLocationsDao, times(deleteLocationFromEmptyLocTimes))
                .deleteLocationFromEmptyLoc(anyString(), anyInt());
        verify(cleanEmptyLocationsDao, times(markLocationNotDeletedTimes))
                .markLocationNotDeleted(anyString(), anyString(), anyInt());
    }
}
