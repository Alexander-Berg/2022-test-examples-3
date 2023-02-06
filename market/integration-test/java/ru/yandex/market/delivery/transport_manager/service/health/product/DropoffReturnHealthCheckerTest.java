package ru.yandex.market.delivery.transport_manager.service.health.product;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.unit_queue.UnitQueue;
import ru.yandex.market.delivery.transport_manager.domain.entity.unit_queue.UnitQueueStatus;
import ru.yandex.market.delivery.transport_manager.domain.entity.unit_queue.UnitQueueType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.UnitQueueMapper;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

@DatabaseSetup("/repository/distribution_unit_center/dropoff_returns.xml")
class DropoffReturnHealthCheckerTest extends AbstractContextualTest {

    @Autowired
    DropoffReturnHealthChecker checker;

    @Autowired
    UnitQueueMapper unitQueueMapper;

    @Autowired
    TestableClock clock;

    @BeforeEach
    void setUp() {
        clock.setFixed(DateTimeUtil.atStartOfDay(LocalDate.of(2033, 11, 11)), ZoneId.of("UTC"));
    }

    @Test
    void returnBagsStateOk() {
        unitQueueMapper.insert(List.of(
            unitQueue("2", UnitQueueType.BAG, 1L, 2L, UnitQueueStatus.PROCESSED),
            unitQueue("3", UnitQueueType.BAG, 3L, 4L, UnitQueueStatus.PROCESSED)
        ));
        softly.assertThat(checker.checkReturnBagsProcessed()).isEqualTo("0;OK");
    }

    @Test
    void returnBagsNotOk() {
        unitQueueMapper.insert(List.of(
            unitQueue("2", UnitQueueType.BAG, 1L, 2L, UnitQueueStatus.PROCESSED),
            unitQueue("3", UnitQueueType.BAG, 3L, 4L, UnitQueueStatus.PROCESSED),
            unitQueue("4", UnitQueueType.BAG, 3L, 4L, UnitQueueStatus.QUEUED)
        ));
        softly.assertThat(checker.checkReturnBagsProcessed()).isEqualTo(
            "2;Failed to process new dc state in time. Check unit_queue table: [UnitQueueStat(dcUnitId=4, " +
                "reachedUnitQueue=true, reachedProcessed=false)]"
        );
    }

    @Test
    @DatabaseSetup("/repository/startrek/choose_ready_return_dropoffs.xml")
    @DatabaseSetup(
        value = "/repository/startrek/update/additional_sc_tickets.xml",
        type = DatabaseOperation.INSERT
    )
    void returnTicketsCreated() {
        clock.setFixed(Instant.parse("2020-09-28T03:00:00Z"), ZoneId.systemDefault());
        softly.assertThat(checker.checkReturnTicketsCreated()).isEqualTo("0;OK");
    }

    @Test
    @DatabaseSetup("/repository/startrek/choose_ready_return_dropoffs.xml")
    void returnTicketsAbsent() {
        clock.setFixed(Instant.parse("2020-09-28T03:00:00Z"), ZoneId.systemDefault());
        softly.assertThat(checker.checkReturnTicketsCreated()).isEqualTo(
            "2;Return dropoff tickets are absent for the following SC point ids: 111, 124"
        );
    }

    @Test
    @DatabaseSetup("/repository/startrek/choose_ready_return_dropoffs.xml")
    void returnTicketsAbsentBeforeCutoff() {
        clock.setFixed(Instant.parse("2020-09-28T01:00:00Z"), ZoneOffset.UTC);
        softly.assertThat(checker.checkReturnTicketsCreated()).isEqualTo("0;OK");
    }

    private UnitQueue unitQueue(
        String unitId,
        UnitQueueType unitType,
        Long pointFrom,
        Long pointTo,
        UnitQueueStatus status
    ) {
        return new UnitQueue()
            .setUnitId(unitId)
            .setUnitType(unitType)
            .setPointFromId(pointFrom)
            .setPointToId(pointTo)
            .setStatus(status)
            .setCreated(LocalDateTime.ofInstant(clock.instant().minusSeconds(600L), ZoneId.of("UTC")));
    }
}
