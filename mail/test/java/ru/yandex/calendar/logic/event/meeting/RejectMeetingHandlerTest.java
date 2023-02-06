package ru.yandex.calendar.logic.event.meeting;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.sending.param.EventMessageParameters;
import ru.yandex.calendar.logic.sending.real.MailSender;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestLayerCollLastUpdateChecker;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractEwsExportedLoginsTest;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;

/**
 * Details: http://wiki.yandex-team.ru/Calendar/sharing/messages
 * @author akirakozov
 */
public class RejectMeetingHandlerTest extends AbstractEwsExportedLoginsTest {
    @Autowired
    private UserManager userManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private MailSender mailSender;
    @Autowired
    private TestLayerCollLastUpdateChecker testLayerCollLastUpdateChecker;

    public RejectMeetingHandlerTest(EwsUsage ewsUsage) {
        super(ewsUsage);
    }

    @Before
    public void setMockEws() {
        setMockEwsProxyWrapper();
    }

    @Test
    @WantsEws
    public void rejectMeeting() {
        final MailSenderMock mailSenderMock = (MailSenderMock) mailSender;

        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-10471");
        TestUserInfo attendee1 = testManager.prepareUser("yandex-team-mm-10472");

        PassportUid organizerUid = organizer.getUid();
        PassportUid attendee1Uid = attendee1.getUid();
        PassportUid attendee2Uid = testManager.prepareUser("yandex-team-mm-10473").getUid();

        setIsEwserIfNeeded(Cf.list(organizer, attendee1));

        mailSenderMock.clear();

        Event event = testManager.createDefaultMeeting(organizerUid, "meeting");

        Instant now = event.getStartTs();
        Instant beforeNow = now.minus(Duration.standardHours(1));
        long organizerLayer = testManager.createDefaultLayerForUser(organizerUid, beforeNow);
        long attendee1Layer = testManager.createDefaultLayerForUser(attendee1Uid, beforeNow);
        long attendee2Layer = testManager.createDefaultLayerForUser(attendee2Uid, beforeNow);

        testManager.addUserParticipantToEvent(event.getId(), organizerUid, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee1Uid, Decision.YES, false);
        testManager.addUserParticipantToEvent(event.getId(), attendee2Uid, Decision.YES, false);
        setIsExportedWithEwsIfNeeded(event);

        if (isEwser(organizer) && isEwser(attendee1)) {
            setMockExchangeIdForEventUser(event.getId(), attendee1Uid);
        }
        eventRoutines.rejectMeeting(
                event.getId(), attendee1Uid, ActionInfo.webTest(now), InvitationProcessingMode.SAVE_ATTACH_SEND);

        MapF<Email, MailType> actualRecipientEmails = mailSenderMock.getReplyMessageParameterss()
               .toMap(new Function<EventMessageParameters, Tuple2<Email, MailType>>(){
                    public Tuple2<Email, MailType> apply(EventMessageParameters a) {
                        return Tuple2.tuple(a.getRecipientEmail(), a.mailType());
                    }
                });
        MapF<Email, MailType> expectedRecipientEmails = isEwser(organizer)
                ? Cf.map()
                : Cf.map(userManager.getEmailByUid(organizerUid).get(), MailType.EVENT_REPLY);
        Assert.A.equals(expectedRecipientEmails, actualRecipientEmails);

        testLayerCollLastUpdateChecker.assertUpdated(Cf.list(organizerLayer, attendee1Layer, attendee2Layer), now);
    }

}
