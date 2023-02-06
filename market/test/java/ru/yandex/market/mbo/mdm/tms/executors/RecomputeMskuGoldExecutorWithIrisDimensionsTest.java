package ru.yandex.market.mbo.mdm.tms.executors;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.WeightDimensionsSilverItemSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenBlocksPostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenSplitterMerger;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuSilverItemPreProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MskuSilverSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmModificationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuGoldenParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.SupplierDqScore;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGoodGroupRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SupplierDqScoreRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CustomsCommCodeRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.GlobalParamValueService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.WarehouseProjectionCacheImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.RslGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.SurplusAndCisGoldenItemServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.WeightDimensionForceInheritanceService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.WeightDimensionsForceInheritancePostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.WeightDimensionsGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuCalculatingProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuCalculatingProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingDataProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingPipeProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuProcessingPipeProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuSskuWithPriorityProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.MskuSskuWithPriorityProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.processing.RecomputeMskuGoldServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CustomsCommCodeMarkupService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CustomsCommCodeMarkupServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MasterDataGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmLmsCargoTypeCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MskuGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.MultivalueBusinessHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.SskuGoldenMasterDataCalculationHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.SskuGoldenReferenceItemCalculationHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.TraceableSskuGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuCalculatingProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingContextProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingDataProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPipeProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPipeProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPostProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPreProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuProcessingPreProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuToRefreshProcessingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuToRefreshProcessingServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuVerdictProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuVerdictProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.MasterDataValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.MasterDataVersionMapService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictCalculationHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.priceinfo.PriceInfoRepository;
import ru.yandex.market.mbo.mdm.common.service.AllOkMasterDataValidator;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.service.MdmParameterValueCachingServiceMock;
import ru.yandex.market.mbo.mdm.common.service.mapping.MdmBestMappingsProvider;
import ru.yandex.market.mbo.mdm.common.service.queue.ProcessMskuQueueService;
import ru.yandex.market.mbo.mdm.common.service.queue.ProcessSskuToRefreshQueueService;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmDbWithCleaningTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataValidator;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepositoryMock;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsService;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsServiceImpl;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffService;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.common.utils.TaskQueueRegistratorMock;

