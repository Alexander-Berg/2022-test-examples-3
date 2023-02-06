package ru.yandex.market.toloka;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.markup2.core.stubs.persisters.YangAssignmentResultsPersisterStub;
import ru.yandex.market.markup2.core.stubs.persisters.YangResultsPoolStatusPersisterStub;
import ru.yandex.market.markup2.core.stubs.persisters.YangTaskToDataItemsPersisterStub;
import ru.yandex.market.markup2.dao.YangAssignmentResultsPersister;
import ru.yandex.market.markup2.dao.YangResultsPoolStatusPersister;
import ru.yandex.market.markup2.dao.YangTaskToDataItemsPersister;
import ru.yandex.market.markup2.entries.yang.YangPoolStatusInfo;
import ru.yandex.market.markup2.utils.Mocks;
import ru.yandex.market.markup2.workflow.TaskProcessManager;
import ru.yandex.market.toloka.model.Pool;
import ru.yandex.market.toloka.model.PoolCloseReason;
import ru.yandex.market.toloka.model.PoolStatus;
import ru.yandex.market.toloka.model.ResultItemStatus;

/**
 * @author york
 * @since 29.06.2020
 */
public class YangResultsDownloaderTest {
    private static int idSeq = 1;
    private YangResultsDownloaderStub yangResultsDownloader;
    private YangTaskToDataItemsPersister yangTaskToDataItemsPersister;
    private YangAssignmentResultsPersister yangAssignmentResultsPersister;
    private YangResultsPoolStatusPersister yangResultsPoolStatusPersister;
    private TolokaApi tolokaApiSpy;
    private TolokaApiStub tolokaApiInternal;
    private Date firstSubmittedDate;
    private Date firstSubmittedDateUpper;
    private Date now = new Date();

