package ru.yandex.market.mboc.common.masterdata.services.united;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.priceinfo.PriceInfo;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class MskuUnitedProcessingPricesTest extends UnitedProcessingServiceSetupBaseTest {
    private static final ModelKey MODEL_KEY = new ModelKey(1000L, 1000L);


    @Test
    public void whenHavePricesComputeMedianForMsku() {
        proceedGoldRecalculation(
            List.of(),
            List.of(
                new PriceInfo("100", 100, 1235.0, LocalDate.now()),
                new PriceInfo("101", 100, 1999.0, LocalDate.now()),
                new PriceInfo("102", 100, 1984.0, LocalDate.now()),
                new PriceInfo("103", 100, 1900.0, LocalDate.now())
            ),
            List.of(),
            List.of(createNumericParamValue(1942.0, KnownMdmParams.PRICE, MasterDataSourceType.AUTO, "", 0))
        );
    }

    @Test
    public void whenFeatureDisabledNotComputePrices() {
        featureSwitchingAssistant.disableFeature(MdmProperties.USE_PRICES_IN_MSKU_GOLD_COMPUTATION);
        featureSwitchingAssistant.invalidateCaches();
        proceedGoldRecalculation(
            List.of(),
            List.of(new PriceInfo("100", 100, 1000.0, LocalDate.now())),
            List.of(),
            List.of()
        );
    }

    @Test
    public void whenPriceGreaterThanVolumeSetPreciousGood() {
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_PRECIOUS_GOOD);
        featureSwitchingAssistant.invalidateCaches();
        proceedGoldRecalculation(
            List.of(
                createSilverDimensionAndSilverItem(
                    new ShopSkuKey(100, "100"),
                    MdmIrisPayload.MasterDataSource.MEASUREMENT,
                    10, //длина
                    10, //ширина
                    10, //высота
                    1,
                    0,
                    "MEASUREMENT421"
                )
            ),
            List.of(new PriceInfo("100", 100, 1200.0, LocalDate.now())),
            List.of(),
            ImmutableList.<MskuParamValue>builder()
                .add(createBooleanParamValue(true, KnownMdmParams.PRECIOUS_GOOD, MasterDataSourceType.AUTO, "", 0))
                .addAll(createGoldDimensions(10, 10, 10, 1,
                    MasterDataSourceType.MEASUREMENT, "supplier_id:100 shop_sku:100 measurement:MEASUREMENT421", 0))
                .add(createNumericParamValue(1200.0, KnownMdmParams.PRICE, MasterDataSourceType.AUTO, "", 0))
                .build()
        );
    }

    @Test
    public void whenHaveOperatorChangesShouldPreferThem() {
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_PRECIOUS_GOOD);
        proceedGoldRecalculation(
            List.of(createSilverDimensionAndSilverItem(
                new ShopSkuKey(100, "100"),
                MdmIrisPayload.MasterDataSource.MEASUREMENT,
                10, //длина
                10, //ширина
                10, //высота
                1,
                0,
                "MEASUREMENT421"
            )),
            List.of(new PriceInfo("100", 100, 1200.0, LocalDate.now())),
            List.of(
                createBooleanParamValue(false, KnownMdmParams.PRECIOUS_GOOD, MasterDataSourceType.MDM_OPERATOR, "", 0)
            ),
            ImmutableList.<MskuParamValue>builder()
                .addAll(createGoldDimensions(10, 10, 10, 1,
                    MasterDataSourceType.MEASUREMENT, "supplier_id:100 shop_sku:100 measurement:MEASUREMENT421", 0))
                .add(createBooleanParamValue(false,
                    KnownMdmParams.PRECIOUS_GOOD, MasterDataSourceType.MDM_OPERATOR, "", 0))
                .add(createNumericParamValue(1200.0, KnownMdmParams.PRICE, MasterDataSourceType.AUTO, "", 0))
                .build()
        );
    }

    @Test
    public void whenComputationDisabledShouldKeepOperatorChanges() {
        featureSwitchingAssistant.disableFeature(MdmProperties.COMPUTE_PRECIOUS_GOOD);
        proceedGoldRecalculation(
            List.of(),
            List.of(),
            List.of(
                createBooleanParamValue(false, KnownMdmParams.PRECIOUS_GOOD, MasterDataSourceType.MDM_OPERATOR, "", 0)
            ),
            ImmutableList.<MskuParamValue>builder()
                .add(createBooleanParamValue(false,
                    KnownMdmParams.PRECIOUS_GOOD, MasterDataSourceType.MDM_OPERATOR, "", 0))
                .build()
        );
    }

    @Test
    public void whenHaveMboOperatorChangesShouldPreferThemToAuto() {
        featureSwitchingAssistant.enableFeature(MdmProperties.COMPUTE_PRECIOUS_GOOD);
        proceedGoldRecalculation(
            List.of(createSilverDimensionAndSilverItem(
                new ShopSkuKey(100, "100"),
                MdmIrisPayload.MasterDataSource.MEASUREMENT,
                10, //длина
                10, //ширина
                10, //высота
                1,
                0,
                "MEASUREMENT421"
            )),
            List.of(new PriceInfo("100", 100, 900.0, LocalDate.now())), // price lower than volume -> false
            List.of(
                createBooleanParamValue(true, KnownMdmParams.PRECIOUS_GOOD, MasterDataSourceType.MBO_OPERATOR, "", 0)
            ),
            ImmutableList.<MskuParamValue>builder()
                .addAll(createGoldDimensions(10, 10, 10, 1,
                    MasterDataSourceType.MEASUREMENT, "supplier_id:100 shop_sku:100 measurement:MEASUREMENT421", 0))
                .add(createBooleanParamValue(true,
                    KnownMdmParams.PRECIOUS_GOOD, MasterDataSourceType.MBO_OPERATOR, "", 0))
                .add(createNumericParamValue(900.0, KnownMdmParams.PRICE, MasterDataSourceType.AUTO, "", 0))
                .build()
        );
    }

    private void proceedGoldRecalculation(
        List<CommonSsku> silverDimensions,
        List<PriceInfo> prices,
        List<MskuParamValue> existingMskuParamValues,
        List<MskuParamValue> expectedMskuParamValues
    ) {
        // Сперва придётся проделать много подготовительной работы.
        // 1. Положим модельку в очередь.
        enqueueMskusWithReason(List.of(MODEL_KEY.getModelId()), MdmEnqueueReason.CHANGED_BY_MBO_OPERATOR);

        // 2. Загрузим reference items
        goldSskuRepository.insertOrUpdateSskus(silverDimensions);

        // 3. Загрузим цены
        priceInfoRepository.insertOrUpdateAll(prices);

        // 4. Создадим маппинги: модель + категория -> офферы.
        LinkedHashSet<ShopSkuKey> sskuKeys = Stream.concat(
            silverDimensions.stream().map(CommonSsku::getKey),
            prices.stream().map(PriceInfo::getShopSkuKey)
        ).collect(Collectors.toCollection(LinkedHashSet::new));
        sskuKeys.forEach(offer -> mappingsCacheRepository
            .insert(new MappingCacheDao().setModelKey(MODEL_KEY).setShopSkuKey(offer).setUpdateStamp(1L)));
        // добавим информацию о поставщике
        List<MdmSupplier> suppliers = sskuKeys.stream()
            .map(ShopSkuKey::getSupplierId)
            .distinct()
            .map(id -> new MdmSupplier().setId(id).setType(MdmSupplierType.THIRD_PARTY))
            .collect(Collectors.toList());
        mdmSupplierRepository.insertBatch(suppliers);

        // 5. Создадим существующую золотую запись, которую нужно будет обновить
        mskuRepository.insertOrUpdateMsku(new CommonMsku(MODEL_KEY, existingMskuParamValues));

        // Запускаем
        execute();

        Assertions.assertThat(mskuRepository.findMsku(MODEL_KEY.getModelId()))
            .map(TestBmdmUtils::removeBmdmIdAndVersion) // удалим служебные storage-api параметры
            .map(TestMdmParamUtils::filterCisCargoTypes) // удалим дефолтные false ЧЗ и Mercury
            .hasValueSatisfying(msku -> assertEquals(new CommonMsku(MODEL_KEY, expectedMskuParamValues), msku));
    }

    private void assertEquals(CommonMsku expected, CommonMsku actual) {
        Map<Long, MskuParamValue> expectedParamValues = expected.getParamValues();
        Map<Long, MskuParamValue> actualParamValues = actual.getParamValues();

        Assertions.assertThat(expectedParamValues.size()).isEqualTo(actualParamValues.size());

        expectedParamValues.forEach((key, expectedValue) -> {
            MskuParamValue actualValue = actualParamValues.get(key);
            Assertions.assertThat(expectedValue.valueAndSourceEquals(actualValue))
                .withFailMessage("Expected msku param value %s, but actual is %s.", expectedValue, actualValue)
                .isTrue();
        });
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

    @SuppressWarnings("checkstyle:ParameterNumber")
    private CommonSsku createSilverDimensionAndSilverItem(
        ShopSkuKey shopSkuKey, MdmIrisPayload.MasterDataSource source,
        double length, double width, double height, double weightGross, long timestamp, String masterDataSourceId
    ) {
        silverSskuRepository.insertOrUpdateSsku(TestDataUtils.wrapSilver(List.of(
            silverValue(shopSkuKey,
                KnownMdmParams.LENGTH,
                String.valueOf(length),
                Instant.ofEpochMilli(timestamp),
                MasterDataSourceType.proto2pojo(
                    MdmIrisPayload.Associate.newBuilder().setType(source).setId(masterDataSourceId).build())),
            silverValue(shopSkuKey,
                KnownMdmParams.WIDTH,
                String.valueOf(width),
                Instant.ofEpochMilli(timestamp),
                MasterDataSourceType.proto2pojo(
                    MdmIrisPayload.Associate.newBuilder().setType(source).setId(masterDataSourceId).build())),
            silverValue(shopSkuKey,
                KnownMdmParams.HEIGHT,
                String.valueOf(height),
                Instant.ofEpochMilli(timestamp),
                MasterDataSourceType.proto2pojo(
                    MdmIrisPayload.Associate.newBuilder().setType(source).setId(masterDataSourceId).build())),
            silverValue(shopSkuKey,
                KnownMdmParams.WEIGHT_GROSS,
                String.valueOf(weightGross),
                Instant.ofEpochMilli(timestamp),
                MasterDataSourceType.proto2pojo(
                    MdmIrisPayload.Associate.newBuilder().setType(source).setId(masterDataSourceId).build()))
        )));

        var shippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(length,
                width,
                height,
                weightGross,
                null,
                null,
                timestamp);
        MdmIrisPayload.Item item = ItemWrapperTestUtil.createItem(shopSkuKey, source, shippingUnit);
        var refItem = new ReferenceItemWrapper(item);
        return new CommonSsku(shopSkuKey).setBaseValues(
            sskuGoldenParamUtil.createSskuGoldenParamValuesFromReferenceItem(
                refItem, SskuGoldenParamUtil.ParamsGroup.SSKU));
    }

    private SskuSilverParamValue silverValue(ShopSkuKey key,
                                             long mdmParamId,
                                             String decimal,
                                             Instant ts,
                                             MasterDataSource source) {
        return (SskuSilverParamValue) new SskuSilverParamValue()
            .setShopSkuKey(key)
            .setMasterDataSource(source)
            .setMdmParamId(mdmParamId)
            .setXslName("-")
            .setNumeric(new BigDecimal(decimal))
            .setSourceUpdatedTs(ts)
            .setUpdatedTs(ts);
    }
}
