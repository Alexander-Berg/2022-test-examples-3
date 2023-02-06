package ru.yandex.market.mboc.app.proto.mappings;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.app.mapping.RecheckMappingService;
import ru.yandex.market.mboc.app.offers.OfferProtoConverter;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.MboMappingsServiceImpl;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepository;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.datacamp.service.converter.DataCampConverterService;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.Msku;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.dict.WarehouseServiceAuditRecorder;
import ru.yandex.market.mboc.common.dict.WarehouseServiceRepository;
import ru.yandex.market.mboc.common.dict.WarehouseServiceService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationRemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.offers.repository.Offer1PService;
import ru.yandex.market.mboc.common.offers.repository.OfferMetaRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferStatService;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.business.BusinessSupplierService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.migration.MigrationService;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.antimapping.AntiMappingService;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.offers.upload.OfferChangeForUploadObserver;
import ru.yandex.market.mboc.common.services.proto.AddProductInfoHelperService;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.services.proto.MboMappingsHelperService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

/**
 * @author yuramalinov
 * @created 06.07.18
 */
public abstract class AbstractMboMappingsServiceImplTest extends BaseMbocAppTest {
    protected static final int BERU_ID = 465852;
    protected static final long SEED = 9;

    @Autowired
    protected OfferRepository offerRepository;
    @Autowired
    protected OfferChangeForUploadObserver uploadObserver;
    @Autowired
    @Qualifier("slaveOfferRepository")
    protected OfferRepository slaveOfferRepository;
    @SpyBean
    protected SupplierRepository supplierRepository;
    @Autowired
    protected MigrationStatusRepository migrationStatusRepository;
    @Autowired
    protected MigrationOfferRepository migrationOfferRepository;
    @Autowired
    protected MigrationRemovedOfferRepository migrationRemovedOfferRepository;
    @SpyBean
    protected MskuRepository mskuRepository;
    @Autowired
    protected AntiMappingRepository antiMappingRepository;
    @Autowired
    private OfferMetaRepository offerMetaRepository;
    @Autowired
    protected OfferUpdateSequenceService offerUpdateSequenceService;
    @Autowired
    protected ContentProcessingQueueRepository contentProcessingQueue;
    @Autowired
    protected WarehouseServiceRepository warehouseServiceRepository;

    protected MboMappingsServiceImpl service;
    protected ModelStorageCachingServiceMock modelStorageCachingService;
    protected CategoryCachingServiceMock categoryCachingService;
    protected Offer1PService offer1PService;
    protected OfferProtoConverter protoConverter;
    protected OfferStatService offerStatService;
    protected AddProductInfoHelperService productInfoHelperService;
    protected MasterDataServiceMock masterDataServiceMock;
    protected SupplierDocumentServiceMock supplierDocumentServiceMock;
    protected MasterDataHelperService masterDataHelperService;
    protected BusinessSupplierService businessSupplierService;
    protected SupplierService supplierService;
    protected MigrationService migrationService;
    protected AntiMappingService antiMappingService;

    protected OfferRepository masterSpy;
    protected OfferRepository slaveSpy;
    protected StorageKeyValueServiceMock storageKeyValueService;
    protected OffersProcessingStatusService offersProcessingStatusService;
    protected WarehouseServiceService warehouseServiceService;
    protected DataCampConverterService dataCampConverterService;
    protected OfferMappingActionService offerMappingActionService;

