package ru.yandex.market.notifier.log;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.notifier.entity.ChannelType;
import ru.yandex.market.notifier.entity.DeliveryChannel;
import ru.yandex.market.notifier.entity.Notification;
import ru.yandex.market.notifier.entity.NotificationStatus;
import ru.yandex.market.notifier.trace.NotifierContextHolder;

/**
 * @author kukabara
 */
public class NotifierLogTest {
    private static final Random RND = new Random();

    @Test
    public void testLogEvent() {
        long time = System.currentTimeMillis();
        NoteMeta meta = new NoteMeta(time);
        for (ChannelType type : ChannelType.values()) {
            meta.addProcessing(type, time + RND.nextInt(1000));
        }
        NotifierContextHolder.setCheckouterEventId(12L);
        NotifierLogger.logEvent(meta);
        NotifierLogger.logEvent(meta);
    }

    @Test
    public void testLogNote() {
        DeliveryChannel dc = new DeliveryChannel();
        dc.setStatus(NotificationStatus.PROCESSED);
        dc.setType(ChannelType.EMAIL);
        dc.setCreatedTs(DateUtil.getToday());
        dc.setStartProcessingTs(System.currentTimeMillis());
        dc.setLastStatusTs(DateUtil.addField(new Date(), Calendar.MILLISECOND, 500));
        NotifierLogger.logNoteSendTime(dc);
    }

    @Test
    public void testLogNotification() {
        Notification notification = new Notification();
        notification.setId(42L);
        notification.setType("testType");
        notification.setAggregation(true);
        notification.setData("someData");
        notification.setInboxTs(new Date());
        notification.setKeepSentDays(314);
        notification.setOrderId(831L);
        notification.setDeliveryChannels(
                Collections.singletonList(new DeliveryChannel(ChannelType.PUSH, "testAddress"))
        );

        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("hello", "world");
        attributes.put("how", "are you");

        NotifierLogger.logNotification(notification, attributes);
    }

    @Test
    public void testLogEmptyNotification() {
        Notification notification = new Notification();
        notification.setId(42L);

        NotifierLogger.logNotification(notification);
    }

}
