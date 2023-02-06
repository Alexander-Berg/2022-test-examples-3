package ru.yandex.market.pers.notify.staff;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.pers.notify.ems.MailAttachment;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.SenderTemplate;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

class StaffWelcomeMailTest extends AbstractStaffWelcomeMailTest {

    private static final String STAFF_TWO_PERSONS_RESPONSE_JSON = "/data/staff/staff_two_persons_response.json";
    private static final String ANOTHER_EXPECTED_EMAIL = "pupkin@yandex-team.ru";


    @Test
    void mailWithNameShouldBeSent() throws Exception {
        scheduleAndProcessWelcomeMails();

        ArgumentCaptor<Map<String, Object>> modelCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
        verify(senderClient).sendTransactionalMail(
                eq(EXPECTED_EMAIL),
                eq(SenderTemplate.MARKET_STAFF_WELCOME),
                eq(true),
                modelCaptor.capture(),
                anyListOf(MailAttachment.class)
        );

        Map<String, Object> model = modelCaptor.getValue();
        assertEquals("Алексей", model.get("name"));

        assertEquals(NotificationEventStatus.SENT, eventSourceDAO.getLastEventByEmail(EXPECTED_EMAIL).getStatus());
    }

    @Test
    void onlyOneMailPerPersonShouldBeScheduled() throws Exception {
        expectCallToStaff(STAFF_ONE_PERSON_RESPONSE_JSON);
        staffWelcomeService.scheduleStaffWelcomeMails(DATE_2015_11_03);

        expectCallToStaff(STAFF_TWO_PERSONS_RESPONSE_JSON);
        staffWelcomeService.scheduleStaffWelcomeMails(DATE_2015_11_03);

        assertSingleEventForEmail(EXPECTED_EMAIL);
        assertSingleEventForEmail(ANOTHER_EXPECTED_EMAIL);
    }

    private void assertSingleEventForEmail(String email) {
        Integer eventsCount = eventSourceDAO
                .getEventsCount(email, NotificationSubtype.MARKET_STAFF_WELCOME, NotificationEventStatus.NEW);
        assertEquals(1, eventsCount.intValue());
    }


}
