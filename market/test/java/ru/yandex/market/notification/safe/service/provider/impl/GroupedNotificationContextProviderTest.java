package ru.yandex.market.notification.safe.service.provider.impl;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import ru.yandex.market.notification.model.context.GroupedNotificationContext;
import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.model.data.NotificationContextData;
import ru.yandex.market.notification.model.data.NotificationData;
import ru.yandex.market.notification.model.data.NotificationType;
import ru.yandex.market.notification.model.transport.NotificationPriorityType;
import ru.yandex.market.notification.model.transport.NotificationTransportType;
import ru.yandex.market.notification.simple.model.context.GroupedNotificationContextImpl;
import ru.yandex.market.notification.simple.model.type.NotificationTransport;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit-тесты для {@link GroupedNotificationContextProvider}.
 *
 * @author Vladislav Bauer
 */
public class GroupedNotificationContextProviderTest {

    @Test
    public void testEmptyContext() {
        final GroupedNotificationContextProvider provider = createProvider();
        final GroupedNotificationContext groupedContext = createContext(Collections.emptySet());

        assertThat(provider.provide(groupedContext), empty());
    }

    @Test
    public void testFewEntries() {
        final GroupedNotificationContextProvider provider = createProvider();
        final Set<NotificationTransportType> transportTypes =
            new HashSet<>(Arrays.asList(NotificationTransport.values()));
        final GroupedNotificationContext groupedContext = createContext(transportTypes);

        final Collection<NotificationContext> contexts = provider.provide(groupedContext);
        assertThat(contexts, hasSize(transportTypes.size()));

        final Iterator<NotificationTransportType> typeIterator = transportTypes.iterator();
        for (final NotificationContext context : contexts) {
            final NotificationTransportType transportType = typeIterator.next();
            checkContext(groupedContext, context, transportType);
        }
    }


    private void checkContext(
        final GroupedNotificationContext groupedContext, final NotificationContext context,
        final NotificationTransportType transport
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

    private GroupedNotificationContext createContext(final Set<NotificationTransportType> transportTypes) {
        return new GroupedNotificationContextImpl(
            mock(NotificationType.class),
            mock(NotificationPriorityType.class),
            transportTypes,
            Collections.emptySet(),
            Instant.now(),
            mock(NotificationContextData.class),
            mock(NotificationData.class)
        );
    }

}
