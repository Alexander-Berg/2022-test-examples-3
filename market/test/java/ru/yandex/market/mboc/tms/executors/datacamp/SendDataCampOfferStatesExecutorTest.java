package ru.yandex.market.mboc.tms.executors.datacamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampResolution;
import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.DataCampValidationResult;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logbroker.LogbrokerEventPublisherMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.solomon.SolomonPushService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.contentprocessing.from.repository.QueueFromContentProcessingRepository;
import ru.yandex.market.mboc.common.datacamp.model.DataCampUnitedOffersEvent;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.datacamp.service.converter.DataCampConverterService;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.enums.MigrationStatusType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.MigrationStatus;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.logbroker.events.ThrottlingLogbrokerEventPublisher;
import ru.yandex.market.mboc.common.msku.TestUtils;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleRepository;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleService;
import ru.yandex.market.mboc.common.offers.acceptance.service.AcceptanceService;
import ru.yandex.market.mboc.common.offers.model.AntiMapping;
import ru.yandex.market.mboc.common.offers.model.ContentProcessingResponse;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferMeta;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.IMasterDataRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationRemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferMetaRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.datacamp.SendDataCampOfferStatesService;
import ru.yandex.market.mboc.common.services.migration.MigrationModelService;
import ru.yandex.market.mboc.common.services.migration.MigrationService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.RealConverter;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingServiceMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mboc.common.utils.OfferTestUtils.BIZ_ID_SUPPLIER;

/**
 * @author dergachevfv
 * @since 7/23/20
 */
public class SendDataCampOfferStatesExecutorTest extends BaseDbTestClass {

    private static final int BERU_BUSINESS_ID = 565853;
    private static final int REAL_SUPPLIER_ID = 44;
    private static final String REAL_SUPPLIER_EXT_ID = "real";
    private static final long TEST_CATEGORY_ID = 100;
    private static final long OFFER_ID_1 = 1L;
    private static final long OFFER_ID_2 = 2L;
    private static final long OFFER_ID_3 = 3L;
    private static final long OFFER_ID_4 = 4L;
    private static final long OFFER_ID_5 = 5L;
    private static final long OFFER_ID_6 = 6L;
    private static final String SHOP_SKU_1 = "shop-sku-1";
    private static final String SHOP_SKU_2 = "shop-sku-2";
    private static final String SHOP_SKU_3 = "shop-sku-3";
    private static final String SHOP_SKU_4 = "shop-sku-4";
    private static final String SHOP_SKU_5 = "shop-sku-5";
    private static final String SHOP_SKU_6 = "shop-sku-6";
    private static final String SHOP_SKU_7 = "shop-sku-7";

    private static final long NOT_MODEL_ID_1 = 1001L;
    private static final long NOT_SKU_ID_2 = 1002L;

    @Autowired
    private MigrationStatusRepository migrationStatusRepository;
    @Autowired
    private MigrationOfferRepository migrationOffersRepository;
    @Autowired
    private MigrationRemovedOfferRepository migrationRemovedOfferRepository;
    @Autowired
    private QueueFromContentProcessingRepository queueFromContentProcessingRepository;
    @Autowired
    private OfferUpdateSequenceService offerUpdateSequenceService;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private IMasterDataRepository masterDataFor1pRepository;
    @Autowired
    private OfferMetaRepository offerMetaRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private AntiMappingRepository antiMappingRepository;
    @Autowired
    private CategoryRuleRepository categoryRuleRepository;

    private CategoryCachingServiceMock categoryCachingServiceMock;
    private LogbrokerEventPublisherMock<DataCampUnitedOffersEvent> logbrokerEventPublisherMock;
    private LogbrokerEventPublisherMock<DataCampUnitedOffersEvent> logbroker1pEventPublisherMock;
    private SupplierConverterServiceMock supplierConverterService;

    private MigrationService migrationService;


    private MskuRepository mskuRepository;
    private GlobalVendorsCachingServiceMock globalVendorsCachingServiceMock;
    private MigrationModelService migrationModelService;

    private SendDataCampOfferStatesExecutor sendDataCampOfferStatesExecutor;

