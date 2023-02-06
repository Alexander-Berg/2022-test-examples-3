package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.mocks.SskuStatusAuditServiceMock;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.pojo.SskuStatusReason;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusDeletedRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepositoryImpl;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.ImportMskuOfferHelperService;
import ru.yandex.market.deepmind.common.services.MboCategoryOfferChangesServiceHelper;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverter;
import ru.yandex.market.deepmind.common.solomon.DeepmindSolomonPushService;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.http.MboCategoryOfferChanges;
import ru.yandex.market.mboc.http.MboCategoryOfferChangesService;
import ru.yandex.market.mboc.http.SupplierOffer;

import static ru.yandex.market.mboc.http.MboCategoryOfferChanges.LogisticSchema.FBY;
import static ru.yandex.market.mboc.http.MboCategoryOfferChanges.LogisticSchema.FBY_PLUS;

public class ImportMskuOfferBySuppliersExecutorTest extends DeepmindBaseDbTestClass {
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private OffersConverter offersConverter;
    @Resource(name = "deepmindTransactionHelper")
    private TransactionHelper transactionHelper;
    @Resource
    private BeruId beruId;
    @Resource
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Resource
    private SskuStatusDeletedRepository sskuStatusDeletedRepository;

    private StorageKeyValueService storageKeyValueService = new StorageKeyValueServiceMock();
    private MboCategoryOfferChangesService offerChangesService;
    private ImportMskuOfferBySuppliersExecutor executor;
    private SskuStatusRepository sskuStatusRepository;

    @Before
    public void setUp() {
        sskuStatusRepository = new SskuStatusRepositoryImpl(jdbcTemplate, transactionTemplate,
            sskuStatusDeletedRepository, new SskuStatusAuditServiceMock());
        offersConverter.clearCache();
        offerChangesService = Mockito.mock(MboCategoryOfferChangesService.class);
        var pushService = Mockito.mock(DeepmindSolomonPushService.class);

        var importMskuOfferHelperService = new ImportMskuOfferHelperService(serviceOfferReplicaRepository,
            sskuStatusRepository, offersConverter, pushService, beruId, transactionHelper);
        executor = new ImportMskuOfferBySuppliersExecutor(storageKeyValueService,
            deepmindSupplierRepository, transactionHelper, new MboCategoryOfferChangesServiceHelper(offerChangesService,
                importMskuOfferHelperService, pushService, beruId),
            beruId
        );
        deepmindMskuRepository.save(
            TestUtils.newMsku(111),
            TestUtils.newMsku(222),
            TestUtils.newMsku(333)
        );
        storageKeyValueService
            .putValue("import_msku_offer_by_suppliers_last_run_key", Instant.now().minus(1, ChronoUnit.DAYS));
    }

