package ru.yandex.qe.mail.meetings.ws;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.qe.mail.meetings.api.resource.CalendarActions;
import ru.yandex.qe.mail.meetings.api.resource.dto.MergeEventRequest;
import ru.yandex.qe.mail.meetings.cron.actions.MockConfiguration;
import ru.yandex.qe.mail.meetings.services.calendar.dto.WebEventData;
import ru.yandex.qe.mail.meetings.ws.mock.CalendarUpdateMock;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Sergey Galyamichev
 */
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MockConfiguration.class})
public class CalendarActionsTest {
    @Inject
    private CalendarActions calendarActions;
    @Inject
    private CalendarUpdateMock calendarUpdateMock;
    @Test
    public void mergeTest() {
        try {
            MergeEventRequest request = new MergeEventRequest();
            request.setMainEventUrl("https://calendar.yandex-team.ru/event/43957666");
            request.setOtherEventUrl("https://calendar.yandex-team.ru/event/44005372");
            calendarActions.mergeEvent("g-s-v", request);
            assertTrue(calendarUpdateMock.isDeleted(44005372));
            WebEventData data = calendarUpdateMock.getUpdated(43957666);
            assertNotNull(data);
        } catch (Exception e) {
            fail("Not expected");
        }
    }

    @Test
    public void noEventTest() {
        try {
            MergeEventRequest request = new MergeEventRequest();
            request.setMainEventUrl("https://calendar.yandex-team.ru/event/43957666");
            request.setOtherEventUrl("https://calendar.yandex-team.ru/event/50000000");
            calendarActions.mergeEvent("g-s-v", request);
        } catch (Exception e) {
            fail("Not expected");
        }
    }
}
