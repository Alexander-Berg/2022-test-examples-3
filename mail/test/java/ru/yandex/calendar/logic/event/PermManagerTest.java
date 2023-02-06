package ru.yandex.calendar.logic.event;

import java.util.Optional;
import java.util.function.Predicate;

import lombok.val;
import org.apache.commons.lang.mutable.MutableObject;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Layer;
import ru.yandex.calendar.logic.beans.generated.LayerUser;
import ru.yandex.calendar.logic.beans.generated.Resource;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.model.EventData;
import ru.yandex.calendar.logic.event.model.EventType;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.layer.LayerType;
import ru.yandex.calendar.logic.layer.LayerUserDao;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.resource.UidOrResourceId;
import ru.yandex.calendar.logic.sharing.InvitationProcessingMode;
import ru.yandex.calendar.logic.sharing.perm.Authorizer;
import ru.yandex.calendar.logic.sharing.perm.EventActionClass;
import ru.yandex.calendar.logic.sharing.perm.LayerInfoForPermsCheck;
import ru.yandex.calendar.micro.perm.LayerAction;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.time.MoscowTime;

import static org.assertj.core.api.Assertions.assertThat;

public class PermManagerTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private LayerUserDao layerUserDao;
    @Autowired
    private Authorizer authorizer;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private EventRoutines eventRoutines;
    @Autowired
    private EventDao eventDao;

    @Test
    public void canViewEventOnForeignLayer() {
        val me = testManager.prepareUser("yandex-team-mm-11701");
        val layerOwner = testManager.prepareUser("yandex-team-mm-11702");

        val event = testManager.createDefaultEvent(layerOwner.getUid(), "event");
        val layerId = layerRoutines.getOrCreateDefaultLayer(layerOwner.getUid());

        testManager.createEventLayer(layerId, event.getId(), true);

        LayerUser layerUser = new LayerUser();
        layerUser.setLayerId(layerId);
        layerUser.setUid(me.getUid());
        layerUser.setPerm(LayerActionClass.LIST);

        layerUserDao.saveLayerUser(layerUser);

        val eventAuthInfo = authorizer.loadEventInfoForPermsCheck(me.getUserInfo(), eventDbManager.getEventWithRelationsById(event.getId()));
        assertThat(authorizer.canViewEvent(me.getUserInfo(), eventAuthInfo, ActionSource.WEB)).isTrue();
    }

    @Test
    public void canNotViewPrivateEventWithResource() {
        val me = testManager.prepareUser("yandex-team-mm-11711");
        val organizer = testManager.prepareUser("yandex-team-mm-11712");
        val resource = testManager.cleanAndCreateSmolny();

        val layerId = layerRoutines.getOrCreateDefaultLayer(organizer.getUid());

        val event = testManager.createDefaultEvent(organizer.getUid(), "event", true);
        testManager.createEventLayer(layerId, event.getId(), true);
        testManager.createEventResource(resource.getId(), event.getId());
        testManager.updateEventTimeIndents(event);

        checkViewPerm(false, me, resource, layerId, event);
        checkViewPerm(true, organizer, resource, layerId, event);
    }

    @Test
    public void canViewOpenEventWithResource() {
        val me = testManager.prepareUser("yandex-team-mm-11711");
        val organizer = testManager.prepareUser("yandex-team-mm-11712");
        val resource = testManager.cleanAndCreateSmolny();

        val layerId = layerRoutines.getOrCreateDefaultLayer(organizer.getUid());

        val event = testManager.createDefaultEvent(organizer.getUid(), "event");
        eventDao.updateEventPermAll(event.getId(), EventActionClass.VIEW);
        testManager.createEventLayer(layerId, event.getId(), true);
        testManager.createEventResource(resource.getId(), event.getId());
        testManager.updateEventTimeIndents(event);

        checkViewPerm(true, me, resource, layerId, event);
        checkViewPerm(true, organizer, resource, layerId, event);
    }

    @Test
    public void externalUserCanListAttachedLayer() {
        val owner = testManager.prepareRandomYaTeamUser(11);
        val user = testManager.prepareRandomYaTeamUser(13);

        val sharedLayerId = owner.getDefaultLayerId();
        val publicLayerId = layerRoutines.createLayer(owner.getUid(), LayerType.USER, new Layer());

        layerRoutines.startNewSharing(user.getUid(), sharedLayerId, LayerActionClass.ACCESS);

        val userInfo = new MutableObject(user.getUserInfo());

        Predicate<Long> canList = layerId -> {
            val layer = layerRoutines.getLayerById(layerId);
            val layerPermInfo = LayerInfoForPermsCheck.fromLayer(layer);
            return authorizer.canPerformLayerAction((UserInfo) userInfo.getValue(), layerPermInfo, Optional.empty(),
                LayerAction.LIST, ActionSource.WEB);
        };

        assertThat(canList.test(sharedLayerId)).isTrue();
        assertThat(canList.test(publicLayerId)).isTrue();

        userInfo.setValue(user.getUserInfo().withIsExternalYtForTest(true));

        assertThat(canList.test(sharedLayerId)).isTrue();
        assertThat(canList.test(publicLayerId)).isFalse();

        canList = layerId -> {
            val layerPermInfo = LayerInfoForPermsCheck.fromLayer(layerRoutines.getLayerById(layerId));
            val sharing = authorizer.loadLayerSharing(user.getUid());
            return authorizer.canPerformLayerAction((UserInfo) userInfo.getValue(), layerPermInfo, Optional.of(sharing),
                LayerAction.LIST, ActionSource.WEB);
        };

        assertThat(canList.test(sharedLayerId)).isTrue();
        assertThat(canList.test(publicLayerId)).isFalse();
    }

    @Test
    public void cannotEditEventAfterLayerAccessLost() {
        val owner = testManager.prepareRandomYaTeamUser(21);
        val creator = testManager.prepareRandomYaTeamUser(23);

        layerRoutines.startNewSharing(creator.getUid(), owner.getDefaultLayerId(), LayerActionClass.CREATE);

        val data = new EventData();

        data.getEvent().setStartTs(Instant.now());
        data.getEvent().setEndTs(Instant.now());

        data.setLayerId(owner.getDefaultLayerId());

        val info = eventRoutines.createUserOrFeedEvent(
                UidOrResourceId.user(creator.getUid()), EventType.USER,
                eventRoutines.createMainEvent(MoscowTime.TZ, ActionInfo.webTest()), data,
                NotificationsData.createEmpty(), InvitationProcessingMode.SAVE_ATTACH_SEND, ActionInfo.webTest());

        val event = eventDbManager.getEventWithRelationsByEvent(info.getEvent());
        val eventAuthInfoForCreator = authorizer.loadEventInfoForPermsCheck(creator.getUserInfo(), event);
        val eventAuthInfoForOwner = authorizer.loadEventInfoForPermsCheck(owner.getUserInfo(), event);

        assertThat(authorizer.canEditEvent(creator.getUserInfo(), eventAuthInfoForCreator, ActionSource.WEB))
            .isTrue();
        assertThat(authorizer.canEditEvent(owner.getUserInfo(), eventAuthInfoForOwner, ActionSource.WEB))
            .isTrue();

        assertThat(authorizer.canDeleteEvent(creator.getUserInfo(), eventAuthInfoForCreator, ActionSource.WEB))
            .isTrue();
        assertThat(authorizer.canDeleteEvent(owner.getUserInfo(), eventAuthInfoForOwner, ActionSource.WEB))
            .isTrue();

        layerRoutines.detach(owner.getUid(), creator.getUid(), owner.getDefaultLayerId(), ActionInfo.webTest());

        val eventAuthInfoForCreatorNew = authorizer.loadEventInfoForPermsCheck(creator.getUserInfo(), event);
        assertThat(authorizer.canEditEvent(creator.getUserInfo(), eventAuthInfoForCreatorNew, ActionSource.WEB))
            .isFalse();
        assertThat(authorizer.canDeleteEvent(creator.getUserInfo(), eventAuthInfoForCreatorNew, ActionSource.WEB))
            .isFalse();
    }

    private void checkViewPerm(
            boolean expectedCanView, TestUserInfo userInfo, Resource resource, long layerId, Event event)
    {
        val limits = EventLoadLimits.intersectsInterval(event.getStartTs(), event.getEndTs());
        val onResource = eventRoutines.getSortedInstancesOnResource(Option.of(userInfo.getUid()),
                EventGetProps.any(), Cf.list(resource.getId()), limits, ActionSource.WEB);
        val onLayer = eventRoutines.getSortedInstancesOnLayer(Option.of(userInfo.getUid()),
                EventGetProps.any(), LayerIdPredicate.list(Cf.list(layerId), false), limits, ActionSource.WEB);

        assertThat(onResource.single().getMayView()).isEqualTo(expectedCanView);
        assertThat(onLayer.single().getMayView()).isEqualTo(expectedCanView);

        val eventAuthInfo = authorizer.loadInfoForPermsCheckByEvents(Optional.of(userInfo.getUserInfo()), eventDao.findEventById(event.getId()));
        assertThat(authorizer.canViewEvent(userInfo.getUserInfo(), eventAuthInfo, ActionSource.WEB))
                .isEqualTo(expectedCanView);
    }

}
