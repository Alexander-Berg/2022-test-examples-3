package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.Resource;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers.OfferIdentifiers;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.HidingStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.PartnerRelationType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.ChangedSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.PartnerRelation;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.mocks.LogbrokerEventPublisherMock;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.partner_relations.PartnerRelationRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.ChangedSskuRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.availability.ShopSkuMatrixAvailabilityServiceMock;
import ru.yandex.market.deepmind.common.utils.MatrixAvailabilityUtils;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.tms.executors.utils.DataCampOfferUtils;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.datacamp.model.DataCampOffersEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability.Reason;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_SOFINO_ID;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.UPLOAD_OFFER_HIDINGS_CROSSDOCK_IS_DISABLED;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.UPLOAD_OFFER_HIDINGS_DROPSHIP_IS_DISABLED;
import static ru.yandex.market.deepmind.common.services.DeepmindConstants.UPLOAD_OFFER_HIDINGS_MAX_LIMIT_HIDDEN_OFFERS;

@Slf4j
@SuppressWarnings("checkstyle:MagicNumber")
public class UploadOfferHidingsToLogbrokerExecutorTest extends DeepmindBaseDbTestClass {
    private static final Warehouse SORTING_CENTER_231 = new Warehouse()
        .setId(231L).setName("Сортировочный центр 231")
        .setType(WarehouseType.SORTING_CENTER)
        .setUsingType(WarehouseUsingType.USE_FOR_DROPSHIP);
    private static final long CROSSDOCK_FROM_WAREHOUSE = 10L;
    public static final long DROPSHIP_FROM_WAREHOUSE = 101L;

    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private PartnerRelationRepository partnerRelationRepository;
    @Resource
    private ChangedSskuRepository changedSskuRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private StorageKeyValueService deepmindStorageKeyValueService;
    private LogbrokerEventPublisherMock<DataCampOffersEvent> logbrokerEventPublisherMock;
    private ShopSkuMatrixAvailabilityServiceMock availabilityService;

    private UploadOfferHidingsToLogbrokerExecutor executor;

    private ServiceOfferReplica crossdock11;
    private ServiceOfferReplica crossdock12;
    private ServiceOfferReplica crossdock21;
    private ServiceOfferReplica crossdock31;
    private ServiceOfferReplica crossdock32;
    private ServiceOfferReplica crossdock33;
    private ServiceOfferReplica dropship11;
    private ServiceOfferReplica dropship12;
    private ServiceOfferReplica ff1;

    @Before
    public void setUp() {
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_CROSSDOCK);
        deepmindWarehouseRepository.save(SORTING_CENTER_231);

        deepmindSupplierRepository.save(createSupplier(1), createSupplier(2), createSupplier(3));
        partnerRelationRepository.save(
            relation(1, PartnerRelationType.CROSSDOCK, CROSSDOCK_FROM_WAREHOUSE, CROSSDOCK_SOFINO_ID),
            relation(2, PartnerRelationType.DROPSHIP, DROPSHIP_FROM_WAREHOUSE, SORTING_CENTER_231.getId())
        );

        crossdock11 = serviceOfferReplicaRepository.save(createOffer(1, "offer-11", 11)).get(0);
        crossdock12 = serviceOfferReplicaRepository.save(createOffer(1, "offer-12", 12)).get(0);
        crossdock21 = serviceOfferReplicaRepository.save(createOffer(1, "offer-21", 21)).get(0);
        crossdock31 = serviceOfferReplicaRepository.save(createOffer(1, "offer-31", 31)).get(0);
        crossdock32 = serviceOfferReplicaRepository.save(createOffer(1, "offer-32", 32)).get(0);
        crossdock33 = serviceOfferReplicaRepository.save(createOffer(1, "offer-33", 33)).get(0);
        dropship11 = serviceOfferReplicaRepository.save(createOffer(2, "offer-2-11", 11)).get(0);
        dropship12 = serviceOfferReplicaRepository.save(createOffer(2, "offer-2-12", 12)).get(0);
        ff1 = serviceOfferReplicaRepository.save(createOffer(3, "offer-3-1", 13)).get(0);

        deepmindStorageKeyValueService = new StorageKeyValueServiceMock();
        logbrokerEventPublisherMock = new LogbrokerEventPublisherMock<>();
        availabilityService = new ShopSkuMatrixAvailabilityServiceMock();

