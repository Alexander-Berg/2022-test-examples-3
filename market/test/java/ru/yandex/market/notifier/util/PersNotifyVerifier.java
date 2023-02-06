package ru.yandex.market.notifier.util;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockito.verification.VerificationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.market.pers.notify.PersNotifyClient;
import ru.yandex.market.pers.notify.PersNotifyClientException;
import ru.yandex.market.pers.notify.model.NotificationTransportType;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Component
public class PersNotifyVerifier {

    @Autowired
    private PersNotifyClient persNotifyClient;

    public void verifyMailSent() {
        verifyMailSent(times(1));
    }

    public void verifyMailSent(int times) {
        verifyMailSent(times(times));
    }

    public void verifyMailSent(VerificationMode verificationMode) {
        try {
            verify(persNotifyClient, verificationMode).createEvent(
                    MockitoHamcrest.argThat(isEventForTransport(NotificationTransportType.MAIL))
            );
        } catch (PersNotifyClientException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void verifyMobilePushSent() {
        verifyMobilePushSent(times(1));
    }

    public void verifyMobilePushSent(VerificationMode verificationMode) {
        try {
            verify(persNotifyClient, verificationMode).createEvent(
                    MockitoHamcrest.argThat(isEventForTransport(NotificationTransportType.PUSH)));
        } catch (PersNotifyClientException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    static Matcher<NotificationEventSource> isEventForTransport(NotificationTransportType type) {
        return new BaseMatcher<>() {
            @Override
            public boolean matches(Object o) {
                if (!(o instanceof NotificationEventSource)) {
                    return false;
                }
                return ((NotificationEventSource) o).getNotificationSubtype().getTransportType() == type;
            }

            @Override
            public void describeTo(Description description) {

            }
        };
    }
}
