package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.ion.Decimal;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType.SUPPLIER;
import static ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue.SskuSilverTransportType.DATACAMP;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.GUARANTEE_PERIOD;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.GUARANTEE_PERIOD_UNIT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.GUARANTEE_PERIOD_UNLIMITED;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.LIFE_TIME;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.LIFE_TIME_UNIT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.LIFE_TIME_UNLIMITED;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SHELF_LIFE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SHELF_LIFE_UNIT;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SHELF_LIFE_UNLIMITED;

public class UnlimitedMdmParamProcessorTest extends MdmBaseDbTestClass {

    private static final MasterDataSource TEST_SOURCE = new MasterDataSource(SUPPLIER, "supplier");
    private static final long TEST_MD_VERSION = 99L;

    @Autowired
    private MdmParamCache cache;

    private UnlimitedMdmParamProcessor processor;

    @Before
    public void setup() {
        processor = new UnlimitedMdmParamProcessor(cache);
    }

    @Test
    public void shouldAddUnlimitedParamWhenLifeTimeIsUnlimited() {
        // given
        ShopSkuKey skuKey = new ShopSkuKey(999, "best");
        Map<Long, SskuSilverParamValue> lifeTimeValuesById = toMap(List.of(
            createSskuSilverParamValue(LIFE_TIME, "ShelfService", "1", skuKey), // дефолт для неограничен
            createSskuSilverParamValue(LIFE_TIME_UNIT, "ShelfService_Unit", new MdmParamOption(6L), skuKey)
        ));

        // when
        Map<Long, MdmParamValue> paramsWithUnlimitedParam =
            processor.processBeforeEntityBuilding(lifeTimeValuesById, SskuSilverParamValue::new);

        // then
        Assertions.assertThat(paramsWithUnlimitedParam).containsKey(LIFE_TIME_UNLIMITED);
        Assertions.assertThat(paramsWithUnlimitedParam).hasSize(1);
    }

    @Test
    public void shouldExpandUnlimitedParamWhenLifeTimeIsUnlimited() {
        // given
        ShopSkuKey skuKey = new ShopSkuKey(999, "best");
        Map<Long, SskuSilverParamValue> lifeTimeValuesById = toMap(List.of(
            createSskuSilverParamValue(LIFE_TIME_UNLIMITED, "mdm_life_time_unlimited", true, skuKey)
        ));

        // when
        Map<Long, MdmParamValue> paramsWithUnlimitedParam =
            processor.processAfterParamParsing(lifeTimeValuesById);

        // then
        Assertions.assertThat(paramsWithUnlimitedParam).containsKey(LIFE_TIME);
        Assertions.assertThat(paramsWithUnlimitedParam.get(LIFE_TIME).getString()).contains("1");
        Assertions.assertThat(paramsWithUnlimitedParam).containsKey(LIFE_TIME_UNIT);
        Assertions.assertThat(paramsWithUnlimitedParam.get(LIFE_TIME_UNIT).getOption())
            .contains(new MdmParamOption(6L));
        Assertions.assertThat(paramsWithUnlimitedParam).hasSize(2);
    }

    @Test
    public void shouldAddUnlimitedParamWhenShelfTimeIsUnlimited() {
        // given
        ShopSkuKey skuKey = new ShopSkuKey(999, "best");
        Map<Long, SskuSilverParamValue> shelfTimeValuesById = toMap(List.of(
            createSskuSilverParamValue(SHELF_LIFE, "LifeShelf", 1, skuKey), // дефолт для неограничен
            createSskuSilverParamValue(SHELF_LIFE_UNIT, "ShelfLife_Unit", new MdmParamOption(6L), skuKey)
        ));

        // when
        Map<Long, MdmParamValue> paramsWithUnlimitedParam =
            processor.processBeforeEntityBuilding(shelfTimeValuesById, SskuSilverParamValue::new);

        // then
        Assertions.assertThat(paramsWithUnlimitedParam).containsKey(SHELF_LIFE_UNLIMITED);
        Assertions.assertThat(paramsWithUnlimitedParam).hasSize(1);
    }

