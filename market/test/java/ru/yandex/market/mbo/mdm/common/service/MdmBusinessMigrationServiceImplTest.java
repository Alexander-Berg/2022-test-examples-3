package ru.yandex.market.mbo.mdm.common.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampUnitedOffer;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.grpc.stub.StreamObserver;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ServiceOfferMigrationInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.BusinessLockKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.BusinessLockStatus;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmBusinessStage;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.BusinessLockStatusRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ReferenceItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.ServiceOfferMigrationRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.BusinessSwitchInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.BusinessSwitchTransport;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmBusinessStageSwitcher;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmBusinessStageSwitcherImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManagerImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingServiceImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static org.mockito.Mockito.doNothing;

public class MdmBusinessMigrationServiceImplTest extends MdmBaseDbTestClass {
    @Autowired
    private MdmSupplierRepository supplierRepository;
    @Autowired
    private ServiceOfferMigrationRepository serviceOfferMigrationRepository;
    @Autowired
    private BusinessLockStatusRepository lockStatusRepository;
    @Autowired
    private MdmBusinessLockService businessLockService;
    @Autowired
    private MasterDataRepository masterDataRepository;
    @Autowired
    private ReferenceItemRepository referenceItemRepository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private ServiceSskuConverter sskuConverter;
    @Autowired
    private MdmQueuesManager mdmQueuesManager;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;

    private MdmBusinessMigrationServiceImpl mdmBusinessMigrationService;
    private MdmSskuGroupManager mdmSskuGroupManager;
    private MdmBusinessStageSwitcher businessStageSwitcher;

    private static final Integer BIZ_ID_1 = 1234;
    private static final Integer BIZ_ID_2 = 5678;
    private static final Integer BIZ_ID_3 = 8901;
    private static final Integer BIZ_ID_4 = 7324827;
    private static final Integer SHOP_ID = 13;
    private static final Integer SHOP_ID_2 = 14;
    private static final Integer SHOP_ID_NON_BUSINESS_ENABLED = 15;
    private static final ShopSkuKey SRC = new ShopSkuKey(BIZ_ID_1, "test");
    private static final ShopSkuKey DST = new ShopSkuKey(BIZ_ID_2, "test1");
    private static final ShopSkuKey RESULT = new ShopSkuKey(BIZ_ID_3, "test2");
    private static final ShopSkuKey UNEXISTENT = new ShopSkuKey(123, "test3");
    private EnhancedRandom random;
    private ArgumentCaptor<BusinessMigration.LockBusinessResponse> lockResponse;
    private ArgumentCaptor<BusinessMigration.UnlockBusinessResponse> unlockResponse;
    private ArgumentCaptor<BusinessMigration.MergeOffersResponse> mergeResponse;
    private StreamObserver<BusinessMigration.LockBusinessResponse> lockSoMock;
    private StreamObserver<BusinessMigration.UnlockBusinessResponse> unlockSoMock;
    private StreamObserver<BusinessMigration.MergeOffersResponse> mergeSoMock;

    @Before
    public void setup() {
        storageKeyValueService.putValue(MdmProperties.BUSINESS_MERGE_ENABLED_KEY, true);

        mdmSskuGroupManager = new MdmSskuGroupManagerImpl(masterDataRepository, referenceItemRepository,
            new MdmSupplierCachingServiceImpl(supplierRepository, storageKeyValueService), sskuConverter,
            mappingsCacheRepository,
            serviceOfferMigrationRepository, storageKeyValueService,
            sskuExistenceRepository, new BeruIdMock());

        businessStageSwitcher = new MdmBusinessStageSwitcherImpl(
            supplierRepository,
            mdmQueuesManager,
            storageKeyValueService
        );

        mdmBusinessMigrationService = new MdmBusinessMigrationServiceImpl(
            businessLockService,
            lockStatusRepository,
            serviceOfferMigrationRepository,
            supplierRepository,
            new MdmBusinessMigrationMonitoringServiceMock()
        );

        random = TestDataUtils.defaultRandom(12312);
        List<MdmSupplier> suppliers = List.of(
            new MdmSupplier().setId(BIZ_ID_1).setType(MdmSupplierType.BUSINESS)
                .setBusinessId(null).setBusinessEnabled(true),
            new MdmSupplier().setId(BIZ_ID_2).setType(MdmSupplierType.BUSINESS)
                .setBusinessEnabled(true),
            new MdmSupplier().setId(BIZ_ID_3),
            new MdmSupplier().setId(BIZ_ID_4),
            new MdmSupplier().setId(SHOP_ID).setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(BIZ_ID_1).setBusinessEnabled(true),
            new MdmSupplier().setId(SHOP_ID_2).setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(BIZ_ID_1).setBusinessEnabled(true),
            new MdmSupplier().setId(SHOP_ID_NON_BUSINESS_ENABLED).setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessId(BIZ_ID_1)
        );
        supplierRepository.insertOrUpdateAll(suppliers);
        lockSoMock = Mockito.mock(StreamObserver.class);
        lockResponse = ArgumentCaptor.forClass(BusinessMigration.LockBusinessResponse.class);
        doNothing().when(lockSoMock).onCompleted();
        doNothing().when(lockSoMock).onNext(lockResponse.capture());
        unlockSoMock = Mockito.mock(StreamObserver.class);
        unlockResponse = ArgumentCaptor.forClass(BusinessMigration.UnlockBusinessResponse.class);
        doNothing().when(unlockSoMock).onCompleted();
        doNothing().when(unlockSoMock).onNext(unlockResponse.capture());
        mergeSoMock = Mockito.mock(StreamObserver.class);
        mergeResponse = ArgumentCaptor.forClass(BusinessMigration.MergeOffersResponse.class);
        doNothing().when(mergeSoMock).onCompleted();
        doNothing().when(mergeSoMock).onNext(mergeResponse.capture());

        generateMasterData(DST);
    }

