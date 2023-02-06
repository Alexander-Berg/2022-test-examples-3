package ru.yandex.market.deepmind.common.services.task_queue.handlers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.matrix.MatrixAvailability;
import ru.yandex.market.deepmind.common.availability.task_queue.events.MskuAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.MskuStatusAvailabilityChangedObserver;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.ChangedSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.ssku.ChangedSskuRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.mbo.taskqueue.TaskQueueTask;

/**
 * Tests of {@link MskuStatusAvailabilityChangedObserver}.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MskuStatusAvailabilityChangedObserverTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    private static final long MSKU_404040 = 404040;
    private static final long MSKU_505050 = 505050;
    private static final long MSKU_100000 = 100000;

    @Resource
    private MskuStatusRepository mskuStatusRepository;
    @Resource
    private SeasonRepository seasonRepository;
    @Resource
    private ChangedSskuRepository changedSskuRepository;
    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;

    public static MskuStatus mskuStatus(long mskuId, MskuStatusValue mskuStatusValue) {
        return new MskuStatus().setMarketSkuId(mskuId).setMskuStatus(mskuStatusValue);
    }

    public static MskuAvailabilityChangedTask changedTask(long mskuId, MatrixAvailability.Reason reason) {
        return new MskuAvailabilityChangedTask(Set.of(mskuId), reason.name(), "", Instant.now());
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
        Season season = seasonRepository.save(new Season().setName("season#1"));

        mskuStatusRepository.save(
            mskuStatus(MSKU_404040, MskuStatusValue.SEASONAL).setSeasonId(season.getId()),
            mskuStatus(MSKU_505050, MskuStatusValue.ARCHIVE)
        );

        List<MskuAvailabilityChangedTask> queueTasks = getQueueTasksOfType(MskuAvailabilityChangedTask.class);
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(MSKU_404040, MatrixAvailability.Reason.MSKU_IN_SEASON),
            changedTask(MSKU_505050, MatrixAvailability.Reason.MSKU_ARCHIVED)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(60, "sku4"),
                changedSsku(77, "sku5")
            );
    }

    @Test
    public void testSetEmpty() {
        // previously create
        MskuStatus mskuStatus = mskuStatusRepository.save(mskuStatus(MSKU_404040, MskuStatusValue.END_OF_LIFE));
        taskQueueRepository.deleteAll();

        // then set empty
        mskuStatusRepository.save(mskuStatus.setMskuStatus(MskuStatusValue.EMPTY));

        // Check only unique rows in queue
        List<MskuAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).containsExactlyInAnyOrder(
            changedTask(MSKU_404040, MatrixAvailability.Reason.MSKU_END_OF_LIFE)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(60, "sku4")
            );
    }

    @Test
    public void testSingleTaskEventIfStatusIsChangedFromTwoStatuses() {
        // сохраняем с одним статусом
        MskuStatus mskuStatus = mskuStatusRepository.save(mskuStatus(MSKU_404040, MskuStatusValue.END_OF_LIFE));
        Assertions.assertThat(getQueueTasks()).containsExactlyInAnyOrder(
            changedTask(MSKU_404040, MatrixAvailability.Reason.MSKU_END_OF_LIFE)
        );
        taskQueueRepository.deleteAll();

        // меняем статус и пересохраняем
        mskuStatusRepository.save(mskuStatus.setMskuStatus(MskuStatusValue.ARCHIVE));

        // проверяем, что в очереди только одно событие
        Assertions.assertThat(getQueueTasks()).containsExactlyInAnyOrder(
            changedTask(MSKU_404040, MatrixAvailability.Reason.MSKU_ARCHIVED)
        );

        execute();

        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(60, "sku4")
            );
    }

    @Test
    public void testSomeStatusesDoNotCreateQueueEvents() {
        mskuStatusRepository.save(mskuStatus(MSKU_404040, MskuStatusValue.REGULAR));
        mskuStatusRepository.save(mskuStatus(MSKU_505050, MskuStatusValue.IN_OUT));
        mskuStatusRepository.save(mskuStatus(MSKU_100000, MskuStatusValue.NPD).setNpdStartDate(LocalDate.now()));

        List<TaskQueueTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks).isEmpty();
    }
}
