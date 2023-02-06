package ru.yandex.market.mboc.common.masterdata.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.WeightDimensionsSilverItemSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuGoldenParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.RslGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.SurplusAndCisGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.WeightDimensionForceInheritanceService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.WeightDimensionsForceInheritancePostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.WeightDimensionsGoldenItemService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.SskuGoldenMasterDataCalculationHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.SskuGoldenReferenceItemCalculationHelper;
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
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuToRefreshProcessingServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuVerdictProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.processing.SskuVerdictProcessorImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.MasterDataVersionMapService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictCalculationHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class SskuToRefreshProcessingServiceForceInheritanceTest extends MdmBaseDbTestClass {
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private ServiceSskuConverter serviceSskuConverter;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private MasterDataBusinessMergeService masterDataBusinessMergeService;
    @Autowired
    private MasterDataVersionMapService masterDataVersionMapService;
    @Autowired
    private OfferCutoffService cutoffService;
    @Autowired
    private VerdictCalculationHelper verdictCalculationHelper;
    @Autowired
    private SskuGoldenParamUtil sskuGoldenParamUtil;
    @Autowired
    private GoldSskuRepository goldSskuRepository;
    @Autowired
    private FeatureSwitchingAssistant featureSwitchingAssistant;
    @Autowired
    private SskuProcessingDataProvider sskuProcessingDataProvider;
    @Autowired
    private MdmQueuesManager queuesManager;
    @Autowired
    private RslGoldenItemService rslGoldenItemService;
    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private SurplusAndCisGoldenItemService surplusAndCisGoldenItemService;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private MdmSupplierCachingService mdmSupplierCachingService;
    @Autowired
    protected WeightDimensionsValidator weightDimensionsValidator;
    @Autowired
    private SskuGoldenMasterDataCalculationHelper sskuGoldenMasterDataCalculationHelper;

    private SskuToRefreshProcessingServiceImpl sskuToRefreshProcessingService;

    @Before
    public void before() {
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        storageKeyValueService.putValue(MdmProperties.CREATE_FORBIDDING_VERDICTS_ON_EMPTY_SHIPPING_UNIT, true);
        storageKeyValueService.putValue(MdmProperties.RSL_GOLD_PROCESSING_ENABLED, true);
        storageKeyValueService.putValue(MdmProperties.MERGE_SSKU_GOLDEN_VERDICTS_ENABLED_KEY, true);
        storageKeyValueService.putValue(MdmProperties.FIX_CATEGORY_ID_IN_MAPPINGS, true);

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

        sskuToRefreshProcessingService = new SskuToRefreshProcessingServiceImpl(
            sskuProcessingDataProvider,
            sskuVerdictProcessor(),
            sskuProcessingPostProcessor(goldenReferenceItemCalculator),
            sskuProcessingContextProvider(),
            sskuProcessingPipeProcessor(),
            sskuProcessingPreProcessor(),
            sskuCalculatingProcessor(goldenReferenceItemCalculator, sskuGoldenMasterDataCalculationHelper));


        storageKeyValueService.invalidateCache();
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
        return new SskuProcessingPipeProcessorImpl(queuesManager, serviceSskuConverter);
    }

    private SskuProcessingPostProcessor sskuProcessingPostProcessor(SskuGoldenReferenceItemCalculationHelper
                                                                        sskuGoldenReferenceItemCalculationHelper) {
        return new SskuProcessingPostProcessorImpl(sskuGoldenReferenceItemCalculationHelper);
    }

    private SskuProcessingPreProcessor sskuProcessingPreProcessor() {
        return new SskuProcessingPreProcessorImpl(mdmSskuGroupManager);
    }

    private SskuVerdictProcessor sskuVerdictProcessor() {
        return new SskuVerdictProcessorImpl(serviceSskuConverter,
            masterDataBusinessMergeService,
            masterDataVersionMapService,
            verdictCalculationHelper);
    }


    @Test
    public void whenHaveOnlyServiceMappingComputeGoldProperly() {
        // given
        // supplier
        int businessId = 13;
        int serviceId = 12;
        MdmSupplier business = new MdmSupplier()
            .setId(businessId)
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier service = new MdmSupplier()
            .setId(serviceId)
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(businessId)
            .setBusinessEnabled(true);
        mdmSupplierRepository.insertOrUpdateAll(List.of(business, service));
        mdmSupplierCachingService.refresh();

        // ssku
        String shopSku = "U238";
        ShopSkuKey businessKey = new ShopSkuKey(businessId, shopSku);
        ShopSkuKey serviceKey = new ShopSkuKey(serviceId, shopSku);
        sskuExistenceRepository.markExistence(serviceKey, true);

        // mapping (only service key)
        long mskuId = 2222L;
        int categoryId = 111;
        mappingsCacheRepository.insertBatch(
            new MappingCacheDao().setCategoryId(categoryId)
                .setShopSkuKey(serviceKey)
                .setMskuId(mskuId)
        );

        // silver
        SilverCommonSsku silverSsku =
            createSilverSsku(businessKey, "10", "10", "10", "1", "vasya", MasterDataSourceType.SUPPLIER, Instant.now());
        silverSskuRepository.insertOrUpdateSsku(silverSsku);

        // msku
        CommonMsku msku =
            createMsku(mskuId, "15", "15", "15", "1.5", "petya", MasterDataSourceType.SUPPLIER, Instant.now());
        mskuRepository.insertOrUpdateMsku(msku);

        raiseForceInheritanceSwitches(categoryId);

        // when
        sskuToRefreshProcessingService.processShopSkuKeys(List.of(businessKey));

        // then
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findAll();
        Assertions.assertThat(referenceItems).hasSize(1);
        ReferenceItemWrapper referenceItem = referenceItems.iterator().next();
        Assertions.assertThat(referenceItem).isNotNull();
        Assertions.assertThat(referenceItem.getKey()).isEqualTo(serviceKey);
        Assertions.assertThat(referenceItem.getCisHandleMode()).contains(MdmIrisPayload.CisHandleMode.NO_RESTRICTION);
        Assertions.assertThat(referenceItem.getSurplusHandleMode()).contains(MdmIrisPayload.SurplusHandleMode.ACCEPT);
        MdmIrisPayload.ShippingUnit shippingUnit = referenceItem.getCombinedItemShippingUnit();
        Assertions.assertThat(shippingUnit).isNotNull();
        Assertions.assertThat(shippingUnit.getLengthMicrometer().getValue())
            .isEqualTo(extractDimensionMicrometer(msku, KnownMdmParams.LENGTH));
        Assertions.assertThat(shippingUnit.getWidthMicrometer().getValue())
            .isEqualTo(extractDimensionMicrometer(msku, KnownMdmParams.WIDTH));
        Assertions.assertThat(shippingUnit.getHeightMicrometer().getValue())
            .isEqualTo(extractDimensionMicrometer(msku, KnownMdmParams.HEIGHT));
        Assertions.assertThat(shippingUnit.getWeightGrossMg().getValue())
            .isEqualTo(extractWeightMg(msku, KnownMdmParams.WEIGHT_GROSS));

        Map<ShopSkuKey, CommonSsku> goldByKeys =
            goldSskuRepository.findSskus(List.of(serviceKey, businessKey));
        Assertions.assertThat(goldByKeys).containsOnlyKeys(businessKey);
        Map<Long, SskuParamValue> paramValues = goldByKeys.get(businessKey).getBaseValuesByParamId();
        Assertions.assertThat(paramValues).hasSize(8);
        Assertions.assertThat(paramValues.get(KnownMdmParams.LENGTH).getNumeric())
            .isEqualTo(msku.getParamValue(KnownMdmParams.LENGTH).flatMap(MdmParamValue::getNumeric));
        Assertions.assertThat(paramValues.get(KnownMdmParams.WIDTH).getNumeric())
            .isEqualTo(msku.getParamValue(KnownMdmParams.WIDTH).flatMap(MdmParamValue::getNumeric));
        Assertions.assertThat(paramValues.get(KnownMdmParams.HEIGHT).getNumeric())
            .isEqualTo(msku.getParamValue(KnownMdmParams.HEIGHT).flatMap(MdmParamValue::getNumeric));
        Assertions.assertThat(paramValues.get(KnownMdmParams.WEIGHT_GROSS).getNumeric())
            .isEqualTo(msku.getParamValue(KnownMdmParams.WEIGHT_GROSS).flatMap(MdmParamValue::getNumeric));
        Assertions.assertThat(paramValues.get(KnownMdmParams.SSKU_LENGTH).getNumeric())
            .isEqualTo(silverSsku.getBaseValue(KnownMdmParams.LENGTH).flatMap(MdmParamValue::getNumeric));
        Assertions.assertThat(paramValues.get(KnownMdmParams.SSKU_WIDTH).getNumeric())
            .isEqualTo(silverSsku.getBaseValue(KnownMdmParams.WIDTH).flatMap(MdmParamValue::getNumeric));
        Assertions.assertThat(paramValues.get(KnownMdmParams.SSKU_HEIGHT).getNumeric())
            .isEqualTo(silverSsku.getBaseValue(KnownMdmParams.HEIGHT).flatMap(MdmParamValue::getNumeric));
        Assertions.assertThat(paramValues.get(KnownMdmParams.SSKU_WEIGHT_GROSS).getNumeric())
            .isEqualTo(silverSsku.getBaseValue(KnownMdmParams.WEIGHT_GROSS).flatMap(MdmParamValue::getNumeric));
    }

    @Test
    public void whenHaveNoMskuUseOwnVghAsGold() {
        // given
        // supplier
        int businessId = 13;
        int serviceId = 12;
        MdmSupplier business = new MdmSupplier()
            .setId(businessId)
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier service = new MdmSupplier()
            .setId(serviceId)
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(businessId)
            .setBusinessEnabled(true);
        mdmSupplierRepository.insertOrUpdateAll(List.of(business, service));
        mdmSupplierCachingService.refresh();

        // ssku
        String shopSku = "U238";
        ShopSkuKey businessKey = new ShopSkuKey(businessId, shopSku);
        ShopSkuKey serviceKey = new ShopSkuKey(serviceId, shopSku);
        sskuExistenceRepository.markExistence(serviceKey, true);

        // mappings
        long mskuId = 0L;
        int categoryId = 111;
        mappingsCacheRepository.insertBatch(
            new MappingCacheDao().setCategoryId(categoryId)
                .setShopSkuKey(serviceKey)
                .setMskuId(mskuId),
            new MappingCacheDao().setCategoryId(categoryId)
                .setShopSkuKey(businessKey)
                .setMskuId(mskuId)
        );

        // valid silver
        SilverCommonSsku silverSsku =
            createSilverSsku(businessKey, "10", "10", "10", "1", "vasya", MasterDataSourceType.SUPPLIER, Instant.now());
        silverSskuRepository.insertOrUpdateSsku(silverSsku);

        raiseForceInheritanceSwitches(categoryId);

        // when
        sskuToRefreshProcessingService.processShopSkuKeys(List.of(businessKey));

        // then
        List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findAll();
        Assertions.assertThat(referenceItems).hasSize(1);
        ReferenceItemWrapper referenceItem = referenceItems.iterator().next();
        Assertions.assertThat(referenceItem).isNotNull();
        Assertions.assertThat(referenceItem.getKey()).isEqualTo(serviceKey);
        Assertions.assertThat(referenceItem.getCisHandleMode()).contains(MdmIrisPayload.CisHandleMode.NO_RESTRICTION);
        Assertions.assertThat(referenceItem.getSurplusHandleMode()).contains(MdmIrisPayload.SurplusHandleMode.ACCEPT);
        MdmIrisPayload.ShippingUnit shippingUnit = referenceItem.getCombinedItemShippingUnit();
        Assertions.assertThat(shippingUnit).isNotNull();
        Assertions.assertThat(shippingUnit.getLengthMicrometer().getValue())
            .isEqualTo(extractDimensionMicrometer(silverSsku, KnownMdmParams.LENGTH));
        Assertions.assertThat(shippingUnit.getWidthMicrometer().getValue())
            .isEqualTo(extractDimensionMicrometer(silverSsku, KnownMdmParams.WIDTH));
        Assertions.assertThat(shippingUnit.getHeightMicrometer().getValue())
            .isEqualTo(extractDimensionMicrometer(silverSsku, KnownMdmParams.HEIGHT));
        Assertions.assertThat(shippingUnit.getWeightGrossMg().getValue())
            .isEqualTo(extractWeightMg(silverSsku, KnownMdmParams.WEIGHT_GROSS));

        Map<ShopSkuKey, CommonSsku> goldByKeys =
            goldSskuRepository.findSskus(List.of(serviceKey, businessKey));
        Assertions.assertThat(goldByKeys).containsOnlyKeys(businessKey);
        Map<Long, SskuParamValue> paramValues = goldByKeys.get(businessKey).getBaseValuesByParamId();
        Assertions.assertThat(paramValues).hasSize(8);
        Assertions.assertThat(paramValues.get(KnownMdmParams.LENGTH).getNumeric())
            .isEqualTo(silverSsku.getBaseValue(KnownMdmParams.LENGTH).flatMap(MdmParamValue::getNumeric));
        Assertions.assertThat(paramValues.get(KnownMdmParams.WIDTH).getNumeric())
            .isEqualTo(silverSsku.getBaseValue(KnownMdmParams.WIDTH).flatMap(MdmParamValue::getNumeric));
        Assertions.assertThat(paramValues.get(KnownMdmParams.HEIGHT).getNumeric())
            .isEqualTo(silverSsku.getBaseValue(KnownMdmParams.HEIGHT).flatMap(MdmParamValue::getNumeric));
        Assertions.assertThat(paramValues.get(KnownMdmParams.WEIGHT_GROSS).getNumeric())
            .isEqualTo(silverSsku.getBaseValue(KnownMdmParams.WEIGHT_GROSS).flatMap(MdmParamValue::getNumeric));
        Assertions.assertThat(paramValues.get(KnownMdmParams.SSKU_LENGTH).getNumeric())
            .isEqualTo(silverSsku.getBaseValue(KnownMdmParams.LENGTH).flatMap(MdmParamValue::getNumeric));
        Assertions.assertThat(paramValues.get(KnownMdmParams.SSKU_WIDTH).getNumeric())
            .isEqualTo(silverSsku.getBaseValue(KnownMdmParams.WIDTH).flatMap(MdmParamValue::getNumeric));
        Assertions.assertThat(paramValues.get(KnownMdmParams.SSKU_HEIGHT).getNumeric())
            .isEqualTo(silverSsku.getBaseValue(KnownMdmParams.HEIGHT).flatMap(MdmParamValue::getNumeric));
        Assertions.assertThat(paramValues.get(KnownMdmParams.SSKU_WEIGHT_GROSS).getNumeric())
            .isEqualTo(silverSsku.getBaseValue(KnownMdmParams.WEIGHT_GROSS).flatMap(MdmParamValue::getNumeric));
    }

    @Test
    public void keepMskuInheritInMultipleComputations() {
        // given
        // supplier
        int businessId = 13;
        int serviceId = 12;
        MdmSupplier business = new MdmSupplier()
            .setId(businessId)
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier service = new MdmSupplier()
            .setId(serviceId)
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(businessId)
            .setBusinessEnabled(true);
        mdmSupplierRepository.insertOrUpdateAll(List.of(business, service));
        mdmSupplierCachingService.refresh();

        // ssku
        String shopSku = "U238";
        ShopSkuKey businessKey = new ShopSkuKey(businessId, shopSku);
        ShopSkuKey serviceKey = new ShopSkuKey(serviceId, shopSku);
        sskuExistenceRepository.markExistence(serviceKey, true);

        // mappings
        long mskuId = 2222L;
        int categoryId = 111;
        mappingsCacheRepository.insertBatch(
            new MappingCacheDao().setCategoryId(categoryId)
                .setShopSkuKey(serviceKey)
                .setMskuId(mskuId),
            new MappingCacheDao().setCategoryId(categoryId)
                .setShopSkuKey(businessKey)
                .setMskuId(mskuId)
        );

        // msku
        CommonMsku msku =
            createMsku(mskuId, "15", "15", "15", "1.5", "petya", MasterDataSourceType.SUPPLIER, Instant.now());
        mskuRepository.insertOrUpdateMsku(msku);

        raiseForceInheritanceSwitches(categoryId);

        for (int i = 0; i < 5; i++) {
            // when
            sskuToRefreshProcessingService.processShopSkuKeys(List.of(businessKey));

            // then
            List<ReferenceItemWrapper> referenceItems = referenceItemRepository.findAll();
            Assertions.assertThat(referenceItems).hasSize(1);
            ReferenceItemWrapper referenceItem = referenceItems.iterator().next();
            Assertions.assertThat(referenceItem).isNotNull();
            Assertions.assertThat(referenceItem.getKey()).isEqualTo(serviceKey);
            Assertions.assertThat(referenceItem.getCisHandleMode())
                .contains(MdmIrisPayload.CisHandleMode.NO_RESTRICTION);
            Assertions.assertThat(referenceItem.getSurplusHandleMode())
                .contains(MdmIrisPayload.SurplusHandleMode.ACCEPT);
            MdmIrisPayload.ShippingUnit shippingUnit = referenceItem.getCombinedItemShippingUnit();
            Assertions.assertThat(shippingUnit).isNotNull();
            Assertions.assertThat(shippingUnit.getLengthMicrometer().getValue())
                .isEqualTo(extractDimensionMicrometer(msku, KnownMdmParams.LENGTH));
            Assertions.assertThat(shippingUnit.getWidthMicrometer().getValue())
                .isEqualTo(extractDimensionMicrometer(msku, KnownMdmParams.WIDTH));
            Assertions.assertThat(shippingUnit.getHeightMicrometer().getValue())
                .isEqualTo(extractDimensionMicrometer(msku, KnownMdmParams.HEIGHT));
            Assertions.assertThat(shippingUnit.getWeightGrossMg().getValue())
                .isEqualTo(extractWeightMg(msku, KnownMdmParams.WEIGHT_GROSS));

            Map<ShopSkuKey, CommonSsku> goldByKeys =
                goldSskuRepository.findSskus(List.of(serviceKey, businessKey));
            Assertions.assertThat(goldByKeys).containsOnlyKeys(businessKey);
            Map<Long, SskuParamValue> paramValues = goldByKeys.get(businessKey).getBaseValuesByParamId();
            Assertions.assertThat(paramValues).hasSize(4); // no silver - no own vgh
            Assertions.assertThat(paramValues.get(KnownMdmParams.LENGTH).getNumeric())
                .isEqualTo(msku.getParamValue(KnownMdmParams.LENGTH).flatMap(MdmParamValue::getNumeric));
            Assertions.assertThat(paramValues.get(KnownMdmParams.WIDTH).getNumeric())
                .isEqualTo(msku.getParamValue(KnownMdmParams.WIDTH).flatMap(MdmParamValue::getNumeric));
            Assertions.assertThat(paramValues.get(KnownMdmParams.HEIGHT).getNumeric())
                .isEqualTo(msku.getParamValue(KnownMdmParams.HEIGHT).flatMap(MdmParamValue::getNumeric));
            Assertions.assertThat(paramValues.get(KnownMdmParams.WEIGHT_GROSS).getNumeric())
                .isEqualTo(msku.getParamValue(KnownMdmParams.WEIGHT_GROSS).flatMap(MdmParamValue::getNumeric));
        }
    }

    private long extractDimensionMicrometer(CommonMsku commonMsku, long dimensionParmaId) {
        return commonMsku.getParamValue(dimensionParmaId)
            .flatMap(MdmParamValue::getNumeric)
            .map(this::fromCmToMicrometer)
            .orElseThrow();
    }

    private long extractWeightMg(CommonMsku commonMsku, long weightParmaId) {
        return commonMsku.getParamValue(weightParmaId)
            .flatMap(MdmParamValue::getNumeric)
            .map(this::fromKgToMg)
            .orElseThrow();
    }

    private long extractDimensionMicrometer(SilverCommonSsku silverSsku, long dimensionParmaId) {
        return silverSsku.getBaseValue(dimensionParmaId)
            .flatMap(MdmParamValue::getNumeric)
            .map(this::fromCmToMicrometer)
            .orElseThrow();
    }

    private long extractWeightMg(SilverCommonSsku silverSsku, long weightParmaId) {
        return silverSsku.getBaseValue(weightParmaId)
            .flatMap(MdmParamValue::getNumeric)
            .map(this::fromKgToMg)
            .orElseThrow();
    }

    private long fromCmToMicrometer(BigDecimal value) {
        return value.movePointRight(4).longValue();
    }

    private long fromKgToMg(BigDecimal value) {
        return value.movePointRight(6).longValue();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private SilverCommonSsku createSilverSsku(ShopSkuKey shopSkuKey,
                                              String length,
                                              String width,
                                              String height,
                                              String weightGross,
                                              String sourceId,
                                              MasterDataSourceType sourceType,
                                              Instant ts) {
        SilverSskuKey silverSskuKey = new SilverSskuKey(shopSkuKey, new MasterDataSource(sourceType, sourceId));
        return new SilverCommonSsku(silverSskuKey)
            .addBaseValue(createNumericPV(KnownMdmParams.LENGTH, length, sourceId, sourceType, ts))
            .addBaseValue(createNumericPV(KnownMdmParams.WIDTH, width, sourceId, sourceType, ts))
            .addBaseValue(createNumericPV(KnownMdmParams.HEIGHT, height, sourceId, sourceType, ts))
            .addBaseValue(createNumericPV(KnownMdmParams.WEIGHT_GROSS, weightGross, sourceId, sourceType, ts));
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private CommonMsku createMsku(long mskuId,
                                  String length,
                                  String width,
                                  String height,
                                  String weightGross,
                                  String sourceId,
                                  MasterDataSourceType sourceType,
                                  Instant ts) {
        return new CommonMsku(mskuId, List.of(
            createNumericMskuPV(mskuId, KnownMdmParams.LENGTH, length, sourceId, sourceType, ts),
            createNumericMskuPV(mskuId, KnownMdmParams.WIDTH, width, sourceId, sourceType, ts),
            createNumericMskuPV(mskuId, KnownMdmParams.HEIGHT, height, sourceId, sourceType, ts),
            createNumericMskuPV(mskuId, KnownMdmParams.WEIGHT_GROSS, weightGross, sourceId, sourceType, ts)
        ));
    }

    private SskuSilverParamValue createNumericSilverPV(ShopSkuKey shopSkuKey,
                                                       long paramId,
                                                       String value,
                                                       String sourceId,
                                                       MasterDataSourceType sourceType,
                                                       Instant ts) {
        MdmParamValue commonPV = createNumericPV(paramId, value, sourceId, sourceType, ts);
        SskuSilverParamValue silverParamValue = new SskuSilverParamValue();
        commonPV.copyTo(silverParamValue);
        silverParamValue.setShopSkuKey(shopSkuKey);
        return silverParamValue;
    }

    private MskuParamValue createNumericMskuPV(long mskuId,
                                               long paramId,
                                               String value,
                                               String sourceId,
                                               MasterDataSourceType sourceType,
                                               Instant ts) {
        MdmParamValue commonPV = createNumericPV(paramId, value, sourceId, sourceType, ts);
        MskuParamValue mskuParamValue = new MskuParamValue().setMskuId(mskuId);
        commonPV.copyTo(mskuParamValue);
        return mskuParamValue;
    }

    private MdmParamValue createNumericPV(long paramId,
                                          String value,
                                          String sourceId,
                                          MasterDataSourceType sourceType,
                                          Instant ts) {
        return new MdmParamValue()
            .setMasterDataSourceId(sourceId)
            .setMasterDataSourceType(sourceType)
            .setSourceUpdatedTs(ts)
            .setUpdatedTs(ts)
            .setMdmParamId(paramId)
            .setXslName(mdmParamCache.get(paramId).getXslName())
            .setNumeric(new BigDecimal(value));
    }

    private void raiseForceInheritanceSwitches(int categoryId) {
        storageKeyValueService.putValue(
            MdmProperties.CATEGORIES_TO_WRITE_GOLD_WD_TO_SSKU_GOLD_TABLE, List.of(categoryId));
        storageKeyValueService.putValue(MdmProperties.CATEGORIES_TO_WRITE_OWN_SSKU_WD, List.of(categoryId));
        storageKeyValueService.putValue(
            MdmProperties.CATEGORIES_TO_USE_OWN_SSKU_WD_FOR_MSKU_GOLD, List.of(categoryId));
        storageKeyValueService.putValue(MdmProperties.CATEGORIES_TO_APPLY_FORCE_INHERITANCE, List.of(categoryId));
        storageKeyValueService.invalidateCache();
    }

    private Map<ShopSkuKey, List<SskuGoldenParamValue>> findAllGoldenParamValues() {
        return goldSskuRepository.findAllSskus().stream()
            .map(CommonSsku::getBaseValues)
            .flatMap(List::stream)
            .map(SskuGoldenParamValue::fromSskuParamValue)
            .collect(Collectors.groupingBy(SskuGoldenParamValue::getShopSkuKey));
    }
}
