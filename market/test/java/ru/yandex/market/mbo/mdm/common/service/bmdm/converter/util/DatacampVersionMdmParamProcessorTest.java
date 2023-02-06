package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCacheMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType.SUPPLIER;
import static ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue.SskuSilverTransportType.DATACAMP;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.DATACAMP_MASTER_DATA_VERSION;

public class DatacampVersionMdmParamProcessorTest {

    private static final MasterDataSource TEST_SOURCE = new MasterDataSource(SUPPLIER, "supplier");
    private static final long TEST_MD_VERSION = 99L;

    private final MdmParamCache cache = new MdmParamCacheMock(TestMdmParamUtils.createDefaultKnownMdmParams());

    private final DatacampVersionMdmParamProcessor processor = new DatacampVersionMdmParamProcessor(cache);

    @Test
    public void shouldAddDatacampVersionIfNotPresent() {
        // given
        ShopSkuKey skuKey = new ShopSkuKey(999, "best");
        var someParamWoVersion = TestMdmParamUtils.createSskuSilverParamValue(KnownMdmParams.HEIGHT,
            "mdm_height",
            100,
            TEST_SOURCE.getSourceType(), TEST_SOURCE.getSourceId(),
            skuKey,
            null,
            DATACAMP);

        SilverServiceSsku silverServiceSsku = new SilverServiceSsku();
        silverServiceSsku.setKey(new SilverSskuKey(skuKey, TEST_SOURCE));
        silverServiceSsku.setParamValues(List.of(someParamWoVersion));
        silverServiceSsku.setMasterDataVersion(TEST_MD_VERSION);

        // when
        Map<Long, ? extends MdmParamValue> result = processor
            .processBeforeEntityBuilding(silverServiceSsku.getValuesByParamId(), silverServiceSsku);

        // then
        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result).containsKey(DATACAMP_MASTER_DATA_VERSION);
        Assertions.assertThat(result.get(DATACAMP_MASTER_DATA_VERSION).getNumeric())
            .contains(new BigDecimal(TEST_MD_VERSION));
    }

    @Test
    public void shouldNotAddDatacampVersionIfNoSourceForVersion() {
        // given
        ShopSkuKey skuKey = new ShopSkuKey(999, "best");
        var someParamWoVersion = TestMdmParamUtils.createSskuSilverParamValue(KnownMdmParams.HEIGHT,
            "mdm_height",
            100,
            TEST_SOURCE.getSourceType(), TEST_SOURCE.getSourceId(),
            skuKey,
            null,
            DATACAMP);

        SilverServiceSsku silverServiceSsku = new SilverServiceSsku();
        silverServiceSsku.setKey(new SilverSskuKey(skuKey, TEST_SOURCE));
        silverServiceSsku.setParamValues(List.of(someParamWoVersion));

        // when
        Map<Long, ? extends MdmParamValue> result = processor
            .processBeforeEntityBuilding(silverServiceSsku.getValuesByParamId(), silverServiceSsku);

        // then
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result).doesNotContainKey(DATACAMP_MASTER_DATA_VERSION);
    }
}