    @Before
    public void setUp() {
        categoryCachingServiceMock = new CategoryCachingServiceMock();
        logbrokerEventPublisherMock = new LogbrokerEventPublisherMock<>();
        logbroker1pEventPublisherMock = new LogbrokerEventPublisherMock<>();
        storageKeyValueService = new StorageKeyValueServiceMock();
        storageKeyValueService.putValue(SendDataCampOfferStatesService.ALIVE_OFFER_ACCEPTANCE_REPORT_FLAG, true);

        supplierRepository.insertBatch(
            OfferTestUtils.businessSupplier(),
            new Supplier(REAL_SUPPLIER_ID, "Test Real Supplier")
                .setType(MbocSupplierType.REAL_SUPPLIER)
                .setRealSupplierId(REAL_SUPPLIER_EXT_ID),
            new Supplier(BERU_BUSINESS_ID, "Yandex.Market Biz")
                .setType(MbocSupplierType.BUSINESS),
            new Supplier(SupplierConverterServiceMock.BERU_ID, "Yandex.Market")
                .setBusinessId(BERU_BUSINESS_ID)
                .setType(MbocSupplierType.FIRST_PARTY)
        );
        categoryCachingServiceMock.addCategory(TEST_CATEGORY_ID);

        migrationService = new MigrationService(migrationStatusRepository,
            migrationOffersRepository, migrationRemovedOfferRepository,
            supplierRepository, offerUpdateSequenceService, offerMetaRepository);

        supplierConverterService = new SupplierConverterServiceMock();
        DataCampIdentifiersService dataCampIdentifiersService = new DataCampIdentifiersService(
            SupplierConverterServiceMock.BERU_ID, BERU_BUSINESS_ID, supplierConverterService);

        DataCampConverterService dataCampConverterService = new DataCampConverterService(
            dataCampIdentifiersService,
            mock(OfferCategoryRestrictionCalculator.class),
            storageKeyValueService, true);

        mskuRepository = mock(MskuRepository.class);
        TestUtils.mockMskuRepositoryFindTitles(mskuRepository);
        globalVendorsCachingServiceMock = new GlobalVendorsCachingServiceMock();


        var supplierService = new SupplierService(supplierRepository);
        var categoryRuleService = new CategoryRuleService(storageKeyValueService, categoryRuleRepository);
        var acceptanceService = new AcceptanceService(categoryInfoRepository, categoryCachingServiceMock,
            supplierService, false,
            categoryRuleService, true, offerDestinationCalculator);
        migrationModelService = mock(MigrationModelService.class);
        var service = new SendDataCampOfferStatesService(
            transactionHelper,
            offerRepository,
            mskuRepository,
            antiMappingRepository,
            supplierRepository,
            queueFromContentProcessingRepository,
            offerMetaRepository,
            categoryCachingServiceMock,
            dataCampConverterService,
            migrationService,
            masterDataFor1pRepository,
            new ThrottlingLogbrokerEventPublisher<>(logbrokerEventPublisherMock),
            new ThrottlingLogbrokerEventPublisher<>(logbroker1pEventPublisherMock),
            globalVendorsCachingServiceMock,
            acceptanceService,
            storageKeyValueService,
            migrationModelService
        );
        sendDataCampOfferStatesExecutor = new SendDataCampOfferStatesExecutor(
            storageKeyValueService,
            offerUpdateSequenceService,
            service,
            mock(SolomonPushService.class)
        ) {
            @Override
            protected ExecutorService getExecutor() {
                ExecutorService res = mock(ExecutorService.class);
                when(res.submit(any(Runnable.class))).thenAnswer(invocation -> {
                    Runnable r = invocation.getArgument(0);
                    r.run();
                    return null;
                });
                return res;
            }
        };

        migrationService.checkAndUpdateCache();
    }

    @After
    public void tearDown() {
        migrationService.invalidateAll();
    }

