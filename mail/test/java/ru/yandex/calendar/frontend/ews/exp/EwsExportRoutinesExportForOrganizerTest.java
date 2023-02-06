package ru.yandex.calendar.frontend.ews.exp;

import com.microsoft.schemas.exchange.services._2006.types.AttendeeType;
import com.microsoft.schemas.exchange.services._2006.types.CalendarItemType;
import com.microsoft.schemas.exchange.services._2006.types.ResponseTypeType;
import com.microsoft.schemas.exchange.services._2006.types.UnindexedFieldURIType;
import lombok.val;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.ews.imp.ExchangeEventDataConverter;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxy;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventInvitationManager;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.event.model.WebReplyData;
import ru.yandex.calendar.logic.sending.real.MailSender;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.UserParticipantInfo;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.misc.time.InstantInterval;
import ru.yandex.misc.time.MoscowTime;

public class EwsExportRoutinesExportForOrganizerTest extends AbstractConfTest {
    private static final Duration EWS_INNER_REACTION_TIME = Duration.standardSeconds(3);
    private static final Duration EWS_NO_REACTION_TIME = Duration.standardSeconds(10);
    private static final int RETRIES = 50;

    @Value("${ews.domain}")
    private String ewsDomain;

    @Autowired
    private TestManager testManager;
    @Autowired
    private EwsExportRoutines ewsExportRoutines;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private EwsProxy ewsProxy;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private UserManager userManager;
    @Autowired
    private MailSender mailSender;
    @Autowired
    private EventInvitationManager eventInvitationManager;
    @Autowired
    private EventUserDao eventUserDao;

    private TestUserInfo organizer;
    private TestUserInfo ewsAttendee;
    private TestUserInfo nonEwsAttendee;

    private final Instant eventStart = MoscowTime.instant(2016, 3, 12, 14, 0);
    private final Instant eventEnd = eventStart.plus(Duration.standardMinutes(15));

    private final ActionInfo actionInfo = ActionInfo.webTest(eventStart.minus(77777));

    private void giveExchangeEnoughTimeToReact(Runnable assertActions) {
        // required if the action includes mails, sent by exchange within himself
        // e.g. invite an exchange user to a meeting
        for (int retry = 0; retry < RETRIES; retry++) {
            try {
                assertActions.run();
                return;
            } catch (AssertionError e) {
                ThreadUtils.sleep(EWS_INNER_REACTION_TIME);
            }
        }
        assertActions.run();
    }

    private Email setEwsDomain(Email email) {
        return new Email(email.getLocalPart() + "@" + ewsDomain);
    }

    @Before
    public void cleanBeforeTest() {
        organizer = testManager.prepareYandexUser(TestManager.createYashunsky());
        ewsAttendee = testManager.prepareRandomYaTeamUser(2211, "conf_rr_3_1_2013");
        nonEwsAttendee = testManager.prepareRandomYaTeamUser(2212);

        testManager.updateIsEwser(organizer, ewsAttendee);

        ewsProxyWrapper.cancelMasterAndSingleMeetings(setEwsDomain(organizer.getEmail()), eventStart, eventEnd);

        ewsProxyWrapper.cancelMasterAndSingleMeetings(setEwsDomain(ewsAttendee.getEmail()), eventStart, eventEnd);
    }

    @Test
    public void createMeetingWithEwsAttendee() {
        createMeetingWithAttendee(true);
    }

    @Test
    public void createMeetingWithNonEwsAttendee() {
        createMeetingWithAttendee(false);
    }

    private Event createEventAndExport(String subject, TestUserInfo attendee) {
        Event event = testManager.createDefaultEvent(
                organizer.getUid(), subject, eventStart, eventEnd);

        mainEventDao.updateIsExportedWithEwsById(event.getMainEventId(), true);

        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.UNDECIDED, false);

        ewsExportRoutines.exportToExchangeIfNeededOnCreate(event.getId(), actionInfo);

