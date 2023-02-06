package ru.yandex.market.deepmind.tms.executors;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import Market.DataCamp.DataCampOffer;
import com.google.common.collect.Streams;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.transactions.TransactionImpl;
import ru.yandex.inside.yt.kosher.operations.Operation;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.HidingStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.PartnerRelationType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.PartnerRelation;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.partner_relations.PartnerRelationRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.ChangedSskuRepository;
import ru.yandex.market.deepmind.common.repository.ssku.ChangedSskuRepository.UpdateVersionTsStats;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.availability.ShopSkuMatrixAvailabilityServiceMock;
import ru.yandex.market.deepmind.common.services.offers_converter.OffersConverter;
import ru.yandex.market.deepmind.common.utils.MatrixAvailabilityUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.deepmind.tms.executors.utils.DataCampOfferUtils;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;

import static org.mockito.Mockito.times;
import static ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability.Reason;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.UPLOAD_OFFER_HIDINGS_CROSSDOCK_IS_DISABLED;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.UPLOAD_OFFER_HIDINGS_DROPSHIP_IS_DISABLED;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.UPLOAD_OFFER_HIDINGS_MAX_LIMIT_HIDDEN_OFFERS;
import static ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting.TOMILINO;

public class UploadOfferHidingsToYtExecutorTest extends DeepmindBaseDbTestClass {
    private static final UpdateVersionTsStats STATS = UpdateVersionTsStats.builder()
        .availabilityTs(java.time.Instant.now())
        .queueTs(java.time.Instant.now())
        .reason(Reason.MSKU)
        .build();
    private static final Warehouse SORTING_CENTER_231 = new Warehouse()
        .setId(231L).setName("Сортировочный центр 231")
        .setType(WarehouseType.SORTING_CENTER)
        .setUsingType(WarehouseUsingType.USE_FOR_DROPSHIP);
    @Resource(name = "serviceOfferReplicaRepository")
    private ServiceOfferReplicaRepository serviceOfferRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    protected ChangedSskuRepository changedSskuRepository;
    @Resource
    private PartnerRelationRepository partnerRelationRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    private OffersConverter offersConverter;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    private StorageKeyValueService deepmindStorageKeyValueService;
    private UnstableInit<Yt> yt;
    private ShopSkuMatrixAvailabilityServiceMock availabilityService;
    private UploadOfferHidingsToYtExecutor uploadExecutor;

    @Before
    public void setUp() {
        offersConverter.clearCache();
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_CROSSDOCK);
        deepmindWarehouseRepository.save(SORTING_CENTER_231);
        var suppliers = YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml");
        deepmindSupplierRepository.save(suppliers);
        partnerRelationRepository.save(
            relation(1, PartnerRelationType.CROSSDOCK, 1001L, CROSSDOCK_SOFINO_ID),
            relation(2, PartnerRelationType.CROSSDOCK, 1002L, CROSSDOCK_ROSTOV_ID),
            relation(3, PartnerRelationType.DROPSHIP, 1003L, SORTING_CENTER_231.getId())
        );
        yt = UnstableInit.simple(createYtMock());
        availabilityService = new ShopSkuMatrixAvailabilityServiceMock();
        deepmindStorageKeyValueService = new StorageKeyValueServiceMock();
        deepmindStorageKeyValueService.putValue(UPLOAD_OFFER_HIDINGS_MAX_LIMIT_HIDDEN_OFFERS, 10);

