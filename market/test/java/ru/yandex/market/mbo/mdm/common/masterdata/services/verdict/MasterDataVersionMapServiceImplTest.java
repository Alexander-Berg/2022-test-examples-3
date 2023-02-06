package ru.yandex.market.mbo.mdm.common.masterdata.services.verdict;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.ServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

/**
 * @author albina-gima
 * @date 1/25/21
 */
public class MasterDataVersionMapServiceImplTest extends MdmBaseDbTestClass {
    private static final ShopSkuKey BUSINESS_KEY = new ShopSkuKey(1000, "sku");
    private static final ShopSkuKey STAGE2_KEY = new ShopSkuKey(2, "sku");
    private static final ShopSkuKey STAGE3_KEY1 = new ShopSkuKey(31, "sku");
    private static final ShopSkuKey STAGE3_KEY2 = new ShopSkuKey(32, "sku");

    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private MdmParamCache paramCache;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;

    private MasterDataVersionMapService masterDataVersionMapService;

    @Before
    public void setUp() throws Exception {
        masterDataVersionMapService = new MasterDataVersionMapServiceImpl(
            mdmSskuGroupManager,
                silverSskuRepository);
    }

    @Test
    public void testBuildMDVersionMapFromSskuSilverParamValuesMapAndReplaceServiceMDVersionWithBusinessMDVersion() {
        prepareBusinessGroup(false);

        Instant ts = Instant.now();
        long businessMDVersion = 6L;
        long stage2MDVersion = 2L;

        var eoxSsku = silverSsku(BUSINESS_KEY, "supplier", MasterDataSourceType.SUPPLIER, ts,
            1, 0, List.of(), List.of(), businessMDVersion);
        // эта версия master data version будет заменена версией из бизнесовой части
        var stage3Ssku = silverSsku(STAGE3_KEY1, "rose", MasterDataSourceType.SUPPLIER, ts,
            3, 0, List.of(), List.of(), stage2MDVersion);

        // не входит в результат, т.к. не принадлежит бизнес-группе (т.е. нет бизнесовой master data version)
        var stage2Ssku = silverSsku(STAGE2_KEY, "rose", MasterDataSourceType.SUPPLIER, ts,
            3, 0, List.of(), List.of(), stage2MDVersion);
        silverSskuRepository.insertOrUpdateSsku(eoxSsku);
        silverSskuRepository.insertOrUpdateSsku(stage3Ssku);
        silverSskuRepository.insertOrUpdateSsku(stage2Ssku);

        List<ShopSkuKey> keys = List.of(STAGE3_KEY1, STAGE3_KEY2, STAGE2_KEY);
        sskuExistenceRepository.markExistence(keys, true);
        Map<ShopSkuKey, Long> masterDataVersionMap =
            masterDataVersionMapService.fromSskuSilverParamValuesUsingBusinessMDVersion(keys);

        Assertions.assertThat(masterDataVersionMap).isNotEmpty();
        Assertions.assertThat(masterDataVersionMap.keySet()).containsExactlyInAnyOrder(STAGE3_KEY1, STAGE3_KEY2);

        // сервисным ключам одного бизнеса присвоена бизнесовая master data version
        Assertions.assertThat(masterDataVersionMap.get(STAGE3_KEY1)).isEqualTo(businessMDVersion);
        Assertions.assertThat(masterDataVersionMap.get(STAGE3_KEY2)).isEqualTo(businessMDVersion);
    }

    @Test
    public void testBuildMDVersionMapFromSskuSilverParamValuesAndConsiderOnlySupplierSourceType() {
        prepareBusinessGroup(false);

        Instant ts = Instant.now();
        long businessMDVersion = 6L;
        long stage3Key2MDVersion = 3L;

        // эта версия должна пойти в результат для всех сервисных ключей бизнес-группы
        var eoxSsku = silverSsku(BUSINESS_KEY, "supplier", MasterDataSourceType.SUPPLIER, ts,
            1, 0, List.of(), List.of(), businessMDVersion);
        // не учитывем эту версию masterDataVersion, т.к. ее единственный источник - MasterDataSourceType.SUPPLIER
        var editorSsku = silverSsku(BUSINESS_KEY, "doctor", MasterDataSourceType.MDM_OPERATOR, ts.plusSeconds(2),
            2, 0, List.of(), List.of(), stage3Key2MDVersion);
        silverSskuRepository.insertOrUpdateSsku(eoxSsku);
        silverSskuRepository.insertOrUpdateSsku(editorSsku);

        List<ShopSkuKey> keys = List.of(STAGE3_KEY1, STAGE3_KEY2);
        sskuExistenceRepository.markExistence(keys, true);
        Map<ShopSkuKey, Long> masterDataVersionMap =
            masterDataVersionMapService.fromSskuSilverParamValuesUsingBusinessMDVersion(keys);

        Assertions.assertThat(masterDataVersionMap).isNotEmpty();
        Assertions.assertThat(masterDataVersionMap.get(STAGE3_KEY1)).isEqualTo(businessMDVersion);
        Assertions.assertThat(masterDataVersionMap.get(STAGE3_KEY2)).isEqualTo(businessMDVersion);
    }

