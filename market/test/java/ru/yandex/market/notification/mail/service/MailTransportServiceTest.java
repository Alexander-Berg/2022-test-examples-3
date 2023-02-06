package ru.yandex.market.notification.mail.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import ru.yandex.market.notification.mail.model.EmailContent;
import ru.yandex.market.notification.mail.model.address.ComposedEmailAddress;
import ru.yandex.market.notification.mail.model.address.EmailAddress;
import ru.yandex.market.notification.mail.service.factory.MimeMessagePreparatorFactory;
import ru.yandex.market.notification.model.context.NotificationTransportContext;
import ru.yandex.market.notification.model.transport.NotificationAddress;
import ru.yandex.market.notification.model.transport.result.NotificationResult;
import ru.yandex.market.notification.service.NotificationTransportService;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link MailTransportService}.
 *
 * @author Vladislav Bauer
 */
public class MailTransportServiceTest {

    @Test
    public void testWrongAddresses() {
        checkWrongAddresses(Collections.emptySet());
        checkWrongAddresses(Collections.singleton(mock(EmailAddress.class)));
    }

    @Test
    public void testCorrectSending() {
        final MimeMessagePreparatorFactory factory = createMimeMessagePreparatorFactory();
        final JavaMailSender sender = mock(JavaMailSender.class);

        final NotificationTransportService transportService = new MailTransportService(factory, sender);
        final NotificationTransportContext context = createContext(createAddresses());
        final NotificationResult result = transportService.send(context);

        checkResult(result, 1, 0);
        checkSendingAttempt(factory, sender);
    }

    @Test
    public void testBadSending() {
        final MimeMessagePreparatorFactory factory = createMimeMessagePreparatorFactory();
        final JavaMailSender sender = mock(JavaMailSender.class);

        doThrow(RuntimeException.class).when(sender).send(any(MimeMessagePreparator.class));

        final NotificationTransportService transportService = new MailTransportService(factory, sender);
        final NotificationTransportContext context = createContext(createAddresses());
        final NotificationResult result = transportService.send(context);

        checkResult(result, 0, 1);
        checkSendingAttempt(factory, sender);
    }

    @Test
    public void testBadPreparing() {
        final JavaMailSender sender = mock(JavaMailSender.class);
        final MimeMessagePreparatorFactory factory = createMimeMessagePreparatorFactory();

        doThrow(RuntimeException.class).when(factory)
            .createPreparator(any(EmailContent.class), any(ComposedEmailAddress.class));

        final NotificationTransportService transportService = new MailTransportService(factory, sender);
        final NotificationTransportContext context = createContext(createAddresses());
        final NotificationResult result = transportService.send(context);

        checkResult(result, 0, 1);

        verify(factory, times(1)).createPreparator(any(EmailContent.class), any(ComposedEmailAddress.class));
        verifyNoMoreInteractions(factory);
        verifyNoMoreInteractions(sender);
    }


    private void checkWrongAddresses(final Collection<NotificationAddress> addresses) {
        final JavaMailSender sender = mock(JavaMailSender.class);
        final MimeMessagePreparatorFactory factory = createMimeMessagePreparatorFactory();

        final NotificationTransportService transportService = new MailTransportService(factory, sender);
        final NotificationTransportContext context = createContext(addresses);
        final NotificationResult result = transportService.send(context);

        checkResult(result, 0, 0);

        verifyZeroInteractions(factory);
        verifyZeroInteractions(sender);
    }

    private void checkResult(final NotificationResult result, final int success, final int failed) {
        assertThat("Wrong number of successful notifications", result.getSuccessful().size(), equalTo(success));
        assertThat("Wrong number of failed notifications", result.getFailed().size(), equalTo(failed));
    }

    private void checkSendingAttempt(final MimeMessagePreparatorFactory factory, final JavaMailSender sender) {
        verify(factory, times(1)).createPreparator(any(EmailContent.class), any(ComposedEmailAddress.class));
        verify(sender, times(1)).send(any(MimeMessagePreparator.class));
        verifyNoMoreInteractions(factory);
        verifyNoMoreInteractions(sender);
    }

    private NotificationTransportContext createContext(final Collection<NotificationAddress> addresses) {
        final NotificationTransportContext context = mock(NotificationTransportContext.class);
        final EmailContent content = createContent();

        when(context.getContent()).thenReturn(content);
        when(context.getAddresses()).thenReturn(addresses);

        return context;
    }

    private MimeMessagePreparatorFactory createMimeMessagePreparatorFactory() {
        final MimeMessagePreparatorFactory factory = mock(MimeMessagePreparatorFactory.class);
        when(factory.createPreparator(any(), any())).thenReturn(mock(MimeMessagePreparator.class));
        return factory;
    }

    private EmailContent createContent() {
        final EmailContent content = mock(EmailContent.class);
        when(content.cast(any())).thenCallRealMethod();
        return content;
    }

    private Set<NotificationAddress> createAddresses() {
        final ComposedEmailAddress address = mock(ComposedEmailAddress.class);
        when(address.cast(any())).thenCallRealMethod();
        return Collections.singleton(address);
    }

}
