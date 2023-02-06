package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.Map;
import java.util.Random;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepositoryParamValueImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.StorageApiSilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku.BmdmEntityToSilverCommonSskuConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.proto.MdmEntityStorageService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class ResyncFromPgToYt1PSilverToolTest extends MdmBaseDbTestClass {
    @Autowired
    private BeruId beruId;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private MdmEntityStorageService mdmEntityStorageService;
    @Autowired
    private BmdmEntityToSilverCommonSskuConverter bmdmEntityToSilverCommonSskuConverter;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private MdmParamCache mdmParamCache;

    private SilverSskuRepositoryParamValueImpl silverSskuRepositoryParamValue;
    private StorageApiSilverSskuRepository storageApiSilverSskuRepository;
    private ResyncFromPgToYt1PSilverTool resyncFromPgToYt1PSilverTool;
    private Random random;

    @Before
    public void setUp() {
        silverSskuRepositoryParamValue = new SilverSskuRepositoryParamValueImpl(
            super.jdbcTemplate,
            super.transactionTemplate,
            mdmSskuGroupManager
        );
        storageApiSilverSskuRepository = new StorageApiSilverSskuRepository(
            mdmEntityStorageService,
            bmdmEntityToSilverCommonSskuConverter,
            mdmSskuGroupManager
        );
        resyncFromPgToYt1PSilverTool = new ResyncFromPgToYt1PSilverTool(
            super.jdbcTemplate,
            beruId,
            storageKeyValueService,
            silverSskuRepositoryParamValue,
            storageApiSilverSskuRepository
        );

        mdmSupplierRepository.insertBatch(
            new MdmSupplier()
                .setId(beruId.getBusinessId())
                .setType(MdmSupplierType.BUSINESS),
            new MdmSupplier()
                .setId(beruId.getId())
                .setType(MdmSupplierType.FIRST_PARTY)
                .setBusinessEnabled(true)
                .setBusinessId(beruId.getBusinessId())
        );

        random = new Random(1234567890L);

        storageKeyValueService.putValue(MdmProperties.SHOULD_RUN_RESYNC_FROM_PG_TO_YT_TOOL, true);
    }

    @Test
    public void testSyncWhenNoYtSilverExists() {
        // given
        String shopSku = "U-238";
        ShopSkuKey serviceKey = new ShopSkuKey(beruId.getId(), shopSku);
        ShopSkuKey businessKey = new ShopSkuKey(beruId.getBusinessId(), shopSku);
        sskuExistenceRepository.markExistence(serviceKey, true);

        MasterDataSource source = new MasterDataSource(MasterDataSourceType.MEASUREMENT, "172");
        SilverSskuKey silverServiceKey = new SilverSskuKey(serviceKey, source);
        SilverSskuKey businessSilverKey = new SilverSskuKey(businessKey, source);

        SilverServiceSsku existing = new SilverServiceSsku(silverServiceKey);
        KnownMdmParams.WEIGHT_DIMENSIONS_PARAMS.stream()
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(existing::addParamValue);
        silverSskuRepositoryParamValue.insertOrUpdateAll(existing.getValues());

        // when
        resyncFromPgToYt1PSilverTool.execute();

        // then
        Assertions.assertThat(storageApiSilverSskuRepository.findSsku(businessSilverKey).orElseThrow().getBaseValues())
            .filteredOn(pv -> pv.getMdmParamId() != KnownMdmParams.BMDM_ID)
            .allMatch(pv -> pv.getSilverSskuKey().equals(businessSilverKey))
            .map(pv -> pv.setShopSkuKey(serviceKey))
            .containsExactlyInAnyOrderElementsOf(existing.getValues());
    }

    @Test
    public void whenYtSilverIsOutdatedDoSync() {
        // given
        String shopSku = "U-238";
        ShopSkuKey serviceKey = new ShopSkuKey(beruId.getId(), shopSku);
        ShopSkuKey businessKey = new ShopSkuKey(beruId.getBusinessId(), shopSku);
        sskuExistenceRepository.markExistence(serviceKey, true);

        MasterDataSource source = new MasterDataSource(MasterDataSourceType.MEASUREMENT, "172");
        SilverSskuKey silverServiceKey = new SilverSskuKey(serviceKey, source);
        SilverSskuKey businessSilverKey = new SilverSskuKey(businessKey, source);

        SilverServiceSsku existingPg = new SilverServiceSsku(silverServiceKey);
        KnownMdmParams.WEIGHT_DIMENSIONS_PARAMS.stream()
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(existingPg::addParamValue);
        silverSskuRepositoryParamValue.insertOrUpdateAll(existingPg.getValues());
        jdbcTemplate.update(
            "update " + SilverSskuRepositoryParamValueImpl.TABLE + " set updated_ts = updated_ts + interval '1 hour'",
            Map.of()
        );

        storageApiSilverSskuRepository.insertOrUpdateSsku(
            new SilverCommonSsku(businessSilverKey)
                .addBaseValue(TestMdmParamUtils.createRandomMdmParamValue(
                    random, mdmParamCache.get(KnownMdmParams.HIDE_LIFE_TIME))
                )
        );
        SilverCommonSsku existingYtSilver = storageApiSilverSskuRepository.findSsku(businessSilverKey).orElseThrow();

        // when
        resyncFromPgToYt1PSilverTool.execute();

        // then
        SilverCommonSsku ytSilverAfter = storageApiSilverSskuRepository.findSsku(businessSilverKey).orElseThrow();
        Assertions.assertThat(ytSilverAfter).isNotEqualTo(existingYtSilver);
        Assertions.assertThat(ytSilverAfter.getBaseValues())
            .filteredOn(pv -> pv.getMdmParamId() != KnownMdmParams.BMDM_ID)
            .allMatch(pv -> pv.getSilverSskuKey().equals(businessSilverKey))
            .map(pv -> pv.setShopSkuKey(serviceKey))
            .containsExactlyInAnyOrderElementsOf(existingPg.getValues());
    }

    @Test
    public void whenYtSilverIsActualDoNotSync() {
        // given
        String shopSku = "U-238";
        ShopSkuKey serviceKey = new ShopSkuKey(beruId.getId(), shopSku);
        ShopSkuKey businessKey = new ShopSkuKey(beruId.getBusinessId(), shopSku);
        sskuExistenceRepository.markExistence(serviceKey, true);

        MasterDataSource source = new MasterDataSource(MasterDataSourceType.MEASUREMENT, "172");
        SilverSskuKey silverServiceKey = new SilverSskuKey(serviceKey, source);
        SilverSskuKey businessSilverKey = new SilverSskuKey(businessKey, source);

        SilverServiceSsku existingPg = new SilverServiceSsku(silverServiceKey);
        KnownMdmParams.WEIGHT_DIMENSIONS_PARAMS.stream()
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(existingPg::addParamValue);
        silverSskuRepositoryParamValue.insertOrUpdateAll(existingPg.getValues());

        storageApiSilverSskuRepository.insertOrUpdateSsku(
            new SilverCommonSsku(businessSilverKey).addBaseValues(existingPg.getValues()));
        SilverCommonSsku existingYtSilver = storageApiSilverSskuRepository.findSsku(businessSilverKey).orElseThrow();

        // when
        resyncFromPgToYt1PSilverTool.execute();

        // then
        Assertions.assertThat(storageApiSilverSskuRepository.findSsku(businessSilverKey).orElseThrow())
            .isEqualTo(existingYtSilver);
    }
}
