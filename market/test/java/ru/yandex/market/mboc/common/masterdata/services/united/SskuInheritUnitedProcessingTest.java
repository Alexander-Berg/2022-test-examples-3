package ru.yandex.market.mboc.common.masterdata.services.united;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class SskuInheritUnitedProcessingTest extends UnitedProcessingServiceSetupBaseTest {

    @Test
    public void shouldInheritNewMskuVghToAllRelatedSskus() {
        storageKeyValueService.putValue(MdmProperties.WRITE_GOLD_WD_TO_SSKU_GOLD_TABLE_GLOBALLY, true);
        storageKeyValueService.putValue(MdmProperties.WRITE_OWN_SSKU_WD_GLOBALLY, true);
        storageKeyValueService.putValue(MdmProperties.USE_OWN_SSKU_WD_FOR_MSKU_GOLD_GLOBALLY, true);
        storageKeyValueService.invalidateCache();

        // given
        long initialTs = 0L;
        var modelId = 111L;

        var business1 = new ShopSkuKey(1000, "firstSku");
        var service1 = new ShopSkuKey(1001, "firstSku");
        prepareBusinessGroup(business1.getSupplierId(), service1.getSupplierId());
        addMappings(modelId, business1);
        addMappings(modelId, service1);

        var business2 = new ShopSkuKey(2000, "secondSku");
        var service2 = new ShopSkuKey(2001, "secondSku");
        prepareBusinessGroup(business2.getSupplierId(), service2.getSupplierId());
        addMappings(modelId, business2);
        addMappings(modelId, service2);

        sskuExistenceRepository.markExistence(List.of(service1, service2), true);

        // Only first ssku has weight
        FromIrisItemWrapper service1Iris = new FromIrisItemWrapper(ItemWrapperTestUtil.createItem(
            service1, MdmIrisPayload.MasterDataSource.SUPPLIER,
            ItemWrapperTestUtil.generateShippingUnit(10.0, 11.0, 12.0, 1.0, null, null, initialTs)));
        fromIrisItemRepository.insert(service1Iris);

        // First ssku weight saved on msku
        var initialMsku = new CommonMsku(modelId, createGoldDimensions(modelId, 10, 11, 12, 1,
            MasterDataSourceType.SUPPLIER, "supplier_id:1000 shop_sku:firstSku", initialTs));
        mskuRepository.insertOrUpdateMsku(initialMsku);

        // new weight
        long updatedTs = 100L;
        var updatedEoxBusiness1 = silverSskuWithVgh(business1, "supplier", MasterDataSourceType.SUPPLIER,
            Instant.ofEpochSecond(updatedTs),
            20, 21, 22, 2, 2L);
        silverSskuRepository.insertOrUpdateSsku(updatedEoxBusiness1);

        // when process old business
        processShopSkuKeys(List.of(business1));

        // then
        // new weight should be propagated to not queued ssku
        var calculatedBusiness2RefItem = referenceItemRepository.findById(service2);
        Assertions.assertThat(calculatedBusiness2RefItem.getCombinedItemShippingUnit().getWeightGrossMg().getValue())
            .isEqualTo(2_000_000L);
        Assertions.assertThat(calculatedBusiness2RefItem.getCombinedItemShippingUnit().getLengthMicrometer().getValue())
            .isEqualTo(200_000L);
        Assertions.assertThat(calculatedBusiness2RefItem.getCombinedItemShippingUnit().getHeightMicrometer().getValue())
            .isEqualTo(210_000L);
        Assertions.assertThat(calculatedBusiness2RefItem.getCombinedItemShippingUnit().getWidthMicrometer().getValue())
            .isEqualTo(220_000L);
    }

    private SilverCommonSsku silverSskuWithVgh(ShopSkuKey key,
                                               String sourceId,
                                               MasterDataSourceType type,
                                               Instant updatedTs,
                                               int length,
                                               int height,
                                               int width,
                                               int weightGross,
                                               Long masterDataVersion) {
        List<SskuSilverParamValue> result = new ArrayList<>();
        if (length > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.LENGTH);
            value.setXslName(paramCache.get(KnownMdmParams.LENGTH).getXslName());
            value.setNumeric(BigDecimal.valueOf(length));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (height > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.HEIGHT);
            value.setXslName(paramCache.get(KnownMdmParams.HEIGHT).getXslName());
            value.setNumeric(BigDecimal.valueOf(height));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (width > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.WIDTH);
            value.setXslName(paramCache.get(KnownMdmParams.WIDTH).getXslName());
            value.setNumeric(BigDecimal.valueOf(width));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (weightGross > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.WEIGHT_GROSS);
            value.setXslName(paramCache.get(KnownMdmParams.WEIGHT_GROSS).getXslName());
            value.setNumeric(BigDecimal.valueOf(weightGross));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (masterDataVersion != null && masterDataVersion > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION)
                .setXslName(paramCache.get(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION).getXslName())
                .setNumeric(BigDecimal.valueOf(masterDataVersion));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        return TestDataUtils.wrapSilver(result);
    }

    private List<MskuParamValue> createGoldDimensions(long modelId,
                                                      double length, double width, double height, double weightGross,
                                                      MasterDataSourceType sourceType, String sourceId,
                                                      long timestamp) {
        MskuParamValue lengthPV =
            createNumericParamValue(modelId, length, KnownMdmParams.LENGTH, sourceType, sourceId, timestamp);
        MskuParamValue widthPV =
            createNumericParamValue(modelId, width, KnownMdmParams.WIDTH, sourceType, sourceId, timestamp);
        MskuParamValue heightPV =
            createNumericParamValue(modelId, height, KnownMdmParams.HEIGHT, sourceType, sourceId, timestamp);
        MskuParamValue weightGrossPV =
            createNumericParamValue(modelId, weightGross, KnownMdmParams.WEIGHT_GROSS, sourceType, sourceId, timestamp);
        return List.of(lengthPV, widthPV, heightPV, weightGrossPV);
    }

    private MskuParamValue createNumericParamValue(long modelId, double value, long mdmParamId,
                                                   MasterDataSourceType sourceType,
                                                   String sourceId, long timestamp) {
        MskuParamValue mskuParamValue = new MskuParamValue().setMskuId(modelId);
        mskuParamValue.setMdmParamId(mdmParamId)
            .setNumeric(BigDecimal.valueOf(value))
            .setMasterDataSourceType(sourceType)
            .setMasterDataSourceId(sourceId)
            .setXslName(mdmParamCache.get(mdmParamId).getXslName())
            .setUpdatedTs(Instant.ofEpochMilli(timestamp));
        return mskuParamValue;
    }

    private void addMappings(long mskuId, ShopSkuKey offer) {

        MappingCacheDao mapping = new MappingCacheDao()
            .setSupplierId(offer.getSupplierId())
            .setShopSku(offer.getShopSku())
            .setMskuId(mskuId)
            .setCategoryId(0);
        mappingsCacheRepository.insert(mapping);
    }
}
