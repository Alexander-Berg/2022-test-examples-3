package ru.yandex.calendar.frontend.web.cmd.run.ui.event;

import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.web.cmd.run.PermissionDeniedUserException;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventInvitation;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.EventInvitationDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;

import java.util.Optional;

/**
 * @author gutman
 */
public class CmdGetEvent2Test extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private EventInvitationDao eventInvitationDao;
    @Autowired
    private EventCmdManager eventCmdManager;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private EventUserDao eventUserDao;

    @Test
    public void byPrivateToken() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11901");
        TestUserInfo invited = testManager.prepareUser("yandex-team-mm-11902");

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "byPrivateToken");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), invited.getUid(), Decision.UNDECIDED, false);

        String privateToken = Random2.R.nextAlnum(20);
        EventUser eventUser = new EventUser();
        long eventUserId =
                eventUserDao.findEventUserByEventIdAndUid(event.getId(), invited.getUid())
                .get().getId();
        eventUser.setId(eventUserId);
        eventUser.setPrivateToken(privateToken);
        eventUserDao.updateEventUser(eventUser, ActionInfo.webTest());

        eventCmdManager.getEvent(new EventQuery(event.getId(), Option.<Long>empty()),
                Option.of(privateToken), Option.<UserInfo>empty(), Option.<Instant>empty());
    }

    @Test
    public void byWrongPrivateToken() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11911");
        TestUserInfo invited = testManager.prepareUser("yandex-team-mm-11912");

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "byWrongPrivateToken");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);

        String privateToken = Random2.R.nextAlnum(20);
        saveEventInvitation(organizer, invited, event, privateToken);
        saveEventLayer(invited, event);

        try {
            eventCmdManager.getEvent(new EventQuery(event.getId(), Option.<Long>empty()),
                Option.of(privateToken + "_wrong"), Option.<UserInfo>empty(), Option.<Instant>empty());
        } catch (PermissionDeniedUserException pdue) {
            return;
        }

        Assert.fail("must be wrong token");
    }

    @Test
    public void byEventIdAuthorised() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11921");
        TestUserInfo invited = testManager.prepareUser("yandex-team-mm-11922");

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "byEventIdAuthorised");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);

        saveEventInvitation(organizer, invited, event, Random2.R.nextAlnum(20));
        saveEventLayer(invited, event);

        eventCmdManager.getEvent(new EventQuery(event.getId(), Option.<Long>empty()),
                Option.<String>empty(), Option.of(invited.getUserInfo()), Option.<Instant>empty());
    }

    @Test
    public void byEventIdUnauthorised() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11931");
        TestUserInfo invited = testManager.prepareUser("yandex-team-mm-11932");

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "byEventIdUnauthorised");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);

        try {
            eventCmdManager.getEvent(new EventQuery(event.getId(), Option.<Long>empty()),
                    Option.<String>empty(), Option.<UserInfo>empty(), Option.<Instant>empty());
        } catch (PermissionDeniedUserException pdue) {
            return;
        }

        Assert.fail("user is unauthorised");
    }

    @Test
    public void byEventIdPermissionDenied() {
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-11941");
        TestUserInfo invited = testManager.prepareUser("yandex-team-mm-11942");

        Event event = testManager.createDefaultMeeting(organizer.getUid(), "byEventIdUnauthorised", Optional.of(true));
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);

        try {
            eventCmdManager.getEvent(new EventQuery(event.getId(),
                    layerRoutines.getDefaultLayerId(organizer.getUid())),
                    Option.empty(), Option.of(invited.getUserInfo()), Option.empty());
        } catch (PermissionDeniedUserException pdue) {
            return;
        }

        Assert.fail("user has no rights to view event");
    }

    private void saveEventInvitation(TestUserInfo organizer, TestUserInfo invited, Event event, String privateToken) {
        EventInvitation invitation = new EventInvitation();
        invitation.setDecision(Decision.UNDECIDED);
        invitation.setEventId(event.getId());
        invitation.setEmail(invited.getEmail().normalize());
        invitation.setCreationTs(new LocalDate(2010, 9, 12).toDateTimeAtStartOfDay(DateTimeZone.UTC).toInstant());
        invitation.setCreatorUid(organizer.getUid());
        invitation.setIsOrganizer(false);
        invitation.setPrivateToken(privateToken);
        eventInvitationDao.saveEventInvitation(invitation, ActionInfo.webTest(Instant.now()));
    }

    private void saveEventLayer(TestUserInfo invited, Event event) {
        testManager.createEventLayer(layerRoutines.getOrCreateDefaultLayer(invited.getUid()), event.getId());
    }

}
