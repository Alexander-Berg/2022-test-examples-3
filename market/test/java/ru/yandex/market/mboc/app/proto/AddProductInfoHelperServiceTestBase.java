package ru.yandex.market.mboc.app.proto;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.common.datacamp.repository.TempImportChangeDeltaRepository;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.honestmark.OfferCategoryRestrictionCalculator;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.model.MasterDataFromMdiConverter;
import ru.yandex.market.mboc.common.masterdata.services.category.MboTimeUnitAliasesService;
import ru.yandex.market.mboc.common.offers.acceptance.rule.CategoryRuleService;
import ru.yandex.market.mboc.common.offers.acceptance.service.AcceptanceService;
import ru.yandex.market.mboc.common.offers.mapping.service.FastSkuMappingsService;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.AntiMappingRepository;
import ru.yandex.market.mboc.common.offers.repository.IMasterDataRepository;
import ru.yandex.market.mboc.common.offers.repository.MboAuditServiceMock;
import ru.yandex.market.mboc.common.offers.repository.MigrationOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationRemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.repository.MigrationStatusRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferBatchProcessor;
import ru.yandex.market.mboc.common.offers.repository.OfferContentRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferMetaRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferUpdateSequenceService;
import ru.yandex.market.mboc.common.offers.repository.RemovedOfferRepository;
import ru.yandex.market.mboc.common.offers.settings.ApplySettingsService;
import ru.yandex.market.mboc.common.services.books.BooksService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryMock;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeServiceMock;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryMock;
import ru.yandex.market.mboc.common.services.migration.MigrationService;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.offers.auto_approves.CompositeAutoApproveService;
import ru.yandex.market.mboc.common.services.offers.auto_approves.SuggestAutoApproveServiceImpl;
import ru.yandex.market.mboc.common.services.offers.auto_approves.SupplierAutoApproveServiceImpl;
import ru.yandex.market.mboc.common.services.offers.enrichment.OffersEnrichmentService;
import ru.yandex.market.mboc.common.services.offers.mapping.LegacyOfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.OfferMappingActionService;
import ru.yandex.market.mboc.common.services.offers.mapping.RecheckClassificationService;
import ru.yandex.market.mboc.common.services.offers.mapping.RetrieveMappingSkuTypeService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;
import ru.yandex.market.mboc.common.services.proto.AddProductInfoHelperService;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.mboc.common.vendor.models.CachedGlobalVendor;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mdm.http.MasterDataProto;
import ru.yandex.market.mdm.http.MdmCommon;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yuramalinov
 * @created 25.11.2019
 */
@SuppressWarnings("checkstyle:MagicNumber")
public abstract class AddProductInfoHelperServiceTestBase extends BaseDbTestClass {
    protected static final int BLUE_SHOP_ID = OfferTestUtils.TEST_SUPPLIER_ID;
    protected static final int BLUE_SHOP_ID_3P = 434343;
    protected static final int WHITE_SHOP_ID = 4242;
    protected static final long MODEL_ID = 101;
    protected static final long MODEL_ID_2 = 102;
    protected static final int PSKU10_MODEL_ID = 107;
    protected static final int PSKU20_MODEL_ID = 108;
    protected static final int FAST_SKU_MODEL_ID = 109;
    protected static final int NONEXISING_MODEL_ID = 1;
    protected static final long CATEGORY_ID = 1;
    protected static final long CATEGORY_ID_2 = 2;
    protected static final String VALID_COUNTRY = "Россия";
    protected static final String INVALID_SHELF_LIFE = "123456789";
    protected static final int INVALID_MIN_SHIPMENT = 10000000;
    @Autowired
    protected OfferRepository offerRepository;
    @Autowired
    protected IMasterDataRepository masterDataFor1pRepository;
    @Autowired
    protected SupplierRepository supplierRepository;
    @Autowired
    protected MboAuditServiceMock auditServiceMock;
    @Autowired
    protected MigrationStatusRepository migrationStatusRepository;
    @Autowired
    protected MigrationOfferRepository migrationOfferRepository;
    @Autowired
    protected MigrationRemovedOfferRepository migrationRemovedOfferRepository;
    @Autowired
    protected RemovedOfferRepository removedOfferRepository;
    @Autowired
    private OfferMetaRepository offerMetaRepository;
    @Autowired
    protected OfferUpdateSequenceService offerUpdateSequenceService;
    @Autowired
    protected AntiMappingRepository antiMappingRepository;
    @Autowired
    protected OfferBatchProcessor offerBatchProcessor;
    @Autowired
    protected TempImportChangeDeltaRepository tempImportChangeDeltaRepository;
    @Autowired
    protected OfferContentRepository offerContentRepository;
    @Autowired
    protected CategoryRuleService categoryRuleService;

