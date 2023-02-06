package ru.yandex.market.deepmind.common.services.task_queue.handlers;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.task_queue.events.SeasonAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.SeasonAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.ChangedSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonPeriod;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;

import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.common.services.task_queue.handlers.MskuStatusAvailabilityChangedObserverTest.mskuStatus;


/**
 * Tests of {@link SeasonAvailabilityChangedHandler}.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SeasonAvailabilityChangedHandlerTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    private static final long MSKU_404040 = 404040;

    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private SeasonRepository seasonRepository;
    @Resource
    private MskuStatusRepository mskuStatusRepository;

    public static SeasonAvailabilityChangedTask changedTask(long seasonId) {
        return new SeasonAvailabilityChangedTask(seasonId, "", "", Instant.now());
    }

    @Before
    public void setUp() {
        deepmindSupplierRepository.save(YamlTestUtil.readSuppliersFromResource("availability/suppliers.yml"));
        var offers = YamlTestUtil.readOffersFromResources("availability/offers.yml");
        Map<Long, Msku> mskus = new HashMap<>();
        offers.forEach(offer -> {
            mskus.put(offer.getMskuId(), TestUtils.newMsku(offer.getMskuId(), 1));
        });
        serviceOfferReplicaRepository.save(offers);
        deepmindMskuRepository.save(mskus.values());
        clearQueue();
    }

    @Test
    public void testSave() {
        Season season1 = seasonRepository.save(new Season().setName("season#1"));
        Season season2 = seasonRepository.save(new Season().setName("season#2"));

        List<SeasonAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(season1.getId()),
            changedTask(season2.getId())
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus).isEmpty();
    }

    @Test
    public void testDelete() {
        Season season = seasonRepository.save(new Season().setName("season#1"));
        taskQueueRepository.deleteAll();

        seasonRepository.delete(season.getId());

        List<SeasonAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(season.getId())
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus).isEmpty();
    }

    @Test
    public void testUpdate() {
        Season season1 = seasonRepository.save(new Season().setName("with ssku mappings"));
        Season season2 = seasonRepository.save(new Season().setName("with msku"));
        Season season3 = seasonRepository.save(new Season().setName("not connected to anything"));

        Msku mskuWithoutMappings = deepmindMskuRepository.save(TestUtils.newMsku(25483L, 1));

        mskuStatusRepository.save(
            mskuStatus(MSKU_404040, MskuStatusValue.SEASONAL).setSeasonId(season1.getId()),
            mskuStatus(mskuWithoutMappings.getId(), MskuStatusValue.SEASONAL).setSeasonId(season2.getId())
        );
        taskQueueRepository.deleteAll();

        // update periods
        SeasonRepository.SeasonWithPeriods save1 = new SeasonRepository.SeasonWithPeriods(season1, List.of(
            new SeasonPeriod()
                .setWarehouseId(TOMILINO_ID)
                .setFromMmDd("12-01")
                .setToMmDd("12-31")
                .setDeliveryFromMmDd("12-02")
                .setDeliveryToMmDd("12-30")
        ));
        SeasonRepository.SeasonWithPeriods save2 = new SeasonRepository.SeasonWithPeriods(season2, List.of(
            new SeasonPeriod()
                .setWarehouseId(TOMILINO_ID)
                .setFromMmDd("01-01")
                .setToMmDd("01-01")
                .setDeliveryFromMmDd("01-01")
                .setDeliveryToMmDd("01-01")
        ));
        SeasonRepository.SeasonWithPeriods save3 = new SeasonRepository.SeasonWithPeriods(season3, List.of(
            new SeasonPeriod()
                .setWarehouseId(TOMILINO_ID)
                .setFromMmDd("12-12")
                .setToMmDd("12-12")
                .setDeliveryFromMmDd("12-12")
                .setDeliveryToMmDd("12-12")
        ));
        seasonRepository.saveWithPeriods(save1);
        seasonRepository.saveWithPeriods(save2);
        seasonRepository.saveWithPeriods(save3);

        List<SeasonAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(season1.getId()),
            changedTask(season2.getId()),
            changedTask(season3.getId())
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(60, "sku4")
            );
    }
}
