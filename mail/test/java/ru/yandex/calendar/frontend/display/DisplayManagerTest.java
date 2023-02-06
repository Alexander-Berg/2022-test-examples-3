package ru.yandex.calendar.frontend.display;

import lombok.val;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.CalendarRequest;
import ru.yandex.calendar.CalendarRequestHandle;
import ru.yandex.calendar.RemoteInfo;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.ActionSource;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.resource.ResourceInfo;
import ru.yandex.calendar.logic.resource.SpecialResources;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.sharing.participant.ResourceParticipantInfo;
import ru.yandex.calendar.logic.sharing.participant.YandexUserParticipantInfo;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.time.MoscowTime;

import static org.assertj.core.api.Assertions.assertThat;

public class DisplayManagerTest extends AbstractConfTest {
    @Autowired
    private DisplayManager displayManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private MailSenderMock mailSender;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private EventUserDao eventUserDao;

    private Resource resource1;
    private Resource resource2;
    private TestUserInfo user1;
    private TestUserInfo user2;
    private TestUserInfo user3;
    private TestUserInfo robotResMaster;

    private CalendarRequestHandle requestHandle;

    private static final DateTime NOW = MoscowTime.dateTime(2017, 6, 19, 12, 0);

    @Before
    public void setUp() {
        requestHandle = CalendarRequest.push(
                new RemoteInfo(Option.empty(), Option.empty()),
                ActionSource.DISPLAY, "test", "", NOW.toInstant(), true);

        resource1 = testManager.cleanAndCreateResourceWithNoSyncWithExchange("rr_1", "Resource 1");
        resource1.setDisplayToken(displayManager.assignRoom(resource1.getId()).getToken());

        resource2 = testManager.cleanAndCreateResourceWithNoSyncWithExchange("rr_2", "Resource 2");
        resource2.setDisplayToken(displayManager.assignRoom(resource2.getId()).getToken());

        user1 = testManager.prepareRandomYaTeamUser(388);
        user2 = testManager.prepareRandomYaTeamUser(389);
        user3 = testManager.prepareRandomYaTeamUser(390);

        robotResMaster = testManager.prepareResourceMaster();
    }

    @After
    public void tearDown() {
        requestHandle.popSafely();
    }

