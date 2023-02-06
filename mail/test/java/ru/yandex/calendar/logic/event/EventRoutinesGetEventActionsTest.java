package ru.yandex.calendar.logic.event;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventUser;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.notification.EventUserWithNotifications;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.AbstractDbDataTest;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public class EventRoutinesGetEventActionsTest extends AbstractDbDataTest {

    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private TestManager testManager;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private UserManager userManager;

    private EventActions getEventActions(PassportUid uid, long eventId, long layerId) {
        EventInstanceInfo ei = eventRoutines.getSingleInstance(
                Option.of(uid), Option.<Instant>empty(), eventId, ActionSource.WEB);

        UserInfo user = userManager.getUserInfo(uid);

        Option<EventUser> eventUserO = ei.getEventUserWithNotifications().map(EventUserWithNotifications.getEventUserF());
        return eventRoutines.getEventActions(
                user, ei.getInfoForPermsCheck(), eventUserO,
                Option.of(layerRoutines.getLayerById(layerId)), ActionSource.WEB);
    }

    @Test
    public void organizerEventActions() {
        PassportUid organizerUid = testManager.prepareUser("yandex-team-mm-19101").getUid();

        Event event = testManager.createDefaultEvent(organizerUid, "organizerEventActions");
        testManager.addUserParticipantToEvent(event.getId(), organizerUid, Decision.YES, true);
        long organizerLid = layerRoutines.getDefaultLayerId(organizerUid).get();

        EventActions organizerActions = getEventActions(organizerUid, event.getId(), organizerLid);

        // dbrylev: organizer can not reject or detach meeting (only delete)
        Assert.A.isFalse(organizerActions.canAccept());
        Assert.A.isFalse(organizerActions.canReject());
        Assert.A.isFalse(organizerActions.canAttach());
        Assert.A.isFalse(organizerActions.canDetach());
        Assert.A.isTrue(organizerActions.canDelete());
    }

    @Test
    public void attendeeEventActions() {
        PassportUid organizerUid = testManager.prepareUser("yandex-team-mm-19111").getUid();
        PassportUid acceptedUid = testManager.prepareUser("yandex-team-mm-19112").getUid();
        PassportUid undecidedUid = testManager.prepareUser("yandex-team-mm-19113").getUid();
        PassportUid rejectedUid = testManager.prepareUser("yandex-team-mm-19114").getUid();

        Event event = testManager.createDefaultEvent(organizerUid, "attendeeEventActions");
        testManager.addUserParticipantToEvent(event.getId(), organizerUid, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), acceptedUid, Decision.YES, false);
        testManager.addUserParticipantToEvent(event.getId(), undecidedUid, Decision.UNDECIDED, false);
        testManager.addUserParticipantToEvent(event.getId(), rejectedUid, Decision.NO, false);

        long acceptedLid = layerRoutines.getDefaultLayerId(acceptedUid).get();
        long undecidedLid = layerRoutines.getDefaultLayerId(undecidedUid).get();
        long rejectedLid = layerRoutines.getDefaultLayerId(rejectedUid).get();

        EventActions acceptedActions = getEventActions(acceptedUid, event.getId(), acceptedLid);
        EventActions undecidedActions = getEventActions(undecidedUid, event.getId(), undecidedLid);
        EventActions rejectedActions = getEventActions(rejectedUid, event.getId(), rejectedLid);

        Assert.A.isFalse(acceptedActions.canAccept());
        Assert.A.isTrue(acceptedActions.canReject());
        Assert.A.isFalse(acceptedActions.canAttach());
        Assert.A.isFalse(acceptedActions.canDetach());
        Assert.A.isFalse(acceptedActions.canDelete());

        Assert.A.isTrue(undecidedActions.canAccept());
        Assert.A.isTrue(undecidedActions.canReject());
        Assert.A.isFalse(undecidedActions.canAttach());
        Assert.A.isFalse(undecidedActions.canDetach());
        Assert.A.isFalse(undecidedActions.canDelete());

        Assert.A.isTrue(rejectedActions.canAccept());
        Assert.A.isFalse(rejectedActions.canReject());
        Assert.A.isTrue(rejectedActions.canAttach());
        Assert.A.isFalse(rejectedActions.canDetach());
        Assert.A.isFalse(rejectedActions.canDelete());
    }

    @Test
    public void sharedLayerEventActions() {
        PassportUid organizerUid = testManager.prepareUser("yandex-team-mm-19121").getUid();
        PassportUid attendeeUid = testManager.prepareUser("yandex-team-mm-19122").getUid();
        PassportUid editorUid = testManager.prepareUser("yandex-team-mm-19123").getUid();
        TestUserInfo attacher = testManager.prepareUser("yandex-team-mm-19124");

        Event event = testManager.createDefaultEvent(organizerUid, "sharedLayerEventActions");
        testManager.addUserParticipantToEvent(event.getId(), organizerUid, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendeeUid, Decision.MAYBE, false);

        long organizerLid = layerRoutines.getDefaultLayerId(organizerUid).get();

        testManager.createOrUpdateLayerUser(attendeeUid, organizerLid, LayerActionClass.VIEW);
        EventActions attendeeActionsOnForeignLayer = getEventActions(attendeeUid, event.getId(), organizerLid);

        Assert.A.isFalse(attendeeActionsOnForeignLayer.canAccept());
        Assert.A.isFalse(attendeeActionsOnForeignLayer.canReject());
        Assert.A.isFalse(attendeeActionsOnForeignLayer.canAttach());
        Assert.A.isFalse(attendeeActionsOnForeignLayer.canDetach());
        Assert.A.isFalse(attendeeActionsOnForeignLayer.canDelete());

        testManager.createOrUpdateLayerUser(editorUid, organizerLid, LayerActionClass.EDIT);
        EventActions editorActionsOnForeignLayer = getEventActions(editorUid, event.getId(), organizerLid);

        Assert.A.isFalse(editorActionsOnForeignLayer.canAccept());
        Assert.A.isFalse(editorActionsOnForeignLayer.canReject());
        Assert.A.isTrue(editorActionsOnForeignLayer.canAttach());
        Assert.A.isFalse(editorActionsOnForeignLayer.canDetach());
        Assert.A.isTrue(editorActionsOnForeignLayer.canDelete());

        event = testManager.createDefaultEvent(attendeeUid, "sharedLayerEventActions2");
        testManager.addUserParticipantToEvent(event.getId(), attendeeUid, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), organizerUid, Decision.MAYBE, false);

        editorActionsOnForeignLayer = getEventActions(editorUid, event.getId(), organizerLid);

        // dbrylev: can detach (reject without notifying organizer) on someones layer
        // if we have edit permissions and he is not organizer (make decision by him)
        Assert.A.isFalse(editorActionsOnForeignLayer.canAccept());
        Assert.A.isFalse(editorActionsOnForeignLayer.canReject());
        Assert.A.isTrue(editorActionsOnForeignLayer.canAttach());
        Assert.A.isTrue(editorActionsOnForeignLayer.canDetach());
        Assert.A.isFalse(editorActionsOnForeignLayer.canDelete());

        long attacherLid = layerRoutines.getOrCreateDefaultLayer(attacher.getUid());
        eventRoutines.attachEventOrMeetingToUser(
                attacher.getUserInfo(), event.getId(),
                Option.of(attacherLid), Decision.YES, new EventUser(),
                NotificationsData.create(Cf.<Notification>list()), ActionInfo.webTest());

        EventActions attacherActions = getEventActions(attacher.getUid(), event.getId(), attacherLid);

        // dbrylev: can detach attached event
        Assert.A.isFalse(attacherActions.canAccept());
        Assert.A.isFalse(attacherActions.canReject());
        Assert.A.isFalse(attacherActions.canAttach());
        Assert.A.isTrue(attacherActions.canDetach());
        Assert.A.isFalse(attacherActions.canDelete());
    }

    @Test
    public void notAttendeeActions() {
        PassportUid organizerUid = testManager.prepareUser("yandex-team-mm-19121").getUid();
        PassportUid attendeeUid = testManager.prepareUser("yandex-team-mm-19122").getUid();

        TestUserInfo actor = testManager.prepareUser("yandex-team-mm-19124");

        Event event = testManager.createDefaultEvent(organizerUid, "notAttendeeActions");
        testManager.addUserParticipantToEvent(event.getId(), organizerUid, Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendeeUid, Decision.MAYBE, false);

        testManager.createEventLayer(actor.getDefaultLayerId(), event.getId());
        testManager.createEventUser(actor.getUid(), event.getId(), Decision.UNDECIDED, Option.empty());

        EventActions actions = getEventActions(actor.getUid(), event.getId(), actor.getDefaultLayerId());

        Assert.isTrue(actions.canDetach());
        Assert.isTrue(actions.canAccept());
        Assert.isTrue(actions.canReject());
    }
}
