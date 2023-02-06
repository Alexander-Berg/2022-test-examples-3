package ru.yandex.market.notification.safe.service.provider.impl;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.notification.model.context.GroupedNotificationContext;
import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.model.data.NotificationContextData;
import ru.yandex.market.notification.model.data.NotificationData;
import ru.yandex.market.notification.model.data.NotificationType;
import ru.yandex.market.notification.model.transport.NotificationPriorityType;
import ru.yandex.market.notification.model.transport.NotificationTransportType;
import ru.yandex.market.notification.simple.model.context.GroupedNotificationContextImpl;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;

/**
 * Unit-тесты для {@link GroupedNotificationContextProvider}.
 *
 * @author Vladislav Bauer
 */
public class GroupedNotificationContextProviderTest {

    @Test
    public void testEmptyContext() {
        var provider = createProvider();
        var groupedContext = createContext(Collections.emptySet());

        assertThat(provider.provide(groupedContext), empty());
    }

    @Test
    public void testFewEntries() {
        var provider = createProvider();
        Set<NotificationTransportType> transportTypes =
            new HashSet<>(Arrays.asList(NotificationTransport.values()));
        var groupedContext = createContext(transportTypes);

        var contexts = provider.provide(groupedContext);
        assertThat(contexts, hasSize(transportTypes.size()));

        var typeIterator = transportTypes.iterator();
        for (var context : contexts) {
            var transportType = typeIterator.next();
            checkContext(groupedContext, context, transportType);
        }
    }


    private void checkContext(
        GroupedNotificationContext groupedContext, NotificationContext context,
        NotificationTransportType transport
    ) {
        assertThat(context.getType(), equalTo(groupedContext.getType()));
        assertThat(context.getData().orElse(null), equalTo(groupedContext.getData().orElse(null)));
        assertThat(context.getContent().isPresent(), equalTo(false));
        assertThat(context.getTransportType(), equalTo(transport));
        assertThat(context.getPriorityType(), equalTo(groupedContext.getPriorityType()));
        assertThat(context.getDeliveryTime(), equalTo(groupedContext.getDeliveryTime()));
        assertThat(context.getDestinations(), equalTo(groupedContext.getDestinations()));
    }

    private GroupedNotificationContextProvider createProvider() {
        return new GroupedNotificationContextProvider();
    }

    private GroupedNotificationContext createContext(Set<NotificationTransportType> transportTypes) {
        return new GroupedNotificationContextImpl(
            mock(NotificationType.class),
            mock(NotificationPriorityType.class),
            transportTypes,
            Collections.emptySet(),
            Instant.now(),
            mock(NotificationContextData.class),
            mock(NotificationData.class),
            false
        );
    }

}
