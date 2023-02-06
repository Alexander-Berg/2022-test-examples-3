package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MasterDataToSilverParamValuesQRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class EnqueueMasterDataToMdToSSPVQueueManualExecutorTest extends MdmBaseDbTestClass {

    private static final int BUSINESS = 1;
    private static final int SERVICE3STAGE = 777;
    private static final int SERVICE3STAGE2 = 888;
    private static final int SERVICE2STAGE = 666;
    private static final int REAL_SUPPLIER = 999;
    private static final String SHOP_SKU =  "sku";
    private static final String WHITE_SHOP_SKU = "001.white_ssku";
    private static final ShopSkuKey BUSINESS_KEY = new ShopSkuKey(BUSINESS, SHOP_SKU);
    private static final ShopSkuKey SERVICE3STAGE_KEY = new ShopSkuKey(SERVICE3STAGE, SHOP_SKU);
    private static final ShopSkuKey SERVICE2STAGE_KEY = new ShopSkuKey(SERVICE2STAGE, SHOP_SKU);
    private static final ShopSkuKey SERVICE3STAGE2_KEY = new ShopSkuKey(SERVICE3STAGE2, SHOP_SKU);
    private static final ShopSkuKey REAL_SUPPLIER_KEY = new ShopSkuKey(REAL_SUPPLIER, SHOP_SKU);

    @Autowired
    MasterDataRepository masterDataRepository;
    @Autowired
    MdmSskuGroupManager groupManager;
    @Autowired
    StorageKeyValueService skv;
    @Autowired
    MasterDataToSilverParamValuesQRepository mdToSpvQ;
    @Autowired
    MdmSupplierRepository supplierRepository;
    @Autowired
    BeruId beruId;

    private EnhancedRandom random;
    private EnqueueMasterDataToMdToSSPVQueueManualExecutor executor;

    @Before
    public void setUp() {
        random = TestDataUtils.defaultRandom(28L);
        supplierRepository.insertOrUpdateAll(List.of(
            new MdmSupplier()
                .setType(MdmSupplierType.BUSINESS)
                .setId(BUSINESS)
                .setDeleted(false),
            new MdmSupplier()
                .setType(MdmSupplierType.THIRD_PARTY)
                .setId(SERVICE3STAGE)
                .setBusinessEnabled(true)
                .setBusinessId(BUSINESS)
                .setDeleted(false),
            new MdmSupplier()
                .setType(MdmSupplierType.THIRD_PARTY)
                .setId(SERVICE2STAGE)
                .setBusinessEnabled(false)
                .setBusinessId(BUSINESS)
                .setDeleted(false),
            new MdmSupplier()
                .setType(MdmSupplierType.FIRST_PARTY)
                .setId(beruId.getId())
                .setBusinessEnabled(false)
                .setDeleted(false),
            new MdmSupplier()
                .setType(MdmSupplierType.REAL_SUPPLIER)
                .setId(REAL_SUPPLIER)
                .setBusinessEnabled(false)
                .setDeleted(false)
                .setRealSupplierId("001")
        ));

        MasterData businessMasterData = TestDataUtils.generateMasterData(BUSINESS_KEY, random);
        MasterData service2stMasterData = TestDataUtils.generateMasterData(SERVICE2STAGE_KEY, random);
        MasterData service3stMasterData = TestDataUtils.generateMasterData(SERVICE3STAGE_KEY, random);
        MasterData service1pMasterData = TestDataUtils.generateMasterData(
            new ShopSkuKey(beruId.getId(), WHITE_SHOP_SKU), random);
        MasterData realSupplierMasterData = TestDataUtils.generateMasterData(REAL_SUPPLIER_KEY, random);
        masterDataRepository.insertOrUpdateAll(List.of(businessMasterData, service2stMasterData,
            service3stMasterData, service1pMasterData, realSupplierMasterData));

        executor = new EnqueueMasterDataToMdToSSPVQueueManualExecutor(masterDataRepository, mdToSpvQ, skv, groupManager);
        skv.putValue(MdmProperties.ENQUEUE_TO_MD_2_SPV_Q_ENABLED, true);
        skv.invalidateCache();
    }

    @Test
    public void testAllEnqueuedAsExpected() {
        executor.execute();

        List<SskuToRefreshInfo> infos = mdToSpvQ.findAll();
        Assertions.assertThat(infos).hasSize(3);
        Assertions.assertThat(infos.stream().map(SskuToRefreshInfo::getEntityKey).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(BUSINESS_KEY, new ShopSkuKey(beruId.getId(), WHITE_SHOP_SKU), REAL_SUPPLIER_KEY);

        // enqueue 2 different service keys in 2 different batches should enqueue only 1 biz key
        supplierRepository.insertOrUpdate( new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(SERVICE3STAGE2)
            .setBusinessEnabled(true)
            .setBusinessId(BUSINESS)
            .setDeleted(false));
        masterDataRepository.insertOrUpdate(TestDataUtils.generateMasterData(SERVICE3STAGE2_KEY, random));
        executor.execute();

        infos = mdToSpvQ.findAll();
        Assertions.assertThat(infos).hasSize(3);
        Assertions.assertThat(infos.stream().map(SskuToRefreshInfo::getEntityKey).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(BUSINESS_KEY, new ShopSkuKey(beruId.getId(), WHITE_SHOP_SKU), REAL_SUPPLIER_KEY);

    }

}