/**
 * @author dmserebr
 * @date 30/03/2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class RecomputeMskuGoldExecutorWithIrisDimensionsTest extends MdmDbWithCleaningTestClass {
    private static final ShopSkuKey SHOP_SKU_KEY_1 = new ShopSkuKey(10, "test1");
    private static final ShopSkuKey SHOP_SKU_KEY_2 = new ShopSkuKey(10, "test2");
    private static final ShopSkuKey SHOP_SKU_KEY_3 = new ShopSkuKey(11, "test3");
    private static final ShopSkuKey SHOP_SKU_KEY_4 = new ShopSkuKey(11, "test4");
    private static final ShopSkuKey SHOP_SKU_KEY_5 = new ShopSkuKey(12, "test5");
    private static final long MSKU_1 = 100L;
    private static final long MSKU_2 = 101L;
    private static final int CATEGORY_ID = 1;
    private static final Instant LONG_AGO = TimestampUtil.toInstant(LocalDateTime.of(2000, 1, 1, 0, 0, 0));

    private static final ShopSkuKey SHOP_SKU_KEY_WT = new ShopSkuKey(13, "test6");
    private static final ShopSkuKey SHOP_SKU_KEY_UNK = new ShopSkuKey(14, "test7");

    private static long updateStampCounter;

    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private FromIrisItemRepository fromIrisItemRepository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MskuToRefreshRepository mskuToRefreshRepository;
    @Autowired
    private SskuToRefreshRepository sskuToRefreshRepository;
    @Autowired
    private MdmQueuesManager mdmQueuesManager;
    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private MdmLmsCargoTypeCache mdmLmsCargoTypeCache;
    @Autowired
    private VerdictCalculationHelper verdictCalculationHelper;
    @Autowired
    protected ServiceSskuConverter serviceSskuConverter;
    @Autowired
    protected MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    protected MasterDataBusinessMergeService masterDataBusinessMergeService;
    @Autowired
    private MasterDataVersionMapService masterDataVersionMapService;
    @Autowired
    private SskuGoldenParamUtil sskuGoldenParamUtil;
    @Autowired
    private GoldSskuRepository goldSskuRepository;
    @Autowired
    private SupplierDqScoreRepository supplierDqScoreRepository;
    @Autowired
    private StorageKeyValueService keyValueService;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private FeatureSwitchingAssistant featureSwitchingAssistant;
    @Autowired
    private CustomsCommCodeRepository codeRepository;
    @Autowired
    private MdmGoodGroupRepository mdmGoodGroupRepository;
    @Autowired
    private SskuProcessingDataProvider sskuProcessingDataProvider;
    @Autowired
    private PriceInfoRepository priceInfoRepository;
    @Autowired
    private RslGoldenItemService rslGoldenItemService;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private MasterDataGoldenItemService masterDataGoldenItemService;
    @Autowired
    private MultivalueBusinessHelper multivalueBusinessHelper;
    @Autowired
    private TraceableSskuGoldenItemService traceableSskuGoldenItemService;
    @Autowired
    private MdmBestMappingsProvider mdmBestMappingsProvider;
    @Autowired
    private WeightDimensionsValidator weightDimensionsValidator;

    private RecomputeMskuGoldServiceImpl recomputeMskuGoldService;
    private ProcessSskuToRefreshQueueService processSskuToRefreshQueueService;
    private MdmParameterValueCachingServiceMock parameterValueCachingServiceMock;
    private WeightDimensionsGoldenItemService weightDimensionsService;
    private MskuGoldenItemService mskuGIS;
    private ProcessMskuQueueService processMskuQueueService;

    private static MasterData createTestMasterData(ShopSkuKey shopSkuKey) {
        MasterData masterData = new MasterData();
        masterData.setShopSkuKey(shopSkuKey);
        masterData.setManufacturerCountries(List.of("Кирибати"));
        return masterData;
    }

    private static MappingCacheDao createTestMapping(ShopSkuKey shopSkuKey, long mskuId) {
        MappingCacheDao item = new MappingCacheDao();
        item.setSupplierId(shopSkuKey.getSupplierId());
        item.setShopSku(shopSkuKey.getShopSku());
        item.setMskuId(mskuId);
        item.setCategoryId(CATEGORY_ID);
        item.setUpdateStamp(updateStampCounter++);
        return item;
    }

    private static MappingCacheDao createTestMappingInCategory(ShopSkuKey shopSkuKey, long mskuId, int categoryId) {
        MappingCacheDao item = new MappingCacheDao();
        item.setSupplierId(shopSkuKey.getSupplierId());
        item.setShopSku(shopSkuKey.getShopSku());
        item.setMskuId(mskuId);
        item.setCategoryId(categoryId);
        item.setUpdateStamp(updateStampCounter++);
        return item;
    }

    @Before
    public void before() {
        CachedItemBlockValidationContextProvider validationContextProvider =
            new CachedItemBlockValidationContextProviderImpl(keyValueService);
        var itemBlockValidationService = new WeightDimensionBlockValidationServiceImpl(
            validationContextProvider, weightDimensionsValidator);
        MasterDataValidator masterDataValidator = new AllOkMasterDataValidator();
        weightDimensionsService = new WeightDimensionsGoldenItemService(
            new WeightDimensionsSilverItemSplitter(new SupplierConverterServiceMock()),
            itemBlockValidationService,
            featureSwitchingAssistant);
        var forceInheritanceService = new WeightDimensionForceInheritanceService(
            featureSwitchingAssistant,
            new WeightDimensionsForceInheritancePostProcessor(),
            itemBlockValidationService
        );

        // queue processor 1
        MskuGoldenSplitterMerger goldenSplitterMerger = new MskuGoldenSplitterMerger(mdmParamCache);
        MskuSilverSplitter mskuSilverSplitter = new MskuSilverSplitter(mdmParamCache, sskuGoldenParamUtil);
        MskuSilverItemPreProcessor mskuSilverItemPreProcessor = new MskuSilverItemPreProcessor(
            mdmParamCache, mdmLmsCargoTypeCache, featureSwitchingAssistant);
        CustomsCommCodeMarkupService markupService = new CustomsCommCodeMarkupServiceImpl(mdmParamCache, codeRepository,
            new CCCodeValidationService(List.of(), codeRepository), categoryParamValueRepository,
            new TaskQueueRegistratorMock(), mdmGoodGroupRepository, mappingsCacheRepository);
        MskuGoldenBlocksPostProcessor mskuGoldenBlocksPostProcessor =
            new MskuGoldenBlocksPostProcessor(featureSwitchingAssistant, mdmParamCache, markupService, keyValueService);
        mskuGIS = new MskuGoldenItemService(mskuSilverSplitter,
            goldenSplitterMerger, goldenSplitterMerger, mskuSilverItemPreProcessor, featureSwitchingAssistant,
            mskuGoldenBlocksPostProcessor, itemBlockValidationService, mdmParamCache);
        parameterValueCachingServiceMock = new MdmParameterValueCachingServiceMock();
        MdmCategorySettingsService settings = new MdmCategorySettingsServiceImpl(parameterValueCachingServiceMock,
            new CargoTypeRepositoryMock(), new CategoryParamValueRepositoryMock());

        var assistant = new MskuProcessingDataProviderImpl(
            mskuRepository,
            categoryParamValueRepository,
            settings,
            masterDataRepository,
            new GlobalParamValueService(mdmParamCache, keyValueService),
            goldSskuRepository,
            keyValueService,
            priceInfoRepository,
            Mockito.mock(WarehouseProjectionCacheImpl.class),
            mdmParamCache,
            mdmBestMappingsProvider
        );
        recomputeMskuGoldService = new RecomputeMskuGoldServiceImpl(
            assistant,
            mskuProcessingPipeProcessor(),
            mskuCalculatingProcessor(mskuGIS, masterDataValidator));

        processMskuQueueService = new ProcessMskuQueueService(mskuToRefreshRepository,
            keyValueService,
            recomputeMskuGoldService);

        // queue processor 2
        SskuGoldenReferenceItemCalculationHelper sskuGoldenReferenceItemCalculationHelper =
            new SskuGoldenReferenceItemCalculationHelper(
                weightDimensionsService,
                forceInheritanceService,
                new SurplusAndCisGoldenItemServiceMock(),
                keyValueService,
                serviceSskuConverter,
                masterDataBusinessMergeService,
                Mockito.mock(OfferCutoffService.class),
                sskuGoldenParamUtil,
                validationContextProvider,
                weightDimensionsValidator,
                rslGoldenItemService
            );
        SskuGoldenMasterDataCalculationHelper sskuGoldenMasterDataCalculationHelper =
            new SskuGoldenMasterDataCalculationHelper(
                serviceSskuConverter,
                new MasterDataValidationService(masterDataValidator),
                masterDataBusinessMergeService,
                keyValueService,
                masterDataGoldenItemService,
                sskuGoldenParamUtil,
                traceableSskuGoldenItemService,
                multivalueBusinessHelper
            );

        SskuToRefreshProcessingService sskuToRefreshProcessingService = new SskuToRefreshProcessingServiceImpl(
            sskuProcessingDataProvider,
            sskuVerdictProcessor(),
            sskuProcessingPostProcessor(sskuGoldenReferenceItemCalculationHelper),
            sskuProcessingContextProvider(),
            sskuProcessingPipeProcessor(),
            sskuProcessingPreProcessor(),
            sskuCalculatingProcessor(sskuGoldenReferenceItemCalculationHelper, sskuGoldenMasterDataCalculationHelper));

        processSskuToRefreshQueueService = new ProcessSskuToRefreshQueueService(
            sskuToRefreshRepository,
            keyValueService,
            mdmSskuGroupManager,
            sskuToRefreshProcessingService,
            transactionTemplate
        );

        //suppliers
        prepareSuppliers(List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3, SHOP_SKU_KEY_4, SHOP_SKU_KEY_5),
            MdmSupplierType.THIRD_PARTY);
        prepareSuppliers(List.of(SHOP_SKU_KEY_WT), MdmSupplierType.MARKET_SHOP);
        keyValueService.putValue(MdmProperties.WRITE_GOLD_WD_TO_SSKU_GOLD_TABLE_GLOBALLY, true);
        keyValueService.putValue(MdmProperties.WRITE_OWN_SSKU_WD_GLOBALLY, true);
        keyValueService.putValue(MdmProperties.USE_OWN_SSKU_WD_FOR_MSKU_GOLD_GLOBALLY, true);
        keyValueService.invalidateCache();
    }


    private MskuProcessingPipeProcessor mskuProcessingPipeProcessor() {
        return new MskuProcessingPipeProcessorImpl(mdmQueuesManager, mskuSskuWithPriorityProvider());
    }

    private MskuSskuWithPriorityProvider mskuSskuWithPriorityProvider() {
        return new MskuSskuWithPriorityProviderImpl(mdmSskuGroupManager);
    }

    private MskuCalculatingProcessor mskuCalculatingProcessor(MskuGoldenItemService mskuGoldenItemService,
                                                              MasterDataValidator masterDataValidator) {
        return new MskuCalculatingProcessorImpl(mskuRepository, mskuGoldenItemService, masterDataValidator);
    }


    private SskuCalculatingProcessorImpl sskuCalculatingProcessor(SskuGoldenReferenceItemCalculationHelper
                                                                      sskuGoldenReferenceItemCalculationHelper,
                                                                  SskuGoldenMasterDataCalculationHelper
                                                                      sskuGoldenMasterDataCalculationHelper) {
        return new SskuCalculatingProcessorImpl(
            sskuGoldenReferenceItemCalculationHelper,
            sskuGoldenMasterDataCalculationHelper,
            referenceItemRepository,
            masterDataRepository,
            goldSskuRepository
        );
    }

    private SskuProcessingContextProvider sskuProcessingContextProvider() {
        return new SskuProcessingContextProviderImpl(keyValueService);
    }

    private SskuProcessingPipeProcessor sskuProcessingPipeProcessor() {
        return new SskuProcessingPipeProcessorImpl(mdmQueuesManager, serviceSskuConverter);
    }

    private SskuProcessingPostProcessor sskuProcessingPostProcessor(SskuGoldenReferenceItemCalculationHelper
                                                                        sskuGoldenReferenceItemCalculationHelper) {
        return new SskuProcessingPostProcessorImpl(sskuGoldenReferenceItemCalculationHelper);
    }

    private SskuProcessingPreProcessor sskuProcessingPreProcessor() {
        return new SskuProcessingPreProcessorImpl(mdmSskuGroupManager);
    }

    private SskuVerdictProcessor sskuVerdictProcessor() {
        return new SskuVerdictProcessorImpl(
            serviceSskuConverter,
            masterDataBusinessMergeService,
            masterDataVersionMapService,
            verdictCalculationHelper);
    }

    @Test
    public void testIrisItemsAreRaisedToMsku() {
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_2, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_3, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_4, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_WT, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_UNK, MSKU_1));

        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_1));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_2));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_3));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_4));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_WT));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_UNK));

        Assertions.assertThat(mskuRepository.findAllMskus()).isEmpty();

        FromIrisItemWrapper ssku1Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null, 100L)));
        FromIrisItemWrapper ssku2Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_2, MdmIrisPayload.MasterDataSource.SUPPLIER,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 25.0, 1.0, 0.8, null, 110L)));
        FromIrisItemWrapper ssku3Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_3, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 30.0, 1.0, null, null, 120L)));

        fromIrisItemRepository.insertOrUpdate(ssku1Item);
        fromIrisItemRepository.insertOrUpdate(ssku2Item);
        fromIrisItemRepository.insertOrUpdate(ssku3Item);

        FromIrisItemWrapper sskuWtItem = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_WT, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 40.0, 1.0,
                null, null, 130L)));
        FromIrisItemWrapper sskuUnkItem = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_UNK, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 50.0, 1.0,
                null, null, 140L)));
        fromIrisItemRepository.insertOrUpdate(sskuWtItem);
        fromIrisItemRepository.insertOrUpdate(sskuUnkItem);

        sskuToRefreshRepository.enqueueAll(
            List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3, SHOP_SKU_KEY_WT, SHOP_SKU_KEY_UNK),
            MdmEnqueueReason.CHANGED_IRIS_ITEM
        );

        processSskuToRefreshQueueAfterCommit();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        processMskuQueueService.processQueueItems();

        Optional<CommonMsku> commonMsku = mskuRepository.findMsku(MSKU_1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.LENGTH))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(10));

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WIDTH))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(15));

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.HEIGHT))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(30));

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_GROSS))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(1));

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_NET))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal("0.8"));
        });
    }

    private void processSskuToRefreshQueueAfterCommit() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
        processSskuToRefreshQueueService.processQueueItems();
    }

    @Test
    public void testIrisWeightDimensionsAreUpdatedInMskuIfAlreadyExistAndDifferent() {
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_2, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_3, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_4, MSKU_1));

        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_1));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_2));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_3));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_4));

        // existing values are with source SUPPLIER
        CommonMsku existingMsku = new CommonMsku(
            new ModelKey(CATEGORY_ID, MSKU_1),
            List.of(
                createNumericMskuParamValue(KnownMdmParams.LENGTH, MSKU_1, 10.0),
                createNumericMskuParamValue(KnownMdmParams.WIDTH, MSKU_1, 14.0),
                createNumericMskuParamValue(KnownMdmParams.HEIGHT, MSKU_1, 30.0),
                createNumericMskuParamValue(KnownMdmParams.WEIGHT_GROSS, MSKU_1, 0.9),
                createNumericMskuParamValue(KnownMdmParams.WEIGHT_TARE, MSKU_1, 0.2),
                (MskuParamValue) createNumericMskuParamValue(KnownMdmParams.WEIGHT_NET, MSKU_1, 0.8)
                    .setMasterDataSourceId("supplier_id:10 shop_sku:test2 supplier:test-supplier")
            )
        );
        mskuRepository.insertOrUpdateMsku(existingMsku);

        Instant oldness = Instant.ofEpochMilli(99L);

        FromIrisItemWrapper ssku1Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null, 100L)));
        FromIrisItemWrapper ssku2Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_2, MdmIrisPayload.MasterDataSource.SUPPLIER, "test-supplier",
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 25.0, 1.0, 0.8, null, 110L)));
        FromIrisItemWrapper ssku3Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_3, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 30.0, 1.0, null, null, 120L)));

        fromIrisItemRepository.insertOrUpdate(ssku1Item);
        fromIrisItemRepository.insertOrUpdate(ssku2Item);
        fromIrisItemRepository.insertOrUpdate(ssku3Item);

        sskuToRefreshRepository.enqueueAll(
            List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3),
            MdmEnqueueReason.CHANGED_IRIS_ITEM
        );
        processSskuToRefreshQueueAfterCommit();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();


        processMskuQueueService.processQueueItems();

        Optional<CommonMsku> updatedMsku = mskuRepository.findMsku(MSKU_1);
        // values that are different from prev version should have their timestamp updated
        SoftAssertions.assertSoftly(softly -> {
            // value the same as previous but source is updated -> timestamp is changed
            softly.assertThat(updatedMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.LENGTH))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(10));
            softly.assertThat(updatedMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.LENGTH))
                .map(MdmParamValue::getModificationInfo)
                .map(MdmModificationInfo::getUpdatedTs)
                .hasValueSatisfying(instant -> Assertions.assertThat(instant).isAfter(oldness));

            // value not the same as previous -> timestamp is changed
            softly.assertThat(updatedMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WIDTH))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(15));
            softly.assertThat(updatedMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WIDTH))
                .map(MdmParamValue::getModificationInfo)
                .map(MdmModificationInfo::getUpdatedTs)
                .hasValueSatisfying(instant -> Assertions.assertThat(instant).isAfter(oldness));

            // value the same as previous but source is updated -> timestamp is changed
            softly.assertThat(updatedMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.HEIGHT))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(30));
            softly.assertThat(updatedMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.HEIGHT))
                .map(MdmParamValue::getModificationInfo)
                .map(MdmModificationInfo::getUpdatedTs)
                .hasValueSatisfying(instant -> Assertions.assertThat(instant).isAfter(oldness));

            // value the same as previous but source is updated -> timestamp is changed
            softly.assertThat(updatedMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_GROSS))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(1));
            softly.assertThat(updatedMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_GROSS))
                .map(MdmParamValue::getModificationInfo)
                .map(MdmModificationInfo::getUpdatedTs)
                .hasValueSatisfying(instant -> Assertions.assertThat(instant).isAfter(oldness));

            // value the same as previous and source is not updated -> timestamp is not changed
            softly.assertThat(updatedMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_NET))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal("0.8"));
            softly.assertThat(updatedMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_NET))
                .map(MdmParamValue::getModificationInfo)
                .map(MdmModificationInfo::getUpdatedTs)
                .hasValueSatisfying(instant -> Assertions.assertThat(instant).isAfter(oldness));

            // inactual value is deleted
            softly.assertThat(updatedMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_TARE))
                .isEmpty();
        });
    }

    @Test
    public void testOnlySupplierWeightDimensions() {
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_1));

        FromIrisItemWrapper sskuItem = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.SUPPLIER,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 27.0, 30.0, 1.0, 0.8, null, 140L)));
        fromIrisItemRepository.insertOrUpdate(sskuItem);

        sskuToRefreshRepository.enqueue(SHOP_SKU_KEY_1, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        processSskuToRefreshQueueAfterCommit();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();


        processMskuQueueService.processQueueItems();

        Optional<CommonMsku> commonMsku = mskuRepository.findMsku(MSKU_1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.LENGTH))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(10));

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WIDTH))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(27));

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.HEIGHT))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(30));

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_GROSS))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(1));

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_NET))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal("0.8"));

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_TARE))
                .isEmpty();
        });
    }

    @Test
    public void testIncompleteSupplierWeightDimensions() {
        // Dimensions without weight; it should not normally happen but there are such items in DB
        // These items should be filtered out by the validator
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_1));

        ReferenceItemWrapper sskuItem = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.SUPPLIER,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 27.0, 30.0, null, null, null, 140L)));
        referenceItemRepository.insertOrUpdate(sskuItem);

        mskuToRefreshRepository.enqueue(MSKU_1, MdmEnqueueReason.CHANGED_IRIS_ITEM);
        processMskuQueueService.processQueueItems();

        Optional<CommonMsku> commonMsku = mskuRepository.findMsku(MSKU_1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.LENGTH))
                .isEmpty();

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WIDTH))
                .isEmpty();

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.HEIGHT))
                .isEmpty();

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_GROSS))
                .isEmpty();

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_NET))
                .isEmpty();

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_TARE))
                .isEmpty();
        });
    }

    @Test
    public void testPriorityForSupplierAndWarehouseWeightDimensions() {
        weightDimensionsService.setReplaceWarehouseWithSupplierFromDefaultTs(
            TimestampUtil.toInstant(LocalDateTime.of(3000, 1, 1, 0, 0)));
        mskuGIS.setReplaceWarehouseWithSupplierFromDefaultTs(
            TimestampUtil.toInstant(LocalDateTime.of(3000, 1, 1, 0, 0)));

        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_2, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_3, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_4, MSKU_1));

        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_1));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_2));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_3));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_4));

        FromIrisItemWrapper ssku1Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145",
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, 0.8, null, 100L)));
        FromIrisItemWrapper ssku2Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "147",
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 25.0, 1.0, 0.81, null, 110L)));
        FromIrisItemWrapper ssku3Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145",
            ItemWrapperTestUtil.generateShippingUnit(10.0, 20.0, 30.0, 1.0, null, null, 120L)));
        FromIrisItemWrapper ssku4Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "147",
            ItemWrapperTestUtil.generateShippingUnit(10.0, 21.0, 30.0, 1.0, null, null, 130L)));
        FromIrisItemWrapper ssku5Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_3, MdmIrisPayload.MasterDataSource.SUPPLIER,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 27.0, 30.0, 1.0, null, null, 140L)));

        fromIrisItemRepository.insertOrUpdate(ssku1Item);
        fromIrisItemRepository.insertOrUpdate(ssku2Item);
        fromIrisItemRepository.insertOrUpdate(ssku3Item);
        fromIrisItemRepository.insertOrUpdate(ssku4Item);
        fromIrisItemRepository.insertOrUpdate(ssku5Item);

        sskuToRefreshRepository.enqueueAll(
            List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3),
            MdmEnqueueReason.CHANGED_IRIS_ITEM
        );
        processSskuToRefreshQueueAfterCommit();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();


        processMskuQueueService.processQueueItems();

        Optional<CommonMsku> commonMsku = mskuRepository.findMsku(MSKU_1);

        // should ignore the supplier shipping unit from SHOP_SKU_KEY_3
        // (despite it is the latest and more than 10% different)
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.LENGTH))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(10));

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WIDTH))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(20));

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.HEIGHT))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(30));

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_GROSS))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(1));

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_NET))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal("0.8"));

            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_TARE))
                .isEmpty();
        });
    }

    @Test
    @Ignore("MARKETMDM-1426")
    public void testDoubleInheritedDimensionsAreIgnoredInBothDirections() {
        // Value inherited from MSKU to SSKU should not be raised MSKU back
        // Value raised from SSKU to MSKU should not strike the same SSKU back (but can affect other SSKUs)

        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_2, MSKU_1));

        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_1));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_2));

        FromIrisItemWrapper ssku1Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145",
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, 0.8, null, 100L)));
        fromIrisItemRepository.insertOrUpdate(ssku1Item);

        sskuToRefreshRepository.enqueueAll(List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2), MdmEnqueueReason.CHANGED_IRIS_ITEM);
        processSskuToRefreshQueueAfterCommit();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();


        processMskuQueueService.processQueueItems();

        Set<SskuToRefreshInfo> activeQueueItems = sskuToRefreshRepository.findAll().stream()
            .filter(i -> !i.isProcessed())
            .collect(Collectors.toSet());
        Assertions.assertThat(activeQueueItems)
            .allSatisfy(info ->
                Assertions.assertThat(info.getOnlyReasons()).containsExactly(MdmEnqueueReason.CHANGED_MSKU_DATA))
            .map(MdmQueueInfoBase::getEntityKey)
            .containsExactlyInAnyOrder(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2);

        // remove weight net from ssku 1
        FromIrisItemWrapper ssku1ItemNew = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145",
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null, 200L)));
        fromIrisItemRepository.insertOrUpdate(ssku1ItemNew);

        processSskuToRefreshQueueAfterCommit();
        processMskuQueueService.processQueueItems();

        Optional<CommonMsku> commonMsku = mskuRepository.findMsku(MSKU_1);
        ReferenceItemWrapper refItem1 = referenceItemRepository.findById(SHOP_SKU_KEY_1);
        ReferenceItemWrapper refItem2 = referenceItemRepository.findById(SHOP_SKU_KEY_2);

        // weight net should disappear from MSKU despite it is still on ssku 2 (due to inheritance)
        // it means that inherited values are ignored in msku
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.LENGTH))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(10));
            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WIDTH))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(15));
            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.HEIGHT))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(20));
            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_GROSS))
                .flatMap(MdmParamValue::getNumeric)
                .contains(new BigDecimal(1));
            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_NET))
                .isEmpty();
            softly.assertThat(commonMsku)
                .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_TARE))
                .isEmpty();

            // ssku1 has new value without weight net
            softly.assertThat(refItem1.getItem().getInformationList()).hasSize(1);
            softly.assertThat(refItem1.getItem().getInformation(0).getSource().getId()).isEqualTo("145");
            softly.assertThat(refItem1.getItem().getInformation(0).getSource().getType())
                .isEqualTo(MdmIrisPayload.MasterDataSource.WAREHOUSE);
            softly.assertThat(clearUpdatedTs(refItem1.getItem().getInformation(0).getItemShippingUnit()))
                .isEqualTo(clearUpdatedTs(
                    ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null, 200L).build()));

            // ssku2 still has inherited value (with weight net and old ts)
            softly.assertThat(refItem2.getItem().getInformationList()).hasSize(1);
            softly.assertThat(refItem2.getItem().getInformation(0).getSource().getId())
                .isEqualTo("msku:" + MSKU_1 + " supplier_id:" +
                    SHOP_SKU_KEY_1.getSupplierId() + " shop_sku:" + SHOP_SKU_KEY_1.getShopSku() + " warehouse:145");
            softly.assertThat(refItem2.getItem().getInformation(0).getSource().getType())
                .isEqualTo(MdmIrisPayload.MasterDataSource.MDM);
            softly.assertThat(clearUpdatedTs(refItem2.getItem().getInformation(0).getItemShippingUnit()))
                .isEqualTo(clearUpdatedTs(
                    ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, 0.8, null, 100L).build()));
        });

        processSskuToRefreshQueueAfterCommit();

        ReferenceItemWrapper refItem2new = referenceItemRepository.findById(SHOP_SKU_KEY_2);
        SoftAssertions.assertSoftly(softly -> {
            // ssku2 has received new value without weight net via inheritance
            softly.assertThat(refItem2new.getItem().getInformationList()).hasSize(1);
            softly.assertThat(refItem2new.getItem().getInformation(0).getSource().getId())
                .isEqualTo("msku:" + MSKU_1 + " supplier_id:" +
                    SHOP_SKU_KEY_1.getSupplierId() + " shop_sku:" + SHOP_SKU_KEY_1.getShopSku() + " warehouse:145");
            softly.assertThat(refItem2new.getItem().getInformation(0).getSource().getType())
                .isEqualTo(MdmIrisPayload.MasterDataSource.MDM);
            softly.assertThat(clearUpdatedTs(refItem2new.getItem().getInformation(0).getItemShippingUnit()))
                .isEqualTo(clearUpdatedTs(
                    ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null, 200L).build()));
        });
    }

    @Test
    public void testComputeHeavyGood() {
        keyValueService.putValue(MdmProperties.COMPUTE_HEAVY_GOOD_IN_MSKU_GOLDEN_SERVICE_KEY, true);
        parameterValueCachingServiceMock.addCategoryParameterValues(1L, List.of(
            MboParameters.ParameterValue.newBuilder()
                .setParamId(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID).setBoolValue(true).build()));

        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_2, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_3, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_4, MSKU_2));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_5, MSKU_2));

        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_1));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_2));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_3));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_4));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_5));

        FromIrisItemWrapper ssku1Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null, 100L)));
        FromIrisItemWrapper ssku2Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_2, MdmIrisPayload.MasterDataSource.SUPPLIER, "",
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 25.0, 1.0, 0.8, null, 110L)));
        FromIrisItemWrapper ssku3Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_3, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 30.0, 50.0, null, null, 120L)));
        FromIrisItemWrapper ssku4Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_4, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null, 100L)));
        FromIrisItemWrapper ssku5Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_5, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 40.0, null, null, 100L)));

        fromIrisItemRepository.insertOrUpdate(ssku1Item);
        fromIrisItemRepository.insertOrUpdate(ssku2Item);
        fromIrisItemRepository.insertOrUpdate(ssku3Item);
        fromIrisItemRepository.insertOrUpdate(ssku4Item);
        fromIrisItemRepository.insertOrUpdate(ssku5Item);

        sskuToRefreshRepository.enqueueAll(
            List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3, SHOP_SKU_KEY_4, SHOP_SKU_KEY_5),
            MdmEnqueueReason.CHANGED_IRIS_ITEM
        );
        processSskuToRefreshQueueAfterCommit();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        processMskuQueueService.processQueueItems();

        // not heavy - because SSKU1 and SSKU2 are not heavy, and only SSKU3 is heavy
        Assertions.assertThat(mskuRepository.findMsku(MSKU_1))
            .flatMap(msku -> msku.getParamValue(KnownMdmParams.HEAVY_GOOD))
            .flatMap(MdmParamValue::getBool)
            .contains(false);

        // heavy - because SSKU4 is not heavy and SSKU5 is heavy. No quorum -> take category default (heavy)
        Assertions.assertThat(mskuRepository.findMsku(MSKU_2))
            .flatMap(msku -> msku.getParamValue(KnownMdmParams.HEAVY_GOOD))
            .flatMap(MdmParamValue::getBool)
            .contains(true);
    }

    @Test
    public void testComputeHeavyGoodWithExistingGold() {
        keyValueService.putValue(MdmProperties.COMPUTE_HEAVY_GOOD_IN_MSKU_GOLDEN_SERVICE_KEY, true);
        parameterValueCachingServiceMock.addCategoryParameterValues(1L, List.of(
            MboParameters.ParameterValue.newBuilder()
                .setParamId(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID).setBoolValue(true).build()));

        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_2, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_3, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_4, MSKU_2));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_5, MSKU_2));

        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_1));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_2));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_3));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_4));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_5));

        FromIrisItemWrapper ssku1Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null, 100L)));
        FromIrisItemWrapper ssku2Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_2, MdmIrisPayload.MasterDataSource.SUPPLIER, "",
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 25.0, 1.0, 0.8, null, 110L)));
        FromIrisItemWrapper ssku3Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_3, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 30.0, 50.0, null, null, 120L)));
        FromIrisItemWrapper ssku4Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_4, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null, 100L)));
        FromIrisItemWrapper ssku5Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_5, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 40.0, null, null, 100L)));

        fromIrisItemRepository.insertOrUpdate(ssku1Item);
        fromIrisItemRepository.insertOrUpdate(ssku2Item);
        fromIrisItemRepository.insertOrUpdate(ssku3Item);
        fromIrisItemRepository.insertOrUpdate(ssku4Item);
        fromIrisItemRepository.insertOrUpdate(ssku5Item);

        // old msku gold
        var oldGold = new CommonMsku(CATEGORY_ID, MSKU_1);
        oldGold.addParamValue((MskuParamValue) new MskuParamValue().setMskuId(MSKU_1)
            .setBool(true)
            .setMdmParamId(KnownMdmParams.HEAVY_GOOD)
            .setXslName("cargoType300")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
        );
        mskuRepository.insertOrUpdateMsku(oldGold);

        sskuToRefreshRepository.enqueueAll(
            List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3, SHOP_SKU_KEY_4, SHOP_SKU_KEY_5),
            MdmEnqueueReason.CHANGED_IRIS_ITEM
        );
        processSskuToRefreshQueueAfterCommit();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        processMskuQueueService.processQueueItems();

        // heavy - because there is operator value, everything else is ignored
        Optional<CommonMsku> commonMsku1 = mskuRepository.findMsku(MSKU_1);
        Assertions.assertThat(commonMsku1)
            .flatMap(msku -> msku.getParamValue(KnownMdmParams.HEAVY_GOOD))
            .flatMap(MdmParamValue::getBool)
            .contains(true);

        // heavy - because SSKU4 is not heavy and SSKU5 is heavy. No quorum -> take category default (heavy)
        Optional<CommonMsku> commonMsku2 = mskuRepository.findMsku(MSKU_2);
        Assertions.assertThat(commonMsku2)
            .flatMap(msku -> msku.getParamValue(KnownMdmParams.HEAVY_GOOD))
            .flatMap(MdmParamValue::getBool)
            .contains(true);
    }

    //shouldn't fall with existing WeightNet and WeightTare
    @Test
    public void testComputeHeavyGoodWithExistingWeightNetAndWeightTare() {
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_2, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_3, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_4, MSKU_2));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_5, MSKU_2));

        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_1));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_2));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_3));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_4));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_5));

        FromIrisItemWrapper ssku1Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, 0.9, 0.1, 100L)));
        FromIrisItemWrapper ssku2Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_2, MdmIrisPayload.MasterDataSource.SUPPLIER, "",
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 25.0, 1.0, 0.8, 0.2, 110L)));
        FromIrisItemWrapper ssku3Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_3, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 30.0, 50.0, 40., 10., 120L)));
        FromIrisItemWrapper ssku4Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_4, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, 0.35, 0.65, 100L)));
        FromIrisItemWrapper ssku5Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_5, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, 0.36, 0.64, 105L)));

        fromIrisItemRepository.insertOrUpdate(ssku1Item);
        fromIrisItemRepository.insertOrUpdate(ssku2Item);
        fromIrisItemRepository.insertOrUpdate(ssku3Item);
        fromIrisItemRepository.insertOrUpdate(ssku4Item);
        fromIrisItemRepository.insertOrUpdate(ssku5Item);

        MskuParamValue oldGoldWeightNet = new MskuParamValue().setMskuId(MSKU_1);
        oldGoldWeightNet.setNumeric(new BigDecimal("0.88"))
            .setMdmParamId(KnownMdmParams.WEIGHT_NET)
            .setXslName("-")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        MskuParamValue oldGoldWeightTare = new MskuParamValue().setMskuId(MSKU_1);
        oldGoldWeightTare.setNumeric(new BigDecimal("0.12"))
            .setMdmParamId(KnownMdmParams.WEIGHT_TARE)
            .setXslName("-")
            .setUpdatedTs(Instant.now())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);

        mskuRepository.insertOrUpdateMsku(new CommonMsku(MSKU_1, List.of(oldGoldWeightNet, oldGoldWeightTare)));

        sskuToRefreshRepository.enqueueAll(
            List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3, SHOP_SKU_KEY_4, SHOP_SKU_KEY_5),
            MdmEnqueueReason.CHANGED_IRIS_ITEM
        );
        processSskuToRefreshQueueAfterCommit();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        processMskuQueueService.processQueueItems();

        // Take latest with 10% logic, because all have same sourceType
        // and we don't use operator changes for this computations
        Optional<CommonMsku> commonMsku1 = mskuRepository.findMsku(MSKU_1);
        Assertions.assertThat(commonMsku1)
            .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_NET))
            .flatMap(MdmParamValue::getNumeric)
            .contains(BigDecimal.valueOf(40));
        Assertions.assertThat(commonMsku1)
            .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_TARE))
            .flatMap(MdmParamValue::getNumeric)
            .contains(BigDecimal.valueOf(10));

        Optional<CommonMsku> commonMsku2 = mskuRepository.findMsku(MSKU_2);
        Assertions.assertThat(commonMsku2)
            .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_NET))
            .flatMap(MdmParamValue::getNumeric)
            .contains(BigDecimal.valueOf(0.35));
        Assertions.assertThat(commonMsku2)
            .flatMap(msku -> msku.getParamValue(KnownMdmParams.WEIGHT_TARE))
            .flatMap(MdmParamValue::getNumeric)
            .contains(BigDecimal.valueOf(0.65));
    }

    @SuppressWarnings("checkstyle:MethodLength")
    @Test
    public void testForceInheritanceWorks() {
        weightDimensionsService.setReplaceWarehouseWithSupplierFromDefaultTs(
            TimestampUtil.toInstant(LocalDateTime.of(3000, 1, 1, 0, 0)));

        raiseForceInheritanceSwitches(CATEGORY_ID);

        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_2, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_3, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_4, MSKU_1));

        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_1));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_2));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_3));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_4));

        // stage 1: force inheritance for new items
        var shippingUnitSsku1 = ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, 0.8, null, 100L);
        FromIrisItemWrapper ssku1Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145", shippingUnitSsku1));

        var shippingUnitSsku2 = ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 25.0, 1.0, 0.81, null, 110L);
        FromIrisItemWrapper ssku2Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "147", shippingUnitSsku2));

        var shippingUnitSsku3 = ItemWrapperTestUtil.generateShippingUnit(10.0, 20.0, 30.0, 1.0, null, null, 120L);
        FromIrisItemWrapper ssku3Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145", shippingUnitSsku3));

        var shippingUnitSsku4 = ItemWrapperTestUtil.generateShippingUnit(10.0, 21.0, 30.0, 1.0, null, null, 130L);
        FromIrisItemWrapper ssku4Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "147", shippingUnitSsku4));

        var shippingUnitSsku5 = ItemWrapperTestUtil.generateShippingUnit(10.0, 27.0, 30.0, 1.0, null, null, 140L);
        FromIrisItemWrapper ssku5Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_3, MdmIrisPayload.MasterDataSource.SUPPLIER, shippingUnitSsku5));

        fromIrisItemRepository.insertOrUpdateAll(List.of(ssku1Item, ssku2Item, ssku3Item, ssku4Item, ssku5Item));

        sskuToRefreshRepository.enqueueAll(
            List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3),
            MdmEnqueueReason.CHANGED_IRIS_ITEM
        );
        processSskuToRefreshQueueAfterCommit();
        Assertions.assertThat(sskuToRefreshRepository.getUnprocessedItemsCount()).isZero();

        processMskuQueueService.processQueueItems();
        processSskuToRefreshQueueAfterCommit();

        List<ReferenceItemWrapper> finalReferenceItems = referenceItemRepository.findAll();

        Map<ShopSkuKey, List<MdmIrisPayload.ReferenceInformation>> informationBySsku = finalReferenceItems.stream()
            .collect(Collectors.toMap(ReferenceItemWrapper::getKey, item -> item.getItem().getInformationList()));
        Assertions.assertThat(informationBySsku.keySet()).containsExactlyInAnyOrder(
            SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3, SHOP_SKU_KEY_4);

        // shippingUnitSsku3 is inherited to all items; weight net is taken from shippingUnitSsku1
        var expectedWeightNetShippingUnit = shippingUnitSsku1
            .clearLengthMicrometer().clearWidthMicrometer().clearHeightMicrometer().clearWeightGrossMg().build();

        // SHOP_SKU_KEY_1 receives dimensions from SHOP_SKU_KEY_2 and weight net from itself
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1)).hasSize(2);
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:10 shop_sku:test2 warehouse:145").build());
        Assertions.assertThat(clearUpdatedTs(informationBySsku.get(SHOP_SKU_KEY_1).get(0).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(shippingUnitSsku3.build()));
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1).get(1).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.WAREHOUSE)
                .setId("145").build());
        Assertions.assertThat(clearUpdatedTs(informationBySsku.get(SHOP_SKU_KEY_1).get(1).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(expectedWeightNetShippingUnit));

        // SHOP_SKU_KEY_2 receives dimensions from itself and weight net from SHOP_SKU_KEY_1
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_2)).hasSize(2);
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_2).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.WAREHOUSE)
                .setId("145").build());
        Assertions.assertThat(clearUpdatedTs(informationBySsku.get(SHOP_SKU_KEY_2).get(0).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(shippingUnitSsku3.build()));
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_2).get(1).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:10 shop_sku:test1 warehouse:145").build());
        Assertions.assertThat(clearUpdatedTs(informationBySsku.get(SHOP_SKU_KEY_2).get(1).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(expectedWeightNetShippingUnit));

        // SHOP_SKU_KEY_3 receives dimensions from SHOP_SKU_KEY_2 and weight net from SHOP_SKU_KEY_1
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_3)).hasSize(2);
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_3).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:10 shop_sku:test2 warehouse:145").build());
        Assertions.assertThat(clearUpdatedTs(informationBySsku.get(SHOP_SKU_KEY_3).get(0).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(shippingUnitSsku3.build()));
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_3).get(1).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:10 shop_sku:test1 warehouse:145").build());
        Assertions.assertThat(clearUpdatedTs(informationBySsku.get(SHOP_SKU_KEY_3).get(1).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(expectedWeightNetShippingUnit));

        // SHOP_SKU_KEY_4 receives dimensions from SHOP_SKU_KEY_2 and weight net from SHOP_SKU_KEY_1
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_4)).hasSize(2);
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_4).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:10 shop_sku:test2 warehouse:145").build());
        Assertions.assertThat(clearUpdatedTs(informationBySsku.get(SHOP_SKU_KEY_4).get(0).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(shippingUnitSsku3.build()));
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_4).get(1).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:10 shop_sku:test1 warehouse:145").build());
        Assertions.assertThat(clearUpdatedTs(informationBySsku.get(SHOP_SKU_KEY_4).get(1).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(expectedWeightNetShippingUnit));

        // ---------------
        // stage 2: change something and check that all SSKUs are updated
        var newShippingUnitSsku1 = ItemWrapperTestUtil.generateShippingUnit(20.0, 30.0, 40.0, 2.0, 0.8, null, 200L);
        FromIrisItemWrapper newSsku1Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "172", newShippingUnitSsku1));
        fromIrisItemRepository.insertOrUpdate(newSsku1Item);

        sskuToRefreshRepository.enqueue(SHOP_SKU_KEY_1, MdmEnqueueReason.CHANGED_IRIS_ITEM);

        processSskuToRefreshQueueAfterCommit();

        processMskuQueueService.processQueueItems();

        processSskuToRefreshQueueAfterCommit();

        List<ReferenceItemWrapper> newFinalReferenceItems = referenceItemRepository.findAll();

        Map<ShopSkuKey, List<MdmIrisPayload.ReferenceInformation>> newInformationBySsku =
            newFinalReferenceItems.stream()
                .collect(Collectors.toMap(ReferenceItemWrapper::getKey, item -> item.getItem().getInformationList()));
        Assertions.assertThat(newInformationBySsku.keySet()).containsExactlyInAnyOrder(
            SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3, SHOP_SKU_KEY_4);

        // SHOP_SKU_KEY_1 receives new dimensions and old weight net from itself
        Assertions.assertThat(newInformationBySsku.get(SHOP_SKU_KEY_1)).hasSize(2);
        Assertions.assertThat(newInformationBySsku.get(SHOP_SKU_KEY_1).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.WAREHOUSE)
                .setId("172").build());
        Assertions.assertThat(clearUpdatedTs(newInformationBySsku.get(SHOP_SKU_KEY_1).get(0).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(newShippingUnitSsku1.clearWeightNetMg().build()));
        Assertions.assertThat(newInformationBySsku.get(SHOP_SKU_KEY_1).get(1).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.WAREHOUSE)
                .setId("145").build());
        Assertions.assertThat(clearUpdatedTs(newInformationBySsku.get(SHOP_SKU_KEY_1).get(1).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(expectedWeightNetShippingUnit));

        // SHOP_SKU_KEY_2 receives new dimensions and old weight net from SHOP_SKU_KEY_1
        Assertions.assertThat(newInformationBySsku.get(SHOP_SKU_KEY_2)).hasSize(2);
        Assertions.assertThat(newInformationBySsku.get(SHOP_SKU_KEY_2).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:10 shop_sku:test1 warehouse:172").build());
        Assertions.assertThat(clearUpdatedTs(newInformationBySsku.get(SHOP_SKU_KEY_2).get(0).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(newShippingUnitSsku1.clearWeightNetMg().build()));
        Assertions.assertThat(newInformationBySsku.get(SHOP_SKU_KEY_2).get(1).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:10 shop_sku:test1 warehouse:145").build());
        Assertions.assertThat(clearUpdatedTs(newInformationBySsku.get(SHOP_SKU_KEY_2).get(1).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(expectedWeightNetShippingUnit));

        // SHOP_SKU_KEY_3 receives new dimensions and old weight net from SHOP_SKU_KEY_1
        Assertions.assertThat(newInformationBySsku.get(SHOP_SKU_KEY_3)).hasSize(2);
        Assertions.assertThat(newInformationBySsku.get(SHOP_SKU_KEY_3).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:10 shop_sku:test1 warehouse:172").build());
        Assertions.assertThat(clearUpdatedTs(newInformationBySsku.get(SHOP_SKU_KEY_3).get(0).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(newShippingUnitSsku1.clearWeightNetMg().build()));
        Assertions.assertThat(newInformationBySsku.get(SHOP_SKU_KEY_3).get(1).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:10 shop_sku:test1 warehouse:145").build());
        Assertions.assertThat(clearUpdatedTs(newInformationBySsku.get(SHOP_SKU_KEY_3).get(1).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(expectedWeightNetShippingUnit));

        // SHOP_SKU_KEY_4 receives new dimensions and old weight net from SHOP_SKU_KEY_1
        Assertions.assertThat(newInformationBySsku.get(SHOP_SKU_KEY_4)).hasSize(2);
        Assertions.assertThat(newInformationBySsku.get(SHOP_SKU_KEY_4).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:10 shop_sku:test1 warehouse:172").build());
        Assertions.assertThat(clearUpdatedTs(newInformationBySsku.get(SHOP_SKU_KEY_4).get(0).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(newShippingUnitSsku1.clearWeightNetMg().build()));
        Assertions.assertThat(newInformationBySsku.get(SHOP_SKU_KEY_4).get(1).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:10 shop_sku:test1 warehouse:145").build());
        Assertions.assertThat(clearUpdatedTs(newInformationBySsku.get(SHOP_SKU_KEY_4).get(1).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(expectedWeightNetShippingUnit));

        // check ssku_golden_param_values
        Map<ShopSkuKey, List<SskuGoldenParamValue>> sskuGoldenParamValues =
            goldSskuRepository.findAllSskus().stream()
                .map(CommonSsku::getBaseValues)
                .flatMap(List::stream)
                .map(SskuGoldenParamValue::fromSskuParamValue)
                .collect(Collectors.groupingBy(SskuParamValue::getShopSkuKey));
        Assertions.assertThat(sskuGoldenParamValues).hasSize(4);
        // ssku 1 has all weight-dimensions params: 5 own (ssku) + 5 inherited
        Assertions.assertThat(sskuGoldenParamValues.get(SHOP_SKU_KEY_1).stream()
            .map(SskuParamValue::getMdmParamId).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder(400L, 401L, 402L, 403L, 404L, 418L, 419L, 420L, 421L, 422L);
        // ssku 2 has 4 own (ssku) weight-dimensions params + 5 inherited
        Assertions.assertThat(sskuGoldenParamValues.get(SHOP_SKU_KEY_2).stream()
            .map(SskuParamValue::getMdmParamId).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder(400L, 401L, 402L, 403L, 404L, 418L, 419L, 420L, 421L);
        // ssku 3 has 4 own (ssku) weight-dimensions params + 5 inherited
        Assertions.assertThat(sskuGoldenParamValues.get(SHOP_SKU_KEY_3).stream()
            .map(SskuParamValue::getMdmParamId).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder(400L, 401L, 402L, 403L, 404L, 418L, 419L, 420L, 421L);
        // ssku 4 has only inherited weight-dimensions params
        Assertions.assertThat(sskuGoldenParamValues.get(SHOP_SKU_KEY_4).stream()
            .map(SskuParamValue::getMdmParamId).collect(Collectors.toSet()))
            .containsExactlyInAnyOrder(400L, 401L, 402L, 403L, 404L);
    }

    @Test
    public void testForceInheritanceRetainsRsl() {
        weightDimensionsService.setReplaceWarehouseWithSupplierFromDefaultTs(
            TimestampUtil.toInstant(LocalDateTime.of(3000, 1, 1, 0, 0)));

        raiseForceInheritanceSwitches(CATEGORY_ID);

        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_2, MSKU_1));

        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_1));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_2));

        // stage 1: force inheritance for new items
        var shippingUnitSsku1 = ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null, 100L);
        FromIrisItemWrapper ssku1Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145", shippingUnitSsku1));

        var shippingUnitSsku3 = ItemWrapperTestUtil.generateShippingUnit(10.0, 20.0, 30.0, 1.0, null, null, 120L);
        FromIrisItemWrapper ssku3Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145", shippingUnitSsku3));

        fromIrisItemRepository.insertOrUpdateAll(List.of(ssku1Item, ssku3Item));

        ReferenceItemWrapper ssku1RefItem = ItemWrapperTestUtil.createReferenceRslItem(SHOP_SKU_KEY_1, 1, 2, 3, 4);
        ReferenceItemWrapper ssku2RefItem = ItemWrapperTestUtil.createReferenceRslItem(SHOP_SKU_KEY_2, 5, 6, 7, 8);

        referenceItemRepository.insertBatch(ssku1RefItem, ssku2RefItem);

        sskuToRefreshRepository.enqueueAll(List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2), MdmEnqueueReason.CHANGED_IRIS_ITEM);

        processSskuToRefreshQueueAfterCommit();

        processMskuQueueService.processQueueItems();

        processSskuToRefreshQueueAfterCommit();

        List<ReferenceItemWrapper> finalReferenceItems = referenceItemRepository.findAll();

        Map<ShopSkuKey, List<MdmIrisPayload.ReferenceInformation>> informationBySsku = finalReferenceItems.stream()
            .collect(Collectors.toMap(ReferenceItemWrapper::getKey, item -> item.getItem().getInformationList()));
        Assertions.assertThat(informationBySsku.keySet()).containsExactlyInAnyOrder(
            SHOP_SKU_KEY_1, SHOP_SKU_KEY_2);

        // SHOP_SKU_KEY_1 receives dimensions from SHOP_SKU_KEY_2 and RSL is retained
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1)).hasSize(2);
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:10 shop_sku:test2 warehouse:145").build());
        Assertions.assertThat(clearUpdatedTs(informationBySsku.get(SHOP_SKU_KEY_1).get(0).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(shippingUnitSsku3.build()));
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1)
            .get(1).getMinInboundLifetimeDay(0).getValue()).isEqualTo(1);

        // SHOP_SKU_KEY_2 receives dimensions from itself and RSL is retained
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_2)).hasSize(2);
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_2).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.WAREHOUSE)
                .setId("145").build());
        Assertions.assertThat(clearUpdatedTs(informationBySsku.get(SHOP_SKU_KEY_2).get(0).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(shippingUnitSsku3.build()));
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_2)
            .get(1).getMinInboundLifetimeDay(0).getValue()).isEqualTo(5);
    }

    @Test
    public void testForceInheritanceRetainsSurplus() {
        weightDimensionsService.setReplaceWarehouseWithSupplierFromDefaultTs(
            TimestampUtil.toInstant(LocalDateTime.of(3000, 1, 1, 0, 0)));

        raiseForceInheritanceSwitches(CATEGORY_ID);

        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_2, MSKU_1));

        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_1));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_2));

        // stage 1: force inheritance for new items
        var shippingUnitSsku1 = ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null, 100L);
        FromIrisItemWrapper ssku1Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145", shippingUnitSsku1));

        var shippingUnitSsku3 = ItemWrapperTestUtil.generateShippingUnit(10.0, 20.0, 30.0, 1.0, null, null, 120L);
        FromIrisItemWrapper ssku3Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145", shippingUnitSsku3));

        fromIrisItemRepository.insertOrUpdateAll(List.of(ssku1Item, ssku3Item));

        ReferenceItemWrapper ssku1SurplusItem = ItemWrapperTestUtil.createSurplusCisReferenceItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.SurplusHandleMode.REJECT, null);
        ReferenceItemWrapper ssku2SurplusItem = ItemWrapperTestUtil.createSurplusCisReferenceItem(
            SHOP_SKU_KEY_2, MdmIrisPayload.SurplusHandleMode.ACCEPT, MdmIrisPayload.CisHandleMode.NO_RESTRICTION);

        referenceItemRepository.insertBatch(ssku1SurplusItem, ssku2SurplusItem);

        sskuToRefreshRepository.enqueueAll(List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2), MdmEnqueueReason.CHANGED_IRIS_ITEM);

        processSskuToRefreshQueueAfterCommit();

        processMskuQueueService.processQueueItems();
        processSskuToRefreshQueueAfterCommit();

        List<ReferenceItemWrapper> finalReferenceItems = referenceItemRepository.findAll();

        Map<ShopSkuKey, List<MdmIrisPayload.ReferenceInformation>> informationBySsku = finalReferenceItems.stream()
            .collect(Collectors.toMap(ReferenceItemWrapper::getKey, item -> item.getItem().getInformationList()));
        Assertions.assertThat(informationBySsku.keySet()).containsExactlyInAnyOrder(
            SHOP_SKU_KEY_1, SHOP_SKU_KEY_2);

        // SHOP_SKU_KEY_1 receives dimensions from SHOP_SKU_KEY_2 and Surplus/CIS is retained
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1)).hasSize(2);
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:10 shop_sku:test2 warehouse:145").build());
        Assertions.assertThat(clearUpdatedTs(informationBySsku.get(SHOP_SKU_KEY_1).get(0).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(shippingUnitSsku3.build()));
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1)
            .get(1).getSurplusHandleInfo().getValue()).isEqualTo(MdmIrisPayload.SurplusHandleMode.REJECT);

        // SHOP_SKU_KEY_2 receives dimensions from itself and Surplus/CIS is retained
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_2)).hasSize(3);
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_2).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.WAREHOUSE)
                .setId("145").build());
        Assertions.assertThat(clearUpdatedTs(informationBySsku.get(SHOP_SKU_KEY_2).get(0).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(shippingUnitSsku3.build()));
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_2)
            .get(1).getSurplusHandleInfo().getValue()).isEqualTo(MdmIrisPayload.SurplusHandleMode.ACCEPT);
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_2)
            .get(2).getCisHandleInfo().getValue()).isEqualTo(MdmIrisPayload.CisHandleMode.NO_RESTRICTION);
    }

    @Test
    public void testForceInheritanceSetsCorrectDqScore() {
        weightDimensionsService.setReplaceWarehouseWithSupplierFromDefaultTs(
            TimestampUtil.toInstant(LocalDateTime.of(3000, 1, 1, 0, 0)));

        raiseForceInheritanceSwitches(CATEGORY_ID);

        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));
        masterDataRepository.insert(createTestMasterData(SHOP_SKU_KEY_1));

        // set dq score for our source
        supplierDqScoreRepository.insert(new SupplierDqScore(MasterDataSourceType.WAREHOUSE, "145", 10));

        // stage 1: force inheritance for new items
        var shippingUnitSsku1 = ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null, 100L);
        FromIrisItemWrapper ssku1Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145", shippingUnitSsku1));

        fromIrisItemRepository.insertOrUpdateAll(List.of(ssku1Item));

        sskuToRefreshRepository.enqueue(SHOP_SKU_KEY_1, MdmEnqueueReason.CHANGED_IRIS_ITEM);

        processSskuToRefreshQueueAfterCommit();

        processMskuQueueService.processQueueItems();

        processSskuToRefreshQueueAfterCommit();

        List<ReferenceItemWrapper> finalReferenceItems = referenceItemRepository.findAll();

        Map<ShopSkuKey, List<MdmIrisPayload.ReferenceInformation>> informationBySsku = finalReferenceItems.stream()
            .collect(Collectors.toMap(ReferenceItemWrapper::getKey, item -> item.getItem().getInformationList()));
        Assertions.assertThat(informationBySsku.keySet()).containsExactlyInAnyOrder(SHOP_SKU_KEY_1);

        // SHOP_SKU_KEY_1 receives dimensions from itself and Surplus/CIS is retained
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1)).hasSize(1);
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.WAREHOUSE)
                .setId("145").build());

        var shippingUnitWithDqScore = ItemWrapperTestUtil.generateShippingUnitWithDqScore(
            10.0, 15.0, 20.0, 1.0, null, null, 100L, 10);
        Assertions.assertThat(clearUpdatedTs(informationBySsku.get(SHOP_SKU_KEY_1).get(0).getItemShippingUnit()))
            .isEqualTo(clearUpdatedTs(shippingUnitWithDqScore.build()));
    }

    @Test
    public void testForceInheritanceWorksIfNoReferenceItemAndBusinessEnabled() {
        // test business enabled configuration
        int businessId = 123456;
        mdmSupplierRepository.insertOrUpdate(new MdmSupplier().setId(businessId).setType(MdmSupplierType.BUSINESS));
        mdmSupplierRepository.insertOrUpdate(new MdmSupplier().setId(SHOP_SKU_KEY_1.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(businessId).setBusinessEnabled(true));
        keyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        sskuExistenceRepository.markExistence(SHOP_SKU_KEY_1, true);

        raiseForceInheritanceSwitches(CATEGORY_ID);
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));

        MskuParamValue mskuLength = (MskuParamValue) new MskuParamValue().setMskuId(MSKU_1)
            .setMdmParamId(KnownMdmParams.LENGTH).setNumeric(new BigDecimal(10))
            .setMasterDataSourceType(MasterDataSourceType.WAREHOUSE)
            .setMasterDataSourceId("supplier_id:1 shop_sku:1 warehouse:1");
        MskuParamValue mskuWidth = (MskuParamValue) new MskuParamValue().setMskuId(MSKU_1)
            .setMdmParamId(KnownMdmParams.WIDTH).setNumeric(new BigDecimal(15))
            .setMasterDataSourceType(MasterDataSourceType.WAREHOUSE)
            .setMasterDataSourceId("supplier_id:1 shop_sku:1 warehouse:1");
        MskuParamValue mskuHeight = (MskuParamValue) new MskuParamValue().setMskuId(MSKU_1)
            .setMdmParamId(KnownMdmParams.HEIGHT).setNumeric(new BigDecimal(20))
            .setMasterDataSourceType(MasterDataSourceType.WAREHOUSE)
            .setMasterDataSourceId("supplier_id:1 shop_sku:1 warehouse:1");
        MskuParamValue mskuWeight = (MskuParamValue) new MskuParamValue().setMskuId(MSKU_1)
            .setMdmParamId(KnownMdmParams.WEIGHT_GROSS).setNumeric(new BigDecimal(1))
            .setMasterDataSourceType(MasterDataSourceType.WAREHOUSE)
            .setMasterDataSourceId("supplier_id:1 shop_sku:1 warehouse:1");
        var msku = new CommonMsku(MSKU_1, List.of(mskuLength, mskuWidth, mskuHeight, mskuWeight));
        mskuRepository.insertOrUpdateMsku(msku);

        mdmQueuesManager.enqueueSsku(SHOP_SKU_KEY_1, MdmEnqueueReason.CHANGED_MSKU_DATA);

        processSskuToRefreshQueueAfterCommit();

        List<ReferenceItemWrapper> refItems = referenceItemRepository.findAll();

        Map<ShopSkuKey, List<MdmIrisPayload.ReferenceInformation>> informationBySsku = refItems.stream()
            .collect(Collectors.toMap(ReferenceItemWrapper::getKey, item -> item.getItem().getInformationList()));
        Assertions.assertThat(informationBySsku.keySet()).containsExactlyInAnyOrder(SHOP_SKU_KEY_1);

        // SHOP_SKU_KEY_1 has dimensions block + surplus / cis blocks
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1)).hasSize(1);
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:1 shop_sku:1 warehouse:1").build());
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1).get(0).getItemShippingUnit()
            .getLengthMicrometer().getValue()).isEqualTo(100000);
    }

    @Test
    public void testForceInheritanceWorksIfMskuCategoryIsDifferentInMappings() {
        raiseForceInheritanceSwitches(CATEGORY_ID);
        keyValueService.putValue(MdmProperties.FIX_CATEGORY_ID_IN_MAPPINGS, true);

        // 1. mapping in some random category id
        mappingsCacheRepository.insert(createTestMappingInCategory(SHOP_SKU_KEY_2, MSKU_1, 1234));
        // 2. mapping in default category id (in which force inheritance is enabled) - latest non-zero mapping
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));
        // 3. mapping zero category id
        mappingsCacheRepository.insert(createTestMappingInCategory(SHOP_SKU_KEY_3, MSKU_1, 0));

        var shippingUnitSsku1 = ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null, 100L);
        FromIrisItemWrapper ssku1Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145", shippingUnitSsku1));
        var shippingUnitSsku2 = ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 25.0, 1.0, null, null, 110L);
        FromIrisItemWrapper ssku2Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "147", shippingUnitSsku2));
        var shippingUnitSsku3 = ItemWrapperTestUtil.generateShippingUnit(10.0, 20.0, 30.0, 1.0, null, null, 120L);
        FromIrisItemWrapper ssku3Item = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_3, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145", shippingUnitSsku3));

        fromIrisItemRepository.insertOrUpdateAll(List.of(ssku1Item, ssku2Item, ssku3Item));

        sskuToRefreshRepository.enqueueAll(
            List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3),
            MdmEnqueueReason.CHANGED_IRIS_ITEM
        );

        processSskuToRefreshQueueAfterCommit();

        processMskuQueueService.processQueueItems();

        processSskuToRefreshQueueAfterCommit();

        List<ReferenceItemWrapper> refItems = referenceItemRepository.findAll();

        Map<ShopSkuKey, List<MdmIrisPayload.ReferenceInformation>> informationBySsku = refItems.stream()
            .collect(Collectors.toMap(ReferenceItemWrapper::getKey, item -> item.getItem().getInformationList()));
        Assertions.assertThat(informationBySsku.keySet())
            .containsExactlyInAnyOrder(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3);

        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1)).hasSize(1);
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:11 shop_sku:test3 warehouse:145").build());
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_1).get(0).getItemShippingUnit()
            .getHeightMicrometer().getValue()).isEqualTo(300000);

        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_2)).hasSize(1);
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_2).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM)
                .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
                .setId("msku:100 supplier_id:11 shop_sku:test3 warehouse:145").build());
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_2).get(0).getItemShippingUnit()
            .getHeightMicrometer().getValue()).isEqualTo(300000);

        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_3)).hasSize(1);
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_3).get(0).getSource()).isEqualTo(
            MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.WAREHOUSE)
                .setId("145").build());
        Assertions.assertThat(informationBySsku.get(SHOP_SKU_KEY_3).get(0).getItemShippingUnit()
            .getHeightMicrometer().getValue()).isEqualTo(300000);

        keyValueService.putValue(MdmProperties.FIX_CATEGORY_ID_IN_MAPPINGS, false);

    }

    private void raiseForceInheritanceSwitches(int categoryId) {
        keyValueService.putValue(MdmProperties.CATEGORIES_TO_WRITE_GOLD_WD_TO_SSKU_GOLD_TABLE, List.of(categoryId));
        keyValueService.putValue(MdmProperties.CATEGORIES_TO_WRITE_OWN_SSKU_WD, List.of(categoryId));
        keyValueService.putValue(MdmProperties.CATEGORIES_TO_USE_OWN_SSKU_WD_FOR_MSKU_GOLD, List.of(categoryId));
        keyValueService.putValue(MdmProperties.CATEGORIES_TO_APPLY_FORCE_INHERITANCE, List.of(categoryId));
    }

    private MskuParamValue createNumericMskuParamValue(long mdmParamId, long mskuId, double number) {
        return TestMdmParamUtils.createMskuParamValue(
            mdmParamId, mskuId, null, number, null, null, MasterDataSourceType.SUPPLIER,
            LONG_AGO);
    }

    private void prepareSuppliers(List<ShopSkuKey> sskus, MdmSupplierType type) {
        // добавим информацию о поставщике
        mdmSupplierRepository.insertBatch(sskus.stream()
            .map(ShopSkuKey::getSupplierId)
            .distinct()
            .map(id -> new MdmSupplier()
                .setId(id)
                .setType(type))
            .collect(Collectors.toList()));
    }

    private static MdmIrisPayload.ShippingUnit clearUpdatedTs(MdmIrisPayload.ShippingUnit shippingUnit) {
        MdmIrisPayload.ShippingUnit.Builder builder = shippingUnit.toBuilder();
        if (builder.hasLengthMicrometer()) {
            builder.setLengthMicrometer(builder.getLengthMicrometerBuilder().clearUpdatedTs());
        }
        if (builder.hasWidthMicrometer()) {
            builder.setWidthMicrometer(builder.getWidthMicrometerBuilder().clearUpdatedTs());
        }
        if (builder.hasHeightMicrometer()) {
            builder.setHeightMicrometer(builder.getHeightMicrometerBuilder().clearUpdatedTs());
        }
        if (builder.hasWeightGrossMg()) {
            builder.setWeightGrossMg(builder.getWeightGrossMgBuilder().clearUpdatedTs());
        }
        if (builder.hasWeightNetMg()) {
            builder.setWeightNetMg(builder.getWeightNetMgBuilder().clearUpdatedTs());
        }
        if (builder.hasWeightTareMg()) {
            builder.setWeightTareMg(builder.getWeightTareMgBuilder().clearUpdatedTs());
        }
        return builder.build();
    }
}