        uploadExecutor = new UploadOfferHidingsToYtExecutor(
            yt,
            changedSskuRepository,
            serviceOfferRepository,
            partnerRelationRepository,
            deepmindWarehouseRepository,
            availabilityService,
            offersConverter,
            deepmindStorageKeyValueService,
            YPath.simple("//tmp/market/mboc/offer_hidings"),
            YPath.simple("//tmp/market/mboc/offer_hidings_like_mstat"),
            "unit-test-pool"
        );
    }

    @After
    public void tearDown() {
        uploadExecutor.setBatchSize(UploadOfferHidingsToYtExecutor.DEFAULT_BATCH_SIZE);
    }

    @Test
    public void execute() {
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer2 = createOffer(2, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);
        updateVersionId(offer1, offer2);
        MatrixAvailability delisted = MatrixAvailabilityUtils.offerDelisted(1, "");
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(1L), TOMILINO,
            null, LocalDate.parse("2020-02-26"), null);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_SOFINO_ID, mskuAvailability);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer2)),
            TOMILINO_ID, delisted);
        uploadExecutor.execute();
        List<YTreeMapNode> actual = captureYtWriteRequest(yt, 1);
        Assertions.assertThat(actual)
            .containsExactlyInAnyOrder(
                mapNode(offer1, 1001L, mskuAvailability),
                mapNode(offer2, 1002L)
            );
    }

    @Test
    public void testConsiderOnlyCrossdockAvailabilities() {
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer2 = createOffer(2, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);
        MatrixAvailability categoryAvailability = MatrixAvailabilityUtils.mskuInCategory(
            CROSSDOCK_SOFINO_ID, "crossdock sofino", 42L, "test", null);
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(200L), TOMILINO,
            null, LocalDate.parse("2020-02-26"), null);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_SOFINO_ID,
            MatrixAvailabilityUtils.offerDelisted(1, ""),
            MatrixAvailabilityUtils.ssku(offer1.getTitle(), offer1.getSupplierId(), offer1.getShopSku(),
                false, null, null, null, null),
            MatrixAvailabilityUtils.mskuArchived(
                new Msku().setId(100L).setTitle("test").setCategoryId(42L)),
            categoryAvailability
        );
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer2)),
            CROSSDOCK_ROSTOV_ID,
            MatrixAvailabilityUtils.offerDelisted(1, ""),
            MatrixAvailabilityUtils.ssku(offer2.getTitle(), offer1.getSupplierId(), offer2.getShopSku(),
                false, null, null, null, null),
            MatrixAvailabilityUtils.mskuEndOfLife(
                new Msku().setId(11L).setTitle("test").setCategoryId(42L)),
            MatrixAvailabilityUtils.sskuSupplier(false, 1, "title", null, null),
            mskuAvailability
        );
        updateVersionId(offer1, offer2);
        uploadExecutor.execute();
        List<YTreeMapNode> actual = captureYtWriteRequest(yt, 1);
        Assertions.assertThat(actual)
            .containsExactlyInAnyOrder(
                mapNode(offer1, 1001L, categoryAvailability),
                mapNode(offer2, 1002L, mskuAvailability)
            );
    }

    @Test
    public void testShouldUploadEvenIfNoBlocks() {
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer2 = createOffer(2, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);
        updateVersionId(offer1, offer2);
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(1L), TOMILINO,
            null, LocalDate.parse("2020-02-26"), null);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_SOFINO_ID, mskuAvailability);
        uploadExecutor.execute();
        List<YTreeMapNode> actual = captureYtWriteRequest(yt, 1);
        Assertions.assertThat(actual)
            .containsExactlyInAnyOrder(
                mapNode(offer1, 1001L, mskuAvailability),
                mapNode(offer2, 1002L)
            );
    }

    @Test
    public void testUploadNotHiddenIfNotToWarehouse() {
        // remove TO_WAREHOUSE
        PartnerRelation partnerRelation = partnerRelationRepository.findBySupplierIdFromWarehouseId(1, 1001L);
        partnerRelationRepository.save(partnerRelation.setToWarehouseId(null));

        var offer1 = createOffer(1, "sku-1", 100L);
        serviceOfferReplicaRepository.save(offer1);

        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_SOFINO_ID,
            MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(100L), TOMILINO, null, null, null)
        );
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_ROSTOV_ID,
            MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(100L), TOMILINO, null, null, null)
        );
        updateVersionId(offer1);

        uploadExecutor.execute();

        List<YTreeMapNode> actual = captureYtWriteRequest(yt, 1);
        Assertions.assertThat(actual)
            .containsExactlyInAnyOrder(
                mapNode(offer1, 1001L)
            );
    }

    @Test
    public void testShouldNotFailIfBlocksExistsButNoChangedSskus() {
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer2 = createOffer(2, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);
        updateVersionId(offer1);
        MatrixAvailability delistedAvailability = MatrixAvailabilityUtils.offerDelisted(1, "");
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(1L), TOMILINO,
            null, LocalDate.parse("2020-02-26"), null);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_SOFINO_ID, mskuAvailability);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer2)),
            CROSSDOCK_SOFINO_ID, delistedAvailability);
        uploadExecutor.execute();
        List<YTreeMapNode> actual = captureYtWriteRequest(yt, 1);
        Assertions.assertThat(actual).containsExactlyInAnyOrder(
            mapNode(offer1, 1001L, mskuAvailability)
        );
    }

    @Test
    public void testShouldNotUploadHiddensForNotCrossdockSuppliers() {
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer2 = createOffer(2, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);
        PartnerRelation partnerRelation = partnerRelationRepository.findBySupplierIdFromWarehouseId(2, 1002L);
        partnerRelationRepository.delete(partnerRelation.getId());
        updateVersionId(offer1, offer2);
        MatrixAvailability delistedAvailability = MatrixAvailabilityUtils.offerDelisted(1, "");
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(1L), TOMILINO,
            null, LocalDate.parse("2020-02-26"), null);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_SOFINO_ID, mskuAvailability);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer2)),
            CROSSDOCK_SOFINO_ID, delistedAvailability);
        uploadExecutor.execute();
        List<YTreeMapNode> actual = captureYtWriteRequest(yt, 1);
        Assertions.assertThat(actual).containsExactlyInAnyOrder(
            mapNode(offer1, 1001L, mskuAvailability)
        );
    }

    @Test
    public void testWithSeveralWarehouses() {
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer2 = createOffer(2, "sku-2", 200L);
        serviceOfferReplicaRepository.save(offer1, offer2);
        updateVersionId(offer1, offer2);
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(100L), TOMILINO,
            null, LocalDate.parse("2020-02-26"), null);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_SOFINO_ID, mskuAvailability);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer2)),
            CROSSDOCK_SOFINO_ID, mskuAvailability);
        uploadExecutor.execute();
        Assertions.assertThat(captureYtWriteRequest(yt, 1))
            .containsExactlyInAnyOrder(
                mapNode(offer1, 1001L, mskuAvailability),
                mapNode(offer2, 1002L)
            );
        availabilityService.clear();
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_ROSTOV_ID, mskuAvailability);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer2)),
            CROSSDOCK_ROSTOV_ID, mskuAvailability);
        changedSskuRepository.updateVersionTs(List.of(getShopSkuKey(offer1), getShopSkuKey(offer2)), STATS);
        uploadExecutor.execute();
        Assertions.assertThat(captureYtWriteRequest(yt, 2))
            .containsExactlyInAnyOrder(
                mapNode(offer1, 1001L),
                mapNode(offer2, 1002L, mskuAvailability)
            );
    }

    @Test
    public void testUploadDropshipHidings() {
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer3 = createOffer(3, "sku-3", 300L);
        serviceOfferReplicaRepository.save(offer1, offer3);
        updateVersionId(offer1, offer3);
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(1L), TOMILINO,
            null, null, null);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_SOFINO_ID, mskuAvailability);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer3)),
            SORTING_CENTER_231.getId(), mskuAvailability);
        uploadExecutor.execute();
        List<YTreeMapNode> actual = captureYtWriteRequest(yt, 1);
        Assertions.assertThat(actual).containsExactlyInAnyOrder(
            mapNode(offer1, 1001L, mskuAvailability),
            mapNode(offer3, 1003L, mskuAvailability)
        );
    }

    @Test // Если to склад неизвестен для разума, то произойдет пустая заливка
    public void testUploadNotHiddenIfRelationToNonExistingWarehouse() {
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer3 = createOffer(3, "sku-3", 300L);
        serviceOfferReplicaRepository.save(offer1, offer3);
        updateVersionId(offer1, offer3);
        // change to warehouse
        var partnerRelation3 = partnerRelationRepository.findBySupplierIdFromWarehouseId(3, 1003L);
        partnerRelation3.setToWarehouseId(10L);
        partnerRelationRepository.save(partnerRelation3);
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(1L), TOMILINO,
            null, null, null);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_SOFINO_ID, mskuAvailability);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer3)),
            SORTING_CENTER_231.getId(), mskuAvailability);
        uploadExecutor.execute();
        List<YTreeMapNode> actual = captureYtWriteRequest(yt, 1);
        Assertions.assertThat(actual).containsExactlyInAnyOrder(
            mapNode(offer1, 1001L, mskuAvailability),
            mapNode(offer3, 1003L)
        );
    }

    @Test // Залили скрытие, потом удалили запись у partner_relation
    public void testReuploadHidingIfToPartnerRelationIsDeleted() {
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer3 = createOffer(3, "sku-3", 300L);
        serviceOfferReplicaRepository.save(offer1, offer3);
        updateVersionId(offer1, offer3);
        // change to warehouse
        var partnerRelation3 = partnerRelationRepository.findBySupplierIdFromWarehouseId(3, 1003L);
        partnerRelationRepository.deleteByEntities(partnerRelation3);
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(1L), TOMILINO,
            null, null, null);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_SOFINO_ID, mskuAvailability);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer3)),
            SORTING_CENTER_231.getId(), mskuAvailability);
        uploadExecutor.execute();
        List<YTreeMapNode> actual = captureYtWriteRequest(yt, 1);
        Assertions.assertThat(actual).containsExactlyInAnyOrder(
            mapNode(offer1, 1001L, mskuAvailability)
            // По хорошему тут мы должны раскрыть ssku3, но на момент реализации такой кейс предусмотрен не был
            // И если товары останутся скрытыми, то ничего страшного не должно произойти, так как все равно такие товары
            // не смогут продаваться
        );
    }

    @Test
    public void testDontSendDropshopOffersIfItDisabled() {
        deepmindStorageKeyValueService.putValue(UPLOAD_OFFER_HIDINGS_DROPSHIP_IS_DISABLED, true);
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer3 = createOffer(3, "sku-3", 300L);
        serviceOfferReplicaRepository.save(offer1, offer3);
        updateVersionId(offer1, offer3);
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(1L), TOMILINO,
            null, null, null);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_SOFINO_ID, mskuAvailability);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer3)),
            SORTING_CENTER_231.getId(), mskuAvailability);
        uploadExecutor.execute();
        List<YTreeMapNode> actual = captureYtWriteRequest(yt, 1);
        Assertions.assertThat(actual).containsExactlyInAnyOrder(
            mapNode(offer1, 1001L, mskuAvailability)
        );
    }

    @Test
    public void testDontSendCrossdockOffersIfItDisabled() {
        deepmindStorageKeyValueService.putValue(UPLOAD_OFFER_HIDINGS_CROSSDOCK_IS_DISABLED, true);
        var offer1 = createOffer(1, "sku-1", 100L);
        var offer3 = createOffer(3, "sku-3", 300L);
        serviceOfferReplicaRepository.save(offer1, offer3);
        updateVersionId(offer1, offer3);
        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(1L), TOMILINO,
            null, null, null);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_SOFINO_ID, mskuAvailability);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer3)),
            SORTING_CENTER_231.getId(), mskuAvailability);
        uploadExecutor.execute();
        List<YTreeMapNode> actual = captureYtWriteRequest(yt, 1);
        Assertions.assertThat(actual).containsExactlyInAnyOrder(
            mapNode(offer3, 1003L, mskuAvailability)
        );
    }

    @Test
    public void testUploadIsStoppedIfCountOfHiddenOffersBecomeMoreThanMax() {
        deepmindStorageKeyValueService.putValue(UPLOAD_OFFER_HIDINGS_MAX_LIMIT_HIDDEN_OFFERS, 2);
        uploadExecutor.setBatchSize(3);

        var crossdock11 = createOffer(1, "sku-1", 100L);
        var crossdock12 = createOffer(1, "sku-2", 300L);
        var dropship31 = createOffer(3, "sku-1", 100L);
        var dropship32 = createOffer(3, "sku-2", 300L);
        serviceOfferReplicaRepository.save(crossdock11, crossdock12, dropship31, dropship32);

        updateVersionId(crossdock11, crossdock12, dropship31, dropship32);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(crossdock11)),
            CROSSDOCK_SOFINO_ID, mskuInCategory());
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(crossdock12)),
            CROSSDOCK_SOFINO_ID, mskuAvailability(300L));
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(dropship31)),
            SORTING_CENTER_231.getId(), mskuAvailability(100L));
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(dropship32)),
            SORTING_CENTER_231.getId(), mskuAvailability(300L));

        Assertions.assertThatThrownBy(() -> uploadExecutor.execute())
            .hasMessage("MBO-26867 UploadOfferHidingsToYtExecutor is stopped, " +
                "because hidden count (3) is greater then max limit (2)");

        // one more run, to make sure that steal fails
        Assertions.assertThatThrownBy(() -> uploadExecutor.execute())
            .hasMessage("MBO-26867 UploadOfferHidingsToYtExecutor is stopped, " +
                "because hidden count (3) is greater then max limit (2)");
    }

    @Test
    public void testIfOfferWasHiddenAndWeUploadItAgainExecutorWontFail() {
        deepmindStorageKeyValueService.putValue(UPLOAD_OFFER_HIDINGS_MAX_LIMIT_HIDDEN_OFFERS, 1);

        var crossdock11 = createOffer(1, "sku-1", 100L);
        var crossdock12 = createOffer(1, "sku-2", 300L);
        serviceOfferReplicaRepository.save(crossdock11, crossdock12);

        updateVersionId(crossdock11, crossdock12);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(crossdock11)),
            CROSSDOCK_SOFINO_ID, mskuInCategory());

        uploadExecutor.execute(); // upload hidden offer first time

        List<YTreeMapNode> actual = captureYtWriteRequest(yt, 1);
        Assertions.assertThat(actual).containsExactlyInAnyOrder(
            mapNode(crossdock11, 1001L, mskuInCategory()),
            mapNode(crossdock12, 1001L)
        );

        // mark & upload second time
        updateVersionId(crossdock11, crossdock12);
        uploadExecutor.execute(); // upload hidden offer second time

        Assertions.assertThat(captureYtWriteRequest(yt, 2))
            .containsExactlyInAnyOrder(
                mapNode(crossdock11, 1001L, mskuInCategory()),
                mapNode(crossdock12, 1001L)
            );
    }

    @Test
    public void testWontFailIfBeforeUploadSomeOffersWasHidden() {
        deepmindStorageKeyValueService.putValue(UPLOAD_OFFER_HIDINGS_MAX_LIMIT_HIDDEN_OFFERS, 2);

        var crossdock11 = createOffer(1, "sku-1", 100L);
        var crossdock12 = createOffer(1, "sku-2", 300L);
        var dropship31 = createOffer(3, "sku-1", 100L);
        var dropship32 = createOffer(3, "sku-2", 300L);
        serviceOfferReplicaRepository.save(crossdock11, crossdock12, dropship31, dropship32);

        // mark some offers as hidden
        updateVersionId(crossdock11, crossdock12);
        var keys = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock11), getShopSkuKey(crossdock12))
            .stream()
            .peek(c -> {
                c.setHidingUploadedStatus(HidingStatus.HIDDEN);
                c.setHidingUploadedVersionTs(c.getVersionTs());
            }).collect(Collectors.toList());
        changedSskuRepository.updateHidingUploadedVersionTsIfMatch(keys, Instant.now(), "");

        // add availabilities
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(dropship31)),
            SORTING_CENTER_231.getId(), mskuAvailability(100));
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(dropship32)),
            SORTING_CENTER_231.getId(), mskuAvailability(300));

        // mark to reupload
        updateVersionId(crossdock11, crossdock12, dropship31, dropship32);

        // Логика тут проста. Эмулируем ситуацию, когда было 2 оффера скрыто
        // их раскрыли
        // и скрыли другие 2
        // получается, что итого снова будет 2 скрыто. Т.е. max limit мы не привысили, а значит и джоба не должна упасть
        uploadExecutor.execute();

        Assertions.assertThat(captureYtWriteRequest(yt, 1))
            .containsExactlyInAnyOrder(
                mapNode(crossdock11, 1001L),
                mapNode(crossdock12, 1001L),
                mapNode(dropship31, 1003L, mskuAvailability(100)),
                mapNode(dropship32, 1003L, mskuAvailability(300))
            );
    }

    @Test
    public void testUploadDropshipOffersWithSeveralFromWarehouses() {
        var partnerRelation = partnerRelationRepository.findBySupplierIdFromWarehouseId(3, 1003L);
        partnerRelationRepository.save(partnerRelation.setFromWarehouseIds(1003L, 333L));

        var offer3 = createOffer(3, "sku-3", 300L);
        serviceOfferReplicaRepository.save(offer3);
        updateVersionId(offer3);

        MatrixAvailability mskuAvailability = MatrixAvailabilityUtils.mskuInWarehouse(false, createMsku(1L), TOMILINO,
            null, null, null);
        availabilityService.addAvailability(serviceOfferRepository.findOfferByKey(getShopSkuKey(offer3)),
            SORTING_CENTER_231.getId(), mskuAvailability);

        uploadExecutor.execute();

        List<YTreeMapNode> actual = captureYtWriteRequest(yt, 1);
        Assertions.assertThat(actual).containsExactlyInAnyOrder(
            mapNode(offer3, 1003L, mskuAvailability),
            mapNode(offer3, 333L, mskuAvailability)
        );
    }

    public static List<YTreeMapNode> captureYtWriteRequest(UnstableInit<Yt> yt, int number) {
        return captureYtWriteRequest(yt.get().tables(), number);
    }

    public static List<YTreeMapNode> captureYtWriteRequest(YtTables ytTables, int number) {
        ArgumentCaptor<Iterator<YTreeMapNode>> requestCaptor = ArgumentCaptor.forClass(Iterator.class);
        Mockito.verify(ytTables, times(number)).write(Mockito.any(), Mockito.anyBoolean(), Mockito.any(), Mockito.any(),
            requestCaptor.capture());
        return requestCaptor.getAllValues().stream().flatMap(Streams::stream).collect(Collectors.toList());
    }

    public static Yt createYtMock() {
        Yt ytMock = Mockito.mock(Yt.class);
        Cypress cypress = Mockito.mock(Cypress.class);
        YtTables ytTables = Mockito.mock(YtTables.class);
        YtTransactions transactions = Mockito.mock(YtTransactions.class);
        YtOperations operations = Mockito.mock(YtOperations.class);
        Transaction transaction = new TransactionImpl(GUID.create(), null, ytMock, Instant.now(),
            Duration.ofHours(1));
        Mockito.when(transactions.startAndGet(Mockito.any(), Mockito.anyBoolean(), Mockito.any()))
            .thenReturn(transaction);
        Operation operation = Mockito.mock(Operation.class);
        Mockito.when(operation.getId()).thenReturn(GUID.create());
        Mockito.when(operations.mergeAndGetOp(Mockito.any(), Mockito.anyBoolean(), Mockito.any()))
            .thenReturn(operation);
        Mockito.when(ytMock.cypress()).thenReturn(cypress);
        Mockito.when(ytMock.tables()).thenReturn(ytTables);
        Mockito.when(ytMock.transactions()).thenReturn(transactions);
        Mockito.when(ytMock.operations()).thenReturn(operations);
        Mockito.doCallRealMethod()
            .when(ytTables)
            .write(Mockito.any(), Mockito.any(), Mockito.anyIterable());
        Mockito.doCallRealMethod()
            .when(ytTables)
            .write(Mockito.any(), Mockito.any(), Mockito.any(Iterator.class));
        return ytMock;
    }

    private YTreeMapNode mapNode(ServiceOfferReplica offer, long fromWarehouseId,
                                 MatrixAvailability... availabilities) {
        var changedSsku = changedSskuRepository.findByShopSkuKeys(
            new ServiceOfferKey(offer.getSupplierId(), offer.getShopSku())).get(0);
        var partnerRelation = partnerRelationRepository.findBySupplierIdFromWarehouseId(
            offer.getBusinessId(), fromWarehouseId);
        DataCampOffer.Offer datacampOffer = DataCampOfferUtils
            .createDatacampOffer(changedSsku, fromWarehouseId, List.of(availabilities),
                partnerRelation.getRelationType(), offer.getBusinessId());
        return uploadExecutor.convert(offer.getMskuId(), datacampOffer);
    }

    private void updateVersionId(ServiceOfferReplica... offers) {
        List<ServiceOfferKey> keys = Arrays.stream(offers)
            .map(o -> new ServiceOfferKey(o.getSupplierId(), o.getShopSku()))
            .collect(Collectors.toList());
        List<ChangedSskuRepository.UpdateVersionTsStats> stats = new ArrayList<>();
        for (ServiceOfferKey key : keys) {
            stats.add(ChangedSskuRepository.UpdateVersionTsStats.builder()
                .reason(Reason.MSKU)
                .queueTs(Instant.now())
                .availabilityTs(Instant.now())
                .build());
        }
        int count = changedSskuRepository.updateVersionTs(keys, stats);
        if (offers.length != count) {
            throw new IllegalStateException("Not all shop skus are updated, only " + count + " are updated");
        }
    }

    private PartnerRelation relation(int supplierId, PartnerRelationType type, long fromWhId, @Nullable Long toWhId) {
        return new PartnerRelation()
            .setSupplierId(supplierId)
            .setRelationType(type)
            .setFromWarehouseIds(fromWhId)
            .setToWarehouseId(toWhId);
    }

    private Msku createMsku(long mskuId) {
        return new Msku().setId(mskuId).setTitle("msku " + mskuId).setCategoryId(-1L).setVendorId(-1L);
    }

    private ServiceOfferReplica createOffer(
        int supplierId, String ssku, long mskuId) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setTitle("title " + ssku)
            .setCategoryId(99L)
            .setSeqId(0L)
            .setMskuId(mskuId)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private MatrixAvailability mskuAvailability(long mskuId) {
        var test = new Msku().setId(mskuId).setTitle("test").setCategoryId(42L);
        return MatrixAvailabilityUtils.mskuInWarehouse(false, test,
            CROSSDOCK_SOFINO_ID, "crossdock sofino", null, null, null);
    }

    private MatrixAvailability mskuInCategory() {
        return MatrixAvailabilityUtils.mskuInCategory(CROSSDOCK_SOFINO_ID, "crossdock sofino", 42L, "test", null);
    }

    private ServiceOfferKey getShopSkuKey(ServiceOfferReplica offer) {
        return new ServiceOfferKey(offer.getSupplierId(), offer.getShopSku());
    }
}