    @Test
    public void testSendOfferStateUpdatesBatched() {
        long id = OFFER_ID_1;
        offerRepository.insertOffers(
            OfferTestUtils.simpleOffer(id++)
                .setDataCampOffer(true)
                .setShopSku("Test-ShopSku")
                .setBusinessId(BIZ_ID_SUPPLIER)
                .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
                .setDataCampContentVersion(Instant.now().toEpochMilli()),
            OfferTestUtils.simpleOffer(id++)
                .setDataCampOffer(true)
                .setShopSku("Test-ShopSku2")
                .setBusinessId(REAL_SUPPLIER_ID)
                .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
                .setDataCampContentVersion(Instant.now().toEpochMilli())
        );
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        // imitate processed seq before for get valid last generated modified_seq_id
        storageKeyValueService.putValue("datacamp_states_export_seq_id", offerUpdateSequenceService.getLastModifiedSequenceId());

        List<Offer> offers = new ArrayList<>();
        for (String shopSku : List.of(SHOP_SKU_1, SHOP_SKU_2, SHOP_SKU_3, SHOP_SKU_4, SHOP_SKU_5)) {
            Offer offer = OfferTestUtils.simpleOffer(id++)
                .setDataCampOffer(true)
                .setShopSku(shopSku)
                .setBusinessId(BIZ_ID_SUPPLIER)
                .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
                .setDataCampContentVersion(Instant.now().toEpochMilli());

            offers.add(offer);
        }
        Offer offer1p = OfferTestUtils.firstPartyOffer(id++)
            .setDataCampOffer(true)
            .setShopSku(SHOP_SKU_6)
            .setBusinessId(REAL_SUPPLIER_ID)
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setDataCampContentVersion(Instant.now().toEpochMilli());

        offers.add(offer1p);
        offers.add(OfferTestUtils.simpleOffer(id++)
                .setDataCampOffer(true)
                .setShopSku(SHOP_SKU_7)
                .setBusinessId(BIZ_ID_SUPPLIER)
                .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
                .setDataCampContentVersion(Instant.now().toEpochMilli())); // process at second batch

        offerRepository.insertOffers(offers);
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        storageKeyValueService.putValue(SendDataCampOfferStatesExecutor.LOG_ID_BATCH_SIZE, 6);
        storageKeyValueService.putValue(SendDataCampOfferStatesExecutor.CHUNK_SIZE, 2);
        storageKeyValueService.putValue(SendDataCampOfferStatesExecutor.EXPORT_BATCH_SIZE, 1);

        sendDataCampOfferStatesExecutor.execute();

        // check events sent
        List<DataCampUnitedOffersEvent> sentEvents = logbrokerEventPublisherMock.getSendEvents();
        List<DataCampUnitedOffersEvent> sent1pEvents = logbroker1pEventPublisherMock.getSendEvents();

        List<DataCampOffer.Offer> dataCampOffers = sentEvents.stream()
            .flatMap(e -> e.getPayload().getUnitedOffersList().stream())
            .flatMap(offersBatch -> offersBatch.getOfferList().stream())
            .map(DataCampUnitedOffer.UnitedOffer::getBasic)
            .collect(Collectors.toList());
        List<DataCampOffer.Offer> dataCamp1pOffers = sent1pEvents.stream()
            .flatMap(e -> e.getPayload().getUnitedOffersList().stream())
            .flatMap(offersBatch -> offersBatch.getOfferList().stream())
            .map(DataCampUnitedOffer.UnitedOffer::getBasic)
            .collect(Collectors.toList());

        assertThat(dataCampOffers)
            .hasSize(5)
            .extracting(DataCampOffer.Offer::getIdentifiers)
            .containsExactlyInAnyOrder(
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(BIZ_ID_SUPPLIER)
                    .setOfferId(SHOP_SKU_1)
                    .build(),
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(BIZ_ID_SUPPLIER)
                    .setOfferId(SHOP_SKU_2)
                    .build(),
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(BIZ_ID_SUPPLIER)
                    .setOfferId(SHOP_SKU_3)
                    .build(),
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(BIZ_ID_SUPPLIER)
                    .setOfferId(SHOP_SKU_4)
                    .build(),
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(BIZ_ID_SUPPLIER)
                    .setOfferId(SHOP_SKU_5)
                    .build());

        assertThat(dataCamp1pOffers)
            .hasSize(1)
                .extracting(DataCampOffer.Offer::getIdentifiers)
                    .contains(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setBusinessId(BERU_BUSINESS_ID)
                        .setOfferId("real." + SHOP_SKU_6)
                        .build());

        logbroker1pEventPublisherMock.clear();
        logbrokerEventPublisherMock.clear();

        sendDataCampOfferStatesExecutor.execute();

        // check events sent
        sentEvents = logbrokerEventPublisherMock.getSendEvents();

        dataCampOffers = sentEvents.stream()
            .flatMap(e -> e.getPayload().getUnitedOffersList().stream())
            .flatMap(offersBatch -> offersBatch.getOfferList().stream())
            .map(DataCampUnitedOffer.UnitedOffer::getBasic)
            .collect(Collectors.toList());
        assertThat(dataCampOffers)
            .hasSize(1)
            .extracting(DataCampOffer.Offer::getIdentifiers)
            .containsExactlyInAnyOrder(
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(BIZ_ID_SUPPLIER)
                    .setOfferId(SHOP_SKU_7)
                    .build());
    }

