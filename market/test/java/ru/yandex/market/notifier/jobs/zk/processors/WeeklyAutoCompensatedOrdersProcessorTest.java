package ru.yandex.market.notifier.jobs.zk.processors;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.notifier.entity.NotifierProperties;
import ru.yandex.market.notifier.orderservice.MbiOrderServiceClient;
import ru.yandex.market.notifier.service.PartnerNotificationService;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WeeklyAutoCompensatedOrdersProcessorTest extends WeeklyCompensatedOrdersProcessorsCommonTest {
    @Mock
    MbiOrderServiceClient mbiOrderServiceClient;
    @Autowired
    NotifierProperties notifierProperties;
    @Mock
    PartnerNotificationService partnerNotificationService;
    @BeforeEach
    public void before() {
        notifierProperties.setEnabledWeeklyCompensatedOrdersMailSendJob(true);
    }
    @Test
    public void testSuccessSingleOrderInvocation() throws IOException {
        WeeklyAutoCompensatedOrdersProcessor processor = new WeeklyAutoCompensatedOrdersProcessor(
                mbiOrderServiceClient,
                notifierProperties,
                partnerNotificationService);
        when(mbiOrderServiceClient.getWeeklyAutoCompensatedOrders()).thenReturn(
                singleResponse());
        processor.processWeeklyAutoCompensatedOrders();
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(partnerNotificationService).sendWeeklyCompensatedOrdersNotification(
                dataCaptor.capture(),
                anyLong());
        checkValue(dataCaptor, "single-order.txt");
    }

    @Test
    public void testSuccessSeveralOrdersInvocation() throws IOException {
        WeeklyAutoCompensatedOrdersProcessor processor = new WeeklyAutoCompensatedOrdersProcessor(
                mbiOrderServiceClient,
                notifierProperties,
                partnerNotificationService);
        when(mbiOrderServiceClient.getWeeklyAutoCompensatedOrders()).thenReturn(
                doubleResponse());
        processor.processWeeklyAutoCompensatedOrders();
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(partnerNotificationService).sendWeeklyCompensatedOrdersNotification(
                dataCaptor.capture(),
                anyLong());
        checkValue(dataCaptor, "several-orders.txt");
    }
}
