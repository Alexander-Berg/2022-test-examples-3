package ru.yandex.market.core.indexer.supplier;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.mds.s3.client.content.consumer.FileContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.consumer.TextContentConsumer;
import ru.yandex.market.common.mds.s3.client.model.FileMeta;
import ru.yandex.market.common.mds.s3.client.model.ResourceListing;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.test.util.StringTestUtil;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.feed.FeedPublishingService;
import ru.yandex.market.core.feed.FeedService;
import ru.yandex.market.core.feed.supplier.SupplierFeedService;
import ru.yandex.market.core.feed.supplier.SupplierFeedServiceImpl;
import ru.yandex.market.core.feed.supplier.db.FeedSupplierDao;
import ru.yandex.market.core.feed.supplier.db.PartnerUtilityFeedDao;
import ru.yandex.market.core.feed.supplier.mapper.SupplierFeedMapperImpl;
import ru.yandex.market.core.history.HistoryService;
import ru.yandex.market.core.indexer.db.generation.GenerationService;
import ru.yandex.market.core.indexer.db.generation.IdxGenerationService;
import ru.yandex.market.core.indexer.db.meta.GenerationMetaService;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.UnitedCatalogStatus;
import ru.yandex.market.core.partner.PartnerService;
import ru.yandex.market.core.upload.FileUploadService;
import ru.yandex.market.core.util.io.Protobuf;
import ru.yandex.market.core.xml.Marshaller;
import ru.yandex.market.indexer.FeedLogBuilder;
import ru.yandex.market.indexer.ImportGenerationsExecutor;
import ru.yandex.market.indexer.IndexerService;
import ru.yandex.market.indexer.ProtoBufIndexerService;
import ru.yandex.market.indexer.listener.UpdatedSupplierFeedStateEventListener;
import ru.yandex.market.mbi.util.io.CloseableIOConsumer;
import ru.yandex.market.mbi.util.io.MbiFiles;
import ru.yandex.market.proto.indexer.v2.FeedLog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Проверяем импорт данных фида.
 */
abstract class AbstractImportGenerationExecutorTest extends FunctionalTest {
    @Autowired
    private FeedService feedService;

    @Autowired
    private GenerationMetaService generationMetaService;

    @Autowired
    private IdxGenerationService idxGenerationService;

    @Autowired
    ParamService paramService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private Supplier<Integer> samovarInactivePeriodSupplier;

    @Autowired
    private PartnerService partnerService;

    private MdsS3Client mdsS3Client;
    private GenerationService generationServiceSpy;
    private ImportGenerationsExecutor importGenerationsExecutor;

    @Value("#{'${indexer.yt.clusters}'.split(',')}")
    private List<String> clusters;

    void initImportGenerationsExecutor(
            GenerationService generationService,
            Collection<UpdatedSupplierFeedStateEventListener> listeners,
            IndexerService.IndexerType indexerType
    ) {
        mdsS3Client = mock(MdsS3Client.class);
        when(mdsS3Client.list(any(), eq(true))).thenReturn(ResourceListing.createListingWithMeta(
                "bucketName",
                List.of(new FileMeta("key", new Date(0))),
                List.of("prefix")
        ));
        when(mdsS3Client.download(any(), any(TextContentConsumer.class)))
                .thenReturn("name=20180517_0717\n" +
                        (
                                indexerType == IndexerService.IndexerType.MAIN
                                        ? "mitype=stratocaster\n"
                                        : "mitype=planeshift.stratocaster\n"
                        ) +
                        "type=full\n" +
                        "start_date=1526530621\n" +
                        "end_date=1526539243\n" +
                        "release_date=1526544184\n" +
                        "sc_version=1526539243");

        IndexerService indexerService = new ProtoBufIndexerService(
                mock(ResourceLocationFactory.class),
                mdsS3Client,
                indexerType,
                clusters
        );

        generationServiceSpy = spy(generationService);
        SupplierFeedService supplierFeedService = new SupplierFeedServiceImpl(
                mock(HistoryService.class),
                new FeedSupplierDao(namedParameterJdbcTemplate,
                        samovarInactivePeriodSupplier),
                mock(FileUploadService.class),
                mock(ApplicationEventPublisher.class),
                mock(PartnerUtilityFeedDao.class),
                mock(FeedPublishingService.class),
                mock(Marshaller.class),
                new SupplierFeedMapperImpl(),
                businessService,
                partnerService
        );

        FeedLogBuilder feedLogBuilder = new FeedLogBuilder();
        feedLogBuilder.setFeedService(feedService);

        importGenerationsExecutor = new ImportGenerationsExecutor(
                indexerService,
                idxGenerationService,
                generationServiceSpy,
                generationMetaService,
                feedService,
                supplierFeedService,
                transactionTemplate,
                feedLogBuilder,
                paramService
        );
        listeners.forEach(importGenerationsExecutor::addSupplierFeedStateListener);
    }

