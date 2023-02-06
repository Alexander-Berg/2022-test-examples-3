package ru.yandex.market.notification.exception;

import java.util.Collection;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.notification.common.TypedRegistry;
import ru.yandex.market.notification.exception.address.AddressException;
import ru.yandex.market.notification.exception.address.InvalidAddressException;
import ru.yandex.market.notification.exception.address.MissedAddressException;
import ru.yandex.market.notification.exception.template.TemplateIOException;
import ru.yandex.market.notification.exception.template.TemplateRenderingException;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit-тесты для проверки исключений.
 *
 * @author Vladislav Bauer
 */
@RunWith(Parameterized.class)
public class ExceptionsTest {

    private final Class<? extends Throwable> parentExceptionClass;
    private final Collection<Throwable> exceptions;


    public ExceptionsTest(
        @Nonnull final Class<? extends Throwable> parentExceptionClass,
        @Nonnull final Collection<Throwable> exceptions
    ) {
        this.parentExceptionClass = parentExceptionClass;
        this.exceptions = exceptions;
    }


    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return asList(
            data(
                NotificationException.class,
                new AddressException(""), new AddressException("", ex())
            ),
            data(
                AddressException.class,
                new InvalidAddressException(""), new InvalidAddressException("", ex())
            ),
            data(
                AddressException.class,
                new MissedAddressException(""), new MissedAddressException("", ex())
            ),

            data(
                NotificationException.class,
                new TemplateIOException(""), new TemplateIOException("", ex())
            ),
            data(
                NotificationException.class,
                new TemplateRenderingException(""), new TemplateRenderingException("", ex())
            ),

            data(
                NotificationException.class,
                new ExternalServiceHttpClientException(""), new ExternalServiceHttpClientException("", ex())
            ),
            data(
                RuntimeException.class,
                new NotificationException(""), new NotificationException("", ex())
            ),
            data(
                NotificationException.class,
                new ObjectSerializationException(""), new ObjectSerializationException("", ex())
            ),
            data(
                NotificationException.class,
                new RegistryElementNotFoundException(mock(TypedRegistry.class), new Object())
            ),
            data(
                NotificationException.class,
                new TransportException(""), new TransportException("", ex())
            ),
            data(
                NotificationException.class,
                new InvalidTypeException(""), new InvalidTypeException("", ex())
            )
        );
    }


    @Test
    public void test() {
        for (final Throwable exception : exceptions) {
            assertThat(exception, instanceOf(parentExceptionClass));
        }
    }


    private static Object[] data(final Class<? extends Throwable> parentExceptionClass, final Throwable... exceptions) {
        return new Object[] { parentExceptionClass, asList(exceptions) };
    }

    private static Exception ex() {
        return new Exception();
    }

}
