package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenBlocksPostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenSplitterMerger;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuSilverItemPreProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MskuSilverSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGoodGroupRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CustomsCommCodeRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToMboQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.GlobalParamValueService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.WarehouseProjectionCacheImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidationService;
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
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmLmsCargoTypeCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MskuGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.priceinfo.PriceInfoRepository;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.service.MdmParameterValueCachingServiceMock;
import ru.yandex.market.mbo.mdm.common.service.mapping.MdmBestMappingsProvider;
import ru.yandex.market.mbo.mdm.common.service.queue.ProcessMskuQueueService;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataValidator;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepository;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsService;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsServiceImpl;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.common.utils.TaskQueueRegistratorMock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class RecomputeMskuGoldExecutorBaseTest extends MdmBaseDbTestClass {
    protected static final long SEED = 7005721L;
    protected MdmParameterValueCachingServiceMock parameterValueCachingServiceMock;
    protected MdmCategorySettingsService mdmCategorySettingsService;
    protected EnhancedRandom random;

    @Autowired
    protected MdmSupplierCachingService mdmSupplierCachingService;
    @Autowired
    protected SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    protected SskuToRefreshRepository sskuQueue;
    @Autowired
    protected MskuToRefreshRepository mskuQueue;
    @Autowired
    protected MdmQueuesManager mdmQueuesManager;
    @Autowired
    protected CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    protected MdmParamCache mdmParamCache;
    @Autowired
    protected MdmLmsCargoTypeCache mdmLmsCargoTypeCache;
    @Autowired
    protected ReferenceItemRepository referenceItemRepository;
    @Autowired
    protected GlobalParamValueService globalParamValueService;
    @Autowired
    protected GoldSskuRepository goldSskuRepository;
    @Autowired
    protected StorageKeyValueService storageKeyValueService;
    @Autowired
    protected FeatureSwitchingAssistant featureSwitchingAssistant;
    @Autowired
    protected CustomsCommCodeRepository codeRepository;
    @Autowired
    protected MdmGoodGroupRepository mdmGoodGroupRepository;
    @Autowired
    protected PriceInfoRepository priceInfoRepository;
    @Autowired
    protected MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    protected SskuGoldenParamUtil sskuGoldenParamUtil;
    @Autowired
    protected MskuRepository mskuRepository;
    @Autowired
    protected MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    protected MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    protected MasterDataRepository masterDataRepository;
    @Autowired
    protected CargoTypeRepository cargoTypeRepository;
    @Autowired
    protected MdmBestMappingsProvider mdmBestMappingsProvider;
    @Autowired
    protected MskuToMboQueueRepository mskuToMboQueue;
    @Autowired
    protected WeightDimensionsValidator weightDimensionsValidator;

    protected MskuProcessingDataProviderImpl mskuProcessingDataProvider;
    protected RecomputeMskuGoldExecutor executor;

    @Before
    public void setup() {

        random = TestDataUtils.defaultRandom(SEED);
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        storageKeyValueService.putValue(MdmProperties.FIX_CATEGORY_ID_IN_MAPPINGS, true);
        storageKeyValueService.putValue(MdmProperties.COMPUTE_MSKU_EXPIR_DATE_FROM_SSKUS_SHELF_LIVES_GLOBAL, true);
        storageKeyValueService.putValue(MdmProperties.WRITE_MSKU_EXPIR_DATE_COMPUTED_FROM_SSKUS_SHELF_LIVES, true);
        storageKeyValueService.putValue(MdmProperties.COMPUTE_GOLD_MSKU_DIMENSIONS_USING_GEOMETRIC_MEAN_GLOBAL, true);
        storageKeyValueService.putValue(MdmProperties.USE_PRICES_IN_MSKU_GOLD_COMPUTATION, true);
        storageKeyValueService.putValue(MdmProperties.WRITE_GOLD_WD_TO_SSKU_GOLD_TABLE_GLOBALLY, true);
        storageKeyValueService.putValue(MdmProperties.WRITE_OWN_SSKU_WD_GLOBALLY, true);
        storageKeyValueService.putValue(MdmProperties.USE_OWN_SSKU_WD_FOR_MSKU_GOLD_GLOBALLY, true);

        parameterValueCachingServiceMock = new MdmParameterValueCachingServiceMock();
        mdmCategorySettingsService = new MdmCategorySettingsServiceImpl(parameterValueCachingServiceMock,
            cargoTypeRepository, categoryParamValueRepository);

        mskuProcessingDataProvider = setupMskuProcessingDataProvider();

        MskuGoldenItemService mskuGIS = mskuGoldenItemService();

        MasterDataValidator masterDataValidator = mock(MasterDataValidator.class);
        when(masterDataValidator.validateMasterData(any(MasterData.class))).thenReturn(List.of());

        RecomputeMskuGoldServiceImpl recomputeMskuGoldService = new RecomputeMskuGoldServiceImpl(
            mskuProcessingDataProvider,
            mskuProcessingPipeProcessor(),
            mskuCalculatingProcessor(mskuGIS, masterDataValidator));

        ProcessMskuQueueService processMskuQueueService = new ProcessMskuQueueService(mskuQueue,
            storageKeyValueService,
            recomputeMskuGoldService);

        executor = new RecomputeMskuGoldExecutor(processMskuQueueService);
        MockitoAnnotations.initMocks(this);
        cargoTypeRepository.deleteAll();
        mdmLmsCargoTypeCache.refresh();
        storageKeyValueService.invalidateCache();
    }

    private MskuProcessingDataProviderImpl setupMskuProcessingDataProvider() {

        return new MskuProcessingDataProviderImpl(
            mskuRepository,
            categoryParamValueRepository,
            mdmCategorySettingsService,
            masterDataRepository,
            globalParamValueService,
            goldSskuRepository,
            storageKeyValueService,
            priceInfoRepository,
            Mockito.mock(WarehouseProjectionCacheImpl.class),
            mdmParamCache,
            mdmBestMappingsProvider
        );
    }

    private MskuGoldenItemService mskuGoldenItemService() {
        MasterDataValidator masterDataValidator = mock(MasterDataValidator.class);
        when(masterDataValidator.validateMasterData(any(MasterData.class))).thenReturn(List.of());

        MskuGoldenSplitterMerger goldenSplitterMerger = new MskuGoldenSplitterMerger(mdmParamCache);
        MskuSilverSplitter mskuSilverSplitter =
            new MskuSilverSplitter(mdmParamCache, sskuGoldenParamUtil);

        MskuSilverItemPreProcessor mskuSilverItemPreProcessor = new MskuSilverItemPreProcessor(
            mdmParamCache, mdmLmsCargoTypeCache, featureSwitchingAssistant);
        CustomsCommCodeMarkupService markupService = new CustomsCommCodeMarkupServiceImpl(mdmParamCache, codeRepository,
            new CCCodeValidationService(List.of(), codeRepository), categoryParamValueRepository,
            new TaskQueueRegistratorMock(), mdmGoodGroupRepository, mappingsCacheRepository);

        MskuGoldenBlocksPostProcessor mskuGoldenBlocksPostProcessor = new MskuGoldenBlocksPostProcessor(
            featureSwitchingAssistant, mdmParamCache, markupService, storageKeyValueService);

        WeightDimensionBlockValidationServiceImpl validationService = new WeightDimensionBlockValidationServiceImpl(
            new CachedItemBlockValidationContextProviderImpl(storageKeyValueService),
            weightDimensionsValidator
        );
        return new MskuGoldenItemService(mskuSilverSplitter,
            goldenSplitterMerger, goldenSplitterMerger, mskuSilverItemPreProcessor, featureSwitchingAssistant,
            mskuGoldenBlocksPostProcessor, validationService, mdmParamCache);
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

}
