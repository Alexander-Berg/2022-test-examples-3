package ru.yandex.market.mbo.mdm.tms.executors;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mboc.common.masterdata.KnownMdmMboParams;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class RecomputeMskuGoldExecutorDimensionsTest extends RecomputeMskuGoldExecutorBaseTest {
    private static final ModelKey MODEL_KEY = new ModelKey(1000L, 1000L);

    @Test
    public void whenNewAlgoEnabledChooseClosestToGeometricMean() {
        var silverDimension = List.of(
            createSilverDimension(new ShopSkuKey(1234, "1"), MdmIrisPayload.MasterDataSource.SUPPLIER,
                12, 12, 12, 1.5, 8797),
            createSilverDimension(new ShopSkuKey(3897, "3"), MdmIrisPayload.MasterDataSource.SUPPLIER,
                13, 14, 12, 1.51, 2000),
            createSilverDimension(new ShopSkuKey(2325, "5"), MdmIrisPayload.MasterDataSource.SUPPLIER,
                15, 17, 16, 1.7, 3000)
        );

        var expectedGold = new CommonMsku(MODEL_KEY, createGoldDimensions(13, 14, 12, 1.51,
            MasterDataSourceType.SUPPLIER, "supplier_id:3897 shop_sku:3 supplier:SUPPLIER421", 2000));

        proceedGoldRecalculation(null, null, silverDimension, new CommonMsku(MODEL_KEY), expectedGold);
    }

    @Test
    public void whenTwoAreClosestToMeanShouldChooseOneThatSatisfyCategoryOverride() {
        var silverDimension = List.of(
            createSilverDimension(new ShopSkuKey(3897, "3"), MdmIrisPayload.MasterDataSource.SUPPLIER,
                13, 14, 12, 17.51, 4800),
            createSilverDimension(new ShopSkuKey(2325, "5"), MdmIrisPayload.MasterDataSource.SUPPLIER,
                25, 37, 20, 35, 3000)
        );

        var expectedGold = new CommonMsku(MODEL_KEY, createGoldDimensions(25, 37, 20, 35,
            MasterDataSourceType.SUPPLIER, "supplier_id:2325 shop_sku:5 supplier:SUPPLIER421", 3000));

        proceedGoldRecalculation(null, KnownMdmMboParams.HEAVY_GOOD_OVERRIDE_TO_TRUE_OPTION_ID,
            silverDimension, new CommonMsku(MODEL_KEY), expectedGold);
    }

    @Test
    public void whenTwoAreClosestToMeanAndNoSatisfyCategorySettingsChooseLatest() {
        var silverDimension = List.of(
            createSilverDimension(new ShopSkuKey(2456, "2"), MdmIrisPayload.MasterDataSource.SUPPLIER,
                13000, 13000, 13000, 100000000, 1000),
            createSilverDimension(new ShopSkuKey(3897, "3"), MdmIrisPayload.MasterDataSource.MEASUREMENT,
                13, 14, 12, 17.51, 4800),
            createSilverDimension(new ShopSkuKey(2324, "4"), MdmIrisPayload.MasterDataSource.SUPPLIER,
                10, 10, 10, 1, 1000),
            createSilverDimension(new ShopSkuKey(2325, "5"), MdmIrisPayload.MasterDataSource.MEASUREMENT,
                25, 37, 20, 35, 3000)
        );

        var expectedGold = new CommonMsku(MODEL_KEY, createGoldDimensions(13, 14, 12, 17.51,
            MasterDataSourceType.MEASUREMENT, "supplier_id:3897 shop_sku:3 measurement:MEASUREMENT421", 4800));

        proceedGoldRecalculation(null, null, silverDimension, new CommonMsku(MODEL_KEY), expectedGold);
    }

    @Test
    public void whenGotInvalidSskuSilverShouldFilter() {
        var silverDimension = List.of(
            createSilverDimension(new ShopSkuKey(2456, "2"), MdmIrisPayload.MasterDataSource.SUPPLIER,
                13000, 13000, 13000, 100000000, 1000),
            createSilverDimension(new ShopSkuKey(3897, "3"), MdmIrisPayload.MasterDataSource.MEASUREMENT,
                13, 14, 12, 0.0, 4800), // <- Нулевой вес самый приоритетный.
            createSilverDimension(new ShopSkuKey(2324, "4"), MdmIrisPayload.MasterDataSource.SUPPLIER,
                10, 10, 10, 1, 1000)
        );

        // Но будет выбран другой, так как приоритетный блок - не валидный.
        var expectedGold = new CommonMsku(MODEL_KEY, createGoldDimensions(10, 10, 10, 1,
            MasterDataSourceType.SUPPLIER, "supplier_id:2324 shop_sku:4 supplier:SUPPLIER421", 1000));

        proceedGoldRecalculation(null, null, silverDimension, new CommonMsku(MODEL_KEY), expectedGold);
    }

    @Test
    public void whenNoValidSilverDimensionsShouldNotKeepOldGold() {
        var silverDimension = List.of(
            createSilverDimension(new ShopSkuKey(3897, "3"), MdmIrisPayload.MasterDataSource.SUPPLIER,
                130, 34, 42, 200.0, 4800) // default max weight = 100 kg
        );

        List<MskuParamValue> existingValues = createGoldDimensions(130, 34, 42, 200.0,
            MasterDataSourceType.SUPPLIER, "supplier_id:3897 shop_sku:3 supplier:SUPPLIER421", 4800);
        var existingGold = new CommonMsku(MODEL_KEY, existingValues);

        proceedGoldRecalculation(null, null, silverDimension, existingGold, null);
    }

    @Test
    public void whenOnlyCategorySettingsChangedHeavyGoodShouldBeRecomputed() {
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_HEAVY_GOOD_FROM_MSKU_DIMENSIONS_GLOBAL);

        ShopSkuKey sskuKey = new ShopSkuKey(3897, "3");
        var silverDimension = List.of(
            createSilverDimension(sskuKey, MdmIrisPayload.MasterDataSource.SUPPLIER, 130, 34, 42, 40.0, 4800));

        List<MskuParamValue> expectedValues = new ArrayList<>(createGoldDimensions(130, 34, 42, 40.0,
            MasterDataSourceType.SUPPLIER, "supplier_id:3897 shop_sku:3 supplier:SUPPLIER421", 4800));
        expectedValues.add(createBooleanParamValue(true, KnownMdmParams.HEAVY_GOOD, MasterDataSourceType.AUTO,
            MasterDataSourceType.HEAVY_GOOD_BY_CATEGORY_OVERRIDE, 0L));
        expectedValues.add(createBooleanParamValue(true, KnownMdmParams.HEAVY_GOOD_20, MasterDataSourceType.AUTO,
            MasterDataSourceType.HEAVY_GOOD_BY_MSKU_DIMENSIONS, 0L));
        var expectedGold = new CommonMsku(MODEL_KEY, expectedValues);

        proceedGoldRecalculation(null, KnownMdmMboParams.HEAVY_GOOD_OVERRIDE_TO_TRUE_OPTION_ID, silverDimension,
            new CommonMsku(MODEL_KEY), expectedGold);

        parameterValueCachingServiceMock.addCategoryParameterValues(MODEL_KEY.getCategoryId(),
            MboParameters.ParameterValue.newBuilder()
                .setParamId(KnownMdmMboParams.HEAVY_GOOD_OVERRIDE_PARAM_ID)
                .setOptionId(KnownMdmMboParams.HEAVY_GOOD_OVERRIDE_TO_FALSE_OPTION_ID)
                .build()
        );

        var expectedGold2 = new CommonMsku(
            MODEL_KEY,
            createGoldDimensions(130, 34, 42, 40.0, MasterDataSourceType.SUPPLIER,
                "supplier_id:3897 shop_sku:3 supplier:SUPPLIER421", 4800)
        );
        expectedGold2.addParamValue(
            createBooleanParamValue(false, KnownMdmParams.HEAVY_GOOD, MasterDataSourceType.AUTO,
                MasterDataSourceType.HEAVY_GOOD_BY_CATEGORY_OVERRIDE, 0L)
        );
        expectedGold2.addParamValue(
            createBooleanParamValue(true, KnownMdmParams.HEAVY_GOOD_20, MasterDataSourceType.AUTO,
                MasterDataSourceType.HEAVY_GOOD_BY_MSKU_DIMENSIONS, 0L)
        );

        mskuQueue.enqueue(MODEL_KEY.getModelId(), MdmEnqueueReason.CHANGED_BY_MDM_OPERATOR);
        executor.execute();

        Assertions.assertThat(mskuQueue.getUnprocessedBatch(1)).isEmpty();
        Map<Long, CommonMsku> resultingMskus = mskuRepository.findAllMskus();
        Assertions.assertThat(resultingMskus).hasSize(1);
        assertEquals(expectedGold2, clearInsignificantParams(resultingMskus.get(MODEL_KEY.getModelId())));
    }

    @Test
    public void testAutoComputationRewriteOperatorValues() {
        var existingGold = new CommonMsku(MODEL_KEY, createGoldDimensions(12, 12, 12, 2,
            MasterDataSourceType.MDM_OPERATOR, "krotkov", 2000));
        existingGold.addParamValue(createGoldWeightNet(1.5, MasterDataSourceType.MDM_OPERATOR, "krotkov", 1900));

        var silverDimension = List.of(
            createSilverDimension(new ShopSkuKey(1112, "0"), MdmIrisPayload.MasterDataSource.SUPPLIER,
                10, 10, 10, 1, 1000),
            createSilverDimension(new ShopSkuKey(1234, "1"), MdmIrisPayload.MasterDataSource.MEASUREMENT,
                12, 12, 12, 1.5, 8797),
            createSilverDimension(new ShopSkuKey(2456, "2"), MdmIrisPayload.MasterDataSource.SUPPLIER,
                13000, 13000, 13000, 100000000, 1000),
            createSilverDimension(new ShopSkuKey(3897, "3"), MdmIrisPayload.MasterDataSource.MEASUREMENT,
                13, 14, 12, 1.51, 2000),
            createSilverDimension(new ShopSkuKey(2324, "4"), MdmIrisPayload.MasterDataSource.SUPPLIER,
                10, 10, 10, 1, 1000),
            createSilverDimension(new ShopSkuKey(2325, "5"), MdmIrisPayload.MasterDataSource.MEASUREMENT,
                15, 17, 16, 1.7, 3000),
            createSilverWeightNet(new ShopSkuKey(2326, "6"), MdmIrisPayload.MasterDataSource.SUPPLIER, 1.25, 1000)
        );

        // latest measurement + the only one weight net
        var expectedGold = new CommonMsku(MODEL_KEY, createGoldDimensions(12, 12, 12, 1.5,
            MasterDataSourceType.MEASUREMENT, "supplier_id:1234 shop_sku:1 measurement:MEASUREMENT421", 2000));
        expectedGold.addParamValue(createGoldWeightNet(
            1.25, MasterDataSourceType.SUPPLIER, "supplier_id:2326 shop_sku:6 supplier:SUPPLIER421", 1000));

        proceedGoldRecalculation(null, null, silverDimension, existingGold, expectedGold);
    }

    private void proceedGoldRecalculation(
        Boolean categoryHeavyGoodOptionId,
        Integer categoryHeavyGoodOverrideOptionId,
        List<CommonSsku> silverDimensions,
        CommonMsku existingGold,
        CommonMsku expectedGold
    ) {
        // Сперва придётся проделать много подготовительной работы.
        // 1. Положим модельку в очередь.
        mskuQueue.enqueue(MODEL_KEY.getModelId(), MdmEnqueueReason.CHANGED_BY_MBO_OPERATOR);

        // 2. Загрузим reference items
        goldSskuRepository.insertOrUpdateSskus(silverDimensions);

        // 3. Сохраним категорийные настройки и оверрайды КГТ
        List<MboParameters.ParameterValue> categoryParams = new ArrayList<>();
        if (categoryHeavyGoodOptionId != null) {
            MboParameters.ParameterValue categoryParam = MboParameters.ParameterValue.newBuilder()
                .setParamId(KnownMdmMboParams.HEAVY_GOOD_CATEGORY_PARAM_ID)
                .setBoolValue(categoryHeavyGoodOptionId)
                .build();
            categoryParams.add(categoryParam);
        }
        if (categoryHeavyGoodOverrideOptionId != null) {
            MboParameters.ParameterValue categoryParam = MboParameters.ParameterValue.newBuilder()
                .setParamId(KnownMdmMboParams.HEAVY_GOOD_OVERRIDE_PARAM_ID)
                .setOptionId(categoryHeavyGoodOverrideOptionId)
                .build();
            categoryParams.add(categoryParam);
        }
        parameterValueCachingServiceMock.addCategoryParameterValues(MODEL_KEY.getCategoryId(), categoryParams);

        // 4. Создадим маппинги: модель + категория -> офферы.
        silverDimensions.stream().map(CommonSsku::getKey).forEach(
            offer -> mappingsCacheRepository
                .insert(new MappingCacheDao().setModelKey(MODEL_KEY).setShopSkuKey(offer).setUpdateStamp(1L))
        );
        // добавим информацию о поставщике
        mdmSupplierRepository.insertBatch(silverDimensions.stream()
            .map(s -> s.getKey().getSupplierId())
            .distinct()
            .map(id -> new MdmSupplier()
                .setId(id.intValue())
                .setType(MdmSupplierType.THIRD_PARTY))
            .collect(Collectors.toList()));

        // 5. Создадим существующую золотую запись, которую нужно будет обновить
        mskuRepository.insertOrUpdateMsku(existingGold);

        // Запускаем
        executor.execute();

        // Проверяем
        Assertions.assertThat(mskuQueue.getUnprocessedBatch(1)).isEmpty();
        if (expectedGold != null) {
            Map<Long, CommonMsku> resultingMskus = mskuRepository.findAllMskus();
            Assertions.assertThat(resultingMskus).hasSize(1);
            assertEquals(expectedGold, clearInsignificantParams(resultingMskus.get(MODEL_KEY.getModelId())));
        } else {
            Assertions.assertThat(mskuRepository.findAllMskus().values())
                .map(RecomputeMskuGoldExecutorDimensionsTest::clearInsignificantParams)
                .filteredOn(commonMsku -> commonMsku.getValues().size() > 0)
                .isEmpty();
        }
    }

    private static CommonMsku clearInsignificantParams(CommonMsku commonMsku) {
        return TestMdmParamUtils.filterCisCargoTypes(TestBmdmUtils.removeBmdmIdAndVersion(commonMsku));
    }

    private void assertEquals(CommonMsku expected, CommonMsku actual) {
        Assertions.assertThat(expected.valueAndSourceEquals(clearInsignificantParams(actual)))
            .withFailMessage("Expected %s, but actual %s", expected, actual)
            .isTrue();
    }

    private MskuParamValue createBooleanParamValue(boolean value, long mdmParamId, MasterDataSourceType sourceType,
                                                   String sourceId, long timestamp) {
        MskuParamValue mskuParamValue = new MskuParamValue().setMskuId(MODEL_KEY.getModelId());
        mskuParamValue.setMdmParamId(mdmParamId)
            .setBool(value)
            .setMasterDataSourceType(sourceType)
            .setMasterDataSourceId(sourceId)
            .setXslName(mdmParamCache.get(mdmParamId).getXslName())
            .setUpdatedTs(Instant.ofEpochMilli(timestamp));
        return mskuParamValue;
    }

    private MskuParamValue createNumericParamValue(double value, long mdmParamId, MasterDataSourceType sourceType,
                                                   String sourceId, long timestamp) {
        MskuParamValue mskuParamValue = new MskuParamValue().setMskuId(MODEL_KEY.getModelId());
        mskuParamValue.setMdmParamId(mdmParamId)
            .setNumeric(BigDecimal.valueOf(value))
            .setMasterDataSourceType(sourceType)
            .setMasterDataSourceId(sourceId)
            .setXslName(mdmParamCache.get(mdmParamId).getXslName())
            .setUpdatedTs(Instant.ofEpochMilli(timestamp));
        return mskuParamValue;
    }

    private List<MskuParamValue> createGoldDimensions(double length, double width, double height, double weightGross,
                                                      MasterDataSourceType sourceType, String sourceId,
                                                      long timestamp) {
        MskuParamValue lengthPV =
            createNumericParamValue(length, KnownMdmParams.LENGTH, sourceType, sourceId, timestamp);
        MskuParamValue widthPV =
            createNumericParamValue(width, KnownMdmParams.WIDTH, sourceType, sourceId, timestamp);
        MskuParamValue heightPV =
            createNumericParamValue(height, KnownMdmParams.HEIGHT, sourceType, sourceId, timestamp);
        MskuParamValue weightGrossPV =
            createNumericParamValue(weightGross, KnownMdmParams.WEIGHT_GROSS, sourceType, sourceId, timestamp);
        return List.of(lengthPV, widthPV, heightPV, weightGrossPV);
    }

    private CommonSsku createSilverDimension(
        ShopSkuKey shopSkuKey, MdmIrisPayload.MasterDataSource source,
        double length, double width, double height, double weightGross, long timestamp
    ) {
        var shippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(length, width, height, weightGross, null, null, timestamp);
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(shopSkuKey, source, shippingUnit);
        var refItem = new ReferenceItemWrapper(item);
        var ssku = new CommonSsku(shopSkuKey);
        var params = sskuGoldenParamUtil.createSskuGoldenParamValuesFromReferenceItem(
            refItem, SskuGoldenParamUtil.ParamsGroup.SSKU);
        return ssku.setBaseValues(params);
    }

    private CommonSsku createSilverWeightNet(ShopSkuKey shopSkuKey,
                                                       MdmIrisPayload.MasterDataSource source,
                                                       double value,
                                                       long timestamp) {
        var shippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(null, null, null, null, value, null, timestamp);
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(shopSkuKey, source, shippingUnit);
        var refItem = new ReferenceItemWrapper(item);
        var ssku = new CommonSsku(shopSkuKey);
        var params = sskuGoldenParamUtil.createSskuGoldenParamValuesFromReferenceItem(
            refItem, SskuGoldenParamUtil.ParamsGroup.SSKU);
        return ssku.setBaseValues(params);
    }

    private MskuParamValue createGoldWeightNet(double value,
                                               MasterDataSourceType sourceType,
                                               String sourceId,
                                               long timestamp) {
        return createNumericParamValue(value, KnownMdmParams.WEIGHT_NET, sourceType, sourceId, timestamp);
    }
}