    @Test
    public void testSendOfferStateUpdates() {
        Offer offer1 = OfferTestUtils.simpleOffer(OFFER_ID_1)
            .setDataCampOffer(true)
            .setShopSku(SHOP_SKU_1)
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setDataCampContentVersion(Instant.now().toEpochMilli());
        Offer offer2 = OfferTestUtils.simpleOffer(OFFER_ID_2)
            .setDataCampOffer(true)
            .setShopSku(SHOP_SKU_2)
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setDataCampContentVersion(Instant.now().toEpochMilli());
        Offer offer1P = OfferTestUtils.simpleOffer(OFFER_ID_3)
            .setDataCampOffer(true)
            .setShopSku(SHOP_SKU_3)
            .setBusinessId(REAL_SUPPLIER_ID)
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setDataCampContentVersion(Instant.now().toEpochMilli());

        offerRepository.insertOffers(offer1, offer2, offer1P);
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        // re-save offer to generate duplicate events
        offer1 = offerRepository.getOfferById(OFFER_ID_1);
        offerRepository.updateOffer(offer1);
        offer1 = offerRepository.getOfferById(OFFER_ID_1);
        offerRepository.updateOffer(offer1);
        offerUpdateSequenceService.copyOfferChangesFromStaging();


        antiMappingRepository.insertBatch(
            List.of(
                new AntiMapping().setOfferId(OFFER_ID_1).setNotModelId(NOT_MODEL_ID_1).setNotSkuId(NOT_SKU_ID_2),
                new AntiMapping().setOfferId(OFFER_ID_2).setNotModelId(NOT_MODEL_ID_1).setDeletedTs(Instant.now())
            )
        );

        sendDataCampOfferStatesExecutor.execute();

        // check events sent
        List<DataCampUnitedOffersEvent> sentEvents = logbrokerEventPublisherMock.getSendEvents();

        List<DataCampOffer.Offer> dataCampOffers = sentEvents.stream()
            .flatMap(e -> e.getPayload().getUnitedOffersList().stream())
            .flatMap(offersBatch -> offersBatch.getOfferList().stream())
            .map(DataCampUnitedOffer.UnitedOffer::getBasic)
            .collect(Collectors.toList());
        assertThat(dataCampOffers)
            .hasSize(3)
            .extracting(DataCampOffer.Offer::getIdentifiers)
            .containsExactlyInAnyOrder(
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(BIZ_ID_SUPPLIER)
                    .setOfferId(SHOP_SKU_1)
                    .build(),
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(BIZ_ID_SUPPLIER)
                    .setOfferId(SHOP_SKU_2)
                    .build(),
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(BERU_BUSINESS_ID)
                    .setOfferId(RealConverter.generateSSKU(REAL_SUPPLIER_EXT_ID, SHOP_SKU_3))
                    .build());

        assertThat(dataCampOffers)
            .hasSize(3)
            .extracting(it -> it.getContent().getBinding().getAntiMappingForUc())
            .map(it -> it.toBuilder().clearMeta().build())
            .containsExactlyInAnyOrder(
                DataCampOfferMapping.AntiMapping.newBuilder()
                    .addNotSkuId(NOT_SKU_ID_2).addNotModelId(NOT_MODEL_ID_1).build(),
                DataCampOfferMapping.AntiMapping.newBuilder()
                    .addAllNotModelId(Collections.emptyList()).addAllNotSkuId(Collections.emptyList()).build(),
                DataCampOfferMapping.AntiMapping.newBuilder().build()
            );

        assertThat(dataCampOffers.get(0).getContent().getBinding().hasAntiMappingForUc()).isTrue();
        assertThat(dataCampOffers.get(1).getContent().getBinding().hasAntiMappingForUc()).isTrue();
        assertThat(dataCampOffers.get(2).getContent().getBinding().hasAntiMappingForUc()).isFalse();

        var recommendations = dataCampOffers.stream()
            .map(DataCampOffer.Offer::getResolution)
            .map(DataCampResolution.Resolution::getBySourceList)
            .flatMap(Collection::stream)
            .filter(verdicts -> verdicts.getMeta().getSource() == DataCampOfferMeta.DataSource.MARKET_MBO)
            .map(DataCampResolution.Verdicts::getVerdictList)
            .flatMap(Collection::stream)
            .map(DataCampResolution.Verdict::getResultsList)
            .flatMap(Collection::stream)
            .filter(DataCampValidationResult.ValidationResult::hasRecommendationStatus)
            .collect(Collectors.toList());

        assertThat(recommendations).hasSize(3);
        for (var result : recommendations) {
            assertThat(result.getRecommendationStatus()).isEqualTo(DataCampValidationResult.RecommendationStatus.FINE);
            assertThat(result.getApplicationsList()).hasSize(5);
        }
    }

