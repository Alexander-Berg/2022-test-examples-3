package ru.yandex.market.hrms.tms.manager.outstaff_timesheet;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.checkpoint.CheckpointCode;
import ru.yandex.market.hrms.core.service.checkpoint.CheckpointService;
import ru.yandex.market.hrms.core.service.dbqueue.QueueType;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.manager.OutstaffDailyTimesheetManager;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

@DbUnitDataSet(before = "OutstaffDailyTimesheetManagerTest.before.csv")
public class OutstaffDailyTimesheetManagerTest extends AbstractTmsTest {

    private static final List<Long> ALL_DOMAIN_IDS = List.of(1L, 2L, 3L);

    @Autowired
    private OutstaffDailyTimesheetManager sut;

    @MockBean
    private CheckpointService checkpointService;

    @Autowired
    private DbQueueTestUtil dbQueueTestUtil;

    @BeforeEach
    public void setUp() {
        mockClock(LocalDate.of(2022, 3, 4));
    }

    @AfterEach
    public void release() {
        Mockito.reset(checkpointService);
    }

    @Test
    public void shouldGenerateEventsCreateTimesheetWhenNotGeneratedEvents() {
        Mockito.when(checkpointService.getLastCheckpoint(CheckpointCode.GENERATE_EVENT_CREATION_OUTSTAFF_TIMESHEET))
                .thenReturn(Optional.empty());

        sut.generateEventToCreateTimesheet(1, ALL_DOMAIN_IDS);
        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_OUTSTAFF_TIMESHEET, 4);
        Mockito.verify(checkpointService).createNewCheckpoint(CheckpointCode.GENERATE_EVENT_CREATION_OUTSTAFF_TIMESHEET,
                        Instant.parse("2022-03-04T21:00:00.00Z"));
    }

    @Test
    public void shouldNotGenerateEventsCreateTimesheetWhenEventsWereGenerated() {
        Mockito.when(checkpointService.getLastCheckpoint(CheckpointCode.GENERATE_EVENT_CREATION_OUTSTAFF_TIMESHEET))
                .thenReturn(Optional.of(Instant.MAX));

        sut.generateEventToCreateTimesheet(1, ALL_DOMAIN_IDS);

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_OUTSTAFF_TIMESHEET, 0);
        Mockito.verify(checkpointService, never()).createNewCheckpoint(any(), any());
    }

    @Test
    public void shouldGenerateEventCreateTimesheetWhenEventsPartiallyWereGenerated() {
        Mockito.when(checkpointService.getLastCheckpoint(CheckpointCode.GENERATE_EVENT_CREATION_OUTSTAFF_TIMESHEET))
                .thenReturn(Optional.of(Instant.parse("2022-03-04T16:00:00.00Z")));

        sut.generateEventToCreateTimesheet(2, ALL_DOMAIN_IDS);

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_OUTSTAFF_TIMESHEET, 6);
        Mockito.verify(checkpointService).createNewCheckpoint(CheckpointCode.GENERATE_EVENT_CREATION_OUTSTAFF_TIMESHEET,
                Instant.parse("2022-03-05T21:00:00.00Z"));
    }

    @Test
    public void shouldGenerateEventSendApprovedTimesheetWhenEventsWereGenerated() {
        Mockito.when(checkpointService.getLastCheckpoint(CheckpointCode.GENERATE_EVENT_SEND_APPROVED_OUTSTAFF_TIMESHEET))
                .thenReturn(Optional.empty());

        sut.generateEventToSendTimesheet(2, ALL_DOMAIN_IDS);

        dbQueueTestUtil.assertQueueHasSize(QueueType.SEND_APPROVED_OUTSTAFF_TIMESHEET, 4);
        Mockito.verify(checkpointService).createNewCheckpoint(CheckpointCode.GENERATE_EVENT_SEND_APPROVED_OUTSTAFF_TIMESHEET,
                Instant.parse("2022-03-05T10:30:00.00Z"));
    }

    @Test
    public void shouldNotGenerateEventSendApprovedTimesheetWhenEventsAlreadyExists() {
        Mockito.when(checkpointService.getLastCheckpoint(CheckpointCode.GENERATE_EVENT_SEND_APPROVED_OUTSTAFF_TIMESHEET))
                .thenReturn(Optional.of(Instant.MAX));

        sut.generateEventToSendTimesheet(2, ALL_DOMAIN_IDS);

        dbQueueTestUtil.assertQueueHasSize(QueueType.SEND_APPROVED_OUTSTAFF_TIMESHEET, 0);
        Mockito.verify(checkpointService, never()).createNewCheckpoint(any(), any());
    }

    @Test
    public void shouldNotGenerateEventSendApprovedTimesheetWhenEventsPartiallyWereGenerated() {
        Mockito.when(checkpointService.getLastCheckpoint(CheckpointCode.GENERATE_EVENT_SEND_APPROVED_OUTSTAFF_TIMESHEET))
                .thenReturn(Optional.of(Instant.parse("2022-03-04T08:00:00.00Z")));

        sut.generateEventToSendTimesheet(2, ALL_DOMAIN_IDS);

        dbQueueTestUtil.assertQueueHasSize(QueueType.SEND_APPROVED_OUTSTAFF_TIMESHEET, 3);
        Mockito.verify(checkpointService).createNewCheckpoint(CheckpointCode.GENERATE_EVENT_SEND_APPROVED_OUTSTAFF_TIMESHEET,
                Instant.parse("2022-03-05T10:30:00.00Z"));
    }
}
