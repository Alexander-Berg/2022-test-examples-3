package ru.yandex.market.deepmind.tms.executors;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.task_queue.events.MskuAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.MskuAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Msku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.MskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.TOMILINO_ID;

public class MskuPeriodChangedExecutorTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {

    @Resource
    private StorageKeyValueService deepmindStorageKeyValueService;
    @Resource
    private MskuAvailabilityMatrixRepository mskuAvailabilityMatrixRepository;
    @Resource
    private MskuAvailabilityChangedHandler mskuAvailabilityChangedHandler;
    @Resource
    private MskuRepository deepmindMskuRepository;

    private MskuPeriodChangedExecutor executor;

    @Before
    public void setUp() {
        executor = new MskuPeriodChangedExecutor(
            deepmindStorageKeyValueService,
            mskuAvailabilityMatrixRepository,
            mskuAvailabilityChangedHandler
        );
    }

    @Test
    public void testPeriodStart() {
        deepmindMskuRepository.save(
            testMsku(123L)
        );
        mskuAvailabilityMatrixRepository.save(
            new MskuAvailabilityMatrix()
                .setMarketSkuId(123L)
                .setFromDate(LocalDate.parse("2020-01-15"))
                .setWarehouseId(ROSTOV_ID)
        );
        taskQueueRepository.deleteAll();

        assertThat(runAtDay("2020-01-14")).isEmpty();
        assertThat(runAtDay("2020-01-15")).containsExactly(changedTask(123L));
        assertThat(runAtDay("2020-01-16")).isEmpty();
    }

    @Test
    public void testPeriodEnd() {
        deepmindMskuRepository.save(
            testMsku(123L)
        );
        mskuAvailabilityMatrixRepository.save(
            new MskuAvailabilityMatrix()
                .setMarketSkuId(123L)
                .setToDate(LocalDate.parse("2020-01-15"))
                .setWarehouseId(ROSTOV_ID)
        );
        taskQueueRepository.deleteAll();

        assertThat(runAtDay("2020-01-15")).isEmpty();
        assertThat(runAtDay("2020-01-16")).containsExactly(changedTask(123L));
        assertThat(runAtDay("2020-01-17")).isEmpty();
    }

    @Test
    public void testPeriodOneDay() {
        deepmindMskuRepository.save(
            testMsku(123L)
        );
        mskuAvailabilityMatrixRepository.save(
            new MskuAvailabilityMatrix()
                .setMarketSkuId(123L)
                .setFromDate(LocalDate.parse("2020-01-15"))
                .setToDate(LocalDate.parse("2020-01-15"))
                .setWarehouseId(ROSTOV_ID)
        );
        taskQueueRepository.deleteAll();

        assertThat(runAtDay("2020-01-14")).isEmpty();
        assertThat(runAtDay("2020-01-15")).containsExactly(changedTask(123L));
        assertThat(runAtDay("2020-01-16")).containsExactly(changedTask(123L));
        assertThat(runAtDay("2020-01-17")).isEmpty();
    }

    @Test
    public void testPeriodManyDays() {
        deepmindMskuRepository.save(
            testMsku(123L)
        );
        mskuAvailabilityMatrixRepository.save(
            new MskuAvailabilityMatrix()
                .setMarketSkuId(123L)
                .setFromDate(LocalDate.parse("2020-01-15"))
                .setToDate(LocalDate.parse("2020-01-20"))
                .setWarehouseId(ROSTOV_ID)
        );
        taskQueueRepository.deleteAll();

        assertThat(runAtDay("2020-01-14")).isEmpty();
        assertThat(runAtDay("2020-01-15")).containsExactly(changedTask(123L));
        assertThat(runAtDay("2020-01-16")).isEmpty();
        assertThat(runAtDay("2020-01-17")).isEmpty();
        assertThat(runAtDay("2020-01-18")).isEmpty();
        assertThat(runAtDay("2020-01-19")).isEmpty();
        assertThat(runAtDay("2020-01-20")).isEmpty();
        assertThat(runAtDay("2020-01-21")).containsExactly(changedTask(123L));
        assertThat(runAtDay("2020-01-22")).isEmpty();
    }

    @Test
    public void testPeriodManyMsku() {
        deepmindMskuRepository.save(
            testMsku(111L),
            testMsku(222L),
            testMsku(333L)
        );
        mskuAvailabilityMatrixRepository.save(
            new MskuAvailabilityMatrix()
                .setMarketSkuId(111L)
                .setFromDate(LocalDate.parse("2020-01-15"))
                .setWarehouseId(ROSTOV_ID),
            new MskuAvailabilityMatrix()
                .setMarketSkuId(222L)
                .setToDate(LocalDate.parse("2020-01-16"))
                .setWarehouseId(ROSTOV_ID),
            new MskuAvailabilityMatrix()
                .setMarketSkuId(333L)
                .setFromDate(LocalDate.parse("2020-01-17"))
                .setToDate(LocalDate.parse("2020-01-18"))
                .setWarehouseId(ROSTOV_ID)
        );
        taskQueueRepository.deleteAll();

        assertThat(runAtDay("2020-01-14")).isEmpty();
        assertThat(runAtDay("2020-01-15")).containsExactly(changedTask(111L));
        assertThat(runAtDay("2020-01-16")).isEmpty();
        assertThat(runAtDay("2020-01-17")).containsExactlyInAnyOrder(changedTask(222L, 333L));
        assertThat(runAtDay("2020-01-18")).isEmpty();
        assertThat(runAtDay("2020-01-19")).containsExactly(changedTask(333L));
        assertThat(runAtDay("2020-01-20")).isEmpty();
    }

    @Test
    public void testOneMskuManyPeriods() {
        deepmindMskuRepository.save(
            testMsku(111L)
        );
        mskuAvailabilityMatrixRepository.save(
            new MskuAvailabilityMatrix()
                .setMarketSkuId(111L)
                .setFromDate(LocalDate.parse("2020-01-15"))
                .setWarehouseId(ROSTOV_ID),
            new MskuAvailabilityMatrix()
                .setMarketSkuId(111L)
                .setFromDate(LocalDate.parse("2020-01-17"))
                .setToDate(LocalDate.parse("2020-01-20"))
                .setWarehouseId(TOMILINO_ID),
            new MskuAvailabilityMatrix()
                .setMarketSkuId(111L)
                .setToDate(LocalDate.parse("2020-01-20"))
                .setWarehouseId(SOFINO_ID)
        );
        taskQueueRepository.deleteAll();

        assertThat(runAtDay("2020-01-14")).isEmpty();
        assertThat(runAtDay("2020-01-15")).containsExactly(changedTask(111L));
        assertThat(runAtDay("2020-01-16")).isEmpty();
        assertThat(runAtDay("2020-01-17")).containsExactly(changedTask(111L));
        assertThat(runAtDay("2020-01-18")).isEmpty();
        assertThat(runAtDay("2020-01-19")).isEmpty();
        assertThat(runAtDay("2020-01-20")).isEmpty();
        assertThat(runAtDay("2020-01-21")).containsExactly(changedTask(111L));
        assertThat(runAtDay("2020-01-22")).isEmpty();
    }

    private List<MskuAvailabilityChangedTask> runAtDay(String day) {
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

    private MskuAvailabilityChangedTask changedTask(long... ids) {
        var mskuIds = Arrays.stream(ids).boxed().collect(Collectors.toSet());
        return new MskuAvailabilityChangedTask(mskuIds, "", "", Instant.now());
    }

    private Msku testMsku(Long mskuId) {
        return TestUtils.newMsku(mskuId, 10L).setCategoryId(10L).setVendorId(1111L);
    }
}