    private void prepareMockFor3P(int batchSize) {
        serviceOfferReplicaRepository.save(
            offer(111, 11, "sku-111", 1, 111),
            offer(222, 22, "sku-222", 2, 222),
            offer(333, 33, "sku-333", 3, 333)
        );
        deepmindSupplierRepository.save(
            new Supplier().setBusinessId(111).setId(11).setName("supplier1").setFulfillment(true),
            new Supplier().setBusinessId(222).setId(22).setName("supplier2").setFulfillment(true),
            new Supplier().setBusinessId(333).setId(33).setName("supplier3").setCrossdock(true)
        );
        storageKeyValueService.putValue("import_msku_offer_by_suppliers_batch_size", batchSize);
        executor.setSupplierBatchSize(100);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.setBusinessId(222).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addOffers(offer3p(222, List.of(22), "sku-222", 2, 222))
                .setLastShopSku("sku-222").build());
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.setBusinessId(333).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addOffers(offer3p(333, List.of(33), "sku-333", 3, 333))
                .setLastShopSku("sku-333").build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
                    .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
                    .addSupplierTypes(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)
                    .setBusinessId(beruId.getId())
                    .setLimit(batchSize).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(222).setLastShopSku("sku-222").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(333).setLastShopSku("sku-333").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
    }

    @Test
    public void simple3PTest() {
        var batchSize = 1;
        prepareMockFor3P(batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
            .setLimit(batchSize);
        // mock to insert "sku-444"
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.setBusinessId(111).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().addAllOffers(List.of(
                    offer3p(111, List.of(11), "sku-111", 1, 111),
                    offer3p(111, List.of(11), "sku-444", 4, 111)))
                .setLastShopSku("sku-444")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(111).setLastShopSku("sku-444").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3);

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(4);
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void shopSkuWithDot3PTest() {
        var batchSize = 1;
        prepareMockFor3P(batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
            .setLimit(batchSize);
        // mock to insert "sku-444.444"
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.setBusinessId(111).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().addAllOffers(List.of(
                    offer3p(111, List.of(11), "sku-111", 1, 111),
                    offer3p(111, List.of(11), "sku-444.444", 4, 111)))
                .setLastShopSku("sku-444.444")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(111).setLastShopSku("sku-444.444").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3);

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(4)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .contains(offer(111, 11, "sku-444.444", 4, 111));
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void simple1PTest() {
        deepmindSupplierRepository.save(new Supplier().setId(444).setName("supplier4")
            .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("000444"));
        var batchSize = 1;
        storageKeyValueService.putValue("import_msku_offer_by_suppliers_batch_size", batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)
            .setBusinessId(beruId.getId())
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addOffers(offer1p(beruId.getId(), beruId.getId(), "000444.sku-444", 4, 444))
                .setLastShopSku("000444.sku-444")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(beruId.getId()).setLastShopSku("000444.sku-444").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .isEmpty();
        Assertions
            .assertThat(sskuStatusRepository.findAll())
            .isEmpty();

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(1)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .contains(offer(beruId.getBusinessId(), 444, "sku-444", 4, 444, SupplierType.REAL_SUPPLIER));
        Assertions
            .assertThat(sskuStatusRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "availability", "comment")
            .containsExactly(
                new SskuStatus()
                    .setSupplierId(444)
                    .setShopSku("sku-444")
                    .setAvailability(OfferAvailability.INACTIVE_TMP)
                    .setReason(SskuStatusReason.WAITING_FOR_ENTER)
                    .setComment(SskuStatusReason.WAITING_FOR_ENTER.getLiteral())
            );
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void turnOffTest() {
        storageKeyValueService.putValue("import_msku_offer_by_suppliers_last_run_key", Instant.now());
        var batchSize = 1;
        prepareMockFor3P(batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.setBusinessId(111).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().addAllOffers(List.of(
                    offer3p(111, List.of(11), "sku-111", 1, 111),
                    offer3p(111, List.of(11), "sku-444", 4, 111)))
                .setLastShopSku("sku-444")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(111).setLastShopSku("sku-444").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3);

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3);
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void insertTest() {
        deepmindSupplierRepository.save(
            new Supplier().setBusinessId(444).setId(44).setName("supplier4").setFulfillment(true));
        var batchSize = 1;
        prepareMockFor3P(batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.setBusinessId(444).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().addAllOffers(List.of(
                    offer3p(444, List.of(44), "sku-444", 4, 444)))
                .setLastShopSku("sku-444")
                .build());
        // mock 111 to stay the same
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.setBusinessId(111).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addOffers(offer3p(111, List.of(11), "sku-111", 1, 111))
                .setLastShopSku("sku-111").build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(111).setLastShopSku("sku-111").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(444).setLastShopSku("sku-444").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3);
        Assertions
            .assertThat(sskuStatusRepository.findAll())
            .isEmpty();

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(4)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .contains(offer(444, 44, "sku-444", 4, 444));
        Assertions
            .assertThat(sskuStatusRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "availability")
            .containsExactly(
                new SskuStatus()
                    .setSupplierId(44)
                    .setShopSku("sku-444")
                    .setAvailability(OfferAvailability.ACTIVE)
            );
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void updateTest() {
        var batchSize = 1;
        prepareMockFor3P(batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.setBusinessId(111).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().addAllOffers(List.of(
                    offer3p(111, List.of(11), "sku-111", 5, 444)))
                .setLastShopSku("sku-111")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(111).setLastShopSku("sku-111").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3);
        Assertions
            .assertThat(sskuStatusRepository.findAll())
            .isEmpty();
        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .contains(offer(111, 11, "sku-111", 5, 444));
        Assertions
            .assertThat(sskuStatusRepository.findAll())
            .isEmpty();
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void updateWithZeroSeqIdTest() {
        var batchSize = 1;
        prepareMockFor3P(batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.setBusinessId(111).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().addAllOffers(List.of(
                    offer3p(111, List.of(11), "sku-111", 0, 444)))
                .setLastShopSku("sku-111")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(111).setLastShopSku("sku-111").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3);

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .contains(offer(111, 11, "sku-111", 0, 444));
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void notUpdateIfEqualsTest() {
        var batchSize = 1;
        prepareMockFor3P(batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.setBusinessId(111).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().addAllOffers(List.of(
                    offer3p(111, List.of(11), "sku-111", 5, 111)))
                .setLastShopSku("sku-111")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(111).setLastShopSku("sku-111").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3);

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .contains(offer(111, 11, "sku-111", 1, 111));
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void updateFromLastStateTest() {
        storageKeyValueService.putValue("import_msku_offer_by_suppliers_last_state",
            new ImportMskuOfferBySuppliersExecutor.LastState(SupplierType.THIRD_PARTY, 111, "sku-112"));
        var batchSize = 1;
        prepareMockFor3P(batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
            .setBusinessId(111)
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().addOffers(
                    offer3p(111, List.of(11), "sku-111", 5, 444))
                .setLastShopSku("sku-111")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setLastShopSku("sku-112").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3);

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "mskuId")
            // not updated because we start from sku-112 and skipped sku-111
            .contains(offer(111, 11, "sku-111", 1, 111));
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void updateFromLastStateByBusinessIdTest() {
        storageKeyValueService.putValue("import_msku_offer_by_suppliers_last_state",
            new ImportMskuOfferBySuppliersExecutor.LastState(SupplierType.THIRD_PARTY, 222, "sku-111"));
        var batchSize = 1;
        prepareMockFor3P(batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
            .setBusinessId(111)
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addOffers(offer3p(111, List.of(11), "sku-111", 5, 444))
                .setLastShopSku("sku-111")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
            Mockito.eq(request.setBusinessId(222).setLastShopSku("sku-111").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addOffers(offer3p(222, List.of(22), "sku-222", 2, 222))
                .setLastShopSku("sku-222").build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3);

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "mskuId")
            // not updated because we start from bizId = 112 and bizId = skipped 111
            .contains(offer(111, 11, "sku-111", 1, 111));
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void updateFromLastState1PTest() {
        deepmindSupplierRepository.save(new Supplier().setId(444).setName("supplier4")
            .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("000444"));
        serviceOfferReplicaRepository.save(offer(beruId.getId(), 444, "sku-444", 5, 111, SupplierType.REAL_SUPPLIER));
        storageKeyValueService.putValue("import_msku_offer_by_suppliers_last_state",
            new ImportMskuOfferBySuppliersExecutor.LastState(
                SupplierType.REAL_SUPPLIER, beruId.getId(), "000444.sku-444"));
        var batchSize = 1;
        storageKeyValueService.putValue("import_msku_offer_by_suppliers_batch_size", batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)
            .setBusinessId(beruId.getId())
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addOffers(offer1p(beruId.getId(), beruId.getId(), "000444.sku-444", 5, 444))
                .setLastShopSku("000444.sku-444")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
            Mockito.eq(request.setLastShopSku("000444.sku-444").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(1);

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(1)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "mskuId")
            // not updated because we start from sku-444 excluding sku-444
            .contains(offer(beruId.getId(), 444, "sku-444", 5, 111));
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void delete3PTest() {
        deepmindSupplierRepository.save(new Supplier().setBusinessId(111).setId(10).setName("supplier10"));
        serviceOfferReplicaRepository.save(
            offer(111, 10, "sku-111", 0, 111),
            offer(111, 10, "sku-222", 0, 111),
            offer(111, 10, "sku-333", 0, 111),
            offer(111, 10, "sku-444", 0, 111),
            offer(111, 11, "sku-222", 0, 111),
            offer(111, 11, "sku-333", 0, 111),
            offer(111, 11, "sku-444", 0, 111)
        );
        var batchSize = 3;
        prepareMockFor3P(batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
            .setBusinessId(111)
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addAllOffers(List.of(
                    offer3p(111, List.of(10, 11), "sku-111", 4, 111),
                    offer3p(111, List.of(11), "sku-222", 5, 111),
                    offer3p(111, List.of(10, 11), "sku-333", 6, 777)
                ))
                .setLastShopSku("sku-333")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
            Mockito.eq(request.setLastShopSku("sku-333").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addAllOffers(List.of(
                    offer3p(111, List.of(10), "sku-555", 7, 111)
                ))
                .setLastShopSku("sku-555")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setLastShopSku("sku-555").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(111, 11, "sku-111", 1, 111),
                offer(222, 22, "sku-222", 2, 222),
                offer(333, 33, "sku-333", 3, 333),
                offer(111, 10, "sku-111", 0, 111),
                offer(111, 10, "sku-222", 0, 111),
                offer(111, 10, "sku-333", 0, 111),
                offer(111, 10, "sku-444", 0, 111),
                offer(111, 11, "sku-222", 0, 111),
                offer(111, 11, "sku-333", 0, 111),
                offer(111, 11, "sku-444", 0, 111)
            );

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(111, 11, "sku-111", 1, 111),
                offer(222, 22, "sku-222", 2, 222),
                offer(333, 33, "sku-333", 3, 333),
                offer(111, 10, "sku-111", 0, 111),
                offer(111, 10, "sku-333", 6, 777),
                offer(111, 10, "sku-555", 7, 111),
                offer(111, 11, "sku-222", 0, 111),
                offer(111, 11, "sku-333", 6, 777)
            );
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void deleteBefore3PTest() {
        deepmindSupplierRepository.save(new Supplier().setBusinessId(111).setId(110).setName("supplier110"));
        serviceOfferReplicaRepository.save(
            offer(111, 10, "sku-112", 5, 111),
            offer(111, 11, "sku-112", 4, 111)
        );
        var batchSize = 1;
        prepareMockFor3P(batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.setBusinessId(111).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().addAllOffers(List.of(
                    offer3p(111, List.of(11), "sku-112", 4, 111)))
                .setLastShopSku("sku-112")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(111).setLastShopSku("sku-112").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(111, 11, "sku-111", 1, 111),
                offer(222, 22, "sku-222", 2, 222),
                offer(333, 33, "sku-333", 3, 333),
                offer(111, 10, "sku-112", 5, 111),
                offer(111, 11, "sku-112", 4, 111)
            );

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(222, 22, "sku-222", 2, 222),
                offer(333, 33, "sku-333", 3, 333),
                offer(111, 11, "sku-112", 4, 111)
            );
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void delete3PTestHavingLastShopSkuWithDot() {
        deepmindSupplierRepository.save(new Supplier().setBusinessId(111).setId(110).setName("supplier110"));
        serviceOfferReplicaRepository.save(
            offer(111, 10, "sku-111", 5, 111),
            offer(111, 11, "sku-112", 4, 111)
        );
        var batchSize = 1;
        prepareMockFor3P(batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.setBusinessId(111).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().addAllOffers(List.of(
                    offer3p(111, List.of(11), "sku-111", 1, 111),
                    offer3p(111, List.of(11), "sku-111.111", 6, 111)))
                .setLastShopSku("sku-111.111")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(111).setLastShopSku("sku-111.111").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(111, 11, "sku-111", 1, 111),
                offer(222, 22, "sku-222", 2, 222),
                offer(333, 33, "sku-333", 3, 333),
                offer(111, 10, "sku-111", 5, 111),
                offer(111, 11, "sku-112", 4, 111)
            );

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(111, 11, "sku-111", 1, 111),
                offer(222, 22, "sku-222", 2, 222),
                offer(333, 33, "sku-333", 3, 333),
                offer(111, 11, "sku-111.111", 6, 111)
            );
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void deleteAfter3PTest() {
        deepmindSupplierRepository.save(new Supplier().setBusinessId(111).setId(112).setName("supplier112"));
        serviceOfferReplicaRepository.save(
            offer(111, 11, "sku-112", 5, 111),
            offer(111, 12, "sku-111", 4, 111)
        );
        var batchSize = 1;
        prepareMockFor3P(batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_THIRD_PARTY)
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.setBusinessId(111).build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().addAllOffers(List.of(
                    offer3p(111, List.of(11), "sku-111", 4, 111)))
                .setLastShopSku("sku-111")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(111).setLastShopSku("sku-111").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(111, 11, "sku-111", 1, 111),
                offer(222, 22, "sku-222", 2, 222),
                offer(333, 33, "sku-333", 3, 333),
                offer(111, 11, "sku-112", 5, 111),
                offer(111, 12, "sku-111", 4, 111)
            );

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(111, 11, "sku-111", 1, 111),
                offer(222, 22, "sku-222", 2, 222),
                offer(333, 33, "sku-333", 3, 333)
            );
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void delete1PTest() {
        deepmindSupplierRepository.save(
            new Supplier().setId(444).setName("supplier4")
                .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("000444"),
            new Supplier().setId(555).setName("supplier5")
                .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("000555")
        );
        serviceOfferReplicaRepository.save(
            offer(beruId.getBusinessId(), 444, "sku-111", 0, 444, SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 444, "sku-222", 0, 444, SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 444, "sku-333", 0, 444, SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 444, "sku-444", 0, 444, SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 444, "sku-666", 0, 444, SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 555, "sku-111", 0, 555, SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 555, "sku-222", 0, 555, SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 555, "sku-333", 0, 555, SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 555, "sku-444", 0, 555, SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 555, "sku-555", 0, 555, SupplierType.REAL_SUPPLIER)
        );
        var batchSize = 3;
        storageKeyValueService.putValue("import_msku_offer_by_suppliers_batch_size", batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)
            .setBusinessId(beruId.getId())
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addAllOffers(List.of(
                    offer1p(beruId.getId(), beruId.getId(), "000444.sku-111", 5, 444),
                    offer1p(beruId.getId(), beruId.getId(), "000444.sku-333", 6, 444),
                    offer1p(beruId.getId(), beruId.getId(), "000444.sku-444", 7, 444)
                ))
                .setLastShopSku("000444.sku-444")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
            Mockito.eq(request.setLastShopSku("000444.sku-444").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addAllOffers(List.of(
                    offer1p(beruId.getId(), beruId.getId(), "000444.sku-555", 8, 444),
                    offer1p(beruId.getId(), beruId.getId(), "000555.sku-111", 11, 555),
                    offer1p(beruId.getId(), beruId.getId(), "000555.sku-222", 12, 555)
                ))
                .setLastShopSku("000555.sku-222")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
            Mockito.eq(request.setLastShopSku("000555.sku-222").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addAllOffers(List.of(
                    offer1p(beruId.getId(), beruId.getId(), "000555.sku-666", 14, 555),
                    offer1p(beruId.getId(), beruId.getId(), "000555.sku-777", 15, 555),
                    offer1p(beruId.getId(), beruId.getId(), "000555.sku-888", 16, 555)
                ))
                .setLastShopSku("000555.sku-888")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
            Mockito.eq(request.setLastShopSku("000555.sku-888").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(beruId.getBusinessId(), 444, "sku-111", 0, 444, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 444, "sku-222", 0, 444, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 444, "sku-333", 0, 444, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 444, "sku-444", 0, 444, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 444, "sku-666", 0, 444, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 555, "sku-111", 0, 555, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 555, "sku-222", 0, 555, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 555, "sku-333", 0, 555, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 555, "sku-444", 0, 555, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 555, "sku-555", 0, 555, SupplierType.REAL_SUPPLIER)
            );

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(beruId.getBusinessId(), 444, "sku-111", 0, 444, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 444, "sku-333", 0, 444, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 444, "sku-444", 0, 444, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 444, "sku-555", 8, 444, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 555, "sku-111", 0, 555, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 555, "sku-222", 0, 555, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 555, "sku-666", 14, 555, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 555, "sku-777", 15, 555, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 555, "sku-888", 16, 555, SupplierType.REAL_SUPPLIER)
            );
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void deleteBefore1PTest() {
        deepmindSupplierRepository.save(
            new Supplier().setId(444).setName("supplier4")
                .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("000444"),
            new Supplier().setId(554).setName("supplier554")
                .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("000554"),
            new Supplier().setId(555).setName("supplier5")
                .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("000555")
        );
        serviceOfferReplicaRepository.save(
            offer(beruId.getBusinessId(), 444, "sku-444", 4, 444).setSupplierType(SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 554, "sku-555", 5, 555).setSupplierType(SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 555, "sku-555", 6, 555).setSupplierType(SupplierType.REAL_SUPPLIER)
        );
        var batchSize = 3;
        storageKeyValueService.putValue("import_msku_offer_by_suppliers_batch_size", batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)
            .setBusinessId(beruId.getId())
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addOffers(offer1p(beruId.getId(), beruId.getId(), "000555.sku-555", 5, 555))
                .setLastShopSku("000555.sku-555")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(beruId.getId()).setLastShopSku("000555.sku-555").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactly(
                offer(beruId.getBusinessId(), 444, "sku-444", 4, 444).setSupplierType(SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 554, "sku-555", 5, 555).setSupplierType(SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 555, "sku-555", 6, 555).setSupplierType(SupplierType.REAL_SUPPLIER)
            );

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(beruId.getBusinessId(), 555, "sku-555", 6, 555).setSupplierType(SupplierType.REAL_SUPPLIER)
            );
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    @Test
    public void deleteAfter1PTest() {
        deepmindSupplierRepository.save(
            new Supplier().setId(444).setName("supplier4")
                .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("000444"),
            new Supplier().setId(554).setName("supplier554")
                .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("000554"),
            new Supplier().setId(555).setName("supplier5")
                .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("000555")
        );
        serviceOfferReplicaRepository.save(
            offer(beruId.getBusinessId(), 444, "sku-444", 4, 444).setSupplierType(SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 444, "sku-445", 5, 555).setSupplierType(SupplierType.REAL_SUPPLIER),
            offer(beruId.getBusinessId(), 555, "sku-555", 6, 555).setSupplierType(SupplierType.REAL_SUPPLIER)
        );
        var batchSize = 1;
        storageKeyValueService.putValue("import_msku_offer_by_suppliers_batch_size", batchSize);
        var request = MboCategoryOfferChanges.FindOffersByBusinessIdRequest.newBuilder()
            .addAllLogisticSchemas(List.of(FBY, FBY_PLUS))
            .addSupplierTypes(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)
            .setBusinessId(beruId.getId())
            .setLimit(batchSize);
        Mockito.when(offerChangesService.findOffersByBusinessId(Mockito.eq(request.build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder()
                .addOffers(offer1p(beruId.getId(), beruId.getId(), "000444.sku-444", 5, 555))
                .setLastShopSku("000444.sku-444")
                .build());
        Mockito.when(offerChangesService.findOffersByBusinessId(
                Mockito.eq(request.setBusinessId(beruId.getId()).setLastShopSku("000444.sku-444").build())))
            .thenReturn(MboCategoryOfferChanges.FindOffersByBusinessIdResponse.newBuilder().build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactly(
                offer(beruId.getBusinessId(), 444, "sku-444", 4, 444, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 444, "sku-445", 5, 555, SupplierType.REAL_SUPPLIER),
                offer(beruId.getBusinessId(), 555, "sku-555", 6, 555, SupplierType.REAL_SUPPLIER)
            );

        executor.execute();

        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "seqId", "mskuId")
            .containsExactlyInAnyOrder(
                offer(beruId.getBusinessId(), 444, "sku-444", 5, 555, SupplierType.REAL_SUPPLIER)
            );
        Assertions
            .assertThat(storageKeyValueService.getInstant("import_msku_offer_by_suppliers_last_run_key", null))
            .isAfter(Instant.now().minus(1, ChronoUnit.DAYS));
    }

    private MboCategoryOfferChanges.SimpleBaseOffer offer3p(
        int businessId, List<Integer> supplierIds, String shopSku, long seqId, long mskuId) {
        return protoOfferBuilder(businessId, supplierIds, shopSku, seqId, mskuId,
            SupplierOffer.SupplierType.TYPE_THIRD_PARTY).build();
    }

    private MboCategoryOfferChanges.SimpleBaseOffer offer1p(
        int businessId, int supplierId, String shopSku, long seqId, long mskuId) {
        return protoOfferBuilder(businessId, List.of(supplierId), shopSku, seqId, mskuId,
            SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER).build();
    }

    private MboCategoryOfferChanges.SimpleBaseOffer.Builder protoOfferBuilder(
        int businessId, List<Integer> supplierIds, String shopSku, long seqId, long mskuId,
        SupplierOffer.SupplierType type) {
        return MboCategoryOfferChanges.SimpleBaseOffer.newBuilder()
            .setBusinessId(businessId)
            .setShopSku(shopSku)
            .setTitle("title")
            .setCategoryId(1L)
            .setModifiedSeqId(seqId)
            .setApprovedMappingMskuId(mskuId)
            .setIsDeleted(false)
            .setModifiedTs(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli())
            .addAllServiceOffers(
                supplierIds.stream().map(supplierId ->
                    MboCategoryOfferChanges.SimpleServiceOffer.newBuilder()
                        .setSupplierId(supplierId)
                        .setSupplierType(type)
                        .setAcceptanceStatus(MboCategoryOfferChanges.SimpleServiceOffer.AcceptanceStatus.OK)
                        .build()
                ).collect(Collectors.toSet())
            );
    }

    private ServiceOfferReplica offer(Integer businessId, int supplierId, String shopSku, long seqId, long mskuId) {
        return offer(businessId, supplierId, shopSku, seqId, mskuId, SupplierType.THIRD_PARTY);
    }

    private ServiceOfferReplica offer(Integer businessId, int supplierId, String shopSku, long seqId, long mskuId,
                        SupplierType type) {
        return new ServiceOfferReplica()
            .setBusinessId(businessId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("title")
            .setCategoryId(1L)
            .setSeqId(seqId)
            .setMskuId(mskuId)
            .setSupplierType(type)
            .setAcceptanceStatus(OfferAcceptanceStatus.OK)
            .setModifiedTs(Instant.now().minus(1, ChronoUnit.DAYS));
    }
}
