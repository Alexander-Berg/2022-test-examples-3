package ru.yandex.market.deepmind.app.openapi;

import java.time.Instant;
import java.util.List;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.deepmind.app.model.ShopSkuStatus;
import ru.yandex.market.deepmind.app.model.UpdateSskuStatusReason;
import ru.yandex.market.deepmind.app.model.UpdateSskuStatusRequest;
import ru.yandex.market.deepmind.app.openapi.exception.ApiResponseEntityExceptionHandler;
import ru.yandex.market.deepmind.app.services.SskuMskuStatusHelperServiceImpl;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.status.SskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.stock.MskuStockRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.background.BackgroundServiceMock;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverter;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuHelperService;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusServiceImpl;
import ru.yandex.market.deepmind.common.services.statuses.SskuMskuStatusValidationServiceImpl;
import ru.yandex.market.deepmind.common.stock.MskuStockInfo;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.mboc.common.MbocErrors;

import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.FIRST_PARTY;
import static ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType.REAL_SUPPLIER;

/**
 * Tests of {@link SskuStatusApiController}.
 */
public class SskuStatusApiControllerTest extends BaseOpenApiTest {
    private static final String SSKU_STATS_UPDATE_URL = "/api/v1/ssku-status/update";
    private static final String SSKU_STATUS_BY_KEY_URL = "/api/v1/ssku-status/by-key";

    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private MskuStockRepository mskuStockRepository;
    @Resource
    private SskuStatusRepository sskuStatusRepository;
    @Resource
    private MskuStatusRepository mskuStatusRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    private OffersConverter offersConverter;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    private TransactionTemplate transactionTemplate;
    private SskuStatusApiController controller;

    @Before
    public void setUp() {
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);