    @Test
    public void whenLockingByExistentShouldReturnOk() {
        var request = buildLockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID);
        mdmBusinessMigrationService.lock(request, lockSoMock);
        Assertions.assertThat(lockResponse.getValue().getMessage()).isEqualTo("Successfully locked");
        Assertions.assertThat(lockResponse.getValue().getStatus()).isEqualTo(BusinessMigration.Status.SUCCESS);
        var key1 = new BusinessLockKey(Long.valueOf(BIZ_ID_1), Long.valueOf(SHOP_ID));
        var key2 = new BusinessLockKey(Long.valueOf(BIZ_ID_2), Long.valueOf(SHOP_ID));
        List<BusinessLockStatus> lockStatuses = lockStatusRepository.findLocked(List.of(key1, key2));
        Assertions.assertThat(lockStatuses).hasSize(2);
        Assertions.assertThat(lockStatuses.stream().map(BusinessLockStatus::getKey).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(key1, key2);
    }

    @Test
    public void whenLockingByBusinessDisabledShouldReturnOkAndEnabledBusiness() {
        var request = buildLockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID_NON_BUSINESS_ENABLED);
        mdmBusinessMigrationService.lock(request, lockSoMock);
        Assertions.assertThat(lockResponse.getValue().getMessage()).isEqualTo("Successfully locked");
        Assertions.assertThat(lockResponse.getValue().getStatus()).isEqualTo(BusinessMigration.Status.SUCCESS);
        var key1 = new BusinessLockKey(Long.valueOf(BIZ_ID_1), Long.valueOf(SHOP_ID_NON_BUSINESS_ENABLED));
        var key2 = new BusinessLockKey(Long.valueOf(BIZ_ID_2), Long.valueOf(SHOP_ID_NON_BUSINESS_ENABLED));
        List<BusinessLockStatus> lockStatuses = lockStatusRepository.findLocked(List.of(key1, key2));
        Assertions.assertThat(lockStatuses).hasSize(2);
        Assertions.assertThat(lockStatuses.stream().map(BusinessLockStatus::getKey).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(key1, key2);

        Map<Integer, MdmSupplier> suppliers = supplierRepository.findByIds(
            Set.of(BIZ_ID_1, BIZ_ID_2, SHOP_ID_NON_BUSINESS_ENABLED)).stream()
            .collect(Collectors.toMap(MdmSupplier::getId, Function.identity()));
        Assertions.assertThat(suppliers.size()).isEqualTo(3);
        Assertions.assertThat(suppliers.get(SHOP_ID_NON_BUSINESS_ENABLED).getBusinessSwitchTransports().keySet())
            .containsExactly(BusinessSwitchTransport.MIGRATION_LOCK_HANDLE);
    }

    @Test
    public void whenLockingByUnexistentShouldReturnInProgress() {
        var request = buildLockRequest(1, 2, 3);
        mdmBusinessMigrationService.lock(request, lockSoMock);
        Assertions.assertThat(lockResponse.getValue().getMessage()).isEqualTo(
            "Unable to find 3 suppliers in MDM database, please try again later! Supplier ids: 1, 2, 3");
        Assertions.assertThat(lockResponse.getValue().getStatus()).isEqualTo(BusinessMigration.Status.IN_PROGRESS);
        List<BusinessLockStatus> lockStatuses = lockStatusRepository.findAll();
        Assertions.assertThat(lockStatuses).hasSize(0);
    }

