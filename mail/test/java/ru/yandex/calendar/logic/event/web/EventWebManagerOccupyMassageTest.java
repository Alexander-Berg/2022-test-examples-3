package ru.yandex.calendar.logic.event.web;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.ReadableInstant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.ews.exp.EwsExportRoutines;
import ru.yandex.calendar.frontend.ews.proxy.EwsProxyWrapper;
import ru.yandex.calendar.frontend.ews.proxy.MockEwsProxyWrapperFactory;
import ru.yandex.calendar.frontend.web.cmd.run.CommandRunException;
import ru.yandex.calendar.frontend.web.cmd.run.PermissionDeniedUserException;
import ru.yandex.calendar.frontend.web.cmd.run.Situation;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.dao.EventResourceDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventInvitationsData;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.resource.ResourceDao;
import ru.yandex.calendar.logic.resource.ResourceRoutines;
import ru.yandex.calendar.logic.resource.ResourceType;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.logic.user.Group;
import ru.yandex.calendar.logic.user.UserGroupsDao;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

import static ru.yandex.calendar.logic.resource.ResourceRoutines.getResourceEmailF;

/**
 * CAL-5673
 *
 * @author dbrylev
 */
public class EventWebManagerOccupyMassageTest extends AbstractConfTest {

    @Autowired
    private EventWebManager eventWebManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private UserManager userManager;
    @Autowired
    private UserGroupsDao userGroupsDao;
    @Autowired
    private EventResourceDao eventResourceDao;
    @Autowired
    private EwsExportRoutines ewsExportRoutines;
    @Autowired
    private EwsProxyWrapper ewsProxyWrapper;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private ResourceDao resourceDao;

    private Resource massageRoom;
    private TestUserInfo occupier;
    private TestUserInfo superUser;

    private final DateTime now = new DateTime(2012, 11, 2, 22, 0, MoscowTime.TZ);

    @Before
    public void cleanBeforeTest() {
        massageRoom = testManager.cleanAndCreateResourceWithNoSyncWithExchange("conf_rr_1_8", "Московский массаж");

        massageRoom.setType(ResourceType.MASSAGE_ROOM);
        resourceDao.updateResource(massageRoom);

        occupier = testManager.prepareYandexUser(TestManager.createSsytnik());

        superUser = testManager.prepareYandexUser(TestManager.createHelga(), Group.SUPER_USER);
        ewsExportRoutines.setEwsProxyWrapperForTest(MockEwsProxyWrapperFactory.getMockEwsProxyWrapperForTest());
    }

    @After
    public void setRealEwsProxyWrapper() {
        ewsExportRoutines.setEwsProxyWrapperForTest(ewsProxyWrapper);
    }

    @Test
    public void existingMassageInFuture() {
        ActionInfo actionInfo = ActionInfo.webTest(now.toInstant());

        createSingleMassageStartingAt(superUser, now.plusDays(1), actionInfo);
        createSingleMassageStartingAt(occupier, now.plusDays(2), actionInfo);

        createSingleMassageStartingAt(superUser, now.plusDays(3), actionInfo);
        try {
            createSingleMassageStartingAt(occupier, now.plusDays(4), actionInfo);
            Assert.fail();
        } catch (Exception e) {
            Assert.isTrue(isMassageDeniedException(e));
        }
    }

    @Test
    public void existingMassageInPast() {
        ActionInfo actionInfo = ActionInfo.webTest(now.toInstant());

        createSingleMassageStartingAt(superUser, now.minusDays(1), actionInfo);
        Event past = createSingleMassageStartingAt(occupier, now.minusDays(2), actionInfo);

        createSingleMassageStartingAt(superUser, now.plusDays(3), actionInfo);
        Event future = createSingleMassageStartingAt(occupier, now.plusDays(4), actionInfo);

        Assert.isTrue(massageRoomOccupiedForEvent(past));
        Assert.isTrue(massageRoomOccupiedForEvent(future));
    }

    @Test
    public void updateMassageInFuture() {
        ActionInfo actionInfo = ActionInfo.webTest(now.toInstant());
        Event massage = createSingleMassageStartingAt(occupier, now.plusDays(1), actionInfo);

        EventData eda = new EventData();
        eda.getEvent().setStartTs(massage.getStartTs().plus(111111));
        eda.getEvent().setEndTs(massage.getEndTs().plus(111111));
        eda.setInvData(new EventInvitationsData(Cf.list(massageRoom).map(getResourceEmailF())));

        updateEvent(occupier, massage, eda, actionInfo);
        Assert.isTrue(massageRoomOccupiedForEvent(massage));
    }

    @Test
    public void createRepeatingMassage() {
        ActionInfo actionInfo = ActionInfo.webTest(now.toInstant());
        Event superMassage = createRepeatingMassageStartingAt(superUser, now.plusHours(1), actionInfo);
        Assert.isTrue(massageRoomOccupiedForEvent(superMassage));

        try {
            createRepeatingMassageStartingAt(occupier, now.plusHours(2), actionInfo);
            Assert.fail();
        } catch (Exception e) {
            Assert.isTrue(isMassageDeniedException(e));
        }
    }

