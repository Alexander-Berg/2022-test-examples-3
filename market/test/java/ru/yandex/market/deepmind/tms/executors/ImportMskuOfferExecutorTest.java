package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverter;
import ru.yandex.market.deepmind.common.solomon.DeepmindSolomonPushService;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.http.MboCategoryOfferChanges;
import ru.yandex.market.mboc.http.MboCategoryOfferChangesService;
import ru.yandex.market.mboc.http.SupplierOffer;

public class ImportMskuOfferExecutorTest extends DeepmindBaseDbTestClass {

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
    private ImportMskuOfferExecutor executor;
    private SskuStatusRepository sskuStatusRepository;

    @Before
    public void setUp() {
        sskuStatusRepository = new SskuStatusRepositoryImpl(
            jdbcTemplate,
            transactionTemplate,
            sskuStatusDeletedRepository,
            new SskuStatusAuditServiceMock()
        );

        offersConverter.clearCache();
        offerChangesService = Mockito.mock(MboCategoryOfferChangesService.class);
        var pushService = Mockito.mock(DeepmindSolomonPushService.class);

        executor = new ImportMskuOfferExecutor(offerChangesService, storageKeyValueService, transactionHelper,
            pushService,
            new ImportMskuOfferHelperService(serviceOfferReplicaRepository, sskuStatusRepository, offersConverter,
                pushService, beruId, transactionHelper));
        deepmindMskuRepository.save(
            TestUtils.newMsku(111),
            TestUtils.newMsku(222),
            TestUtils.newMsku(333)
        );
        deepmindSupplierRepository.save(
            new Supplier().setBusinessId(111).setId(111).setName("supplier1"),
            new Supplier().setBusinessId(222).setId(222).setName("supplier2"),
            new Supplier().setBusinessId(333).setId(333).setName("supplier3")
        );
        var modifiedTs = Instant.now().minus(2, ChronoUnit.DAYS);
        serviceOfferReplicaRepository.save(
            offer(111, 111, "sku-111", 1, 111).setModifiedTs(modifiedTs),
            offer(222, 222, "sku-222", 2, 222).setModifiedTs(modifiedTs),
            offer(333, 333, "sku-333", 3, 333).setModifiedTs(modifiedTs)
        );
        storageKeyValueService.putValue("import_msku_offer_executor_run_key", true);
        Mockito.when(offerChangesService.getMaxAndMinModifiedSeqId(Mockito.any()))
            .thenReturn(MboCategoryOfferChanges.GetMaxMinModifiedSeqIdResponse.newBuilder()
                .setMinModifiedSeqId(1)
                .setMaxModifiedSeqId(100)
                .build());
    }

