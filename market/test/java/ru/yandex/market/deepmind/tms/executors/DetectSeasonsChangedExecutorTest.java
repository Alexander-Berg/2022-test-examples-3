package ru.yandex.market.deepmind.tms.executors;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.task_queue.events.SeasonAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.SeasonAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SeasonPeriod;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository.SeasonWithPeriods;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository
    .CROSSDOCK_SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;
import static ru.yandex.market.deepmind.common.repository.season.SeasonRepository.DEFAULT_ID;


public class DetectSeasonsChangedExecutorTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {

    @Resource
    private StorageKeyValueService deepmindStorageKeyValueService;
    @Resource
    private SeasonRepository seasonRepository;
    @Resource
    private SeasonAvailabilityChangedHandler seasonAvailabilityChangedHandler;

    private DetectSeasonsChangedExecutor executor;

    @Before
    public void setUp() {
        executor = new DetectSeasonsChangedExecutor(
            deepmindStorageKeyValueService,
            seasonRepository,
            seasonAvailabilityChangedHandler
        );
    }

    @Test
    public void testDetect() {
        // 25 May -> 1 June
        var season = seasonRepository.saveWithPeriodsAndReturn(season("season", DEFAULT_ID, "05-25", "06-01"));
        taskQueueRepository.deleteAll();

        Assertions.assertThat(runAtDay("2020-05-24")).isEmpty();
        Assertions.assertThat(runAtDay("2020-05-25")).containsExactlyInAnyOrder(changedTask(season.getId()));
        Assertions.assertThat(runAtDay("2020-05-26")).isEmpty();
        Assertions.assertThat(runAtDay("2020-05-31")).isEmpty();
        Assertions.assertThat(runAtDay("2020-06-01")).isEmpty();
        Assertions.assertThat(runAtDay("2020-06-02")).containsExactlyInAnyOrder(changedTask(season.getId()));
        Assertions.assertThat(runAtDay("2020-06-03")).isEmpty();
    }

    @Test
    public void testDetectWithDoublePeriod() {
        // 10 Jan -> 10 February, 1 September -> 31 December
        var season1 = seasonRepository.saveWithPeriodsAndReturn(season("season#1",
            DEFAULT_ID, "01-10", "02-10",
            TOMILINO_ID, "09-01", "12-31")
        );
        // 11 Feb -> 12 Feb
        var season2 = seasonRepository.saveWithPeriodsAndReturn(season("season#2",
            SOFINO_ID, "02-11", "02-12")
        );
        taskQueueRepository.deleteAll();

        // Jan
        Assertions.assertThat(runAtDay("2020-01-09")).isEmpty();
        Assertions.assertThat(runAtDay("2020-01-10")).containsExactlyInAnyOrder(changedTask(season1.getId()));
        Assertions.assertThat(runAtDay("2020-01-11")).isEmpty();

        // Feb
        Assertions.assertThat(runAtDay("2020-02-09")).isEmpty();
        Assertions.assertThat(runAtDay("2020-02-10")).isEmpty();
        Assertions.assertThat(runAtDay("2020-02-11")).containsExactlyInAnyOrder(
            changedTask(season1.getId()), changedTask(season2.getId())
        );
        Assertions.assertThat(runAtDay("2020-02-12")).isEmpty();
        Assertions.assertThat(runAtDay("2020-02-13")).containsExactlyInAnyOrder(changedTask(season2.getId()));
        Assertions.assertThat(runAtDay("2020-02-14")).isEmpty();

        // Sep
        Assertions.assertThat(runAtDay("2020-09-01")).containsExactlyInAnyOrder(changedTask(season1.getId()));
        Assertions.assertThat(runAtDay("2020-09-02")).isEmpty();

        // Dec
        Assertions.assertThat(runAtDay("2020-12-30")).isEmpty();
        Assertions.assertThat(runAtDay("2020-12-31")).isEmpty();
        Assertions.assertThat(runAtDay("2021-01-01")).containsExactlyInAnyOrder(changedTask(season1.getId()));
        Assertions.assertThat(runAtDay("2021-01-02")).isEmpty();

        // Years
        Assertions.assertThat(runAtDay("2019-01-01")).containsExactlyInAnyOrder(changedTask(season1.getId()));
        Assertions.assertThat(runAtDay("2020-01-01")).containsExactlyInAnyOrder(changedTask(season1.getId()));
        Assertions.assertThat(runAtDay("2021-01-01")).containsExactlyInAnyOrder(changedTask(season1.getId()));
        Assertions.assertThat(runAtDay("2022-01-01")).containsExactlyInAnyOrder(changedTask(season1.getId()));
    }

    @Test
    public void test29FebFrom() {
        // 29 Feb -> 15 March
        var season = seasonRepository.saveWithPeriodsAndReturn(season("season",
            CROSSDOCK_SOFINO_ID, "02-29", "03-15"
        ));
        taskQueueRepository.deleteAll();

        Assertions.assertThat(runAtDay("2019-02-27")).isEmpty();
        Assertions.assertThat(runAtDay("2019-02-28")).containsExactlyInAnyOrder(changedTask(season.getId()));
        Assertions.assertThat(runAtDay("2019-03-01")).isEmpty();

        Assertions.assertThat(runAtDay("2020-02-27")).isEmpty();
        Assertions.assertThat(runAtDay("2020-02-28")).isEmpty();
        Assertions.assertThat(runAtDay("2020-02-29")).containsExactlyInAnyOrder(changedTask(season.getId()));
        Assertions.assertThat(runAtDay("2020-03-01")).isEmpty();

        Assertions.assertThat(runAtDay("2021-02-27")).isEmpty();
        Assertions.assertThat(runAtDay("2021-02-28")).containsExactlyInAnyOrder(changedTask(season.getId()));
        Assertions.assertThat(runAtDay("2021-03-01")).isEmpty();
    }