    @Before
    public void setup() {
        masterSpy = Mockito.spy(offerRepository);
        slaveSpy = Mockito.spy(slaveOfferRepository);

        storageKeyValueService = new StorageKeyValueServiceMock();
        masterDataServiceMock = new MasterDataServiceMock();
        supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
            new SupplierConverterServiceMock(), storageKeyValueService);
        businessSupplierService = new BusinessSupplierService(supplierRepository, masterSpy);

        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("app-offers/app-test-suppliers.yml"));
        masterSpy.insertOffers(YamlTestUtil.readOffersFromResources("app-offers/app-search-test.yml"));
        YamlTestUtil.readOffersFromResources("app-offers/app-search-test.yml").forEach(o -> {
            if (o.getUploadToYtStamp() == null) {
                return;
            }
            OfferTestUtils.hardSetYtStamp(namedParameterJdbcTemplate, o.getId(), o.getUploadToYtStamp());
        });

        addTestMasterData(masterDataHelperService);

        offer1PService = new Offer1PService(BERU_ID, supplierRepository);
        categoryCachingService = new CategoryCachingServiceMock().enableAuto();

        protoConverter = new OfferProtoConverter(categoryCachingService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), mskuRepository, BERU_ID);
        modelStorageCachingService = new ModelStorageCachingServiceMock();

        offerStatService = Mockito.mock(OfferStatService.class);
        productInfoHelperService = Mockito.mock(AddProductInfoHelperService.class);

        supplierService = new SupplierService(supplierRepository);

        migrationService = new MigrationService(migrationStatusRepository,
            migrationOfferRepository, migrationRemovedOfferRepository,
            supplierRepository, offerUpdateSequenceService, offerMetaRepository);

        antiMappingService = new AntiMappingService(antiMappingRepository, transactionHelper);

        var needContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        var categoryKnowledgeService = new CategoryKnowledgeServiceMock();
        categoryKnowledgeService.enableAllCategories();
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(modelStorageCachingService, null,
            supplierRepository);
        LegacyOfferMappingActionService legacyOfferMappingActionService = new LegacyOfferMappingActionService(
            needContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);

        offersProcessingStatusService = new OffersProcessingStatusService(null, needContentStatusService,
            supplierService, categoryKnowledgeService, retrieveMappingSkuTypeService,
            offerMappingActionService, categoryInfoRepository, antiMappingRepository, offerDestinationCalculator,
            storageKeyValueService, new FastSkuMappingsService(needContentStatusService), false, false, 3, categoryInfoCache);
        warehouseServiceService = Mockito.spy(new WarehouseServiceService(
            warehouseServiceRepository,
            masterSpy,
            supplierRepository,
            mskuRepository,
            Mockito.mock(WarehouseServiceAuditRecorder.class)
        ));
        dataCampConverterService = new DataCampConverterService(
            Mockito.mock(DataCampIdentifiersService.class),
            Mockito.mock(OfferCategoryRestrictionCalculator.class),
            storageKeyValueService,
            true
        );

        setupService();

        modelStorageCachingService
            .addModel(createTestModel(101010L, 10))
            .addModel(createTestModel(303030L, 22))
            .addModel(createTestModel(171717L, 88));

        migrationService.checkAndUpdateCache();
    }

    @After
    public void tearDown() {
        migrationService.invalidateAll();
    }

    protected void setupService() {
        service = new MboMappingsServiceImpl(
            masterSpy,
            slaveSpy, productInfoHelperService,
            protoConverter,
            supplierRepository,
            offer1PService,
            modelStorageCachingService,
            Mockito.mock(ComplexMonitoring.class),
            categoryCachingService,
            null,
            BERU_ID,
            offerStatService,
            new MboMappingsHelperService(supplierRepository, masterDataHelperService,
                businessSupplierService, BERU_ID),
            new BusinessSupplierService(supplierRepository, masterSpy),
            supplierService, storageKeyValueService, mskuRepository, migrationService,
            antiMappingRepository, antiMappingService,
            offersProcessingStatusService, contentProcessingQueue, transactionHelper, warehouseServiceService,
            dataCampConverterService, offerMappingActionService,
            new RecheckMappingService(offerRepository,
                offerMappingActionService,
                offersProcessingStatusService,
                transactionHelper,
                antiMappingService));
    }


    public EnhancedRandom defaultRandom() {
        return TestDataUtils.defaultRandom(SEED);
    }

    public MasterData createMasterData(int suppierId, String shopSku,
                                       int minShipment, String customCommunityCode) {
        MasterData result = TestDataUtils.generateMasterData(shopSku, suppierId, defaultRandom());
        result.setMinShipment(minShipment);
        result.setCustomsCommodityCode(customCommunityCode);
        return result;
    }

    public void addTestMasterData(MasterDataHelperService masterDataHelperService) {
        masterDataHelperService.saveSskuMasterDataAndDocuments(Arrays.asList(
            createMasterData(61, "sku4", 4, "cccode4"),
            createMasterData(77, "sku5", 5, "cccode5")
        ));
    }

    public Model createTestModel(long id, long categoryId) {
        return new Model()
            .setId(id)
            .setTitle("Model " + id)
            .setCategoryId(categoryId)
            .setModelType(Model.ModelType.GURU)
            .setVendorCodes(Arrays.asList("TEST", "TSET"))
            .setBarCodes(Arrays.asList("A", "B"))
            .setParameterValues(Collections.singletonList(ModelStorage.ParameterValue.newBuilder()
                .setXslName("test")
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setBoolValue(true)
                .build()));
    }

    protected Offer offer(int id, int supplierId, String shopSku) {
        return Offer.builder()
            .id(id)
            .title("1")
            .categoryId(12345L)
            .mappingDestination(Offer.MappingDestination.BLUE)
            .approvedSkuMapping(new Offer.Mapping(22L, DateTimeUtils.dateTimeNow(), null))
            .approvedSkuMappingConfidence(Offer.MappingConfidence.CONTENT)
            .shopCategoryName("c")
            .isOfferContentPresent(true)
            .offerContent(OfferContent.initEmptyContent())
            .uploadToYtStamp(11L)
            .shopSku(shopSku)
            .businessId(supplierId)
            .serviceOffers(List.of(new Offer.ServiceOffer(
                supplierId, MbocSupplierType.THIRD_PARTY, Offer.AcceptanceStatus.OK)))
            .build();
    }

    protected Offer createOfferWithApprovedSku(int businessId, String shopSku, long skuId, Supplier supplier) {
        return new Offer()
            .setBusinessId(businessId)
            .setShopSku(shopSku)
            .setTitle("testTitle")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName("testName")
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setApprovedSkuMappingInternal(new Offer.Mapping(skuId, LocalDateTime.now()))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER_SELF)
            .addNewServiceOfferIfNotExistsForTests(supplier)
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK);
    }

    protected Msku createMsku(long skuId, long parentModelId) {
        return new Msku()
            .setMarketSkuId(skuId)
            .setParentModelId(parentModelId)
            .setCategoryId(1L)
            .setVendorId(1L)
            .setCreationTs(Instant.now())
            .setModificationTs(Instant.now());
    }
}
