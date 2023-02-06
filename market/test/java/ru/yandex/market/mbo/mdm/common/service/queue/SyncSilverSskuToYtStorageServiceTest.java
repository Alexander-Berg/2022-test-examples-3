package ru.yandex.market.mbo.mdm.common.service.queue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepositoryParamValueImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.StorageApiSilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SilverSskuYtStorageQueue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.TestBmdmUtils;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.ssku.BmdmEntityToSilverCommonSskuConverter;
import ru.yandex.market.mbo.mdm.common.service.bmdm.proto.MdmEntityStorageService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class SyncSilverSskuToYtStorageServiceTest extends MdmBaseDbTestClass {
    @Autowired
    private SilverSskuYtStorageQueue sskuYtStorageQueue;
    @Autowired
    private StorageKeyValueService skv;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private MdmSupplierCachingService mdmSupplierCachingService;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;
    @Autowired
    private MdmEntityStorageService mdmEntityStorageService;
    @Autowired
    private BmdmEntityToSilverCommonSskuConverter bmdmEntityToSilverCommonSskuConverter;
    @Autowired
    private MdmParamCache mdmParamCache;

    private SilverSskuRepository pgRepository;
    private SilverSskuRepository ytRepository;

    private SyncSilverSskuToYtStorageService service;

    @Before
    public void setup() {
        pgRepository = new SilverSskuRepositoryParamValueImpl(jdbcTemplate, transactionTemplate, mdmSskuGroupManager);
        ytRepository = new StorageApiSilverSskuRepository(
            mdmEntityStorageService,
            bmdmEntityToSilverCommonSskuConverter,
            mdmSskuGroupManager
        );
        service = new SyncSilverSskuToYtStorageService(
            sskuYtStorageQueue,
            skv,
            pgRepository,
            ytRepository
        );
        skv.putValue(MdmProperties.SILVER_SSKU_YT_STORAGE_UPLOAD_ENABLED, true);
        skv.invalidateCache();
    }

    @Test
    public void testQueueProcessing() {
        List<SilverCommonSsku> sskus = sskus(100);
        pgRepository.insertOrUpdateSskus(sskus);
        ytRepository.insertOrUpdateSskus(sskus);

        sskuYtStorageQueue.enqueueAll(
            sskus.stream()
                .map(SilverCommonSsku::getBusinessKey)
                .collect(Collectors.toList())
        );

        service.processQueueItems();
        Assertions.assertThat(sskuYtStorageQueue.findAll()).allMatch(MdmQueueInfoBase::isProcessed);
        Assertions.assertThat(ytRepository.findAll())
            .filteredOn(pv -> pv.getMdmParamId() != KnownMdmParams.BMDM_ID)
            .containsExactlyInAnyOrderElementsOf(sskus.stream()
                .map(SilverCommonSsku::getAllValues)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));
    }

    @Test
    public void testMissingInRepoProcessed() {
        List<SilverCommonSsku> sskus = sskus(100);
        sskuYtStorageQueue.enqueueAll(
            sskus.stream()
                .map(SilverCommonSsku::getBusinessKey)
                .collect(Collectors.toList())
        );
        service.processQueueItems();
        Assertions.assertThat(sskuYtStorageQueue.findAll()).allMatch(MdmQueueInfoBase::isProcessed);
        Assertions.assertThat(ytRepository.findAll()).isEmpty();
    }

    @Test
    public void whenSilverDeletedInPgReplaceItInYtWithEmptySsku() {
        List<SilverCommonSsku> sskus = sskus(100);
        pgRepository.insertOrUpdateSskus(sskus);
        ytRepository.insertOrUpdateSskus(sskus);

        sskuYtStorageQueue.enqueueAll(
            sskus.stream()
                .map(SilverCommonSsku::getBusinessKey)
                .collect(Collectors.toList())
        );
        service.processQueueItems();

        Assertions.assertThat(sskuYtStorageQueue.findAll()).allMatch(MdmQueueInfoBase::isProcessed);
        Assertions.assertThat(ytRepository.findAll())
            .filteredOn(pv -> pv.getMdmParamId() != KnownMdmParams.BMDM_ID)
            .containsExactlyInAnyOrderElementsOf(sskus.stream()
                .map(SilverCommonSsku::getAllValues)
                .flatMap(Collection::stream)
                .collect(Collectors.toList()));

        pgRepository.deleteSskus(sskus);

        sskuYtStorageQueue.enqueueAll(
            sskus.stream()
                .map(SilverCommonSsku::getBusinessKey)
                .collect(Collectors.toList())
        );
        service.processQueueItems();

        Assertions.assertThat(sskuYtStorageQueue.findAll()).allMatch(MdmQueueInfoBase::isProcessed);
        Assertions.assertThat(ytRepository.findAll())
            .allMatch(pv -> pv.getMdmParamId() == KnownMdmParams.BMDM_ID);
    }

    @Test
    public void testNoSyncByServiceKey() {
        // given
        Random random = new Random("Anna Dalvey".hashCode());

        int businessId = 10;
        int serviceId = 9;
        mdmSupplierRepository.insertBatch(
            new MdmSupplier()
                .setId(businessId)
                .setType(MdmSupplierType.BUSINESS),
            new MdmSupplier()
                .setId(serviceId)
                .setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessEnabled(true)
                .setBusinessStateUpdatedTs(Instant.now())
                .setBusinessId(businessId)
        );

        String shopSku = "abc";
        sskuExistenceRepository.markExistence(new ShopSkuKey(serviceId, shopSku), true);

        MasterDataSource source = new MasterDataSource(MasterDataSourceType.WAREHOUSE, "145");
        SilverSskuKey serviceKey = new SilverSskuKey(new ShopSkuKey(serviceId, shopSku), source);
        SilverSskuKey rootKey = new SilverSskuKey(new ShopSkuKey(businessId, shopSku), source);

        SilverCommonSsku silverCommonSsku = new SilverCommonSsku(rootKey);
        KnownMdmParams.WEIGHT_DIMENSIONS_PARAMS.stream()
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(silverCommonSsku::addBaseValue);
        SilverServiceSsku silverServiceSsku = new SilverServiceSsku(serviceKey);
        silverServiceSsku.addParamValue(
            TestMdmParamUtils.createRandomMdmParamValue(random, mdmParamCache.get(KnownMdmParams.DELIVERY_TIME)));
        silverCommonSsku.putServiceSsku(silverServiceSsku);

        pgRepository.insertOrUpdateSsku(silverCommonSsku);

        // when sync by service key
        sskuYtStorageQueue.enqueueAll(List.of(serviceKey));
        service.processQueueItems();

        // then no sync
        Assertions.assertThat(ytRepository.findSskusBySilverKeys(List.of(rootKey, serviceKey)))
            .isEmpty();

        // when sync deleted by service key
        sskuYtStorageQueue.enqueueAll(List.of(rootKey));
        service.processQueueItems();
        pgRepository.deleteSsku(silverCommonSsku);
        sskuYtStorageQueue.enqueueAll(List.of(serviceKey));
        service.processQueueItems();

        // then no sync (we delete from yt only by root key)
        Assertions.assertThat(ytRepository.findSsku(rootKey))
            .map(TestBmdmUtils::removeBmdmIdAndVersion)
            .hasValue(silverCommonSsku);
    }

    private List<SilverCommonSsku> sskus(int count) {
        List<MdmSupplier> suppliers = new ArrayList<>(count);
        List<ShopSkuKey> keys = new ArrayList<>(count);
        List<SilverCommonSsku> sskus = IntStream.range(1, count).boxed().map(businessId -> {
            suppliers.add(new MdmSupplier()
                .setId(businessId)
                .setType(MdmSupplierType.BUSINESS));
            suppliers.add(new MdmSupplier()
                .setId(count + businessId)
                .setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessEnabled(true)
                .setBusinessStateUpdatedTs(Instant.now())
                .setBusinessId(businessId)
            );
            var serviceKey = new ShopSkuKey(count + businessId, "fddfhtahw");
            var businessKey = new ShopSkuKey(businessId, "fddfhtahw");
            keys.add(serviceKey);
            return ssku(businessKey);
        }).collect(Collectors.toList());
        mdmSupplierRepository.insertBatch(suppliers);
        sskuExistenceRepository.markExistence(keys, true);
        mdmSupplierCachingService.refresh();
        return sskus;

    }

    private SilverCommonSsku ssku(ShopSkuKey key) {
        var ssku = new SilverCommonSsku(new SilverSskuKey(key, MasterDataSource.DEFAULT_SOURCE));
        ssku.addBaseValue(new SskuSilverParamValue()
            .setMdmParamId(KnownMdmParams.BOX_COUNT)
            .setXslName(mdmParamCache.get(KnownMdmParams.BOX_COUNT).getXslName())
            .setNumeric(BigDecimal.valueOf(12)));
        return ssku;
    }
}
