package ru.yandex.market.notifier.jobs.zk;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.annotation.Resource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.notifier.ResourceLoadUtil;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.entity.NotificationStatus;
import ru.yandex.market.notifier.jobs.tms.NotificationEvictionJob;
import ru.yandex.market.notifier.service.InboxService;
import ru.yandex.market.notifier.storage.InboxDao;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class NotificationEvictionJobTest extends AbstractServicesTestBase {

    @Resource
    private NotificationEvictionJob notificationEvictionJob;

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
                dc.setStatus(NotificationStatus.DELETED);
            });
            inboxService.saveNotification(note);
        }
    }

    @Test
    @DisplayName("Проверяет корректность удаления просроченных уведомлений")
    public void testEvictionJob() {
        // перемещаемся в будущее, где текущие уведомления станут не актуальными
        setFixedTime(Instant.now().plus(10, ChronoUnit.DAYS), ZoneId.systemDefault());

        notificationEvictionJob.doJob(null);

        // проверяем, что наши уведомления удалены
        Assertions.assertEquals(0, inboxDao.getAllNotifications().size());
    }
}
