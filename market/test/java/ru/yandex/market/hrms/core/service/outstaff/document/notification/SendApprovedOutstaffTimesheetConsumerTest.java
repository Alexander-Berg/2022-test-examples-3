package ru.yandex.market.hrms.core.service.outstaff.document.notification;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.outstaff.bot.OutstaffTelegramBot;
import ru.yandex.market.hrms.core.service.outstaff_document.notification.SendApprovedOutstaffTimesheetConsumer;
import ru.yandex.market.hrms.core.service.outstaff_document.notification.SendApprovedOutstaffTimesheetPayload;
import ru.yandex.market.hrms.core.service.s3.S3Service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DbUnitDataSet(schema = "public", before = "SendApprovedOutstaffTimesheetConsumerTest.before.csv")
public class SendApprovedOutstaffTimesheetConsumerTest extends AbstractCoreTest {

    @Autowired
    private SendApprovedOutstaffTimesheetConsumer sut;

    @MockBean
    private OutstaffTelegramBot outstaffTelegramBot;

    @MockBean
    private S3Service s3Service;

    @BeforeEach
    public void setUp() {
        when(s3Service.getObject(any(), any())).thenReturn(Optional.of(new byte[0]));
    }

    @AfterEach
    public void release() {
        Mockito.reset(outstaffTelegramBot);
    }

    @Test
    public void shouldSendFilesToTgWhenExistApprovedTimesheets() throws Exception {
        SendApprovedOutstaffTimesheetPayload payload = SendApprovedOutstaffTimesheetPayload.builder()
                .domainId(1L)
                .shiftDate(LocalDate.of(2022, 1, 3))
                .build();

        sut.processPayload(payload);

        verify(outstaffTelegramBot, times(1)).sendDocument(eq("chat_warner"), anyString(), any());
        verify(outstaffTelegramBot, times(2)).sendDocument(eq("chat_disney"), anyString(), any());
    }

    @Test
    public void shouldNotSendFilesToTgWhenNotExistApprovedTimesheets() throws Exception {
        SendApprovedOutstaffTimesheetPayload payload = SendApprovedOutstaffTimesheetPayload.builder()
                .domainId(2L)
                .shiftDate(LocalDate.of(2022, 1, 3))
                .build();

        sut.processPayload(payload);

        verify(outstaffTelegramBot, never()).sendDocument(eq("chat_warner"), anyString(), any());
        verify(outstaffTelegramBot, never()).sendDocument(eq("chat_disney"), anyString(), any());
    }
}
