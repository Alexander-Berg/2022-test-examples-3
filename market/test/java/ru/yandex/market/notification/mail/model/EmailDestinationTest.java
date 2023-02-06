package ru.yandex.market.notification.mail.model;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.notification.mail.model.address.EmailAddress;

import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link EmailDestination}.
 *
 * @author Vladislav Bauer
 */
public class EmailDestinationTest {

    @Test(expected = UnsupportedOperationException.class)
    public void testImmutableAddresses() {
        final EmailAddress address = Mockito.mock(EmailAddress.class);
        final EmailDestination destination = new EmailDestination();

        fail(String.valueOf(destination.getAddresses().add(address)));
    }

}
