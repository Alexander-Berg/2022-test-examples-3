package ru.yandex.market.mbo.mdm.common.masterdata.services.iris;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.transaction.TestTransaction;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldenItemPostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.WeightDimensionsSilverItemSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuGoldenVerdictRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MasterDataGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
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
import ru.yandex.market.mbo.mdm.common.service.AllOkMasterDataValidator;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.service.queue.ProcessSskuToRefreshQueueService;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmDbWithCleaningTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

@SuppressWarnings("checkstyle:magicNumber")
public abstract class GoldenItemServiceImplTestBase extends MdmDbWithCleaningTestClass {
    private static final int SEED = 43;
    protected static final int SUPPLIER_ID = 1;
    protected static final int CATEGORY_ID_1 = 10;
    protected static final int CATEGORY_ID_2 = 11;
    protected static final int BUSINESS_ID = 100;

    protected static final ShopSkuKey BUSINESS_KEY = new ShopSkuKey(1000, "sku");
    protected static final ShopSkuKey STAGE2_KEY = new ShopSkuKey(2, "sku");
    protected static final ShopSkuKey STAGE3_KEY1 = new ShopSkuKey(31, "sku");
    protected static final ShopSkuKey STAGE3_KEY2 = new ShopSkuKey(32, "sku");

    protected EnhancedRandom random;

    @Autowired
    protected MasterDataRepository masterDataRepository;
    @Autowired
    protected FromIrisItemRepository fromIrisItemRepository;
    @Autowired
    protected ReferenceItemRepository referenceItemRepository;
    @Autowired
    protected CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    protected MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    protected MskuRepository mskuRepository;
    @Autowired
    protected StorageKeyValueService storageKeyValueService;
    @Autowired
    private MdmQueuesManager mdmQueuesManager;
    @Autowired
    protected SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    protected SskuGoldenVerdictRepository sskuGoldenVerdictRepository;
    @Autowired
    protected ServiceSskuConverter serviceSskuConverter;
    @Autowired
    protected MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    protected MasterDataBusinessMergeService masterDataBusinessMergeService;
    @Autowired
    protected MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    protected SilverSskuRepository silverSskuRepository;
    @Autowired
    protected MdmSupplierCachingService mdmSupplierCachingService;
    @Autowired
    private MasterDataVersionMapService masterDataVersionMapService;
    @Autowired
    private VerdictCalculationHelper verdictCalculationHelper;
    @Autowired
    private SskuGoldenParamUtil sskuGoldenParamUtil;
    @Autowired
    protected GoldSskuRepository goldSskuRepository;
    @Autowired
    private FeatureSwitchingAssistant featureSwitchingAssistant;
    @Autowired
    private SskuProcessingDataProvider sskuProcessingDataProvider;
    @Autowired
    protected SskuToRefreshRepository sskuToRefreshRepository;
    @Autowired
    private RslGoldenItemService rslGoldenItemService;
    @Autowired
    private MultivalueBusinessHelper multivalueBusinessHelper;
    @Autowired
    private MasterDataGoldenItemService masterDataGoldenItemService;
    @Autowired
    private TraceableSskuGoldenItemService traceableSskuGoldenItemService;
    @Autowired
    private WeightDimensionsValidator weightDimensionsValidator;


    protected WeightDimensionsGoldenItemService weightDimensionsService;
    private ProcessSskuToRefreshQueueService sskuToRefreshQueueService;

    protected void setUp() throws Exception {
        random = TestDataUtils.defaultRandom(SEED);

        CachedItemBlockValidationContextProvider validationContextProvider =
            new CachedItemBlockValidationContextProviderImpl(storageKeyValueService);
        var itemBlockValidationService = new WeightDimensionBlockValidationServiceImpl(
            validationContextProvider,
            weightDimensionsValidator
        );
        weightDimensionsService = new WeightDimensionsGoldenItemService(
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
                new SurplusAndCisGoldenItemServiceMock(),
                storageKeyValueService,
                serviceSskuConverter,
                masterDataBusinessMergeService,
                Mockito.mock(OfferCutoffService.class),
                sskuGoldenParamUtil,
                validationContextProvider,
                weightDimensionsValidator,
                rslGoldenItemService
            );

        SskuGoldenMasterDataCalculationHelper goldenMasterDataCalculator =
            new SskuGoldenMasterDataCalculationHelper(
                serviceSskuConverter,
                new MasterDataValidationService(new AllOkMasterDataValidator()),
                masterDataBusinessMergeService,
                storageKeyValueService,
                masterDataGoldenItemService,
                sskuGoldenParamUtil,
                traceableSskuGoldenItemService,
                multivalueBusinessHelper
            );

        SskuToRefreshProcessingService sskuToRefreshProcessingService = new SskuToRefreshProcessingServiceImpl(
            sskuProcessingDataProvider,
            sskuVerdictProcessor(),
            sskuProcessingPostProcessor(goldenReferenceItemCalculator),
            sskuProcessingContextProvider(),
            sskuProcessingPipeProcessor(),
            sskuProcessingPreProcessor(),
            sskuCalculatingProcessor(goldenReferenceItemCalculator, goldenMasterDataCalculator));

        sskuToRefreshQueueService = new ProcessSskuToRefreshQueueService(
            sskuToRefreshRepository,
            storageKeyValueService,
            mdmSskuGroupManager,
            sskuToRefreshProcessingService,
            transactionTemplate
        );

        weightDimensionsService.setReplaceWarehouseWithSupplierFromDefaultTs(
            TimestampUtil.toInstant(LocalDateTime.of(3000, 1, 1, 0, 0)));
        sskuExistenceRepository.markExistence(List.of(STAGE2_KEY, STAGE3_KEY1, STAGE3_KEY2), true);
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
        return new SskuProcessingContextProviderImpl(storageKeyValueService);
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

    protected GoldenItemPostProcessor<ReferenceItemWrapper> getGoldenItemPostProcessor() {
        return GoldenItemPostProcessor.doNothingPostProcessor();
    }

    protected void processGoldComputation() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();
        sskuToRefreshQueueService.processQueueItems();
    }

