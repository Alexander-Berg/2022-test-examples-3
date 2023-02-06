package ru.yandex.market.notification.simple.model.context;

import java.time.Instant;
import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.notification.model.context.GroupedNotificationContext;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;
import ru.yandex.market.notification.test.model.AbstractModelTest;

import static ru.yandex.market.notification.simple.model.type.NotificationPriority.HIGH;
import static ru.yandex.market.notification.simple.model.type.NotificationPriority.LOW;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.EMAIL;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.SMS;

/**
 * Unit-тесты для {@link GroupedNotificationContextImpl}.
 *
 * @author Vladislav Bauer
 */
public class GroupedNotificationContextImplTest extends AbstractModelTest {

    @Test
    public void testBasicMethods() {
        final Instant now = Instant.now();
        final GroupedNotificationContext data = createContext(1L, HIGH, EMAIL, now);
        final GroupedNotificationContext sameData = createContext(1L, HIGH, EMAIL, now);
        final GroupedNotificationContext otherData = createContext(2L, LOW, SMS, now);

        checkBasicMethods(data, sameData, otherData);
    }


    private GroupedNotificationContextImpl createContext(
        final long type, final NotificationPriority priorityType, final NotificationTransport notificationTransport,
        final Instant deliveryType
    ) {
        return new GroupedNotificationContextImpl(
            new CodeNotificationType(type), priorityType, Collections.singleton(notificationTransport),
            Collections.emptySet(), deliveryType, null, null
        );
    }

}