    protected SupplierService supplierService;

    protected ModelStorageCachingServiceMock modelServiceMock;
    protected CategoryCachingServiceMock categoryCachingService;
    protected AddProductInfoHelperService service;
    protected OffersEnrichmentService offersEnrichmentService;
    protected MasterDataServiceMock masterDataServiceMock;
    protected CategoryKnowledgeServiceMock categoryKnowledgeService;
    protected MigrationService migrationService;
    private OfferMappingActionService offerMappingActionService;
    private MasterDataHelperService masterDataHelperService;
    private NeedContentStatusService toNeedContentStatusService;
    private SupplierDocumentServiceMock supplierDocumentServiceMock;
    protected GlobalVendorsCachingService globalVendorsCachingService;
    private RecheckClassificationService recheckClassificationService;


    @SuppressWarnings("checkstyle:MethodLength")
    @Before
    public void setup() {
        supplierService = new SupplierService(supplierRepository);
        modelServiceMock = Mockito.spy(new ModelStorageCachingServiceMock());
        categoryCachingService = new CategoryCachingServiceMock().enableAuto().setGoodContentDefault(true);
        toNeedContentStatusService = new NeedContentStatusService(categoryCachingService, supplierService,
            new BooksService(categoryCachingService, Collections.emptySet()));
        var legacyOfferMappingActionService = new LegacyOfferMappingActionService(toNeedContentStatusService,
            Mockito.mock(OfferCategoryRestrictionCalculator.class), offerDestinationCalculator, storageKeyValueService);
        offerMappingActionService = new OfferMappingActionService(legacyOfferMappingActionService);
        masterDataServiceMock = new MasterDataServiceMock();
        supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
            new SupplierConverterServiceMock(), storageKeyValueService);
        categoryInfoRepository = new CategoryInfoRepositoryMock(new MboUsersRepositoryMock());
        auditServiceMock.reset();

        MasterDataFromMdiConverter masterDataFromMdiConverter = new MasterDataFromMdiConverter(
            Mockito.mock(MboTimeUnitAliasesService.class)
        );

        globalVendorsCachingService = Mockito.mock(GlobalVendorsCachingService.class);
        CachedGlobalVendor globalVendor = new CachedGlobalVendor();
        globalVendor.setRequireGtinBarcodes(true);
        Mockito.doReturn(Optional.of(globalVendor)).when(globalVendorsCachingService).getVendor(Mockito.anyLong());

        Mockito.doAnswer(invocation -> {
            //noinspection unchecked
            var ids = (Collection<Long>) invocation.getArgument(0);
            var result = new HashMap<Long, Optional<CachedGlobalVendor>>();
            ids.forEach(id -> {
                var copy = new CachedGlobalVendor();
                copy.setId(id);
                copy.setCachedAt(globalVendor.getCachedAt());
                copy.setNames(globalVendor.getNames());
                copy.setRequireGtinBarcodes(globalVendor.isRequireGtinBarcodes());
                result.put(id, Optional.of(copy));
            });
            return result;
        }).when(globalVendorsCachingService).getVendorMap(Mockito.any());
        offersEnrichmentService = Mockito.mock(OffersEnrichmentService.class);

        migrationService = new MigrationService(migrationStatusRepository,
            migrationOfferRepository, migrationRemovedOfferRepository,
            supplierRepository, offerUpdateSequenceService, offerMetaRepository);

        var modelStorageCachingService = new ModelStorageCachingServiceMock();
        var retrieveMappingSkuTypeService = new RetrieveMappingSkuTypeService(
            modelStorageCachingService, offerBatchProcessor, supplierRepository);

        categoryKnowledgeService = new CategoryKnowledgeServiceMock();

        OffersProcessingStatusService offersProcessingStatusService = new OffersProcessingStatusService(
            offerBatchProcessor,
            toNeedContentStatusService,
            supplierService,
            categoryKnowledgeService,
            retrieveMappingSkuTypeService,
            offerMappingActionService,
            categoryInfoRepository,
            antiMappingRepository,
            offerDestinationCalculator,
            storageKeyValueService,
            new FastSkuMappingsService(toNeedContentStatusService),
            false,
            false,
            3, categoryInfoCache);