    protected List<FromIrisItemWrapper> getUnprocessedItems(int batchSize) {
        return fromIrisItemRepository.getUnprocessedItemsBatch(batchSize);
    }

    protected MasterData createMasterData(ShopSkuKey key) {
        MasterData masterData = TestDataUtils.generateMasterData(key, random);
        masterData.setItemShippingUnit(null);
        masterData.setGoldenItemShippingUnit(null);
        masterData.setGoldenRsl(null);
        masterData.setSurplusHandleMode(null);
        masterData.setCisHandleMode(null);
        masterData.setMeasurementState(null);
        return masterData;
    }

    protected SilverCommonSsku createSskuSilverParamValuesFromShippingUnit(
        MdmIrisPayload.ShippingUnit shippingUnit,
        ShopSkuKey shopSkuKey,
        MasterDataSourceType sourceType,
        String sourceId) {

        List<SskuSilverParamValue> result = new ArrayList<>();
        if (shippingUnit.hasLengthMicrometer()) {
            result.add(createMdmParamValue(KnownMdmParams.LENGTH,
                MdmProperties.CM_IN_UM.multiply(new BigDecimal(shippingUnit.getLengthMicrometer().getValue())),
                sourceType,
                sourceId,
                TimestampUtil.toInstant(shippingUnit.getLengthMicrometer().getUpdatedTs()),
                shopSkuKey));
        }
        if (shippingUnit.hasWidthMicrometer()) {
            result.add(createMdmParamValue(KnownMdmParams.WIDTH,
                MdmProperties.CM_IN_UM.multiply(new BigDecimal(shippingUnit.getWidthMicrometer().getValue())),
                sourceType,
                sourceId,
                TimestampUtil.toInstant(shippingUnit.getWidthMicrometer().getUpdatedTs()),
                shopSkuKey));
        }
        if (shippingUnit.hasHeightMicrometer()) {
            result.add(createMdmParamValue(KnownMdmParams.HEIGHT,
                MdmProperties.CM_IN_UM.multiply(new BigDecimal(shippingUnit.getHeightMicrometer().getValue())),
                sourceType,
                sourceId,
                TimestampUtil.toInstant(shippingUnit.getHeightMicrometer().getUpdatedTs()),
                shopSkuKey));
        }
        if (shippingUnit.hasWeightGrossMg()) {
            result.add(createMdmParamValue(KnownMdmParams.WEIGHT_GROSS,
                MdmProperties.KG_IN_MG.multiply(new BigDecimal(shippingUnit.getWeightGrossMg().getValue())),
                sourceType,
                sourceId,
                TimestampUtil.toInstant(shippingUnit.getWeightGrossMg().getUpdatedTs()),
                shopSkuKey));
        }
        if (shippingUnit.hasWeightNetMg()) {
            result.add(createMdmParamValue(KnownMdmParams.WEIGHT_NET,
                MdmProperties.KG_IN_MG.multiply(new BigDecimal(shippingUnit.getWeightNetMg().getValue())),
                sourceType,
                sourceId,
                TimestampUtil.toInstant(shippingUnit.getWeightNetMg().getUpdatedTs()),
                shopSkuKey));
        }
        if (shippingUnit.hasWeightTareMg()) {
            result.add(createMdmParamValue(KnownMdmParams.WEIGHT_TARE,
                MdmProperties.KG_IN_MG.multiply(new BigDecimal(shippingUnit.getWeightTareMg().getValue())),
                sourceType,
                sourceId,
                TimestampUtil.toInstant(shippingUnit.getWeightTareMg().getUpdatedTs()),
                shopSkuKey));
        }
        return new SilverCommonSsku(
            new SilverSskuKey(shopSkuKey, new MasterDataSource(sourceType, sourceId))
        ).addBaseValues(result);
    }

    protected SskuSilverParamValue createMdmParamValue(long paramId, BigDecimal value,
                                                       MasterDataSourceType sourceType, String sourceId,
                                                       Instant sourceUpdatedTs, ShopSkuKey shopSkuKey) {
        var paramValue = new SskuSilverParamValue();
        paramValue.setMdmParamId(paramId);
        paramValue.setNumeric(value);
        paramValue.setMasterDataSourceType(sourceType);
        paramValue.setMasterDataSourceId(sourceId);
        paramValue.setSourceUpdatedTs(sourceUpdatedTs);
        paramValue.setShopSkuKey(shopSkuKey);
        return paramValue;
    }
}