    void checkFeedIndxLoad(String indexedStatus) {
        when(mdsS3Client.download(any(), any(FileContentConsumer.class)))
                .thenReturn(generate(prepareFeedLog(indexedStatus)));

        doReturn(1L).when(generationServiceSpy).saveGeneration(any());
        when(generationServiceSpy.checkGenerationsExist(anyCollection())).thenReturn(Set.of());

        importGenerationsExecutor.doJob(null);
    }

    private FeedLog.Feed prepareFeedLog(String indexedStatus) {
        String parseLog = StringTestUtil.getString(getClass(), "parse_log.txt");
        return FeedLog.Feed.newBuilder()
                .setFeedId(497424)
                .setShopId(465984)
                .setYxShopName("Pudra.ru")
                .setLastSession(
                        FeedLog.RobotFeedSession.newBuilder()
                                .setSessionName("20180516_2147")
                                .setStartDate(1526507231)
                                .setDownloadDate(1526507231)
                                .setParseRetcode(1)
                                .build()
                )
                .setDownloadRetcode(0)
                .setDownloadStatus("200 OK")
                .setParseLog(parseLog)
                .setOffersCount(34)
                .setCpaOffersCount(79)
                .setDiscountOffersCount(0)
                .setOffersHosts("")
                .setPlatform(FeedLog.Platform.newBuilder().setName("").setVersion("").build())
                .setIndexedStatus(indexedStatus)
                .setMatchedOffersCount(79)
                .setPublishedSession("20180516_2147")
                .setAgency("")
                .setEmail("")
                .setFeedUrl(
                        "https://market-mbi-prod.s3.mds.yandex.net/supplier-feed/suppliers/465984/feeds/497423/data")
                .setMatchedClusterOffersCount(0)
                .setTotalServicesCount(0)
                .setValidServicesCount(0)
                .setTotalPromosCount(0)
                .setValidCpcPromosCount(0)
                .setValidCpaPromosCount(0)
                .setPrimaryOffersWithPromoCount(0)
                .setDeliverycalcUpdateTimeTs(1526494500849L)
                .setYmlDate("")
                .setParseStats(FeedLog.ParseStats.newBuilder()
                        .setTotalOffers(79)
                        .setValidOffers(37)
                        .setValidOffersWithStocksAndSizes(34)
                        .setDuplicateOffers(0)
                        .build())
                .setIndexation(FeedLog.ProcessingSummary.newBuilder()
                        .setStatistics(FeedLog.ParseStats.newBuilder()
                                .setTotalOffers(70)
                                .setValidOffers(30)
                                .build())
                        .build())
                .setFeedProcessingType(FeedLog.FeedProcessingType.PULL)
                .setBusinessId(987654321)
                .setUnitedCatalog(UnitedCatalogStatus.SUCCESS.toString())
                .build();
    }

    private File generate(FeedLog.Feed protoMessage) {
        String FILE_NAME = "stratocaster.full.1526544184.pbuf.sn";
        File file;
        try {
            file = Files.createTempFile(FILE_NAME, null).toFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            fill(file, protoMessage);
        } catch (Exception e) {
            FileUtils.deleteQuietly(file);
            throw new RuntimeException(e);
        }
        return file;
    }

    private void fill(File file, FeedLog.Feed protoMessage) {
        try (
                OutputStream byteStream = Files.newOutputStream(file.toPath());
                BufferedOutputStream bufferedStream = MbiFiles.bufferedOutputStream(byteStream);
                CloseableIOConsumer<FeedLog.Feed> stream = Protobuf.snappyLenvalOutputStream("FLOG", bufferedStream)
        ) {
            stream.accept(protoMessage);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
