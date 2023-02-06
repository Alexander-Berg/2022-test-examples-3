package ru.yandex.market.mboc.tms.executors.dbs;

import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.services.dsbs.OldFashionReimportService;
import ru.yandex.market.mboc.common.services.dsbs.ReimportTask;
import ru.yandex.market.mboc.common.services.dsbs.YtReimportReader;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

/**
 * @author apluhin
 * @created 2/10/22
 */
public class OldDsbsReimportExecutorTest extends BaseDbTestClass {

    private static final String KEY = "OldFashionDsbsReimportExecutor.last_upload_date";

    private OldFashionReimportService oldFashionReimportService;
    private YtReimportReader ytReimportReader;
    @Autowired
    private StorageKeyValueService storageKeyValueService;

    private OldDsbsReimportExecutor oldDsbsReimportExecutor;

    @Before
    public void setUp() throws Exception {
        oldFashionReimportService = Mockito.mock(OldFashionReimportService.class);
        ytReimportReader = Mockito.mock(YtReimportReader.class);
        oldDsbsReimportExecutor = new OldDsbsReimportExecutor(
            oldFashionReimportService,
            ytReimportReader,
            storageKeyValueService
        );
        storageKeyValueService.putValue(OldDsbsReimportExecutor.ENABLED_REIMPORT_KEY, true);
    }

    @Test
    public void testReimportCompletedDate() {
        storageKeyValueService.putValue(OldDsbsReimportExecutor.DATED_REIMPORT_PATH_KEY, "test");
        storageKeyValueService.putValue(KEY, LocalDate.now());
        oldDsbsReimportExecutor.execute();
        Mockito.verify(ytReimportReader, Mockito.times(0)).nextReimportTask(Mockito.any(), Mockito.any());
    }

    @Test
    public void testReimportNotCompletedDate() {
        storageKeyValueService.putValue(OldDsbsReimportExecutor.DATED_REIMPORT_PATH_KEY, "test");
        List<Long> offers = List.of(1L);
        Mockito.when(ytReimportReader.nextReimportTask(Mockito.any(), Mockito.any())).thenReturn(
            new ReimportTask(
                "2022-01-01",
                offers
            )
        );
        oldDsbsReimportExecutor.execute();
        Mockito.verify(oldFashionReimportService, Mockito.times(1))
            .reimportOfferKeys(Mockito.eq(offers));
        Mockito.verify(oldFashionReimportService, Mockito.times(1))
            .clearOffset();
        Assertions.assertThat(
            storageKeyValueService.getString(KEY, null)
        ).isNotNull();
    }

    @Test
    public void testReimportFullPaths() {
        List<Long> offers = List.of(1L);
        storageKeyValueService.putValue(OldDsbsReimportExecutor.FULL_REIMPORT_PATH_KEY, "test1");
        storageKeyValueService.invalidateCache();
        var fullPath = "//home/market/production/mbo/mboc/test1";

        Mockito.when(ytReimportReader.nextReimportTaskByPath(fullPath)).thenReturn(
            new ReimportTask("2022-01-01", offers));
        oldDsbsReimportExecutor.execute();

        Mockito.verify(oldFashionReimportService, Mockito.times(1))
            .reimportOfferKeys(Mockito.eq(offers));
        Mockito.verify(oldFashionReimportService, Mockito.times(1))
            .clearOffset();
    }
}