    @Test
    public void testSendOfferStateUpdatesInMigration() {
        Offer offer1 = OfferTestUtils.simpleOffer(OFFER_ID_1)
            .setDataCampOffer(true)
            .setShopSku(SHOP_SKU_1)
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setDataCampContentVersion(Instant.now().toEpochMilli());

        MigrationStatus migrationStatus = new MigrationStatus()
            .setId(1L)
            .setTargetBusinessId(BIZ_ID_SUPPLIER)
            .setSupplierId(1234123)
            .setSourceBusinessId(1)
            .setCreatedTs(Instant.now())
            .setMigrationStatus(MigrationStatusType.ACTIVE);
        migrationStatusRepository.save(migrationStatus);

        migrationService.checkAndUpdateCache();

        offerRepository.insertOffers(offer1);
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        offer1 = offerRepository.getOfferById(OFFER_ID_1);
        offerRepository.updateOffer(offer1);
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        sendDataCampOfferStatesExecutor.execute();

        List<OfferMeta> offerMetas = offerMetaRepository.findAll();
        assertThat(offerMetas).hasSize(1)
            .allMatch(e -> e != null
                && e.getChangedDuringMigrationBusinessId() != null
                && e.getChangedDuringMigrationBusinessId() == BIZ_ID_SUPPLIER)
            .extracting(OfferMeta::getOfferId)
            .containsExactlyInAnyOrder(OFFER_ID_1);

        List<DataCampUnitedOffersEvent> sentEvents = logbrokerEventPublisherMock.getSendEvents();
        assertThat(sentEvents).isEmpty();
    }

    @Test
    public void testSendOfferOneMissing() {
        Offer offer1 = OfferTestUtils.simpleOffer(OFFER_ID_1)
            .setDataCampOffer(true)
            .setShopSku(SHOP_SKU_1)
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setDataCampContentVersion(Instant.now().toEpochMilli());
        Offer offer2 = OfferTestUtils.simpleOffer(OFFER_ID_2)
            .setDataCampOffer(true)
            .setShopSku(SHOP_SKU_2)
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setDataCampContentVersion(Instant.now().toEpochMilli());

        offerRepository.insertOffers(offer2);
        offerRepository.deleteAllInTest();
        offerRepository.insertOffers(offer1);

        offerUpdateSequenceService.copyOfferChangesFromStaging();

        sendDataCampOfferStatesExecutor.execute();

        // check events sent
        List<DataCampUnitedOffersEvent> sentEvents = logbrokerEventPublisherMock.getSendEvents();

        List<DataCampOffer.Offer> dataCampOffers = sentEvents.stream()
            .flatMap(e -> e.getPayload().getUnitedOffersList().stream())
            .flatMap(offersBatch -> offersBatch.getOfferList().stream())
            .map(DataCampUnitedOffer.UnitedOffer::getBasic)
            .collect(Collectors.toList());
        assertThat(dataCampOffers)
            .hasSize(1)
            .extracting(DataCampOffer.Offer::getIdentifiers)
            .containsExactlyInAnyOrder(
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(BIZ_ID_SUPPLIER)
                    .setOfferId(SHOP_SKU_1)
                    .build());
    }

