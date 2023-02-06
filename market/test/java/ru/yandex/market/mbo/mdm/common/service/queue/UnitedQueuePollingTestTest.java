package ru.yandex.market.mbo.mdm.common.service.queue;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.ImpersonalSourceId;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.BatchProcessingProperties;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.CommonSskuBuilder;
import ru.yandex.market.mboc.common.masterdata.services.united.UnitedProcessingServiceSetupBaseTest;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class UnitedQueuePollingTestTest extends UnitedProcessingServiceSetupBaseTest {
    private static final int BUSINESS_ID = 12;
    private static final int SERVICE_ID = 13;
    private static final ShopSkuKey KEY = new ShopSkuKey(BUSINESS_ID, "ooo");
    private static final ShopSkuKey SERVICE_KEY = new ShopSkuKey(SERVICE_ID, "ooo");
    private static final long MSKU_ID = 92;
    private static final MappingCacheDao MAPPING = new MappingCacheDao().setCategoryId(1).setMskuId(MSKU_ID)
        .setShopSkuKey(KEY);

    private MskuAndSskuQueueProcessingService service;

    @Before
    @Override
    public void setup() {
        storageKeyValueService.putValue(MdmProperties.WRITE_GOLD_WD_TO_SSKU_GOLD_TABLE_GLOBALLY, true);
        storageKeyValueService.putValue(MdmProperties.WRITE_OWN_SSKU_WD_GLOBALLY, true);
        storageKeyValueService.putValue(MdmProperties.USE_OWN_SSKU_WD_FOR_MSKU_GOLD_GLOBALLY, true);
        storageKeyValueService.putValue(MdmProperties.APPLY_FORCE_INHERITANCE_GLOBALLY, true);
        storageKeyValueService.invalidateCache();
        super.setup();

        MdmSupplier business = new MdmSupplier()
            .setId(BUSINESS_ID)
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier serviceSupplier = new MdmSupplier()
            .setId(SERVICE_ID)
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS_ID)
            .setBusinessEnabled(true);
        mdmSupplierRepository.insertOrUpdateAll(List.of(business, serviceSupplier));
        sskuExistenceRepository.markExistence(SERVICE_KEY, true);
        mdmSupplierCachingService.refresh();

        service = new MskuAndSskuQueueProcessingService(
            storageKeyValueService, unitedProcessingService, mskuAndSskuQueue
        );
    }

    @Test
    public void testUnitedPollingOnOrphanSsku() {
        // given
        var supplierSource = new MasterDataSource(MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name());
        var sskuWithFinalVgh = new CommonSskuBuilder(mdmParamCache, KEY)
            .withVghAfterInheritance(10, 11, 12, 1)
            .customized(v -> v.setMasterDataSource(supplierSource))
            .build();
        var silverSsku = SilverCommonSsku.fromCommonSsku(sskuWithFinalVgh, supplierSource);
        silverSskuRepository.insertOrUpdateSsku(silverSsku);

        // when
        mskuAndSskuQueue.enqueueSsku(KEY, MdmEnqueueReason.CHANGED_SSKU_SILVER_DATA, 1);
        service.pollAndCompute(new BatchProcessingProperties.BatchProcessingPropertiesBuilder()
            .requeueFailed(true)
            .build());

        // then
        Assertions.assertThat(mskuAndSskuQueue.getUnprocessedItemsCount()).isZero();

        var interGold = goldSskuRepository.findSsku(KEY).get();
        for (long mdmParamId : KnownMdmParams.MANDATORY_WEIGHT_DIMENSIONS_PARAMS) {
            Assertions.assertThat(
                interGold.getBaseValue(mdmParamId).get().valueEquals(
                    silverSsku.getBaseValue(mdmParamId).get())).isTrue();
        }

        var finalGold = serviceSskuConverter.toServiceSsku(referenceItemRepository.findById(SERVICE_KEY));
        for (long mdmParamId : KnownMdmParams.MANDATORY_WEIGHT_DIMENSIONS_PARAMS) {
            Assertions.assertThat(
                finalGold.getParamValue(mdmParamId).get().valueEquals(
                    silverSsku.getBaseValue(mdmParamId).get())).isTrue();
        }
    }

    @Test
    public void testUnitedPollingOnOrphanMsku() {
        // given
        var msku = new CommonMsku(MAPPING.getMskuId(), List.of(
            numericValue(KnownMdmParams.WIDTH, 12),
            numericValue(KnownMdmParams.HEIGHT, 13),
            numericValue(KnownMdmParams.LENGTH, 14),
            numericValue(KnownMdmParams.WEIGHT_GROSS, 2)
        ));
        mskuRepository.insertOrUpdateMsku(msku);

        // when
        mskuAndSskuQueue.enqueueMsku(msku.getMskuId(), MdmEnqueueReason.CHANGED_MAPPING_EOX, 1);
        service.pollAndCompute(new BatchProcessingProperties.BatchProcessingPropertiesBuilder()
            .requeueFailed(true)
            .build());

        // then
        Assertions.assertThat(mskuAndSskuQueue.getUnprocessedItemsCount()).isZero();

        msku = mskuRepository.findMsku(msku.getMskuId()).get();
        for (long mdmParamId : KnownMdmParams.MANDATORY_WEIGHT_DIMENSIONS_PARAMS) {
            Assertions.assertThat(msku.getParamValue(mdmParamId)).isEmpty();
        }
    }

    @Test
    public void testUnitedPollingOnMskuAndSskuMapping() {
        mappingsCacheRepository.insert(MAPPING);

        // given
        var supplierSource = new MasterDataSource(MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name());
        var sskuWithFinalVgh = new CommonSskuBuilder(mdmParamCache, KEY)
            .withVghAfterInheritance(10, 11, 12, 1)
            .customized(v -> v.setMasterDataSource(supplierSource))
            .build();
        var silverSsku = SilverCommonSsku.fromCommonSsku(sskuWithFinalVgh, supplierSource);
        silverSskuRepository.insertOrUpdateSsku(silverSsku);

        // when
        mskuAndSskuQueue.enqueueSsku(KEY, MdmEnqueueReason.CHANGED_SSKU_SILVER_DATA, 1);
        service.pollAndCompute(new BatchProcessingProperties.BatchProcessingPropertiesBuilder()
            .requeueFailed(true)
            .build());

        // then
        Assertions.assertThat(mskuAndSskuQueue.getUnprocessedItemsCount()).isZero();

        var interGold = goldSskuRepository.findSsku(KEY).get();
        for (long mdmParamId : KnownMdmParams.MANDATORY_WEIGHT_DIMENSIONS_PARAMS) {
            Assertions.assertThat(
                interGold.getBaseValue(mdmParamId).get().valueEquals(
                    silverSsku.getBaseValue(mdmParamId).get())).isTrue();
        }

        var finalGold = serviceSskuConverter.toServiceSsku(referenceItemRepository.findById(SERVICE_KEY));
        for (long mdmParamId : KnownMdmParams.MANDATORY_WEIGHT_DIMENSIONS_PARAMS) {
            Assertions.assertThat(
                finalGold.getParamValue(mdmParamId).get().valueEquals(
                    silverSsku.getBaseValue(mdmParamId).get())).isTrue();
        }

        var msku = mskuRepository.findMsku(MAPPING.getMskuId()).get();
        for (long mdmParamId : KnownMdmParams.MANDATORY_WEIGHT_DIMENSIONS_PARAMS) {
            Assertions.assertThat(
                msku.getParamValue(mdmParamId).get().valueEquals(
                    silverSsku.getBaseValue(mdmParamId).get())).isTrue();
        }
    }

    private MskuParamValue numericValue(long mdmParamId, int value) {
        MskuParamValue mskuParamValue = new MskuParamValue().setMskuId(MAPPING.getMskuId());
        mskuParamValue.setMdmParamId(mdmParamId)
            .setNumeric(BigDecimal.valueOf(value))
            .setXslName(mdmParamCache.get(mdmParamId).getXslName());
        return mskuParamValue;
    }
}