    @Test
    public void shouldExpandUnlimitedParamWhenShelfTimeIsUnlimited() {
        // given
        ShopSkuKey skuKey = new ShopSkuKey(999, "best");
        Map<Long, SskuSilverParamValue> lifeTimeValuesById = toMap(List.of(
            createSskuSilverParamValue(SHELF_LIFE_UNLIMITED, "mdm_shelf_life_unlimited", true, skuKey)
        ));

        // when
        Map<Long, MdmParamValue> paramsWithUnlimitedParam =
            processor.processAfterParamParsing(lifeTimeValuesById);

        // then
        Assertions.assertThat(paramsWithUnlimitedParam).containsKey(SHELF_LIFE);
        Assertions.assertThat(paramsWithUnlimitedParam.get(SHELF_LIFE).getNumeric()).contains(Decimal.valueOf(1));
        Assertions.assertThat(paramsWithUnlimitedParam).containsKey(SHELF_LIFE_UNIT);
        Assertions.assertThat(paramsWithUnlimitedParam.get(SHELF_LIFE_UNIT).getOption())
            .contains(new MdmParamOption(6L));
        Assertions.assertThat(paramsWithUnlimitedParam).hasSize(2);
    }

    @Test
    public void shouldAddUnlimitedParamWhenGuaranteeTimeIsUnlimited() {
        // given
        ShopSkuKey skuKey = new ShopSkuKey(999, "best");
        Map<Long, SskuSilverParamValue> shelfTimeValuesById = toMap(List.of(
            createSskuSilverParamValue(
                GUARANTEE_PERIOD, "WarrantyPeriod", "1", skuKey), // дефолт для неограничен
            createSskuSilverParamValue(
                GUARANTEE_PERIOD_UNIT, "WarrantyPeriod_Unit", new MdmParamOption(6L), skuKey)
        ));

        // when
        Map<Long, MdmParamValue> paramsWithUnlimitedParam =
            processor.processBeforeEntityBuilding(shelfTimeValuesById, SskuSilverParamValue::new);

        // then
        Assertions.assertThat(paramsWithUnlimitedParam).containsKey(GUARANTEE_PERIOD_UNLIMITED);
        Assertions.assertThat(paramsWithUnlimitedParam).hasSize(1);
    }

    @Test
    public void shouldExpandUnlimitedParamWhenGuaranteeTimeIsUnlimited() {
        // given
        ShopSkuKey skuKey = new ShopSkuKey(999, "best");
        Map<Long, SskuSilverParamValue> lifeTimeValuesById = toMap(List.of(
            createSskuSilverParamValue(
                GUARANTEE_PERIOD_UNLIMITED, "mdm_guarantee_period_unlimited", true, skuKey)
        ));

        // when
        Map<Long, MdmParamValue> paramsWithUnlimitedParam =
            processor.processAfterParamParsing(lifeTimeValuesById);

        // then
        Assertions.assertThat(paramsWithUnlimitedParam).containsKey(GUARANTEE_PERIOD);
        Assertions.assertThat(paramsWithUnlimitedParam.get(GUARANTEE_PERIOD).getString()).contains("1");
        Assertions.assertThat(paramsWithUnlimitedParam).containsKey(GUARANTEE_PERIOD_UNIT);
        Assertions.assertThat(paramsWithUnlimitedParam.get(GUARANTEE_PERIOD_UNIT).getOption())
            .contains(new MdmParamOption(6L));
        Assertions.assertThat(paramsWithUnlimitedParam).hasSize(2);
    }

    private Map<Long, SskuSilverParamValue> toMap(List<SskuSilverParamValue> lifeTimeValues) {
        return lifeTimeValues.stream().collect(Collectors.toMap(
            SskuSilverParamValue::getMdmParamId,
            Function.identity())
        );
    }

    private SskuSilverParamValue createSskuSilverParamValue(long paramId, String xslName,
                                                            Object value,
                                                            ShopSkuKey shopSkuKey) {
        return TestMdmParamUtils.createSskuSilverParamValue(paramId, xslName,
            value,
            TEST_SOURCE.getSourceType(), TEST_SOURCE.getSourceId(),
            shopSkuKey,
            TEST_MD_VERSION,
            DATACAMP);
    }
}
