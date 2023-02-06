package ru.yandex.market.pers.notify.staff;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import ru.yandex.market.pers.notify.ems.MailAttachment;
import ru.yandex.market.pers.notify.model.SenderTemplate;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.test.RunInTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@RunInTesting
public class StaffWelcomeTestingRedirectionTest extends AbstractStaffWelcomeMailTest {
    @Value("${testing.redirect.address}")
    private String redirectAddress;

    @Test
    void welcomeMailShouldBeRedirectedInTesting() throws Exception {
        scheduleAndProcessWelcomeMails();

        verify(senderClient, never()).sendTransactionalMail(eq(EXPECTED_EMAIL), anyObject(), anyBoolean(),
                anyMapOf(String.class, Object.class), anyListOf(MailAttachment.class));

        verify(senderClient).sendTransactionalMail(eq(redirectAddress), eq(SenderTemplate.MARKET_STAFF_WELCOME), anyBoolean(),
                anyMapOf(String.class, Object.class), anyListOf(MailAttachment.class));

        assertEquals(NotificationEventStatus.SENT, eventSourceDAO.getLastEventByEmail(EXPECTED_EMAIL).getStatus());

    }
}