    @Test
    public void testSendContentProcessingResponse() throws Exception {
        var offer = OfferTestUtils.simpleOffer(OFFER_ID_1)
            .setDataCampOffer(true)
            .setShopSku(SHOP_SKU_1)
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setDataCampContentVersion(Instant.now().toEpochMilli());
        offerRepository.insertOffers(offer);
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var response = DataCampOfferMarketContent.MarketContentProcessing.newBuilder()
            .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_OK)
            .build();

        queueFromContentProcessingRepository.insert(new ContentProcessingResponse()
            .setOfferId(OFFER_ID_1)
            .setProcessingResponse(response)
        );

        sendDataCampOfferStatesExecutor.execute();

        var sentEvents = logbrokerEventPublisherMock.getSendEvents();
        List<DataCampOffer.Offer> dataCampOffers = sentEvents.stream()
            .flatMap(e -> e.getPayload().getUnitedOffersList().stream())
            .flatMap(offersBatch -> offersBatch.getOfferList().stream())
            .map(DataCampUnitedOffer.UnitedOffer::getBasic)
            .collect(Collectors.toList());
        assertThat(dataCampOffers)
            .hasSize(1)
            .extracting(DataCampOffer.Offer::getIdentifiers)
            .containsExactlyInAnyOrder(
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(BIZ_ID_SUPPLIER)
                    .setOfferId(SHOP_SKU_1)
                    .build());
        assertThat(dataCampOffers)
            .hasSize(1)
            .extracting(o -> o.getContent().getPartner().getMarketSpecificContent().getProcessingResponse())
            .extracting(r -> r.toBuilder().clearMeta().build())
            .containsExactlyInAnyOrder(DataCampOfferMarketContent.MarketContentProcessing.newBuilder().build());

        logbrokerEventPublisherMock.clear();

        var error = MbocErrors.get().contentProcessingFailed("123");
        offer = offerRepository.getOfferById(OFFER_ID_1);
        offer.setContentStatusActiveError(error);
        offerRepository.updateOffer(offer);
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        sendDataCampOfferStatesExecutor.execute();

        var sentEvents2 = logbrokerEventPublisherMock.getSendEvents();
        List<DataCampOffer.Offer> dataCampOffers2 = sentEvents2.stream()
            .flatMap(e -> e.getPayload().getUnitedOffersList().stream())
            .flatMap(offersBatch -> offersBatch.getOfferList().stream())
            .map(DataCampUnitedOffer.UnitedOffer::getBasic)
            .collect(Collectors.toList());
        assertThat(dataCampOffers2)
            .hasSize(1)
            .extracting(DataCampOffer.Offer::getIdentifiers)
            .containsExactlyInAnyOrder(
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(BIZ_ID_SUPPLIER)
                    .setOfferId(SHOP_SKU_1)
                    .build());
        assertThat(dataCampOffers2)
            .hasSize(1)
            .extracting(o -> o.getContent().getStatus().getContentSystemStatus().getActiveErrorList())
            .hasSize(1);

        var recommendations = dataCampOffers2.stream()
            .map(DataCampOffer.Offer::getResolution)
            .map(DataCampResolution.Resolution::getBySourceList)
            .flatMap(Collection::stream)
            .filter(verdicts -> verdicts.getMeta().getSource() == DataCampOfferMeta.DataSource.MARKET_MBO)
            .map(DataCampResolution.Verdicts::getVerdictList)
            .flatMap(Collection::stream)
            .map(DataCampResolution.Verdict::getResultsList)
            .flatMap(Collection::stream)
            .filter(DataCampValidationResult.ValidationResult::hasRecommendationStatus)
            .collect(Collectors.toList());

