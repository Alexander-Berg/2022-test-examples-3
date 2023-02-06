package ru.yandex.market.notification.mail.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
        var factory = createMimeMessagePreparatorFactory();
        var sender = mock(JavaMailSender.class);

        NotificationTransportService transportService = new MailTransportService(factory, sender);
        var context = createContext(createAddresses());
        var result = transportService.send(context);

        checkResult(result, 1, 0);
        checkSendingAttempt(factory, sender);
    }

    @Test
    public void testBadSending() {
        var factory = createMimeMessagePreparatorFactory();
        var sender = mock(JavaMailSender.class);

        doThrow(RuntimeException.class).when(sender).send(any(MimeMessagePreparator.class));

        NotificationTransportService transportService = new MailTransportService(factory, sender);
        var context = createContext(createAddresses());
        var result = transportService.send(context);

        checkResult(result, 0, 1);
        checkSendingAttempt(factory, sender);
    }

    @Test
    public void testBadPreparing() {
        var sender = mock(JavaMailSender.class);
        var factory = createMimeMessagePreparatorFactory();

        doThrow(RuntimeException.class).when(factory)
            .createPreparator(any(EmailContent.class), any(ComposedEmailAddress.class));

        NotificationTransportService transportService = new MailTransportService(factory, sender);
        var context = createContext(createAddresses());
        var result = transportService.send(context);

        checkResult(result, 0, 1);

        verify(factory, times(1)).createPreparator(any(EmailContent.class), any(ComposedEmailAddress.class));
        verifyNoMoreInteractions(factory, sender);
    }


    private void checkWrongAddresses(Collection<NotificationAddress> addresses) {
        var sender = mock(JavaMailSender.class);
        var factory = createMimeMessagePreparatorFactory();

        NotificationTransportService transportService = new MailTransportService(factory, sender);
        var context = createContext(addresses);
        var result = transportService.send(context);

        checkResult(result, 0, 0);

        verifyNoInteractions(factory, sender);
    }

    private void checkResult(NotificationResult result, int success, int failed) {
        assertThat("Wrong number of successful notifications", result.getSuccessful().size(), equalTo(success));
        assertThat("Wrong number of failed notifications", result.getFailed().size(), equalTo(failed));
    }

    private void checkSendingAttempt(MimeMessagePreparatorFactory factory, JavaMailSender sender) {
        verify(factory, times(1)).createPreparator(any(EmailContent.class), any(ComposedEmailAddress.class));
        verify(sender, times(1)).send(any(MimeMessagePreparator.class));
        verifyNoMoreInteractions(factory);
        verifyNoMoreInteractions(sender);
    }

    private NotificationTransportContext createContext(Collection<NotificationAddress> addresses) {
        var context = mock(NotificationTransportContext.class);
        var content = createContent();

        when(context.getContent()).thenReturn(content);
        when(context.getAddresses()).thenReturn(addresses);

        return context;
    }

    private MimeMessagePreparatorFactory createMimeMessagePreparatorFactory() {
        var factory = mock(MimeMessagePreparatorFactory.class);
        when(factory.createPreparator(any(), any())).thenReturn(mock(MimeMessagePreparator.class));
        return factory;
    }

    private EmailContent createContent() {
        var content = mock(EmailContent.class);
        when(content.cast(any())).thenCallRealMethod();
        return content;
    }

    private Set<NotificationAddress> createAddresses() {
        var address = mock(ComposedEmailAddress.class);
        when(address.cast(any())).thenCallRealMethod();
        return Collections.singleton(address);
    }

}
