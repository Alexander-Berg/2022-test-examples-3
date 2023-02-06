package ru.yandex.market.wms.scheduler.service.archive;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.QueryTimeoutException;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.scheduler.dao.ArchiveTransportUnitTrackingDao;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchiveTransportUnitTrackingServiceTest {
    @InjectMocks
    private ArchiveTransportUnitTrackingService archiveService;

    @Mock
    private ArchiveTransportUnitTrackingDao dao;

    @Mock
    private DbConfigService dbConfigService;

    private static final int BATCH_SIZE = 10;
    private static final int DAYS = 0;
    private static final int SELECTION_TIMEOUT = 5;
    private static final int ARCHIVING_TIMEOUT = 3;
    private static final int DELETE_ALLOWED = 1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        setUpConfigServiceMock();
    }

    @Test
    void executeWhenKeysListLessThanPartitionSizeTest() throws InterruptedException {
        List<String> keys = asList("1", "5");
        List<String> unarchivedkeys = Collections.singletonList("1");
        String expectedResult = "Records moved to SCPRDARC: TRANSPORT_UNIT_TRACKING: 7";
        when(dao.prepareUnitTrackingKeys(anyInt(), anyInt(), anyInt()))
                .thenReturn(keys)
                .thenReturn(Collections.emptyList());
        when(dao.prepareUnarchivedUnitTrackingKeys(any(), anyInt())).thenReturn(unarchivedkeys);
        when(dao.copyAndDeleteOrderRelated(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(7);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_UNIT_TRACKING_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(10);

        String actualResult = archiveService.execute();

        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1, 1, 1);
    }

    @Test
    void executeWhenKeysListGreaterThanPartitionSizeTest() throws InterruptedException {
        List<String> keys = asList("1", "5");
        List<String> unarchivedkeys = Collections.emptyList();
        String expectedResult = "Records moved to SCPRDARC: TRANSPORT_UNIT_TRACKING: 14";
        when(dao.prepareUnitTrackingKeys(anyInt(), anyInt(), anyInt()))
                .thenReturn(keys)
                .thenReturn(Collections.emptyList());
        when(dao.prepareUnarchivedUnitTrackingKeys(any(), anyInt())).thenReturn(unarchivedkeys);
        when(dao.copyAndDeleteOrderRelated(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(7);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_UNIT_TRACKING_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(1);

        String actualResult = archiveService.execute();

        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(2, 2, 2);
    }

    @Test
    void executeWhenDaoThrowsQueryTimeoutExceptionTest() throws InterruptedException {
        List<String> keys = asList("1", "5", "6", "7", "8");
        List<String> unarchivedkeys = Collections.emptyList();
        String expectedResult = "Records moved to SCPRDARC: TRANSPORT_UNIT_TRACKING: 7 number of failed attempts: 2";
        when(dao.prepareUnitTrackingKeys(anyInt(), anyInt(), anyInt())).thenReturn(keys);
        when(dao.prepareUnarchivedUnitTrackingKeys(any(), anyInt()))
                .thenThrow(new QueryTimeoutException("Message"))
                .thenReturn(unarchivedkeys);
        when(dao.copyAndDeleteOrderRelated(any(), any(), anyInt(), anyBoolean()))
                .thenThrow(new QueryTimeoutException("Message"))
                .thenReturn(7);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_UNIT_TRACKING_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(15);

        String actualResult = archiveService.execute();

        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(3, 3, 2);
    }

    @Test
    void executeWhenKeysNotFoundTest() throws InterruptedException {
        List<String> keys = Collections.emptyList();
        String expectedResult = "Records moved to SCPRDARC: TRANSPORT_UNIT_TRACKING: 0";
        when(dao.prepareUnitTrackingKeys(anyInt(), anyInt(), anyInt())).thenReturn(keys);

        String actualResult = archiveService.execute();

        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1, 0, 0);
    }

    private void verifyDaoMock(
            int prepareUnitTrackingKeysTimes,
            int prepareUnarchivedUnitTrackingKeysTimes,
            int copyAndDeleteOrderRelatedTimes
    ) {
        verify(dao, times(prepareUnitTrackingKeysTimes)).prepareUnitTrackingKeys(anyInt(), anyInt(), anyInt());
        verify(dao, times(prepareUnarchivedUnitTrackingKeysTimes)).prepareUnarchivedUnitTrackingKeys(anyList(),
                anyInt());
        verify(dao, times(copyAndDeleteOrderRelatedTimes))
                .copyAndDeleteOrderRelated(anyList(), anyList(), anyInt(), anyBoolean());
    }

    private void setUpConfigServiceMock() {
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_UNIT_TRACKING_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(BATCH_SIZE);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_UNIT_TRACKING_PERIOD_DAYS"),
                anyInt(), anyInt(), anyInt())).thenReturn(DAYS);
        when(dbConfigService.getConfigAsIntegerBetween(eq("SEL_UNIT_TRACKING_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(SELECTION_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_UNIT_TRACKING_ALLOWED"),
                anyInt(), anyInt(), anyInt())).thenReturn(DELETE_ALLOWED);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_UNIT_TRACKING_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(ARCHIVING_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_UNIT_TRACKING_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(10000);
    }
}
