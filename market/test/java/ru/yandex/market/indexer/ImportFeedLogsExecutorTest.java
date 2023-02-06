package ru.yandex.market.indexer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableMap;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.apache.commons.io.FileUtils;
import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.Magics;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.mds.s3.client.content.consumer.FileContentConsumer;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.feed.FeedService;
import ru.yandex.market.core.feed.model.FeedSiteType;
import ru.yandex.market.core.indexer.db.generation.IdxGenerationService;
import ru.yandex.market.core.indexer.db.meta.GenerationMetaService;
import ru.yandex.market.core.indexer.db.session.FeedSessionService;
import ru.yandex.market.core.indexer.feedlog.FeedLogHelper;
import ru.yandex.market.core.notification.model.data.FeedLogNotificationContainer;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.util.io.Protobuf;
import ru.yandex.market.indexer.event.FeedEventHandler;
import ru.yandex.market.indexer.event.IndexedFeedEvent;
import ru.yandex.market.indexer.event.NotInIndexFeedEvent;
import ru.yandex.market.indexer.mds.MdsIndexerService;
import ru.yandex.market.indexer.problem.FeedIndexationInfoCollectionMatcher;
import ru.yandex.market.indexer.problem.ProblemFeedsProcessor;
import ru.yandex.market.indexer.problem.strategy.impl.CommonFeedsStrategy;
import ru.yandex.market.indexer.yt.generation.YtErrorsProviderFactory;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.db.DbUtil;
import ru.yandex.market.notification.exception.NotificationException;
import ru.yandex.market.proto.indexer.v2.FeedLog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@DbUnitDataBaseConfig(@DbUnitDataBaseConfig.Entry(
        name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
        value = "true"
))
@DbUnitDataSet(before = "data/ImportFeedLogsExecutorTest.before.csv")
@ExtendWith(MockitoExtension.class)
class ImportFeedLogsExecutorTest extends FunctionalTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final ImmutableMap<String, FeedLogFile> FEEDLOG_FILES = ImmutableMap.<String, FeedLogFile>builder()
            // для теста {@code oneGenExecutionTest}
            // для меты id=1231
            .put("yellow.gibson.full.1546300860.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getOneGenTestFmcgFeedlog))
            // для теста {@code twoGensExecutionTest}
            // для меты id=1232
            .put("yellow.stratocaster.full.1546819260.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getTwoGensTestFmcgFeedlog1))
            // для меты id=1233
            .put("yellow.gibson.full.1547078460.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getTwoGensTestFmcgFeedlog2))
            // для теста {@code errorWhileExecutionTest}
            // для меты id=1234
            .put("yellow.gibson.full.1548892860.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getFmcgFeedlogWithBrokenFeeds))
            // для меты id=1235
            .put("gibson.full.1548979260.pbuf.sn", FeedLogFile.of(ImportFeedLogsExecutorTest::getShopFeedlog))
            // для меты id=1236
            .put("stratocaster.full.1548986460.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getShopFeedlogWithBrokenFeeds))
            // для теста {@code workWithFeedSessionsTest}
            // для меты id=1401
            .put("gibson.full.1556755200.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getShopFeedlogWithParseLogs1))
            // для меты id=1402
            .put("gibson.full.1556773200.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getShopFeedlogWithParseLogs2))
            // для меты id=1202
            .put("stratocaster.full.1546178400.pbuf.sn", FeedLogFile.of(ImportFeedLogsExecutorTest::getOkWhiteFeedLog))
            // для меты id=1203
            .put("stratocaster.full.1545573600.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getPushWhiteFeedLog))
            // для меты id=1205
            .put("stratocaster.full.1542204000.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getOnlyLastWhiteFeedLog))
            //для меты id=1600
            .put("stratocaster.full.1546117200.pbuf.sn", FeedLogFile.of(ImportFeedLogsExecutorTest::getOkWhiteFeedLog))
            //для меты id=1601
            .put("stratocaster.full.1546117201.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getWarnWhiteFeedLog))
            //для меты id=1602
            .put("stratocaster.full.1546117202.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getErrorWhiteFeedLog))
            //для меты id=1603
            .put("stratocaster.full.1546117203.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getWarnWhiteFeedLog))
            //для меты id=1604
            .put("stratocaster.full.1546117204.pbuf.sn", FeedLogFile.of(ImportFeedLogsExecutorTest::getOkWhiteFeedLog))
            //для меты id=1605
            .put("stratocaster.full.1546117205.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getWarnWhiteFeedLog))
            //для меты id=1606
            .put("stratocaster.full.1546117212.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getErrorDirectFeedLog))
            //для меты id=1701
            .put("stratocaster.diff.1546783200.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getValidWhiteDiffFeedLog))
            //для меты id=1801
            .put("blue.stratocaster.full.1546092240.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getEmptyFeedLog))
            //для меты id=1901
            .put("stratocaster.full.1545660000.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getValidWhiteWithVirtualFeedLog))
            //для меты id=2001
            .put("stratocaster.full.1545746400.pbuf.sn",
                    FeedLogFile.of(ImportFeedLogsExecutorTest::getValidWhiteWithCachedFeedLog))
            .build();


    @Autowired
    private MdsIndexerService mdsIndexerService;

    @Autowired
    private GenerationMetaService generationMetaService;

    @Autowired
    private IdxGenerationService idxGenerationService;

    @Autowired
    private FeedService feedService;

    @Autowired
    private FeedSessionService feedSessionService;

    @Autowired
    @Spy
    private FeedEventHandler feedEventHandler;

    @Value("${market.mbi.feedlogs.executor.queue.limit}")
    private int executorQueueLimit;

    @Value("${market.mbi.feedlogs.executor.threads.num}")
    private int executorThreadsNum;

    @Autowired
    @Qualifier("feedlogResourceLocationFactory")
    private ResourceLocationFactory resourceLocationFactory;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private ParamService paramService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CommonFeedsStrategy commonFeedsStrategy;

    @Autowired
    private BalanceService impatientBalanceService;

    @Autowired
    private YtErrorsProviderFactory ytErrorsProviderFactory;

    @Autowired
    private EnvironmentService environmentService;

    private ImportFeedLogsExecutor importFeedLogsExecutor;

    private static File getEmptyFeedLog() {
        return getFeedlogFile();
    }

    private static File getValidWhiteWithVirtualFeedLog() {
        return getFeedlogFile(
                // Обычный белый
                "validWhiteWithVirtual/10.json",

                // Красный виртуальный
                "validWhiteWithVirtual/546151.json",

                // Фиолетовый виртуальный
                "validWhiteWithVirtual/655301.json"
        );
    }

    private static File getValidWhiteWithCachedFeedLog() {
        return getFeedlogFile(
                // Белый фид, который скачивался, но не парсился
                "validWhiteWithCached/100.json",

                // Белый фид, который не скачивался и не парсился
                "validWhiteWithCached/200.json",

                // Белый фид, который не скачивался и не парсился
                "validWhiteWithCached/300.json"
        );
    }

    private static File getOkWhiteFeedLog() {
        return getFeedlogFile(
                "validWhite/10.json",
                // дефолтный фид
                "validWhite/222.json"
        );
    }

    private static File getPushWhiteFeedLog() {
        return getFeedlogFile(
                "whitePush/10.json",
                // дефолтный фид
                "whitePush/101.json",
                "whitePush/300101.json",
                "whitePush/400101.json",
                "whitePush/500101.json"
        );
    }

    private static File getErrorWhiteFeedLog() {
        return getFeedlogFile(
                "errorWhite/10.json"
        );
    }

    private static File getErrorDirectFeedLog() {
        return getFeedlogFile(
                "direct/direct.json"
        );
    }
    private static File getWarnWhiteFeedLog() {
        return getFeedlogFile(
                "warnWhite/10.json"
        );
    }

    private static File getOnlyLastWhiteFeedLog() {
        return getFeedlogFile(
                "onlyLastWarnWhite/10.json"
        );
    }

    private static File getValidWhiteDiffFeedLog() {
        return getFeedlogFile(
                "validWhiteDiff/10.json"
        );
    }

    private static File getOneGenTestFmcgFeedlog() {
        return getFeedlogFile(
                // в предыдущем поколении был OK, в этом поколении WARN (parse_retcode=1)
                "oneGenTestFmcg/13.json",

                // в предыдущем поколении был OK, в этом поколении Error, кэшированной сессии нет
                // (indexed_status="failed")
                "oneGenTestFmcg/14.json",

                // в предыдущем поколении был WARN, в этом поколении FATAL при парсинге, есть кэшированная сессия
                "oneGenTestFmcg/15.json",

                // в предыдущем поколении был ERROR, в этом поколении FATAL при скачивании,
                "oneGenTestFmcg/16.json",

                // в предыдущем поколении был ERROR, в этом поколении OK
                "oneGenTestFmcg/17.json",

                // в предыдущем поколении был FATAL, в этом поколении WARN
                "oneGenTestFmcg/18.json",

                // это какой-то ненатуральный кейс, но я не придумала как еще протестировать "поколение забвения"
                // в предыдущем поколении был не в индексе, до этого был ERROR, в этом поколении ERROR,
                // но забываем предыдущие ошибки (lastNotFatalFullGenerationId < forgetGenerationId)
                "oneGenTestFmcg/19.json",

                // фид первый раз в индексе, FATAL при скачивании в последней сессии, опубликованных сессий нет
                "oneGenTestFmcg/20.json",

                // фид первый раз в индексе, FATAL при скачивании в последней сессии, но
                // до этого успели опубликовать хорошую сессию
                "oneGenTestFmcg/21.json",

                // фид первый раз в индексе, статус WARNING
                "oneGenTestFmcg/22.json",

                // фид не той среды (пример: индексатор в тестинге подмешивает продовые фиды)
                "oneGenTestFmcg/567123.json"

                // фид id=23 выпал из индекса
                // фиды id=8,9 были удалены
        );
    }

    private static File getTwoGensTestFmcgFeedlog1() {
        return getFeedlogFile(
                "twoGensTestFmcg1/13.json",
                "twoGensTestFmcg1/14.json",
                "twoGensTestFmcg1/15.json",
                "twoGensTestFmcg1/16.json"
        );
    }

    private static File getTwoGensTestFmcgFeedlog2() {
        return getFeedlogFile(
                "twoGensTestFmcg2/13.json",
                "twoGensTestFmcg2/14.json",
                "twoGensTestFmcg2/15.json",
                "twoGensTestFmcg2/16.json"
        );
    }

    private static File getFmcgFeedlogWithBrokenFeeds() {
        return getFeedlogFile(
                // делаем некоторые фидлоги некорректными - при попытке распарсить такие статусы
                // упадем с IllegalArgumentException
                new FeedLog.Feed[]{
                        FeedLogTestFactory.getCorrectFeedLogBase()
                                .setFeedId(13)
                                .setIndexation(FeedLog.ProcessingSummary.newBuilder().setStatus(FeedLog.Status.OK))
                                .build(),
                        FeedLogTestFactory.getCorrectFeedLogBase()
                                .setFeedId(14)
                                .setIndexation(FeedLog.ProcessingSummary.newBuilder().setStatus(FeedLog.Status.OK))
                                .setIndexedStatus("abracadabra")
                                .build(),
                        FeedLogTestFactory.getWarningFeedLogBase()
                                .setFeedId(15)
                                .setIndexation(FeedLog.ProcessingSummary.newBuilder().setStatus(FeedLog.Status.WARN))
                                .build(),
                        FeedLogTestFactory.getCorrectFeedLogBase()
                                .setFeedId(16)
                                .setIndexation(FeedLog.ProcessingSummary.newBuilder().setStatus(FeedLog.Status.OK))
                                .build(),
                        FeedLogTestFactory.getCorrectFeedLogBase()
                                .setFeedId(17)
                                .setIndexation(FeedLog.ProcessingSummary.newBuilder().setStatus(FeedLog.Status.OK))
                                .setIndexedStatus("invalidStatus")
                                .build(),
                        FeedLogTestFactory.getCorrectFeedLogBase()
                                .setFeedId(18)
                                .setIndexation(FeedLog.ProcessingSummary.newBuilder().setStatus(FeedLog.Status.OK))
                                .setIndexedStatus("blablabla")
                                .build(),
                });
    }

    private static File getShopFeedlog() {
        return getFeedlogFile(
                "shop/12.json",
                "shop/121.json",
                "shop/122.json",
                "shop/123.json"
        );
    }

    private static File getShopFeedlogWithBrokenFeeds() {
        return getFeedlogFile(
                // делаем некоторые фидлоги некорректными - при попытке распарсить такие статусы
                // упадем с IllegalArgumentException
                new FeedLog.Feed[]{
                        FeedLogTestFactory.getCorrectFeedLogBase()
                                .setFeedId(12)
                                .setIndexation(FeedLog.ProcessingSummary.newBuilder().setStatus(FeedLog.Status.OK))
                                .build(),
                        FeedLogTestFactory.getCorrectFeedLogBase()
                                .setFeedId(121)
                                .setIndexation(FeedLog.ProcessingSummary.newBuilder().setStatus(FeedLog.Status.OK))
                                .setIndexedStatus("invalid")
                                .build(),
                        FeedLogTestFactory.getCorrectFeedLogBase()
                                .setFeedId(122)
                                .setIndexation(FeedLog.ProcessingSummary.newBuilder().setStatus(FeedLog.Status.OK))
                                .setIndexedStatus("one-more-invalid")
                                .build(),
                        FeedLogTestFactory.getCorrectFeedLogBase()
                                .setFeedId(123)
                                .setIndexation(FeedLog.ProcessingSummary.newBuilder().setStatus(FeedLog.Status.OK))
                                .build(),
                });
    }

    private static File getShopFeedlogWithParseLogs1() {
        return getFeedlogFile(
                "shopFeedlogWithParseLogs1/1101.json",
                "shopFeedlogWithParseLogs1/1102.json",
                "shopFeedlogWithParseLogs1/1104.json",
                "shopFeedlogWithParseLogs1/1105.json",
                "shopFeedlogWithParseLogs1/1106.json",
                "shopFeedlogWithParseLogs1/1107.json"
        );
    }

    private static File getShopFeedlogWithParseLogs2() {
        return getFeedlogFile(
                "shopFeedlogWithParseLogs2/1101.json",
                "shopFeedlogWithParseLogs2/1102.json",
                "shopFeedlogWithParseLogs2/1104.json",
                "shopFeedlogWithParseLogs2/1105.json",
                "shopFeedlogWithParseLogs2/1106.json",
                "shopFeedlogWithParseLogs2/1107.json"
        );
    }

    static FeedLog.ProcessingSummary getProcessingSummary(FeedLog.Status status) {
        FeedLog.ProcessingSummary.Builder builder = FeedLog.ProcessingSummary.newBuilder();
        if (status != null) {
            builder.setStatus(status);
        }
        builder.setStatistics(getParseStats());
        return builder.build();
    }

    private static FeedLog.ParseStats getParseStats() {
        return FeedLog.ParseStats.newBuilder()
                .setTotalOffers(4)
                .setValidOffers(5)
                .setWarnOffers(6)
                .setErrorOffers(7)
                .build();
    }

    /**
     * Сгенерировать протобуфку на основе json-представлений.
     */
    private static File getFeedlogFile(String... feedlogs) {
        FeedLog.Feed[] feeds = Arrays.stream(feedlogs)
                .map(feedlog -> ProtoTestUtil.getProtoMessageByJson(
                        FeedLog.Feed.class,
                        "feedlog/" + feedlog,
                        ImportFeedLogsExecutorTest.class
                ))
                .toArray(FeedLog.Feed[]::new);

        return getFeedlogFile(feeds);
    }

    private static File getFeedlogFile(FeedLog.Feed[] feeds) {
        File feedlog = TempFileUtils.createTempFile();
        feedlog.deleteOnExit();
        try (OutputStream outputStream = new FileOutputStream(feedlog)) {
            byte[] outBytes =
                    Protobuf.messagesSnappyLenvalStreamBytes(Magics.MagicConstants.FLOG.name(), feeds);
            outputStream.write(outBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return feedlog;
    }

    @BeforeEach
    void init() {
        ProblemFeedsProcessor problemFeedsProcessor = new ProblemFeedsProcessor(
                feedService,
                ImmutableMap.of(
                        FeedSiteType.MARKET, commonFeedsStrategy
                ));
        importFeedLogsExecutor = new ImportFeedLogsExecutor(
                mdsIndexerService,
                generationMetaService,
                idxGenerationService,
                feedService,
                feedSessionService,
                paramService,
                problemFeedsProcessor,
                feedEventHandler,
                executorQueueLimit,
                executorThreadsNum,
                transactionTemplate,
                ytErrorsProviderFactory,
                environmentService);

        for (Map.Entry<String, FeedLogFile> feedlog : FEEDLOG_FILES.entrySet()) {
            String key = FeedLogHelper.FEEDLOG + KeyGenerator.DELIMITER_FOLDER + feedlog.getKey();
            ResourceLocation feedlogLocation = resourceLocationFactory.createLocation(key);
            when(mdsS3Client.download(eq(feedlogLocation), any(FileContentConsumer.class)))
                    .thenReturn(feedlog.getValue().create());
        }

        doNothing().when(mdsS3Client).delete(any(ResourceLocation.class), any(ResourceLocation.class));
        when(impatientBalanceService.getClient(anyLong())).thenReturn(null);
    }

    @AfterEach
    void finish() {
        FEEDLOG_FILES.values().stream().map(FeedLogFile::get).forEach(FileUtils::deleteQuietly);
    }

    /**
     * Убеждаемся что джоба работает штатно при отсутствии новых поколений. Синие еще на старом импорте.
     */
    @Test
    @DbUnitDataSet(
            before = "data/zeroGensExecutionTest.before.csv",
            after = "data/zeroGensExecutionTest.after.csv"
    )
    void zeroGensExecutionTest() {
        importFeedLogsExecutor.doJob(null);

        Mockito.verify(feedEventHandler, Mockito.never())
                .handle(any(NotInIndexFeedEvent.class));
        Mockito.verify(feedEventHandler, Mockito.never())
                .handle(any(IndexedFeedEvent.class));

        // не скачиваем меты
        Mockito.verify(mdsS3Client, Mockito.never())
                .download(any(ResourceLocation.class), any(FileContentConsumer.class));
    }

    /**
     * Проверяем работу джобы на одном поколении.
     * Сценарии прописаны в комментах к фидлогам.
     */
    @Test
    @DbUnitDataSet(
            before = "data/oneGenExecutionTest.before.csv",
            after = "data/oneGenExecutionTest.after.csv"
    )
    void oneGenExecutionTest() {
        importFeedLogsExecutor.doJob(null);

        Mockito.verify(feedEventHandler, Mockito.times(3))
                .handle(any(NotInIndexFeedEvent.class));
        Mockito.verify(feedEventHandler, Mockito.times(10))
                .handle(any(IndexedFeedEvent.class));

        Mockito.verify(mdsS3Client, Mockito.times(1))
                .download(any(ResourceLocation.class), any(FileContentConsumer.class));

        // желтое поколение, стратегии других площадок не вызываются
        Mockito.verify(commonFeedsStrategy, Mockito.never())
                .processProblemFeeds(anyCollection());

        Mockito.verify(paramService).setSystemParam(eq(ParamType.IS_IN_INDEX), contains("feed_log_history"));
        Mockito.verify(paramService, Mockito.never()).setSystemParam(eq(ParamType.IS_IN_TEST_INDEX), anyString());
    }

    /**
     * Проверяем работу джобы на двух последовательных поколениях.
     * Проверяются сценарии:
     * 1. У фида 13 давно не было опубликованных сессий, кэшированная выходит за TTL с поколения id=1233.
     * 2. Фид 14 индексируется с warnings, в последней индексации фид не поменялся (т. е. parse_retcode=0 и
     * cached_parse_retcode=1). Проверяется что варнинг не мигает.
     * 3. Фид 15 в поколении N был с indexed_status: "failed", а в поколении N+1 пришел с indexed_status: "cached".
     * Это произошло следующим образом: после индексации в поколении N магазин поправил ошибки и изменение попало под
     * первое скачивание фида, а в поколение N+1 попало второе скачивание фида, когда магазин ответил
     * что фид не изменился.
     * 4. Фид 16 в поколении N пришел с indexed_status: "cached" из-за фатала в последней сессии, в поколении N+1 все
     * ок.
     * 5. Фиды 17 и 18 (одного магазина) выпадают из индекса в поколении N, проверяем что в поколении N+1
     * новых записей о них не появляется.
     */
    @Test
    @DbUnitDataSet(
            before = "data/twoGensExecutionTest.before.csv",
            after = "data/twoGensExecutionTest.after.csv"
    )
    void twoGensExecutionTest() {
        importFeedLogsExecutor.doJob(null);

        Mockito.verify(feedEventHandler, Mockito.times(2))
                .handle(any(NotInIndexFeedEvent.class));
        Mockito.verify(feedEventHandler, Mockito.times(8))
                .handle(any(IndexedFeedEvent.class));

        Mockito.verify(mdsS3Client, Mockito.times(2))
                .download(any(ResourceLocation.class), any(FileContentConsumer.class));

        Mockito.verify(paramService, Mockito.times(2))
                .setSystemParam(eq(ParamType.IS_IN_INDEX), contains("feed_log_history")); // after every gen
        Mockito.verify(paramService, Mockito.never()).setSystemParam(eq(ParamType.IS_IN_TEST_INDEX), any());
    }

    /**
     * В случае, если по какой-то причине в диффе пришел новый фид,
     * то сохраняем для него только feed_log_history. feed_status не создаем. Кейс возможен,
     * например, когда индексатор не смог в mbi загрузить полное поколение из-за таймаутов.
     * А потом загрузил дифф. Поведение перенесено со старого алгоритма импорта.
     */
    @Test
    @DbUnitDataSet(
            before = "data/diffWithNewFeedTest.before.csv",
            after = "data/diffWithNewFeedTest.after.csv"
    )
    void diffWithNewFeedTest() {
        importFeedLogsExecutor.doJob(null);

        Mockito.verify(feedEventHandler, Mockito.never())
                .handle(any(NotInIndexFeedEvent.class));
        Mockito.verify(feedEventHandler, Mockito.times(1))
                .handle(any(IndexedFeedEvent.class));

        Mockito.verify(mdsS3Client, Mockito.times(1))
                .download(any(ResourceLocation.class), any(FileContentConsumer.class));

        Mockito.verifyNoMoreInteractions(feedEventHandler, mdsS3Client);
    }

    /**
     * Если полное поколения для фида было FATAL, то следующий diff все равно все должен загружаться нормально.
     */
    @Test
    @DbUnitDataSet(
            before = "data/diffAfterFirstFailTest.before.csv",
            after = "data/diffAfterFirstFailTest.after.csv"
    )
    void diffAfterFirstFailTest() {
        importFeedLogsExecutor.doJob(null);

        Mockito.verify(feedEventHandler, Mockito.never())
                .handle(any(NotInIndexFeedEvent.class));
        Mockito.verify(feedEventHandler, Mockito.times(1))
                .handle(any(IndexedFeedEvent.class));

        Mockito.verify(mdsS3Client, Mockito.times(1))
                .download(any(ResourceLocation.class), any(FileContentConsumer.class));

        Mockito.verify(ytErrorsProviderFactory, Mockito.never()).create(any());
        Mockito.verifyNoMoreInteractions(feedEventHandler, mdsS3Client, ytErrorsProviderFactory);
    }

    /**
     * Фид не должен повторно импортироваться для одного поколения.
     * Например, повторная попытка после неудачного импорта.
     */
    @Test
    @DbUnitDataSet(
            before = "data/skipAlreadyImportedFeedsTest.before.csv",
            after = "data/skipAlreadyImportedFeedsTest.after.csv"
    )
    void skipAlreadyImportedFeedsTest() {
        importFeedLogsExecutor.doJob(null);

        Mockito.verify(feedEventHandler, Mockito.never())
                .handle(any(NotInIndexFeedEvent.class));
        Mockito.verify(feedEventHandler, Mockito.times(1))
                .handle(any(IndexedFeedEvent.class));

        Mockito.verify(mdsS3Client, Mockito.times(1))
                .download(any(ResourceLocation.class), any(FileContentConsumer.class));

        Mockito.verifyNoMoreInteractions(feedEventHandler, mdsS3Client);
    }

    /**
     * Должны импортироваться только разрешенные виртуальные фиды.
     * Если цвета нет в списке разрешенных, то такие виртуальные фиды импортироваться не будут.
     * В тесте импортируем белый фидлог, в котором есть фиолетовый и синий виртуальные фиды.
     * В данный момент никакие виртуальные фиды полученные от индексаторы не должны импортироваться.
     */
    @Test
    @DbUnitDataSet(
            before = "data/importOnlyAllowedVirtualFeedsTest.before.csv",
            after = "data/importOnlyAllowedVirtualFeedsTest.after.csv"
    )
    void importOnlyAllowedVirtualFeedsTest() {
        importFeedLogsExecutor.doJob(null);

        Mockito.verify(feedEventHandler, Mockito.never())
                .handle(any(NotInIndexFeedEvent.class));
        Mockito.verify(feedEventHandler, Mockito.times(2))
                .handle(any(IndexedFeedEvent.class));

        Mockito.verify(mdsS3Client, Mockito.times(1))
                .download(any(ResourceLocation.class), any(FileContentConsumer.class));

        Mockito.verifyNoMoreInteractions(feedEventHandler, mdsS3Client);
    }

    /**
     * Проверяем работу джобы в случае, когда при обработке некоторых фидлогов поколения были брошены исключения.
     * Все хорошие (не сломанные) фидлоги должны обработаться и записаться в бд нормально, но по завершению
     * джоба должна кинуть exception и не должна помечать поколение проимпортированным.
     * Также проверяем, что поломка фидлогов одной платформы не мешает фидлогам другой импортироваться.
     * <p>
     * На желтом импортируется одно поколение, в нем есть битые фидлоги.
     * На белом импортируются два поколения, в первом все ок (должно быть помечено проимпортированным), во втором
     * так же есть побитые фидлоги.
     */
    @Test
    @DbUnitDataSet(
            before = "data/errorWhileExecutionTest.before.csv",
            after = "data/errorWhileExecutionTest.after.csv"
    )
    void errorWhileExecutionTest() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> importFeedLogsExecutor.doJob(null))
                .withMessageContainingAll(
                        "in MARKET feeds: Total errors count: 2",
                        "in YELLOW_MARKET feeds: Total errors count: 3"
                );

        Mockito.verify(feedEventHandler, Mockito.times(1))
                .handle(any(NotInIndexFeedEvent.class));
        Mockito.verify(feedEventHandler, Mockito.times(14))
                .handle(any(IndexedFeedEvent.class));

        Mockito.verify(mdsS3Client, Mockito.times(3))
                .download(any(ResourceLocation.class), any(FileContentConsumer.class));

        // вызывается для единственного полностью проимпортированного поколения id=1235
        Mockito.verify(commonFeedsStrategy)
                .processProblemFeeds(
                        argThat(new FeedIndexationInfoCollectionMatcher(Arrays.asList(475690L, 121L, 122L)))
                );
    }

    /**
     * Тест проверяет работу с сессиями парсинга фида.
     */
    @Test
    @DbUnitDataSet(
            before = "data/workWithFeedSessionsTest.before.csv",
            after = "data/workWithFeedSessionsTest.after.csv"
    )
    void workWithFeedSessionsTest() {
        importFeedLogsExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "data/tankerTest.before.csv",
            after = "data/tankerTest.after.csv"
    )
    void tankerTest() {
        importFeedLogsExecutor.doJob(null);
        String result = jdbcTemplate.queryForObject(
                "select parse_log from shops_web.feed_session where parse_log is not null fetch next 1 rows only",
                (rs, n) -> DbUtil.getClobAsString(rs, "parse_log")
        );
        assertThat(result.split("\n")).containsExactlyInAnyOrder(
                "Ошибка: \"Не указана характеристика товара Размер\" (строка 32, столбец 14907414)",
                "Ошибка: \"Не указана характеристика товара Размер\" (строка 32, столбец 14933266)",
                "Ошибка: \"Не указана характеристика товара Размер\" (строка 32, столбец 14934762)",
                "Ошибка: \"Не указана характеристика товара Размер\" (строка 32, столбец 14935509)",
                "Ошибка: \"Не указана характеристика товара Размер\" (строка 32, столбец 14936958)",
                "Предупреждение: \"Неверный идентификатор акции\" (строка 13832, столбец 35)",
                "Предупреждение: \"В прайс-листе нет такого предложения\" (строка 13740, столбец 52)",
                "Предупреждение: \"В описании акции не указан основной товар\" (строка 13816, столбец 47)"
        );
    }

    @Test
    @DbUnitDataSet(
            before = "data/notOnly492Test.before.csv",
            after = "data/notOnly492Test.after.csv"
    )
    void notOnly492Test() {
        importFeedLogsExecutor.doJob(null);
    }

    /**
     * Для белых пушей не импортируются ошибки из yt (берутся из таблиц парсинга).
     */
    @Test
    @DbUnitDataSet(
            before = "data/whitePushTest.before.csv",
            after = "data/whitePushTest.after.csv"
    )
    void whitePushParsingTest() {
        importFeedLogsExecutor.doJob(null);
    }

    /**
     * Для белых пушей не импортируются ошибки из yt (берутся из таблиц парсинга).
     */
    @Test
    @DisplayName("Скипаем обычный магазин, если он в черном списке")
    @DbUnitDataSet(
            before = {
                    "data/whitePushTest.before.csv",
                    "data/whiteFeedBlacklist.before.csv"
            },
            after = "data/whiteFeedBlacklist.after.csv"
    )
    void whiteFeedSkippedTest() {
        importFeedLogsExecutor.doJob(null);
    }

    /**
     * Для белых пушей ошибки скачивания подмешиваются из самоварной таблицы.
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "data/whitePushTest.before.csv",
                    "data/whitePushDownloadTest.before.csv"
            },
            after = "data/whitePushDownloadTest.after.csv"
    )
    void whitePushDownloadTest() {
        importFeedLogsExecutor.doJob(null);
    }

    /**
     * Для белых non-smb пушей счетчики офферов для дефолтного фида работают по аналогии с обычным фидом.
     */
    @Test
    @DbUnitDataSet(
            before = "data/whitePushTest.before.csv",
            after = "data/whiteNonSmbPushCountersAreHackedTest.after.csv"
    )
    void whiteNonSmbPushCountersAreHackedTest() {
        importFeedLogsExecutor.doJob(null);
    }

    /**
     * Для белых пушей импортируется история парсинга фида из Единого офферного.
     */
    @Test
    @DbUnitDataSet(
            before = {
                    "data/whitePushTest.before.csv",
                    "data/dataCampFeedsErrorsAreMixedIntoFeedSessionsTest.before.csv"
            },
            after = "data/dataCampFeedsErrorsAreMixedIntoFeedSessionsTest.after.csv"
    )
    void dataCampFeedsErrorsAreMixedIntoFeedSessionsTest() {
        importFeedLogsExecutor.doJob(null);
    }

    /**
     * Нормально импортирует сессии, у которых не было парсинга.
     */
    @Test
    @DbUnitDataSet(
            before = "data/importSessionWithoutParsingTest.before.csv",
            after = "data/importSessionWithoutParsingTest.after.csv"
    )
    void importSessionWithoutParsingTest() {
        importFeedLogsExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "data/whiteDiffSimpleTest.before.csv",
            after = "data/whiteDiffSimpleTest.after.csv"
    )
    void whiteDiffSimpleTest() {
        importFeedLogsExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "data/virtualBlueTest.before.csv",
            after = "data/virtualBlueTest.after.csv"
    )
    void virtualBlueTest() {
        importFeedLogsExecutor.doJob(null);
    }

    @Test
    @DisplayName("Скипаем виртуальный магазин, если он в черном списке")
    @DbUnitDataSet(
            before = {
                    "data/virtualBlueTest.before.csv",
                    "data/blacklistVirtual.before.csv"
            },
            after = "data/blacklistVirtual.after.csv"
    )
    void virtualFeedSkippedTest() {
        importFeedLogsExecutor.doJob(null);
    }

    /**
     * Магазину отправляется 2 уведомления.
     * 1:OK->WARNING->WARNING
     * 2:WARNING->WARNING->OK
     */
    @Test
    @DbUnitDataSet(before = "data/shopWarningWarningOkTest.before.csv", after = "data/shopNoCutoff.csv")
    void shopWarningWarningOkTest() throws NotificationException {
        importFeedLogsExecutor.doJob(null);
        verifyNotification(2, 100, 10, CommonFeedsStrategy.FEED_PROBLEM_NOTIFICATION_ID);
    }

    /**
     * Магазину отправляется уведомлениe.
     * 1: OK->ERROR
     */
    @Test
    @DbUnitDataSet(before = "data/shopOkErrorTest.before.csv", after = "data/shopNoCutoff.csv")
    void shopOkErrorTest() throws NotificationException {
        importFeedLogsExecutor.doJob(null);
        verifyNotification(1, 100, 10, CommonFeedsStrategy.FEED_PROBLEM_NOTIFICATION_ID);
    }

    /**
     * Магазину отправляется уведомлениe.
     * 1: OK->ERROR
     */
    @Test
    @DbUnitDataSet(before = "data/directOkErrorTest.before.csv", after = "data/shopNoCutoff.csv")
    void directOkErrorTest() throws NotificationException {
        importFeedLogsExecutor.doJob(null);
        Mockito.verifyNoMoreInteractions(notificationService);
    }

    /**
     * Магазину отправляется 2 уведомления.
     * 1: OK->ERROR
     * 2: ERROR->OK
     */
    @Test
    @DbUnitDataSet(before = "data/shopErrorOkTest.before.csv", after = "data/shopNoCutoff.csv")
    void shopErrorOkTest() throws NotificationException {
        importFeedLogsExecutor.doJob(null);
        verifyNotification(2, 100, 10, CommonFeedsStrategy.FEED_PROBLEM_NOTIFICATION_ID);
    }

    /**
     * Магазину отправляется уведомление.
     * ERROR,WARNING
     */
    @Test
    @DbUnitDataSet(before = "data/shopWarningErrorTest.before.csv", after = "data/shopNoCutoff.csv")
    void shopWarningErrorTest() throws NotificationException {
        importFeedLogsExecutor.doJob(null);
        verifyNotification(1, 100, 10, CommonFeedsStrategy.FEED_PROBLEM_NOTIFICATION_ID);
    }

    /**
     * Магазину отправляется 2 уведомления.
     * OK->ERROR
     * ERROR->WARNING->WARNING
     */
    @Test
    @DbUnitDataSet(before = "data/shopErrorWarningWarningTest.before.csv", after = "data/shopNoCutoff.csv")
    void shopErrorWarningWarningTest() throws NotificationException {
        importFeedLogsExecutor.doJob(null);
        verifyNotification(2, 100, 10, CommonFeedsStrategy.FEED_PROBLEM_NOTIFICATION_ID);
    }

    /**
     * Магазину отправляется 2 уведомления
     * 1: фидов нет, получаем сразу error (OK->ERROR)
     * 2: ERROR->WARNING->OK
     */
    @Test
    @DbUnitDataSet(before = "data/shopErrorWarningOkTest.before.csv", after = "data/shopNoCutoff.csv")
    void shopErrorWarningOkTest() throws NotificationException {
        importFeedLogsExecutor.doJob(null);
        verifyNotification(2, 100, 10, CommonFeedsStrategy.FEED_PROBLEM_NOTIFICATION_ID);
    }

    /**
     * Магазину не отправляет уведомление.
     * OK -> OK
     */
    @Test
    @DbUnitDataSet(before = "data/shopOkOkTest.before.csv", after = "data/shopNoCutoff.csv")
    void shopOkOkTest() throws NotificationException {
        importFeedLogsExecutor.doJob(null);
        Mockito.verifyNoMoreInteractions(notificationService);
    }

    /**
     * Магазину не отправляет уведомление при первом WARNING.
     * OK -> WARNING
     */
    @Test
    @DbUnitDataSet(before = "data/shopOkWarningTest.before.csv", after = "data/shopNoCutoff.csv")
    void shopOkWarningTest() throws NotificationException {
        importFeedLogsExecutor.doJob(null);
        Mockito.verifyNoMoreInteractions(notificationService);
    }

    /**
     * Магазину отправляется уведомление, это 5 ошибка подряд.
     */
    @Test
    @DbUnitDataSet(before = "data/shopFiveErrorsTest.before.csv", after = "data/shopNoCutoff.csv")
    void shopFiveErrorsTest() throws NotificationException {
        importFeedLogsExecutor.doJob(null);
        verifyNotification(1, 100, 10, CommonFeedsStrategy.FEED_PROBLEM_NOTIFICATION_ID);
    }

    /**
     * Магазину открывается cutoff, это 11 ошибка подряд.
     */
    @Test
    @DbUnitDataSet(before = "data/shopOpenCutoffTest.before.csv", after = "data/shopOpenCutoffTest.after.csv")
    void shopOpenCutoffTest() throws NotificationException {
        importFeedLogsExecutor.doJob(null);
        verifyNotification(1, 100, 10, CommonFeedsStrategy.FEED_PROBLEM_CUTOFF_NOTIFICATION_ID);
    }

    /**
     * Магазину не открывается cutoff, это только 10 ошибка подряд.
     * Письмо не отправляется, потому что уже отправлялось ранее.
     */
    @Test
    @DbUnitDataSet(before = "data/shopNoOpenCutoffTest.before.csv", after = "data/shopNoCutoff.csv")
    void shopNoOpenCutoffTest() throws NotificationException {
        importFeedLogsExecutor.doJob(null);
        Mockito.verifyNoMoreInteractions(notificationService);
    }

    /**
     * Магазину не открывается катоф и не отправляются письма, потому что катоф уже есть.
     */
    @Test
    @DbUnitDataSet(before = "data/shopAlreadyOpenCutoffTest.before.csv", after = "data/shopAlreadyOpenCutoffTest" +
            ".after.csv")
    void shopAlreadyOpenCutoffTest() throws NotificationException {
        importFeedLogsExecutor.doJob(null);
        Mockito.verifyNoMoreInteractions(notificationService);
    }

    void verifyNotification(int times, long shopId, long feedId, int notificationType) {
        var argumentCaptor = ArgumentCaptor.forClass(NotificationSendContext.class);
        Mockito.verify(notificationService, Mockito.times(times)).send(argumentCaptor.capture());
        var sendContext = argumentCaptor.getValue();
        assertThat(((FeedLogNotificationContainer) sendContext.getData().get(0)).getFeedId()).isEqualTo(feedId);
        assertThat(sendContext.getShopId()).isEqualTo(shopId);
        assertThat(sendContext.getTypeId()).isEqualTo(notificationType);
        Mockito.verifyNoMoreInteractions(notificationService);
    }

    static class FeedLogFile {
        File createdFile;
        Supplier<File> creator;

        FeedLogFile(Supplier<File> creator) {
            this.creator = creator;
        }

        static FeedLogFile of(Supplier<File> creator) {
            return new FeedLogFile(creator);
        }

        public File get() {
            return createdFile;
        }

        public File create() {
            return createdFile = creator.get();
        }
    }
}
