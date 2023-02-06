package ru.yandex.market.notification.mail.service.provider.address;

import java.util.Collection;

import org.junit.Test;

import ru.yandex.market.notification.mail.model.EmailDestination;
import ru.yandex.market.notification.mail.model.address.EmailAddress;
import ru.yandex.market.notification.model.context.NotificationAddressProviderContext;
import ru.yandex.market.notification.model.data.NotificationType;
import ru.yandex.market.notification.model.transport.NotificationAddress;
import ru.yandex.market.notification.simple.service.provider.context.NotificationAddressProviderContextImpl;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit-тесты для {@link SimpleEmailAddressProvider}.
 *
 * @author Vladislav Bauer
 */
public class SimpleEmailAddressProviderTest {

    @Test
    public void testEmptyDestination() {
        final EmailDestination destination = new EmailDestination();
        final Collection<NotificationAddress> addresses = provide(destination);

        assertThat(addresses, empty());
    }

    @Test
    public void testSingleAddress() {
        final EmailAddress emailAddress = mock(EmailAddress.class);
        final EmailDestination destination = EmailDestination.create(emailAddress);
        final Collection<NotificationAddress> addresses = provide(destination);

        assertThat(addresses, hasSize(1));
        assertThat(addresses.iterator().next(), equalTo(emailAddress));
    }


    private Collection<NotificationAddress> provide(final EmailDestination destination) {
        final NotificationType type = mock(NotificationType.class);
        final SimpleEmailAddressProvider provider = new SimpleEmailAddressProvider();
        final NotificationAddressProviderContext context =
            new NotificationAddressProviderContextImpl(type, destination);

        return provider.provide(context);
    }

}