        var suggestAutoApprovedService = new SuggestAutoApproveServiceImpl(categoryInfoRepository, modelServiceMock,
            offerMappingActionService, antiMappingRepository);
        var supplierAutoApprovedService = new SupplierAutoApproveServiceImpl(modelServiceMock,
            offerMappingActionService, antiMappingRepository);
        var compositeAutoApproveService = new CompositeAutoApproveService(
            antiMappingRepository, supplierAutoApprovedService, suggestAutoApprovedService);
        var acceptanceService = new AcceptanceService(categoryInfoRepository, categoryCachingService, supplierService,
            false, categoryRuleService, true, offerDestinationCalculator);
        var fastSkuMappingsService = new FastSkuMappingsService(toNeedContentStatusService);
        var applySettingsService = new ApplySettingsService(supplierService,
            acceptanceService, compositeAutoApproveService, offersProcessingStatusService, fastSkuMappingsService);

        recheckClassificationService = new RecheckClassificationService(offerMappingActionService, categoryCachingService,
                supplierService);
        service = new AddProductInfoHelperService(
            offerRepository,
            supplierService,
            modelServiceMock,
            offerMappingActionService,
            TransactionHelper.MOCK,
            categoryCachingService,
            masterDataHelperService,
            masterDataFromMdiConverter,
            offersEnrichmentService,
            applySettingsService,
            offersProcessingStatusService,
            migrationService,
            removedOfferRepository,
            antiMappingRepository,
            tempImportChangeDeltaRepository,
            offerDestinationCalculator,
            storageKeyValueService,
            hashCalculator,
            recheckClassificationService,
            true);

        Supplier blueShift = new Supplier(BLUE_SHOP_ID, "Test");
        blueShift.setType(MbocSupplierType.REAL_SUPPLIER);
        blueShift.setRealSupplierId("0001");
        Supplier whiteNoise = new Supplier(WHITE_SHOP_ID, "Sh-sh-sh-sh");
        whiteNoise.setType(MbocSupplierType.MARKET_SHOP);
        supplierRepository.insert(blueShift);
        supplierRepository.insert(whiteNoise);
        supplierRepository.insert(blueShift.setId(BLUE_SHOP_ID_3P).setType(MbocSupplierType.THIRD_PARTY));

        modelServiceMock.addModel(new Model()
            .setId(MODEL_ID)
            .setCategoryId(1)
            .setTitle("TestTitle")
            .setPublishedOnBlueMarket(true)
            .setSkuModel(true)
            .setModelType(Model.ModelType.GURU)
            .setModelQuality(Model.ModelQuality.OPERATOR));

        modelServiceMock.addModel(new Model()
            .setId(MODEL_ID_2)
            .setCategoryId(1)
            .setTitle("TestTitle2")
            .setPublishedOnBlueMarket(true)
            .setSkuModel(true)
            .setModelType(Model.ModelType.GURU)
            .setModelQuality(Model.ModelQuality.OPERATOR));

        modelServiceMock.addModel(new Model()
            .setId(PSKU10_MODEL_ID)
            .setCategoryId(1)
            .setTitle("TestPsku10Title")
            .setSupplierId((long) BLUE_SHOP_ID)
            .setPublishedOnBlueMarket(true)
            .setSkuModel(true)
            .setModelType(Model.ModelType.PARTNER_SKU)
            .setModelQuality(Model.ModelQuality.PARTNER));

        modelServiceMock.addModel(new Model()
            .setId(PSKU20_MODEL_ID)
            .setCategoryId(1)
            .setTitle("TestPsku20Title")
            .setSupplierId((long) BLUE_SHOP_ID)
            .setPublishedOnBlueMarket(true)
            .setSkuModel(true)
            .setModelType(Model.ModelType.SKU)
            .setModelQuality(Model.ModelQuality.PARTNER));

        modelServiceMock.addModel(new Model()
            .setId(FAST_SKU_MODEL_ID)
            .setCategoryId(1)
            .setTitle("TestFastSkuTitle")
            .setSupplierId((long) BLUE_SHOP_ID)
            .setPublishedOnBlueMarket(true)
            .setSkuModel(true)
            .setModelType(Model.ModelType.FAST_SKU));

        Supplier bizSupplier = OfferTestUtils.businessSupplier();
        supplierRepository.insert(bizSupplier);

