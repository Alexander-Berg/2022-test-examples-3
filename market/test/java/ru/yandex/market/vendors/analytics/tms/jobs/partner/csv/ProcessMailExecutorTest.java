package ru.yandex.market.vendors.analytics.tms.jobs.partner.csv;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.vendors.analytics.core.model.mail.MessageStatus;
import ru.yandex.market.vendors.analytics.core.service.startrek.StartrekClient;
import ru.yandex.market.vendors.analytics.tms.FunctionalTest;
import ru.yandex.market.vendors.analytics.tms.service.mail.MailService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Функциональный тест для джобы {@link ProcessMailExecutor}.
 *
 * @author sergeymironov
 */
public class ProcessMailExecutorTest extends FunctionalTest {

    @Autowired
    MailService mailService;
    @Autowired
    private ProcessMailExecutor processMailExecutor;
    @Autowired
    StartrekClient startrekClient;

    @Test
    void noNonvalidMessagesTest() {
        var emptyStatusErrorInfo = new HashMap<MessageStatus, List<String>>();
        when(mailService.processInboxAndGetStatistics()).thenReturn(emptyStatusErrorInfo);

        processMailExecutor.doJob(null);
        Mockito.verify(startrekClient, never()).createTicket(any());
    }

    @Test
    void hasNonvalidMessagesTest() {
        var statusErrorInfo = new HashMap<MessageStatus, List<String>>();
        statusErrorInfo.put(MessageStatus.NON_VALID, Collections.singletonList("file has error"));
        when(mailService.processInboxAndGetStatistics()).thenReturn(statusErrorInfo);

        processMailExecutor.doJob(null);
        Mockito.verify(startrekClient, times(1)).createTicket(any());
    }

    @Test
    void hasValidMessagesTest() {
        var statusErrorInfo = new HashMap<MessageStatus, List<String>>();
        statusErrorInfo.put(MessageStatus.VALID, Collections.singletonList("file is valid"));
        when(mailService.processInboxAndGetStatistics()).thenReturn(statusErrorInfo);

        processMailExecutor.doJob(null);
        Mockito.verify(startrekClient, times(1)).createTicket(any());
    }
}
