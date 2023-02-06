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

public class WeeklyCompensatedOrdersByClaimProcessorTest extends WeeklyCompensatedOrdersProcessorsCommonTest {
    @Mock
    MbiOrderServiceClient mbiOrderServiceClient;
    @Autowired
    NotifierProperties notifierProperties;
    @Mock
    PartnerNotificationService partnerNotificationService;
    @BeforeEach
    public void before() {
        notifierProperties.setEnabledWeeklyCompensatedOrdersByClaimMailSendJob(true);
    }
    @Test
    void testSuccessSingleOrderInvocation() throws IOException {
        WeeklyCompensatedOrdersByClaimProcessor processor = new WeeklyCompensatedOrdersByClaimProcessor(
                mbiOrderServiceClient,
                notifierProperties,
                partnerNotificationService);
        when(mbiOrderServiceClient.getWeeklyCompensatedOrdersByClaim())
                .thenReturn(singleResponse());
        processor.processWeeklyCompensatedOrdersByClaim();
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(partnerNotificationService)
                .sendWeeklyCompensatedOrdersByClaimNotification(
                        dataCaptor.capture(),
                        anyLong());
        checkValue(dataCaptor, "single-order.txt");
    }
    @Test
    public void testSuccessSeveralOrdersInvocation() throws IOException {
        WeeklyCompensatedOrdersByClaimProcessor processor = new WeeklyCompensatedOrdersByClaimProcessor(
                mbiOrderServiceClient,
                notifierProperties,
                partnerNotificationService);
        when(mbiOrderServiceClient.getWeeklyCompensatedOrdersByClaim())
                .thenReturn(doubleResponse());
        processor.processWeeklyCompensatedOrdersByClaim();
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(partnerNotificationService)
                .sendWeeklyCompensatedOrdersByClaimNotification(
                        dataCaptor.capture(),
                        anyLong());
        checkValue(dataCaptor, "several-orders.txt");
    }
}