    @Test
    public void simpleTest() {
        Mockito.when(offerChangesService.findChanges(Mockito.any(MboCategoryOfferChanges.FindChangesRequest.class)))
            .thenReturn(MboCategoryOfferChanges.FindChangesResponse.newBuilder().addAllOffers(List.of(
                    protoOffer(111, 111, "sku-111", 4, 111),
                    protoOffer(444, 444, "sku-444", 5, 444))
                )
            .setLastModifiedSeqId(5)
            .build());
        deepmindSupplierRepository.save(new Supplier().setBusinessId(444).setId(444).setName("supplier4"));
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3);
        executor.execute();
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(4);
        Assertions
            .assertThat(storageKeyValueService.getLong("msku_offer_last_max_seq_id_key", 0L))
            .isEqualTo(5);
    }

    @Test
    public void simpleTest1P() {
        deepmindSupplierRepository.save(new Supplier().setId(444).setName("supplier4")
            .setSupplierType(SupplierType.REAL_SUPPLIER).setRealSupplierId("000444"));
        Mockito.when(offerChangesService.findChanges(Mockito.any(MboCategoryOfferChanges.FindChangesRequest.class)))
            .thenReturn(MboCategoryOfferChanges.FindChangesResponse.newBuilder().addOffers(
                    protoOffer(beruId.getId(), beruId.getId(), "000444.sku-444", 4, 444,
                        SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)
                )
                .setLastModifiedSeqId(5)
                .build());
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
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "mskuId", "seqId")
            .contains(offer(beruId.getBusinessId(), 444, "sku-444", 4, 444));
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
            .assertThat(storageKeyValueService.getLong("msku_offer_last_max_seq_id_key", 0L))
            .isEqualTo(5);
    }

    @Test
    public void turnOffTest() {
        storageKeyValueService.putValue("import_msku_offer_executor_run_key", false);
        Mockito.when(offerChangesService.findChanges(Mockito.any(MboCategoryOfferChanges.FindChangesRequest.class)))
            .thenReturn(MboCategoryOfferChanges.FindChangesResponse.newBuilder().addAllOffers(List.of(
                protoOffer(111, 111, "sku-111", 4, 111),
                protoOffer(444, 444, "sku-444", 5, 444))
            )
            .setLastModifiedSeqId(5)
            .build());
        deepmindSupplierRepository.save(new Supplier().setBusinessId(444).setId(444).setName("supplier4"));
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3);
        executor.execute();
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3);
    }

    @Test
    public void deleteTest() {
        Mockito.when(offerChangesService.findChanges(Mockito.any(MboCategoryOfferChanges.FindChangesRequest.class)))
            .thenReturn(MboCategoryOfferChanges.FindChangesResponse.newBuilder().addAllOffers(List.of(
                protoOffer(111, 111, "sku-111", 4, 0),
                protoOffer(222, 222, "sku-222", 5, 222, true))
            )
            .setLastModifiedSeqId(5)
            .build());
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3);
        executor.execute();
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(1);
        Assertions
            .assertThat(storageKeyValueService.getLong("msku_offer_last_max_seq_id_key", 0L))
            .isEqualTo(5);
    }

    @Test
    public void insertTest() {
        Mockito.when(offerChangesService.findChanges(Mockito.any(MboCategoryOfferChanges.FindChangesRequest.class)))
            .thenReturn(MboCategoryOfferChanges.FindChangesResponse.newBuilder().addAllOffers(List.of(
                protoOffer(444, 444, "sku-444", 5, 444))
            )
            .setLastModifiedSeqId(5)
            .build());
        deepmindSupplierRepository.save(new Supplier().setBusinessId(444).setId(444).setName("supplier4"));
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
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "mskuId")
            .contains(offer(444, 444, "sku-444", 5, 444));
        Assertions
            .assertThat(sskuStatusRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "availability")
            .containsExactly(
                new SskuStatus()
                    .setSupplierId(444)
                    .setShopSku("sku-444")
                    .setAvailability(OfferAvailability.ACTIVE)
            );
        Assertions
            .assertThat(storageKeyValueService.getLong("msku_offer_last_max_seq_id_key", 0L))
            .isEqualTo(5);
    }

    @Test
    public void insertWithExistingSskuStatusTest() {
        Mockito.when(offerChangesService.findChanges(Mockito.any(MboCategoryOfferChanges.FindChangesRequest.class)))
            .thenReturn(MboCategoryOfferChanges.FindChangesResponse.newBuilder().addAllOffers(List.of(
                    protoOffer(444, 444, "sku-444", 5, 444))
                )
                .setLastModifiedSeqId(5)
                .build());
        deepmindSupplierRepository.save(new Supplier().setBusinessId(444).setId(444).setName("supplier4"));
        sskuStatusRepository.save(new SskuStatus()
            .setSupplierId(444)
            .setShopSku("sku-444")
            .setAvailability(OfferAvailability.DELISTED)
            .setModifiedByUser(true)
            .setModifiedAt(Instant.now())
        );
        var oldStatus = sskuStatusRepository.findByKey(444, "sku-444").get();
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(3);
        Assertions
            .assertThat(sskuStatusRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "availability")
            .containsExactly(
                new SskuStatus()
                    .setSupplierId(444)
                    .setShopSku("sku-444")
                    .setAvailability(OfferAvailability.DELISTED)
            );
        executor.execute();
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .hasSize(4)
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "mskuId")
            .contains(offer(444, 444, "sku-444", 5, 444));
        Assertions
            .assertThat(sskuStatusRepository.findAll())
            .usingElementComparatorOnFields("supplierId", "shopSku", "availability")
            .containsExactly(
                new SskuStatus()
                    .setSupplierId(444)
                    .setShopSku("sku-444")
                    .setAvailability(OfferAvailability.ACTIVE)
            );
        var newStatus = sskuStatusRepository.findByKey(444, "sku-444").get();
        Assertions
            .assertThat(oldStatus.getModifiedAt().isBefore(newStatus.getModifiedAt()))
            .isTrue();
        Assertions
            .assertThat(storageKeyValueService.getLong("msku_offer_last_max_seq_id_key", 0L))
            .isEqualTo(5);
    }

    @Test
    public void updateTest() {
        Mockito.when(offerChangesService.findChanges(Mockito.any(MboCategoryOfferChanges.FindChangesRequest.class)))
            .thenReturn(MboCategoryOfferChanges.FindChangesResponse.newBuilder().addAllOffers(List.of(
                protoOffer(111, 111, "sku-111", 5, 555))
            )
            .setLastModifiedSeqId(5)
            .build());
        Assertions
            .assertThat(sskuStatusRepository.findAll())
            .isEmpty();
        executor.execute();
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "mskuId")
            .contains(offer(111, 111, "sku-111", 5, 555));
        Assertions
            .assertThat(sskuStatusRepository.findAll())
            .isEmpty();
        Assertions
            .assertThat(storageKeyValueService.getLong("msku_offer_last_max_seq_id_key", 0L))
            .isEqualTo(5);
    }

    @Test
    public void dontUpdateIfOldTest() {
        serviceOfferReplicaRepository.save(
            offer(111, 111, "sku-111", 5, 555).setModifiedTs(Instant.now())
        );
        Mockito.when(offerChangesService.findChanges(Mockito.any(MboCategoryOfferChanges.FindChangesRequest.class)))
            .thenReturn(MboCategoryOfferChanges.FindChangesResponse.newBuilder().addAllOffers(List.of(
                    protoOffer(111, 111, "sku-111", 6, 666, Instant.now().minus(10, ChronoUnit.DAYS)))
                )
                .setLastModifiedSeqId(5)
                .build());
        executor.execute();
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "mskuId")
            .contains(offer(111, 111, "sku-111", 5, 555));
        Assertions
            .assertThat(storageKeyValueService.getLong("msku_offer_last_max_seq_id_key", 0L))
            .isEqualTo(5);
    }

    @Test
    public void notUpdateIfEqualsTest() {
        Mockito.when(offerChangesService.findChanges(Mockito.any(MboCategoryOfferChanges.FindChangesRequest.class)))
            .thenReturn(MboCategoryOfferChanges.FindChangesResponse.newBuilder().addAllOffers(List.of(
                protoOffer(111, 111, "sku-111", 5, 111))
            )
            .setLastModifiedSeqId(5)
            .build());
        executor.execute();
        Assertions
            .assertThat(serviceOfferReplicaRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "mskuId", "seqId")
            .contains(offer(111, 111, "sku-111", 1, 111));
        Assertions
            .assertThat(storageKeyValueService.getLong("msku_offer_last_max_seq_id_key", 0L))
            .isEqualTo(5);
    }

    private MboCategoryOfferChanges.SimpleBaseOffer protoOffer(
        int businessId, int supplierId, String shopSku, long seqId, long mskuId) {
        return protoOfferBuilder(businessId, supplierId, shopSku, seqId, mskuId,
            SupplierOffer.SupplierType.TYPE_THIRD_PARTY, false, null).build();
    }

    private MboCategoryOfferChanges.SimpleBaseOffer protoOffer(
        int businessId, int supplierId, String shopSku, long seqId, long mskuId, Instant modifiedTs) {
        return protoOfferBuilder(businessId, supplierId, shopSku, seqId, mskuId,
            SupplierOffer.SupplierType.TYPE_THIRD_PARTY, false, modifiedTs).build();
    }

    private MboCategoryOfferChanges.SimpleBaseOffer protoOffer(
        int businessId, int supplierId, String shopSku, long seqId, long mskuId, boolean isDeleted) {
        return protoOfferBuilder(businessId, supplierId, shopSku, seqId, mskuId,
            SupplierOffer.SupplierType.TYPE_THIRD_PARTY, isDeleted, null).build();
    }

    private MboCategoryOfferChanges.SimpleBaseOffer protoOffer(
        int businessId, int supplierId, String shopSku, long seqId, long mskuId,  SupplierOffer.SupplierType type) {
        return protoOfferBuilder(businessId, supplierId, shopSku, seqId, mskuId, type, false, null).build();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private MboCategoryOfferChanges.SimpleBaseOffer.Builder protoOfferBuilder(
        int businessId, int supplierId, String shopSku, long seqId, long mskuId, SupplierOffer.SupplierType type,
        boolean isDeleted, Instant modifiedTs) {
        return MboCategoryOfferChanges.SimpleBaseOffer.newBuilder()
            .setBusinessId(businessId)
            .setShopSku(shopSku)
            .setTitle("title")
            .setCategoryId(1L)
            .setModifiedSeqId(seqId)
            .setApprovedMappingMskuId(mskuId)
            .setIsDeleted(isDeleted)
            .setModifiedTs(modifiedTs == null
                ? Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli()
                : modifiedTs.toEpochMilli())
            .addServiceOffers(
                MboCategoryOfferChanges.SimpleServiceOffer.newBuilder()
                    .setSupplierId(supplierId)
                    .setSupplierType(type)
                    .setAcceptanceStatus(MboCategoryOfferChanges.SimpleServiceOffer.AcceptanceStatus.OK)
                    .build());
    }

    private ServiceOfferReplica offer(int businessId, int supplierId, String shopSku, long seqId, long mskuId) {
        return new ServiceOfferReplica()
            .setBusinessId(businessId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("title")
            .setCategoryId(1L)
            .setSeqId(seqId)
            .setMskuId(mskuId)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }
}
