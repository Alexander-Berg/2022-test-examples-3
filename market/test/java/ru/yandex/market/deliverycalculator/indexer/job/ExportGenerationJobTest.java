package ru.yandex.market.deliverycalculator.indexer.job;

import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.delivery.net.protobuf.DeliveryCalcProtos;
import ru.yandex.market.deliverycalculator.PbSnUtils;
import ru.yandex.market.deliverycalculator.common.StrUtils;
import ru.yandex.market.deliverycalculator.indexer.FunctionalTest;
import ru.yandex.market.deliverycalculator.indexerclient.modelv2.DeliveryType;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffProgramType;
import ru.yandex.market.deliverycalculator.storage.StorageTestUtils;
import ru.yandex.market.deliverycalculator.storage.service.impl.TariffInfoProvider;
import ru.yandex.market.deliverycalculator.storage.util.PooledIdGenerator;
import ru.yandex.market.deliverycalculator.workflow.solomon.BoilingSolomonService;
import ru.yandex.market.deliverycalculator.workflow.solomon.BoilingSolomonTestUtil;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.BoilingKey;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.BoilingStageType;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.BucketCountingBoilingKey;
import ru.yandex.market.deliverycalculator.workflow.solomon.model.PreparingTariffBoilingKey;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class ExportGenerationJobTest extends FunctionalTest {

    @Autowired
    private ExportYadoTariffGenerationJob exportWhiteTariffGenerationJob;

    @Autowired
    private ExportYadoTariffGenerationJob exportBlueCourierTariffGenerationJob;

    @Autowired
    private ExportYadoTariffGenerationJob exportBluePickupTariffGenerationJob;

    @Autowired
    private ExportYadoTariffGenerationJob exportBluePostTariffGenerationJob;

    @Autowired
    private ExportYadoTariffGenerationJob exportDaasTariffGenerationJob;

    @Autowired
    private ExportShopGenerationJob exportShopGenerationJob;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @Autowired
    private TariffInfoProvider tariffInfoProvider;

    @Autowired
    private BoilingSolomonService boilingSolomonService;

    @Autowired
    private PooledIdGenerator mockedModifierIdGenerator;

    @BeforeEach
    void init() {
        when(mdsS3Client.getUrl(any()))
                .thenAnswer(invocation -> new URL("http://some.url.here-" + StrUtils.getRandomString()));
        mockModifierIdGenerator();
    }

    /**
     * Проверяется корректность приготовленного джобой exportBlueCourierTariffGenerationJob поколения для
     * MardoCourier стратегии.
     */
    @Test
    @DbUnitDataSet(before = "mardoCourierTariffWorkflow.marketDelivery.before.csv",
            after = "mardoCourierTariffWorkflow.after.csv")
    void mardoCourierTariffWorkflowMarketDeliveryExportTest() {
        initMocksForBlueTariffsTests(DeliveryType.COURIER.name().length());
        StorageTestUtils.initProviderMock(tariffInfoProvider, arg -> "mardo-courier/tariff_" + arg, getClass());

        runTariffJob(exportBlueCourierTariffGenerationJob);
    }

    /**
     * 1. Проверяется корректность приготовленных джобой exportBlueCourierTariffGenerationJob поколений для
     * MardoPickup стратегии.
     * 2. Проверяется что для каждого тарифа создается свое поколение. Т е что КД гарантирует, что поколения между
     * тарифами не дублируются.
     */
    @Test
    @DbUnitDataSet(before = "mardo-pickup/mardoPickupTariffWorkflow.before.csv",
            after = "mardo-pickup/mardoPickupTariffWorkflow.after.csv")
    void mardoPickupTariffWorkflowMarketDeliveryExportTest() {
        initMocksForBlueTariffsTests(DeliveryType.PICKUP.name().length());
        StorageTestUtils.initProviderMock(tariffInfoProvider, arg -> "mardo-pickup/tariff_" + arg, getClass());

        runTariffJob(exportBluePickupTariffGenerationJob);
    }

    /**
     * Проверяется корректность приготовленного джобой exportBluePostTariffGenerationJob поколения для
     * MardoPost стратегии.
     */
    @Test
    @DbUnitDataSet(before = "mardo-post/mardoPostTariffWorkflow.before.csv",
            after = "mardo-post/mardoPostTariffWorkflow.after.csv")
    void mardoPostTariffWorkflowMarketDeliveryExportTest() {
        initMocksForBlueTariffsTests(DeliveryType.POST.name().length());
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "mardo-post/tariff.xml", getClass());

        runTariffJob(exportBluePostTariffGenerationJob);
    }

    private void initMocksForBlueTariffsTests(int deliveryTypeLen) {
        when(resourceLocationFactory.createLocation(any()))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    return ResourceLocation.create("bucketName", key.substring(0, 27 + deliveryTypeLen));
                });
        when(mdsS3Client.getUrl(any()))
                .thenAnswer(invocation -> {
                    ResourceLocation resourceLocation = invocation.getArgument(0);
                    return new URL("http://" + resourceLocation.getKey());
                });
    }

    /**
     * Проверяет корректность приготовленного джобой exportWhiteTariffGenerationJob
     * поколения для авторасчета СиС Белого Маркета (самовывоз).
     */
    @Test
    @DbUnitDataSet(before = "mardoWhitePickupGenerationExportTest.before.csv",
            after = "mardoWhitePickupGenerationExportTest.after.csv")
    void mardoWhitePickupGenerationExportTest() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "white-pickup/tariff_1110.xml", getClass());
        doAnswer(invocation -> {
            ContentProvider contentProvider = invocation.getArgument(1);
            assertNotNull(readFeedDeliveryOptionsResp(contentProvider.getInputStream()));
            return null;
        }).when(mdsS3Client).upload(any(), any());
        final BoilingKey key = PreparingTariffBoilingKey.of(1110L, DeliveryTariffProgramType.WHITE_MARKET_DELIVERY,
                BoilingStageType.TARIFF_PREPARING_STAGE);
        final BoilingKey bucketKey = BucketCountingBoilingKey.of(1110L, 1L, 1L, 1, DeliveryTariffProgramType.WHITE_MARKET_DELIVERY,
                BoilingStageType.BUCKET_COUNTING_STAGE);
        runTariffJob(exportWhiteTariffGenerationJob, key, bucketKey);
    }

    @Test
    @DisplayName("Тест, что при выставленном флаге выключения варки джоба экспорта ничего не сварит и выбросит исключение")
    @DbUnitDataSet(before = "mardoWhitePickupGenerationDisabledByFlagExportTest.before.csv",
            after = "mardoWhitePickupGenerationDisabledByFlagExportTest.before.csv")
    void testEmergencyBreakForExport() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "white-pickup/tariff_1110.xml", getClass());
        doAnswer(invocation -> {
            ContentProvider contentProvider = invocation.getArgument(1);
            assertNotNull(readFeedDeliveryOptionsResp(contentProvider.getInputStream()));
            return null;
        }).when(mdsS3Client).upload(any(), any());
        final BoilingKey key = PreparingTariffBoilingKey.of(1110L, DeliveryTariffProgramType.WHITE_MARKET_DELIVERY,
                BoilingStageType.TARIFF_PREPARING_STAGE);
        final BoilingKey bucketKey = BucketCountingBoilingKey.of(1110L, 1L, 1L, 1, DeliveryTariffProgramType.WHITE_MARKET_DELIVERY,
                BoilingStageType.BUCKET_COUNTING_STAGE);
        final RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> runTariffJob(exportWhiteTariffGenerationJob, key, bucketKey));
        Assertions.assertEquals("Export is disabled by emergency break flag", exception.getMessage());
    }

    /**
     * Проверяет корректность приготовленного джобой exportDaasTariffGenerationJob
     * поколения для DAAS курьерской стратегии.
     */
    @Test
    @DbUnitDataSet(before = "daasCourierGenerationExportTest.before.csv",
            after = "daasCourierGenerationExportTest.after.csv")
    void daasCourierGenerationExportTest() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "daas-courier/tariff_1234.xml", getClass());

        doAnswer(invocation -> {
            ContentProvider contentProvider = invocation.getArgument(1);
            assertNotNull(readFeedDeliveryOptionsResp(contentProvider.getInputStream()));
            return null;
        }).when(mdsS3Client).upload(any(), any());
        final BoilingKey key = PreparingTariffBoilingKey.of(1234L, DeliveryTariffProgramType.DAAS,
                BoilingStageType.TARIFF_PREPARING_STAGE);
        final BoilingKey bucketKey = BucketCountingBoilingKey.of(1234L, 1L, 1L, 2, DeliveryTariffProgramType.DAAS,
                BoilingStageType.BUCKET_COUNTING_STAGE);
        runTariffJob(exportDaasTariffGenerationJob, key, bucketKey);
    }

    /**
     * Проверяет корректность приготовленного джобой exportDaasTariffGenerationJob
     * поколения для DAAS курьерской стратегии. Так как в исходных данных уже имеются данные из прошлого поколения,
     * также проверяет корректность переиспользования ранее сохраненных бакетов.
     */
    @Test
    @DbUnitDataSet(before = "daasCourierGenerationExportExistingDataTest.before.csv",
            after = "daasCourierGenerationExportExistingDataTest.after.csv")
    void daasCourierGenerationExportWithExistingDataTest() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "daas-courier/tariff_1234.xml", getClass());

        final BoilingKey key = PreparingTariffBoilingKey.of(1234L, DeliveryTariffProgramType.DAAS,
                BoilingStageType.TARIFF_PREPARING_STAGE);
        final BoilingKey bucketKey = BucketCountingBoilingKey.of(1234L, 2L, 1L, 2, DeliveryTariffProgramType.DAAS,
                BoilingStageType.BUCKET_COUNTING_STAGE);
        runTariffJob(exportDaasTariffGenerationJob, key, bucketKey);
    }

    /**
     * Проверяет корректность приготовленного джобой exportDaasTariffGenerationJob
     * поколения для DAAS курьерской стратегии в случае, если тарифная сетка поменялась.
     * Так как в исходных данных уже имеются данные из прошлого поколения, также проверяет корректность
     * переиспользования ранее сохраненных бакетов.
     */
    @Test
    @DbUnitDataSet(before = "daasCourierGenerationExportTariffGridIsChanged.before.csv",
            after = "daasCourierGenerationExportTariffGridIsChanged.after.csv")
    void daasCourierGenerationExportWhenTariffGridIsChanged() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "daas-courier/tariff_1234.xml", getClass());

        final BoilingKey key = PreparingTariffBoilingKey.of(1234L, DeliveryTariffProgramType.DAAS,
                BoilingStageType.TARIFF_PREPARING_STAGE);
        final BoilingKey bucketKey = BucketCountingBoilingKey.of(1234L, 2L, 2L, 2, DeliveryTariffProgramType.DAAS,
                BoilingStageType.BUCKET_COUNTING_STAGE);
        runTariffJob(exportDaasTariffGenerationJob, key, bucketKey);
    }

    /**
     * Проверяет корректность приготовленного джобой exportDaasTariffGenerationJob
     * поколения для DAAS курьерской стратегии в случае, если тарифная сетка не поменялась.
     * Так как в исходных данных уже имеются данные из прошлого поколения, также проверяет корректность
     * переиспользования ранее сохраненных бакетов.
     */
    @Test
    @DbUnitDataSet(before = "daasCourierGenerationExportTariffGridIsNotChanged.before.csv",
            after = "daasCourierGenerationExportTariffGridIsNotChanged.after.csv")
    void daasCourierGenerationExportWhenTariffGridIsNotChanged() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "daas-courier/tariff_1234.xml", getClass());

        final BoilingKey key = PreparingTariffBoilingKey.of(1234L, DeliveryTariffProgramType.DAAS,
                BoilingStageType.TARIFF_PREPARING_STAGE);
        final BoilingKey bucketKey = BucketCountingBoilingKey.of(1234L, 2L, 1L, 2, DeliveryTariffProgramType.DAAS,
                BoilingStageType.BUCKET_COUNTING_STAGE);
        runTariffJob(exportDaasTariffGenerationJob, key, bucketKey);
    }

    /**
     * Проверяет корректность приготовленного джобой exportDaasTariffGenerationJob
     * поколения для DAAS курьерской стратегии в случае, если тариф был сперва удален, потом вновь становится активным.
     */
    @Test
    @DbUnitDataSet(before = "daasCourierGenerationExportTariffActivated.before.csv",
            after = "daasCourierGenerationExportTariffActivated.after.csv")
    void daasCourierGenerationExportWhenTariffWasReactivated() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "daas-courier/tariff_1234.xml", getClass());

        final BoilingKey key = PreparingTariffBoilingKey.of(1234L, DeliveryTariffProgramType.DAAS,
                BoilingStageType.TARIFF_PREPARING_STAGE);
        final BoilingKey bucketKey = BucketCountingBoilingKey.of(1234L, 2L, 2L, 2, DeliveryTariffProgramType.DAAS,
                BoilingStageType.BUCKET_COUNTING_STAGE);
        runTariffJob(exportDaasTariffGenerationJob, key, bucketKey);
    }

    /**
     * Проверяет корректность приготовленного джобой exportDaasTariffGenerationJob
     * поколения для DAAS ПВЗ стратегии.
     */
    @Test
    @DbUnitDataSet(before = "daasPickupGenerationExportTest.before.csv",
            after = "daasPickupGenerationExportTest.after.csv")
    void daasPickupGenerationExportTest() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "daas-pickup/tariff_1234_1.xml", getClass());

        final BoilingKey key = PreparingTariffBoilingKey.of(1234L, DeliveryTariffProgramType.DAAS,
                BoilingStageType.TARIFF_PREPARING_STAGE);
        final BoilingKey bucketKey = BucketCountingBoilingKey.of(1234L, 1L, 1L, 3, DeliveryTariffProgramType.DAAS,
                BoilingStageType.BUCKET_COUNTING_STAGE);
        runTariffJob(exportDaasTariffGenerationJob, key, bucketKey);
    }

    /**
     * Проверяет корректность приготовленного джобой exportDaasTariffGenerationJob
     * поколения для DAAS ПВЗ стратегии.
     */
    @Test
    @Disabled("Проблема с икрементами сиквенса, тест не работает совместно с другими тестами из класса. " +
            "Пофиксить вместе с DeliveryCalculatorMetaStorageServiceImplTest задизейбленными тестами. " +
            "Проблема та же =(")
    @DbUnitDataSet(before = "daas-pickup/before.csv")
    void daasPickupHugeTariffGenerationExportTest() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "daas-pickup/hugeTariff.xml", getClass());

        final BoilingKey key = PreparingTariffBoilingKey.of(2001L, DeliveryTariffProgramType.DAAS,
                BoilingStageType.TARIFF_PREPARING_STAGE);
        final BoilingKey bucketKey = BucketCountingBoilingKey.of(2001L, 1L, 1L, 43, DeliveryTariffProgramType.DAAS,
                BoilingStageType.BUCKET_COUNTING_STAGE);
        runTariffJob(exportDaasTariffGenerationJob, key, bucketKey);
    }

    /**
     * Проверяет корректность приготовленного джобой exportDaasTariffGenerationJob поколения для DAAS ПВЗ стратегии.
     * Так как в исходных данных уже имеются данные из прошлого поколения, также проверяет корректность
     * переиспользования ранее сохраненных бакетов.
     */
    @Test
    @DbUnitDataSet(before = "daasPickupGenerationExportExistingDataTest.before.csv",
            after = "daasPickupGenerationExportExistingDataTest.after.csv")
    void daasPickupGenerationExportWithExistingDataTest() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "daas-pickup/tariff_1234.xml", getClass());

        final BoilingKey key = PreparingTariffBoilingKey.of(1234L, DeliveryTariffProgramType.DAAS,
                BoilingStageType.TARIFF_PREPARING_STAGE);
        final BoilingKey bucketKey = BucketCountingBoilingKey.of(1234L, 1L, 1L, 3, DeliveryTariffProgramType.DAAS,
                BoilingStageType.BUCKET_COUNTING_STAGE);
        runTariffJob(exportDaasTariffGenerationJob, key, bucketKey);
    }

    /**
     * Проверяет корректность приготовленного джобой exportDaasTariffGenerationJob
     * поколения для DAAS стратегии пикапов в случае, если тарифная сетка не поменялась.
     */
    @Test
    @DbUnitDataSet(before = "daasPickupGenerationExportTestTariffGridNotChanged.before.csv",
            after = "daasPickupGenerationExportTestTariffGridNotChanged.after.csv")
    void daasPickupGenerationExportTestTariffGridNotChanged() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "daas-pickup/tariff_1234_1.xml", getClass());

        final BoilingKey key = PreparingTariffBoilingKey.of(1234L, DeliveryTariffProgramType.DAAS,
                BoilingStageType.TARIFF_PREPARING_STAGE);
        final BoilingKey bucketKey = BucketCountingBoilingKey.of(1234L, 2L, 1L, 3, DeliveryTariffProgramType.DAAS,
                BoilingStageType.BUCKET_COUNTING_STAGE);
        runTariffJob(exportDaasTariffGenerationJob, key, bucketKey);
    }

    /**
     * Проверяет корректность приготовленного джобой exportDaasTariffGenerationJob
     * поколения для DAAS стратегии Почты России.
     */
    @Test
    @DbUnitDataSet(before = "daasPostGenerationExportTest.before.csv",
            after = "daasPostGenerationExportTest.after.csv")
    void daasPostGenerationExportTest() {
        final BoilingKey key = PreparingTariffBoilingKey.of(1234L, DeliveryTariffProgramType.DAAS,
                BoilingStageType.TARIFF_PREPARING_STAGE);
        final BoilingKey bucketKey = BucketCountingBoilingKey.of(1234L, 2L, 2L, 3, DeliveryTariffProgramType.DAAS,
                BoilingStageType.BUCKET_COUNTING_STAGE);
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "daas-post/tariff_1234.xml", getClass());
        runTariffJob(exportDaasTariffGenerationJob, key, bucketKey);
    }

    /**
     * Проверяет корректность приготовленного джобой exportDaasTariffGenerationJob
     * поколения для DAAS стратегии Почты России в случае, если тарифная сетка не поменялась.
     */
    @Test
    @DbUnitDataSet(before = "daasPostGenerationExportTestTariffGridIsNotChanged.before.csv",
            after = "daasPostGenerationExportTestTariffGridIsNotChanged.after.csv")
    void daasPostGenerationExportTestTariffGridIsNotChanged() {
        final BoilingKey key = PreparingTariffBoilingKey.of(1234L, DeliveryTariffProgramType.DAAS,
                BoilingStageType.TARIFF_PREPARING_STAGE);
        final BoilingKey bucketKey = BucketCountingBoilingKey.of(1234L, 2L, 1L, 3, DeliveryTariffProgramType.DAAS,
                BoilingStageType.BUCKET_COUNTING_STAGE);
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "daas-post/tariff_1234.xml", getClass());
        runTariffJob(exportDaasTariffGenerationJob, key, bucketKey);
    }

    @Test
    @DbUnitDataSet(before = "exportSenderSettingsGeneration.before.csv",
            after = "exportSenderSettingsGeneration.after.csv")
    void testExportSenderSettings() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "white-pickup/tariff_1234.xml", getClass());
        exportShopGenerationJob.doJob(null);
    }

    @Test
    @DbUnitDataSet(before = "exportShopModifiersGeneration.before.csv",
            after = "exportShopModifiersGeneration.after.csv")
    void testExportShopModifiers() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "white-pickup/tariff_1234.xml", getClass());
        runShopJob(exportShopGenerationJob);
    }

    @Test
    @DbUnitDataSet(before = "exportDeletedShopModifiersGeneration.before.csv",
            after = "exportDeletedShopModifiersGeneration.after.csv")
    void testExportDeletedShopModifiers() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "white-pickup/tariff_1234.xml", getClass());
        runShopJob(exportShopGenerationJob);
    }

    /**
     * Проверяется корректность приготовленного джобой exportGenerationJob поколения для мультипрограммного тарифа.
     */
    @Test
    @DbUnitDataSet(before = "multiprogramTariffExportTest.before.csv",
            after = "multiprogramTariffExportTest.after.csv")
    void multiprogramTariffExportTest() {
        StorageTestUtils.initProviderMock(tariffInfoProvider, filename -> "white-pickup/tariff_1234.xml", getClass());

        doAnswer(invocation -> {
            ContentProvider contentProvider = invocation.getArgument(1);
            assertNotNull(readFeedDeliveryOptionsResp(contentProvider.getInputStream()));
            return null;
        }).when(mdsS3Client).upload(any(), any());

        final BoilingKey keyDaas = PreparingTariffBoilingKey.of(1234L, DeliveryTariffProgramType.DAAS,
                BoilingStageType.TARIFF_PREPARING_STAGE);
        final BoilingKey keyWhite = PreparingTariffBoilingKey.of(1234L, DeliveryTariffProgramType.WHITE_MARKET_DELIVERY,
                BoilingStageType.TARIFF_PREPARING_STAGE);
        final BoilingKey bucketKeyWhite = BucketCountingBoilingKey.of(1234L, 1L, 1L, 0, DeliveryTariffProgramType.WHITE_MARKET_DELIVERY,
                BoilingStageType.BUCKET_COUNTING_STAGE);
        final BoilingKey bucketKeyDaas = BucketCountingBoilingKey.of(1234L, 2L, 2L, 0, DeliveryTariffProgramType.DAAS,
                BoilingStageType.BUCKET_COUNTING_STAGE);
        runTariffJob(exportWhiteTariffGenerationJob, keyWhite, bucketKeyWhite);
        runTariffJob(exportDaasTariffGenerationJob, keyDaas, keyWhite, bucketKeyDaas, bucketKeyWhite);
    }

    private void runShopJob(final ExportShopGenerationJob job) {
        job.doJob(null);
    }

    private void runTariffJob(final ExportYadoTariffGenerationJob job,
                              final BoilingKey... expectedKeys
    ) {
        job.doJob(null);
        if (expectedKeys.length > 0) {
            BoilingSolomonTestUtil.checkStageEvents(boilingSolomonService, expectedKeys);
        }
    }

    private void mockModifierIdGenerator() {
        when(mockedModifierIdGenerator.generate()).thenAnswer(new Answer<>() {
            private long count = 0;

            @Override
            public Object answer(InvocationOnMock invocation) {
                return count++;
            }
        });
    }

    private static DeliveryCalcProtos.FeedDeliveryOptionsResp readFeedDeliveryOptionsResp(InputStream stream)
            throws Exception {
        return PbSnUtils.readPbSnMessage("DCFA", DeliveryCalcProtos.FeedDeliveryOptionsResp.parser(), stream);
    }
}
