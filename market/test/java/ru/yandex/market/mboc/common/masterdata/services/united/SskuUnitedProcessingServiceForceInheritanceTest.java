package ru.yandex.market.mboc.common.masterdata.services.united;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.CommonSskuBuilder;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class SskuUnitedProcessingServiceForceInheritanceTest extends UnitedProcessingServiceSetupBaseTest {

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
        mappingsCacheRepository.insertBatch(
            new MappingCacheDao().setCategoryId(categoryId)
                .setShopSkuKey(businessKey)
                .setMskuId(mskuId)
        );

        // silver
        SilverCommonSsku silverSsku =
            createSilverSsku(businessKey, "10", "10", "10", "1", "vasya", MasterDataSourceType.SUPPLIER, Instant.now());
        silverSskuRepository.insertOrUpdateSsku(silverSsku);

        // msku
        // на самом деле не важно, какие тут ВГХ, т.к. в вычислении возьмутся реальные данные от источников, а эти
        // подвешенные в воздухе МСКУшные ВГХ из ниоткуда - исчезнут.
        CommonMsku msku =
            createMsku(mskuId, "1500", "9000", "3.1415", "1.5", "petya", MasterDataSourceType.SUPPLIER, Instant.now());
        mskuRepository.insertOrUpdateMsku(msku);

        raiseForceInheritanceSwitches(categoryId);

        // when
        processShopSkuKeys(List.of(businessKey));

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
    public void whenHaveNoMappingsUseOwnVghAsGold() {
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

        // valid silver
        SilverCommonSsku silverSsku =
            createSilverSsku(businessKey, "10", "10", "10", "1", "vasya", MasterDataSourceType.SUPPLIER, Instant.now());
        silverSskuRepository.insertOrUpdateSsku(silverSsku);

        raiseInterGoldSwitches();

        // when
        processShopSkuKeys(List.of(businessKey));

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
        processShopSkuKeys(List.of(businessKey));

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
    public void ifNoVghInSilverShouldDropItFromAllLayers() {
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

        // Обратите внимание, что нет серебра ВГХ на ССКУ, а есть только "старые" данные на МСКУ. Такое может быть
        // после смены маппинга, когда ВГХносная ССКУшка исчезла из группы.

        // В этом случае выходит, что те ВГХ, что на МСКУ, уже неактуальны, т.к. за ними нет реального источника.
        // Поэтому они должны исчезнуть и вся группа должна остаться без ВГХ.

        // when
        processShopSkuKeys(List.of(businessKey));

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
        Assertions.assertThat(shippingUnit).isNull();

        msku = mskuRepository.findMsku(mskuId).get();
        Assertions.assertThat(msku.getContainedMdmParamIds()).doesNotContain(
            KnownMdmParams.WIDTH, KnownMdmParams.HEIGHT, KnownMdmParams.LENGTH, KnownMdmParams.WEIGHT_GROSS);

        Map<ShopSkuKey, CommonSsku> goldByKeys =
            goldSskuRepository.findSskus(List.of(serviceKey, businessKey));
        Assertions.assertThat(goldByKeys).isEmpty();
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
        var source = new MasterDataSource(MasterDataSourceType.SUPPLIER, "petya");
        String shopSku = "U238";
        ShopSkuKey businessKey = new ShopSkuKey(businessId, shopSku);
        ShopSkuKey serviceKey = new ShopSkuKey(serviceId, shopSku);
        sskuExistenceRepository.markExistence(serviceKey, true);

        var ssku = new CommonSskuBuilder(mdmParamCache, businessKey)
            .withVghAfterInheritance(15, 15, 15, 2)
            .build();
        silverSskuRepository.insertOrUpdateSsku(SilverCommonSsku.fromCommonSsku(ssku, source));

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
        // можно даже не заполнять, она сама посчитается с поставщиковых ВГХ от дочерней ССКУ

        raiseForceInheritanceSwitches(categoryId);

        for (int i = 0; i < 5; i++) {
            // when
            processShopSkuKeys(List.of(businessKey));

            // then
            var msku = mskuRepository.findMsku(mskuId).get();
            Assertions.assertThat(msku.getParamValue(KnownMdmParams.WIDTH).get().getNumeric().get().intValue())
                .isEqualTo(15);
            Assertions.assertThat(msku.getParamValue(KnownMdmParams.HEIGHT).get().getNumeric().get().intValue())
                .isEqualTo(15);
            Assertions.assertThat(msku.getParamValue(KnownMdmParams.LENGTH).get().getNumeric().get().intValue())
                .isEqualTo(15);
            Assertions.assertThat(msku.getParamValue(KnownMdmParams.WEIGHT_GROSS).get().getNumeric().get().intValue())
                .isEqualTo(2);

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
            Assertions.assertThat(paramValues).hasSize(8);

            // final inherited vgh
            Assertions.assertThat(paramValues.get(KnownMdmParams.LENGTH).getNumeric())
                .isEqualTo(msku.getParamValue(KnownMdmParams.LENGTH).flatMap(MdmParamValue::getNumeric));
            Assertions.assertThat(paramValues.get(KnownMdmParams.WIDTH).getNumeric())
                .isEqualTo(msku.getParamValue(KnownMdmParams.WIDTH).flatMap(MdmParamValue::getNumeric));
            Assertions.assertThat(paramValues.get(KnownMdmParams.HEIGHT).getNumeric())
                .isEqualTo(msku.getParamValue(KnownMdmParams.HEIGHT).flatMap(MdmParamValue::getNumeric));
            Assertions.assertThat(paramValues.get(KnownMdmParams.WEIGHT_GROSS).getNumeric())
                .isEqualTo(msku.getParamValue(KnownMdmParams.WEIGHT_GROSS).flatMap(MdmParamValue::getNumeric));

            // intermediate supplier vgh
            Assertions.assertThat(paramValues.get(KnownMdmParams.SSKU_LENGTH).getNumeric())
                .isEqualTo(msku.getParamValue(KnownMdmParams.LENGTH).flatMap(MdmParamValue::getNumeric));
            Assertions.assertThat(paramValues.get(KnownMdmParams.SSKU_WIDTH).getNumeric())
                .isEqualTo(msku.getParamValue(KnownMdmParams.WIDTH).flatMap(MdmParamValue::getNumeric));
            Assertions.assertThat(paramValues.get(KnownMdmParams.SSKU_HEIGHT).getNumeric())
                .isEqualTo(msku.getParamValue(KnownMdmParams.HEIGHT).flatMap(MdmParamValue::getNumeric));
            Assertions.assertThat(paramValues.get(KnownMdmParams.SSKU_WEIGHT_GROSS).getNumeric())
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

    private void raiseInterGoldSwitches() {
        storageKeyValueService.putValue(MdmProperties.WRITE_GOLD_WD_TO_SSKU_GOLD_TABLE_GLOBALLY, true);
        storageKeyValueService.putValue(MdmProperties.WRITE_OWN_SSKU_WD_GLOBALLY, true);
        storageKeyValueService.putValue(MdmProperties.USE_OWN_SSKU_WD_FOR_MSKU_GOLD_GLOBALLY, true);
        storageKeyValueService.invalidateCache();
    }
}