    @Test
    public void keepUserDecisionAfterMoveMeetingEndTest() {
        val meeting = testManager.createDefaultEvent(user1.getUid(), "Meeting with moved end");
        testManager.addUserParticipantToEvent(meeting.getId(), user1.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(meeting.getId(), user2.getUid(), Decision.YES, false);
        testManager.addResourceParticipantToEvent(meeting.getId(), resource1);

        val meetingId = meeting.getId();
        val instanceStart = new LocalDateTime(meeting.getStartTs(), MoscowTime.TZ);
        val instanceEnd = new LocalDateTime(meeting.getEndTs().minus(Duration.standardMinutes(1)), MoscowTime.TZ);

        displayManager.moveMeetingEnd(resource1.getDisplayToken().get(), meetingId, instanceStart, instanceEnd,
            requestHandle.getActionInfo());

        val eventUserOpt = eventUserDao.findEventUserByEventIdAndUid(meeting.getId(), user2.getUid());

        assertThat(eventUserOpt.map(EventUser::getDecision))
            .contains(Decision.YES);
    }

    @Test
    public void sendLettersToOutlookersOnMoveMeetingEnd() {
        testManager.updateIsEwser(user1);

        val meeting = testManager.createDefaultEvent(user1.getUid(), "Meeting with moved end");
        testManager.addUserParticipantToEvent(meeting.getId(), user1.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(meeting.getId(), user2.getUid(), Decision.YES, false);
        testManager.addResourceParticipantToEvent(meeting.getId(), resource1);

        mailSender.clear();

        val meetingId = meeting.getId();
        val instanceStart = new LocalDateTime(meeting.getStartTs(), MoscowTime.TZ);
        val instanceEnd = instanceStart.plusHours(1).minusMinutes(30);

        displayManager.moveMeetingEnd(resource1.getDisplayToken().get(), meetingId, instanceStart, instanceEnd,
            requestHandle.getActionInfo());

        assertThat(mailSender.getEventMessageParameters())
            .hasSize(2);
        assertThat(mailSender.getEventRecipientUids())
            .containsExactlyInAnyOrder(user1.getUid(), user2.getUid());
        assertThat(mailSender.getEventMailTypes())
            .allMatch(MailType.EVENT_UPDATE::equals);

        assertThat(mailSender.getEventMessageParameters())
            .allSatisfy(params -> {
                val info = params.getEventMessageInfo();
                assertThat(new LocalDateTime(info.getEventStartTs(), MoscowTime.TZ))
                    .isEqualTo(instanceStart);
                assertThat(new LocalDateTime(info.getEventEndTs(), MoscowTime.TZ))
                    .isNotEqualTo(instanceEnd);
            });
    }

    @Test
    public void moveMeetingEndIfSeveralResources() {
        val meeting = testManager.createDefaultEvent(user1.getUid(), "Meeting multiple resources");
        testManager.addUserParticipantToEvent(meeting.getId(), user1.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(meeting.getId(), user2.getUid(), Decision.YES, false);
        testManager.addUserParticipantToEvent(meeting.getId(), user3.getUid(), Decision.YES, false);
        testManager.addResourceParticipantToEvent(meeting.getId(), resource1);
        testManager.addResourceParticipantToEvent(meeting.getId(), resource2);

        testManager.updateIsEwser(user1, user2);

        val meetingId = meeting.getId();
        val instanceStart = new LocalDateTime(meeting.getStartTs(), MoscowTime.TZ);
        var instanceEnd = instanceStart.plusHours(1).minusMinutes(1);

        mailSender.clear();

        displayManager.moveMeetingEnd(resource1.getDisplayToken().get(), meetingId, instanceStart, instanceEnd,
            requestHandle.getActionInfo());

        var updatedEvent = eventDbManager.getEventWithRelationsById(meeting.getId());
        assertThat(updatedEvent.getEndTs())
            .isEqualTo(meeting.getEndTs());
        assertThat(updatedEvent.getResources().map(ResourceInfo::getResourceId))
            .doesNotContain(resource1.getId());
        assertThat(mailSender.getEventMessageParameters())
            .hasSize(3);
        assertThat(mailSender.getEventMailTypes())
            .allMatch(MailType.EVENT_UPDATE::equals);
        assertThat(mailSender.getEventRecipientUids())
            .contains(user1.getUid(), user2.getUid(), user3.getUid());

        mailSender.clear();

        instanceEnd = instanceEnd.minus(Duration.standardMinutes(30));

        displayManager.moveMeetingEnd(resource2.getDisplayToken().get(), meetingId, instanceStart, instanceEnd,
            requestHandle.getActionInfo());

        updatedEvent = eventDbManager.getEventWithRelationsById(meeting.getId());
        assertThat(updatedEvent.getEndTs())
            .isEqualTo(meeting.getEndTs());
        assertThat(updatedEvent.getResources().map(ResourceInfo::getResourceId))
            .doesNotContain(resource2.getId());
        assertThat(mailSender.getEventMessageParameters())
            .hasSize(3);
        assertThat(mailSender.getEventMailTypes())
            .allMatch(MailType.EVENT_UPDATE::equals);
        assertThat(mailSender.getEventRecipientUids())
            .contains(user1.getUid(), user2.getUid(), user3.getUid());
    }

    @Test
    public void declineUserOrganizedMeeting() {
        val meeting = testManager.createDefaultEvent(user1.getUid(), "User organized meeting");
        testManager.addUserParticipantToEvent(meeting.getId(), user1.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(meeting.getId(), user2.getUid(), Decision.YES, false);
        testManager.addResourceParticipantToEvent(meeting.getId(), resource1);
        testManager.addResourceParticipantToEvent(meeting.getId(), resource2);

        val meetingId = meeting.getId();
        val instanceStart = new LocalDateTime(meeting.getStartTs(), MoscowTime.TZ);

        mailSender.clear();
        displayManager.declineMeeting(resource1.getDisplayToken().get(), meetingId, instanceStart,
            requestHandle.getActionInfo());

        assertThat(findUserParticipant(user1, meetingId))
            .isNotEmpty();
        assertThat(findUserParticipant(user2, meetingId))
            .isNotEmpty();
        assertThat(findResourceParticipant(resource1, meetingId))
            .isEmpty();
        assertThat(findResourceParticipant(resource2, meetingId))
            .isNotEmpty();

        assertThat(mailSender.getEventMessageParameters())
            .hasSize(2);
        assertThat(mailSender.getEventRecipientUids().unique())
            .containsExactlyInAnyOrder(user1.getUid(), user2.getUid());
        assertThat(mailSender.getEventMailTypes())
            .allMatch(MailType.RESOURCE_UNCHECKIN::equals);

        assertLastDeclineReasonIs("resource-uncheckin");
    }

    @Test
    public void declineResourceOrganizedMeeting() {
        val meeting = testManager.createDefaultEvent(user1.getUid(), "Resource organized meeting");
        testManager.addUserParticipantToEvent(meeting.getId(), robotResMaster.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(meeting.getId(), user1.getUid(), Decision.YES, false);
        testManager.addUserParticipantToEvent(meeting.getId(), user2.getUid(), Decision.YES, false);
        testManager.addResourceParticipantToEvent(meeting.getId(), resource2);

        val meetingId = meeting.getId();
        val instanceStart = new LocalDateTime(meeting.getStartTs(), MoscowTime.TZ);

        mailSender.clear();
        displayManager.declineMeeting(resource2.getDisplayToken().get(), meetingId, instanceStart,
            requestHandle.getActionInfo());

        assertThat(eventDbManager.getEventByIdSafe(meetingId))
            .isEmpty();

        assertThat(mailSender.getEventMessageParameters())
            .hasSize(3);
        assertThat(mailSender.getEventRecipientUids().unique())
            .containsExactlyInAnyOrder(user1.getUid(), user2.getUid(), robotResMaster.getUid());
        assertThat(mailSender.getEventMailTypes())
            .allMatch(MailType.EVENT_CANCEL::equals);
    }

    @Test
    public void totalDeclineUserOrganizedMeeting() {
        val start = NOW.minusDays(1);

        val master = testManager.createDefaultEvent(user1.getUid(), "User organized repeating meeting", start);
        testManager.addUserParticipantToEvent(master.getId(), user1.getUid(), Decision.YES, true);
        testManager.addResourceParticipantToEvent(master.getId(), resource1);

        testManager.createDailyRepetitionAndLinkToEvent(master.getId());
        val recurrence = testManager.createDefaultRecurrence(user1.getUid(), master.getId(), start.plusDays(20));
        testManager.addUserParticipantToEvent(recurrence.getId(), user1.getUid(), Decision.YES, true);
        testManager.addResourceParticipantToEvent(recurrence.getId(), resource1);

        val pastRecurrence = testManager.createDefaultRecurrence(user1.getUid(), master.getId(), start);
        testManager.addUserParticipantToEvent(pastRecurrence.getId(), user1.getUid(), Decision.YES, true);
        testManager.addResourceParticipantToEvent(pastRecurrence.getId(), resource1);

        var instanceStart = new LocalDateTime(start, MoscowTime.TZ).plusDays(1);

        for (int i = 1; i < SpecialResources.getDisplaySettings(resource1).getTimesToTotalDecline(); ++i) {
            displayManager.declineMeeting(resource1.getDisplayToken().get(), master.getId(), instanceStart,
                requestHandle.getActionInfo());
            assertThat(findResourceParticipant(resource1, master.getId()))
                .isNotEmpty();
            assertThat(findResourceParticipant(resource1, recurrence.getId()))
                .isNotEmpty();

            assertLastDeclineReasonIs("resource-uncheckin");
            instanceStart = instanceStart.plusDays(1);
        }
        displayManager.declineMeeting(resource1.getDisplayToken().get(), master.getId(), instanceStart,
            requestHandle.getActionInfo());
        assertThat(findResourceParticipant(resource1, master.getId()))
            .isNotEmpty();
        assertThat(findResourceParticipant(resource1, pastRecurrence.getId()))
            .isNotEmpty();
        assertThat(findResourceParticipant(resource1, recurrence.getId()))
            .isEmpty();

        assertLastDeclineReasonIs("resource-total-uncheckin");
    }

    @Test
    public void skipDeclineOnDayOff() {
        val meeting = testManager.createDefaultEvent(user1.getUid(), "Meeting on day off",
                MoscowTime.dateTime(2017, 2, 23, 14, 0));

        testManager.addUserParticipantToEvent(meeting.getId(), user1.getUid(), Decision.YES, true);
        testManager.addResourceParticipantToEvent(meeting.getId(), resource1);

        val meetingId = meeting.getId();
        val instanceStart = new LocalDateTime(meeting.getStartTs(), MoscowTime.TZ);
        displayManager.declineMeeting(resource1.getDisplayToken().get(), meetingId, instanceStart,
            requestHandle.getActionInfo());

        val event = eventDbManager.getEventWithRelationsById(meetingId);

        assertThat(event.getResourceIds())
            .contains(resource1.getId());
    }

    public Option<YandexUserParticipantInfo> findUserParticipant(TestUserInfo user, long eventId) {
        return eventInvitationManager
                .getParticipantByEventIdAndParticipantId(eventId, ParticipantId.yandexUid(user.getUid()))
                .uncheckedCast();
    }

    public Option<ResourceParticipantInfo> findResourceParticipant(Resource resource, long eventId) {
        return eventInvitationManager
                .getParticipantByEventIdAndParticipantId(eventId, ParticipantId.resourceId(resource.getId()))
                .uncheckedCast();
    }

    private void assertLastDeclineReasonIs(String reason) {
        val element = mailSender.getEventMessageParameters().last().toOldStyleXml();
        assertThat(element.getChild("special-reason").getText())
            .isEqualTo(reason);
    }
}