    @Test
    public void makeMassageRepeating() {
        ActionInfo actionInfo = ActionInfo.webTest(now.toInstant());
        Event massage = createSingleMassageStartingAt(occupier, now, actionInfo);

        EventData eda = new EventData();
        eda.setRepetition(TestManager.createDailyRepetitionTemplate());
        eda.setInvData(new EventInvitationsData(Cf.list(massageRoom).map(getResourceEmailF())));

        try {
            updateEvent(occupier, massage, eda, actionInfo);
            Assert.fail();
        } catch (Exception e) {
            Assert.isTrue(isMassageDeniedException(e));
        }
    }

    @Test
    public void inviteMassageRoom() {
        ActionInfo actionInfo = ActionInfo.webTest(now.toInstant());
        Resource noMassageRoom = testManager.cleanAndCreateResourceWithNoSyncWithExchange("pigs", "XXX");

        Event event = createSingleEventWithResourceStartingAt(occupier, noMassageRoom, now.plusDays(3), actionInfo);
        EventData eda = new EventData();
        eda.setInvData(new EventInvitationsData(Cf.list(massageRoom, noMassageRoom).map(getResourceEmailF())));

        createSingleMassageStartingAt(occupier, now.plusDays(4), actionInfo);
        try {
            updateEvent(occupier, event, eda, actionInfo);
            Assert.fail();
        } catch (Exception e) {
            Assert.isTrue(isMassageDeniedException(e));
        }
    }

    @Test
    public void massageGiftFromFriend() {
        ActionInfo actionInfo = ActionInfo.webTest(now.toInstant());

        TestUserInfo friend = testManager.prepareRandomYaTeamUser(12);
        Resource noMassageRoom = testManager.cleanAndCreateResourceWithNoSyncWithExchange("pigs", "XXX");

        layerRoutines.startNewSharing(friend.getUid(), occupier.getDefaultLayerId(), LayerActionClass.EDIT);

        Event m1 = createSingleEventWithResourceStartingAt(occupier, noMassageRoom, now.plusDays(1), actionInfo);
        Event m2 = createSingleEventWithResourceStartingAt(occupier, noMassageRoom, now.plusDays(2), actionInfo);

        EventData eda = new EventData();

        eda.getEvent().setId(m1.getId());
        eda.setInvData(new EventInvitationsData(Cf.list(massageRoom).map(getResourceEmailF())));

        eventWebManager.update(friend.getUserInfo(), eda, false, actionInfo);

        eda.getEvent().setId(m2.getId());
        Assert.assertThrows(
                () -> eventWebManager.update(friend.getUserInfo(), eda, false, actionInfo),
                CommandRunException.class, this::isMassageDeniedException);
    }

    @Test
    public void occupyByYaMoneyUser() {
        TestUserInfo yaMoneyUser = testManager.prepareRandomYaTeamUser(10);
        userManager.makeYaMoneyUserForTest(yaMoneyUser.getUid());
        ActionInfo actionInfo = ActionInfo.webTest(now.toInstant());

        Assert.assertThrows(
                () -> createSingleMassageStartingAt(yaMoneyUser, now.plusDays(1), actionInfo),
                PermissionDeniedUserException.class);
    }

    private boolean massageRoomOccupiedForEvent(Event event) {
        return eventResourceDao.findEventResourceByEventIdAndResourceId(event.getId(), massageRoom.getId()).isPresent();
    }

    private Event createSingleMassageStartingAt(TestUserInfo organizer, ReadableInstant start, ActionInfo actionInfo) {
        return createEventWithResourceStartingAt(organizer, massageRoom, start, false, actionInfo);
    }

    private Event createRepeatingMassageStartingAt(TestUserInfo organizer, ReadableInstant start, ActionInfo actionInfo) {
        return createEventWithResourceStartingAt(organizer, massageRoom, start, true, actionInfo);
    }

    private Event createSingleEventWithResourceStartingAt(
            TestUserInfo organizer, Resource resource, ReadableInstant start, ActionInfo actionInfo)
    {
        return createEventWithResourceStartingAt(organizer, resource, start, false, actionInfo);
    }

    private Event createEventWithResourceStartingAt(
            TestUserInfo organizer, Resource resource, ReadableInstant start, boolean repeating, ActionInfo actionInfo)
    {
        EventData eda = new EventData();
        eda.getEvent().setStartTs(start.toInstant());
        eda.getEvent().setEndTs(start.toInstant().plus(Duration.standardHours(1)));

        eda.setInvData(new EventInvitationsData(Cf.list(ResourceRoutines.getResourceEmail(resource))));

        if (repeating) {
            eda.setRepetition(TestManager.createDailyRepetitionTemplate());
        }

        eda.setTimeZone(MoscowTime.TZ);
        return eventWebManager.createUserEvent(
                organizer.getUid(), eda, InvitationProcessingMode.SAVE_ATTACH, actionInfo).getEvent();
    }

    private Option<Long> updateEvent(TestUserInfo updater, Event event, EventData eventData, ActionInfo actionInfo) {
        eventData.setTimeZone(MoscowTime.TZ);
        eventData.getEvent().setFieldDefaults(event);

        return eventWebManager.update(updater.getUserInfo(), eventData, false, actionInfo);
    }

    private boolean isMassageDeniedException(Exception e) {
        return e instanceof CommandRunException && ((CommandRunException) e).isSituation(Situation.MASSAGE_DENIED);
    }
}
