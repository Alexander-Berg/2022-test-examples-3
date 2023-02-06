package ru.yandex.market.deepmind.app.controllers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.db.monitoring.DbMonitoring;
import ru.yandex.market.db.monitoring.DbMonitoringUnit;
import ru.yandex.market.deepmind.app.DeepmindBaseAppDbTestClass;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.WarehouseCargotypeChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.SkuTypeEnum;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.ImportMskuOfferHelperService;
import ru.yandex.market.deepmind.common.services.MboCategoryOfferChangesServiceHelper;
import ru.yandex.market.deepmind.common.services.OfferAvailabilityYtService;
import ru.yandex.market.deepmind.common.services.OfferAvailabilityYtServiceImpl;
import ru.yandex.market.deepmind.common.services.cargotype.DeepmindCargoTypeCachingServiceMock;
import ru.yandex.market.deepmind.common.services.cargotype.SyncCargotypeService;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverter;
import ru.yandex.market.deepmind.common.solomon.DeepmindSolomonPushService;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCargoTypesDto;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.msku.CargoType;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.http.MboCategoryOfferChanges;
import ru.yandex.market.mboc.http.MboCategoryOfferChangesService;
import ru.yandex.market.mboc.http.SupplierOffer;

import static ru.yandex.market.mboc.http.MboCategoryOfferChanges.LogisticSchema.FBY;
import static ru.yandex.market.mboc.http.MboCategoryOfferChanges.LogisticSchema.FBY_PLUS;

/**
 * Tests of {@link DeepmindDevController}.
 */
public class DeepmindDevControllerTest extends DeepmindBaseAppDbTestClass {

    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    private SskuStatusRepository sskuStatusRepository;
    @Resource
    private OffersConverter offersConverter;
    @Resource
    private BeruId beruId;
    @Resource(name = "deepmindTransactionHelper")
    private TransactionHelper transactionHelper;

    private LMSClient lmsClientMock;
    private DeepmindDevController controller;
    private OfferAvailabilityYtService offerAvailabilityYtService;
    private DbMonitoring dbMonitoring;
    private final MboCategoryOfferChangesService offerChangesService
        = Mockito.mock(MboCategoryOfferChangesService.class);

    @Before
    public void setUp() throws Exception {
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_CROSSDOCK);

        offersConverter.clearCache();
        lmsClientMock = Mockito.mock(LMSClient.class);
        dbMonitoring = Mockito.mock(DbMonitoring.class);
        offerAvailabilityYtService = Mockito.spy(new OfferAvailabilityYtServiceImpl(
            Mockito.mock(UnstableInit.class),
            Mockito.mock(UnstableInit.class),
            YPath.simple("//tmp"),
            YPath.simple("//tmp"),
            YPath.simple("//home/link"),
            dbMonitoring,
            Mockito.mock(StorageKeyValueService.class)));

