package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldComputationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenBlocksPostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenSplitterMerger;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuSilverItemPreProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MskuSilverSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.priceinfo.PriceInfo;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistantImpl;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class MskuGoldenItemServicePricesTest {
    private static final long MODEL_ID = 12345L;
    private static final long CATEGORY_ID = 67890L;
    private static final ModelKey MODEL_KEY = new ModelKey(CATEGORY_ID, MODEL_ID);
    private static final ShopSkuKey SHOP_SKU_KEY_1 = new ShopSkuKey(10, "ssku1");
    private static final ShopSkuKey SHOP_SKU_KEY_2 = new ShopSkuKey(11, "ssku2");
    private static final ShopSkuKey SHOP_SKU_KEY_3 = new ShopSkuKey(12, "ssku3");

    private MskuGoldenItemService service;
    private FeatureSwitchingAssistant featureSwitchingAssistant;
    private StorageKeyValueService keyValueService;

    @Before
    public void setup() {
        MdmParamCache cache = TestMdmParamUtils.createParamCacheMock();
        MdmLmsCargoTypeCacheMock ctCache = TestMdmParamUtils.createCargoTypeCacheMock(new ArrayList<>());

        CustomsCommCodeMarkupService markupService = Mockito.mock(CustomsCommCodeMarkupServiceImpl.class);
        Mockito.when(markupService.generateMskuParamValues(Mockito.anyLong(), Mockito.anyString()))
            .thenReturn(List.of());

        keyValueService = new StorageKeyValueServiceMock();
        featureSwitchingAssistant = new FeatureSwitchingAssistantImpl(keyValueService);

        service = new MskuGoldenItemService(
            new MskuSilverSplitter(cache, Mockito.mock(SskuGoldenParamUtil.class)),
            new MskuGoldenSplitterMerger(cache),
            new MskuGoldenSplitterMerger(cache),
            new MskuSilverItemPreProcessor(cache, ctCache, featureSwitchingAssistant),
            featureSwitchingAssistant,
            new MskuGoldenBlocksPostProcessor(featureSwitchingAssistant, cache, markupService, keyValueService),
            new WeightDimensionBlockValidationServiceImpl(
                new CachedItemBlockValidationContextProviderImpl(keyValueService),
                new WeightDimensionsValidator(new CategoryCachingServiceMock())
            ),
            cache
        );
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_PRECIOUS_GOOD);
    }

    @Test
    public void whenComputedPreciousGoodIsTrueCreateAppropriateParamValue() {
        ReferenceItemWrapper itemWrapper = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null)));
        PriceInfo priceInfo1 =
            new PriceInfo(SHOP_SKU_KEY_1.getShopSku(), SHOP_SKU_KEY_1.getSupplierId(), 3500, LocalDate.now());
        PriceInfo priceInfo2 =
            new PriceInfo(SHOP_SKU_KEY_2.getShopSku(), SHOP_SKU_KEY_2.getSupplierId(), 2800, LocalDate.now());
        PriceInfo priceInfo3 =
            new PriceInfo(SHOP_SKU_KEY_3.getShopSku(), SHOP_SKU_KEY_3.getSupplierId(), 3800, LocalDate.now());

        Optional<CommonMsku> result = service.calculateGoldenItem(MODEL_KEY, GoldComputationContext.EMPTY_CONTEXT,
            List.of(itemWrapper, priceInfo1, priceInfo2, priceInfo3), null);

        Assertions.assertThat(result).isPresent();

        Map<Long, MskuParamValue> paramValues = result.get().getParamValues();

        Assertions.assertThat(paramValues).hasSize(6); // 4 ВГХ + цена + ценное

        BigDecimal length = paramValues.get(KnownMdmParams.LENGTH).getNumeric().orElse(null);
        Assertions.assertThat(length).isEqualByComparingTo(BigDecimal.valueOf(10.0));
        BigDecimal width = paramValues.get(KnownMdmParams.WIDTH).getNumeric().orElse(null);
        Assertions.assertThat(width).isEqualByComparingTo(BigDecimal.valueOf(15.0));
        BigDecimal height = paramValues.get(KnownMdmParams.HEIGHT).getNumeric().orElse(null);
        Assertions.assertThat(height).isEqualByComparingTo(BigDecimal.valueOf(20.0));
        BigDecimal weight = paramValues.get(KnownMdmParams.WEIGHT_GROSS).getNumeric().orElse(null);
        Assertions.assertThat(weight).isEqualByComparingTo(BigDecimal.valueOf(1.0));
        BigDecimal price = paramValues.get(KnownMdmParams.PRICE).getNumeric().orElse(null);
        Assertions.assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(3500));
        Boolean preciousGood = paramValues.get(KnownMdmParams.PRECIOUS_GOOD).getBool().orElse(null);
        Assertions.assertThat(preciousGood).isTrue();
    }

    @Test
    public void whenComputedPreciousGoodIsFalseCreateAppropriateParamValue() {
        ReferenceItemWrapper itemWrapper = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null)));
        PriceInfo priceInfo1 =
            new PriceInfo(SHOP_SKU_KEY_1.getShopSku(), SHOP_SKU_KEY_1.getSupplierId(), 3500, LocalDate.now());
        PriceInfo priceInfo2 =
            new PriceInfo(SHOP_SKU_KEY_2.getShopSku(), SHOP_SKU_KEY_2.getSupplierId(), 2100, LocalDate.now());

        Optional<CommonMsku> result = service.calculateGoldenItem(MODEL_KEY, GoldComputationContext.EMPTY_CONTEXT,
            List.of(itemWrapper, priceInfo1, priceInfo2), null);

        Assertions.assertThat(result).isPresent();

        Map<Long, MskuParamValue> paramValues = result.get().getParamValues();

        Assertions.assertThat(paramValues).hasSize(6); // 4 ВГХ + цена + ценное

        BigDecimal length = paramValues.get(KnownMdmParams.LENGTH).getNumeric().orElse(null);
        Assertions.assertThat(length).isEqualByComparingTo(BigDecimal.valueOf(10.0));
        BigDecimal width = paramValues.get(KnownMdmParams.WIDTH).getNumeric().orElse(null);
        Assertions.assertThat(width).isEqualByComparingTo(BigDecimal.valueOf(15.0));
        BigDecimal height = paramValues.get(KnownMdmParams.HEIGHT).getNumeric().orElse(null);
        Assertions.assertThat(height).isEqualByComparingTo(BigDecimal.valueOf(20.0));
        BigDecimal weight = paramValues.get(KnownMdmParams.WEIGHT_GROSS).getNumeric().orElse(null);
        Assertions.assertThat(weight).isEqualByComparingTo(BigDecimal.valueOf(1.0));
        BigDecimal price = paramValues.get(KnownMdmParams.PRICE).getNumeric().orElse(null);
        Assertions.assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(2800));
        Boolean preciousGood = paramValues.get(KnownMdmParams.PRECIOUS_GOOD).getBool().orElse(null);
        Assertions.assertThat(preciousGood).isFalse();
    }
}
