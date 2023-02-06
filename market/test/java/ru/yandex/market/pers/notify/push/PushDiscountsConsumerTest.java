package ru.yandex.market.pers.notify.push;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.pers.notify.mock.MockFactory;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.ems.event.NotificationEventPayload;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author artemmz
 *         created on 17.05.16.
 */
public class PushDiscountsConsumerTest extends MarketMailerMockedDbTest {
    @Autowired
    PushDiscountsConsumer pushDiscountsConsumer;
    @Autowired
    private MobileAppInfoDAO mobileAppInfoDAO;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() throws Exception {
        jdbcTemplate.update("TRUNCATE TABLE MOBILE_APP_INFO");
    }

    @Test
    public void getNoLogin() throws Exception {
        boolean authorized = RND.nextBoolean();
        NotificationEvent event = getEventAds(authorized, UUID.randomUUID().toString(), (long) RND.nextInt(10_000));

        assertEquals(NotificationEventStatus.REJECTED_AS_UNNECESSARY,
            pushDiscountsConsumer.check(NotificationEventPayload.from(event), NotificationEventStatus.SENT));

        event = getEventNotAds(authorized, UUID.randomUUID().toString(), RND.nextLong());

        assertEquals(NotificationEventStatus.REJECTED_AS_UNNECESSARY,
            pushDiscountsConsumer.check(NotificationEventPayload.from(event), NotificationEventStatus.SENT));
    }

    @Test
    public void getLastLoginTime() throws Exception {
        boolean authorized = RND.nextBoolean();
        MobileAppInfo info = new MobileAppInfo(authorized ? (long) RND.nextInt(10_000) : null,
            UUID.randomUUID().toString(), "app", null, "", MobilePlatform.ANY, true, new Date(), (long) RND.nextInt(10_000), (long) RND.nextInt(10_000), true);
        mobileAppInfoDAO.add(info);

        NotificationEvent event = getEventAds(authorized, info.getUuid(), info.getUid());
        long time = pushDiscountsConsumer.getLastLoginTime(NotificationEventPayload.from(event));
        long currentTime = new Date().getTime();
        System.out.println(time);
        System.out.println(currentTime);
        assertTrue(2_000 > currentTime - time);

        assertEquals(NotificationEventStatus.SENT,
            pushDiscountsConsumer.check(NotificationEventPayload.from(event), NotificationEventStatus.SENT));

        event = getEventNotAds(authorized, info.getUuid(), info.getUid());
        assertEquals(NotificationEventStatus.SENT,
            pushDiscountsConsumer.check(NotificationEventPayload.from(event), NotificationEventStatus.SENT));
    }

    @Test
    public void getLastLoginTimeBig() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2015);
        calendar.set(Calendar.MONTH, Calendar.JANUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        boolean authorized = RND.nextBoolean();
        MobileAppInfo info = new MobileAppInfo(authorized ? (long) RND.nextInt(10_000) : null,
            UUID.randomUUID().toString(), "app", null, "", MobilePlatform.ANY, true, calendar.getTime(), (long) RND.nextInt(10_000), (long) RND.nextInt(10_000), true);
        mobileAppInfoDAO.add(info);

        NotificationEvent event = getEventAds(authorized, info.getUuid(), info.getUid());
        assertTrue(PushDiscountsConsumer.DO_NOT_SEND_ADS_AFTER_OFFLINE_MS < new Date().getTime() - pushDiscountsConsumer.getLastLoginTime(NotificationEventPayload.from(event)));

        assertEquals(NotificationEventStatus.REJECTED_AS_UNPOLITE,
            pushDiscountsConsumer.check(NotificationEventPayload.from(event), NotificationEventStatus.SENT));

        event = getEventNotAds(authorized, info.getUuid(), info.getUid());
        assertEquals(NotificationEventStatus.SENT,
            pushDiscountsConsumer.check(NotificationEventPayload.from(event), NotificationEventStatus.SENT));
    }

    private NotificationEvent getEventAds(boolean authorized, String uuid, Long uid) {
        NotificationEvent event = MockFactory.generateNotificationEvent();
        event.setUid(authorized ? uid : null);
        event.setUuid(!authorized ? uuid : null);
        event.setAddress(authorized ? String.valueOf(uid) : uuid);
        event.setNotificationSubtype(PushDiscountsConsumer.ADS_PUSH.stream().findAny().orElse(null));
        return event;
    }

    private NotificationEvent getEventNotAds(boolean authorized, String uuid, Long uid) {
        NotificationEvent event = MockFactory.generateNotificationEvent();
        event.setUid(authorized ? uid : null);
        event.setUuid(!authorized ? uuid : null);
        event.setAddress(authorized ? String.valueOf(uid) : uuid);
        event.setNotificationSubtype(Arrays.stream(NotificationSubtype.values())
            .filter(t -> t.getTransportType() == NotificationTransportType.PUSH)
            .filter(t -> !PushDiscountsConsumer.ADS_PUSH.contains(t))
            .findAny().orElse(null));
        return event;
    }

}
