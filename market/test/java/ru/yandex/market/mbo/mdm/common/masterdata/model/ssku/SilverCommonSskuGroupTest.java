package ru.yandex.market.mbo.mdm.common.masterdata.model.ssku;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.DATACAMP_MASTER_DATA_VERSION;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.IS_REMOVED;

public class SilverCommonSskuGroupTest {

    private static final ShopSkuKey BASE_KEY = new ShopSkuKey(1, "someGreatStaff");
    private static final ShopSkuKey FIRST_SERVICE_KEY = new ShopSkuKey(2, "someGreatStaff");
    private static final ShopSkuKey SECOND_SERVICE_KEY = new ShopSkuKey(3, "someGreatStaff");

    private static final MasterDataSource SUPPLIER_SOURCE =
        new MasterDataSource(MasterDataSourceType.SUPPLIER, "0");
    private static final MasterDataSource DBS_SOURCE =
        new MasterDataSource(MasterDataSourceType.DBS, "0");
    private static final MasterDataSource WAREHOUSE_SOURCE =
        new MasterDataSource(MasterDataSourceType.WAREHOUSE, "99");

    @Test
    public void whenNoSupplierSilverWholeGroupDeleted() {
        // given
        var warehouseSilverBase = silverWithMdVersion(BASE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverService = silverWithMdVersion(FIRST_SERVICE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, WAREHOUSE_SOURCE))
            .setBaseSsku(warehouseSilverBase)
            .putServiceSsku(warehouseSilverService);
        var group = new SilverCommonSskuGroup(List.of(warehouseSilverCommon));

        // when
        var cleared = group.clearRemoved();

        // then
        assertThat(cleared.getRemoved()).containsExactly(warehouseSilverCommon);
        assertThat(cleared.getClearedWithUpdates()).isEmpty();
        assertThat(cleared.removedServiceKeys()).containsExactlyInAnyOrder(FIRST_SERVICE_KEY);
    }

