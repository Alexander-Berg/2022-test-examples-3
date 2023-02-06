package ru.yandex.calendar.logic.event.meeting;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.LayerUser;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.sending.param.CancelEventMessageParameters;
import ru.yandex.calendar.logic.sending.real.MailSender;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.test.auto.db.util.TestLayerCollLastUpdateChecker;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractEwsExportedLoginsTest;
import ru.yandex.misc.test.Assert;

/**
 * Details: http://wiki.yandex-team.ru/Calendar/sharing/messages
 * @author ssytnik
 */
public class CancelMeetingHandlerTest extends AbstractEwsExportedLoginsTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private MailSender mailSender;
    @Autowired
    private TestLayerCollLastUpdateChecker testLayerCollLastUpdateChecker;

    public CancelMeetingHandlerTest(EwsUsage ewsUsage) {
        super(ewsUsage);
    }

    @Test
    @WantsEws
    public void cancelMeeting() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(12700);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(12701);
        TestUserInfo subscriber = testManager.prepareRandomYaTeamSuperUser(12702);

        setIsEwserIfNeeded(organizer);

        Instant now = TestDateTimes.moscow(2011, 11, 21, 21, 28);
        long organizerLayer = testManager
                .createDefaultLayerForUser(organizer.getUid(), now.minus(Duration.standardDays(1)));
        long attendeeLayer = testManager
                .createDefaultLayerForUser(attendee.getUid(), now.minus(Duration.standardDays(1)));

        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;
        mailSenderMock.clear();

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "cancelMeeting");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);
        setIsExportedWithEwsIfNeeded(event);

        testManager.addSubscriberToEvent(event.getId(), subscriber.getUid());

        eventRoutines.deleteEvent(
                Option.of(organizer.getUserInfo()), event.getId(),
                InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest(now));

        ListF<CancelEventMessageParameters> cancelList = mailSenderMock.getCancelEventMessageParameterss();

        if (isEwser(organizer)) {
            Assert.hasSize(1, cancelList);
            Assert.some(MailType.EVENT_CANCEL, mailSenderMock.findEventMailType(subscriber.getEmail()));
        } else {
            Assert.hasSize(3, cancelList);
            Assert.some(MailType.EVENT_CANCEL, mailSenderMock.findEventMailType(attendee.getEmail()));
            Assert.some(MailType.EVENT_CANCEL, mailSenderMock.findEventMailType(organizer.getEmail()));
            Assert.some(MailType.EVENT_CANCEL, mailSenderMock.findEventMailType(subscriber.getEmail()));
        }
        testLayerCollLastUpdateChecker.assertUpdated(Cf.list(organizerLayer, attendeeLayer), now);
    }

    @Test
    @WantsEws
    public void cancelMeetingByAttendee() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(12700);
        TestUserInfo attendee = testManager.prepareRandomYaTeamUser(12701);
        TestUserInfo subscriber = testManager.prepareRandomYaTeamSuperUser(12702);

        setIsEwserIfNeeded(organizer);

        Instant now = TestDateTimes.moscow(2011, 11, 21, 21, 28);

        MailSenderMock mailSenderMock = (MailSenderMock) mailSender;
        mailSenderMock.clear();

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "cancelMeeting");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);
        setIsExportedWithEwsIfNeeded(event);

        testManager.addSubscriberToEvent(event.getId(), subscriber.getUid());

        LayerUser layerUserOverrides = new LayerUser();
        layerUserOverrides.setPerm(LayerActionClass.ADMIN);
        layerRoutines.createLayerUserForUserAndLayer(attendee.getUid(), organizer.getDefaultLayerId(),
                layerUserOverrides, Cf.<Notification>list());

        eventRoutines.deleteEvent(
                Option.of(attendee.getUserInfo()), event.getId(),
                InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest(now));

        ListF<CancelEventMessageParameters> cancelList = mailSenderMock.getCancelEventMessageParameterss();

        if (isEwser(organizer)) {
            Assert.hasSize(1, cancelList);
            Assert.some(MailType.EVENT_CANCEL, mailSenderMock.findEventMailType(subscriber.getEmail()));
        } else {
            Assert.hasSize(3, cancelList);
            Assert.some(MailType.EVENT_CANCEL, mailSenderMock.findEventMailType(attendee.getEmail()));
            Assert.some(MailType.EVENT_CANCEL, mailSenderMock.findEventMailType(organizer.getEmail()));
            Assert.some(MailType.EVENT_CANCEL, mailSenderMock.findEventMailType(subscriber.getEmail()));
        }

        testLayerCollLastUpdateChecker.assertUpdated(
                Cf.list(organizer.getDefaultLayerId(), attendee.getDefaultLayerId()), now);
    }
}