    @Test
    public void test29FebTo() {
        // 15 Feb -> 29 Feb
        var season = seasonRepository.saveWithPeriodsAndReturn(season("season",
            CROSSDOCK_SOFINO_ID, "01-15", "02-29"
        ));
        taskQueueRepository.deleteAll();

        Assertions.assertThat(runAtDay("2019-02-28")).isEmpty();
        Assertions.assertThat(runAtDay("2019-03-01")).containsExactlyInAnyOrder(changedTask(season.getId()));
        Assertions.assertThat(runAtDay("2019-03-02")).isEmpty();

        Assertions.assertThat(runAtDay("2020-02-28")).isEmpty();
        Assertions.assertThat(runAtDay("2020-02-29")).isEmpty();
        Assertions.assertThat(runAtDay("2020-03-01")).containsExactlyInAnyOrder(changedTask(season.getId()));
        Assertions.assertThat(runAtDay("2019-03-02")).isEmpty();

        Assertions.assertThat(runAtDay("2021-02-28")).isEmpty();
        Assertions.assertThat(runAtDay("2021-03-01")).containsExactlyInAnyOrder(changedTask(season.getId()));
        Assertions.assertThat(runAtDay("2021-03-02")).isEmpty();
    }

    @Test
    public void test28FebFrom() {
        // 28 Feb -> 15 March
        var season = seasonRepository.saveWithPeriodsAndReturn(season("season",
            CROSSDOCK_SOFINO_ID, "02-28", "03-15"
        ));
        taskQueueRepository.deleteAll();

        Assertions.assertThat(runAtDay("2019-02-27")).isEmpty();
        Assertions.assertThat(runAtDay("2019-02-28")).containsExactlyInAnyOrder(changedTask(season.getId()));
        Assertions.assertThat(runAtDay("2019-03-01")).isEmpty();

        Assertions.assertThat(runAtDay("2020-02-27")).isEmpty();
        Assertions.assertThat(runAtDay("2020-02-28")).containsExactlyInAnyOrder(changedTask(season.getId()));
        Assertions.assertThat(runAtDay("2020-02-29")).isEmpty();
        Assertions.assertThat(runAtDay("2020-03-01")).isEmpty();

        Assertions.assertThat(runAtDay("2021-02-27")).isEmpty();
        Assertions.assertThat(runAtDay("2021-02-28")).containsExactlyInAnyOrder(changedTask(season.getId()));
        Assertions.assertThat(runAtDay("2021-03-01")).isEmpty();
    }

    @Test
    public void test28FebTo() {
        // 15 Feb -> 28 Feb
        var season = seasonRepository.saveWithPeriodsAndReturn(season("season",
            CROSSDOCK_SOFINO_ID, "01-15", "02-28"
        ));
        taskQueueRepository.deleteAll();

        Assertions.assertThat(runAtDay("2019-02-28")).isEmpty();
        Assertions.assertThat(runAtDay("2019-03-01")).containsExactlyInAnyOrder(changedTask(season.getId()));
        Assertions.assertThat(runAtDay("2019-03-02")).isEmpty();

        Assertions.assertThat(runAtDay("2020-02-28")).isEmpty();
        Assertions.assertThat(runAtDay("2020-02-29")).containsExactlyInAnyOrder(changedTask(season.getId()));
        Assertions.assertThat(runAtDay("2020-03-01")).isEmpty();
        Assertions.assertThat(runAtDay("2019-03-02")).isEmpty();

        Assertions.assertThat(runAtDay("2021-02-28")).isEmpty();
        Assertions.assertThat(runAtDay("2021-03-01")).containsExactlyInAnyOrder(changedTask(season.getId()));
        Assertions.assertThat(runAtDay("2021-03-02")).isEmpty();
    }

    @Test
    public void testDoubleRunWontFail() {
        executor.execute();
        executor.execute();
    }

    private List<SeasonAvailabilityChangedTask> runAtDay(String day) {
        try {
            Instant now = LocalDate.parse(day).atStartOfDay(ZoneOffset.UTC).toInstant();
            Clock clock = Clock.fixed(now, ZoneOffset.UTC);

            executor.setClock(clock);
            executor.execute();
            return getQueueTasks();
        } finally {
            executor.setClock(Clock.systemDefaultZone());
            taskQueueRepository.deleteAll();
        }
    }

    public SeasonWithPeriods season(String name, long warehouseId, String from, String to) {
        return new SeasonWithPeriods(
            new Season().setName(name),
            List.of(new SeasonPeriod().setWarehouseId(warehouseId)
                .setFromMmDd("01-01")
                .setDeliveryFromMmDd(from)
                .setToMmDd("02-02")
                .setDeliveryToMmDd(to)
            )
        );
    }

    public SeasonWithPeriods season(String name,
                                    long warehouseId1, String from1, String to1,
                                    long warehouseId2, String from2, String to2) {
        return new SeasonWithPeriods(
            new Season().setName(name),
            List.of(
                new SeasonPeriod().setWarehouseId(warehouseId1)
                    .setFromMmDd("01-01")
                    .setDeliveryFromMmDd(from1)
                    .setToMmDd("02-02")
                    .setDeliveryToMmDd(to1),
                new SeasonPeriod().setWarehouseId(warehouseId2)
                    .setFromMmDd("03-03")
                    .setDeliveryFromMmDd(from2)
                    .setToMmDd("04-04")
                    .setDeliveryToMmDd(to2)
            )
        );
    }

    public SeasonAvailabilityChangedTask changedTask(Long id) {
        return new SeasonAvailabilityChangedTask(id, "", "", Instant.now());
    }
}
