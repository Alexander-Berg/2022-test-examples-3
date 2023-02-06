package ru.yandex.market.deepmind.tms.services;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.jooq.DSLContext;
import org.jooq.impl.TableRecordImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.availability.task_queue.events.ShopSkuAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.ShopSkuAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.Tables;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.HidingReasonType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.ChangedSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Hiding;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.HidingReasonDescription;
import ru.yandex.market.deepmind.common.hiding.HidingReason;
import ru.yandex.market.deepmind.common.hiding.HidingReasonDescriptionRepository;
import ru.yandex.market.deepmind.common.pojo.ServiceOfferKey;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.WarehouseInstancesForTesting;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.mbo.taskqueue.TaskQueueTask;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of {@link ShopSkuAvailabilityChangedHandler}.
 */
public class ShopSkuAvailabilityChangedHandlerTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource(name = "deepmindDsl")
    private DSLContext dslContext;
    @Resource
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Resource
    private HidingReasonDescriptionRepository hidingReasonDescriptionRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    private DeepmindWarehouseRepository deepmindWarehouseRepository;

    private SyncSskuAvailabilityMatrixService syncSskuAvailabilityMatrixService;
    private ShopSkuAvailabilityChangedHandler shopSkuAvailabilityChangedHandler;
    private Long reasonKeyId;

    @Before
    public void setUp() {
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        serviceOfferReplicaRepository.save(YamlTestUtil.readOffersFromResources("availability/offers.yml"));
        deepmindWarehouseRepository.save(WarehouseInstancesForTesting.ALL_FULFILLMENT);

        shopSkuAvailabilityChangedHandler = new ShopSkuAvailabilityChangedHandler(
            changedSskuRepository,
            taskQueueRegistrator
        );

        syncSskuAvailabilityMatrixService = new SyncSskuAvailabilityMatrixService(
            namedParameterJdbcTemplate, shopSkuAvailabilityChangedHandler, hidingReasonDescriptionRepository
        );
        clearQueue();
        hidingReasonDescriptionRepository.save(new HidingReasonDescription()
            .setReasonKey(HidingReason.ABO_LEGAL_SUBREASON.toReasonKey())
            .setType(HidingReasonType.REASON_KEY)
            .setExtendedDesc("").setReplaceWithDesc(""));
        reasonKeyId = hidingReasonDescriptionRepository.save(new HidingReasonDescription()
            .setReasonKey(HidingReason.SKK_45K_SUBREASON.toReasonKey())
            .setType(HidingReasonType.REASON_KEY)
            .setExtendedDesc("").setReplaceWithDesc("")).getId();
    }

    @Test
    public void testRegisterAndHandleResult() {
        shopSkuAvailabilityChangedHandler.registerShopSkuChanges(
            List.of(
                new ServiceOfferKey(42, "sku1"),
                new ServiceOfferKey(42, "sku2"),
                new ServiceOfferKey(60, "sku4"),
                new ServiceOfferKey(77, "sku5")
            ), MatrixAvailability.Reason.OFFER_DELISTED.name(),
            "unit-test", Instant.now()
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(60, "sku4"),
                changedSsku(77, "sku5")
            );
    }

    @Test
    public void testRegisterEventsFromAutoAvailability() {
        Hiding ass = createHiding("жопа", new ServiceOfferKey(60, "sku4"));
        Hiding drugs = createHiding("наркотики", new ServiceOfferKey(60, "sku4"));
        insertHidings(ass, drugs);
        syncSskuAvailabilityMatrixService.syncHidingsAvailabilities();

        List<TaskQueueTask> queueTasks = getQueueTasks();
        assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                new ShopSkuAvailabilityChangedTask(
                    Set.of(
                        new ServiceOfferKey(60, "sku4")
                    ),
                    MatrixAvailability.Reason.SSKU.name(),
                    SyncSskuAvailabilityMatrixService.class.getName(),
                    Instant.now())
            );
    }

    @Test
    public void testRegisterUnexistingSupplier() {
        shopSkuAvailabilityChangedHandler.registerShopSkuChanges(
            List.of(
                new ServiceOfferKey(123456789, "sku")
            ), MatrixAvailability.Reason.OFFER_DELISTED.name(),
            "unit-test", Instant.now()
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        assertThat(changedSskus).isEmpty();
    }

    private void insertHidings(Hiding... hidings) {
        Arrays.stream(hidings)
            .map(hiding -> dslContext.newRecord(Tables.HIDING, hiding))
            .forEach(TableRecordImpl::insert);
    }

    private Hiding createHiding(String subreasonId, ServiceOfferKey shopSkuKey) {
        return new Hiding()
            .setReasonKeyId(reasonKeyId)
            .setSubreasonId(subreasonId)
            .setSupplierId(shopSkuKey.getSupplierId())
            .setShopSku(shopSkuKey.getShopSku());
    }
}
