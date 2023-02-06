package ru.yandex.market.mbo.mdm.tms.executors;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuPartnerVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.VerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.GoldSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToDeleteRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuGoldenVerdictRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuPartnerVerdictRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.DATACAMP_MASTER_DATA_VERSION;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.IS_REMOVED;

public class SskuToDeleteExecutorTest extends MdmBaseDbTestClass {

    private static final ShopSkuKey BASE_KEY = new ShopSkuKey(1, "someGreatStaff");
    private static final ShopSkuKey SERVICE_KEY = new ShopSkuKey(2, "someGreatStaff");
    private static final ShopSkuKey SERVICE_KEY_2 = new ShopSkuKey(3, "someGreatStaff");
    private static final MasterDataSource SUPPLIER_SOURCE =
        new MasterDataSource(MasterDataSourceType.SUPPLIER, "0");
    private static final MasterDataSource WAREHOUSE_SOURCE =
        new MasterDataSource(MasterDataSourceType.WAREHOUSE, "99");
    private static final Long MD_VERSION = 1000L;

    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private SskuGoldenVerdictRepository sskuGoldenVerdictRepository;
    @Autowired
    private SskuPartnerVerdictRepository sskuPartnerVerdictRepository;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private MasterDataRepository masterDataRepository;

    @Autowired
    private StorageKeyValueService skv;
    @Autowired
    private SskuToDeleteRepository queue;
    @Autowired
    private GoldSskuRepository goldSskuRepository;
    @Autowired
    private SskuExistenceRepository existenceRepository;
    @Autowired
    private MdmQueuesManager queuesManager;
    @Autowired
    private SskuToRefreshRepository sskuToRefreshRepository;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;

    private SskuToDeleteExecutor executor;

    @Before
    public void setUp() throws Exception {
        skv.putValue(MdmProperties.SHOULD_RUN_DELETE_OFFER, true);

        executor = new SskuToDeleteExecutor(
            skv,
            queue,
            referenceItemRepository,
            masterDataRepository,
            goldSskuRepository,
            sskuGoldenVerdictRepository,
            sskuPartnerVerdictRepository,
            silverSskuRepository,
            existenceRepository,
            queuesManager
        );

        prepareBusinessGroup(BASE_KEY.getSupplierId(), SERVICE_KEY.getSupplierId(), SERVICE_KEY_2.getSupplierId());
        existenceRepository.markExistence(List.of(SERVICE_KEY), true);
        existenceRepository.markExistence(List.of(SERVICE_KEY_2), true);
    }