        offersConverter.clearCache();
        var sskuMskuHelperService = new SskuMskuHelperService(serviceOfferReplicaRepository, sskuStatusRepository,
            mskuStatusRepository);
        var sskuMskuStatusValidationService = new SskuMskuStatusValidationServiceImpl(mskuStockRepository,
            serviceOfferReplicaRepository, deepmindSupplierRepository, sskuMskuHelperService);
        var sskuMskuStatusService = new SskuMskuStatusServiceImpl(sskuStatusRepository, mskuStatusRepository,
            sskuMskuStatusValidationService, sskuMskuHelperService, transactionTemplate);
        var sskuMskuStatusHelperService = new SskuMskuStatusHelperServiceImpl(serviceOfferReplicaRepository,
            new BackgroundServiceMock(), sskuMskuStatusService, sskuMskuStatusValidationService, sskuStatusRepository,
            deepmindMskuRepository, mskuStatusRepository, transactionTemplate);
        controller = new SskuStatusApiController(serviceOfferReplicaRepository, sskuMskuStatusHelperService,
            offersConverter, sskuStatusRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new ApiResponseEntityExceptionHandler()).build();
    }

    @Test
    public void apiV1SskuStatusUpdatePostSimpleTest() throws Exception {
        deepmindSupplierRepository.save(supplier(111), supplier(222), supplier(333));
        serviceOfferReplicaRepository.save(
            createOffer(111, "shopSku-111"),
            createOffer(222, "shopSku-222"),
            createOffer(333, "shopSku-333")
        );
        sskuStatusRepository.save(
            sskuStatus(111, "shopSku-111", OfferAvailability.ACTIVE, null),
            sskuStatus(222, "shopSku-222", OfferAvailability.ACTIVE, null),
            sskuStatus(333, "shopSku-333", OfferAvailability.ACTIVE, null)
        );

        var keys = List.of(
            new ServiceOfferKey(111, "shopSku-111"),
            new ServiceOfferKey(222, "shopSku-222"),
            new ServiceOfferKey(333, "shopSku-333")
        );
        var request = updateRequest(keys, OfferAvailability.INACTIVE_TMP, "2021-01-01T14:39:53Z");
        postJson(SSKU_STATS_UPDATE_URL, request);
        var statuses = sskuStatusRepository.find(List.of(
            new ServiceOfferKey(111, "shopSku-111"),
            new ServiceOfferKey(222, "shopSku-222"),
            new ServiceOfferKey(333, "shopSku-333")
        ));
        Assertions
            .assertThat(statuses)
            .extracting(SskuStatus::getAvailability)
            .containsOnly(OfferAvailability.INACTIVE_TMP);
    }

    @Test
    public void apiV1SskuStatusUpdatePost1PSimpleTest() throws Exception {
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(
            createOffer(1002, "shopSku-111")
        );
        sskuStatusRepository.save(
            sskuStatus(1002, "shopSku-111", OfferAvailability.ACTIVE, null)
        );

        var request = updateRequest(List.of(new ServiceOfferKey(465852, "001234.shopSku-111")),
            OfferAvailability.INACTIVE_TMP, "2021-01-01T14:39:53Z");
        postJson(SSKU_STATS_UPDATE_URL, request);

        var statuses = sskuStatusRepository.find(List.of(
            new ServiceOfferKey(1002, "shopSku-111")
        ));
        Assertions.assertThat(statuses)
            .extracting(SskuStatus::getAvailability)
            .containsExactly(OfferAvailability.INACTIVE_TMP);
    }

    @Test
    public void apiV1SskuStatusUpdatePostStatusFinishErrorTest() throws Exception {
        deepmindSupplierRepository.save(supplier(111), supplier(222), supplier(333));
        serviceOfferReplicaRepository.save(
            createOffer(111, "shopSku-111"),
            createOffer(222, "shopSku-222"),
            createOffer(333, "shopSku-333")
        );
        sskuStatusRepository.save(
            sskuStatus(111, "shopSku-111", OfferAvailability.ACTIVE, null),
            sskuStatus(222, "shopSku-222", OfferAvailability.ACTIVE, null),
            sskuStatus(333, "shopSku-333", OfferAvailability.ACTIVE, null)
        );
        var keys = List.of(
            new ServiceOfferKey(111, "shopSku-111"),
            new ServiceOfferKey(222, "shopSku-222"),
            new ServiceOfferKey(333, "shopSku-333")
        );
        var request = updateRequest(keys, OfferAvailability.INACTIVE, "wrong format");
        post400Json(SSKU_STATS_UPDATE_URL, request, "Attribute status_finish_at in wrong format");

        request = updateRequest(keys, OfferAvailability.INACTIVE_TMP, null);
        post400Json(SSKU_STATS_UPDATE_URL, request, "statusFinishAt and comment is mandatory for INACTIVE_TMP");

        var statuses = sskuStatusRepository.find(List.of(
            new ServiceOfferKey(111, "shopSku-111"),
            new ServiceOfferKey(222, "shopSku-222"),
            new ServiceOfferKey(333, "shopSku-333")
        ));
        Assertions
            .assertThat(statuses)
            .extracting(SskuStatus::getAvailability)
            .containsOnly(OfferAvailability.ACTIVE);
    }

    @Test
    public void apiV1SskuStatusUpdatePostNoOffersErrorTest() throws Exception {
        deepmindSupplierRepository.save(supplier(111), supplier(222), supplier(333));
        serviceOfferReplicaRepository.save(
            createOffer(111, "shopSku-111"),
            createOffer(222, "shopSku-222"),
            createOffer(333, "shopSku-333")
        );
        sskuStatusRepository.save(
            sskuStatus(111, "shopSku-111", OfferAvailability.ACTIVE, null),
            sskuStatus(222, "shopSku-222", OfferAvailability.ACTIVE, null),
            sskuStatus(333, "shopSku-333", OfferAvailability.ACTIVE, null)
        );
        var keys = List.of(
            new ServiceOfferKey(111, "shopSku-444"),
            new ServiceOfferKey(222, "shopSku-555")
        );
        var request = updateRequest(keys, OfferAvailability.INACTIVE, null);

        post400Json(SSKU_STATS_UPDATE_URL, request, "Some of the shopSku keys doesn't have offers: " +
            "(supplier_id: 222; shop_sku: shopSku-555), (supplier_id: 111; shop_sku: shopSku-444)");

        var statuses = sskuStatusRepository.find(List.of(
            new ServiceOfferKey(111, "shopSku-111"),
            new ServiceOfferKey(222, "shopSku-222"),
            new ServiceOfferKey(333, "shopSku-333")
        ));
        Assertions
            .assertThat(statuses)
            .extracting(SskuStatus::getAvailability)
            .containsOnly(OfferAvailability.ACTIVE);
    }

    @Test
    public void apiV1SskuStatusUpdatePostSaveStatusErrorTest() throws Exception {
        serviceOfferReplicaRepository.save(
            createOffer(111, "shopSku-111", 1, REAL_SUPPLIER),
            createOffer(222, "shopSku-222", 2, REAL_SUPPLIER),
            createOffer(333, "shopSku-333", 3, REAL_SUPPLIER)
        );
        sskuStatusRepository.save(
            sskuStatus(111, "shopSku-111", OfferAvailability.ACTIVE),
            sskuStatus(222, "shopSku-222", OfferAvailability.ACTIVE),
            sskuStatus(333, "shopSku-333", OfferAvailability.ACTIVE)
        );

        deepmindWarehouseRepository.save(new Warehouse().setId(1L).setType(WarehouseType.FULFILLMENT).setName("name1")
            .setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT));
        var stockInfo = new MskuStockInfo()
            .setSupplierId(111)
            .setShopSku("shopSku-111")
            .setWarehouseId(1)
            .setFitInternal(1);
        mskuStockRepository.insert(stockInfo);

        var keys = List.of(
            new ServiceOfferKey(111, "shopSku-111"),
            new ServiceOfferKey(222, "shopSku-222"),
            new ServiceOfferKey(333, "shopSku-333")
        );
        var request = updateRequest(keys, OfferAvailability.DELISTED, null);

        post500Json(SSKU_STATS_UPDATE_URL, request, MbocErrors.get().offerDelistedHasStocks().toString());

        var statuses = sskuStatusRepository.find(List.of(
            new ServiceOfferKey(111, "shopSku-111"),
            new ServiceOfferKey(222, "shopSku-222"),
            new ServiceOfferKey(333, "shopSku-333")
        ));
        Assertions
            .assertThat(statuses)
            .extracting(SskuStatus::getAvailability)
            .containsOnly(OfferAvailability.ACTIVE);

    }

    @Test
    public void apiV1SskuStatusByKeyPostSimpleSupplier() throws Exception {
        serviceOfferReplicaRepository.save(
            createOffer(111, "shopSku-111", 1, FIRST_PARTY),
            createOffer(222, "shopSku-222", 2, FIRST_PARTY),
            createOffer(333, "shopSku-333", 3, FIRST_PARTY),
            createOffer(444, "shopSku-333", 3, FIRST_PARTY)
        );
        sskuStatusRepository.save(
            sskuStatus(111, "shopSku-111", OfferAvailability.ACTIVE, null),
            sskuStatus(222, "shopSku-222", OfferAvailability.DELISTED, null),
            sskuStatus(333, "shopSku-333", OfferAvailability.PENDING, null)
        );

        var key1 = new ServiceOfferKey(111, "shopSku-111");
        var key2 = new ServiceOfferKey(222, "shopSku-222");
        var key3 = new ServiceOfferKey(333, "shopSku-333");

        var mvcResult = postJson(SSKU_STATUS_BY_KEY_URL, List.of(key1, key2, key3));
        var statuses = readJsonList(mvcResult, ShopSkuStatus.class);

        Assertions.assertThat(statuses)
                .containsExactlyInAnyOrder(
                    new ShopSkuStatus()
                        .supplierId(111)
                        .shopSku("shopSku-111")
                        .status(OfferAvailability.ACTIVE),
                    new ShopSkuStatus()
                        .supplierId(222)
                        .shopSku("shopSku-222")
                        .status(OfferAvailability.DELISTED),
                    new ShopSkuStatus()
                        .supplierId(333)
                        .shopSku("shopSku-333")
                        .status(OfferAvailability.PENDING)
                );
    }

    @Test
    public void firstPartySupplierShouldReturnInExternalForm() throws Exception {
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(
            createOffer(102, "shopSku-111"),
            createOffer(1002, "shopSku-111")
        );
        sskuStatusRepository.save(
            sskuStatus(1002, "shopSku-111", OfferAvailability.INACTIVE, null)
        );

        var mvcResult = postJson(SSKU_STATUS_BY_KEY_URL, List.of(new ServiceOfferKey(465852, "001234.shopSku-111")));
        var statuses = readJsonList(mvcResult, ShopSkuStatus.class);

        Assertions.assertThat(statuses)
            .containsExactlyInAnyOrder(
                new ShopSkuStatus()
                    .supplierId(465852)
                    .shopSku("001234.shopSku-111")
                    .status(OfferAvailability.INACTIVE)
            );
    }

    @Test
    public void dontReturnIfNoStatus() throws Exception {
        var mvcResult = postJson(SSKU_STATUS_BY_KEY_URL, List.of(new ServiceOfferKey(465852, "001234.12121212121")));
        var statuses = readJsonList(mvcResult, ShopSkuStatus.class);

        Assertions.assertThat(statuses).isEmpty();
    }

    private UpdateSskuStatusRequest updateRequest(List<ServiceOfferKey> keys, OfferAvailability status,
                                                  String statusFinishAt) {
        return new UpdateSskuStatusRequest()
            .keys(keys)
            .status(status)
            .reason(UpdateSskuStatusReason.TENDER_PURCHASES)
            .comment("comment1")
            .statusFinishAt(statusFinishAt);
    }

    private SskuStatus sskuStatus(int supId, String shopSku, OfferAvailability availability) {
        return new SskuStatus().setSupplierId(supId).setShopSku(shopSku)
            .setAvailability(availability)
            .setStatusStartAt(Instant.now());
    }

    private SskuStatus sskuStatus(int supplierId, String shopSku, OfferAvailability availability,
                                  Instant statusFinishAt) {
        return sskuStatus(supplierId, shopSku, availability)
            .setComment("comment")
            .setStatusFinishAt(statusFinishAt)
            .setModifiedByUser(false);
    }

    private ServiceOfferReplica createOffer(int supplierId, String shopSku, long mskuId, SupplierType supplierType) {
        if (deepmindSupplierRepository.findByIds(List.of(supplierId)).isEmpty()) {
            var supplier = new Supplier().setId(supplierId).setName("test_supplier_" + supplierId)
                .setSupplierType(supplierType);
                if (supplierType == REAL_SUPPLIER) {
                    supplier.setRealSupplierId("0000" + supplierId);
                }
            deepmindSupplierRepository.save(supplier);
        }
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(1L)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(supplierType)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private ServiceOfferReplica createOffer(int supplierId, String ssku) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setTitle("title " + ssku)
            .setCategoryId(33L)
            .setSeqId(0L)
            .setMskuId(111L)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private Supplier supplier(int id) {
        return new Supplier()
            .setId(id)
            .setSupplierType(FIRST_PARTY)
            .setName("name_" + id)
            .setFulfillment(true);
    }
}
