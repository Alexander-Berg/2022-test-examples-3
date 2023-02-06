package ru.yandex.market.mbo.mdm.tms.executors;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.core.tax.model.VatRate;
import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuGoldenParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;


/**
 * @author albina-gima
 * @date 5/25/21
 */
public class RecomputeMskuGoldExecutorMasterDataSourceTest extends RecomputeMskuGoldExecutorBaseTest {
    private static final String WAREHOUSE_ID = "172";

   @Test
    public void whenSskuGoldenParamValuesHaveMeasurementSourceThenIgnoreCategoryLimitsForMskuGold() {
        int supplierId = 565773;
        String shopSku = "061-SB";
        int categoryId = 13626675;
        long mskuId = 100810476172L;
        ShopSkuKey offer = new ShopSkuKey(supplierId, shopSku);
        ModelKey modelKey = new ModelKey(categoryId, mskuId);

        double expectedMskuGoldLength = 1.0;
        double expectedMskuGoldWidth = 32.0;
        double expectedMskuGoldHeight = 32.0;
        double expectedMskuGoldWeightGross = 0.36;
        double expectedMskuGoldWeightNet = 0.4;

        addFlagsForCategory(categoryId);

        addMappingsAndSupplier(categoryId, mskuId, offer);

        addCategorySettings(modelKey);

        addMskuParamValues(modelKey);

        addSskuGoldenParamValues(offer, expectedMskuGoldLength, expectedMskuGoldWidth, expectedMskuGoldHeight,
            expectedMskuGoldWeightGross, expectedMskuGoldWeightNet);

        addMasterDataForSsku(offer);

        // Запускаем.
        mskuQueue.enqueue(modelKey.getModelId(), MdmEnqueueReason.CHANGED_BY_MBO_OPERATOR);
        executor.execute();

        Map<Long, CommonMsku> mskus = mskuRepository.findAllMskus();
        Assertions.assertThat(mskus).hasSize(1);
        CommonMsku msku = mskus.get(mskuId);
        Assertions.assertThat(msku).isNotNull();

        Assertions.assertThat(msku.getParamValue(KnownMdmParams.LENGTH))
            .hasValueSatisfying(value -> checkNumericValueAndSource(
                value,
                BigDecimal.valueOf((long) expectedMskuGoldLength),
                MasterDataSourceType.MEASUREMENT
                )
            );
        Assertions.assertThat(msku.getParamValue(KnownMdmParams.WIDTH))
            .hasValueSatisfying(value -> checkNumericValueAndSource(
                value,
                BigDecimal.valueOf((long) expectedMskuGoldWidth),
                MasterDataSourceType.MEASUREMENT
                )
            );
        Assertions.assertThat(msku.getParamValue(KnownMdmParams.HEIGHT))
            .hasValueSatisfying(value -> checkNumericValueAndSource(
                value,
                BigDecimal.valueOf((long) expectedMskuGoldHeight),
                MasterDataSourceType.MEASUREMENT
                )
            );
        Assertions.assertThat(msku.getParamValue(KnownMdmParams.WEIGHT_GROSS))
            .hasValueSatisfying(value -> checkNumericValueAndSource(
                value,
                BigDecimal.valueOf(expectedMskuGoldWeightGross),
                MasterDataSourceType.MEASUREMENT
                )
            );
        Assertions.assertThat(msku.getParamValue(KnownMdmParams.WEIGHT_NET))
            .hasValueSatisfying(value -> checkNumericValueAndSource(
                value,
                BigDecimal.valueOf(expectedMskuGoldWeightNet),
                MasterDataSourceType.WAREHOUSE
                )
            );
    }

    private void addFlagsForCategory(int categoryId) {
        storageKeyValueService.putValue(MdmProperties.CATEGORIES_TO_USE_OWN_SSKU_WD_FOR_MSKU_GOLD,
            List.of(categoryId));
    }

    private void addMappingsAndSupplier(int categoryId, long mskuId, ShopSkuKey offer) {
        int businessSupplierId = 920011;

        MappingCacheDao mapping = new MappingCacheDao()
            .setSupplierId(offer.getSupplierId())
            .setShopSku(offer.getShopSku())
            .setMskuId(mskuId)
            .setCategoryId(categoryId);
        mappingsCacheRepository.insert(mapping);

        MdmSupplier businessSupplier = new MdmSupplier()
            .setId(businessSupplierId)
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier blueSupplier = new MdmSupplier()
            .setId(offer.getSupplierId())
            .setBusinessId(businessSupplierId)
            .setBusinessEnabled(false)
            .setType(MdmSupplierType.THIRD_PARTY);
        mdmSupplierRepository.insertBatch(businessSupplier, blueSupplier);
    }

