package ru.yandex.market.pers.notify.push.generator.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.mail.generator.UserWithPayload;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.model.UserModel;
import ru.yandex.market.pers.notify.ems.event.NotificationEvent;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         15.02.17
 */
public class ExistingNotificationFilterTest extends MarketMailerMockedDbTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private NotificationEventService notificationEventService;

    @Test
    public void testZeroDaysNeverFilter() throws Exception {
        ExistingNotificationFilter filter = new ExistingNotificationFilter(jdbcTemplate, Arrays.asList(NotificationSubtype.values()), 0);
        List<UserWithPayload> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            long uid = RND.nextInt(100_000);
            users.add(new UserWithPayload(new UserModel(uid, null, null), null));
            NotificationEvent event = notificationEventService.addEvent(NotificationEventSource.fromUid(uid, randomType(NotificationTransportType.MAIL)).build());
            users.get(users.size() - 1).setAddress(event.getAddress());
        }
        assertEquals(users, filter.apply(users.stream()).collect(Collectors.toList()));
    }

    private NotificationSubtype randomType(NotificationTransportType transportType) {
        List<NotificationSubtype> list = Arrays.stream(NotificationSubtype.values())
            .filter(type -> transportType == type.getTransportType())
            .collect(Collectors.toList());
        return list.get(RND.nextInt(list.size()));
    }

    @Test
    public void testMix() throws Exception {
        long uidExistingNotification = 1L;
        long uidNoNotification = 2L;
        NotificationEvent event1 = notificationEventService.addEvent(NotificationEventSource.fromUid(uidExistingNotification, NotificationSubtype.CART_1).build());
        NotificationEvent event2 = notificationEventService.addEvent(NotificationEventSource.fromUid(uidNoNotification, NotificationSubtype.WISHLIST_AWAIT).build());
        ExistingNotificationFilter filter = new ExistingNotificationFilter(jdbcTemplate, Collections.singletonList(NotificationSubtype.CART_1), 1);
        List<UserWithPayload> users = new ArrayList<>();
        users.add(new UserWithPayload(new UserModel(uidExistingNotification, null, null), null));
        users.add(new UserWithPayload(new UserModel(uidNoNotification, null, null), null));
        users.get(0).setAddress(event1.getAddress());
        users.get(1).setAddress(event2.getAddress());
        List<UserWithPayload> filteredUsers = filter.apply(users.stream()).collect(Collectors.toList());
        assertEquals(1, filteredUsers.size());
        assertEquals(uidNoNotification, (long) filteredUsers.get(0).getModel().getUid());
    }
}
