package ru.yandex.market.notifier.jobs.zk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import ch.qos.logback.classic.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.notifier.ResourceLoadUtil;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.entity.NotificationStatus;
import ru.yandex.market.notifier.jobs.tms.DeliveryWorkerJob;
import ru.yandex.market.notifier.service.InboxService;
import ru.yandex.market.notifier.service.TransmissionService;
import ru.yandex.market.notifier.util.InMemoryAppender;
import ru.yandex.market.notifier.util.NotifierTestUtils;
import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.request.trace.Module;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class DeliveryWorkerJobTest extends AbstractServicesTestBase {

    private static final Random RND = new Random();
    private final PersNotifyClient clientMock = mock(PersNotifyClient.class);
    private final List<NotificationEventSource> events = new ArrayList<>();
    private final TransmissionService transmissionService = spy(new TransmissionService());
    private final InboxService inboxService = mock(InboxService.class);

    @Autowired
    private ResourceLoadUtil resourceLoadUtil;

    @Resource(name = "notificationDeliveryTmsJob_0")
    private DeliveryWorkerJob deliveryWorkerJob;

    private InMemoryAppender traceAppender;
    private String requestId = UUID.randomUUID().toString();

    @BeforeEach
    public void init() throws Exception {
        transmissionService.setPersNotifyClient(NotifierTestUtils.getMockClient(clientMock, events));
        deliveryWorkerJob.setTransmissionService(transmissionService);
        deliveryWorkerJob.setInboxService(inboxService);
        when(inboxService.findFailed(anyInt(), anyInt(), any(Date.class)))
                .thenReturn(Collections.emptyList());
        traceAppender = new InMemoryAppender();
        traceAppender.start();

        ((Logger) LoggerFactory.getLogger("requestTrace")).addAppender(traceAppender);
    }

    private List<Notification> getNotifications(ChannelType type, ChannelType... filter) throws IOException {
        Set<ChannelType> channelTypes = EnumSet.of(type, filter);
        return resourceLoadUtil.getSampleNotifications().stream()
                .filter(n -> !n.getData().contains(DeliveryWorkerJob.ENCLOSING_TAG_NAME))
                .filter(n -> n.getDeliveryChannels().stream()
                        .allMatch(dc -> channelTypes.contains(dc.getType())))
                .peek(n -> n.getDeliveryChannels().forEach(ch -> {
                    ch.setId(RND.nextLong());
                    ch.setRegionId(213L);
                    ch.setAddress("abc@test.net");
                    ch.setStatus(NotificationStatus.NEW);
                    ch.setCreatedTs(new Date());
                    ch.setRequestId(requestId);
                }))
                .peek(n -> n.setInboxTs(new Date(0)))
                .peek(n -> n.setId(RND.nextLong()))
                .collect(Collectors.toList());
    }

    private List<Notification> getNotificationsToAggregate(List<Notification> notifications) throws IOException {
        List<Notification> result = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            result.addAll(notifications.stream()
                    .peek(n -> n.setAggregation(true)).collect(Collectors.toList()));
        }
        return result;
    }

    @Test
    public void shouldCreateMarketMailerEvent() throws Exception {
        List<Notification> expected = getNotifications(ChannelType.EMAIL);

        when(inboxService.findNewNonBlocked(
                anyInt(),
                anyInt(),
                anyInt(),
                isNull(),
                any(Date.class)
        ))
                .thenReturn(getNotificationsToAggregate(expected));

        deliveryWorkerJob.doJob(null, 0);

        assertTrue(events.size() == expected.size());

        events.forEach(e -> assertNotNull(e.getEmail()));
    }

    @Test
    public void shouldWriteTraceLog() throws IOException {
        List<Notification> expected = getNotifications(ChannelType.EMAIL);

        when(inboxService.findNewNonBlocked(
                anyInt(),
                anyInt(),
                anyInt(),
                isNull(),
                any(Date.class)
        ))
                .thenReturn(expected);

        deliveryWorkerJob.doJob(null, 0);

        List<Map<String, String>> logs = traceAppender.getTskvMaps();

        assertThat(logs, notNullValue());
        assertThat(logs, hasSize(1));

        assertThat(logs, hasItems(
                allOf(
                        hasEntry("request_method", "DeliveryWorkerJob.deliver"),
                        hasEntry("source_module", Module.MARKET_NOTIFIER.toString()),
                        hasEntry("target_module", Module.MARKET_NOTIFIER.toString()),
                        hasEntry(is("request_id"), startsWith(requestId))
                )
        ));
    }
}