    @Test
    public void whenSupplierSilverRemovedWholeGroupDeleted() {
        // given
        var warehouseSilverBase = silverWithMdVersion(BASE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverService = silverWithMdVersion(FIRST_SERVICE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, WAREHOUSE_SOURCE))
            .setBaseSsku(warehouseSilverBase)
            .putServiceSsku(warehouseSilverService);
        var supplierSilverBase = silverWithMdVersion(BASE_KEY, SUPPLIER_SOURCE, true);
        var supplierSilverService = silverWithMdVersion(FIRST_SERVICE_KEY, SUPPLIER_SOURCE, false);
        var supplierSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, SUPPLIER_SOURCE))
            .setBaseSsku(supplierSilverBase)
            .putServiceSsku(supplierSilverService);

        var group = new SilverCommonSskuGroup(List.of(warehouseSilverCommon, supplierSilverCommon));

        // when
        var cleared = group.clearRemoved();

        // then
        assertThat(cleared.getRemoved()).containsExactlyInAnyOrder(warehouseSilverCommon, supplierSilverCommon);
        assertThat(cleared.getClearedWithUpdates()).isEmpty();
        assertThat(cleared.removedServiceKeys()).containsExactlyInAnyOrder(FIRST_SERVICE_KEY);
    }

    @Test
    public void whenMostActualSupplierSilverRemovedWholeGroupDeleted() {
        // given
        var warehouseSilverBase = silverWithMdVersion(BASE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverService = silverWithMdVersion(FIRST_SERVICE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, WAREHOUSE_SOURCE))
            .setBaseSsku(warehouseSilverBase)
            .putServiceSsku(warehouseSilverService);
        var supplierSilverBase = silverWithMdVersion(BASE_KEY, SUPPLIER_SOURCE, true);
        var supplierSilverService = silverWithMdVersion(FIRST_SERVICE_KEY, SUPPLIER_SOURCE, false);
        var supplierSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, SUPPLIER_SOURCE))
            .setBaseSsku(supplierSilverBase)
            .putServiceSsku(supplierSilverService);

        var otherSupplierSource = new MasterDataSource(MasterDataSourceType.SUPPLIER, "100");
        var moreOldSupplierSilverBase = silverWithMdVersion(BASE_KEY, otherSupplierSource, Long.MIN_VALUE, false);
        var moreOldSupplierSilverService =
            silverWithMdVersion(FIRST_SERVICE_KEY, otherSupplierSource, Long.MIN_VALUE, false);

        var moreOldSupplierSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, SUPPLIER_SOURCE))
            .setBaseSsku(moreOldSupplierSilverBase)
            .putServiceSsku(moreOldSupplierSilverService);

        var group = new SilverCommonSskuGroup(
            List.of(warehouseSilverCommon, supplierSilverCommon, moreOldSupplierSilverCommon));

        // when
        var cleared = group.clearRemoved();

        // then
        assertThat(cleared.getRemoved())
            .containsExactlyInAnyOrder(warehouseSilverCommon, supplierSilverCommon, moreOldSupplierSilverCommon);
        assertThat(cleared.getClearedWithUpdates()).isEmpty();
        assertThat(cleared.removedServiceKeys()).containsExactlyInAnyOrder(FIRST_SERVICE_KEY);
    }

    @Test
    public void whenAllSupplierSilverServicesRemovedWholeGroupDeleted() {
        // given
        var warehouseSilverBase = silverWithMdVersion(BASE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverService = silverWithMdVersion(FIRST_SERVICE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverService2 = silverWithMdVersion(SECOND_SERVICE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, WAREHOUSE_SOURCE))
            .setBaseSsku(warehouseSilverBase)
            .putServiceSsku(warehouseSilverService)
            .putServiceSsku(warehouseSilverService2);
        var supplierSilverBase = silverWithMdVersion(BASE_KEY, SUPPLIER_SOURCE, false);
        var supplierSilverService = silverWithMdVersion(FIRST_SERVICE_KEY, SUPPLIER_SOURCE, true);
        var supplierSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, SUPPLIER_SOURCE))
            .setBaseSsku(supplierSilverBase)
            .putServiceSsku(supplierSilverService);

        var group = new SilverCommonSskuGroup(List.of(warehouseSilverCommon, supplierSilverCommon));

        // when
        var cleared = group.clearRemoved();

        // then
        assertThat(cleared.getRemoved()).containsExactlyInAnyOrder(warehouseSilverCommon, supplierSilverCommon);
        assertThat(cleared.getClearedWithUpdates()).isEmpty();
        assertThat(cleared.removedServiceKeys()).containsExactlyInAnyOrder(FIRST_SERVICE_KEY, SECOND_SERVICE_KEY);

    }

    @Test
    public void whenNoServiceOnSupplierSilverRemoveItFromGroup() {
        // given
        var warehouseSilverBase = silverWithMdVersion(BASE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverService = silverWithMdVersion(FIRST_SERVICE_KEY, WAREHOUSE_SOURCE, false);
        // the service not presented on supplier silver:
        var warehouseSilverService2 = silverWithMdVersion(SECOND_SERVICE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, WAREHOUSE_SOURCE))
            .setBaseSsku(warehouseSilverBase)
            .putServiceSskus(List.of(warehouseSilverService, warehouseSilverService2));
        var supplierSilverBase = silverWithMdVersion(BASE_KEY, SUPPLIER_SOURCE, false);
        var supplierSilverService = silverWithMdVersion(FIRST_SERVICE_KEY, SUPPLIER_SOURCE, false);
        var supplierSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, SUPPLIER_SOURCE))
            .setBaseSsku(supplierSilverBase)
            .putServiceSsku(supplierSilverService);

        var group = new SilverCommonSskuGroup(List.of(warehouseSilverCommon, supplierSilverCommon));

        // when
        var cleared = group.clearRemoved();

        // then
        assertThat(cleared.getRemoved()).isEmpty();
        var warehouseSilverCommonWithoutSecondService = new SilverCommonSsku(new SilverSskuKey(BASE_KEY,
            WAREHOUSE_SOURCE))
            .setBaseSsku(warehouseSilverBase)
            .putServiceSsku(warehouseSilverService);
        assertThat(cleared.getCleared())
            .containsExactlyInAnyOrder(warehouseSilverCommonWithoutSecondService, supplierSilverCommon);
        assertThat(cleared.getClearedWithUpdates())
            .containsExactlyInAnyOrder(warehouseSilverCommonWithoutSecondService);
        assertThat(cleared.removedServiceKeys()).containsExactlyInAnyOrder(SECOND_SERVICE_KEY);

    }

    @Test
    public void whenNoServiceOnSupplierAndNoOtherLiveServiceOnSilverRemoveWholeSilverFromGroup() {
        // given
        var warehouseSilverBase = silverWithMdVersion(BASE_KEY, WAREHOUSE_SOURCE, false);
        // the service not presented on supplier silver:
        var warehouseSilverService = silverWithMdVersion(FIRST_SERVICE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, WAREHOUSE_SOURCE))
            .setBaseSsku(warehouseSilverBase)
            .putServiceSsku(warehouseSilverService);
        var supplierSilverBase = silverWithMdVersion(BASE_KEY, SUPPLIER_SOURCE, false);
        var supplierSilverService = silverWithMdVersion(SECOND_SERVICE_KEY, SUPPLIER_SOURCE, false);
        var supplierSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, SUPPLIER_SOURCE))
            .setBaseSsku(supplierSilverBase)
            .putServiceSsku(supplierSilverService);

        var group = new SilverCommonSskuGroup(List.of(warehouseSilverCommon, supplierSilverCommon));

        // when
        var cleared = group.clearRemoved();

        // then
        var warehouseSilverCommonWithoutService = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, WAREHOUSE_SOURCE))
            .setBaseSsku(warehouseSilverBase);
        assertThat(cleared.getRemoved()).containsExactly(warehouseSilverCommonWithoutService);

        assertThat(cleared.getClearedWithUpdates()).isEmpty();
        assertThat(cleared.getCleared()).containsExactlyInAnyOrder(supplierSilverCommon);
        assertThat(cleared.removedServiceKeys()).containsExactlyInAnyOrder(FIRST_SERVICE_KEY);
    }

    @Test
    public void whenDbsTheMostActualItUsedToHandleGroup() {
        // given
        var dbsSilverBase = silverWithMdVersion(BASE_KEY, DBS_SOURCE, false);
        var dbsSilverService = silverWithMdVersion(FIRST_SERVICE_KEY, DBS_SOURCE, false);
        var dbsSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, DBS_SOURCE))
            .setBaseSsku(dbsSilverBase)
            .putServiceSsku(dbsSilverService);


        var supplierSilverBase = silverWithMdVersion(BASE_KEY, SUPPLIER_SOURCE, 0L, false);
        var supplierSilverService = silverWithMdVersion(FIRST_SERVICE_KEY, SUPPLIER_SOURCE, 0L, false);
        var supplierSilverServiceNotPresentedInActualDbs = silverWithMdVersion(SECOND_SERVICE_KEY, SUPPLIER_SOURCE,
            0L, false);
        var supplierSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, SUPPLIER_SOURCE))
            .setBaseSsku(supplierSilverBase)
            .putServiceSsku(supplierSilverService)
            .putServiceSsku(supplierSilverServiceNotPresentedInActualDbs);

        var group = new SilverCommonSskuGroup(List.of(dbsSilverCommon, supplierSilverCommon));

        // when
        var cleared = group.clearRemoved();

        // then
        var supplierSilverCommonWithoutService = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, WAREHOUSE_SOURCE))
            .setBaseSsku(supplierSilverBase)
            .putServiceSsku(supplierSilverService);
        assertThat(cleared.getRemoved()).isEmpty();

        assertThat(cleared.getCleared()).containsExactlyInAnyOrder(supplierSilverCommonWithoutService, dbsSilverCommon);
        assertThat(cleared.getClearedWithUpdates()).containsExactlyInAnyOrder(supplierSilverCommonWithoutService);
        assertThat(cleared.removedServiceKeys()).containsExactlyInAnyOrder(SECOND_SERVICE_KEY);
    }

    private SilverServiceSsku silverWithMdVersion(ShopSkuKey key,
                                                  MasterDataSource source,
                                                  boolean removed) {
        return silverWithMdVersion(key, source, 10000L, removed);
    }

    private SilverServiceSsku silverWithMdVersion(ShopSkuKey key,
                                                  MasterDataSource source,
                                                  Long mdVersion,
                                                  boolean removed) {
        var silverServiceSsku = new SilverServiceSsku();
        silverServiceSsku.setKey(new SilverSskuKey(key, source));
        silverServiceSsku.setMasterDataVersion(mdVersion);
        var params = new ArrayList<SskuSilverParamValue>();
        var mdVersionParam = silverValue(key, DATACAMP_MASTER_DATA_VERSION, source);
        mdVersionParam.setNumeric(new BigDecimal(mdVersion));
        params.add(mdVersionParam);
        if (removed) {
            var removedParam = silverValue(key, IS_REMOVED, source);
            removedParam.setBool(removed);
            params.add(removedParam);
        }
        silverServiceSsku.setParamValues(params);
        return silverServiceSsku;
    }

    private SskuSilverParamValue silverValue(ShopSkuKey key,
                                             long mdmParamId,
                                             MasterDataSource source) {
        return silverValue(key, mdmParamId, Instant.now(), source);
    }

    private SskuSilverParamValue silverValue(ShopSkuKey key,
                                             long mdmParamId,
                                             Instant ts,
                                             MasterDataSource source) {
        return (SskuSilverParamValue) new SskuSilverParamValue()
            .setShopSkuKey(key)
            .setMasterDataSource(source)
            .setMdmParamId(mdmParamId)
            .setXslName("-")
            .setSourceUpdatedTs(ts)
            .setUpdatedTs(ts);
    }
}
