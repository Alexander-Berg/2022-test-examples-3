package ru.yandex.market.mboc.common.masterdata.services.united;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Preconditions;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmModificationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuGoldenParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuQueueInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class MskuUnitedProcessingTest extends UnitedProcessingServiceSetupBaseTest {


    @Test
    public void whenEmptyQueueThenExistingGoldUnaffected() {
        Preconditions.checkArgument(mskuAndSskuQueue.findAll().isEmpty());
        var randomMsku = new CommonMsku(
            1L,
            List.of(
                randomMskuParamValue(1L, KnownMdmParams.PRICE),
                randomMskuParamValue(1L, KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX),
                randomMskuParamValue(1L, KnownMdmParams.HOUSEHOLD_CHEMICALS)
            )
        );
        mskuRepository.insertOrUpdateMsku(randomMsku);
        ShopSkuKey offer = new ShopSkuKey(12, "moo");
        MasterData masterData = TestDataUtils.generateMasterData(offer, random);
        masterDataRepository.insert(masterData);
        mappingsCacheRepository.insert(
            new MappingCacheDao().setMskuId(1L).setCategoryId(9041).setShopSkuKey(offer).setUpdateStamp(1L));
        // добавим информацию о поставщике
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(offer.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));

        execute();
        assertThat(mskuRepository.findAllMskus().values())
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .containsOnly(randomMsku);
    }

    @Test
    public void whenMskuEnqueuedShouldComputeGold() {
        // Сперва придётся проделать много подготовительной работы.
        ModelKey key = new ModelKey(1L, 100L);

        // 1. Положим модельку в очередь.
        enqueueMskusWithReason(List.of(key.getModelId()), MdmEnqueueReason.CHANGED_BY_MBO_OPERATOR);

        // 2. Сгенерим SSKU мастер-данные. Пусть на уровне SSKU будет задан ТН ВЭД и применимость срока годности.
        ShopSkuKey offer = new ShopSkuKey(12, "moo");
        MasterData masterData = new MasterData();
        masterData.setShopSkuKey(offer);
        masterData.setCustomsCommodityCode("50600980031");
        masterDataRepository.insert(masterData);

        //сгенерим белые данные и данные неизвестного поставщика - они не должны повлиять на расчет
        ShopSkuKey offerWhite = new ShopSkuKey(22, "mau");
        MasterData masterDataWhite = new MasterData().setShopSkuKey(offerWhite).setCustomsCommodityCode("700");
        masterDataWhite.setVat(VatRate.VAT_10);
        masterDataRepository.insert(masterDataWhite);
        ShopSkuKey offerUnknown = new ShopSkuKey(32, "gav");
        masterDataRepository.insert(new MasterData().setShopSkuKey(offerUnknown).setCustomsCommodityCode("800"));

        // 3. Сгенерим настройку уровня категории. Пусть это будет префикс ТН ВЭДа (не путать с цельным ТН ВЭДом выше).
        CategoryParamValue categoryValue = new CategoryParamValue().setCategoryId(key.getCategoryId());
        categoryValue.setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX);
        categoryValue.setString("50600");
        categoryValue.setXslName(mdmParamCache.get(KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX).getXslName());
        categoryValue.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);
        categoryParamValueRepository.insert(categoryValue);

        // 4. Создадим маппинг: модель + категория + оффер.
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key).setShopSkuKey(offer).setUpdateStamp(1L));
        //мапинги для данных белонеизвестных поставщиков
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key)
            .setShopSkuKey(offerWhite).setUpdateStamp(2L));
        mappingsCacheRepository.insert(new MappingCacheDao().setModelKey(key)
            .setShopSkuKey(offerUnknown).setUpdateStamp(3L));
        // добавим информацию о поставщике
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(offer.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(offerWhite.getSupplierId())
            .setType(MdmSupplierType.MARKET_SHOP));

        // 5. Для чистоты эксперимента создадим уже существующий золотой параметр. Пусть это будет КГТ.
        MskuParamValue heavyGoodMskuValue = new MskuParamValue().setMskuId(key.getModelId());
        heavyGoodMskuValue.setBool(true);
        heavyGoodMskuValue.setXslName(mdmParamCache.get(KnownMdmParams.HEAVY_GOOD).getXslName());
        heavyGoodMskuValue.setMdmParamId(KnownMdmParams.HEAVY_GOOD);
        heavyGoodMskuValue.setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
        mskuRepository.insertOrUpdateMsku(new CommonMsku(key, List.of(heavyGoodMskuValue)));

        // 6. Наконец, создадим параметры, которые мы ожидаем увидеть на уровне МСКУ после перерасчёта.
        MskuParamValue expectedCustomsCommCode = new MskuParamValue().setMskuId(key.getModelId());
        expectedCustomsCommCode.setString(masterData.getCustomsCommodityCode());
        expectedCustomsCommCode.setXslName(mdmParamCache.get(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID).getXslName());
        expectedCustomsCommCode.setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID);
        expectedCustomsCommCode.setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedCodePrefix = new MskuParamValue().setMskuId(key.getModelId());
        categoryValue.copyTo(expectedCodePrefix);
        expectedCodePrefix.setModificationInfo(
            new MdmModificationInfo()
                .setMasterDataSourceType(MasterDataSourceType.AUTO)
                .setMasterDataSourceId(MasterDataSourceType.SIMPLE_PARAM_VALUE_FROM_CATEGORY_SETTINGS)
        );

        var paramValues = generateCisCargoTypes(key.getModelId());
        paramValues.addAll(List.of(expectedCustomsCommCode, expectedCodePrefix, heavyGoodMskuValue));
        var expectedMsku = new CommonMsku(key, paramValues);

        // Запускаем
        execute();
