package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldComputationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSilverItem;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenBlocksPostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenSplitterMerger;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuSilverItemPreProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MskuSilverItem;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MskuSilverSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistantImpl;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class MskuGoldenItemServiceDimensionTest {

    private static final long MODEL_ID = 0L;
    private static final long CATEGORY_ID = 0L;
    private static final ModelKey MODEL_KEY = new ModelKey(CATEGORY_ID, MODEL_ID);
    private static final ShopSkuKey SHOP_SKU_KEY = new ShopSkuKey(10, "test");
    private FeatureSwitchingAssistant featureSwitchingAssistant;
    private MskuGoldenItemService service;

    @Before
    public void setup() {
        MdmParamCache cache = TestMdmParamUtils.createParamCacheMock(TestMdmParamUtils.createDefaultKnownMdmParams());
        MdmLmsCargoTypeCacheMock ctCache = TestMdmParamUtils.createCargoTypeCacheMock(new ArrayList<>());
        StorageKeyValueService storageKeyValueService = new StorageKeyValueServiceMock();
        featureSwitchingAssistant = new FeatureSwitchingAssistantImpl(new StorageKeyValueServiceMock());
        CustomsCommCodeMarkupService markupService = Mockito.mock(CustomsCommCodeMarkupServiceImpl.class);
        Mockito.when(markupService.generateMskuParamValues(Mockito.anyLong(), Mockito.anyString()))
            .thenReturn(List.of());
        service = new MskuGoldenItemService(
            new MskuSilverSplitter(cache, Mockito.mock(SskuGoldenParamUtil.class)),
            new MskuGoldenSplitterMerger(cache),
            new MskuGoldenSplitterMerger(cache),
            new MskuSilverItemPreProcessor(cache, ctCache, featureSwitchingAssistant),
            featureSwitchingAssistant,
            new MskuGoldenBlocksPostProcessor(featureSwitchingAssistant, cache, markupService, storageKeyValueService),
            new WeightDimensionBlockValidationServiceImpl(
                new CachedItemBlockValidationContextProviderImpl(storageKeyValueService),
                new WeightDimensionsValidator(new CategoryCachingServiceMock())
            ),
            cache
        );
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_GOLD_MSKU_DIMENSIONS_USING_GEOMETRIC_MEAN_GLOBAL);
    }

    @Test
    public void whenNoSilverAndNoOldOperatorGoldReturnNoGold() {
        Optional<CommonMsku> newGold =
            service.calculateGoldenItem(MODEL_KEY, GoldComputationContext.EMPTY_CONTEXT, List.of(), null);
        assertThat(newGold).isEmpty();
    }

    @Test
    public void whenValidSilverIsProvidedThenGoldIsReturned() {
        List<MskuSilverItem> silverItems = generateSilverItems();
        Optional<CommonMsku> newGold =
            service.calculateGoldenItem(MODEL_KEY, GoldComputationContext.EMPTY_CONTEXT, silverItems, null);
        assertThat(newGold).isPresent();
        Set<MskuParamValue> values = newGold.get().getValues();
        assertThat(values).hasSize(6); //2 shelf lives + 4 dimensions
        Map<Long, MskuParamValue> valuesById = values.stream()
            .collect(Collectors.toMap(MskuParamValue::getMdmParamId, Function.identity()));
        // choose latest measurement
        assertThat(valuesById.get(KnownMdmParams.LENGTH).getNumeric().orElse(null)).isEqualTo(new BigDecimal(20));
        assertThat(valuesById.get(KnownMdmParams.WIDTH).getNumeric().orElse(null)).isEqualTo(new BigDecimal(25));
        assertThat(valuesById.get(KnownMdmParams.HEIGHT).getNumeric().orElse(null)).isEqualTo(new BigDecimal(18));
        assertThat(valuesById.get(KnownMdmParams.WEIGHT_GROSS).getNumeric().orElse(null))
            .isEqualTo(new BigDecimal("10"));
    }

    @Test
    public void whenClosestToMeanAlgoDisabledUseDefaultAlgo() {
        featureSwitchingAssistant
            .disableFeature(MdmProperties.COMPUTE_GOLD_MSKU_DIMENSIONS_USING_GEOMETRIC_MEAN_GLOBAL);
        List<MskuSilverItem> silverItems = generateSilverItems();
        Optional<CommonMsku> newGold =
            service.calculateGoldenItem(MODEL_KEY, GoldComputationContext.EMPTY_CONTEXT, silverItems, null);
        assertThat(newGold).isPresent();
        Set<MskuParamValue> values = newGold.get().getValues();
        assertThat(values).hasSize(6); //2 shelf lives + 4 dimensions
        Map<Long, MskuParamValue> valuesById = values.stream()
            .collect(Collectors.toMap(MskuParamValue::getMdmParamId, Function.identity()));
        // choose latest measurement, that significant differ from older
        assertThat(valuesById.get(KnownMdmParams.LENGTH).getNumeric().orElse(null)).isEqualTo(new BigDecimal(20));
        assertThat(valuesById.get(KnownMdmParams.WIDTH).getNumeric().orElse(null)).isEqualTo(new BigDecimal(25));
        assertThat(valuesById.get(KnownMdmParams.HEIGHT).getNumeric().orElse(null)).isEqualTo(new BigDecimal(18));
        assertThat(valuesById.get(KnownMdmParams.WEIGHT_GROSS).getNumeric().orElse(null)).isEqualTo(new BigDecimal(10));
    }

    private List<MskuSilverItem> generateSilverItems() {
        LocalDateTime first = LocalDateTime.of(2021, 1, 1, 0, 0);
        return Stream.of(
            generateSilverItems(10., 10., 10., 2., MdmIrisPayload.MasterDataSource.MEASUREMENT, first),
            generateSilverItems(11., 11., 11., 2.2, MdmIrisPayload.MasterDataSource.SUPPLIER, first.plusDays(1)),
            generateSilverItems(10., 12., 8., 2.1, MdmIrisPayload.MasterDataSource.MEASUREMENT, first.plusDays(123)),
            generateSilverItems(50., 10., 40., 30., MdmIrisPayload.MasterDataSource.SUPPLIER, first.plusDays(432)),
            generateSilverItems(11., 10., 10., 2.4, MdmIrisPayload.MasterDataSource.MEASUREMENT, first.plusDays(587)),
            generateSilverItems(1., 1., 1., 0.2, MdmIrisPayload.MasterDataSource.SUPPLIER, first.plusDays(601)),
            generateSilverItems(20., 25., 18., 10., MdmIrisPayload.MasterDataSource.MEASUREMENT, first.plusDays(709)),
            generateSilverItems(100., 100., 1., 1000., MdmIrisPayload.MasterDataSource.SUPPLIER, first.plusDays(845))
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private List<MskuSilverItem> generateSilverItems(Double length, Double width, Double height, Double weight,
                                                     MdmIrisPayload.MasterDataSource sourceType,
                                                     LocalDateTime timestamp) {
        MasterData masterData = new MasterData();
        masterData.setShelfLife(TimeInUnits.UNLIMITED);

        ReferenceItemWrapper itemWrapper = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY, sourceType,
            ItemWrapperTestUtil.generateShippingUnit(length, width, height, weight, null, null, timestamp)));

        return List.of(new MasterDataSilverItem(masterData), itemWrapper);
    }
}