        return event;
    }

    private CalendarItemType getOrganizerItem(long eventId) {
        Option<String> exchangeId = eventUserDao.findEventUserByEventIdAndUid(eventId, organizer.getUid())
                .filterMap(EventUser::getExchangeId);

        Assert.some(exchangeId);

        ListF<CalendarItemType> organizerItems = ewsProxyWrapper.getEvents(exchangeId, Cf.list(
                UnindexedFieldURIType.ITEM_SUBJECT,
                UnindexedFieldURIType.CALENDAR_ORGANIZER,
                UnindexedFieldURIType.CALENDAR_REQUIRED_ATTENDEES));

        Assert.hasSize(1, organizerItems);

        return organizerItems.single();
    }

    private void createMeetingWithAttendee(boolean isEwsAttendee) {
        TestUserInfo attendee = isEwsAttendee ? ewsAttendee : nonEwsAttendee;

        String subject = "meeting with " + (isEwsAttendee ? "" : "non") + "exchange attendee";

        Event event = createEventAndExport(subject, attendee);

        CalendarItemType organizerItem = getOrganizerItem(event.getId());

        Assert.equals(subject, organizerItem.getSubject());

        ListF<Email> attendeesEmails = Cf.toList(organizerItem.getRequiredAttendees().getAttendee())
                .map(a -> a.getMailbox().getEmailAddress()).map(Email::new);

        Assert.hasSize(1, attendeesEmails);
        Assert.assertContains(ExchangeEventDataConverter.getOrganizerEmailSafe(organizerItem), organizer.getEmail());

        giveExchangeEnoughTimeToReact(() -> {
            Option<InstantInterval> interval = Option.of(new InstantInterval(eventStart, eventEnd));

            if (!isEwsAttendee) {
                ThreadUtils.sleep(EWS_NO_REACTION_TIME);
            }

            ListF<CalendarItemType> attendeeItems =
                    ewsProxy.findMasterAndSingleEvents(userManager.getLdEmailByUid(attendee.getUid()), interval, false);

            if (isEwsAttendee) {
                Assert.hasSize(1, attendeeItems);
                Assert.equals(subject, attendeeItems.single().getSubject());
                Assert.assertContains(attendeesEmails, setEwsDomain(attendee.getEmail()));
            } else {
                Assert.hasSize(0, attendeeItems);
                Assert.assertContains(attendeesEmails, attendee.getEmail());
            }
        });
    }

    // YES and MAYBE decisions are processed the same way
    @Test
    public void acceptInvitationByEwsAttendee() {
        makeDecision(true, Decision.YES);
    }

    @Test
    public void acceptInvitationByNonEwsAttendee() {
        makeDecision(false, Decision.YES);
    }

    @Test
    public void declineInvitationByEwsAttendee() {
        makeDecision(true, Decision.NO);
    }

    @Test
    public void declineInvitationByNonEwsAttendee() {
        makeDecision(false, Decision.NO);
    }

    private void makeDecision(boolean isEwsAttendee, Decision decision) {
        TestUserInfo attendee = isEwsAttendee ? ewsAttendee : nonEwsAttendee;
        String subject = "meeting with " + (isEwsAttendee ? "" : "non") + "exchange attendee with changed decision";
        Event event = createEventAndExport(subject, attendee);
        if (isEwsAttendee) {
            giveExchangeEnoughTimeToReact(() -> {
                InstantInterval interval = new InstantInterval(event.getStartTs(), event.getEndTs());
                String externalId = mainEventDao.findMainEventByEventId(event.getId()).getExternalId();
                Assert.hasSize(1, ewsProxyWrapper.findMasterAndSingleEventIdsByExternalId(
                        userManager.getLdEmailByUid(ewsAttendee.getUid()), interval, externalId));
            });
        }

        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;
        mailSenderMock.clear();

        eventInvitationManager.handleEventInvitationDecision(attendee.getUid(),
                getParticipant(event, attendee.getEmail()), new WebReplyData(decision), false, actionInfo);

        if (isEwsAttendee) {
            giveExchangeEnoughTimeToReact(() -> {
                val parameters = mailSenderMock.getReplyMessageParameterss();
                Assert.hasSize(0, parameters);

                val organizerItem = getOrganizerItem(event.getId());
                ListF<ResponseTypeType> responseTypes = Cf.toList(organizerItem.getRequiredAttendees().getAttendee())
                        .filter(a -> setEwsDomain(attendee.getEmail())
                                .equalsIgnoreCase(new Email(a.getMailbox().getEmailAddress())))
                        .map(AttendeeType::getResponseType);

                Assert.hasSize(1, responseTypes);
                Assert.equals(responseTypes.single(), decision.getRespType());
            });
        } else {
            val parameters = mailSenderMock.getReplyMessageParameterss();
            Assert.equals(organizer.getEmail(), parameters.single().getRecipientEmail());
        }
    }

    private UserParticipantInfo getParticipant(Event event, Email email) {
        return (UserParticipantInfo) eventInvitationManager.getParticipantByEventIdAndEmail(event.getId(), email).get();
    }
}
