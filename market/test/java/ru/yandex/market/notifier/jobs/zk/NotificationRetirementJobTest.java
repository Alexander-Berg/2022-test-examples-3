package ru.yandex.market.notifier.jobs.zk;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.notifier.ResourceLoadUtil;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.entity.DeliveryChannel;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.entity.NotificationStatus;
import ru.yandex.market.notifier.jobs.tms.NotificationRetirementJob;
import ru.yandex.market.notifier.service.InboxService;
import ru.yandex.market.notifier.storage.InboxDao;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class NotificationRetirementJobTest extends AbstractServicesTestBase {

    @Resource
    private NotificationRetirementJob notificationRetirementJob;

    @Autowired
    private InboxDao inboxDao;

    @Autowired
    private ResourceLoadUtil resourceLoadUtil;

    @Autowired
    private InboxService inboxService;

    @BeforeEach
    public void setup() throws IOException {
        List<Notification> notifications = resourceLoadUtil.getSampleNotifications();
        assertFalse(notifications.isEmpty());
        RequestContextHolder.createNewContext();
        for (Notification note : notifications) {
            note.getDeliveryChannels().forEach(dc -> {
                dc.setStatus(NotificationStatus.PROCESSED);
            });
            inboxService.saveNotification(note);
        }
    }

    @Test
    @DisplayName("Проверяет корректность устаревания каналов доставки")
    public void testRetirementJob() {
        // перемещаемся в будущее, где текущие уведомления станут не актуальными
        setFixedTime(Instant.now().plus(60, ChronoUnit.DAYS), ZoneId.systemDefault());
        notificationRetirementJob.doJob(null);

        // проверяем, что каналы доставки стали DELETED
        Set<DeliveryChannel> channels = inboxDao.getAllNotifications().stream()
                .map(Notification::getDeliveryChannels)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        assertThat(channels, everyItem(hasProperty("status", equalTo(NotificationStatus.DELETED))));
    }
}
