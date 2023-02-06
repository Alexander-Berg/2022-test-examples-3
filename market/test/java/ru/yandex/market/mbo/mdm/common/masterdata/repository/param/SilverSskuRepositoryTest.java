package ru.yandex.market.mbo.mdm.common.masterdata.repository.param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

/**
 * @author dmserebr
 * @date 11/11/2020
 */
public abstract class SilverSskuRepositoryTest extends MdmBaseDbTestClass {
    protected static final int BUSINESS = 1000;
    protected static final int SERVICE1 = 1001;
    protected static final int SERVICE2 = 1002;
    protected static final int SERVICE3 = 1003;
    protected static final int ORPHAN = 999;

    @Autowired
    protected MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    protected MdmSupplierCachingService mdmSupplierCachingService;
    @Autowired
    protected SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    protected MdmParamCache mdmParamCache;

    private EnhancedRandom random;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(4658965L);
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(BUSINESS)
            .setType(MdmSupplierType.BUSINESS)
        );
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(SERVICE1)
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS)
            .setBusinessEnabled(true)
        );
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(SERVICE2)
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS)
            .setBusinessEnabled(true)
        );
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(SERVICE3)
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS)
            .setBusinessEnabled(true)
        );
        mdmSupplierRepository.insert(new MdmSupplier()
            .setId(ORPHAN)
            .setType(MdmSupplierType.FIRST_PARTY)
            .setBusinessId(BUSINESS)
            .setBusinessEnabled(false)
        );
        mdmSupplierCachingService.refresh();
    }

    protected abstract SilverSskuRepository repository();

    @Test
    public void testInsert() {
        var businessSsku = businessSsku();
        var orphanSsku = orphanSsku();
        List<SilverCommonSsku> sskus = List.of(businessSsku, orphanSsku);
        repository().insertOrUpdateSskus(sskus);

        List<SilverSskuKey> keys = keysOf(sskus);
        Map<SilverSskuKey, SilverCommonSsku> result = repository().findSskusBySilverKeys(keys);
        Stream.of(businessSsku, orphanSsku)
            .map(SilverCommonSsku::getBusinessKey)
            .forEach(key -> result.computeIfPresent(key, (k, v) -> TestBmdmUtils.removeBmdmIdAndVersion(v)));
        Assertions.assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
            businessSsku.getBusinessKey(), businessSsku,
            orphanSsku.getBusinessKey(), orphanSsku
        ));
    }

    @Test
    public void testUpdate() {
        var businessSsku = businessSsku();
        var orphanSsku = orphanSsku();
        List<SilverCommonSsku> sskus = List.of(businessSsku, orphanSsku);
        repository().insertOrUpdateSskus(sskus);

        // апнем все нумерики на единичку
        businessSsku.getBaseValues().stream()
            .filter(v -> v.getNumeric().isPresent())
            .forEach(v -> v.setNumeric(v.getNumeric().get().add(BigDecimal.ONE)));

        businessSsku.getServiceSskus().values()
            .stream()
            .flatMap(s -> s.getValues().stream())
            .filter(v -> v.getNumeric().isPresent())
            .forEach(v -> v.setNumeric(v.getNumeric().get().add(BigDecimal.ONE)));

        orphanSsku.getBaseValues().stream()
            .filter(v -> v.getNumeric().isPresent())
            .forEach(v -> v.setNumeric(v.getNumeric().get().add(BigDecimal.ONE)));

        repository().insertOrUpdateSskus(sskus);
        List<SilverSskuKey> keys = keysOf(sskus);
        Map<SilverSskuKey, SilverCommonSsku> result = repository().findSskusBySilverKeys(keys);
        Stream.of(businessSsku, orphanSsku)
            .map(SilverCommonSsku::getBusinessKey)
            .forEach(key -> result.computeIfPresent(key, (k, v) -> TestBmdmUtils.removeBmdmIdAndVersion(v)));
        Assertions.assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
            businessSsku.getBusinessKey(), businessSsku,
            orphanSsku.getBusinessKey(), orphanSsku
        ));
    }

    @Test
    public void testFindBySilverRootKey() {
        var businessSsku = businessSsku();
        var orphanSsku = orphanSsku();
        List<SilverCommonSsku> sskus = List.of(businessSsku, orphanSsku);
        repository().insertOrUpdateSskus(sskus);

        Assertions.assertThat(repository().findSsku(businessSsku.getBusinessKey()))
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .hasValue(businessSsku);
        Assertions.assertThat(repository().findSsku(orphanSsku.getBusinessKey()))
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .hasValue(orphanSsku);
    }

    @Test
    public void testFindByServiceKey() {
        var businessSsku = businessSsku();
        var orphanSsku = orphanSsku();
        List<SilverCommonSsku> sskus = List.of(businessSsku, orphanSsku);
        repository().insertOrUpdateSskus(sskus);

        Assertions.assertThat(repository().findSsku(businessSsku.getServiceKeys().iterator().next().getShopSkuKey()))
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .containsExactly(businessSsku);
        Assertions.assertThat(repository().findSsku(orphanSsku.getBusinessKey().getShopSkuKey()))
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .containsExactly(orphanSsku);
    }

    @Test
    public void testFindByBusinessKey() {
        var businessSsku = businessSsku();
        var orphanSsku = orphanSsku();
        List<SilverCommonSsku> sskus = List.of(businessSsku, orphanSsku);
        repository().insertOrUpdateSskus(sskus);

        Assertions.assertThat(repository().findSsku(businessSsku.getBusinessKey().getShopSkuKey()))
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .containsExactly(businessSsku);
        Assertions.assertThat(repository().findSsku(orphanSsku.getBusinessKey().getShopSkuKey()))
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .containsExactly(orphanSsku);
    }

    private SilverServiceSsku flatSsku(SilverSskuKey parentKey, int supplierId) {
        SilverSskuKey serviceKey = new SilverSskuKey(supplierId, parentKey.getShopSku(), parentKey.getSourceType(),
            parentKey.getSourceId());

        SilverServiceSsku ssku = new SilverServiceSsku(serviceKey);
        SskuSilverParamValue value1 = new SskuSilverParamValue();
        value1.setShopSkuKey(serviceKey.getShopSkuKey())
            .setMasterDataSource(serviceKey.getMasterDataSource())
            .setMdmParamId(randomServiceNumericParam())
            .setNumeric(BigDecimal.valueOf(random.nextInt(100)));
        SskuSilverParamValue value2 = new SskuSilverParamValue();
        value2.setShopSkuKey(serviceKey.getShopSkuKey())
            .setMasterDataSource(serviceKey.getMasterDataSource())
            .setMdmParamId(KnownMdmParams.VAT)
            .setOption(new MdmParamOption(1L + random.nextInt(KnownMdmParams.VAT_RATES.size()) - 1));

        ssku.addParamValue(value1);
        ssku.addParamValue(value2);

        if (supplierId == ORPHAN) {
            SskuSilverParamValue value3 = new SskuSilverParamValue();
            value3.setShopSkuKey(serviceKey.getShopSkuKey())
                .setMasterDataSource(serviceKey.getMasterDataSource())
                .setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY)
                .setStrings(List.of(random.nextObject(String.class), random.nextObject(String.class)));
            ssku.addParamValue(value3);
        }
        sskuExistenceRepository.markExistence(serviceKey.getShopSkuKey(), true);
        return ssku;
    }

    private SilverCommonSsku businessSsku() {
        String shopSku = random.nextObject(String.class);
        SilverSskuKey key = new SilverSskuKey(BUSINESS, shopSku, MasterDataSourceType.AUTO,
            random.nextObject(String.class));

        SilverCommonSsku ssku = new SilverCommonSsku(key);

        SskuSilverParamValue manufacturerCountryValue = new SskuSilverParamValue();
        manufacturerCountryValue.setShopSkuKey(key.getShopSkuKey())
            .setMasterDataSource(key.getMasterDataSource())
            .setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY)
            .setStrings(List.of(random.nextObject(String.class), random.nextObject(String.class)));
        ssku.addBaseValue(manufacturerCountryValue);

        SskuSilverParamValue boxCountValue = new SskuSilverParamValue();
        boxCountValue.setShopSkuKey(key.getShopSkuKey())
            .setMasterDataSource(key.getMasterDataSource())
            .setMdmParamId(KnownMdmParams.BOX_COUNT)
            .setNumeric(BigDecimal.valueOf(random.nextInt(100)));
        ssku.addBaseValue(boxCountValue);

        ssku.putServiceSsku(flatSsku(key, SERVICE1));
        ssku.putServiceSsku(flatSsku(key, SERVICE2));
        ssku.putServiceSsku(flatSsku(key, SERVICE3));
        return ssku;
    }

    private SilverCommonSsku orphanSsku() {
        String shopSku = random.nextObject(String.class);
        SilverSskuKey key = new SilverSskuKey(ORPHAN, shopSku, MasterDataSourceType.AUTO,
            random.nextObject(String.class));
        SilverCommonSsku ssku = new SilverCommonSsku(key);
        ssku.setBaseSsku(flatSsku(key, ORPHAN));
        return ssku;
    }

    private long randomServiceNumericParam() {
        return List.of(
            KnownMdmParams.DELIVERY_TIME,
            KnownMdmParams.QUANTITY_IN_PACK,
            KnownMdmParams.QUANTUM_OF_SUPPLY,
            KnownMdmParams.TRANSPORT_UNIT_SIZE
        ).get(random.nextInt(4));
    }

    private static List<SilverSskuKey> keysOf(List<SilverCommonSsku> sskus) {
        return sskus.stream().map(SilverCommonSsku::getBusinessKey).collect(Collectors.toList());
    }
}
