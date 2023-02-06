package ru.yandex.market.pers.notify.push;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pers.notify.ems.NotificationProcessor;
import ru.yandex.market.pers.notify.ems.persistence.NotificationEventService;
import ru.yandex.market.pers.notify.ems.service.MailerNotificationEventService;
import ru.yandex.market.pers.notify.model.NotificationSubtype;
import ru.yandex.market.pers.notify.model.event.NotificationEventDataName;
import ru.yandex.market.pers.notify.model.event.NotificationEventSource;
import ru.yandex.market.pers.notify.model.event.NotificationEventStatus;
import ru.yandex.market.pers.notify.model.push.MobileAppInfo;
import ru.yandex.market.pers.notify.model.push.MobilePlatform;
import ru.yandex.market.pers.notify.model.push.PushDeeplink;
import ru.yandex.market.pers.notify.model.push.PushTemplateType;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyVararg;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GradeCommentedPushTest extends MarketMailerMockedDbTest {
    private static final long USER_ID = 12345L;
    private static final long VOTE_ID = 5678L;
    private static final long GRADE_ID = 7890L;
    private static final String UUID = "some-uuid";
    @Autowired
    @Qualifier("pushProcessor")
    private NotificationProcessor pushProcessor;

    @Autowired
    private NotificationEventService notificationEventService;
    @Autowired
    private MailerNotificationEventService mailerNotificationEventService;
    @Autowired
    private MobileAppInfoDAO mobileAppInfoDAO;
    @Autowired
    @Qualifier("marketPusherService")
    private PusherService marketPusherServiceMock;

    @BeforeEach
    public void setUp() {
        mobileAppInfoDAO.add(new MobileAppInfo(USER_ID, UUID, "app", "token", MobilePlatform.ANDROID, false, new Date()));
    }

    @Test
    public void pushMustBeSentForValidVote() {
        when(gradeClient.checkVoteExistsForMailer(VOTE_ID)).thenReturn(true);
        long eventId = notificationEventService.addEvent(createEvent()
            .addDataParam(NotificationEventDataName.VOTE_ID, String.valueOf(VOTE_ID))
            .build()).getId();
        pushProcessor.process();
        verify(marketPusherServiceMock).push(eq(USER_ID), anyString(), eq(NotificationSubtype.PUSH_GRADE_VOTED), isNull(String.class), anyString(), anyVararg());
        assertEquals(NotificationEventStatus.SENT, mailerNotificationEventService.getEvent(eventId).getStatus());
    }

    @Test
    public void pushMustBeSentByYandexUid() {
        assertTrue(mobileAppInfoDAO.add(
            new MobileAppInfo(USER_ID, "123", "app", "someYandexUid", "push_token", MobilePlatform.ANDROID, false, new Date(), 1L, 2L, true)));

        when(gradeClient.checkVoteExistsForMailer(VOTE_ID)).thenReturn(true);
        long eventId = notificationEventService.addEvent(
            NotificationEventSource.pushFromYandexUid("someYandexUid", PushTemplateType.GRADE_VOTED)
                .addTemplateParam(NotificationEventDataName.MESSAGE, "")
                .addDeeplink(PushDeeplink.REVIEWS)
                .setSourceId(GRADE_ID)
                .setSendTime(Date.from(Instant.now()))
                .addDataParam(NotificationEventDataName.VOTE_ID, String.valueOf(VOTE_ID))
                .build()).getId();
        pushProcessor.process();
        verify(marketPusherServiceMock).push(eq(USER_ID), eq("123"), eq(NotificationSubtype.PUSH_GRADE_VOTED), isNull(String.class), anyString(), anyVararg());
        assertEquals(NotificationEventStatus.SENT, mailerNotificationEventService.getEvent(eventId).getStatus());
    }

    @Test
    public void pushMustNotBeSentForInvalidVote() {
        when(gradeClient.checkVoteExistsForMailer(VOTE_ID)).thenReturn(false);
        long eventId = notificationEventService.addEvent(createEvent()
            .addDataParam(NotificationEventDataName.VOTE_ID, String.valueOf(VOTE_ID))
            .build()).getId();
        pushProcessor.process();
        verify(marketPusherServiceMock, never()).push(anyLong(), anyString(), any(NotificationSubtype.class), anyString(), anyString(), anyVararg());
        assertEquals(NotificationEventStatus.REJECTED_AS_UNNECESSARY, mailerNotificationEventService.getEvent(eventId).getStatus());
    }

    @Test
    public void pushMustNotBeSentIfVoteIdIsMissing() {
        when(gradeClient.checkVoteExistsForMailer(VOTE_ID)).thenReturn(false);
        long eventId = notificationEventService.addEvent(createEvent().build()).getId();
        pushProcessor.process();
        verify(marketPusherServiceMock, never()).push(anyLong(), anyString(), any(NotificationSubtype.class), anyString(), anyString(), anyVararg());
        assertEquals(NotificationEventStatus.REJECTED_AS_UNNECESSARY, mailerNotificationEventService.getEvent(eventId).getStatus());
    }

    private NotificationEventSource.PushBuilder createEvent() {
        return NotificationEventSource
            .pushFromUid(USER_ID, PushTemplateType.GRADE_VOTED)
            .addTemplateParam(NotificationEventDataName.MESSAGE, "")
            .addDeeplink(PushDeeplink.REVIEWS)
            .setSourceId(GRADE_ID)
            .setSendTime(Date.from(Instant.now()));
    }

}
