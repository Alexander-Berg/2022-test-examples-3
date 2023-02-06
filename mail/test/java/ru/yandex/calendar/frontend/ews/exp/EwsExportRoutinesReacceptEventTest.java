package ru.yandex.calendar.frontend.ews.exp;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.MainEvent;
import ru.yandex.calendar.logic.beans.generated.SettingsYt;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.MailType;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;

/**
 * @author yashunsky
 */
public class EwsExportRoutinesReacceptEventTest extends AbstractConfTest {

    @Autowired
    private TestManager testManager;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private EwsExportRoutines ewsExportRoutines;
    @Autowired
    private MailSenderMock mailSender;

    @Test
    public void reacceptEvent() {
        TestUserInfo organizer = testManager.prepareRandomYaTeamUser(22);
        TestUserInfo attendeeEwser = testManager.prepareRandomYaTeamSuperUser(23);

        SettingsYt settingsYt = new SettingsYt();
        settingsYt.setIsEwser(true);

        settingsRoutines.updateSettingsYtByUid(settingsYt, attendeeEwser.getUid());

        Event event = testManager.createDefaultEvent(organizer.getUid(), "simple event");
        testManager.addUserParticipantToEvent(event.getId(), organizer, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendeeEwser, Decision.NO, false);

        MainEvent mainEvent = mainEventDao.findMainEventById(event.getMainEventId());

        mailSender.clear();
        ewsExportRoutines.updateAttendeeDecisionIfNeeded(event, mainEvent, UidOrResourceId.user(attendeeEwser.getUid()), Decision.YES, Option.empty(), true, ActionInfo.webTest());

        Assert.hasSize(1, mailSender.getEventMessageParameters());
        Assert.some(MailType.EVENT_INVITATION, mailSender.findEventMailType(attendeeEwser.getEmail()));
    }
}