    @Test
    public void testBuildFromCommonSskusUsingOriginalMDVersion() {
        long businessMDVersion = 2L;
        long serviceMDVersion = 4L;

        CommonSsku commonSsku = new CommonSsku(BUSINESS_KEY);
        commonSsku.setMasterDataVersion(businessMDVersion);

        ServiceSsku ssku1 = new ServiceSsku(STAGE3_KEY1);
        ssku1.setMasterDataVersion(serviceMDVersion);
        ServiceSsku ssku2 = new ServiceSsku(STAGE3_KEY2);
        ssku2.setMasterDataVersion(serviceMDVersion);

        commonSsku.putServiceSskus(List.of(ssku1, ssku2));

        Map<ShopSkuKey, Long> masterDataVersionMap =
            masterDataVersionMapService.fromCommonSskusUsingOriginalMDVersion(List.of(commonSsku));

        Assertions.assertThat(masterDataVersionMap.size()).isEqualTo(3);
        Assertions.assertThat(masterDataVersionMap.keySet()).containsExactlyInAnyOrder(BUSINESS_KEY, STAGE3_KEY1,
            STAGE3_KEY2);
        Assertions.assertThat(masterDataVersionMap.get(BUSINESS_KEY)).isEqualTo(businessMDVersion);
        Assertions.assertThat(masterDataVersionMap.get(STAGE3_KEY1)).isEqualTo(serviceMDVersion);
        Assertions.assertThat(masterDataVersionMap.get(STAGE3_KEY2)).isEqualTo(serviceMDVersion);
    }

    @Test
    public void testBuildFromCommonSskusUsingBusinessMDVersion() {
        long businessMDVersion = 2L;
        long serviceMDVersion = 4L;

        CommonSsku commonSsku = new CommonSsku(BUSINESS_KEY);
        commonSsku.setMasterDataVersion(businessMDVersion);

        ServiceSsku ssku1 = new ServiceSsku(STAGE3_KEY1);
        ssku1.setMasterDataVersion(serviceMDVersion);
        ServiceSsku ssku2 = new ServiceSsku(STAGE3_KEY2);
        ssku2.setMasterDataVersion(serviceMDVersion);

        commonSsku.putServiceSskus(List.of(ssku1, ssku2));

        Map<ShopSkuKey, Long> masterDataVersionMap =
            masterDataVersionMapService.fromCommonSskusUsingBusinessMDVersion(List.of(commonSsku));

        Assertions.assertThat(masterDataVersionMap.size()).isEqualTo(3);
        Assertions.assertThat(masterDataVersionMap.keySet()).containsExactlyInAnyOrder(BUSINESS_KEY, STAGE3_KEY1,
            STAGE3_KEY2);
        Assertions.assertThat(masterDataVersionMap.get(BUSINESS_KEY)).isEqualTo(businessMDVersion);
        Assertions.assertThat(masterDataVersionMap.get(STAGE3_KEY1)).isEqualTo(businessMDVersion);
        Assertions.assertThat(masterDataVersionMap.get(STAGE3_KEY2)).isEqualTo(businessMDVersion);
    }

    private SilverCommonSsku silverSsku(ShopSkuKey key,
                                        String sourceId,
                                        MasterDataSourceType type,
                                        Instant updatedTs,
                                        int boxCount,
                                        int deliveryTime,
                                        List<String> countries,
                                        List<Long> regNumbers,
                                        Long masterDataVersion) {
        List<SskuSilverParamValue> result = new ArrayList<>();
        if (boxCount > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.BOX_COUNT);
            value.setXslName(paramCache.get(KnownMdmParams.BOX_COUNT).getXslName());
            value.setNumeric(BigDecimal.valueOf(boxCount));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (deliveryTime > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.DELIVERY_TIME);
            value.setXslName(paramCache.get(KnownMdmParams.DELIVERY_TIME).getXslName());
            value.setNumeric(BigDecimal.valueOf(deliveryTime));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (countries.size() > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY);
            value.setXslName(paramCache.get(KnownMdmParams.MANUFACTURER_COUNTRY).getXslName());
            value.setStrings(countries);
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (regNumbers.size() > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.DOCUMENT_REG_NUMBER);
            value.setXslName(paramCache.get(KnownMdmParams.DOCUMENT_REG_NUMBER).getXslName());
            value.setStrings(regNumbers.stream().map(Object::toString).collect(Collectors.toList()));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        return new SilverCommonSsku(
            new SilverSskuKey(key, new MasterDataSource(type, sourceId))
        ).addBaseValues(result);
    }

    private SskuSilverParamValue silverValue(ShopSkuKey key,
                                             String sourceId,
                                             MasterDataSourceType type,
                                             Instant updatedTs) {
        return (SskuSilverParamValue) new SskuSilverParamValue()
            .setShopSkuKey(key)
            .setMasterDataSourceId(sourceId)
            .setMasterDataSourceType(type)
            .setUpdatedTs(updatedTs)
            .setSourceUpdatedTs(updatedTs);
    }

    private void prepareBusinessGroup(boolean fullStage3) {
        MdmSupplier business = new MdmSupplier()
            .setId(BUSINESS_KEY.getSupplierId())
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier stage2Partner = new MdmSupplier()
            .setId(STAGE2_KEY.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(business.getId())
            .setBusinessEnabled(fullStage3);
        MdmSupplier stage3Partner1 = new MdmSupplier()
            .setId(STAGE3_KEY1.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(business.getId())
            .setBusinessEnabled(true);
        MdmSupplier stage3Partner2 = new MdmSupplier()
            .setId(STAGE3_KEY2.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(business.getId())
            .setBusinessEnabled(true);
        mdmSupplierRepository.insertBatch(business, stage2Partner, stage3Partner1, stage3Partner2);
    }
}
