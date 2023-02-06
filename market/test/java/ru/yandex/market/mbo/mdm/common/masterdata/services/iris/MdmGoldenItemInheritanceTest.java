package ru.yandex.market.mbo.mdm.common.masterdata.services.iris;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.WeightDimensionsSilverItemSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MasterDataGoldenItemService;
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
import ru.yandex.market.mbo.mdm.common.service.AllOkMasterDataValidator;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

/**
 * @author dmserebr
 * @date 08/04/2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MdmGoldenItemInheritanceTest extends MdmBaseDbTestClass {
    private static final ShopSkuKey SHOP_SKU_KEY_1 = new ShopSkuKey(10, "test1");
    private static final ShopSkuKey SHOP_SKU_KEY_2 = new ShopSkuKey(10, "test2");
    private static final ShopSkuKey SHOP_SKU_KEY_3 = new ShopSkuKey(11, "test3");
    private static final ShopSkuKey SHOP_SKU_KEY_4 = new ShopSkuKey(11, "test4");
    private static final long MSKU_1 = 100L;
    private static final int CATEGORY_ID = 1;

    private static final LocalDateTime VERY_LONG_AGO = LocalDateTime.of(1990, 1, 1, 0, 0, 0);
    private static final LocalDateTime LONG_AGO = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
    private static final LocalDateTime NOT_LONG_AGO = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
    private static final LocalDateTime RECENTLY = LocalDateTime.of(2020, 4, 1, 0, 0, 0);

    @Autowired
    private FromIrisItemRepository fromIrisItemRepository;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private StorageKeyValueService keyValueService;
    @Autowired
    private MdmQueuesManager mdmQueuesManager;
    @Autowired
    protected ServiceSskuConverter serviceSskuConverter;
    @Autowired
    protected MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    protected MasterDataBusinessMergeService masterDataBusinessMergeService;
    @Autowired
    private MasterDataVersionMapService masterDataVersionMapService;
    @Autowired
    private VerdictCalculationHelper verdictCalculationHelper;
    @Autowired
    private SskuGoldenParamUtil sskuGoldenParamUtil;
    @Autowired
    private GoldSskuRepository goldSskuRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private FeatureSwitchingAssistant featureSwitchingAssistant;
    @Autowired
    private SskuProcessingDataProvider sskuProcessingDataProvider;
    @Autowired
    private RslGoldenItemService rslGoldenItemService;
    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private MultivalueBusinessHelper multivalueBusinessHelper;
    @Autowired
    private MasterDataGoldenItemService masterDataGoldenItemService;
    @Autowired
    private TraceableSskuGoldenItemService traceableSskuGoldenItemService;
    @Autowired
    private WeightDimensionsValidator weightDimensionsValidator;

    private SskuToRefreshProcessingService sskuToRefreshProcessingService;

    private static MappingCacheDao createTestMapping(ShopSkuKey shopSkuKey, long mskuId) {
        MappingCacheDao item = new MappingCacheDao();
        item.setSupplierId(shopSkuKey.getSupplierId());
        item.setShopSku(shopSkuKey.getShopSku());
        item.setMskuId(mskuId);
        item.setCategoryId(CATEGORY_ID);
        return item;
    }

    private static MdmIrisPayload.Associate createMskuSource(long mskuId) {
        return MdmIrisPayload.Associate.newBuilder()
            .setId(MasterDataSourceType.MSKU_SOURCE_PREFIX + mskuId)
            .setType(MdmIrisPayload.MasterDataSource.MDM)
            .setSubtype(MasterDataSourceType.MSKU_INHERIT.name())
            .build();
    }

    private static MdmIrisPayload.Associate createWarehouseSource(String id) {
        return MdmIrisPayload.Associate.newBuilder()
            .setId(id)
            .setType(MdmIrisPayload.MasterDataSource.WAREHOUSE)
            .build();
    }

    private static MdmIrisPayload.Associate createSupplierSource(int id) {
        return MdmIrisPayload.Associate.newBuilder()
            .setId(String.valueOf(id))
            .setType(MdmIrisPayload.MasterDataSource.SUPPLIER)
            .build();
    }

    @Before
    public void before() {
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_1, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_2, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_3, MSKU_1));
        mappingsCacheRepository.insert(createTestMapping(SHOP_SKU_KEY_4, MSKU_1));
        mdmSupplierRepository.insertBatch(
            Stream.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3, SHOP_SKU_KEY_1)
                .map(ShopSkuKey::getSupplierId)
                .distinct()
                .map(id -> new MdmSupplier().setId(id).setType(MdmSupplierType.THIRD_PARTY))
                .collect(Collectors.toList())
        );

        CachedItemBlockValidationContextProvider validationContextProvider =
            new CachedItemBlockValidationContextProviderImpl(keyValueService);
        var itemBlockValidationService = new WeightDimensionBlockValidationServiceImpl(
            validationContextProvider, weightDimensionsValidator);
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

        SskuGoldenMasterDataCalculationHelper goldenMasterDataCalculator =
            new SskuGoldenMasterDataCalculationHelper(
                serviceSskuConverter,
                new MasterDataValidationService(new AllOkMasterDataValidator()),
                masterDataBusinessMergeService,
                keyValueService,
                masterDataGoldenItemService,
                sskuGoldenParamUtil,
                traceableSskuGoldenItemService,
                multivalueBusinessHelper
            );

        sskuToRefreshProcessingService = new SskuToRefreshProcessingServiceImpl(
            sskuProcessingDataProvider,
            sskuVerdictProcessor(),
            sskuProcessingPostProcessor(goldenReferenceItemCalculator),
            sskuProcessingContextProvider(),
            sskuProcessingPipeProcessor(),
            sskuProcessingPreProcessor(),
            sskuCalculatingProcessor(goldenReferenceItemCalculator, goldenMasterDataCalculator));
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
    public void testCanInherit() {
        var msku1 = new CommonMsku(
            MSKU_1,
            List.of(
                createNumericMskuParamValue(KnownMdmParams.LENGTH, MSKU_1, 10.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WIDTH, MSKU_1, 14.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.HEIGHT, MSKU_1, 30.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WEIGHT_GROSS, MSKU_1, 0.9, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WEIGHT_NET, MSKU_1, 0.8, LONG_AGO)
            )
        );
        mskuRepository.insertOrUpdateMsku(msku1);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2));

        Map<ShopSkuKey, ReferenceItemWrapper> refItems = referenceItemRepository.findAll().stream()
            .collect(Collectors.toMap(ReferenceItemWrapper::getKey, Function.identity()));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(refItems.keySet()).containsExactlyInAnyOrder(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2);

            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformationList()).hasSize(1);
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(0).getSource())
                .isEqualTo(createMskuSource(MSKU_1));
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(0).getItemShippingUnit())
                .isEqualTo(ItemWrapperTestUtil.generateShippingUnit(10.0, 14.0, 30.0, 0.9, 0.8, null, LONG_AGO)
                    .build());

            softly.assertThat(refItems.get(SHOP_SKU_KEY_2).getItem().getInformationList()).hasSize(1);
            softly.assertThat(refItems.get(SHOP_SKU_KEY_2).getItem().getInformation(0).getSource())
                .isEqualTo(createMskuSource(MSKU_1));
            softly.assertThat(refItems.get(SHOP_SKU_KEY_2).getItem().getInformation(0).getItemShippingUnit())
                .isEqualTo(ItemWrapperTestUtil.generateShippingUnit(10.0, 14.0, 30.0, 0.9, 0.8, null, LONG_AGO)
                    .build());
        });
    }

    @Test
    public void testWarehouseAndRslSskuValuesAreNotOverwritten() {
        var msku1 = new CommonMsku(
            MSKU_1,
            List.of(
                createNumericMskuParamValue(KnownMdmParams.LENGTH, MSKU_1, 10.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WIDTH, MSKU_1, 14.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.HEIGHT, MSKU_1, 30.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WEIGHT_GROSS, MSKU_1, 0.9, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WEIGHT_NET, MSKU_1, 0.8, LONG_AGO)
            )
        );
        mskuRepository.insertOrUpdateMsku(msku1);

        // ssku 1 has 2 warehouse shipping units
        var item1 = ItemWrapperTestUtil.createItem(SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145",
            ItemWrapperTestUtil.generateShippingUnit(20.0, 25.0, 30.0, 2.0, 1.5, null, NOT_LONG_AGO));
        var item2 = ItemWrapperTestUtil.createItem(SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "147",
            ItemWrapperTestUtil.generateShippingUnit(25.0, 25.0, 30.0, 2.5, 1.5, null, RECENTLY));
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item1));
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item2));

        // ssku 2 has warehouse shipping unit
        var item3 = ItemWrapperTestUtil.createItem(SHOP_SKU_KEY_2, MdmIrisPayload.MasterDataSource.WAREHOUSE, "147",
            ItemWrapperTestUtil.generateShippingUnit(10.0, 14.0, 30.0, 0.9, null, null, VERY_LONG_AGO));
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item3));

        // ssku 3 has supplier shipping unit
        var item4 = ItemWrapperTestUtil.createItem(SHOP_SKU_KEY_3, MdmIrisPayload.MasterDataSource.SUPPLIER,
            String.valueOf(SHOP_SKU_KEY_3.getSupplierId()),
            ItemWrapperTestUtil.generateShippingUnit(10.0, 14.0, 30.0, 0.9, 0.6, null, VERY_LONG_AGO));
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item4));

        // ssku 4 existing RSL item
        var rslItemBuilder = ItemWrapperTestUtil.createItem(SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.MDM,
            MasterDataSourceType.RSL_SOURCE_ID)
            .toBuilder();
        MdmIrisPayload.ReferenceInformation.Builder rslInfo = rslItemBuilder.getInformation(0).toBuilder()
            .addMinInboundLifetimeDay(MdmIrisPayload.RemainingLifetime.newBuilder().setValue(1).setStartDate(10))
            .addMinOutboundLifetimeDay(MdmIrisPayload.RemainingLifetime.newBuilder().setValue(2).setStartDate(10));
        rslItemBuilder.clearInformation().addInformation(rslInfo);
        MdmIrisPayload.Item rslItem = rslItemBuilder.build();
        referenceItemRepository.insertOrUpdate(new ReferenceItemWrapper(rslItem));

        // ssku 5 has no shipping unit (should be derived from MSKU)
        sskuToRefreshProcessingService.processShopSkuKeys(
            List.of(SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3, SHOP_SKU_KEY_4));

        Map<ShopSkuKey, ReferenceItemWrapper> refItems = referenceItemRepository.findAll().stream()
            .collect(Collectors.toMap(ReferenceItemWrapper::getKey, Function.identity()));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(refItems.keySet()).containsExactlyInAnyOrder(
                SHOP_SKU_KEY_1, SHOP_SKU_KEY_2, SHOP_SKU_KEY_3, SHOP_SKU_KEY_4);

            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformationList()).hasSize(3);
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(0).getSource())
                .isEqualTo(createWarehouseSource("147"));
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(0).getItemShippingUnit())
                .isEqualTo(ItemWrapperTestUtil.generateShippingUnit(25.0, 25.0, 30.0, 2.5, null, null, RECENTLY)
                    .build());
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(1).getSource())
                .isEqualTo(createWarehouseSource("145"));
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(1).getItemShippingUnit())
                .isEqualTo(ItemWrapperTestUtil.generateShippingUnit(null, null, null, null, 1.5, null, NOT_LONG_AGO)
                    .build());

            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(2).hasItemShippingUnit())
                .isFalse();
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(2).getMinInboundLifetimeDayCount())
                .isNotZero();
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(2).getMinOutboundLifetimeDayCount())
                .isNotZero();

            softly.assertThat(refItems.get(SHOP_SKU_KEY_2).getItem().getInformationList()).hasSize(2);
            softly.assertThat(refItems.get(SHOP_SKU_KEY_2).getItem().getInformation(0).getSource())
                .isEqualTo(createWarehouseSource("147"));
            softly.assertThat(refItems.get(SHOP_SKU_KEY_2).getItem().getInformation(0).getItemShippingUnit())
                .isEqualTo(ItemWrapperTestUtil.generateShippingUnit(10.0, 14.0, 30.0, 0.9, null, null, VERY_LONG_AGO)
                    .build());
            softly.assertThat(refItems.get(SHOP_SKU_KEY_2).getItem().getInformation(1).getSource())
                .isEqualTo(createMskuSource(MSKU_1));
            softly.assertThat(refItems.get(SHOP_SKU_KEY_2).getItem().getInformation(1).getItemShippingUnit())
                .isEqualTo(ItemWrapperTestUtil.generateShippingUnit(null, null, null, null, 0.8, null, LONG_AGO)
                    .build());

            softly.assertThat(refItems.get(SHOP_SKU_KEY_3).getItem().getInformationList()).hasSize(1);
            softly.assertThat(refItems.get(SHOP_SKU_KEY_3).getItem().getInformation(0).getSource())
                .isEqualTo(createSupplierSource(SHOP_SKU_KEY_3.getSupplierId()));
            softly.assertThat(refItems.get(SHOP_SKU_KEY_3).getItem().getInformation(0).getItemShippingUnit())
                .isEqualTo(ItemWrapperTestUtil.generateShippingUnit(10.0, 14.0, 30.0, 0.9, 0.6, null, VERY_LONG_AGO)
                    .build());

            softly.assertThat(refItems.get(SHOP_SKU_KEY_4).getItem().getInformationList()).hasSize(1);
            softly.assertThat(refItems.get(SHOP_SKU_KEY_4).getItem().getInformation(0).getSource())
                .isEqualTo(createMskuSource(MSKU_1));
            softly.assertThat(refItems.get(SHOP_SKU_KEY_4).getItem().getInformation(0).getItemShippingUnit())
                .isEqualTo(ItemWrapperTestUtil.generateShippingUnit(10.0, 14.0, 30.0, 0.9, 0.8, null, LONG_AGO)
                    .build());
        });
    }

    @Test
    public void testPreviouslyInheritedValuesAreOverwrittenWithNewInheritedValues() {
        var msku1 = new CommonMsku(
            MSKU_1,
            List.of(
                createNumericMskuParamValue(KnownMdmParams.LENGTH, MSKU_1, 10.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WIDTH, MSKU_1, 14.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.HEIGHT, MSKU_1, 30.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WEIGHT_GROSS, MSKU_1, 0.9, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WEIGHT_NET, MSKU_1, 0.8, LONG_AGO)
            )
        );
        mskuRepository.insertOrUpdateMsku(msku1);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(SHOP_SKU_KEY_1));

        var updatedMsku = new CommonMsku(
            MSKU_1,
            List.of(
                createNumericMskuParamValue(KnownMdmParams.LENGTH, MSKU_1, 12.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WIDTH, MSKU_1, 20.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.HEIGHT, MSKU_1, 30.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WEIGHT_GROSS, MSKU_1, 0.9, LONG_AGO)
            )
        );
        mskuRepository.insertOrUpdateMsku(updatedMsku);

        keyValueService.putValue(MdmProperties.APPLY_FORCE_INHERITANCE_GLOBALLY, true);
        sskuToRefreshProcessingService.processShopSkuKeys(List.of(SHOP_SKU_KEY_1));

        Map<ShopSkuKey, ReferenceItemWrapper> refItems = referenceItemRepository.findAll().stream()
            .collect(Collectors.toMap(ReferenceItemWrapper::getKey, Function.identity()));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(refItems.keySet()).containsExactly(SHOP_SKU_KEY_1);

            // newly inherited values are applied
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformationList()).hasSize(1);
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(0).getSource())
                .isEqualTo(createMskuSource(MSKU_1));
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(0).getItemShippingUnit())
                .isEqualTo(ItemWrapperTestUtil.generateShippingUnit(12.0, 20.0, 30.0, 0.9, null, null, LONG_AGO)
                    .build());
        });
    }

    @Test
    public void testInheritedValuesAreOverwrittenWithIrisValues() {
        var msku1 = new CommonMsku(
            MSKU_1,
            List.of(
                createNumericMskuParamValue(KnownMdmParams.LENGTH, MSKU_1, 10.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WIDTH, MSKU_1, 14.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.HEIGHT, MSKU_1, 30.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WEIGHT_GROSS, MSKU_1, 0.9, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WEIGHT_NET, MSKU_1, 0.8, LONG_AGO)
            )
        );
        mskuRepository.insertOrUpdateMsku(msku1);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(SHOP_SKU_KEY_1));

        var item1 = ItemWrapperTestUtil.createItem(SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145",
            ItemWrapperTestUtil.generateShippingUnit(20.0, 25.0, 30.0, 2.0, null, null, NOT_LONG_AGO));
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item1));

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(SHOP_SKU_KEY_1));

        Map<ShopSkuKey, ReferenceItemWrapper> refItems = referenceItemRepository.findAll().stream()
            .collect(Collectors.toMap(ReferenceItemWrapper::getKey, Function.identity()));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(refItems.keySet()).containsExactly(SHOP_SKU_KEY_1);

            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformationList()).hasSize(2);
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(0).getSource())
                .isEqualTo(createWarehouseSource("145"));
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(0).getItemShippingUnit())
                .isEqualTo(ItemWrapperTestUtil.generateShippingUnit(20.0, 25.0, 30.0, 2.0, null, null, NOT_LONG_AGO)
                    .build());

            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(1).getSource())
                .isEqualTo(createMskuSource(MSKU_1));
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(1).getItemShippingUnit())
                .isEqualTo(ItemWrapperTestUtil.generateShippingUnit(null, null, null, null, 0.8, null, LONG_AGO)
                    .build());
        });
    }

    @Test
    public void testRecomputedGoldNotWrittenGloballyInForceInheritanceMode() {
        var msku1 = new CommonMsku(
            MSKU_1,
            List.of(
                createNumericMskuParamValue(KnownMdmParams.LENGTH, MSKU_1, 10.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WIDTH, MSKU_1, 14.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.HEIGHT, MSKU_1, 30.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WEIGHT_GROSS, MSKU_1, 0.9, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WEIGHT_NET, MSKU_1, 0.8, LONG_AGO)
            )
        );
        mskuRepository.insertOrUpdateMsku(msku1);

        keyValueService.putValue(MdmProperties.APPLY_FORCE_INHERITANCE_GLOBALLY, true);

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(SHOP_SKU_KEY_1));

        var item1 = ItemWrapperTestUtil.createItem(SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145",
            ItemWrapperTestUtil.generateShippingUnit(20.0, 25.0, 30.0, 2.0, null, null, NOT_LONG_AGO));
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item1));

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(SHOP_SKU_KEY_1));

        Map<ShopSkuKey, ReferenceItemWrapper> refItems = referenceItemRepository.findAll().stream()
            .collect(Collectors.toMap(ReferenceItemWrapper::getKey, Function.identity()));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(refItems.keySet()).containsExactly(SHOP_SKU_KEY_1);
            // force inheritance - all values come from MSKU
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformationList()).hasSize(1);

            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(0).getSource())
                .isEqualTo(createMskuSource(MSKU_1));

            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(0).getItemShippingUnit())
                .isEqualTo(ItemWrapperTestUtil.generateShippingUnit(10.0, 14.0, 30.0, 0.9, 0.8, null, LONG_AGO)
                    .build());
        });
    }

    @Test
    public void testRecomputedGoldNotWrittenInCategoryInForceInheritanceMode() {
        var msku1 = new CommonMsku(
            MSKU_1,
            List.of(
                createNumericMskuParamValue(KnownMdmParams.LENGTH, MSKU_1, 10.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WIDTH, MSKU_1, 14.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.HEIGHT, MSKU_1, 30.0, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WEIGHT_GROSS, MSKU_1, 0.9, LONG_AGO),
                createNumericMskuParamValue(KnownMdmParams.WEIGHT_NET, MSKU_1, 0.8, LONG_AGO)
            )
        );
        mskuRepository.insertOrUpdateMsku(msku1);

        keyValueService.putValue(MdmProperties.CATEGORIES_TO_APPLY_FORCE_INHERITANCE, List.of(CATEGORY_ID));

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(SHOP_SKU_KEY_1));

        var item1 = ItemWrapperTestUtil.createItem(SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE, "145",
            ItemWrapperTestUtil.generateShippingUnit(20.0, 25.0, 30.0, 2.0, null, null, NOT_LONG_AGO));
        fromIrisItemRepository.insert(new FromIrisItemWrapper(item1));

        sskuToRefreshProcessingService.processShopSkuKeys(List.of(SHOP_SKU_KEY_1));

        Map<ShopSkuKey, ReferenceItemWrapper> refItems = referenceItemRepository.findAll().stream()
            .collect(Collectors.toMap(ReferenceItemWrapper::getKey, Function.identity()));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(refItems.keySet()).containsExactly(SHOP_SKU_KEY_1);
            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformationList()).hasSize(1);

            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(0).getSource())
                .isEqualTo(createMskuSource(MSKU_1));

            softly.assertThat(refItems.get(SHOP_SKU_KEY_1).getItem().getInformation(0).getItemShippingUnit())
                .isEqualTo(ItemWrapperTestUtil.generateShippingUnit(10.0, 14.0, 30.0, 0.9, 0.8, null, LONG_AGO)
                    .build());
        });
    }

    private MskuParamValue createNumericMskuParamValue(long mdmParamId, long mskuId, double number, LocalDateTime ts) {
        return TestMdmParamUtils.createMskuParamValue(
            mdmParamId, mskuId, null, number, null, null, MasterDataSourceType.AUTO,
            TimestampUtil.toInstant(ts));
    }
}
