package ru.yandex.market.logistics.cs.dayoff;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;
import ru.yandex.market.logistics.cs.domain.entity.ServiceDayOff;
import ru.yandex.market.logistics.cs.repository.ServiceDayOffRepository;
import ru.yandex.market.logistics.cs.service.ServiceDayOffService;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

@ParametersAreNonnullByDefault
@DatabaseSetup("/repository/dayoff/before/base_dayoff.xml")
public abstract class AbstractDayOffTest extends AbstractIntegrationTest {

    private static final QueueShardId SHARD_ID = new QueueShardId("master");
    protected static final Comparator<LocalDateTime> LOCAL_DATE_TIME_COMPARATOR = Comparator.comparing(
        LocalDateTime::toLocalDate
    );

    protected static final long SERVICE_ID_10 = 10L;
    protected static final long SERVICE_ID_11 = 11L;
    protected static final long SERVICE_ID_20 = 20L;
    protected static final long SERVICE_ID_21 = 21L;
    protected static final long CAPACITY_ID_1L = 1L;
    protected static final long CAPACITY_ID_2L = 2L;
    protected static final LocalDate TWENTIETH_OF_MAY = LocalDate.parse("2021-05-20");
    protected static final LocalDate TENTH_OF_MAY = LocalDate.parse("2021-05-10");

    @Autowired
    protected ServiceDayOffRepository dayOffDayRepository;

    @Autowired
    protected ServiceDayOffService service;

    @Nonnull
    protected ServiceDayOff dayOff(Long serviceId, LocalDate date) {
        return ServiceDayOff.builder()
            .serviceId(serviceId)
            .day(date)
            .build();
    }

    @Nonnull
    protected <T> Task<T> createTask(T payload) {
        return new Task<>(
            SHARD_ID,
            payload,
            0,
            ZonedDateTime.now(ZoneId.of("UTC")),
            "traceInfo",
            "actor"
        );
    }
}
