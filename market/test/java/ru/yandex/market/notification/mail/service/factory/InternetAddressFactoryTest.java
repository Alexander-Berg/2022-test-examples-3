package ru.yandex.market.notification.mail.service.factory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import javax.mail.internet.InternetAddress;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import ru.yandex.market.notification.exception.address.InvalidAddressException;
import ru.yandex.market.notification.mail.model.address.EmailAddress;
import ru.yandex.market.notification.mail.model.address.EmailAddress.Type;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link InternetAddressFactory}.
 */
public class InternetAddressFactoryTest {

    private static final String PERSONAL = "Яндекс.Маркет";
    private static final String EMAIL_WITH_PERSONAL_FORMAT = PERSONAL + " <%s>";

    private static final String[] EMAIL_CORRECT = {
        "vbauer@yandex-team.ru",
    };

    private static final String[] EMAIL_INCORRECT = {
        "I am not email address",
        "a@b",
    };


    @Test
    public void testCreateEmailPositive() {
        for (final Type type : Type.values()) {
            for (final String email : EMAIL_CORRECT) {
                assertThat(createEmail(email, type), equalTo(email));
            }
        }
    }

    @Test
    public void testCreateEmailNegative() {
        for (final Type type : Type.values()) {
            for (final String email : EMAIL_INCORRECT) {
                try {
                    final String validEmail = createEmail(email, type);
                    fail("Email address should not be valid " + validEmail);
                } catch (final InvalidAddressException ignored) {
                }
            }
        }
    }

    @Test
    public void testCreateInternetAddressPositive() {
        for (final String email : EMAIL_CORRECT) {
            final InternetAddress address = InternetAddressFactory.createInternetAddress(email);

            assertThat(address, notNullValue());
            assertThat(address.getAddress(), equalTo(email));
        }
    }

    @Test
    public void testCreateInternetAddressWithPersonal() {
        for (final String email : EMAIL_CORRECT) {
            final String emailWithPersonal = String.format(EMAIL_WITH_PERSONAL_FORMAT, email);
            final InternetAddress address = InternetAddressFactory.createInternetAddress(emailWithPersonal);

            assertThat(address, notNullValue());
            assertThat(address.getAddress(), equalTo(email));
            assertThat(address.getPersonal(), equalTo(PERSONAL));
        }
    }

    @Test
    public void testCreateInternetAddressNegative() {
        for (final String email : EMAIL_INCORRECT) {
            try {
                final InternetAddress address = InternetAddressFactory.createInternetAddress(email);
                fail("Internet address should be invalid " + address);
            } catch (final InvalidAddressException ignored) {
            }
        }
    }

    @Test
    public void testCreateInternetAddressesPositive() {
        final List<String> emails = asList(EMAIL_CORRECT);
        final InternetAddress[] addresses = InternetAddressFactory.createInternetAddresses(emails);

        assertThat(addresses.length, equalTo(emails.size()));

        IntStream.range(0, emails.size())
            .forEach(i -> assertThat(addresses[i].getAddress(), equalTo(emails.get(i))));
    }

    @Test(expected = InvalidAddressException.class)
    public void testCreateInternetAddresses() {
        final List<String> emails = ImmutableList.<String>builder()
            .add(EMAIL_CORRECT)
            .addAll(asList(EMAIL_INCORRECT))
            .build();

        fail(Arrays.toString(InternetAddressFactory.createInternetAddresses(emails)));
    }

    @Test
    public void testIsValidAddress() {
        for (final Type type : Type.values()) {
            for (final String email : EMAIL_INCORRECT) {
                final EmailAddress address = EmailAddress.create(email, type);
                final boolean validAddress = InternetAddressFactory.isValidAddress(address);

                assertThat(validAddress, equalTo(false));
            }

            for (final String email : EMAIL_CORRECT) {
                final EmailAddress address = EmailAddress.create(email, type);
                assertThat(InternetAddressFactory.isValidAddress(address), equalTo(true));
            }
        }
    }

    private String createEmail(final String address, final Type type) {
        final EmailAddress emailAddress = EmailAddress.create(address, type);
        return InternetAddressFactory.createEmail(emailAddress);
    }

}