    private void addSskuGoldenParamValues(ShopSkuKey offer,
                                          double expectedGoldLength,
                                          double expectedGoldWidth,
                                          double expectedGoldHeight,
                                          double expectedGoldWeightGross,
                                          double expectedGoldWeightNet) {
        ReferenceItemWrapper goldenItemCommon = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            offer, MdmIrisPayload.MasterDataSource.WAREHOUSE, WAREHOUSE_ID, ItemWrapperTestUtil.generateShippingUnit(
                31.0, 32.0, 5.0, 0.4, 0.4, null,
                1234L)));
        List<SskuGoldenParamValue> goldenParamValues = sskuGoldenParamUtil.createSskuGoldenParamValuesFromReferenceItem(
            goldenItemCommon, SskuGoldenParamUtil.ParamsGroup.COMMON);

        ReferenceItemWrapper goldenItemSsku = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            offer, MdmIrisPayload.MasterDataSource.MEASUREMENT, WAREHOUSE_ID, ItemWrapperTestUtil.generateShippingUnit(
                expectedGoldLength, expectedGoldWidth, expectedGoldHeight, expectedGoldWeightGross, null, null,
                123456L)));
        List<SskuGoldenParamValue> goldenParamValuesSsku =
            sskuGoldenParamUtil.createSskuGoldenParamValuesFromReferenceItem(
                goldenItemSsku, SskuGoldenParamUtil.ParamsGroup.SSKU);

        ReferenceItemWrapper goldenItemSskuWH = new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(
            offer, MdmIrisPayload.MasterDataSource.WAREHOUSE, WAREHOUSE_ID, ItemWrapperTestUtil.generateShippingUnit(
                null, null, null, null, expectedGoldWeightNet, null,
                12345678L)));
        List<SskuGoldenParamValue> goldenParamValuesWH =
            sskuGoldenParamUtil.createSskuGoldenParamValuesFromReferenceItem(
                goldenItemSskuWH, SskuGoldenParamUtil.ParamsGroup.SSKU);

        CommonSsku commonSsku = new CommonSsku(offer);
        Stream.of(goldenParamValues, goldenParamValuesSsku, goldenParamValuesWH)
            .flatMap(List::stream)
            .forEach(commonSsku::addBaseValue);
        goldSskuRepository.insertOrUpdateSsku(commonSsku);
    }

    private void addMasterDataForSsku(ShopSkuKey offer) {
        MasterData md = new MasterData();
        md.setShopSkuKey(offer);
        md.setVat(VatRate.fromString("7").orElseThrow());
        md.setGtins(List.of("6930145000617"));
        md.setMinShipment(0);
        md.setDeliveryTime(0);
        md.setManufacturer("Белоснежка");
        md.setQuantityInPack(0);
        md.setQuantumOfSupply(0);
        md.setShelfLifeRequired(false);
        md.setTransportUnitSize(0);
        md.setManufacturerCountries(List.of("Россия"));
        masterDataRepository.insert(md);
    }

    private void addMskuParamValues(ModelKey modelKey) {
        MskuParamValue weightNetto = new MskuParamValue().setMskuId(modelKey.getModelId());
        weightNetto.setNumeric(BigDecimal.valueOf(0.4));
        weightNetto.setXslName("mdm_weight_net");
        weightNetto.setMdmParamId(KnownMdmParams.WEIGHT_NET);
        weightNetto.setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);

        MskuParamValue expirDateApply = new MskuParamValue().setMskuId(modelKey.getModelId());
        expirDateApply.setBool(false);
        expirDateApply.setXslName("expir_date");
        expirDateApply.setMdmParamId(KnownMdmParams.EXPIR_DATE);
        expirDateApply.setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);

        MskuParamValue kgt = new MskuParamValue().setMskuId(modelKey.getModelId());
        kgt.setBool(false);
        kgt.setXslName("cargoType300");
        kgt.setMdmParamId(KnownMdmParams.HEAVY_GOOD);
        kgt.setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);

        MskuParamValue kgt20 = new MskuParamValue().setMskuId(modelKey.getModelId());
        kgt20.setBool(false);
        kgt20.setXslName("cargoType301");
        kgt20.setMdmParamId(KnownMdmParams.HEAVY_GOOD_20);
        kgt20.setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);

        mskuRepository.insertOrUpdateMsku(new CommonMsku(modelKey, List.of(weightNetto, expirDateApply, kgt, kgt20)));
    }

    private void addCategorySettings(ModelKey modelKey) {
        CategoryParamValue maxLimitShortSide = new CategoryParamValue().setCategoryId(modelKey.getCategoryId());
        maxLimitShortSide.setMdmParamId(KnownMdmParams.SIZE_SHORT_MAX_CM);
        maxLimitShortSide.setNumeric(BigDecimal.valueOf(30));
        maxLimitShortSide.setXslName("maxLimitShortSide");
        maxLimitShortSide.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        CategoryParamValue minLimitWeight = new CategoryParamValue().setCategoryId(modelKey.getCategoryId());
        minLimitWeight.setMdmParamId(KnownMdmParams.WEIGHT_MIN_KG);
        minLimitWeight.setNumeric(BigDecimal.valueOf(0.001));
        minLimitWeight.setXslName("minLimitWeight");
        minLimitWeight.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        CategoryParamValue maxLimitWeight = new CategoryParamValue().setCategoryId(modelKey.getCategoryId());
        maxLimitWeight.setMdmParamId(KnownMdmParams.WEIGHT_MAX_KG);
        maxLimitWeight.setNumeric(BigDecimal.valueOf(10));
        maxLimitWeight.setXslName("maxLimitWeight");
        maxLimitWeight.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        CategoryParamValue minLimitLongSide = new CategoryParamValue().setCategoryId(modelKey.getCategoryId());
        minLimitLongSide.setMdmParamId(KnownMdmParams.SIZE_LONG_MIN_CM);
        minLimitLongSide.setNumeric(BigDecimal.valueOf(0.1));
        minLimitLongSide.setXslName("minLimitLongSide");
        minLimitLongSide.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        CategoryParamValue maxLimitLongSide = new CategoryParamValue().setCategoryId(modelKey.getCategoryId());
        maxLimitLongSide.setMdmParamId(KnownMdmParams.SIZE_LONG_MAX_CM);
        maxLimitLongSide.setNumeric(BigDecimal.valueOf(50));
        maxLimitLongSide.setXslName("maxLimitLongSide");
        maxLimitLongSide.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        CategoryParamValue minLimitMiddleSide = new CategoryParamValue().setCategoryId(modelKey.getCategoryId());
        minLimitMiddleSide.setMdmParamId(KnownMdmParams.SIZE_MIDDLE_MIN_CM);
        minLimitMiddleSide.setNumeric(BigDecimal.valueOf(0.1));
        minLimitMiddleSide.setXslName("minLimitMiddleSide");
        minLimitMiddleSide.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        CategoryParamValue maxLimitMiddleSide = new CategoryParamValue().setCategoryId(modelKey.getCategoryId());
        maxLimitMiddleSide.setMdmParamId(KnownMdmParams.SIZE_MIDDLE_MAX_CM);
        maxLimitMiddleSide.setNumeric(BigDecimal.valueOf(30));
        maxLimitMiddleSide.setXslName("maxLimitMiddleSide");
        maxLimitMiddleSide.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        CategoryParamValue minLimitShortSide = new CategoryParamValue().setCategoryId(modelKey.getCategoryId());
        minLimitShortSide.setMdmParamId(KnownMdmParams.SIZE_SHORT_MIN_CM);
        minLimitShortSide.setNumeric(BigDecimal.valueOf(0.1));
        minLimitShortSide.setXslName("minLimitShortSide");
        minLimitShortSide.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        CategoryParamValue expirationDatesApply = new CategoryParamValue().setCategoryId(modelKey.getCategoryId());
        expirationDatesApply.setMdmParamId(KnownMdmParams.EXPIRATION_DATES_APPLY);
        expirationDatesApply.setOption(new MdmParamOption().setId(3).setRenderedValue("не применим"));
        expirationDatesApply.setXslName("ExpirationDatesApply");
        expirationDatesApply.setMasterDataSourceType(MasterDataSourceType.MDM_ADMIN);

        categoryParamValueRepository.insertBatch(maxLimitShortSide, minLimitWeight, maxLimitWeight,
            minLimitLongSide, maxLimitLongSide, minLimitMiddleSide, maxLimitMiddleSide, minLimitShortSide,
            expirationDatesApply);
    }

    private void checkNumericValueAndSource(MskuParamValue mpv,
                                            BigDecimal expectedValue,
                                            MasterDataSourceType expectedSourceType) {
        Assertions.assertThat(mpv.getNumeric()).hasValue(expectedValue);
        Assertions.assertThat(mpv.getModificationInfo().getMasterDataSourceType()).isEqualTo(expectedSourceType);
    }
}