//        assertThat(mskuAndSskuQueue.getUnprocessedBatch(1)).isEmpty();
        assertThat(mskuRepository.findAllMskus().values())
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .containsOnly(expectedMsku);

        List<MdmMskuQueueInfo> allIMskuToMboInfos = mskuToMboQueue.findAll();
        assertThat(allIMskuToMboInfos).hasSize(1);
        assertThat(allIMskuToMboInfos.stream())
            .filteredOn(info -> !info.isProcessed())
            .map(MdmQueueInfoBase::getEntityKey)
            .allMatch(id -> id == key.getModelId());
    }


    @Test
    public void testUsingOfSskuGoldenParamsForMskuCalculation() {
        ModelKey key1 = new ModelKey(1L, 100L);

        SskuGoldenParamValue paramValue1 = generateSskuGoldenParam();
        paramValue1.setMdmParamId(KnownMdmParams.SSKU_LENGTH);
        paramValue1.setNumeric(new BigDecimal("10"));

        SskuGoldenParamValue paramValue2 = generateSskuGoldenParam();
        paramValue2.setShopSkuKey(paramValue1.getShopSkuKey());
        paramValue2.setMdmParamId(KnownMdmParams.SSKU_WIDTH);
        paramValue2.setNumeric(new BigDecimal("20"));

        SskuGoldenParamValue paramValue3 = generateSskuGoldenParam();
        paramValue3.setShopSkuKey(paramValue1.getShopSkuKey());
        paramValue3.setMdmParamId(KnownMdmParams.SSKU_HEIGHT);
        paramValue3.setNumeric(new BigDecimal("10"));

        SskuGoldenParamValue paramValue4 = generateSskuGoldenParam();
        paramValue4.setShopSkuKey(paramValue1.getShopSkuKey());
        paramValue4.setMdmParamId(KnownMdmParams.SSKU_WEIGHT_GROSS);
        paramValue4.setNumeric(new BigDecimal("4"));

        goldSskuRepository.insertOrUpdateSsku(
            new CommonSsku(paramValue1.getShopSkuKey())
                .addBaseValue(paramValue1)
                .addBaseValue(paramValue2)
                .addBaseValue(paramValue3)
                .addBaseValue(paramValue4)
        );

        mappingsCacheRepository.insert(
            new MappingCacheDao().setModelKey(key1).setShopSkuKey(paramValue1.getShopSkuKey()).setUpdateStamp(1L));
        // добавим информацию о поставщике
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(paramValue1.getShopSkuKey().getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));

        enqueueMskusWithReason(List.of(key1.getModelId()), MdmEnqueueReason.CHANGED_BY_MBO_OPERATOR);

        storageKeyValueService.putValue(MdmProperties.USE_OWN_SSKU_WD_FOR_MSKU_GOLD_GLOBALLY, true);

        execute();

        Map<Long, CommonMsku> resultingMskus = mskuRepository.findAllMskus();
        Assertions.assertThat(resultingMskus).hasSize(1);

        CommonMsku resultingMsku = resultingMskus.get(key1.getModelId());
        Assertions.assertThat(resultingMsku).isNotNull();

        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.LENGTH))
            .flatMap(MdmParamValue::getNumeric)
            .hasValueSatisfying(value -> Assertions.assertThat(value).isEqualByComparingTo(new BigDecimal("10")));
        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.WIDTH))
            .flatMap(MdmParamValue::getNumeric)
            .hasValueSatisfying(value -> Assertions.assertThat(value).isEqualByComparingTo(new BigDecimal("20")));
        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.HEIGHT))
            .flatMap(MdmParamValue::getNumeric)
            .hasValueSatisfying(value -> Assertions.assertThat(value).isEqualByComparingTo(new BigDecimal("10")));
        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.WEIGHT_GROSS))
            .flatMap(MdmParamValue::getNumeric)
            .hasValueSatisfying(value -> Assertions.assertThat(value).isEqualByComparingTo(new BigDecimal("4")));
    }

    @Test
    public void testUsingOfSskuGoldenParamsForMskuShelfLifeGoldCalculation() {
        ModelKey key1 = new ModelKey(1L, 100L);

        SskuGoldenParamValue paramValue1 = new SskuGoldenParamValue();
        paramValue1.setMdmParamId(KnownMdmParams.SSKU_SHELF_LIFE);
        paramValue1.setNumeric(new BigDecimal("10"));
        paramValue1.setShopSkuKey(new ShopSkuKey(123, "ssku"));
        paramValue1.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        var unitOption =
            new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.YEAR));
        SskuGoldenParamValue paramValue2 = new SskuGoldenParamValue();
        paramValue2.setShopSkuKey(paramValue1.getShopSkuKey());
        paramValue2.setMdmParamId(KnownMdmParams.SSKU_SHELF_LIFE_UNIT);
        paramValue2.setOption(
            new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.YEAR)));
        paramValue2.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        SskuGoldenParamValue paramValue3 = new SskuGoldenParamValue();
        paramValue3.setShopSkuKey(paramValue1.getShopSkuKey());
        paramValue3.setMdmParamId(KnownMdmParams.SSKU_SHELF_LIFE_COMMENT);
        paramValue3.setString("comment");
        paramValue3.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        goldSskuRepository.insertOrUpdateSsku(
            new CommonSsku(paramValue1.getShopSkuKey())
                .addBaseValue(paramValue1)
                .addBaseValue(paramValue2)
                .addBaseValue(paramValue3)
        );

        mappingsCacheRepository.insert(
            new MappingCacheDao().setModelKey(key1).setShopSkuKey(paramValue1.getShopSkuKey()).setUpdateStamp(1L));
        // добавим информацию о поставщике
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(paramValue1.getShopSkuKey().getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY));

        enqueueMskusWithReason(List.of(key1.getModelId()), MdmEnqueueReason.CHANGED_BY_MBO_OPERATOR);
        execute();

        Map<Long, CommonMsku> resultingMskus = mskuRepository.findAllMskus();
        Assertions.assertThat(resultingMskus).hasSize(1);

        CommonMsku resultingMsku = resultingMskus.get(key1.getModelId());
        Assertions.assertThat(resultingMsku).isNotNull();

        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.SHELF_LIFE)
            .flatMap(MdmParamValue::getNumeric))
            .hasValue(new BigDecimal("10"));
        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.SHELF_LIFE_UNIT)
            .flatMap(MdmParamValue::getOption))
            .hasValue(unitOption);
        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.SHELF_LIFE_UNIT)
            .flatMap(MdmParamValue::getOption)
            .flatMap(option -> Optional.of(option.getRenderedValue())))
            .hasValue("годы");
        Assertions.assertThat(resultingMsku.getParamValue(KnownMdmParams.SHELF_LIFE_COMMENT)
            .flatMap(MdmParamValue::getString))
            .hasValue("comment");
    }

    private SskuGoldenParamValue generateSskuGoldenParam() {
        return random.nextObject(SskuGoldenParamValue.class);
    }

    private List<MskuParamValue> generateCisCargoTypes(Long mskuId) {
        MskuParamValue expectedMercuryCisOptional = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_OPTIONAL)
            .setXslName("mercuryOptionalStub")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedMercuryCisDistinct = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_DISTINCT)
            .setXslName("mercuryDistinctStub")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedMercuryCisRequired = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.MERCURY_CIS_REQUIRED)
            .setXslName("mercuryRequiredStub")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);

        MskuParamValue expectedHsCisOptional = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_OPTIONAL)
            .setXslName("cargoType990")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedHsCisDistinct = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_DISTINCT)
            .setXslName("cargoType985")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);
        MskuParamValue expectedHsCisRequired = (MskuParamValue) new MskuParamValue().setMskuId(mskuId)
            .setBools(List.of(false))
            .setMdmParamId(KnownMdmParams.HONEST_SIGN_CIS_REQUIRED)
            .setXslName("cargoType980")
            .setMasterDataSourceType(MasterDataSourceType.AUTO);

        return new ArrayList<>(List.of(expectedMercuryCisOptional, expectedMercuryCisDistinct,
            expectedMercuryCisRequired,
            expectedHsCisOptional, expectedHsCisDistinct, expectedHsCisRequired));
    }

    private MskuParamValue randomMskuParamValue(long mskuId, long mdmParamId) {
        MskuParamValue result = new MskuParamValue().setMskuId(mskuId);
        TestMdmParamUtils.createRandomMdmParamValue(random, mdmParamCache.get(mdmParamId)).copyTo(result);
        return result;
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private SilverCommonSsku silverSskuWithShelfLife(ShopSkuKey key,
                                                     String sourceId,
                                                     MasterDataSourceType type,
                                                     Instant updatedTs,
                                                     int shelfLifePeriod,
                                                     boolean shelfLifeUnit,
                                                     String shelLifeComment,
                                                     Long masterDataVersion) {
        List<SskuSilverParamValue> result = new ArrayList<>();
        if (shelfLifePeriod > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.SHELF_LIFE);
            value.setXslName(paramCache.get(KnownMdmParams.SHELF_LIFE).getXslName());
            value.setNumeric(BigDecimal.valueOf(shelfLifePeriod));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (shelfLifeUnit) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT);
            value.setXslName(paramCache.get(KnownMdmParams.SHELF_LIFE_UNIT).getXslName());
            value.setOption(
                new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.YEAR)));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (shelLifeComment != null) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.SHELF_LIFE_COMMENT);
            value.setXslName(paramCache.get(KnownMdmParams.SHELF_LIFE_COMMENT).getXslName());
            value.setStrings(List.of(shelLifeComment));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        return TestDataUtils.wrapSilver(result);
    }
}
