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
import ru.yandex.market.wms.scheduler.dao.ArchiveSorterDimensionsControlDao;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchiveSorterDimensionsControlServiceTest {
    @InjectMocks
    private ArchiveSorterDimensionsControlService archiveService;

    @Mock
    private ArchiveSorterDimensionsControlDao dao;

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
    void executeWhenIdsListLessThanPartitionSizeTest() throws InterruptedException {
        List<String> ids = asList("1", "5");
        List<String> unarchivedIds = Collections.singletonList("1");
        String expectedResult = "Records moved to SCPRDARC: SORTERDIMENSIONSCONTROL: 7";
        when(dao.prepareSorterDimensionIds(anyInt(), anyInt(), anyInt()))
                .thenReturn(ids)
                .thenReturn(Collections.emptyList());
        when(dao.prepareUnarchivedSorterDimensionIds(any(), anyInt())).thenReturn(unarchivedIds);
        when(dao.copyAndDeleteSorterDimensions(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(7);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_SORTER_DIMENSIONS_CONTROL_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(10);

        String actualResult = archiveService.execute();

        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1, 1, 1);
    }

    @Test
    void executeWhenIdsListGreaterThanPartitionSizeTest() throws InterruptedException {
        List<String> ids = asList("1", "5");
        List<String> unarchivedIds = Collections.emptyList();
        String expectedResult = "Records moved to SCPRDARC: SORTERDIMENSIONSCONTROL: 14";

        when(dao.prepareSorterDimensionIds(anyInt(), anyInt(), anyInt()))
                .thenReturn(ids)
                .thenReturn(Collections.emptyList());
        when(dao.prepareUnarchivedSorterDimensionIds(any(), anyInt())).thenReturn(unarchivedIds);
        when(dao.copyAndDeleteSorterDimensions(any(), any(), anyInt(), anyBoolean()))
                .thenReturn(7);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_SORTER_DIMENSIONS_CONTROL_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(1);

        String actualResult = archiveService.execute();

        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(2, 2, 2);
    }

    @Test
    void executeWhenDaoThrowsQueryTimeoutExceptionTest() throws InterruptedException {
        List<String> ids = asList("1", "5", "6", "7", "8");
        List<String> unarchivedIds = Collections.emptyList();
        String expectedResult = "Records moved to SCPRDARC: SORTERDIMENSIONSCONTROL: 7 number of failed attempts: 2";

        when(dao.prepareSorterDimensionIds(anyInt(), anyInt(), anyInt())).thenReturn(ids);
        when(dao.prepareUnarchivedSorterDimensionIds(any(), anyInt()))
                .thenThrow(new QueryTimeoutException("Message"))
                .thenReturn(unarchivedIds);
        when(dao.copyAndDeleteSorterDimensions(any(), any(), anyInt(), anyBoolean()))
                .thenThrow(new QueryTimeoutException("Message"))
                .thenReturn(7);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_SORTER_DIMENSIONS_CONTROL_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(15);

        String actualResult = archiveService.execute();

        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(3, 3, 2);
    }

    @Test
    void executeWhenKeysNotFoundTest() throws InterruptedException {
        List<String> ids = Collections.emptyList();
        String expectedResult = "Records moved to SCPRDARC: SORTERDIMENSIONSCONTROL: 0";

        when(dao.prepareSorterDimensionIds(anyInt(), anyInt(), anyInt())).thenReturn(ids);

        String actualResult = archiveService.execute();

        Assertions.assertEquals(expectedResult, actualResult);
        verifyDaoMock(1, 0, 0);
    }

    private void verifyDaoMock(
            int prepareSorterDimensionIdsTimes,
            int prepareUnarchivedSorterDimensionIdsTimes,
            int copyAndDeleteOrderRelatedTimes
    ) {
        verify(dao, times(prepareSorterDimensionIdsTimes)).prepareSorterDimensionIds(anyInt(), anyInt(), anyInt());
        verify(dao, times(prepareUnarchivedSorterDimensionIdsTimes)).prepareUnarchivedSorterDimensionIds(anyList(),
                anyInt());
        verify(dao, times(copyAndDeleteOrderRelatedTimes))
                .copyAndDeleteSorterDimensions(anyList(), anyList(), anyInt(), anyBoolean());
    }

    private void setUpConfigServiceMock() {
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_SORTER_DIMENSIONS_CONTROL_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(BATCH_SIZE);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_SORTER_DIMENSIONS_CONTROL_PERIOD_DAYS"),
                anyInt(), anyInt(), anyInt())).thenReturn(DAYS);
        when(dbConfigService.getConfigAsIntegerBetween(eq("SEL_SORTER_DIMENSIONS_CONTROL_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(SELECTION_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_SORTER_DIMENSIONS_CONTROL_ALLOWED"),
                anyInt(), anyInt(), anyInt())).thenReturn(DELETE_ALLOWED);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_SORTER_DIMENSIONS_CONTROL_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(ARCHIVING_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_SORTER_DIMENSIONS_CONTROL_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(10000);
    }
}
