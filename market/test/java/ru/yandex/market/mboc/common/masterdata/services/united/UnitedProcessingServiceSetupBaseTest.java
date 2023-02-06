package ru.yandex.market.mboc.common.masterdata.services.united;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.WeightDimensionsSilverItemSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenBlocksPostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenSplitterMerger;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuSilverItemPreProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MskuSilverSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuGoldenParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueuePriorities;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGoodGroupRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SupplierDqScoreRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CustomsCommCodeRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuAndSskuQueue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MskuToMboQueueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.CategoryRslRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.MskuRslRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.SupplierRslRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuGoldenVerdictRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuPartnerVerdictRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.GlobalParamValueService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.WarehouseProjectionCacheImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.RslGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.SurplusAndCisGoldenItemService;
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
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CustomsCommCodeMarkupService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.CustomsCommCodeMarkupServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MasterDataGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmLmsCargoTypeCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MskuGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.rsl.RslMarkupsParamsService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.CommonSskuBuilder;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.MskuParamValuesForSskuLoader;
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
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuVerdictProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuVerdictProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.united.UnitedProcessingDataProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.services.united.UnitedProcessingDataProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.united.UnitedProcessingRawDataLoader;
import ru.yandex.market.mbo.mdm.common.masterdata.services.united.UnitedProcessingRawDataLoaderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.united.UnitedProcessingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.united.UnitedProcessingServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.MasterDataValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.MasterDataVersionMapService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictCalculationHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.warehouse.MdmWarehouseService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.VghValidationRequirementsProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.priceinfo.PriceInfoRepository;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.service.MdmParameterValueCachingServiceMock;
import ru.yandex.market.mbo.mdm.common.service.mapping.MdmBestMappingsProvider;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.parsing.MasterDataValidator;
import ru.yandex.market.mboc.common.masterdata.repository.CargoTypeRepository;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsService;
import ru.yandex.market.mboc.common.masterdata.services.category.MdmCategorySettingsServiceImpl;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.common.utils.TaskQueueRegistratorMock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class UnitedProcessingServiceSetupBaseTest extends MdmBaseDbTestClass {
    protected static final long SEED = 7005721L;
    protected MdmParameterValueCachingServiceMock parameterValueCachingServiceMock;
    protected MdmCategorySettingsService mdmCategorySettingsService;
    protected EnhancedRandom random;

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
    protected ServiceSskuConverter serviceSskuConverter;
    @Autowired
    protected MasterDataBusinessMergeService masterDataBusinessMergeService;
    @Autowired
    protected MasterDataVersionMapService masterDataVersionMapService;
    @Autowired
    protected OfferCutoffService cutoffService;
    @Autowired
    protected VerdictCalculationHelper verdictCalculationHelper;
    @Autowired
    protected SskuProcessingDataProvider sskuProcessingDataProvider;
    @Autowired
    protected MdmQueuesManager queuesManager;
    @Autowired
    protected RslGoldenItemService rslGoldenItemService;
    @Autowired
    protected MasterDataValidationService masterDataValidationService;
    @Autowired
    protected MasterDataGoldenItemService masterDataGoldenItemService;
    @Autowired
    protected FromIrisItemRepository fromIrisItemRepository;
    @Autowired
    protected SupplierDqScoreRepository supplierDqScoreRepository;
    @Autowired
    protected MskuParamValuesForSskuLoader mskuParamValuesForSskuLoader;
    @Autowired
    protected VghValidationRequirementsProvider vghValidationRequirementsProvider;
    @Autowired
    protected RslMarkupsParamsService rslMarkupsParamsService;
    @Autowired
    protected MskuRepository mskuRepository;
    @Autowired
    protected MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    protected MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    protected SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    protected MskuAndSskuQueue mskuAndSskuQueue;
    @Autowired
    protected MdmParamCache paramCache;
    @Autowired
    protected SilverSskuRepository silverSskuRepository;
    @Autowired
    protected QualityDocumentRepository qualityDocumentRepository;
    @Autowired
    protected MasterDataRepository masterDataRepository;
    @Autowired
    protected SskuGoldenVerdictRepository verdictRepository;
    @Autowired
    protected SskuGoldenVerdictRepository sskuGoldenVerdictRepository;
    @Autowired
    protected MdmSupplierCachingService mdmSupplierCachingService;
    @Autowired
    protected MdmWarehouseService mdmWarehouseService;
    @Autowired
    protected SskuPartnerVerdictRepository sskuPartnerVerdictRepository;
    @Autowired
    protected MskuRslRepository mskuRslRepository;
    @Autowired
    protected CategoryRslRepository categoryRslRepository;
    @Autowired
    protected SupplierRslRepository supplierRslRepository;
    @Autowired
    protected WeightDimensionBlockValidationService weightDimensionBlockValidationService;
    @Autowired
    protected CargoTypeRepository cargoTypeRepository;
    @Autowired
    protected MskuToMboQueueRepository mskuToMboQueue;
    @Autowired
    protected SurplusAndCisGoldenItemService surplusAndCisGoldenItemService;
    @Autowired
    protected TraceableSskuGoldenItemService traceableSskuGoldenItemService;
    @Autowired
    protected MdmBestMappingsProvider mdmBestMappingsProvider;
    @Autowired
    protected WeightDimensionsValidator weightDimensionsValidator;
    @Autowired
    protected SskuGoldenMasterDataCalculationHelper sskuGoldenMasterDataCalculationHelper;

    protected UnitedProcessingService unitedProcessingService;
    protected UnitedProcessingDataProvider unitedProcessingDataProvider;
    protected MskuProcessingDataProviderImpl mskuProcessingDataProvider;
    protected GoldSskuRepository goldSskuRepositorySpy;

    @Before
    public void setup() {

        random = TestDataUtils.defaultRandom(SEED);
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        storageKeyValueService.putValue(MdmProperties.CREATE_FORBIDDING_VERDICTS_ON_EMPTY_SHIPPING_UNIT, true);
        storageKeyValueService.putValue(MdmProperties.RSL_GOLD_PROCESSING_ENABLED, true);
        storageKeyValueService.putValue(MdmProperties.FIX_CATEGORY_ID_IN_MAPPINGS, true);
        storageKeyValueService.putValue(MdmProperties.MERGE_SSKU_GOLDEN_VERDICTS_ENABLED_KEY, true);
        storageKeyValueService.putValue(MdmProperties.COMPUTE_MSKU_EXPIR_DATE_FROM_SSKUS_SHELF_LIVES_GLOBAL, true);
        storageKeyValueService.putValue(MdmProperties.WRITE_MSKU_EXPIR_DATE_COMPUTED_FROM_SSKUS_SHELF_LIVES, true);
        storageKeyValueService.putValue(MdmProperties.COMPUTE_GOLD_MSKU_DIMENSIONS_USING_GEOMETRIC_MEAN_GLOBAL, true);
        storageKeyValueService.putValue(MdmProperties.USE_PRICES_IN_MSKU_GOLD_COMPUTATION, true);

        parameterValueCachingServiceMock = new MdmParameterValueCachingServiceMock();
        mdmCategorySettingsService = new MdmCategorySettingsServiceImpl(parameterValueCachingServiceMock,
            cargoTypeRepository, categoryParamValueRepository);

        goldSskuRepositorySpy = Mockito.spy(goldSskuRepository);

        unitedProcessingDataProvider = setupUnitedProcessingDataProvider();
        mskuProcessingDataProvider = setupMskuProcessingDataProvider();


        CachedItemBlockValidationContextProvider validationContextProvider =
            new CachedItemBlockValidationContextProviderImpl(storageKeyValueService);
        var itemBlockValidationService = new WeightDimensionBlockValidationServiceImpl(
            validationContextProvider,
            weightDimensionsValidator
        );
        var weightDimensionsService = new WeightDimensionsGoldenItemService(
            new WeightDimensionsSilverItemSplitter(new SupplierConverterServiceMock()),
            itemBlockValidationService,
            featureSwitchingAssistant);
        var forceInheritanceService = new WeightDimensionForceInheritanceService(
            featureSwitchingAssistant,
            new WeightDimensionsForceInheritancePostProcessor(),
            itemBlockValidationService
        );

        SskuGoldenReferenceItemCalculationHelper goldenReferenceItemCalculator =
            new SskuGoldenReferenceItemCalculationHelper(
                weightDimensionsService,
                forceInheritanceService,
                surplusAndCisGoldenItemService,
                storageKeyValueService,
                serviceSskuConverter,
                masterDataBusinessMergeService,
                cutoffService,
                sskuGoldenParamUtil,
                validationContextProvider,
                weightDimensionsValidator,
                rslGoldenItemService
            );
        MasterDataValidator masterDataValidator = mock(MasterDataValidator.class);
        unitedProcessingService = new UnitedProcessingServiceImpl(
            unitedProcessingDataProvider,
            sskuCalculatingProcessor(goldenReferenceItemCalculator, sskuGoldenMasterDataCalculationHelper),
            sskuVerdictProcessor(),
            sskuProcessingContextProvider(),
            sskuProcessingPostProcessor(goldenReferenceItemCalculator),
            mskuCalculatingProcessor(mskuGoldenItemService(), masterDataValidator),
            mskuProcessingPipeProcessor(),
            sskuProcessingPipeProcessor());

        storageKeyValueService.invalidateCache();
    }

    private MskuProcessingDataProviderImpl setupMskuProcessingDataProvider() {

        MasterDataValidator masterDataValidator = mock(MasterDataValidator.class);
        when(masterDataValidator.validateMasterData(any(MasterData.class))).thenReturn(List.of());
        return new MskuProcessingDataProviderImpl(
            mskuRepository,
            categoryParamValueRepository,
            mdmCategorySettingsService,
            masterDataRepository,
            globalParamValueService,
            goldSskuRepositorySpy,
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

    private SskuProcessingPipeProcessor sskuProcessingPipeProcessor() {
        return new SskuProcessingPipeProcessorImpl(mdmQueuesManager, serviceSskuConverter);
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
            goldSskuRepositorySpy
        );
    }

    private SskuProcessingContextProvider sskuProcessingContextProvider() {
        return new SskuProcessingContextProviderImpl(storageKeyValueService);
    }

    private SskuProcessingPostProcessor sskuProcessingPostProcessor(SskuGoldenReferenceItemCalculationHelper
                                                                        sskuGoldenReferenceItemCalculationHelper) {
        return new SskuProcessingPostProcessorImpl(sskuGoldenReferenceItemCalculationHelper);
    }

    private SskuVerdictProcessor sskuVerdictProcessor() {
        return new SskuVerdictProcessorImpl(
            serviceSskuConverter,
            masterDataBusinessMergeService,
            masterDataVersionMapService,
            verdictCalculationHelper);
    }

    public UnitedProcessingDataProviderImpl setupUnitedProcessingDataProvider() {
        return new UnitedProcessingDataProviderImpl(
            storageKeyValueService,
            mskuParamValuesForSskuLoader,
            unitedProcessingRawDataLoader()
        );
    }

    public UnitedProcessingRawDataLoader unitedProcessingRawDataLoader() {
        return new UnitedProcessingRawDataLoaderImpl(
            referenceItemRepository,
            goldSskuRepositorySpy,
            mdmSskuGroupManager,
            categoryParamValueRepository,
            fromIrisItemRepository,
            silverSskuRepository,
            supplierDqScoreRepository,
            masterDataRepository,
            mskuParamValuesForSskuLoader,
            vghValidationRequirementsProvider,
            rslMarkupsParamsService,
            qualityDocumentRepository,
            Mockito.mock(WarehouseProjectionCacheImpl.class),
            mdmBestMappingsProvider,
            mskuRepository,
            mdmCategorySettingsService,
            globalParamValueService,
            storageKeyValueService,
            priceInfoRepository,
            mdmParamCache,
            sskuPartnerVerdictRepository,
            sskuGoldenVerdictRepository);
    }

    protected void processShopSkuKeys(Collection<ShopSkuKey> keys) {
        mskuAndSskuQueue.enqueueSskus(keys, MdmEnqueueReason.CHANGED_SSKU_DATA, MdmQueuePriorities.NORMAL_PRIORITY);
        unitedProcessingService.processKeys(mskuAndSskuQueue.findAll());

    }

    protected void enqueueAndProcessMskus(Collection<Long> ids) {
        mskuAndSskuQueue.enqueueMskus(ids, MdmEnqueueReason.CHANGED_MSKU_DATA, MdmQueuePriorities.NORMAL_PRIORITY);
        unitedProcessingService.processKeys(mskuAndSskuQueue.findAll());
    }

    protected void execute() {
        unitedProcessingService.processKeys(mskuAndSskuQueue.findAll());
    }

    protected void enqueueMskusWithReason(Collection<Long> ids, MdmEnqueueReason reason) {
        mskuAndSskuQueue.enqueueMskus(ids, reason, MdmQueuePriorities.NORMAL_PRIORITY);
    }

    protected SskuSilverParamValue silverValue(ShopSkuKey key,
                                               String sourceId,
                                               MasterDataSourceType type,
                                               Instant updatedTs) {
        return (SskuSilverParamValue) new SskuSilverParamValue()
            .setShopSkuKey(key)
            .setMasterDataSourceId(sourceId)
            .setMasterDataSourceType(type)
            .setUpdatedTs(updatedTs)
            .setSourceUpdatedTs(updatedTs);
    }

    protected SskuGoldenParamValue goldenValue(ShopSkuKey key,
                                               String sourceId,
                                               MasterDataSourceType type,
                                               Instant updatedTs) {
        return (SskuGoldenParamValue) new SskuGoldenParamValue()
            .setShopSkuKey(key)
            .setMasterDataSourceId(sourceId)
            .setMasterDataSourceType(type)
            .setUpdatedTs(updatedTs)
            .setSourceUpdatedTs(updatedTs);
    }

    protected CommonSskuBuilder builder(ShopSkuKey key) {
        return new CommonSskuBuilder(paramCache, key);
    }

    protected void prepareBusinessGroup(int businessIds, int... shopIds) {
        List<MdmSupplier> suppliers = new ArrayList<>();

        MdmSupplier business = new MdmSupplier()
            .setId(businessIds)
            .setType(MdmSupplierType.BUSINESS);
        suppliers.add(business);

        for (var shopId : shopIds) {
            MdmSupplier service = new MdmSupplier()
                .setId(shopId)
                .setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(business.getId())
                .setBusinessEnabled(true);
            suppliers.add(service);
        }

        mdmSupplierRepository.insertBatch(suppliers);
        mdmSupplierCachingService.refresh();
    }
}