        var helperService = new ImportMskuOfferHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            offersConverter, Mockito.mock(DeepmindSolomonPushService.class), beruId, transactionHelper);
        controller = new DeepmindDevController(null, new DeepmindCargoTypeCachingServiceMock(),
            null, null,
            new SyncCargotypeService(deepmindWarehouseRepository, lmsClientMock,
                Mockito.mock(WarehouseCargotypeChangedHandler.class)), offerAvailabilityYtService, offerChangesService,
            helperService,
            new MboCategoryOfferChangesServiceHelper(offerChangesService, helperService,
                Mockito.mock(DeepmindSolomonPushService.class), beruId)
        );
    }

    @Test
    public void syncCargotypes() {
        Mockito.when(lmsClientMock.getPartnerCargoTypes(List.of(172L)))
            .thenReturn(List.of(new PartnerCargoTypesDto(172L, 172L,
                Set.of(123, 456, (int) CargoType.HEAVY_GOOD20.lmsId()))));

        controller.syncCargotypes(List.of(172L));

        Assertions.assertThat(deepmindWarehouseRepository.findById(172L).orElseThrow().getCargoTypeLmsIds())
            .containsExactlyInAnyOrder(123L, 456L, CargoType.HEAVY_GOOD20.lmsId());

        Assertions.assertThat(deepmindWarehouseRepository.findById(-172L).orElseThrow().getCargoTypeLmsIds())
            .containsExactlyInAnyOrder(123L, 456L);
    }

    @Test
    public void toggleYtOfferAvailabilityToBackupTest() {
        var unit = Mockito.mock(DbMonitoringUnit.class);
        Mockito.when(dbMonitoring.getOrCreateUnit(Mockito.anyString())).thenReturn(unit);

        // catch clause test
        Mockito.doThrow(new RuntimeException("BASE EXCEPTION")).when(offerAvailabilityYtService)
            .changeOfferAvailabilityBackupYtLink(Mockito.anyString(), Mockito.anyBoolean());

        Mockito.doReturn(OfferAvailabilityYtService.DatacentersState.PRIMARY_BACKUP)
            .when(offerAvailabilityYtService).checkIfSomeLinkPointsBackup();
        Mockito.doThrow(new RuntimeException("PRIMARY_BACKUP")).when(unit).critical(Mockito.anyString());
        Assertions
            .assertThatThrownBy(() -> controller.ytOfferAvailabilityToBackup("table"))
            .hasMessageContaining("PRIMARY_BACKUP");

        Mockito.doReturn(OfferAvailabilityYtService.DatacentersState.SECONDARY_BACKUP)
            .when(offerAvailabilityYtService).checkIfSomeLinkPointsBackup();
        Mockito.doThrow(new RuntimeException("SECONDARY_BACKUP")).when(unit).critical(Mockito.anyString());
        Assertions
            .assertThatThrownBy(() -> controller.ytOfferAvailabilityToBackup("table"))
            .hasMessageContaining("SECONDARY_BACKUP");

        Mockito.doReturn(OfferAvailabilityYtService.DatacentersState.BOTH_BACKUP)
            .when(offerAvailabilityYtService).checkIfSomeLinkPointsBackup();
        Mockito.doThrow(new RuntimeException("BOTH_BACKUP")).when(unit).critical(Mockito.anyString());
        Assertions
            .assertThatThrownBy(() -> controller.ytOfferAvailabilityToBackup("table"))
            .hasMessageContaining("BOTH_BACKUP");

        Mockito.doThrow(new RuntimeException("OK")).when(unit).ok();
        Mockito.doReturn(OfferAvailabilityYtService.DatacentersState.BOTH_OK)
            .when(offerAvailabilityYtService).checkIfSomeLinkPointsBackup();
        Mockito.when(unit.getStatus()).thenReturn(MonitoringStatus.OK);
        Assertions
            .assertThatThrownBy(() -> controller.ytOfferAvailabilityToBackup("table"))
            .hasMessageContaining("BASE EXCEPTION");

        Mockito.when(unit.getStatus()).thenReturn(MonitoringStatus.WARNING);
        Assertions
            .assertThatThrownBy(() -> controller.ytOfferAvailabilityToBackup("table"))
            .hasMessageContaining("OK");

    }

    @Test
    public void syncMskuOffersUpdateTest() {
        deepmindMskuRepository.save(
            TestUtils.newMsku(111, SkuTypeEnum.SKU),
            TestUtils.newMsku(222, SkuTypeEnum.SKU),
            TestUtils.newMsku(444, SkuTypeEnum.SKU)
        );
        deepmindSupplierRepository.save(
            new Supplier().setBusinessId(111).setId(111).setName("supplier1"),
            new Supplier().setBusinessId(222).setId(222).setName("supplier2")
        );
        serviceOfferReplicaRepository.save(
            offer(111, 111, "sku-111", 1, 111),
            offer(222, 222, "sku-222", 2, 222)
        );
        var request = MboCategoryOfferChanges.GetBaseOffersRequest.newBuilder()
            .setBusinessId(111)
            .addShopSkus("sku-111")
            .build();
        // it will be updated
        Mockito.when(offerChangesService.getBaseOffers(Mockito.eq(request)))
            .thenReturn(MboCategoryOfferChanges.GetBaseOffersResponse.newBuilder()
                .addOffers(protoOffer(111, 111, "sku-111", 4, 444))
                .build()
            );
        request = MboCategoryOfferChanges.GetBaseOffersRequest.newBuilder()
            .setBusinessId(222)
            .addShopSkus("sku-222")
            .build();
        // it will be the same
        Mockito.when(offerChangesService.getBaseOffers(Mockito.eq(request)))
            .thenReturn(MboCategoryOfferChanges.GetBaseOffersResponse.newBuilder()
                .addOffers(protoOffer(222, 222, "sku-222", 2, 222))
                .build()
            );
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(2);
        Assertions
            .assertThat(sskuStatusRepository.findAll())
            .isEmpty();
        controller.syncMskuOffers(List.of(
            new BusinessSkuKey(111, "sku-111"),
            new BusinessSkuKey(222, "sku-222")
        ));
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(2)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(111, 111, "sku-111", 4, 444),
                offer(222, 222, "sku-222", 2, 222)
            );
    }

    @Test
    public void syncMskuOffersInsertTest() {
        deepmindMskuRepository.save(
            TestUtils.newMsku(111, SkuTypeEnum.SKU),
            TestUtils.newMsku(222, SkuTypeEnum.SKU),
            TestUtils.newMsku(444, SkuTypeEnum.SKU)
        );
        deepmindSupplierRepository.save(
            new Supplier().setBusinessId(111).setId(111).setName("supplier1"),
            new Supplier().setBusinessId(222).setId(222).setName("supplier2"),
            new Supplier().setBusinessId(333).setId(333).setName("supplier3")
        );
        serviceOfferReplicaRepository.save(
            offer(111, 111, "sku-111", 1, 111),
            offer(222, 222, "sku-222", 2, 222)
        );
        var request = MboCategoryOfferChanges.GetBaseOffersRequest.newBuilder()
            .setBusinessId(333)
            .addShopSkus("sku-333")
            .build();
        // it will be inserted
        Mockito.when(offerChangesService.getBaseOffers(Mockito.eq(request)))
            .thenReturn(MboCategoryOfferChanges.GetBaseOffersResponse.newBuilder()
                .addOffers(protoOffer(333, 333, "sku-333", 3, 333))
                .build()
            );
        request = MboCategoryOfferChanges.GetBaseOffersRequest.newBuilder()
            .setBusinessId(222)
            .addShopSkus("sku-222")
            .build();
        // it will be the same
        Mockito.when(offerChangesService.getBaseOffers(Mockito.eq(request)))
            .thenReturn(MboCategoryOfferChanges.GetBaseOffersResponse.newBuilder()
                .addOffers(protoOffer(222, 222, "sku-222", 2, 222))
                .build()
            );
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(2);
        Assertions
            .assertThat(sskuStatusRepository.findAll())
            .isEmpty();
        controller.syncMskuOffers(List.of(
            new BusinessSkuKey(222, "sku-222"),
            new BusinessSkuKey(333, "sku-333")
        ));
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(111, 111, "sku-111", 1, 111),
                offer(222, 222, "sku-222", 2, 222),
                offer(333, 333, "sku-333", 3, 333)
            );
        Assertions
            .assertThat(sskuStatusRepository.findAll())
            .hasSize(1)
            .extracting(s -> new ServiceOfferKey(s.getSupplierId(), s.getShopSku()))
            .containsExactly(new ServiceOfferKey(333, "sku-333"));
    }

    @Test
    public void syncMskuOffersDeleteTest() {
        deepmindMskuRepository.save(
            TestUtils.newMsku(111, SkuTypeEnum.SKU),
            TestUtils.newMsku(222, SkuTypeEnum.SKU),
            TestUtils.newMsku(444, SkuTypeEnum.SKU)
        );
        deepmindSupplierRepository.save(
            new Supplier().setBusinessId(111).setId(111).setName("supplier1"),
            new Supplier().setBusinessId(222).setId(222).setName("supplier2"),
            new Supplier().setBusinessId(333).setId(333).setName("supplier3")
        );
        serviceOfferReplicaRepository.save(
            offer(111, 111, "sku-111", 1, 111),
            offer(222, 222, "sku-222", 2, 222)
        );
        var request = MboCategoryOfferChanges.GetBaseOffersRequest.newBuilder()
            .setBusinessId(111)
            .addShopSkus("sku-111")
            .build();
        // it will be deleted
        Mockito.when(offerChangesService.getBaseOffers(Mockito.eq(request)))
            .thenReturn(MboCategoryOfferChanges.GetBaseOffersResponse.newBuilder()
                .addOffers(protoOffer(111, 111, "sku-111", 1, 0))
                .build()
            );
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(2);
        controller.syncMskuOffers(List.of(
            new BusinessSkuKey(111, "sku-111")
        ));
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(1)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(222, 222, "sku-222", 2, 222)
            );
    }

    @Test
    public void syncMskuOffersByBusinessId3PTest() {
        deepmindMskuRepository.save(
            TestUtils.newMsku(111, SkuTypeEnum.SKU),
            TestUtils.newMsku(222, SkuTypeEnum.SKU),
            TestUtils.newMsku(333, SkuTypeEnum.SKU)
        );
        deepmindSupplierRepository.save(
            new Supplier().setBusinessId(111).setId(111).setName("supplier1"),
            new Supplier().setBusinessId(111).setId(222).setName("supplier2"),
            new Supplier().setBusinessId(111).setId(333).setName("supplier3")
        );
        serviceOfferReplicaRepository.save(
            offer(111, 111, "sku-111", 1, 111),
            offer(111, 222, "sku-222", 2, 222)
        );
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
            .setLimit(5000);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.setBusinessId(111).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addAllOffers(List.of(
                    protoOffer(111, 111, "sku-111", 4, 222),
                    protoOffer(111, 333, "sku-333", 3, 333)
                ))
                .setLastShopSku("sku-333").build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(111).setLastShopSku("sku-333").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(2)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(111, 111, "sku-111", 1, 111),
                offer(111, 222, "sku-222", 2, 222)
            );
        controller.syncMskuOffersByBusinessId(List.of(111));
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(2)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(111, 111, "sku-111", 4, 222),
                offer(111, 333, "sku-333", 3, 333)
            );
    }

    @Test
    public void syncMskuOffersByBusinessId1PTest() {
        deepmindMskuRepository.save(
            TestUtils.newMsku(111, SkuTypeEnum.SKU),
            TestUtils.newMsku(222, SkuTypeEnum.SKU),
            TestUtils.newMsku(333, SkuTypeEnum.SKU)
        );
        deepmindSupplierRepository.save(
            new Supplier().setId(beruId.getBusinessId()).setName("beru").setSupplierType(SupplierType.BUSINESS),
            new Supplier().setId(111).setBusinessId(111).setName("supplier1"),
            new Supplier().setId(222).setBusinessId(beruId.getBusinessId()).setName("supplier2")
                .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("000222"),
            new Supplier().setBusinessId(beruId.getBusinessId()).setId(333).setName("supplier3")
                .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("000333")
        );
        serviceOfferReplicaRepository.save(
            offer(111, 111, "sku-111", 1, 111),
            offer(beruId.getBusinessId(), 222, "sku-222", 2, 222).setSupplierType(SupplierType.REAL_SUPPLIER)
        );
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)
            .setLimit(5000);
        Mockito.when(offerChangesService
                .findOffersByBusinessId(Mockito.eq(request.setBusinessId(beruId.getId()).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addOffers(protoOffer(beruId.getBusinessId(), beruId.getId(), "000333.sku-333", 3, 333,
                    SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER))
                .setLastShopSku("000333.sku-333").build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(beruId.getId()).setLastShopSku("000333.sku-333").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(2)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(111, 111, "sku-111", 1, 111),
                offer(beruId.getBusinessId(), 222, "sku-222", 2, 222)
                    .setSupplierType(SupplierType.REAL_SUPPLIER)
            );
        controller.syncMskuOffersByBusinessId(List.of(beruId.getId()));
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(2)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(111, 111, "sku-111", 1, 111),
                offer(beruId.getBusinessId(), 333, "sku-333", 3, 333)
                    .setSupplierType(SupplierType.REAL_SUPPLIER)
            );
    }

    @Test
    public void syncingByBeruBusinessIdDoesNotLeadsToDeletingOf1POffers() {
        //arrange
        deepmindMskuRepository.save(TestUtils.newMsku(111, SkuTypeEnum.SKU));
        deepmindSupplierRepository.save(
            new Supplier().setId(111).setBusinessId(beruId.getBusinessId()).setName("real-supplier")
                .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("123")
        );
        var offer1P = offer(beruId.getBusinessId(), 111, "123.sku-111", 1, 111)
            .setSupplierType(SupplierType.REAL_SUPPLIER);
        serviceOfferReplicaRepository.save(offer1P);

        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)
            .setLimit(5000);

        request.setBusinessId(beruId.getId());
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addOffers(protoOffer(beruId.getBusinessId(), 111, "123.sku-111", 1, 111,
                    SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER))
                .setLastShopSku("123.sku-111").build());

        request.setBusinessId(beruId.getId()).setLastShopSku("123.sku-111");
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());

        //act
        controller.syncMskuOffersByBusinessId(List.of(beruId.getBusinessId()));

        //assert that no 1P offers are being deleted
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(1)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(beruId.getBusinessId(), 111, "123.sku-111", 1, 111)
            );
    }

    private MboCategoryOfferChanges.SimpleBaseOffer protoOffer(
        int businessId, int supplierId, String shopSku, long seqId, long mskuId) {
        return protoOfferBuilder(businessId, supplierId, shopSku, seqId, mskuId,
            SupplierOffer.SupplierType.TYPE_THIRD_PARTY).build();
    }

    private MboCategoryOfferChanges.SimpleBaseOffer protoOffer(
        int businessId, int supplierId, String shopSku, long seqId, long mskuId, SupplierOffer.SupplierType type) {
        return protoOfferBuilder(businessId, supplierId, shopSku, seqId, mskuId, type).build();
    }

    private MboCategoryOfferChanges.SimpleBaseOffer.Builder protoOfferBuilder(
        int businessId, int supplierId, String shopSku, long seqId, long mskuId, SupplierOffer.SupplierType type) {
        return MboCategoryOfferChanges.SimpleBaseOffer.newBuilder()
            .setBusinessId(businessId)
            .setShopSku(shopSku)
            .setTitle("title")
            .setCategoryId(1L)
            .setModifiedSeqId(seqId)
            .setApprovedMappingMskuId(mskuId)
            .setIsDeleted(false)
            .setModifiedTs(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli())
            .addServiceOffers(
                MboCategoryOfferChanges.SimpleServiceOffer.newBuilder()
                    .setSupplierId(supplierId)
                    .setSupplierType(type)
                    .setAcceptanceStatus(MboCategoryOfferChanges.SimpleServiceOffer.AcceptanceStatus.OK)
                    .build());
    }

    private ServiceOfferReplica offer(Integer businessId, int supplierId, String shopSku, long seqId, long mskuId) {
        return new ServiceOfferReplica()
            .setBusinessId(businessId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("title")
            .setCategoryId(1L)
            .setSeqId(seqId)
            .setMskuId(mskuId)
            .setModifiedTs(Instant.now().minus(1, ChronoUnit.DAYS))
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

}
