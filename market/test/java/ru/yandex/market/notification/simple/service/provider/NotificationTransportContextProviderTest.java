package ru.yandex.market.notification.simple.service.provider;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.notification.model.context.NotificationAddressProviderContext;
import ru.yandex.market.notification.model.context.NotificationContext;
import ru.yandex.market.notification.model.context.NotificationTransportContext;
import ru.yandex.market.notification.model.data.NotificationContent;
import ru.yandex.market.notification.model.data.NotificationType;
import ru.yandex.market.notification.model.transport.NotificationDestination;
import ru.yandex.market.notification.service.NotificationFacade;
import ru.yandex.market.notification.simple.model.type.CodeNotificationType;
import ru.yandex.market.notification.simple.service.provider.complex.ComplexNotificationAddressProvider;
import ru.yandex.market.notification.simple.service.provider.complex.ComplexNotificationContentProvider;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link NotificationTransportContextProvider}.
 *
 * @author Vladislav Bauer
 */
public class NotificationTransportContextProviderTest {
    private static final NotificationType NOTIFICATION_TYPE = new CodeNotificationType(1L);
    private Collection<NotificationDestination> destinations;

    @Before
    public void before() {
        destinations = Collections.singleton(mock(NotificationDestination.class));
    }

    @Test
    public void testProvide() {
        final NotificationContext context = createNotificationContext();
        final ComplexNotificationContentProvider contentProvider = createContentProvider(context);
        final ComplexNotificationAddressProvider addressProvider = createAddressProvider();

        final NotificationTransportContextProvider transportContextProvider =
            createTransportContextProvider(contentProvider, addressProvider);

        final NotificationTransportContext transportContext = transportContextProvider.provide(context);

        assertThat(transportContext.getContent(), notNullValue());
        assertThat(transportContext.getAddresses(), notNullValue());
        assertThat(transportContext.getType(), sameInstance(NOTIFICATION_TYPE));
        assertThat(transportContext.getDestinations(), sameInstance(destinations));

        verify(contentProvider, times(1)).provide(eq(context));
        verifyNoMoreInteractions(contentProvider);

        verify(addressProvider, times(1)).provide(any(NotificationAddressProviderContext.class));
        verifyNoMoreInteractions(addressProvider);
    }


    private NotificationContext createNotificationContext() {
        final NotificationContext context = mock(NotificationContext.class);
        when(context.getDestinations()).thenReturn(destinations);
        when(context.getType()).thenReturn(NOTIFICATION_TYPE);
        return context;
    }

    private ComplexNotificationContentProvider createContentProvider(final NotificationContext context) {
        final ComplexNotificationContentProvider provider = mock(ComplexNotificationContentProvider.class);
        when(provider.provide(eq(context))).thenReturn(mock(NotificationContent.class));
        return provider;
    }

    private ComplexNotificationAddressProvider createAddressProvider() {
        return mock(ComplexNotificationAddressProvider.class);
    }

    private NotificationTransportContextProvider createTransportContextProvider(
        final ComplexNotificationContentProvider contentProvider,
        final ComplexNotificationAddressProvider addressProvider
    ) {
        final NotificationFacade facade = mock(NotificationFacade.class);
        return new NotificationTransportContextProvider(facade) {
            @Nonnull
            @Override
            protected ComplexNotificationContentProvider createContentProvider() {
                return contentProvider;
            }

            @Nonnull
            @Override
            protected ComplexNotificationAddressProvider createAddressProvider() {
                return addressProvider;
            }
        };
    }

}
