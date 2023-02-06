package ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.MdmSskuGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MasterDataBusinessMergeService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.CommonSskuBuilder;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class MeasurementBizSplitMergeTest extends MdmBaseDbTestClass {
    @Autowired
    private StorageKeyValueService skv;
    @Autowired
    private MdmParamCache mdmParamCache;
    @Autowired
    private MasterDataBusinessMergeService service;

    @Before
    public void setup() {
        skv.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);
        skv.invalidateCache();
    }

    @Test
    public void testSplitHandleMergeValidMeasurementExistenceData() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, new ShopSkuKey(1, ""))
            .with(KnownMdmParams.HAS_MEASUREMENT_AFTER_INHERIT, true)
            .with(KnownMdmParams.LAST_MEASUREMENT_TIMESTAMP_AFTER_INHERIT, 100500L)
            .startServiceValues(11)
                .with(KnownMdmParams.MIN_SHIPMENT, 100L) // чтобы мёржер не отстрелил оффер как сироту
            .endServiceValues()
            .build();

        var group = groupize(ssku);

        Assertions.assertThat(service.merge(group)).isEqualTo(ssku);
    }

    @Test
    public void testSplitHandleMergeInvalidMeasurementExistenceData1() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, new ShopSkuKey(1, ""))
            .with(KnownMdmParams.HAS_MEASUREMENT_AFTER_INHERIT, true)
            .startServiceValues(11)
            .with(KnownMdmParams.MIN_SHIPMENT, 100L) // чтобы мёржер не отстрелил оффер как сироту
            .endServiceValues()
            .build();

        var group = groupize(ssku);

        Assertions.assertThat(service.merge(group).getBaseParamIds()).isEmpty();
    }

    @Test
    public void testSplitHandleMergeInvalidMeasurementExistenceData2() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, new ShopSkuKey(1, ""))
            .with(KnownMdmParams.HAS_MEASUREMENT_AFTER_INHERIT, false)
            .with(KnownMdmParams.LAST_MEASUREMENT_TIMESTAMP_AFTER_INHERIT, 100500L)
            .startServiceValues(11)
            .with(KnownMdmParams.MIN_SHIPMENT, 100L) // чтобы мёржер не отстрелил оффер как сироту
            .endServiceValues()
            .build();

        var group = groupize(ssku);

        Assertions.assertThat(service.merge(group).getBaseParamIds()).isEmpty();
    }

    @Test
    public void testSplitHandleMergeInvalidMeasurementExistenceData3() {
        CommonSsku ssku = new CommonSskuBuilder(mdmParamCache, new ShopSkuKey(1, ""))
            .with(KnownMdmParams.HAS_MEASUREMENT_BEFORE_INHERIT, true)
            .with(KnownMdmParams.LAST_MEASUREMENT_TIMESTAMP_BEFORE_INHERIT, 100500L)
            .startServiceValues(11)
            .with(KnownMdmParams.MIN_SHIPMENT, 100L) // чтобы мёржер не отстрелил оффер как сироту
            .endServiceValues()
            .build();

        var group = groupize(ssku);

        Assertions.assertThat(service.merge(group).getBaseParamIds()).isEmpty();
    }

    private static MdmSskuGroup groupize(CommonSsku ssku) {
        return MdmSskuGroup.createBusinessGroup(ssku.getBaseSsku(), ssku.getServiceSskus().values());
    }
}