    @Test
    public void whenSupplierSilverRemovedWholeGroupRemoved() {
        // given
        var warehouseSilverBase = silverWithMdVersion(BASE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverService = silverWithMdVersion(SERVICE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, WAREHOUSE_SOURCE))
            .setBaseSsku(warehouseSilverBase)
            .putServiceSsku(warehouseSilverService);

        // base removed - we remove it
        var supplierSilverBase = silverWithMdVersion(BASE_KEY, SUPPLIER_SOURCE, true);
        var supplierSilverService = silverWithMdVersion(SERVICE_KEY, SUPPLIER_SOURCE, false);
        var supplierSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, SUPPLIER_SOURCE))
            .setBaseSsku(supplierSilverBase)
            .putServiceSsku(supplierSilverService);

        silverSskuRepository.insertOrUpdateSskus(List.of(supplierSilverCommon, warehouseSilverCommon));
        referenceItemRepository.insert(referenceItem(SERVICE_KEY));
        masterDataRepository.insert(masterData(SERVICE_KEY));
        goldSskuRepository.insertOrUpdateSsku(commonSsku(BASE_KEY));
        sskuPartnerVerdictRepository.insert(sskuPartnerVerdictResult(SERVICE_KEY));
        sskuPartnerVerdictRepository.insert(sskuPartnerVerdictResult(BASE_KEY));
        sskuGoldenVerdictRepository.insert(sskuVerdictResult(SERVICE_KEY));
        sskuGoldenVerdictRepository.insert(sskuVerdictResult(BASE_KEY));

        queue.enqueueAll(List.of(BASE_KEY), MdmEnqueueReason.CHANGED_SSKU_SILVER_DATA);

        // when
        executor.execute();

        // then
        var leftSskus = silverSskuRepository.findSskusByRootKeys(List.of(BASE_KEY));
        var leftRefItems = referenceItemRepository.findByIds(List.of(SERVICE_KEY));
        var leftMasterData = masterDataRepository.findByIds(List.of(SERVICE_KEY));
        var leftGoldSsku = masterDataRepository.findByIds(List.of(BASE_KEY));
        var leftPartnerVerdict = sskuPartnerVerdictRepository.findByIds(List.of(SERVICE_KEY, BASE_KEY));
        var leftGoldenVerdict = sskuGoldenVerdictRepository.findByIds(List.of(SERVICE_KEY, BASE_KEY));
        Assertions.assertThat(leftSskus).isEmpty();
        Assertions.assertThat(leftRefItems).isEmpty();
        Assertions.assertThat(leftMasterData).isEmpty();
        Assertions.assertThat(leftGoldSsku).isEmpty();
        Assertions.assertThat(leftPartnerVerdict).isEmpty();
        Assertions.assertThat(leftGoldenVerdict).isEmpty();

        // and
        var existedKeys = existenceRepository.retainExisting(List.of(SERVICE_KEY));
        Assertions.assertThat(existedKeys).isEmpty();
    }

    @Test
    public void whenSupplierSilverRemovedItShouldBeRemovedFromExistence() {
        // given
        existenceRepository.markExistence(List.of(SERVICE_KEY_2), true);

        var warehouseSilverBase = silverWithMdVersion(BASE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverService = silverWithMdVersion(SERVICE_KEY, WAREHOUSE_SOURCE, false);
        var warehouseSilverService2 = silverWithMdVersion(SERVICE_KEY_2, WAREHOUSE_SOURCE, false);
        var warehouseSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, WAREHOUSE_SOURCE))
            .setBaseSsku(warehouseSilverBase)
            .putServiceSsku(warehouseSilverService)
            .putServiceSsku(warehouseSilverService2);

        // one service removed - we should unmark it from existence
        var supplierSilverBase = silverWithMdVersion(BASE_KEY, SUPPLIER_SOURCE, false);
        var supplierSilverService = silverWithMdVersion(SERVICE_KEY, SUPPLIER_SOURCE, false);
        var supplierSilverService2 = silverWithMdVersion(SERVICE_KEY_2, SUPPLIER_SOURCE, true);
        var supplierSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, SUPPLIER_SOURCE))
            .setBaseSsku(supplierSilverBase)
            .putServiceSsku(supplierSilverService)
            .putServiceSsku(supplierSilverService2);

        silverSskuRepository.insertOrUpdateSskus(List.of(supplierSilverCommon, warehouseSilverCommon));
        queue.enqueueAll(List.of(BASE_KEY), MdmEnqueueReason.CHANGED_SSKU_SILVER_DATA);

        // when
        executor.execute();

        // then
        var existedKeys = existenceRepository.retainExisting(List.of(SERVICE_KEY_2));
        Assertions.assertThat(existedKeys).isEmpty();
    }

    @Test
    public void whenOnlySomeServiceRemovedMainGoldShouldNot() {
        // given
        existenceRepository.markExistence(List.of(SERVICE_KEY_2), true);

        // one service removed - we should unmark it from existence
        var supplierSilverBase = silverWithMdVersion(BASE_KEY, SUPPLIER_SOURCE, false);
        var supplierSilverService = silverWithMdVersion(SERVICE_KEY, SUPPLIER_SOURCE, false);
        var supplierSilverService2 = silverWithMdVersion(SERVICE_KEY_2, SUPPLIER_SOURCE, true);
        var supplierSilverCommon = new SilverCommonSsku(new SilverSskuKey(BASE_KEY, SUPPLIER_SOURCE))
            .setBaseSsku(supplierSilverBase)
            .putServiceSsku(supplierSilverService)
            .putServiceSsku(supplierSilverService2);

        silverSskuRepository.insertOrUpdateSskus(List.of(supplierSilverCommon));
        queue.enqueueAll(List.of(BASE_KEY), MdmEnqueueReason.CHANGED_SSKU_SILVER_DATA);
        referenceItemRepository.insert(referenceItem(SERVICE_KEY));
        referenceItemRepository.insert(referenceItem(SERVICE_KEY_2));
        masterDataRepository.insert(masterData(SERVICE_KEY));
        masterDataRepository.insert(masterData(SERVICE_KEY_2));
        goldSskuRepository.insertOrUpdateSsku(commonSsku(BASE_KEY));
        sskuPartnerVerdictRepository.insert(sskuPartnerVerdictResult(BASE_KEY));
        sskuPartnerVerdictRepository.insert(sskuPartnerVerdictResult(SERVICE_KEY));
        sskuPartnerVerdictRepository.insert(sskuPartnerVerdictResult(SERVICE_KEY_2));
        sskuGoldenVerdictRepository.insert(sskuVerdictResult(BASE_KEY));
        sskuGoldenVerdictRepository.insert(sskuVerdictResult(SERVICE_KEY));
        sskuGoldenVerdictRepository.insert(sskuVerdictResult(SERVICE_KEY_2));

        // when
        executor.execute();

        // then only SERVICE_1 related data left
        var leftRefItems = referenceItemRepository.findByIds(List.of(SERVICE_KEY, SERVICE_KEY_2));
        var leftMasterData = masterDataRepository.findByIds(List.of(SERVICE_KEY, SERVICE_KEY_2));
        var leftPartnerVerdict = sskuPartnerVerdictRepository
            .findByIds(List.of(BASE_KEY, SERVICE_KEY, SERVICE_KEY_2));
        var leftGoldenVerdict = sskuGoldenVerdictRepository
            .findByIds(List.of(BASE_KEY, SERVICE_KEY, SERVICE_KEY_2));
        Assertions.assertThat(leftRefItems).extracting(ReferenceItemWrapper::getKey).containsExactly(SERVICE_KEY);
        Assertions.assertThat(leftMasterData).extracting(MasterData::getShopSkuKey).containsExactly(SERVICE_KEY);
        Assertions.assertThat(leftPartnerVerdict).extracting(VerdictResult::getKey)
            .containsExactly(BASE_KEY, SERVICE_KEY);
        Assertions.assertThat(leftGoldenVerdict).extracting(VerdictResult::getKey)
            .containsExactly(BASE_KEY, SERVICE_KEY);

        // and common gold not touched
        var leftGoldSsku = goldSskuRepository.findSsku(BASE_KEY);
        Assertions.assertThat(leftGoldSsku).map(CommonSsku::getKey).contains(BASE_KEY);

        // but enqueued
        var enqueued = sskuToRefreshRepository.getUnprocessedBatch(100);
        Assertions.assertThat(enqueued).extracting(MdmQueueInfoBase::getEntityKey).containsExactly(BASE_KEY);
    }

    private SilverServiceSsku silverWithMdVersion(ShopSkuKey key,
                                                  MasterDataSource source,
                                                  boolean removed) {
        var silverServiceSsku = new SilverServiceSsku();
        silverServiceSsku.setKey(new SilverSskuKey(key, source));
        silverServiceSsku.setMasterDataVersion(MD_VERSION);
        var params = new ArrayList<SskuSilverParamValue>();
        var mdVersionParam = silverValue(key, DATACAMP_MASTER_DATA_VERSION, source);
        mdVersionParam.setNumeric(new BigDecimal(MD_VERSION));
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
        return (SskuSilverParamValue) new SskuSilverParamValue()
            .setShopSkuKey(key)
            .setMasterDataSource(source)
            .setMdmParamId(mdmParamId)
            .setXslName("-")
            .setSourceUpdatedTs(Instant.now())
            .setUpdatedTs(Instant.now());
    }

    private ReferenceItemWrapper referenceItem(ShopSkuKey key) {
        return new ReferenceItemWrapper(ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.MDM));
    }

    private MasterData masterData(ShopSkuKey key) {
        return new MasterData().setShopSkuKey(key);
    }


    private CommonSsku commonSsku(ShopSkuKey key) {
        return new CommonSsku(key);
    }

    private SskuPartnerVerdictResult sskuPartnerVerdictResult(ShopSkuKey key) {
        var verdict = new SskuPartnerVerdictResult();
        verdict.setKey(key);
        return verdict;
    }

    private SskuVerdictResult sskuVerdictResult(ShopSkuKey key) {
        var verdict = new SskuVerdictResult();
        verdict.setKey(key);
        return verdict;
    }

    private void prepareBusinessGroup(int businessId, int... serviceIds) {
        MdmSupplier business = new MdmSupplier()
            .setId(businessId)
            .setType(MdmSupplierType.BUSINESS);

        List<MdmSupplier> services = Arrays.stream(serviceIds)
            .mapToObj(it ->
                new MdmSupplier()
                    .setId(it)
                    .setType(MdmSupplierType.THIRD_PARTY)
                    .setBusinessId(business.getId())
                    .setBusinessEnabled(true)
            ).collect(Collectors.toList());

        mdmSupplierRepository.insert(business);
        mdmSupplierRepository.insertBatch(services);
    }

}
