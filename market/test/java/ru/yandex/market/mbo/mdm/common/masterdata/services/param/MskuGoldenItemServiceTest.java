package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.GoldComputationContext;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSilverItem;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenBlocksPostProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuGoldenSplitterMerger;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.msku.MskuSilverItemPreProcessor;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MskuSilverItem;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MskuSilverSplitter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.CachedItemBlockValidationContextProviderImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistant;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistantImpl;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits.TimeUnit;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class MskuGoldenItemServiceTest extends MdmBaseDbTestClass {

    private static final long MODEL_ID = 0L;
    private static final long CATEGORY_ID = 0L;
    private static final ModelKey MODEL_KEY = new ModelKey(CATEGORY_ID, MODEL_ID);
    private static final ShopSkuKey SHOP_SKU_KEY_1 = new ShopSkuKey(10, "test");
    private static final ShopSkuKey SHOP_SKU_KEY_2 = new ShopSkuKey(10, "test2");
    private static final ShopSkuKey SHOP_SKU_KEY_3 = new ShopSkuKey(10, "test3");
    private FeatureSwitchingAssistant featureSwitchingAssistant;
    private MskuGoldenItemService service;

    @Autowired
    private MdmParamCache cache;
    @Autowired
    private MdmLmsCargoTypeCache ctCache;
    @Autowired
    private SskuGoldenParamUtil sskuGoldenParamUtil;
    @Autowired
    private WeightDimensionsValidator weightDimensionsValidator;

    @Before
    public void setup() {
        StorageKeyValueService keyValueService = new StorageKeyValueServiceMock();
        featureSwitchingAssistant = new FeatureSwitchingAssistantImpl(keyValueService);
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_HEAVY_GOOD_IN_MSKU_GOLDEN_SERVICE_KEY);
        featureSwitchingAssistant.enableFeature(MdmProperties.HIDE_TIMES_PARAMS_CREATION_ENABLED);
        CustomsCommCodeMarkupService markupService = Mockito.mock(CustomsCommCodeMarkupServiceImpl.class);
        Mockito.when(markupService.generateMskuParamValues(Mockito.anyLong(), Mockito.anyString()))
            .thenReturn(List.of());
        service = new MskuGoldenItemService(
            new MskuSilverSplitter(cache, sskuGoldenParamUtil),
            new MskuGoldenSplitterMerger(cache),
            new MskuGoldenSplitterMerger(cache),
            new MskuSilverItemPreProcessor(cache, ctCache, featureSwitchingAssistant),
            featureSwitchingAssistant,
            new MskuGoldenBlocksPostProcessor(featureSwitchingAssistant, cache, markupService, keyValueService),
            new WeightDimensionBlockValidationServiceImpl(
                new CachedItemBlockValidationContextProviderImpl(keyValueService),
                weightDimensionsValidator
            ),
            cache
        );
    }

    @Test
    public void whenNoSilverThenNoGold() {
        // or it create hide time params
        featureSwitchingAssistant.disableFeature(MdmProperties.HIDE_TIMES_PARAMS_CREATION_ENABLED);
        Optional<CommonMsku> goldIngot =
            service.calculateGoldenItem(MODEL_KEY, GoldComputationContext.EMPTY_CONTEXT, List.of(), null);
        assertThat(goldIngot).isEmpty();
    }

    @Test
    public void whenValidSilverIsProvidedThenGoldIsReturned() {
        TimeInUnits shelfLife = new TimeInUnits(10, TimeUnit.DAY);
        String shelfLifeComment = "Хранить во влажном, тёплом, доступном для детей месте";
        TimeInUnits lifeTime = new TimeInUnits(15, TimeUnit.WEEK);
        String lifeTimeComment = "15 недель, но Вам надоест раньше";
        TimeInUnits guaranteePeriod = new TimeInUnits(20, TimeUnit.WEEK);
        String guaranteePeriodComment = "Так-то, конечно, 20 недель, но менять/чинить мы вам это всё равно не будем";
        VatRate vatRate = VatRate.VAT_18_118;
        boolean shelfLifeRequired = true;
        String customsCommCode = "8509800000";

        MasterData masterData = new MasterData();
        masterData.setShelfLife(shelfLife);
        masterData.setShelfLifeComment(shelfLifeComment);
        masterData.setLifeTime(lifeTime);
        masterData.setLifeTimeComment(lifeTimeComment);
        masterData.setGuaranteePeriod(guaranteePeriod);
        masterData.setGuaranteePeriodComment(guaranteePeriodComment);
        masterData.setVat(vatRate); // ignored (now we are not computing this param for msku)
        masterData.setShelfLifeRequired(shelfLifeRequired);
        masterData.setCustomsCommodityCode(customsCommCode);

        ReferenceItemWrapper itemWrapper = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, 0.8, null)));

        MasterDataSilverItem masterDataSilverItem = new MasterDataSilverItem(masterData);

        GoldComputationContext goldComputationContext = new GoldComputationContext(
            0L,
            List.of(),
            Map.of(
                KnownMdmMboParams.LIFE_TIME_USE_PARAM_ID, true,
                KnownMdmMboParams.GUARANTEE_PERIOD_USE_PARAM_ID, true
            ),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Set.of()
        );

        Optional<CommonMsku> goldIngot = service.calculateGoldenItem(MODEL_KEY,
            goldComputationContext, List.of(itemWrapper, masterDataSilverItem), null);

        assertThat(goldIngot).isPresent();
        Set<MskuParamValue> values = goldIngot.get().getValues();

        // 3 shelfLife, 3 lifeTime, 3 guaranteePeriod, 1 customs comm code, 2 heavy good,
        // 5 weight / dimensions, 3 hide shelf/life/guarantee time/period
        assertThat(values).hasSize(3 + 3 + 3 + 1 + 2 + 5 + 3);
        Map<Long, MskuParamValue> valuesById = values.stream()
            .collect(Collectors.toMap(MskuParamValue::getMdmParamId, Function.identity()));

        assertThat(valuesById.get(KnownMdmParams.SHELF_LIFE).getNumeric().get())
            .isEqualTo(BigDecimal.valueOf(shelfLife.getTime()));
        assertThat(valuesById.get(KnownMdmParams.SHELF_LIFE_UNIT).getOption().get().getId())
            .isEqualTo(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(shelfLife.getUnit()));
        assertThat(valuesById.get(KnownMdmParams.SHELF_LIFE_COMMENT).getString().get())
            .isEqualTo(shelfLifeComment);

        assertThat(valuesById.get(KnownMdmParams.LIFE_TIME).getString().get())
            .isEqualTo(String.valueOf(lifeTime.getTime()));
        assertThat(valuesById.get(KnownMdmParams.LIFE_TIME_UNIT).getOption().get().getId())
            .isEqualTo(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(lifeTime.getUnit()));
        assertThat(valuesById.get(KnownMdmParams.LIFE_TIME_COMMENT).getString().get())
            .isEqualTo(lifeTimeComment);

        assertThat(valuesById.get(KnownMdmParams.GUARANTEE_PERIOD).getString().get())
            .isEqualTo(String.valueOf(guaranteePeriod.getTime()));
        assertThat(valuesById.get(KnownMdmParams.GUARANTEE_PERIOD_UNIT).getOption().get().getId())
            .isEqualTo(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(guaranteePeriod.getUnit()));
        assertThat(valuesById.get(KnownMdmParams.GUARANTEE_PERIOD_COMMENT).getString().get())
            .isEqualTo(guaranteePeriodComment);
        assertThat(valuesById.get(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID).getString().get())
            .isEqualTo(customsCommCode);

        assertThat(valuesById.get(KnownMdmParams.LENGTH).getNumeric().get()).isEqualTo(new BigDecimal(10));
        assertThat(valuesById.get(KnownMdmParams.WIDTH).getNumeric().get()).isEqualTo(new BigDecimal(15));
        assertThat(valuesById.get(KnownMdmParams.HEIGHT).getNumeric().get()).isEqualTo(new BigDecimal(20));
        assertThat(valuesById.get(KnownMdmParams.WEIGHT_GROSS).getNumeric().get()).isEqualTo(new BigDecimal(1));
        assertThat(valuesById.get(KnownMdmParams.WEIGHT_NET).getNumeric().get()).isEqualTo(new BigDecimal("0.8"));

        //all values are valid, so hide nothing
        assertThat(valuesById.get(KnownMdmParams.HIDE_SHELF_LIFE).getBool().orElse(null)).isEqualTo(false);
        assertThat(valuesById.get(KnownMdmParams.HIDE_LIFE_TIME).getBool().orElse(null)).isEqualTo(false);
        assertThat(valuesById.get(KnownMdmParams.HIDE_GUARANTEE_PERIOD).getBool().orElse(null)).isEqualTo(false);
    }

    @Test
    public void whenSeveralSilversThenMergeLogicIsApplied() {
        TimeInUnits earliestShelfLife = new TimeInUnits(1, TimeUnit.DAY);
        TimeInUnits latestShelfLife = new TimeInUnits(2, TimeUnit.WEEK);
        String mostCommonComment = "Хранить 1 раз";
        String someOtherComment = "Хранить 2 раза";
        boolean mostCommonExpireApply = true;
        boolean someOtherExpireApply = false;
        MasterData masterData1 = new MasterData();
        MasterData masterData2 = new MasterData();
        MasterData masterData3 = new MasterData();
        MasterData masterData4 = new MasterData();
        MasterData masterData5 = new MasterData();

        Instant ts = Instant.now();

        ReferenceItemWrapper itemWrapper1 = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, null, null, 1L)));
        itemWrapper1.setWeightDimensionsUpdatedTs(ts);

        ReferenceItemWrapper itemWrapper2 = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_2, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 30.0, 1.0, 0.8, null, 2L)));
        itemWrapper2.setWeightDimensionsUpdatedTs(ts.plusSeconds(600));

        // less than 10 percent diff to itemWrapper2
        ReferenceItemWrapper itemWrapper3 = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_3, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 31.0, 1.0, null, null, 3L)));
        itemWrapper3.setWeightDimensionsUpdatedTs(ts.plusSeconds(1200));

        masterData1.setShelfLife(earliestShelfLife);
        masterData1.setModifiedTimestamp(DateTimeUtils.dateTimeNow().plusMinutes(1));
        masterData1.setShelfLifeRequired(someOtherExpireApply);

        masterData2.setShelfLife(latestShelfLife);
        masterData2.setModifiedTimestamp(DateTimeUtils.dateTimeNow().plusMinutes(2));
        masterData2.setShelfLifeRequired(mostCommonExpireApply);

        masterData3.setShelfLifeComment(mostCommonComment);
        masterData3.setModifiedTimestamp(DateTimeUtils.dateTimeNow().minusMinutes(3));
        masterData3.setShelfLifeRequired(mostCommonExpireApply);

        masterData4.setShelfLifeComment(mostCommonComment);
        masterData4.setModifiedTimestamp(DateTimeUtils.dateTimeNow().minusMinutes(4));
        masterData4.setShelfLifeRequired(mostCommonExpireApply);

        masterData5.setShelfLifeComment(someOtherComment);
        masterData5.setModifiedTimestamp(DateTimeUtils.dateTimeNow().minusMinutes(5));
        masterData5.setShelfLifeRequired(someOtherExpireApply);

        List<MskuSilverItem> silver = List.of(
            new MasterDataSilverItem(masterData1),
            new MasterDataSilverItem(masterData2),
            new MasterDataSilverItem(masterData3),
            new MasterDataSilverItem(masterData4),
            new MasterDataSilverItem(masterData5),
            itemWrapper1,
            itemWrapper2,
            itemWrapper3
        );

        Optional<CommonMsku> goldIngot =
            service.calculateGoldenItem(MODEL_KEY, GoldComputationContext.EMPTY_CONTEXT, silver, null);

        assertThat(goldIngot).isPresent();
        Set<MskuParamValue> values = goldIngot.get().getValues();

        assertThat(values).hasSize(12);
        Map<Long, MskuParamValue> valuesById = values.stream()
            .collect(Collectors.toMap(MskuParamValue::getMdmParamId, Function.identity()));

        assertThat(valuesById.get(KnownMdmParams.SHELF_LIFE).getNumeric().get())
            .isEqualTo(BigDecimal.valueOf(latestShelfLife.getTime()));
        assertThat(valuesById.get(KnownMdmParams.SHELF_LIFE_UNIT).getOption().get().getId())
            .isEqualTo(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(latestShelfLife.getUnit()));
        // блоки с комментом проиграли гонку по таймштампам и не попали в золото.
        assertThat(valuesById.get(KnownMdmParams.SHELF_LIFE_COMMENT)).isNull();

        assertThat(valuesById.get(KnownMdmParams.LENGTH).getNumeric().get()).isEqualTo(new BigDecimal(10));
        assertThat(valuesById.get(KnownMdmParams.WIDTH).getNumeric().get()).isEqualTo(new BigDecimal(15));
        assertThat(valuesById.get(KnownMdmParams.HEIGHT).getNumeric().get()).isEqualTo(new BigDecimal(30));
        assertThat(valuesById.get(KnownMdmParams.WEIGHT_GROSS).getNumeric().get()).isEqualTo(new BigDecimal(1));
        assertThat(valuesById.get(KnownMdmParams.WEIGHT_NET).getNumeric().get()).isEqualTo(new BigDecimal("0.8"));

        //hide shelf life, life time, guarantee period params will be generated
        //have valid shelf life so not hide it
        assertThat(valuesById.get(KnownMdmParams.HIDE_SHELF_LIFE).getBool().orElse(null)).isEqualTo(false);
        //don't have valid life time and guarantee period so hide it
        assertThat(valuesById.get(KnownMdmParams.HIDE_LIFE_TIME).getBool().orElse(null)).isEqualTo(true);
        assertThat(valuesById.get(KnownMdmParams.HIDE_GUARANTEE_PERIOD).getBool().orElse(null)).isEqualTo(true);
    }

    @Test
    public void whenHaveHideTimesMskuParamValuesByOperatorsShouldMoveThemToNewGold() {
        TimeInUnits shelfLife = new TimeInUnits(10, TimeUnit.DAY);
        String shelfLifeComment = "Хранить во влажном, тёплом, доступном для детей месте";
        TimeInUnits lifeTime = new TimeInUnits(15, TimeUnit.WEEK);
        String lifeTimeComment = "15 недель, но Вам надоест раньше";
        TimeInUnits guaranteePeriod = new TimeInUnits(20, TimeUnit.WEEK);
        String guaranteePeriodComment = "Так-то, конечно, 20 недель, но менять/чинить мы вам это всё равно не будем";

        MasterData masterData = new MasterData();
        masterData.setShelfLife(shelfLife);
        masterData.setShelfLifeComment(shelfLifeComment);
        masterData.setLifeTime(lifeTime);
        masterData.setLifeTimeComment(lifeTimeComment);
        masterData.setGuaranteePeriod(guaranteePeriod);
        masterData.setGuaranteePeriodComment(guaranteePeriodComment);

        ReferenceItemWrapper itemWrapper = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, 0.8, null)));

        MskuParamValue hideShelfLife = new MskuParamValue();
        hideShelfLife
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
            .setMdmParamId(KnownMdmParams.HIDE_SHELF_LIFE)
            .setBool(true);
        MskuParamValue hideLifeTime = new MskuParamValue();
        hideLifeTime
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
            .setMdmParamId(KnownMdmParams.HIDE_LIFE_TIME)
            .setBool(true);

        Optional<CommonMsku> newGoldenItem =
            service.calculateGoldenItem(MODEL_KEY, GoldComputationContext.EMPTY_CONTEXT,
                List.of(new MasterDataSilverItem(masterData), itemWrapper),
                new CommonMsku(MODEL_KEY, List.of(hideShelfLife, hideLifeTime))
            );

        assertThat(newGoldenItem).isPresent();
        Map<Long, MskuParamValue> valuesById = newGoldenItem.get().getValues().stream()
            .collect(Collectors.toMap(MskuParamValue::getMdmParamId, Function.identity()));

        assertThat(valuesById.get(KnownMdmParams.HIDE_SHELF_LIFE).getBool().orElse(null)).isEqualTo(true);
        assertThat(valuesById.get(KnownMdmParams.HIDE_LIFE_TIME).getBool().orElse(null)).isEqualTo(true);
        assertThat(valuesById.get(KnownMdmParams.HIDE_GUARANTEE_PERIOD).getBool().orElse(null)).isEqualTo(false);
    }

    @Test
    public void whenHaveHideTimesMskuParamValuesByMboOperatorsShouldMoveThemToNewGold() {
        // given
        TimeInUnits shelfLife = new TimeInUnits(10, TimeUnit.DAY);
        String shelfLifeComment = "Хранить во влажном, тёплом, доступном для детей месте";
        TimeInUnits lifeTime = new TimeInUnits(15, TimeUnit.WEEK);
        String lifeTimeComment = "15 недель, но Вам надоест раньше";
        TimeInUnits guaranteePeriod = new TimeInUnits(20, TimeUnit.WEEK);
        String guaranteePeriodComment = "Так-то, конечно, 20 недель, но менять/чинить мы вам это всё равно не будем";

        MasterData masterData = new MasterData();
        masterData.setShelfLife(shelfLife);
        masterData.setShelfLifeComment(shelfLifeComment);
        masterData.setLifeTime(lifeTime);
        masterData.setLifeTimeComment(lifeTimeComment);
        masterData.setGuaranteePeriod(guaranteePeriod);
        masterData.setGuaranteePeriodComment(guaranteePeriodComment);

        ReferenceItemWrapper itemWrapper = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            SHOP_SKU_KEY_1, MdmIrisPayload.MasterDataSource.WAREHOUSE,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 15.0, 20.0, 1.0, 0.8, null)));

        CommonMsku existingMsku = new CommonMsku(
            MODEL_KEY,
            List.of(
                (MskuParamValue) new MskuParamValue()
                    .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
                    .setMdmParamId(KnownMdmParams.HIDE_SHELF_LIFE)
                    .setBool(true),
                (MskuParamValue) new MskuParamValue()
                    .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
                    .setMdmParamId(KnownMdmParams.HIDE_LIFE_TIME)
                    .setBool(true)
            )
        );

        // when
        CommonMsku calculationResult = service.calculateGoldenItem(
            MODEL_KEY,
                GoldComputationContext.EMPTY_CONTEXT,
                List.of(new MasterDataSilverItem(masterData), itemWrapper),
                existingMsku
            ).orElseThrow();

        // then
        assertThat(calculationResult.getParamValue(KnownMdmParams.HIDE_SHELF_LIFE))
            .isEqualTo(existingMsku.getParamValue(KnownMdmParams.HIDE_SHELF_LIFE));
        assertThat(calculationResult.getParamValue(KnownMdmParams.HIDE_LIFE_TIME))
            .isEqualTo(existingMsku.getParamValue(KnownMdmParams.HIDE_LIFE_TIME));
        assertThat(calculationResult.getParamValue(KnownMdmParams.HIDE_GUARANTEE_PERIOD))
            .flatMap(MdmParamValue::getBool)
            .contains(false);
    }

    @Test
    public void whenOldGoldHasOperatorAndNewGoldDoesntShouldRetainOldValue() {
        // Нарочно делаем значения из разных МДшек одинаковыми. В нормальной ситуации они должны попасть в золото, как
        // самые частые, но ниже мы создадим операторские параметры, которые это передоминируют.
        TimeInUnits shelfLife1 = new TimeInUnits(10, TimeUnit.DAY);
        String shelfLifeComment1 = "Хранить во влажном, тёплом, доступном для детей месте";

        TimeInUnits shelfLife2 = new TimeInUnits(10, TimeUnit.DAY);
        String shelfLifeComment2 = "Хранить во влажном, тёплом, доступном для детей месте";

        MasterData md1 = new MasterData();
        md1.setShelfLife(shelfLife1);
        md1.setShelfLifeComment(shelfLifeComment1);
        md1.setModifiedTimestamp(LocalDateTime.now());

        MasterData md2 = new MasterData();
        md2.setShelfLife(shelfLife2);
        md2.setShelfLifeComment(shelfLifeComment2);
        md2.setModifiedTimestamp(LocalDateTime.now());

        // Параметры сущ-го золота: делаем их максимально несостоятельными. Пусть значение будет наименее популярное из
        // трёх и будет иметь самый поздний таймштамп. По всем законам жанра такой параметр не должен попасть в золото.
        // Но он имеет операторский источник и это сыграет решающую роль.
        MskuParamValue goldShelfLife = new MskuParamValue();
        goldShelfLife
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
            .setSourceUpdatedTs(Instant.now().plusSeconds(100500))
            .setMdmParamId(KnownMdmParams.SHELF_LIFE)
            .setNumeric(BigDecimal.valueOf(20));
        MskuParamValue goldShelfLifeUnit = new MskuParamValue();
        goldShelfLifeUnit
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
            .setSourceUpdatedTs(Instant.now().plusSeconds(100500))
            .setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT)
            .setOption(new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeUnit.WEEK)));
        MskuParamValue goldShelfLifeComment = new MskuParamValue();
        goldShelfLifeComment
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
            .setSourceUpdatedTs(Instant.now().plusSeconds(100500))
            .setMdmParamId(KnownMdmParams.SHELF_LIFE_COMMENT)
            .setString("Хранить в недоступном для взрослых месте");
        // И карготип на десерт докинем, которого в серебре вообще нет
        MskuParamValue goldCargotype  = new MskuParamValue();
        goldCargotype
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
            .setSourceUpdatedTs(Instant.now().plusSeconds(100500))
            .setMdmParamId(KnownMdmParams.DANGEROUS_AIR_410)
            .setBool(true);

        Optional<CommonMsku> newGoldenItem =
            service.calculateGoldenItem(MODEL_KEY, GoldComputationContext.EMPTY_CONTEXT,
                List.of(
                    new MasterDataSilverItem(md1), new MasterDataSilverItem(md2)
                ), new CommonMsku(MODEL_KEY,
                    List.of(goldShelfLife, goldShelfLifeUnit, goldShelfLifeComment, goldCargotype))
            );

        assertThat(newGoldenItem).isPresent();
        Map<Long, MskuParamValue> valuesById = newGoldenItem.get().getValues().stream()
            .collect(Collectors.toMap(MskuParamValue::getMdmParamId, Function.identity()));

        // Старое операторское значение победило, не смотря на то, что оно "плохое" по всем остальным правилам
        // схлопывания.
        assertThat(valuesById.get(KnownMdmParams.SHELF_LIFE).getNumeric().orElse(null))
            .isEqualTo(BigDecimal.valueOf(20));
        assertThat(valuesById.get(KnownMdmParams.SHELF_LIFE_UNIT).getOption().orElse(null))
            .isEqualTo(new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeUnit.WEEK)));
        assertThat(valuesById.get(KnownMdmParams.SHELF_LIFE_COMMENT).getString().orElse(null))
            .isEqualTo("Хранить в недоступном для взрослых месте");
        assertThat(valuesById.get(KnownMdmParams.DANGEROUS_AIR_410).getBool().orElse(null))
            .isEqualTo(true);
    }

    @Test
    public void whenOldGoldHasPriorityButIsNotManualAndNewGoldIsAnythingShouldPickNewOne() {
        // Если в старом золоте !не операторские! изменения, то старое золото не участвует в фестивале, даже если
        // оно более приоритетное.
        TimeInUnits shelfLife1 = new TimeInUnits(10, TimeUnit.DAY);
        String shelfLifeComment1 = "Хранить во влажном, тёплом, доступном для детей месте";

        MasterData md1 = new MasterData();
        md1.setShelfLife(shelfLife1);
        md1.setShelfLifeComment(shelfLifeComment1);
        md1.setModifiedTimestamp(LocalDateTime.now());

        // Параметры сущ-го золота: делаем их максимально состоятельными. Пусть значение будет складское. Тем не менее,
        // это не спасёт от перетирания, т.к. такого складского значения больше нет в серебре,
        // и этого замера больше не существует.
        MskuParamValue goldShelfLife = new MskuParamValue();
        goldShelfLife
            .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT)
            .setSourceUpdatedTs(Instant.now().plusSeconds(100500))
            .setMdmParamId(KnownMdmParams.SHELF_LIFE)
            .setNumeric(BigDecimal.valueOf(20));
        MskuParamValue goldShelfLifeUnit = new MskuParamValue();
        goldShelfLifeUnit
            .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT)
            .setSourceUpdatedTs(Instant.now().plusSeconds(100500))
            .setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT)
            .setOption(new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeUnit.WEEK)));
        MskuParamValue goldShelfLifeComment = new MskuParamValue();
        goldShelfLifeComment
            .setMasterDataSourceType(MasterDataSourceType.MEASUREMENT)
            .setSourceUpdatedTs(Instant.now().plusSeconds(100500))
            .setMdmParamId(KnownMdmParams.SHELF_LIFE_COMMENT)
            .setString("Хранить в недоступном для взрослых месте");

        Optional<CommonMsku> newGoldenItem =
            service.calculateGoldenItem(MODEL_KEY, GoldComputationContext.EMPTY_CONTEXT,
                List.of(new MasterDataSilverItem(md1)),
                new CommonMsku(MODEL_KEY, List.of(goldShelfLife, goldShelfLifeUnit, goldShelfLifeComment))
            );

        assertThat(newGoldenItem).isPresent();
        Map<Long, MskuParamValue> valuesById = newGoldenItem.get().getValues().stream()
            .collect(Collectors.toMap(MskuParamValue::getMdmParamId, Function.identity()));

        // Новое значение победило, не смотря на то, что оно менее приоритетное, чем источник существующего золота.
        // Строго говоря, оно и не "сражалось", т.к. в этом случае !не операторское! старое золото вообще не участвует.
        assertThat(valuesById.get(KnownMdmParams.SHELF_LIFE).getNumeric().orElse(null))
            .isEqualTo(BigDecimal.valueOf(10));
        assertThat(valuesById.get(KnownMdmParams.SHELF_LIFE_UNIT).getOption().orElse(null))
            .isEqualTo(new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeUnit.DAY)));
        assertThat(valuesById.get(KnownMdmParams.SHELF_LIFE_COMMENT).getString().orElse(null))
            .isEqualTo("Хранить во влажном, тёплом, доступном для детей месте");
    }

    @Test
    @Ignore
    public void whenOldGoldHasOperatorAndNewGoldTooShouldPickNewOperatorValue() {
        // TODO: сценарий, когда в серебре поступают операторские значения.
        // Сейчас такой сценарий невозможен, т.к. операторские значения сохраняются сразу в золото MSKU.
    }

    @Test
    @Ignore
    public void whenOldGoldHasAutoValueAndNewGoldHasOperatorShouldPickNewValue() {
        // TODO: сценарий, когда в серебре поступают операторские значения.
        // Сейчас такой сценарий невозможен, т.к. операторские значения сохраняются сразу в золото MSKU.
    }

    @Test
    public void whenGoldHasOperatorChangesShouldNotEraseModificationInfoAfterCalculation() {
        List<List<Long>> paramIds = List.of(
            List.of(
                KnownMdmParams.SHELF_LIFE,
                KnownMdmParams.SHELF_LIFE_UNIT,
                KnownMdmParams.SHELF_LIFE_COMMENT
            ),
            List.of(
                KnownMdmParams.LIFE_TIME,
                KnownMdmParams.LIFE_TIME_UNIT,
                KnownMdmParams.LIFE_TIME_COMMENT
            ),
            List.of(
                KnownMdmParams.GUARANTEE_PERIOD,
                KnownMdmParams.GUARANTEE_PERIOD_UNIT,
                KnownMdmParams.GUARANTEE_PERIOD_COMMENT
            )
        );

        List<MskuParamValue> goldenParams = new ArrayList<>();
        for (List<Long> threeParamId : paramIds) {
            MskuParamValue valueParam = new MskuParamValue();
            valueParam.setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
                .setSourceUpdatedTs(Instant.now().minusSeconds(10000))
                .setMdmParamId(threeParamId.get(0));

            if (threeParamId.get(0) == KnownMdmParams.SHELF_LIFE) {
                valueParam.setNumeric(BigDecimal.valueOf(10));
            } else {
                valueParam.setString(BigDecimal.valueOf(10).toString());
            }

            MskuParamValue unitParam = new MskuParamValue();
            unitParam.setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
                .setSourceUpdatedTs(Instant.now().minusSeconds(10000))
                .setMdmParamId(threeParamId.get(1))
                .setOption(new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeUnit.DAY)));

            MskuParamValue commentParam = new MskuParamValue();
            commentParam
                .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR)
                .setSourceUpdatedTs(Instant.now().minusSeconds(10000))
                .setMdmParamId(threeParamId.get(2))
                .setString("Хранить во влажном, месте");

            goldenParams.addAll(List.of(valueParam, unitParam, commentParam));
        }


        Optional<CommonMsku> newGoldenItem =
            service.calculateGoldenItem(MODEL_KEY, GoldComputationContext.EMPTY_CONTEXT,
                List.of(), new CommonMsku(MODEL_KEY, goldenParams)
            );

        assertThat(newGoldenItem).isPresent();

        List<Long> flatListOfParaIds = paramIds.stream().flatMap(Collection::stream).collect(Collectors.toList());
        for (Long paramId : flatListOfParaIds) {
            Optional<MskuParamValue> paramValue = newGoldenItem.get().getParamValue(paramId);
            assertThat(paramValue).isPresent();

            MasterDataSourceType masterDataSourceType = paramValue.get()
                .getModificationInfo()
                .getMasterDataSourceType();
            assertThat(masterDataSourceType).isEqualTo(MasterDataSourceType.MDM_OPERATOR);
        }
    }

    @Test
    public void whenHaveMboOperatorGuaranteePeriodOrLifeTimeKeepThem() {
        // given
        featureSwitchingAssistant.disableFeature(MdmProperties.HIDE_TIMES_PARAMS_CREATION_ENABLED);

        MasterData masterData = new MasterData()
            .setCustomsCommodityCode("00.01.223")
            .setLifeTime(new TimeInUnits(15, TimeUnit.WEEK))
            .setLifeTimeComment("15 недель, но Вам надоест раньше")
            .setGuaranteePeriod(new TimeInUnits(10, TimeUnit.WEEK))
            .setGuaranteePeriodComment("Так-то, конечно, 10 недель, но менять/чинить мы вам это всё равно не будем");

        CommonMsku existing = new CommonMsku(MODEL_KEY, List.of(
            (MskuParamValue) new MskuParamValue()
                .setMskuId(MODEL_ID)
                .setMdmParamId(KnownMdmParams.LIFE_TIME)
                .setXslName(cache.get(KnownMdmParams.LIFE_TIME).getXslName())
                .setString("10")
                .setMasterDataSourceType(MasterDataSourceType.MBO_OPERATOR)
                .setMasterDataSourceId("krotkov"),
            (MskuParamValue) new MskuParamValue()
                .setMskuId(MODEL_ID)
                .setMdmParamId(KnownMdmParams.LIFE_TIME_UNIT)
                .setXslName(cache.get(KnownMdmParams.LIFE_TIME_UNIT).getXslName())
                .setOption(new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeUnit.WEEK)))
                .setMasterDataSourceType(MasterDataSourceType.MBO_OPERATOR)
                .setMasterDataSourceId("krotkov"),
            (MskuParamValue) new MskuParamValue()
                .setMskuId(MODEL_ID)
                .setMdmParamId(KnownMdmParams.GUARANTEE_PERIOD)
                .setXslName(cache.get(KnownMdmParams.GUARANTEE_PERIOD).getXslName())
                .setString("1")
                .setMasterDataSourceType(MasterDataSourceType.MBO_OPERATOR)
                .setMasterDataSourceId("krotkov"),
            (MskuParamValue) new MskuParamValue()
                .setMskuId(MODEL_ID)
                .setMdmParamId(KnownMdmParams.GUARANTEE_PERIOD_UNIT)
                .setXslName(cache.get(KnownMdmParams.GUARANTEE_PERIOD_UNIT).getXslName())
                .setOption(new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeUnit.MONTH)))
                .setMasterDataSourceType(MasterDataSourceType.MBO_OPERATOR)
                .setMasterDataSourceId("krotkov"),
            (MskuParamValue) new MskuParamValue()
                .setMskuId(MODEL_ID)
                .setMdmParamId(KnownMdmParams.GUARANTEE_PERIOD_COMMENT)
                .setXslName(cache.get(KnownMdmParams.GUARANTEE_PERIOD_COMMENT).getXslName())
                .setString("Самая честная гарантия")
                .setMasterDataSourceType(MasterDataSourceType.MBO_OPERATOR)
                .setMasterDataSourceId("krotkov")
        ));

        GoldComputationContext goldComputationContext = new GoldComputationContext(
            0L,
            List.of(),
            Map.of(
                KnownMdmMboParams.LIFE_TIME_USE_PARAM_ID, true,
                KnownMdmMboParams.GUARANTEE_PERIOD_USE_PARAM_ID, true
            ),
            Map.of(),
            Map.of(),
            Map.of(),
            Map.of(),
            Set.of()
        );

        // when
        CommonMsku calculationResult = service.calculateGoldenItem(
            MODEL_KEY,
            goldComputationContext,
            List.of(new MasterDataSilverItem(masterData)),
            existing
        ).orElseThrow();

        // then
        Assertions.assertThat(calculationResult.getParamValue(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID))
            .flatMap(MdmParamValue::getString)
            .hasValue(masterData.getCustomsCommodityCode());
        Assertions.assertThat(calculationResult.getValues())
            .filteredOn(pv -> pv.getMdmParamId() != KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID)
            .containsExactlyInAnyOrderElementsOf(existing.getValues());
    }
}
