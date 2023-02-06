package ru.yandex.market.hrms.core.service.checkpoint;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.util.HrmsDateTimeUtil;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;

import static ru.yandex.market.hrms.core.domain.checkpoint.CheckpointCode.GENERATE_EVENT_CREATION_OUTSTAFF_TIMESHEET;

public class CheckpointServiceTest extends AbstractCoreTest {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private CheckpointService sut;

    @Test
    public void shouldGetEmptyCheckpointWhenCheckpointsNotExist() {
        Optional<Instant> lastCheckpoint = sut.getLastCheckpoint(GENERATE_EVENT_CREATION_OUTSTAFF_TIMESHEET);

        Assertions.assertTrue(lastCheckpoint.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "CheckpointServiceTest.shouldGetLastCheckpointWhenCheckpointExists.before.csv")
    public void shouldGetLastCheckpointWhenCheckpointExists() {
        Optional<Instant> lastCheckpoint = sut.getLastCheckpoint(GENERATE_EVENT_CREATION_OUTSTAFF_TIMESHEET);

        Assertions.assertTrue(lastCheckpoint.isPresent());
        Assertions.assertEquals(lastCheckpoint.get(), createInstantUtc("2022-03-15 16:00:00"));
    }

    @Test
    @DbUnitDataSet(after = "CheckpointServiceTest.shouldCreateNewCheckpointWhenCheckpointsNotExist.after.csv")
    public void shouldCreateNewCheckpointWhenCheckpointsNotExist() {
        Instant newCheckpoint = createInstantUtc("2022-04-13 13:12:00");

        sut.createNewCheckpoint(GENERATE_EVENT_CREATION_OUTSTAFF_TIMESHEET, newCheckpoint);
    }

    @Test
    @DbUnitDataSet(before = "CheckpointServiceTest.shouldNotCreateNewCheckpointWhenCheckpointExists.before.csv")
    public void shouldNotCreateNewCheckpointWhenCheckpointsIntersect() {
        Instant newCheckpointDateTime = createInstantUtc("2022-04-02 13:00:00");

        Assertions.assertThrows(TplIllegalArgumentException.class,
                () -> sut.createNewCheckpoint(GENERATE_EVENT_CREATION_OUTSTAFF_TIMESHEET, newCheckpointDateTime));
    }

    private Instant createInstantUtc(String instantString) {
        return HrmsDateTimeUtil.toInstant(LocalDateTime.parse(instantString, DATE_TIME_FORMATTER), ZoneOffset.UTC);
    }
}