    @Test
    public void whenLockingByLockedShouldReturnOk() {
        var request = buildLockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID);
        mdmBusinessMigrationService.lock(request, lockSoMock);
        var key1 = new BusinessLockKey(Long.valueOf(BIZ_ID_1), Long.valueOf(SHOP_ID));
        var key2 = new BusinessLockKey(Long.valueOf(BIZ_ID_2), Long.valueOf(SHOP_ID));
        List<BusinessLockStatus> lockStatuses = lockStatusRepository.findLocked(List.of(key1, key2));
        Assertions.assertThat(lockStatuses).hasSize(2);

        mdmBusinessMigrationService.lock(request, lockSoMock);
        Assertions.assertThat(lockResponse.getValue().getMessage()).isEqualTo(
            "Successfully locked, but 2 businesses out of 2 were already locked");
        Assertions.assertThat(lockResponse.getValue().getStatus()).isEqualTo(BusinessMigration.Status.SUCCESS);
        Assertions.assertThat(lockStatusRepository.findAll()).hasSize(2);
    }

    @Test
    public void whenUnlockingByLockedShouldReturnOk() {
        var request = buildLockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID);
        mdmBusinessMigrationService.lock(request, lockSoMock);
        var key1 = new BusinessLockKey(Long.valueOf(BIZ_ID_1), Long.valueOf(SHOP_ID));
        var key2 = new BusinessLockKey(Long.valueOf(BIZ_ID_2), Long.valueOf(SHOP_ID));
        List<BusinessLockStatus> lockStatuses = lockStatusRepository.findLocked(List.of(key1, key2));
        Assertions.assertThat(lockStatuses).hasSize(2);

        // simulate business change from MBI
        MdmSupplier shopIdSupplier = supplierRepository.findById(SHOP_ID);
        supplierRepository.update(shopIdSupplier.setBusinessId(BIZ_ID_2)
            .setBusinessEnabled(true)
            .setBusinessSwitchTransports(Map.of(BusinessSwitchTransport.MBI_LOGBROKER, Instant.now()))
            .setBusinessStateUpdatedTs(Instant.now()));

        mdmBusinessMigrationService.unlock(buildUnlockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID), unlockSoMock);
        Assertions.assertThat(unlockResponse.getValue().getMessage()).isEqualTo("Successfully unlocked");
        Assertions.assertThat(unlockResponse.getValue().getStatus()).isEqualTo(BusinessMigration.Status.SUCCESS);
        lockStatuses = lockStatusRepository.findLocked(List.of(key1, key2));
        Assertions.assertThat(lockStatuses).hasSize(0);
        lockStatuses = lockStatusRepository.findUnlocked(List.of(key1, key2));
        Assertions.assertThat(lockStatuses).hasSize(2);
        Assertions.assertThat(lockStatuses.stream().map(BusinessLockStatus::getKey).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(key1, key2);
    }

    @Test
    public void whenUnlockingByUnexistentShouldReturnError() {
        var request = buildUnlockRequest(1, 2, 3);
        mdmBusinessMigrationService.unlock(request, unlockSoMock);
        Assertions.assertThat(unlockResponse.getValue().getMessage()).isEqualTo(
            "Failed: unable to find 3 suppliers in MDM database, supplier ids: 1, 2, 3");
        Assertions.assertThat(unlockResponse.getValue().getStatus()).isEqualTo(BusinessMigration.Status.FAIL);
        List<BusinessLockStatus> lockStatuses = lockStatusRepository.findAll();
        Assertions.assertThat(lockStatuses).hasSize(0);
    }

    @Test
    public void whenUnlockByUnlockedShouldReturnOk() {
        var request = buildLockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID);
        mdmBusinessMigrationService.lock(request, lockSoMock);
        var key1 = new BusinessLockKey(Long.valueOf(BIZ_ID_1), Long.valueOf(SHOP_ID));
        var key2 = new BusinessLockKey(Long.valueOf(BIZ_ID_2), Long.valueOf(SHOP_ID));
        List<BusinessLockStatus> lockStatuses = lockStatusRepository.findLocked(List.of(key1, key2));
        Assertions.assertThat(lockStatuses).hasSize(2);

        // simulate business change from MBI
        MdmSupplier shopIdSupplier = supplierRepository.findById(SHOP_ID);
        supplierRepository.update(shopIdSupplier.setBusinessId(BIZ_ID_2)
            .setBusinessEnabled(true)
            .setBusinessSwitchTransports(Map.of(BusinessSwitchTransport.MBI_LOGBROKER, Instant.now()))
            .setBusinessStateUpdatedTs(Instant.now()));

        mdmBusinessMigrationService.unlock(buildUnlockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID), unlockSoMock);
        Assertions.assertThat(lockStatusRepository.findUnlocked(List.of(key1, key2))).hasSize(2);

        mdmBusinessMigrationService.unlock(buildUnlockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID), unlockSoMock);
        Assertions.assertThat(unlockResponse.getValue().getMessage()).isEqualTo(
            "Successfully unlocked, but 2 businesses out of 2 were already unlocked");
        Assertions.assertThat(unlockResponse.getValue().getStatus()).isEqualTo(BusinessMigration.Status.SUCCESS);
        Assertions.assertThat(lockStatusRepository.findAll()).hasSize(2);
    }

    @Test
    public void whenUnlockByPartiallyUnlockedShouldReturnOk() {
        var request = buildLockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID);
        mdmBusinessMigrationService.lock(request, lockSoMock);
        mdmBusinessMigrationService.lock(buildLockRequest(BIZ_ID_3, BIZ_ID_4, SHOP_ID), lockSoMock);

        // simulate business change from MBI
        MdmSupplier shopIdSupplier = supplierRepository.findById(SHOP_ID);
        supplierRepository.update(shopIdSupplier.setBusinessId(BIZ_ID_2)
            .setBusinessEnabled(true)
            .setBusinessSwitchTransports(Map.of(BusinessSwitchTransport.MBI_LOGBROKER, Instant.now()))
            .setBusinessStateUpdatedTs(Instant.now()));

        mdmBusinessMigrationService.unlock(buildUnlockRequest(BIZ_ID_1, BIZ_ID_1, SHOP_ID), unlockSoMock);

        mdmBusinessMigrationService.unlock(buildUnlockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID), unlockSoMock);
        Assertions.assertThat(unlockResponse.getValue().getMessage()).isEqualTo("Successfully unlocked");
        Assertions.assertThat(unlockResponse.getValue().getStatus()).isEqualTo(BusinessMigration.Status.SUCCESS);
    }

    @Test
    public void whenUnlockWhileMbiBusinessMigrationDidNotHappenedShouldReturnInProgress() {
        mdmBusinessMigrationService.lock(buildLockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID), lockSoMock);
        var key1 = new BusinessLockKey(Long.valueOf(BIZ_ID_1), Long.valueOf(SHOP_ID));
        var key2 = new BusinessLockKey(Long.valueOf(BIZ_ID_2), Long.valueOf(SHOP_ID));
        List<BusinessLockStatus> lockStatuses = lockStatusRepository.findLocked(List.of(key1, key2));
        Assertions.assertThat(lockStatuses).hasSize(2);

        mdmBusinessMigrationService.unlock(buildUnlockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID), unlockSoMock);
        Assertions.assertThat(unlockResponse.getValue().getMessage())
            .isEqualTo("Failed: service supplier 13 has not migrated to business 5678 in MDM database yet");
        Assertions.assertThat(unlockResponse.getValue().getStatus()).isEqualTo(BusinessMigration.Status.IN_PROGRESS);
        lockStatuses = lockStatusRepository.findLocked(List.of(key1, key2));
        Assertions.assertThat(lockStatuses).hasSize(2);
        Assertions.assertThat(lockStatuses.stream().map(BusinessLockStatus::getKey).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(key1, key2);
        lockStatuses = lockStatusRepository.findUnlocked(List.of(key1, key2));
        Assertions.assertThat(lockStatuses).isEmpty();
    }

    @Test
    public void whenUnlockWhileBusinessEnabledHasNotArrivedFromMbiShouldReturnInProgress() {
        // первичный заезд: srcBusinessId = dstBusinessId
        mdmBusinessMigrationService.lock(buildLockRequest(BIZ_ID_1, BIZ_ID_1, SHOP_ID_NON_BUSINESS_ENABLED), lockSoMock);
        var key1 = new BusinessLockKey(Long.valueOf(BIZ_ID_1), Long.valueOf(SHOP_ID_NON_BUSINESS_ENABLED));
        List<BusinessLockStatus> lockStatuses = lockStatusRepository.findLocked(List.of(key1));
        Assertions.assertThat(lockStatuses).hasSize(1);

        // 1st unlock attempt
        mdmBusinessMigrationService.unlock(
            buildUnlockRequest(BIZ_ID_1, BIZ_ID_1, SHOP_ID_NON_BUSINESS_ENABLED), unlockSoMock);
        Assertions.assertThat(unlockResponse.getValue().getStatus()).isEqualTo(BusinessMigration.Status.IN_PROGRESS);
        Assertions.assertThat(unlockResponse.getValue().getMessage()).isEqualTo(
            "Failed: service supplier 15 has not received business_enabled flag from MBI yet, cannot unlock");
        lockStatuses = lockStatusRepository.findLocked(List.of(key1));
        Assertions.assertThat(lockStatuses.stream().map(BusinessLockStatus::getKey).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(key1);

        List<BusinessLockStatus> unlocked = lockStatusRepository.findUnlocked(List.of(key1));
        Assertions.assertThat(unlocked).isEmpty();

        // simulate business enabled flag from MBI
        businessStageSwitcher.applySupplierBusinessChanges(List.of(
            BusinessSwitchInfo.stageSwitch(SHOP_ID_NON_BUSINESS_ENABLED, MdmBusinessStage.BUSINESS_ENABLED, 0,
                BusinessSwitchTransport.MBI_LOGBROKER)
        ));

        // 2nd unlock attempt
        mdmBusinessMigrationService.unlock(
            buildUnlockRequest(BIZ_ID_1, BIZ_ID_1, SHOP_ID_NON_BUSINESS_ENABLED), unlockSoMock);
        Assertions.assertThat(unlockResponse.getValue().getStatus()).isEqualTo(BusinessMigration.Status.SUCCESS);
        Assertions.assertThat(unlockResponse.getValue().getMessage()).isEqualTo("Successfully unlocked");
        Assertions.assertThat(lockStatusRepository.findLocked(List.of(key1))).isEmpty();

        unlocked = lockStatusRepository.findUnlocked(List.of(key1));
        Assertions.assertThat(unlocked.stream().map(BusinessLockStatus::getKey).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(key1);
    }

    @Test
    public void whenMergeByExistentShouldEnqueue() {
        var lockRequest = buildLockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID);
        mdmBusinessMigrationService.lock(lockRequest, lockSoMock);
        mdmBusinessMigrationService.lock(buildLockRequest(BIZ_ID_3, BIZ_ID_4, SHOP_ID), lockSoMock);

        var request = buildMergeRequest(SRC, List.of(SHOP_ID), DST, DST);
        mdmBusinessMigrationService.merge(request, mergeSoMock);
        checkMergeResponseItem(mergeResponse.getValue().getMergeResponseItem(0),
            DST,
            BusinessMigration.Resolution.MERGEABLE);

        Assertions.assertThat(mergeResponse.getValue().getSuccess()).isTrue();
        Assertions.assertThat(mergeResponse.getValue().getMessage()).isEqualTo("Merge request completed succesfully");

        List<ServiceOfferMigrationInfo> enqueued = serviceOfferMigrationRepository.findAll();
        Assertions.assertThat(enqueued).usingElementComparatorIgnoringFields("addedTimestamp")
            .containsExactlyInAnyOrder(new ServiceOfferMigrationInfo()
                .setSupplierId(SHOP_ID).setShopSku(SRC.getShopSku())
                .setSrcBusinessId(SRC.getSupplierId()).setDstBusinessId(DST.getSupplierId()));
    }

    @Test
    public void whenMergeByUnexistentShouldReturnIncompatible() {
        var request = buildMergeRequest(SRC, List.of(SHOP_ID), UNEXISTENT, UNEXISTENT);
        mdmBusinessMigrationService.merge(request, mergeSoMock);
        checkMergeResponseItem(mergeResponse.getValue().getMergeResponseItem(0),
            UNEXISTENT,
            BusinessMigration.Resolution.INCOMPATIBLE);

        Assertions.assertThat(mergeResponse.getValue().getSuccess()).isFalse();
        Assertions.assertThat(mergeResponse.getValue().getMessage()).isEqualTo(
            "Some of the request items have validation errors: [supplier_id: 13, shop_sku: test] " +
                "-> the following supplier ids are not found in MDM: 123");

        List<ServiceOfferMigrationInfo> enqueued = serviceOfferMigrationRepository.findAll();
        Assertions.assertThat(enqueued).hasSize(0);
    }

    @Test
    public void whenMergeWithoutShopIdShouldReturnIncompatible() {
        var request = buildMergeRequest(SRC, List.of(), DST, RESULT);
        mdmBusinessMigrationService.merge(request, mergeSoMock);
        checkMergeResponseItem(mergeResponse.getValue().getMergeResponseItem(0),
            RESULT,
            BusinessMigration.Resolution.INCOMPATIBLE);

        Assertions.assertThat(mergeResponse.getValue().getSuccess()).isFalse();
        Assertions.assertThat(mergeResponse.getValue().getMessage()).isEqualTo(
            "Some of the request items have validation errors: [supplier_id: 1234, shop_sku: test] " +
                "-> item has 0 service offers in source (1 required), can't merge");

        List<ServiceOfferMigrationInfo> enqueued = serviceOfferMigrationRepository.findAll();
        Assertions.assertThat(enqueued).hasSize(0);
    }

    @Test
    public void whenMergeWithMultipleShopIdsShouldReturnIncompatible() {
        var request = buildMergeRequest(SRC, List.of(SHOP_ID, SHOP_ID + 1), DST, RESULT);
        mdmBusinessMigrationService.merge(request, mergeSoMock);
        checkMergeResponseItem(mergeResponse.getValue().getMergeResponseItem(0),
            RESULT,
            BusinessMigration.Resolution.INCOMPATIBLE);

        Assertions.assertThat(mergeResponse.getValue().getSuccess()).isFalse();
        Assertions.assertThat(mergeResponse.getValue().getMessage()).isEqualTo(
            "Some of the request items have validation errors: [supplier_id: 1234, shop_sku: test] " +
                "-> item has 2 service offers in source (1 required), can't merge");

        List<ServiceOfferMigrationInfo> enqueued = serviceOfferMigrationRepository.findAll();
        Assertions.assertThat(enqueued).hasSize(0);
    }

    @Test
    public void whenMergeSomeOfItemsFailsShoudReturnFailure() {
        var lockRequest = buildLockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID);
        mdmBusinessMigrationService.lock(lockRequest, lockSoMock);
        mdmBusinessMigrationService.lock(buildLockRequest(BIZ_ID_3, BIZ_ID_4, SHOP_ID), lockSoMock);

        var request1 = buildMergeRequest(SRC, List.of(SHOP_ID), DST, RESULT);
        var request2 = buildMergeRequest(SRC, List.of(), DST, RESULT);
        var request = BusinessMigration.MergeOffersRequest.newBuilder();
        request.addMergeRequestItem(request1.getMergeRequestItem(0));
        request.addMergeRequestItem(request2.getMergeRequestItem(0));

        mdmBusinessMigrationService.merge(request.build(), mergeSoMock);

        Assertions.assertThat(mergeResponse.getValue().getSuccess()).isFalse();
        Assertions.assertThat(mergeResponse.getValue().getMessage()).isEqualTo(
            "Some of the request items have validation errors: [supplier_id: 1234, shop_sku: test] " +
                "-> item has 0 service offers in source (1 required), can't merge");

        checkMergeResponseItem(mergeResponse.getValue().getMergeResponseItem(0),
            RESULT,
            BusinessMigration.Resolution.MERGEABLE);
        checkMergeResponseItem(mergeResponse.getValue().getMergeResponseItem(1),
            RESULT,
            BusinessMigration.Resolution.INCOMPATIBLE);

        List<ServiceOfferMigrationInfo> enqueued = serviceOfferMigrationRepository.findAll();
        Assertions.assertThat(enqueued).hasSize(0);
    }

    @Test
    public void whenMergeWithDryRunReturnError() {
        var request = buildMergeRequest(SRC, List.of(), UNEXISTENT, RESULT, true);
        mdmBusinessMigrationService.merge(request, mergeSoMock);

        Assertions.assertThat(mergeResponse.getValue().getSuccess()).isFalse();
        checkMergeResponseItem(mergeResponse.getValue().getMergeResponseItem(0),
            RESULT,
            BusinessMigration.Resolution.INCOMPATIBLE);

        List<ServiceOfferMigrationInfo> enqueued = serviceOfferMigrationRepository.findAll();
        Assertions.assertThat(enqueued).hasSize(0);
    }

    @Test
    public void whenMergeAndNotLockedShouldReturnError() {
        var request = buildMergeRequest(SRC, List.of(SHOP_ID), DST, DST);
        mdmBusinessMigrationService.merge(request, mergeSoMock);
        checkMergeResponseItem(mergeResponse.getValue().getMergeResponseItem(0),
            DST,
            BusinessMigration.Resolution.INCOMPATIBLE);

        Assertions.assertThat(mergeResponse.getValue().getSuccess()).isFalse();
        Assertions.assertThat(mergeResponse.getValue().getMessage()).isEqualTo(
            "Some of the request items have validation errors: [supplier_id: 13, shop_sku: test] " +
                "-> the following businesses are not locked in MDM: 1234, 5678");

        List<ServiceOfferMigrationInfo> enqueued = serviceOfferMigrationRepository.findAll();
        Assertions.assertThat(enqueued).hasSize(0);
    }

    @Test
    public void whenUnlockWithCancelShouldReturnFail() {
        var lockRequest = buildLockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID);
        mdmBusinessMigrationService.lock(lockRequest, lockSoMock);

        ServiceOfferMigrationInfo migrationInfo = new ServiceOfferMigrationInfo()
            .setProcessed(false)
            .setSupplierId(SHOP_ID)
            .setShopSku("1")
            .setSrcBusinessId(BIZ_ID_1)
            .setDstBusinessId(BIZ_ID_2)
            .setAddedTimestamp(Instant.now());
        serviceOfferMigrationRepository.insertOrUpdate(migrationInfo);

        var unlockRequest =
            buildUnlockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID, true);
        mdmBusinessMigrationService.unlock(unlockRequest, unlockSoMock);

        Assertions.assertThat(unlockResponse.getValue().getMessage()).isEqualTo(String.format(
            "Failed: service suppliers: %d has unprocessed migration requests", SHOP_ID));
        Assertions.assertThat(unlockResponse.getValue().getStatus()).isEqualTo(BusinessMigration.Status.FAIL);
    }

    @Test
    public void whenUnlockWithCalcelSouldReturnSuccess() {
        var lockRequest = buildLockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID);
        mdmBusinessMigrationService.lock(lockRequest, lockSoMock);

        var unlockRequest =
            buildUnlockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID, true);
        mdmBusinessMigrationService.unlock(unlockRequest, unlockSoMock);

        Assertions.assertThat(unlockResponse.getValue().getMessage()).isEqualTo("Successfully unlocked");
        Assertions.assertThat(unlockResponse.getValue().getStatus()).isEqualTo(BusinessMigration.Status.SUCCESS);

        mdmBusinessMigrationService.lock(lockRequest, lockSoMock);
        ServiceOfferMigrationInfo migrationInfo = new ServiceOfferMigrationInfo()
            .setProcessed(true)
            .setSupplierId(SHOP_ID)
            .setShopSku("1")
            .setSrcBusinessId(BIZ_ID_3)
            .setDstBusinessId(BIZ_ID_1)
            .setAddedTimestamp(Instant.now());
        serviceOfferMigrationRepository.insertOrUpdate(migrationInfo);
        mdmBusinessMigrationService.unlock(unlockRequest, unlockSoMock);

        Assertions.assertThat(unlockResponse.getValue().getMessage()).isEqualTo("Successfully unlocked");
        Assertions.assertThat(unlockResponse.getValue().getStatus()).isEqualTo(BusinessMigration.Status.SUCCESS);
    }

    @Test
    public void whenUnlockingByLockedWhiteShouldReturnOkAndProcessMigrations() {
        var request = buildLockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID);
        mdmBusinessMigrationService.lock(request, lockSoMock);
        var key1 = new BusinessLockKey(Long.valueOf(BIZ_ID_1), Long.valueOf(SHOP_ID));
        var key2 = new BusinessLockKey(Long.valueOf(BIZ_ID_2), Long.valueOf(SHOP_ID));
        List<BusinessLockStatus> lockStatuses = lockStatusRepository.findLocked(List.of(key1, key2));
        Assertions.assertThat(lockStatuses).hasSize(2);

        var mergeRequest = buildMergeRequest(SRC, List.of(SHOP_ID), DST, DST);
        mdmBusinessMigrationService.merge(mergeRequest, mergeSoMock);

        // simulate business change from MBI
        MdmSupplier shopIdSupplier = supplierRepository.findById(SHOP_ID);
        supplierRepository.update(shopIdSupplier.setBusinessId(BIZ_ID_2)
            .setBusinessEnabled(true)
            .setBusinessSwitchTransports(Map.of(BusinessSwitchTransport.MBI_LOGBROKER, Instant.now()))
            .setBusinessStateUpdatedTs(Instant.now())
            .setType(MdmSupplierType.MARKET_SHOP));

        mdmBusinessMigrationService.unlock(buildUnlockRequest(BIZ_ID_1, BIZ_ID_2, SHOP_ID), unlockSoMock);
        List<ServiceOfferMigrationInfo> migrations =
            serviceOfferMigrationRepository.findBySupplierIds(List.of(SHOP_ID));
        Assertions.assertThat(migrations.stream().allMatch(migrationInfo -> migrationInfo.isProcessed())).isTrue();
        Assertions.assertThat(unlockResponse.getValue().getMessage()).isEqualTo("Successfully unlocked");
        Assertions.assertThat(unlockResponse.getValue().getStatus()).isEqualTo(BusinessMigration.Status.SUCCESS);
        lockStatuses = lockStatusRepository.findLocked(List.of(key1, key2));
        Assertions.assertThat(lockStatuses).hasSize(0);
        lockStatuses = lockStatusRepository.findUnlocked(List.of(key1, key2));
        Assertions.assertThat(lockStatuses).hasSize(2);
        Assertions.assertThat(lockStatuses.stream().map(BusinessLockStatus::getKey).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(key1, key2);
    }

    private BusinessMigration.LockBusinessRequest buildLockRequest(int src, int dst, int shopId) {
        BusinessMigration.LockBusinessRequest.Builder request = BusinessMigration.LockBusinessRequest.newBuilder();
        request.setSrcBusinessId(Long.valueOf(src));
        request.setDstBusinessId(Long.valueOf(dst));
        request.setShopId(Long.valueOf(shopId));
        return request.build();
    }

    private BusinessMigration.UnlockBusinessRequest buildUnlockRequest(int src, int dst, int shopId) {
        return buildUnlockRequest(src, dst, shopId, false);
    }

    private BusinessMigration.UnlockBusinessRequest buildUnlockRequest(int src, int dst, int shopId, boolean cancel) {
        BusinessMigration.UnlockBusinessRequest.Builder request = BusinessMigration.UnlockBusinessRequest.newBuilder();
        request.setSrcBusinessId(Long.valueOf(src));
        request.setDstBusinessId(Long.valueOf(dst));
        request.setShopId(Long.valueOf(shopId));
        request.setCancel(cancel);
        return request.build();
    }

    private BusinessMigration.MergeOffersRequest buildMergeRequest(ShopSkuKey src, List<Integer> srcServiceIds,
                                                                   ShopSkuKey dst, ShopSkuKey result,
                                                                   boolean dryRun) {
        BusinessMigration.MergeOffersRequest.Builder request = BusinessMigration.MergeOffersRequest.newBuilder();
        setMergeRequestItem(request, src, srcServiceIds, dst, result, dryRun);
        return request.build();
    }

    private BusinessMigration.MergeOffersRequest buildMergeRequest(ShopSkuKey src, List<Integer> srcServiceIds,
                                                                   ShopSkuKey dst, ShopSkuKey result) {
        return buildMergeRequest(src, srcServiceIds, dst, result, false);
    }


    private BusinessMigration.MergeOffersRequest.Builder setMergeRequestItem(
        BusinessMigration.MergeOffersRequest.Builder request, ShopSkuKey src, List<Integer> serviceIds,
        ShopSkuKey dst, ShopSkuKey result, boolean dryRun) {

        return request.addAllMergeRequestItem(List.of(BusinessMigration.MergeOffersRequestItem.newBuilder()
            .setSource(generateUnitedOffer(src, serviceIds.toArray(new Integer[0])))
            .setTarget(generateUnitedOffer(dst))
            .setResult(generateUnitedOffer(result))
            .build()))
            .setDryRun(dryRun);
    }

    private DataCampUnitedOffer.UnitedOffer generateUnitedOffer(ShopSkuKey businesskey,
                                                                Integer... serviceIds) {
        var builder = DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setOfferId(businesskey.getShopSku())
                    .setBusinessId(businesskey.getSupplierId()))
                .build());

        for (Integer serviceId : serviceIds) {
            builder.putService(serviceId,
                DataCampOffer.Offer.newBuilder()
                    .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                        .setOfferId(businesskey.getShopSku())
                        .setBusinessId(businesskey.getSupplierId())
                        .setShopId(serviceId)
                        .build())
                    .build());
        }
        return builder.build();
    }

    private void checkMergeResponseItem(BusinessMigration.MergeOffersResponseItem item, ShopSkuKey expectedSsku,
                                        BusinessMigration.Resolution expectedResolution) {
        Assertions.assertThat(item.getResult().getBasic().getIdentifiers().getBusinessId())
            .isEqualTo(expectedSsku.getSupplierId());
        Assertions.assertThat(item.getResult().getBasic().getIdentifiers().getOfferId())
            .isEqualTo(expectedSsku.getShopSku());
        Assertions.assertThat(item.getResolution()).isEqualTo(expectedResolution);
    }

    private MasterData generateMasterData(ShopSkuKey key) {
        MasterData md = TestDataUtils.generateMasterData(key, random);
        masterDataRepository.insert(md);
        return md;
    }
}