        migrationService.checkAndUpdateCache();
    }

    @After
    public void tearDown() {
        migrationService.invalidateAll();
    }

    protected Offer commonBlueOffer() {
        return new Offer()
            .setBusinessId(BLUE_SHOP_ID)
            .setCategoryIdForTests(CATEGORY_ID, Offer.BindingKind.SUGGESTED)
            .setShopSku("ShopSku")
            .setTitle("Before")
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setShopCategoryName("Before")
            .setSupplierSkuMapping(new Offer.Mapping(123, DateTimeUtils.dateTimeNow(), Offer.SkuType.MARKET))
            .setSupplierSkuMappingStatus(Offer.MappingStatus.NEW)
            .addNewServiceOfferIfNotExistsForTests(OfferTestUtils.simpleSupplier());
    }

    protected Offer commonWhiteOffer() {
        return commonBlueOffer()
            .setMappingDestination(Offer.MappingDestination.WHITE)
            .setBusinessId(WHITE_SHOP_ID)
            .setIsOfferContentPresent(true)
            .storeOfferContent(OfferContent.initEmptyContent())
            .setServiceOffers(Collections.emptyList())
            .addNewServiceOfferIfNotExistsForTests(OfferTestUtils.whiteSupplier());
    }

    protected MboMappings.ProductUpdateRequestInfo.Builder commonRequestInfo() {
        return MboMappings.ProductUpdateRequestInfo.newBuilder()
            .setChangeSource(MboMappings.ProductUpdateRequestInfo.ChangeSource.CONTENT)
            .setUserLogin("test-login");
    }

    protected MboMappings.ProviderProductInfo.Builder whiteProductInfo() {
        return MboMappings.ProviderProductInfo.newBuilder()
            .setShopId(WHITE_SHOP_ID)
            .setShopSkuId("ShopSku")
            .setTitle("Title")
            .setShopCategoryName("ShopCategoryName")
            .setDescription("Description")
            .setVendor("Vendor")
            .setVendorCode("VendorCode")
            .addBarcode("123")
            .addBarcode("321")
            .setMappingType(MboMappings.MappingType.PRICE_COMPARISION);
    }

    private MboMappings.ProviderProductInfo.Builder blueProductInfoWithoutMD() {
        return MboMappings.ProviderProductInfo.newBuilder()
            .setShopId(BLUE_SHOP_ID)
            .setShopSkuId("ShopSku")
            .setTitle("Title")
            .setShopCategoryName("ShopCategoryName")
            .setDescription("Description")
            .setVendor("Vendor")
            .setVendorCode("VendorCode")
            .addBarcode("123")
            .addBarcode("321")
            .setMarketSkuId(MODEL_ID)
            .setMappingType(MboMappings.MappingType.SUPPLIER);
    }

    protected MboMappings.ProviderProductInfo.Builder fmcgProductInfoWithoutMD() {
        return MboMappings.ProviderProductInfo.newBuilder()
            .setShopId(OfferTestUtils.FMCG_SUPPLIER_ID)
            .setShopSkuId("ShopSku")
            .setTitle("Title")
            .setShopCategoryName("ShopCategoryName")
            .setDescription("Description")
            .setVendor("Vendor")
            .setVendorCode("VendorCode")
            .addBarcode("123")
            .addBarcode("321")
            .setMarketSkuId(MODEL_ID);
    }

    protected MboMappings.ProviderProductInfo.Builder blueProductInfo() {
        return blueProductInfoWithoutMD().setMasterDataInfo(masterData());
    }

    protected MasterDataProto.MasterDataInfo.Builder masterData() {
        return MasterDataProto.MasterDataInfo.newBuilder()
            .setProviderProductMasterData(providerProductMasterData().build())
            .setShelfLife("180")
            .setLifeTime("365")
            .setGuaranteePeriod("30");
    }

    protected MasterDataProto.ProviderProductMasterData.Builder providerProductMasterData() {
        return MasterDataProto.ProviderProductMasterData.newBuilder()
            .addManufacturerCountry(VALID_COUNTRY)
            .setManufacturer("Manufacturer")
            .setMinShipment(100)
            .setTransportUnitSize(5)
            .setQuantumOfSupply(20)
            .setDeliveryTime(3);
    }

    protected MasterDataProto.OperationInfo validationError(
        MboMappings.ProviderProductInfo offerInfo, String code, String template, String params
    ) {
        return MasterDataProto.OperationInfo.newBuilder()
            .setStatus(MasterDataProto.OperationStatus.VALIDATION_ERROR)
            .setKey(MdmCommon.ShopSkuKey.newBuilder()
                .setSupplierId(offerInfo.getShopId())
                .setShopSku(offerInfo.getShopSkuId())
                .build())
            .addErrors(MasterDataProto.ErrorInfo.newBuilder()
                .setErrorCode(code)
                .setMessage("Some error with template: " + template)
                .setMustacheTemplate(template)
                .setJsonDataForMustacheTemplate(params)
                .build()
            )
            .build();
    }

    protected void validateHasError(MboMappings.ProviderProductInfoResponse.ProductResult result,
                                    MboMappings.ProviderProductInfoResponse.ErrorKind errorKind) {
        assertThat(result.getErrorsList().stream()
            .map(MboMappings.ProviderProductInfoResponse.Error::getErrorKind)
            .collect(Collectors.toList())
        ).contains(errorKind);
    }
}
