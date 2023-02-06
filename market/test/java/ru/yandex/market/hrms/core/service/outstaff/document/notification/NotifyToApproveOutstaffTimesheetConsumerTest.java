package ru.yandex.market.hrms.core.service.outstaff.document.notification;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.dbqueue.QueueType;
import ru.yandex.market.hrms.core.service.email.MailSenderService;
import ru.yandex.market.hrms.core.service.outstaff.bot.OutstaffTelegramBot;
import ru.yandex.market.hrms.core.service.outstaff_document.notification.NotifyToApproveOutstaffTimesheetConsumer;
import ru.yandex.market.hrms.core.service.outstaff_document.notification.NotifyToApproveOutstaffTimesheetPayload;
import ru.yandex.market.hrms.model.outstaff.OutStaffShiftType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(schema = "public", before = "NotifyToApproveOutstaffTimesheetConsumerTest.before.csv")
public class NotifyToApproveOutstaffTimesheetConsumerTest extends AbstractCoreTest {

    @Autowired
    private NotifyToApproveOutstaffTimesheetConsumer sut;

    @Autowired
    private DbQueueTestUtil dbQueueTestUtil;

    @MockBean
    private OutstaffTelegramBot outstaffTelegramBot;

    @MockBean
    private MailSenderService mailSenderService;

    @AfterEach
    public void release() {
        Mockito.reset(outstaffTelegramBot, mailSenderService);
    }

    @Test
    public void shouldSendMessageToTgWhenNotApprovedOnFirstIteration() throws Exception {
        NotifyToApproveOutstaffTimesheetPayload payload = NotifyToApproveOutstaffTimesheetPayload.builder()
                .domainId(1L)
                .shiftDate(LocalDate.of(2022, 1, 1))
                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                .iteration(1)
                .shiftEndDatetime(Instant.parse("2022-01-01T06:00:00+03:00"))
                .delayMinutes(30)
                .build();

        sut.processPayload(payload);

        verify(outstaffTelegramBot, times(1)).sendMessage(anyString(), anyString());
        dbQueueTestUtil.assertQueueHasSize(QueueType.NOTIFY_TO_APPROVE_OUTSTAFF_TIMESHEET, 1);
    }

    @Test
    public void shouldSendMessageToTgWhenNotApprovedOnSecondIteration() throws Exception {
        NotifyToApproveOutstaffTimesheetPayload payload = NotifyToApproveOutstaffTimesheetPayload.builder()
                .domainId(1L)
                .shiftDate(LocalDate.of(2022, 1, 1))
                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                .iteration(2)
                .shiftEndDatetime(Instant.parse("2022-01-01T06:00:00+03:00"))
                .delayMinutes(30)
                .build();

        sut.processPayload(payload);

        verify(outstaffTelegramBot, times(1)).sendMessage(anyString(), anyString());
        dbQueueTestUtil.assertQueueHasSize(QueueType.NOTIFY_TO_APPROVE_OUTSTAFF_TIMESHEET, 1);
    }

    @Test
    public void shouldSendEmailToOfficialWhenNotApprovedOnThirdIteration() throws Exception {
        NotifyToApproveOutstaffTimesheetPayload payload = NotifyToApproveOutstaffTimesheetPayload.builder()
                .domainId(1L)
                .shiftDate(LocalDate.of(2022, 1, 1))
                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                .iteration(3)
                .shiftEndDatetime(Instant.parse("2022-01-01T06:00:00+03:00"))
                .delayMinutes(30)
                .build();

        sut.processPayload(payload);

        verify(outstaffTelegramBot, never()).sendMessage(anyString(), anyString());
        verify(mailSenderService, times(1)).sendEmail(anyString(), anyString(), any(), any());
        dbQueueTestUtil.isEmpty(QueueType.NOTIFY_TO_APPROVE_OUTSTAFF_TIMESHEET);
    }

    @Test
    public void shouldNotSendMessageWhenNotExistNotApprovedOnShift() throws Exception {
        NotifyToApproveOutstaffTimesheetPayload payload = NotifyToApproveOutstaffTimesheetPayload.builder()
                .domainId(2L)
                .shiftDate(LocalDate.of(2022, 1, 1))
                .shiftType(OutStaffShiftType.FIRST_SHIFT)
                .iteration(1)
                .shiftEndDatetime(Instant.parse("2022-01-01T06:00:00+03:00"))
                .delayMinutes(30)
                .build();

        sut.processPayload(payload);

        verify(outstaffTelegramBot, never()).sendMessage(anyString(), anyString());
        verify(mailSenderService, never()).sendEmail(anyString(), anyString(), any(), any());
        dbQueueTestUtil.isEmpty(QueueType.NOTIFY_TO_APPROVE_OUTSTAFF_TIMESHEET);
    }
}
