package ru.yandex.market.notification.model.context;

import java.time.Instant;
import java.util.Collections;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.simple.model.context.GroupedNotificationContextImpl;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.model.type.NotificationPriority;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static ru.yandex.market.notification.simple.model.type.NotificationPriority.HIGH;
import static ru.yandex.market.notification.simple.model.type.NotificationPriority.LOW;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.EMAIL;
import static ru.yandex.market.notification.simple.model.type.NotificationTransport.SMS;

/**
 * Unit-тесты для {@link GroupedNotificationContextImpl}.
 *
 * @author Vladislav Bauer
 */
public class GroupedNotificationContextImplTest {

    @Test
    public void testBasicMethods() {
        var now = Instant.now();
        var data = createContext(1L, HIGH, EMAIL, now);
        var sameData = createContext(1L, HIGH, EMAIL, now);
        var otherData = createContext(2L, LOW, SMS, now);

        checkBasicMethods(data, sameData, otherData);
    }


    private GroupedNotificationContext createContext(
            long type, NotificationPriority priorityType, NotificationTransport notificationTransport,
            Instant deliveryType
    ) {
        return new GroupedNotificationContextImpl(
                new CodeNotificationType(type), priorityType, Collections.singleton(notificationTransport),
                Collections.emptySet(), deliveryType, null, null, false
        );
    }

    private <T> void checkBasicMethods(@Nonnull T object, @Nonnull T same, @Nonnull T other) {
        assertThat(object.toString(), not(isEmptyOrNullString()));

        assertThat(object.equals(new Object()), equalTo(false));
        assertThat(object, not(equalTo(other)));
        assertThat(object, equalTo(same));
        assertThat(object.hashCode(), equalTo(same.hashCode()));
        assertThat(object.toString(), equalTo(same.toString()));
    }

}