        deepmindStorageKeyValueService.putValue(UPLOAD_OFFER_HIDINGS_MAX_LIMIT_HIDDEN_OFFERS, 10);
        executor = new UploadOfferHidingsToLogbrokerExecutor(
            logbrokerEventPublisherMock,
            availabilityService,
            changedSskuRepository,
            partnerRelationRepository,
            deepmindStorageKeyValueService,
            deepmindWarehouseRepository,
            serviceOfferReplicaRepository
        );
    }

    @After
    public void tearDown() {
        executor.setBatchSize(UploadOfferHidingsToLogbrokerExecutor.BATCH_SIZE);
    }

    @Test
    public void dryRun() {
        executor.execute();

        List<ChangedSsku> all = changedSskuRepository.findAll();
        assertThat(all).isEmpty();
    }

    @Test
    public void uploadNotDisabledOffers() {
        updateVersionId(crossdock11, crossdock21);
        ChangedSsku ssku11 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock11)).get(0);
        ChangedSsku ssku21 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock21)).get(0);

        executor.execute();

        // check logbroker events
        List<DataCampOffer.Offer> sendOffers = getSendOffers();
        assertThat(sendOffers)
            .containsExactlyInAnyOrder(
                dataCampOffer(ssku11, CROSSDOCK_FROM_WAREHOUSE),
                dataCampOffer(ssku21, CROSSDOCK_FROM_WAREHOUSE)
            );

        // check upload
        List<ChangedSsku> all = changedSskuRepository.findAll();
        assertThat(all)
            .usingElementComparatorOnFields("supplierId", "shopSku", "versionTs", "hidingUploadedVersionTs")
            .containsExactlyInAnyOrder(
                changedSsku(getShopSkuKey(crossdock11), ssku11.getVersionTs(), ssku11.getVersionTs()),
                changedSsku(getShopSkuKey(crossdock21), ssku21.getVersionTs(), ssku21.getVersionTs())
            );
    }

    @Test
    public void testConsiderOnlyCrossdockAvailabilities() {
        updateVersionId(crossdock11, crossdock21);
        ChangedSsku ssku11 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock11)).get(0);
        ChangedSsku ssku21 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock21)).get(0);

        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(crossdock11)),
            CROSSDOCK_SOFINO_ID,
            MatrixAvailabilityUtils.offerDelisted(1, ""),
            MatrixAvailabilityUtils.ssku(crossdock11.getTitle(), ssku11.getSupplierId(), ssku11.getShopSku(),
                false, null, null, null, null),
            MatrixAvailabilityUtils.mskuArchived(
                new Msku().setId(11L).setTitle("test").setCategoryId(42L)),
            mskuInCategory()
        );
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(crossdock21)),
            CROSSDOCK_SOFINO_ID,
            MatrixAvailabilityUtils.offerDelisted(1, ""),
            MatrixAvailabilityUtils.ssku(crossdock21.getTitle(), ssku21.getSupplierId(), ssku21.getShopSku(),
                false, null, null, null, null),
            MatrixAvailabilityUtils.mskuEndOfLife(
                new Msku().setId(11L).setTitle("test").setCategoryId(42L)),
            MatrixAvailabilityUtils.sskuSupplier(false, 1, "title", null, null),
            mskuAvailability(21)
        );

        executor.execute();

        List<DataCampOffer.Offer> sendOffers = getSendOffers();
        assertThat(sendOffers)
            .containsExactlyInAnyOrder(
                dataCampOffer(ssku11, CROSSDOCK_FROM_WAREHOUSE, mskuInCategory()),
                dataCampOffer(ssku21, CROSSDOCK_FROM_WAREHOUSE, mskuAvailability(21))
            );
    }

    @Test
    public void testConsiderOnlyDropshipAvailabilities() {
        updateVersionId(crossdock11, crossdock21);
        ChangedSsku ssku11 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock11)).get(0);
        ChangedSsku ssku21 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock21)).get(0);

        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(crossdock11)),
            CROSSDOCK_SOFINO_ID,
            MatrixAvailabilityUtils.offerDelisted(1, ""),
            MatrixAvailabilityUtils.ssku(crossdock11.getTitle(), ssku11.getSupplierId(), ssku11.getShopSku(),
                false, null, null, null, null),
            MatrixAvailabilityUtils.mskuArchived(
                new Msku().setId(11L).setTitle("test").setCategoryId(42L)),
            mskuInCategory()
        );
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(crossdock21)),
            CROSSDOCK_SOFINO_ID,
            MatrixAvailabilityUtils.offerDelisted(1, ""),
            MatrixAvailabilityUtils.ssku(crossdock21.getTitle(), ssku11.getSupplierId(), ssku11.getShopSku(),
                false, null, null, null, null),
            MatrixAvailabilityUtils.mskuEndOfLife(
                new Msku().setId(11L).setTitle("test").setCategoryId(42L)),
            MatrixAvailabilityUtils.sskuSupplier(false, 1, "title", null, null),
            mskuAvailability(21)
        );

        executor.execute();

        List<DataCampOffer.Offer> sendOffers = getSendOffers();
        assertThat(sendOffers)
            .containsExactlyInAnyOrder(
                dataCampOffer(ssku11, CROSSDOCK_FROM_WAREHOUSE, mskuInCategory()),
                dataCampOffer(ssku21, CROSSDOCK_FROM_WAREHOUSE, mskuAvailability(21))
            );
    }

    @Test
    public void testUploadDisabledOffers() {
        updateVersionId(crossdock11, crossdock21);
        ChangedSsku ssku11 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock11)).get(0);
        ChangedSsku ssku21 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock21)).get(0);

        // mark ssku as have blocks
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(crossdock11)),
            CROSSDOCK_SOFINO_ID, mskuAvailability(11));
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(crossdock21)),
            CROSSDOCK_SOFINO_ID, mskuAvailability(21));

        executor.execute();

        // check logbroker events
        List<DataCampOffer.Offer> sendOffers = getSendOffers();
        assertThat(sendOffers)
            .containsExactlyInAnyOrder(
                dataCampOffer(ssku11, CROSSDOCK_FROM_WAREHOUSE, mskuAvailability(11)),
                dataCampOffer(ssku21, CROSSDOCK_FROM_WAREHOUSE, mskuAvailability(21))
            );

        // check upload
        List<ChangedSsku> all = changedSskuRepository.findAll();
        assertThat(all)
            .usingElementComparatorOnFields("supplierId", "shopSku", "versionTs", "hidingUploadedVersionTs")
            .containsExactlyInAnyOrder(
                changedSsku(getShopSkuKey(crossdock11), ssku11.getVersionTs(), ssku11.getVersionTs()),
                changedSsku(getShopSkuKey(crossdock21), ssku21.getVersionTs(), ssku21.getVersionTs())
            );
    }

    @Test
    public void testUploadNotHiddenIfNotToWarehouse() {
        // remove TO_WAREHOUSE
        PartnerRelation partnerRelation = partnerRelationRepository.findBySupplierIdFromWarehouseId(1, 10L);
        partnerRelationRepository.save(partnerRelation.setToWarehouseId(null));

        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(crossdock11)),
            CROSSDOCK_SOFINO_ID, mskuAvailability(100L));
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(crossdock11)),
            CROSSDOCK_ROSTOV_ID, mskuAvailability(100L));
        updateVersionId(crossdock11);

        executor.execute();

        // check logbroker events
        List<DataCampOffer.Offer> sendOffers = getSendOffers();
        assertThat(sendOffers)
            .containsExactlyInAnyOrder(
                dataCampOffer(crossdock11, CROSSDOCK_FROM_WAREHOUSE)
            );
    }

    @Test
    public void testUploadFailed() {
        updateVersionId(crossdock11, crossdock21);
        ChangedSsku ssku11 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock11)).get(0);
        ChangedSsku ssku21 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock21)).get(0);

        logbrokerEventPublisherMock.setLogbrokerEventFilter((DataCampOffersEvent event) -> {
            List<DataCampOffer.Offer> offers = event.getPayload().getOffersList().stream()
                .flatMap(s -> s.getOfferList().stream())
                .collect(Collectors.toList());
            List<OfferIdentifiers> offerIds = offers.stream()
                .map(DataCampOffer.Offer::getIdentifiers)
                .collect(Collectors.toList());

            OfferIdentifiers offerId21 = OfferIdentifiers.newBuilder()
                .setShopId(crossdock21.getBusinessId())
                .setOfferId(crossdock21.getShopSku())
                .setWarehouseId(Math.toIntExact(CROSSDOCK_FROM_WAREHOUSE))
                .build();
            if (offerIds.contains(offerId21)) {
                log.debug("Offer failed to upload: {}", offerId21);
                return false;
            } else {
                return true;
            }
        });

        Assertions.assertThatThrownBy(() -> executor.execute())
            .hasMessage("Error encountered while sending event to Logbroker");

        // check logbroker events
        List<DataCampOffer.Offer> sendOffers = getSendOffers();
        assertThat(sendOffers).isEmpty();

        // check upload
        List<ChangedSsku> all = changedSskuRepository.findAll();
        assertThat(all)
            .usingElementComparatorOnFields("supplierId", "shopSku", "versionTs", "hidingUploadedVersionTs")
            .containsExactlyInAnyOrder(
                changedSsku(getShopSkuKey(crossdock11), ssku11.getVersionTs(), null),
                changedSsku(getShopSkuKey(crossdock21), ssku21.getVersionTs(), null)
            );
    }

    @Test
    public void testUploadPartiallyFailed() {
        executor.setBatchSize(2);
        // Обновляем всех последовательно, чтобы в том же порядке заливка была
        updateVersionId(crossdock11);
        updateVersionId(crossdock12);
        updateVersionId(crossdock21);
        updateVersionId(crossdock31);
        updateVersionId(crossdock32);
        updateVersionId(crossdock33);

        ChangedSsku ssku11 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock11)).get(0);
        ChangedSsku ssku12 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock12)).get(0);
        ChangedSsku ssku21 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock21)).get(0);
        ChangedSsku ssku31 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock31)).get(0);
        ChangedSsku ssku32 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock32)).get(0);
        ChangedSsku ssku33 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock33)).get(0);

        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(crossdock12)),
            CROSSDOCK_SOFINO_ID, mskuAvailability(12));

        logbrokerEventPublisherMock.setLogbrokerEventFilter((DataCampOffersEvent event) -> {
            List<DataCampOffer.Offer> offers = event.getPayload().getOffersList().stream()
                .flatMap(s -> s.getOfferList().stream())
                .collect(Collectors.toList());
            List<OfferIdentifiers> offerIds = offers.stream()
                .map(DataCampOffer.Offer::getIdentifiers)
                .collect(Collectors.toList());

            OfferIdentifiers offerId31 = OfferIdentifiers.newBuilder()
                .setShopId(crossdock31.getBusinessId())
                .setOfferId(crossdock31.getShopSku())
                .setWarehouseId(Math.toIntExact(CROSSDOCK_FROM_WAREHOUSE))
                .build();
            if (offerIds.contains(offerId31)) {
                log.debug("Offer failed to upload: {}", offerId31);
                return false;
            } else {
                return true;
            }
        });

        Assertions.assertThatThrownBy(() -> executor.execute())
            .hasMessage("Error encountered while sending event to Logbroker");

        // check logbroker events
        List<DataCampOffer.Offer> sendOffers = getSendOffers();
        assertThat(sendOffers)
            .containsExactlyInAnyOrder(
                dataCampOffer(ssku11, CROSSDOCK_FROM_WAREHOUSE),
                dataCampOffer(ssku12, CROSSDOCK_FROM_WAREHOUSE, mskuAvailability(12))
            );

        // check upload
        List<ChangedSsku> all = changedSskuRepository.findAll();
        assertThat(all)
            .usingElementComparatorOnFields("supplierId", "shopSku", "versionTs", "hidingUploadedVersionTs")
            .containsExactlyInAnyOrder(
                changedSsku(getShopSkuKey(crossdock11), ssku11.getVersionTs(), ssku11.getVersionTs()),
                changedSsku(getShopSkuKey(crossdock12), ssku12.getVersionTs(), ssku12.getVersionTs()),
                // Ниже офферы, которые не залились
                changedSsku(getShopSkuKey(crossdock21), ssku21.getVersionTs(), null),
                changedSsku(getShopSkuKey(crossdock31), ssku31.getVersionTs(), null),
                changedSsku(getShopSkuKey(crossdock32), ssku32.getVersionTs(), null),
                changedSsku(getShopSkuKey(crossdock33), ssku33.getVersionTs(), null)
            );
    }

    @Test
    public void testDoubleUpload() {
        // first update and upload
        updateVersionId(crossdock11);
        ChangedSsku ssku11 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock11)).get(0);
        executor.execute();

        ChangedSsku updated = changedSskuRepository.findByShopSkuKey(ssku11.getSupplierId(), ssku11.getShopSku());
        assertThat(List.of(updated))
            .usingElementComparatorOnFields("supplierId", "shopSku", "versionTs", "hidingUploadedVersionTs")
            .containsExactlyInAnyOrder(
                changedSsku(getShopSkuKey(crossdock11), ssku11.getVersionTs(), ssku11.getVersionTs())
            );

        // second update and upload
        updateVersionId(crossdock11);
        ssku11 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock11)).get(0);
        executor.execute();

        ChangedSsku updated2 = changedSskuRepository.findByShopSkuKey(ssku11.getSupplierId(), ssku11.getShopSku());
        assertThat(List.of(updated2))
            .usingElementComparatorOnFields("supplierId", "shopSku", "versionTs", "hidingUploadedVersionTs")
            .containsExactlyInAnyOrder(
                changedSsku(getShopSkuKey(crossdock11), ssku11.getVersionTs(), ssku11.getVersionTs())
            );

        // check, that timestamps are changed
        assertThat(updated.getVersionTs()).isNotEqualTo(updated2.getVersionTs());
        assertThat(updated.getHidingUploadedVersionTs()).isNotEqualTo(updated2.getHidingUploadedVersionTs());
    }

    @Test
    public void testUploadDropshipOffers() {
        updateVersionId(dropship11, dropship12);
        ChangedSsku ssku211 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(dropship11)).get(0);
        ChangedSsku ssku212 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(dropship12)).get(0);

        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(dropship12)),
            SORTING_CENTER_231.getId(), mskuAvailability(dropship12.getMskuId())
        );

        executor.execute();

        List<DataCampOffer.Offer> sentOffers = getSendOffers();
        assertThat(sentOffers)
            .containsExactlyInAnyOrder(
                dataCampOffer(ssku211, DROPSHIP_FROM_WAREHOUSE),
                dataCampOffer(ssku212, DROPSHIP_FROM_WAREHOUSE,
                    mskuAvailability(dropship12.getMskuId()))
            );

        List<ChangedSsku> all = changedSskuRepository.findAll();
        assertThat(all)
            .usingElementComparatorOnFields("supplierId", "shopSku", "versionTs", "hidingUploadedVersionTs")
            .containsExactlyInAnyOrder(
                changedSsku(getShopSkuKey(dropship11), ssku211.getVersionTs(), ssku211.getVersionTs()),
                changedSsku(getShopSkuKey(dropship12), ssku212.getVersionTs(), ssku212.getVersionTs())
            );
    }

    @Test
    public void testNotConsiderFFOffers() {
        updateVersionId(ff1, dropship11, crossdock11);
        ChangedSsku ff = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(ff1)).get(0);
        ChangedSsku dropship = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(dropship11)).get(0);
        ChangedSsku crossdock = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock11)).get(0);
        assertThat(ff.getSupplierRelationType()).isNull();
        assertThat(dropship.getSupplierRelationType()).isEqualTo(PartnerRelationType.DROPSHIP);
        assertThat(crossdock.getSupplierRelationType()).isEqualTo(PartnerRelationType.CROSSDOCK);

        executor.execute();

        List<DataCampOffer.Offer> sendOffers = getSendOffers();
        assertThat(sendOffers)
            .containsExactlyInAnyOrder(
                dataCampOffer(dropship, DROPSHIP_FROM_WAREHOUSE),
                dataCampOffer(crossdock, CROSSDOCK_FROM_WAREHOUSE)
            );
    }

    @Test
    public void testWithSeveralWarehouses() {
        deepmindSupplierRepository.save(createSupplier(42));
        deepmindSupplierRepository.save(createSupplier(43));
        partnerRelationRepository.save(
            relation(42, PartnerRelationType.CROSSDOCK, CROSSDOCK_FROM_WAREHOUSE, CROSSDOCK_SOFINO_ID),
            relation(43, PartnerRelationType.CROSSDOCK, 11L, CROSSDOCK_ROSTOV_ID)
        );

        var offer1 = serviceOfferReplicaRepository.save(createOffer(42, "offer111", 11)).get(0);
        var offer2 = serviceOfferReplicaRepository.save(createOffer(43, "offer222", 12)).get(0);
        updateVersionId(offer1, offer2);
        ChangedSsku ssku1 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(offer1)).get(0);
        ChangedSsku ssku2 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(offer2)).get(0);

        // mark ssku as have blocks
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_SOFINO_ID, mskuAvailability(11));
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(offer2)),
            CROSSDOCK_SOFINO_ID, mskuAvailability(12));

        executor.execute();

        assertThat(getSendOffers())
            .containsExactlyInAnyOrder(
                dataCampOffer(ssku1, CROSSDOCK_FROM_WAREHOUSE, mskuAvailability(11)),
                dataCampOffer(ssku2, 11L)
            );

        availabilityService.clear();
        logbrokerEventPublisherMock.clear();
        updateVersionId(offer1, offer2);
        ssku1 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(offer1)).get(0);
        ssku2 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(offer2)).get(0);
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(offer1)),
            CROSSDOCK_ROSTOV_ID, mskuAvailability(11));
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(offer2)),
            CROSSDOCK_ROSTOV_ID, mskuAvailability(12));

        executor.execute();
        assertThat(getSendOffers())
            .containsExactlyInAnyOrder(
                dataCampOffer(ssku1, CROSSDOCK_FROM_WAREHOUSE),
                dataCampOffer(ssku2, 11L, mskuAvailability(12))
            );
    }

    @Test // Залили скрытие, потом у partner_relation поменялся to склад на неизвестный и нам нужно снять скрытие
    public void testReuploadHidingIfToWarehouseIsChanged() {
        updateVersionId(dropship11, dropship12);
        ChangedSsku ssku211 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(dropship11)).get(0);
        ChangedSsku ssku212 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(dropship12)).get(0);

        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(dropship12)),
            SORTING_CENTER_231.getId(), mskuAvailability(dropship12.getMskuId())
        );

        executor.execute();

        List<DataCampOffer.Offer> sentOffers = getSendOffers();
        assertThat(sentOffers)
            .containsExactlyInAnyOrder(
                dataCampOffer(ssku211, DROPSHIP_FROM_WAREHOUSE),
                dataCampOffer(ssku212, DROPSHIP_FROM_WAREHOUSE,
                    mskuAvailability(dropship12.getMskuId()))
            );

        // меняем склад
        logbrokerEventPublisherMock.clear();
        long sortingCenter10 = 10;
        deepmindWarehouseRepository.save(new Warehouse().setId(sortingCenter10).setName("SC 10")
            .setType(WarehouseType.SORTING_CENTER));

        var partnerRelation = partnerRelationRepository.findBySupplierIdFromWarehouseId(2, DROPSHIP_FROM_WAREHOUSE);
        partnerRelation.setToWarehouseId(sortingCenter10);
        partnerRelationRepository.save(partnerRelation);

        updateVersionId(dropship11, dropship12);
        ssku211 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(dropship11)).get(0);
        ssku212 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(dropship12)).get(0);

        executor.execute();

        sentOffers = getSendOffers();
        assertThat(sentOffers)
            .containsExactlyInAnyOrder(
                dataCampOffer(ssku211, DROPSHIP_FROM_WAREHOUSE),
                dataCampOffer(ssku212, DROPSHIP_FROM_WAREHOUSE)
            );
    }

    @Test // Залили скрытие, потом удалили запись у partner_relation
    public void testReuploadHidingIfToPartnerRelationIsDeleted() {
        updateVersionId(dropship11, dropship12);
        ChangedSsku ssku211 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(dropship11)).get(0);
        ChangedSsku ssku212 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(dropship12)).get(0);

        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(dropship12)),
            SORTING_CENTER_231.getId(), mskuAvailability(dropship12.getMskuId())
        );

        executor.execute();

        List<DataCampOffer.Offer> sentOffers = getSendOffers();
        assertThat(sentOffers)
            .containsExactlyInAnyOrder(
                dataCampOffer(ssku211, DROPSHIP_FROM_WAREHOUSE),
                dataCampOffer(ssku212, DROPSHIP_FROM_WAREHOUSE,
                    mskuAvailability(dropship12.getMskuId()))
            );

        // удаляем связь
        logbrokerEventPublisherMock.clear();
        var relation = partnerRelationRepository.findBySupplierIdFromWarehouseId(2, 101L);
        partnerRelationRepository.deleteByEntities(relation);

        updateVersionId(dropship11, dropship12);

        executor.execute();

        sentOffers = getSendOffers();
        // По хорошему тут мы должны раскрыть ssku, но на момент реализации такой кейс предусмотрен не был
        // И если товары останутся скрытыми, то ничего страшного не должно произойти, так как все равно такие товары
        // не смогут продаваться
        assertThat(sentOffers).isEmpty();
    }

    @Test
    public void testDontSendDropshopOffersIfItDisabled() {
        deepmindStorageKeyValueService.putValue(UPLOAD_OFFER_HIDINGS_DROPSHIP_IS_DISABLED, true);

        updateVersionId(crossdock11, dropship11, dropship12);
        ChangedSsku ssku11 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock11)).get(0);

        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(crossdock11)),
            CROSSDOCK_SOFINO_ID, mskuAvailability(dropship12.getMskuId())
        );

        executor.execute();

        List<DataCampOffer.Offer> sentOffers = getSendOffers();
        assertThat(sentOffers).containsExactlyInAnyOrder(
            dataCampOffer(ssku11, CROSSDOCK_FROM_WAREHOUSE,
                mskuAvailability(dropship12.getMskuId()))
        );
    }

    @Test
    public void testDontSendCrossdockOffersIfItDisabled() {
        deepmindStorageKeyValueService.putValue(UPLOAD_OFFER_HIDINGS_CROSSDOCK_IS_DISABLED, true);

        updateVersionId(crossdock11, dropship12);
        ChangedSsku ssku211 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(dropship12)).get(0);

        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(dropship12)),
            SORTING_CENTER_231.getId(), mskuAvailability(dropship12.getMskuId())
        );

        executor.execute();

        List<DataCampOffer.Offer> sentOffers = getSendOffers();
        assertThat(sentOffers).containsExactlyInAnyOrder(
            dataCampOffer(ssku211, DROPSHIP_FROM_WAREHOUSE,
                mskuAvailability(dropship12.getMskuId()))
        );
    }

    @Test
    public void testNotUploadOffersWithoutMappings() {
        updateVersionId(dropship12);
        ChangedSsku ssku212 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(dropship12)).get(0);

        serviceOfferReplicaRepository.delete(getShopSkuKey(dropship12));
        executor.execute();

        List<DataCampOffer.Offer> sentOffers = getSendOffers();
        assertThat(sentOffers).isEmpty();

        List<ChangedSsku> all = changedSskuRepository.findAll();
        assertThat(all)
            .usingElementComparatorOnFields("supplierId", "shopSku", "versionTs", "hidingUploadedVersionTs")
            .containsExactlyInAnyOrder(
                changedSsku(getShopSkuKey(dropship12), ssku212.getVersionTs(), ssku212.getVersionTs())
                    .setHidingUploadedStatus(HidingStatus.NOT_HIDDEN)
            );
    }

    @Test
    public void testUploadOfferWithBizId() {
        deepmindSupplierRepository.save(createSupplier(42));
        partnerRelationRepository.save(
            relation(42, PartnerRelationType.CROSSDOCK, CROSSDOCK_FROM_WAREHOUSE, CROSSDOCK_SOFINO_ID)
        );
        ServiceOfferReplica offer = serviceOfferReplicaRepository.save(
            createOffer(2, "offer-2-42", 12).setBusinessId(42)).get(0);

        updateVersionId(offer);
        ChangedSsku ssku = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(offer)).get(0);

        executor.execute();

        List<DataCampOffer.Offer> sentOffers = getSendOffers();
        assertThat(sentOffers).containsExactlyInAnyOrder(
            dataCampOffer(ssku, DROPSHIP_FROM_WAREHOUSE, 42)
        );

        List<ChangedSsku> all = changedSskuRepository.findAll();
        assertThat(all)
            .usingElementComparatorOnFields("supplierId", "shopSku", "versionTs")
            .containsExactlyInAnyOrder(
                changedSsku(getShopSkuKey(offer), ssku.getVersionTs(), ssku.getVersionTs())
                    .setHidingUploadedStatus(HidingStatus.NOT_HIDDEN)
            );
    }

    @Test
    public void testUploadIsStoppedIfCountOfHiddenOffersBecomeMoreThanMax() {
        deepmindStorageKeyValueService.putValue(UPLOAD_OFFER_HIDINGS_MAX_LIMIT_HIDDEN_OFFERS, 2);
        executor.setBatchSize(3);

        updateVersionId(crossdock11, crossdock21, dropship11, dropship12);
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(crossdock11)),
            CROSSDOCK_SOFINO_ID, mskuInCategory());
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(crossdock21)),
            CROSSDOCK_SOFINO_ID, mskuAvailability(21));
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(dropship11)),
            SORTING_CENTER_231.getId(), mskuAvailability(11));
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(dropship12)),
            SORTING_CENTER_231.getId(), mskuAvailability(12));

        Assertions.assertThatThrownBy(() -> executor.execute())
            .hasMessage("MBO-26867 UploadOfferHidingsToLogbrokerExecutor is stopped, " +
                "because hidden count (3) is greater then max limit (2)");

        List<DataCampOffer.Offer> sendOffers = getSendOffers();
        assertThat(sendOffers)
            .containsExactlyInAnyOrder(
                dataCampOffer(crossdock11, CROSSDOCK_FROM_WAREHOUSE, mskuInCategory()),
                dataCampOffer(crossdock21, CROSSDOCK_FROM_WAREHOUSE, mskuAvailability(21)),
                dataCampOffer(dropship11, DROPSHIP_FROM_WAREHOUSE, mskuAvailability(11))
            );

        // one more run, zero new offer hidings expected to upload
        logbrokerEventPublisherMock.clear();
        Assertions.assertThatThrownBy(() -> executor.execute())
            .hasMessage("MBO-26867 UploadOfferHidingsToLogbrokerExecutor is stopped, " +
                "because hidden count (3) is greater then max limit (2)");

        sendOffers = getSendOffers();
        assertThat(sendOffers).isEmpty();
    }

    @Test
    public void testIfOfferWasHiddenAndWeUploadItAgainExecutorWontFail() {
        deepmindStorageKeyValueService.putValue(UPLOAD_OFFER_HIDINGS_MAX_LIMIT_HIDDEN_OFFERS, 1);

        updateVersionId(crossdock11, crossdock21);
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(crossdock11)),
            CROSSDOCK_SOFINO_ID, mskuInCategory());

        executor.execute(); // upload hidden offer first time

        List<DataCampOffer.Offer> sendOffers = getSendOffers();
        assertThat(sendOffers)
            .containsExactlyInAnyOrder(
                dataCampOffer(crossdock11, CROSSDOCK_FROM_WAREHOUSE, mskuInCategory()),
                dataCampOffer(crossdock21, CROSSDOCK_FROM_WAREHOUSE)
            );

        logbrokerEventPublisherMock.clear();
        // mark & upload second time
        updateVersionId(crossdock11, crossdock21);
        executor.execute(); // upload hidden offer second time

        List<DataCampOffer.Offer> sendOffers2 = getSendOffers();
        assertThat(sendOffers2)
            .containsExactlyInAnyOrder(
                dataCampOffer(crossdock11, CROSSDOCK_FROM_WAREHOUSE, mskuInCategory()),
                dataCampOffer(crossdock21, CROSSDOCK_FROM_WAREHOUSE)
            );
    }

    @Test
    public void testWontFailIfBeforeUploadSomeOffersWasHidden() {
        deepmindStorageKeyValueService.putValue(UPLOAD_OFFER_HIDINGS_MAX_LIMIT_HIDDEN_OFFERS, 2);

        // mark some offers as hidden
        updateVersionId(crossdock11, crossdock21);
        var keys = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(crossdock11), getShopSkuKey(crossdock21))
            .stream()
            .peek(c -> {
                c.setHidingUploadedStatus(HidingStatus.HIDDEN);
                c.setHidingUploadedVersionTs(c.getVersionTs());
            }).collect(Collectors.toList());
        changedSskuRepository.updateHidingUploadedVersionTsIfMatch(keys, Instant.now(), "");

        // add availabilities
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(dropship11)),
            SORTING_CENTER_231.getId(), mskuAvailability(11));
        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(dropship12)),
            SORTING_CENTER_231.getId(), mskuAvailability(12));

        // mark to reupload
        updateVersionId(crossdock11, crossdock21, dropship11, dropship12);

        // Логика тут проста. Эмулируем ситуацию, когда было 2 оффера скрыто
        // их раскрыли
        // и скрыли другие 2
        // получается, что итого снова будет 2 скрыто. Т.е. max limit мы не привысили, а значит и джоба не должна упасть
        executor.execute();

        List<DataCampOffer.Offer> sendOffers = getSendOffers();
        assertThat(sendOffers)
            .containsExactlyInAnyOrder(
                dataCampOffer(crossdock11, CROSSDOCK_FROM_WAREHOUSE),
                dataCampOffer(crossdock21, CROSSDOCK_FROM_WAREHOUSE),
                dataCampOffer(dropship11, DROPSHIP_FROM_WAREHOUSE, mskuAvailability(11)),
                dataCampOffer(dropship12, DROPSHIP_FROM_WAREHOUSE, mskuAvailability(12))
            );
    }

    @Test
    public void testUploadDropshipOffersWithSeveralFromWarehouses() {
        var partnerRelation = partnerRelationRepository.findBySupplierIdFromWarehouseId(2, DROPSHIP_FROM_WAREHOUSE);
        partnerRelationRepository.save(partnerRelation.setFromWarehouseIds(DROPSHIP_FROM_WAREHOUSE, 102L));

        updateVersionId(dropship11, dropship12);
        ChangedSsku ssku211 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(dropship11)).get(0);
        ChangedSsku ssku212 = changedSskuRepository.findByShopSkuKeys(getShopSkuKey(dropship12)).get(0);

        availabilityService.addAvailability(serviceOfferReplicaRepository.findOfferByKey(getShopSkuKey(dropship12)),
            SORTING_CENTER_231.getId(), mskuAvailability(dropship12.getMskuId())
        );

        executor.execute();

        List<DataCampOffer.Offer> sentOffers = getSendOffers();
        assertThat(sentOffers)
            .containsExactlyInAnyOrder(
                dataCampOffer(ssku211, DROPSHIP_FROM_WAREHOUSE),
                dataCampOffer(ssku211, 102L),
                dataCampOffer(ssku212, DROPSHIP_FROM_WAREHOUSE,
                  mskuAvailability(dropship12.getMskuId())),
                dataCampOffer(ssku212, 102L,
                    mskuAvailability(dropship12.getMskuId()))
            );

        List<ChangedSsku> all = changedSskuRepository.findAll();
        assertThat(all)
            .usingElementComparatorOnFields("supplierId", "shopSku", "versionTs", "hidingUploadedVersionTs")
            .containsExactlyInAnyOrder(
                changedSsku(getShopSkuKey(dropship11), ssku211.getVersionTs(), ssku211.getVersionTs()),
                changedSsku(getShopSkuKey(dropship12), ssku212.getVersionTs(), ssku212.getVersionTs())
            );
    }

    private Supplier createSupplier(int supplierId) {
        return new Supplier().setId(supplierId).setName("Supplier: " + supplierId);
    }

    private ServiceOfferReplica createOffer(
        int supplierId, String shopSku, long msku) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setTitle("Offer: " + shopSku)
            .setCategoryId(42L)
            .setSeqId(0L)
            .setMskuId(msku)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private ChangedSsku changedSsku(ServiceOfferKey key, Instant versionTs, Instant hidingUploadedVersionTs) {
        return new ChangedSsku()
            .setSupplierId(key.getSupplierId())
            .setShopSku(key.getShopSku())
            .setVersionTs(versionTs)
            .setHidingUploadedVersionTs(hidingUploadedVersionTs);
    }

    private List<DataCampOffer.Offer> getSendOffers() {
        return logbrokerEventPublisherMock.getSendEvents().stream()
            .map(v -> v.getPayload())
            .flatMap(v -> v.getOffersList().stream())
            .flatMap(v -> v.getOfferList().stream())
            .collect(Collectors.toList());
    }

    private DataCampOffer.Offer dataCampOffer(ServiceOfferReplica offer, long fromWarehouseId,
                                              MatrixAvailability... availabilities) {
        var changedSsku = changedSskuRepository.findByShopSkuKey(offer.getBusinessId(), offer.getShopSku());
        return dataCampOffer(changedSsku, fromWarehouseId, offer.getBusinessId(), availabilities);
    }

    private DataCampOffer.Offer dataCampOffer(ChangedSsku changedSsku, long fromWarehouseId,
                                              MatrixAvailability... availabilities) {
        return dataCampOffer(changedSsku, fromWarehouseId, changedSsku.getSupplierId(), availabilities);
    }

    private DataCampOffer.Offer dataCampOffer(ChangedSsku changedSsku,
                                              long fromWarehouseId,
                                              int businessId,
                                              MatrixAvailability... availabilities) {
        var partnerRelation = partnerRelationRepository
            .findBySupplierIdFromWarehouseId(changedSsku.getSupplierId(), fromWarehouseId);
        return DataCampOfferUtils.createDatacampOffer(changedSsku, fromWarehouseId,
            Arrays.asList(availabilities), partnerRelation.getRelationType(), businessId);
    }

    private void updateVersionId(ServiceOfferReplica... offers) {
        List<ServiceOfferKey> keys = Arrays.stream(offers)
            .map(o -> new ServiceOfferKey(o.getSupplierId(), o.getShopSku())).collect(Collectors.toList());
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

    private MatrixAvailability mskuAvailability(long mskuId) {
        var msku = new Msku().setId(mskuId).setTitle("test").setCategoryId(42L);
        return MatrixAvailabilityUtils.mskuInWarehouse(false, msku,
            CROSSDOCK_SOFINO_ID, "crossdock sofino", null, null, null);
    }

    private MatrixAvailability mskuInCategory() {
        return MatrixAvailabilityUtils.mskuInCategory(CROSSDOCK_SOFINO_ID, "crossdock sofino", 42L, "test", null);
    }

    private PartnerRelation relation(int supplierId, PartnerRelationType type, long fromWhId, @Nullable Long toWhId) {
        return new PartnerRelation()
            .setSupplierId(supplierId)
            .setRelationType(type)
            .setFromWarehouseIds(fromWhId)
            .setToWarehouseId(toWhId);
    }

    private ServiceOfferKey getShopSkuKey(ServiceOfferReplica offer) {
        return new ServiceOfferKey(offer.getSupplierId(), offer.getShopSku());
    }
}