    @Before
    public void setUp() {
        TransactionTemplate transactionTemplate = Mockito.mock(TransactionTemplate.class);
        Mockito.when(transactionTemplate.execute(Mockito.any())).then(invocation -> {
            TransactionCallback callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        yangTaskToDataItemsPersister = new YangTaskToDataItemsPersisterStub();
        yangAssignmentResultsPersister = new YangAssignmentResultsPersisterStub();
        yangResultsPoolStatusPersister = new YangResultsPoolStatusPersisterStub();
        tolokaApiInternal = new TolokaApiStub(null);
        tolokaApiSpy = Mockito.spy(tolokaApiInternal);
        firstSubmittedDate = new Date(1600361449806L);
        firstSubmittedDateUpper = new Date(firstSubmittedDate.getTime()
                + TimeUnit.HOURS.toMillis(YangResultsDownloader.MAX_DOWNLOAD_INTERVAL_HOURS));

        yangResultsDownloader = new YangResultsDownloaderStub() {
            @Override
            protected String getCurrentSubmittedTs() {
                return TolokaApi.DATE_FORMAT.format(now);
            }
        };
        yangResultsDownloader.setFirstSubmittedDate(firstSubmittedDate);
        yangResultsDownloader.setTolokaApi(tolokaApiSpy);
        yangResultsDownloader.setExecutorService(Mocks.instantExecutorService());
        yangResultsDownloader.setTaskProcessManager(Mockito.mock(TaskProcessManager.class));
        yangResultsDownloader.setYangAssignmentResultsPersister(yangAssignmentResultsPersister);
        yangResultsDownloader.setTransactionTemplate(transactionTemplate);
        yangResultsDownloader.setYangTaskToDataItemsPersister(yangTaskToDataItemsPersister);
        yangResultsDownloader.setYangResultsPoolStatusPersister(yangResultsPoolStatusPersister);
    }

    @Test
    public void testRequestedPoolIsPersisted() {
        Pool pool = createPool();
        YangPoolStatusInfo yangPoolStatusInfo = yangResultsDownloader.getYangPoolStatusInfo(pool.getId());
        Assertions.assertThat(yangPoolStatusInfo).isNotNull();
        Assertions.assertThat(yangPoolStatusInfo.getLastCloseReason()).isNull();
        Assertions.assertThat(yangPoolStatusInfo.getStatus()).isEqualTo(pool.getStatus());
        Assertions.assertThat(yangResultsPoolStatusPersister.getValue(pool.getId())).isEqualTo(yangPoolStatusInfo);
    }

    @Test
    public void testRequestedPoolIsDownloaded() {
        Pool pool = createPool();
        yangResultsDownloader.getYangPoolStatusInfo(pool.getId());
        yangResultsDownloader.downloadResultsAllPools();


        Mockito.verify(tolokaApiSpy).getResult(Mockito.eq(pool.getId()), Mockito.eq(Optional.empty()),
                Mockito.eq(Optional.of(TolokaApi.DATE_FORMAT.format(firstSubmittedDate))),
                Mockito.eq(Optional.of(TolokaApi.DATE_FORMAT.format(firstSubmittedDateUpper))),
                Mockito.eq(ResultItemStatus.SUBMITTED), Mockito.eq(ResultItemStatus.ACCEPTED));
    }

    @Test
    public void testStartingForActivePools() {
        Pool pool1 = createPool();
        Pool pool2 = createPool();
        pool2.setStatus(PoolStatus.CLOSED);
        pool2.setLastCloseReason(PoolCloseReason.COMPLETED);

        yangResultsPoolStatusPersister.persist(YangResultsDownloader.convert(pool1, true));
        yangResultsPoolStatusPersister.persist(YangResultsDownloader.convert(pool2, true));

        yangResultsDownloader.onStart();
        yangResultsDownloader.downloadResultsAllPools();
        Mockito.verify(tolokaApiSpy).getPoolInfo(Mockito.eq(pool1.getId()));

        Mockito.verify(tolokaApiSpy).getResult(Mockito.eq(pool1.getId()), Mockito.eq(Optional.empty()),
                Mockito.eq(Optional.empty()),
                Mockito.eq(Optional.empty()),
                Mockito.eq(ResultItemStatus.ACTIVE));

        Mockito.verify(tolokaApiSpy).getResult(Mockito.eq(pool1.getId()), Mockito.eq(Optional.empty()),
                Mockito.eq(Optional.of(TolokaApi.DATE_FORMAT.format(firstSubmittedDate))),
                Mockito.eq(Optional.of(TolokaApi.DATE_FORMAT.format(firstSubmittedDateUpper))),
                Mockito.eq(ResultItemStatus.SUBMITTED), Mockito.eq(ResultItemStatus.ACCEPTED));

        Mockito.verifyNoMoreInteractions(tolokaApiSpy);
    }

    @Test
    public void testUpdatedSubmittedTSLongAgo() {
        Pool pool = createPool();
        tolokaApiInternal.createPool(pool);
        yangResultsDownloader.getYangPoolStatusInfo(pool.getId());

        yangResultsDownloader.downloadResultsAllPools();

        Mockito.verify(tolokaApiSpy).getResult(Mockito.eq(pool.getId()), Mockito.eq(Optional.empty()),
                Mockito.eq(Optional.of(TolokaApi.DATE_FORMAT.format(firstSubmittedDate))),
                Mockito.eq(Optional.of(TolokaApi.DATE_FORMAT.format(firstSubmittedDateUpper))),
                Mockito.eq(ResultItemStatus.SUBMITTED), Mockito.eq(ResultItemStatus.ACCEPTED));

        yangResultsDownloader.downloadResultsAllPools();

        YangPoolStatusInfo p = new YangPoolStatusInfo(0, true);
        p.setLastSubmittedTS(TolokaApi.DATE_FORMAT.format(firstSubmittedDateUpper));
        String ts = yangResultsDownloader.getLastSubmitted(p);

        Mockito.verify(tolokaApiSpy).getResult(Mockito.eq(pool.getId()), Mockito.eq(Optional.empty()),
                Mockito.eq(Optional.of(ts)),
                Mockito.any(Optional.class),
                Mockito.eq(ResultItemStatus.SUBMITTED), Mockito.eq(ResultItemStatus.ACCEPTED));
    }

    @Test
    public void testUpdatedSubmittedTSRecent() {
        Pool pool = createPool();
        tolokaApiInternal.createPool(pool);
        yangResultsDownloader.getYangPoolStatusInfo(pool.getId());
        firstSubmittedDate = new Date();
        firstSubmittedDateUpper = new Date(firstSubmittedDate.getTime()
                + TimeUnit.HOURS.toMillis(YangResultsDownloader.MAX_DOWNLOAD_INTERVAL_HOURS));
        yangResultsDownloader.setFirstSubmittedDate(firstSubmittedDate);

        yangResultsDownloader.downloadResultsAllPools();

        Mockito.verify(tolokaApiSpy).getResult(Mockito.eq(pool.getId()), Mockito.eq(Optional.empty()),
                Mockito.eq(Optional.of(TolokaApi.DATE_FORMAT.format(firstSubmittedDate))),
                Mockito.eq(Optional.of(TolokaApi.DATE_FORMAT.format(firstSubmittedDateUpper))),
                Mockito.eq(ResultItemStatus.SUBMITTED), Mockito.eq(ResultItemStatus.ACCEPTED));

        YangPoolStatusInfo p = new YangPoolStatusInfo(0, true);
        p.setLastSubmittedTS(TolokaApi.DATE_FORMAT.format(now));
        String ts = yangResultsDownloader.getLastSubmitted(p);

        yangResultsDownloader.downloadResultsAllPools();

        Mockito.verify(tolokaApiSpy).getResult(Mockito.eq(pool.getId()), Mockito.eq(Optional.empty()),
                Mockito.eq(Optional.of(ts)),
                Mockito.any(Optional.class),
                Mockito.eq(ResultItemStatus.SUBMITTED), Mockito.eq(ResultItemStatus.ACCEPTED));
    }

    private Pool createPool() {
        Pool pool = new Pool();
        pool.setId(idSeq++);
        pool.setStatus(PoolStatus.OPEN);
        tolokaApiInternal.createPool(pool);
        return pool;
    }
}
