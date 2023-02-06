package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.ImpersonalSourceId;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;

public class SilverCommonSskuComparisonTest {

    @Test
    public void testSameSskuIsNotNewerThanItself() {
        var ssku = ssku(100500L, service(1, 100600L), service(2, 100600L));

        Assertions.assertThat(ssku.isPartiallyNewerThan(ssku)).isFalse();
    }

    @Test
    public void testIfBaseVersionIsGreaterThenSskuIsNewer() {
        var leftSsku = ssku(100501L, service(1, 0L));
        var rightSsku = ssku(100500L, service(1, 100600L), service(2, 100600L));

        Assertions.assertThat(leftSsku.isPartiallyNewerThan(rightSsku)).isTrue();
    }

    @Test
    public void testIfMoreServicesThenSskuIsNewer() {
        var leftSsku = ssku(100500L, service(1, 0L), service(2, 0L));
        var rightSsku = ssku(100501L, service(1, 100600L));

        Assertions.assertThat(leftSsku.isPartiallyNewerThan(rightSsku)).isTrue();
    }

    @Test
    public void testIfSomeServicesAppearInLeftSskuOnlyThenItIsNewer() {
        var leftSsku = ssku(100500L, service(1, 0L), service(2, 0L));
        var rightSsku = ssku(100501L, service(1, 100600L), service(3, 100600L));

        Assertions.assertThat(leftSsku.isPartiallyNewerThan(rightSsku)).isTrue();
    }

    @Test
    public void testIfAtLeastOneLeftServiceHasHigherVersionThenWholeSskuIsNewer() {
        var leftSsku = ssku(100500L, service(1, 0L), service(2, 0L), service(3, 100601L));
        var rightSsku = ssku(100501L, service(1, 100600L), service(2, 100600L), service(3, 100600L));

        Assertions.assertThat(leftSsku.isPartiallyNewerThan(rightSsku)).isTrue();
    }

    @Test
    public void testIfNoNewVersionsAndNoNewServicesThenSskuIsNotNewer() {
        var rightSsku = ssku(100600L, service(1, 100600L), service(2, 100600L), service(3, 100600L));

        Assertions.assertThat(ssku(100L, service(1, 0L), service(2, 0L), service(3, 0L))
            .isPartiallyNewerThan(rightSsku)).isFalse();
        Assertions.assertThat(ssku(100600L)
            .isPartiallyNewerThan(rightSsku)).isFalse();
        Assertions.assertThat(ssku(100600L, service(1, 100600L))
            .isPartiallyNewerThan(rightSsku)).isFalse();
    }

    private SilverServiceSsku service(int serviceId, long version) {
        SilverServiceSsku ssku = new SilverServiceSsku(new SilverSskuKey(
            serviceId, "shop_sku", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name()
        ));
        SskuSilverParamValue dcVersion = new SskuSilverParamValue();
        dcVersion.setMdmParamId(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION);
        dcVersion.setNumeric(BigDecimal.valueOf(version));
        ssku.addParamValue(dcVersion);
        return ssku;
    }

    private SilverCommonSsku ssku(long version, SilverServiceSsku... serviceSskus) {
        SilverCommonSsku ssku = new SilverCommonSsku(new SilverSskuKey(
            0, "shop_sku", MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name()
        ));
        SskuSilverParamValue dcVersion = new SskuSilverParamValue();
        dcVersion.setMdmParamId(KnownMdmParams.DATACAMP_MASTER_DATA_VERSION);
        dcVersion.setNumeric(BigDecimal.valueOf(version));
        ssku.addBaseValue(dcVersion);
        ssku.putServiceSskus(List.of(serviceSskus));
        return ssku;
    }
}