        assertThat(recommendations).hasSize(1);
        var result = recommendations.get(0);
        assertThat(result.getRecommendationStatus()).isEqualTo(DataCampValidationResult.RecommendationStatus.FINE);
        assertThat(result.getApplicationsList()).hasSize(5);
    }

    @Test
    public void testSendDuplicateOfferStateUpdates() {
        long seqFrom = storageKeyValueService.getLong("datacamp_states_export_seq_id", 0L);
        int seqIdBatchSize = storageKeyValueService.getInt("datacamp_states_log_id_batch_size", 100);
        var offer = OfferTestUtils.simpleOffer(OFFER_ID_1)
            .setDataCampOffer(true)
            .setShopSku(SHOP_SKU_1)
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setDataCampContentVersion(Instant.now().toEpochMilli());

        var offer2 = OfferTestUtils.simpleOffer(OFFER_ID_2)
            .setDataCampOffer(true)
            .setShopSku(SHOP_SKU_2)
            .setBusinessId(BIZ_ID_SUPPLIER)
            .setCategoryIdForTests(TEST_CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setDataCampContentVersion(Instant.now().toEpochMilli());

        offerRepository.insertOffers(offer, offer2);
        offerUpdateSequenceService.copyOfferChangesFromStaging();
        offerUpdateSequenceService.markOffersModified(List.of(offer.getId()));
        offerUpdateSequenceService.copyOfferChangesFromStaging();

        var modified = offerUpdateSequenceService.getModifiedRecordsIdBatch(seqFrom, seqIdBatchSize);
        Assertions.assertThat(modified).hasSize(3);
        Assertions.assertThat(modified.get(0).getKey()).isEqualTo(offer.getId());
        Assertions.assertThat(modified.get(1).getKey()).isEqualTo(offer2.getId());
        Assertions.assertThat(modified.get(2).getKey()).isEqualTo(offer.getId());

        var response = DataCampOfferMarketContent.MarketContentProcessing.newBuilder()
            .setResult(DataCampOfferMarketContent.MarketContentProcessing.TotalResult.TOTAL_OK)
            .build();

        queueFromContentProcessingRepository.insert(new ContentProcessingResponse()
            .setOfferId(OFFER_ID_1)
            .setProcessingResponse(response)
        );

        queueFromContentProcessingRepository.insert(new ContentProcessingResponse()
            .setOfferId(OFFER_ID_2)
            .setProcessingResponse(response)
        );

        sendDataCampOfferStatesExecutor.execute();

        var sentEvents = logbrokerEventPublisherMock.getSendEvents();
        Assertions.assertThat(sentEvents).hasSize(1);
        List<DataCampOffer.Offer> dataCampOffers = sentEvents.stream()
            .flatMap(e -> e.getPayload().getUnitedOffersList().stream())
            .flatMap(offersBatch -> offersBatch.getOfferList().stream())
            .map(DataCampUnitedOffer.UnitedOffer::getBasic)
            .collect(Collectors.toList());
        assertThat(dataCampOffers)
            .hasSize(2)
            .extracting(DataCampOffer.Offer::getIdentifiers)
            .containsExactlyInAnyOrder(
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(BIZ_ID_SUPPLIER)
                    .setOfferId(SHOP_SKU_1)
                    .build(),
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(BIZ_ID_SUPPLIER)
                    .setOfferId(SHOP_SKU_2)
                    .build());
        assertThat(dataCampOffers)
            .hasSize(2)
            .extracting(o -> o.getContent().getPartner().getMarketSpecificContent().getProcessingResponse())
            .extracting(r -> r.toBuilder().clearMeta().build())
            .containsExactlyInAnyOrder(
                DataCampOfferMarketContent.MarketContentProcessing.newBuilder().build(),
                DataCampOfferMarketContent.MarketContentProcessing.newBuilder().build());
    }
}
