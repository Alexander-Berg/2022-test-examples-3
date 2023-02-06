package ru.yandex.calendar.logic.layer;

import Yandex.RequestPackage.ParamValue;
import Yandex.RequestPackage.RequestData;
import org.joda.time.Duration;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.calendar.logic.beans.GenericBeanDao;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventFields;
import ru.yandex.calendar.logic.beans.generated.EventLayer;
import ru.yandex.calendar.logic.beans.generated.EventLayerFields;
import ru.yandex.calendar.logic.beans.generated.IcsFeed;
import ru.yandex.calendar.logic.beans.generated.Layer;
import ru.yandex.calendar.logic.beans.generated.LayerHelper;
import ru.yandex.calendar.logic.beans.generated.LayerUser;
import ru.yandex.calendar.logic.beans.generated.LayerUserFields;
import ru.yandex.calendar.logic.beans.generated.LayerUserHelper;
import ru.yandex.calendar.logic.beans.generated.Settings;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.MainEventDao;
import ru.yandex.calendar.logic.ics.feed.IcsFeedDao;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationDbManager;
import ru.yandex.calendar.logic.notification.NotificationRoutines;
import ru.yandex.calendar.logic.notification.NotificationsData;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.svc.DbSvcRoutines;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.calendar.util.data.AliasedRequestDataProvider;
import ru.yandex.calendar.util.data.DataProvider;
import ru.yandex.calendar.util.idlent.RequestWrapper;
import ru.yandex.inside.passport.PassportSid;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

/**
 * @author Stepan Koltsov
 */
public class LayerRoutinesTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private GenericBeanDao genericBeanDao;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private EventLayerDao eventLayerDao;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private MainEventDao mainEventDao;
    @Autowired
    private LayerDao layerDao;
    @Autowired
    private IcsFeedDao icsFeedDao;
    @Autowired
    private LayerUserDao layerUserDao;
    @Autowired
    private NotificationDbManager notificationDbManager;
    @Autowired
    private NotificationRoutines notificationRoutines;
    @Autowired
    private UserManager userManager;

    @Test
    public void createLayerWithoutName() {
        PassportUid uid = new PassportUid(23456);
        testManager.cleanUser(uid);

        Layer layerOverrides = new Layer();

        LayerType type = LayerType.USER;
        layerRoutines.createLayer(uid, type, layerOverrides);

        // test that search by layer-users returns nothing (as there are no layer-users)
        Assert.assertEmpty(layerDao.findLayersByLayerUserUid(uid));

        // test that search by creator_uid field returns this layer
        Layer layer = layerDao.findLayersByUid(Cf.list(uid)).single();
        Assert.A.equals(Option.<String>empty(), layer.getName()); // XXX ssytnik: RR
    }

    @Test
    public void createLayerVerifyNameSubstitution() {
        PassportUid uid = new PassportUid(23458);
        testManager.cleanUser(uid);

        long layerId = layerRoutines.createUserLayer(uid);

        // test how default name substitution works
        ListF<LayerUserWithRelations> layerInfos = layerRoutines.getLayerUsersWithRelationsByUid(uid, Option.empty())
                .filter(lu -> lu.layerIdIs(layerId));
        final String actualLayerName = layerInfos.single().getEvaluatedLayerName();
        Assert.A.equals(LayerRoutines.DEFAULT_USER_LAYER_NAME.getName(Language.RUSSIAN), actualLayerName);
    }

    @Test
    public void createLayerWithDefaultNotification() {
        PassportUid uid = new PassportUid(23500);
        testManager.cleanUser(uid);

        long layerId = layerRoutines.createUserLayer(uid);

        long layerUserId = layerUserDao.findLayerUserByLayerIdAndUid(layerId, uid).get().getId();
        ListF<Notification> expected = notificationRoutines.getDefaultNotifications();
        ListF<Notification> actual = notificationDbManager.getNotificationsByLayerUserId(layerUserId);

        Assert.equals(expected.unique(), actual.unique());
    }

    @Test
    public void createLayerWithSpecifiedNotification() {
        PassportUid uid = new PassportUid(23510);
        testManager.cleanUser(uid);

        Layer layerOverrides = new Layer();
        String layerName = "Layer with notification";
        layerOverrides.setName(layerName);

        LayerUser layerUserOverrides = new LayerUser();
        ListF<Notification> expected =
                Cf.list(Notification.sms(Duration.standardHours(-2)), Notification.sms(Duration.standardHours(-3)));

        long layerId = layerRoutines.createUserLayer(uid, expected, layerOverrides, false, layerUserOverrides);

        Layer layer = genericBeanDao.loadBeanById(LayerHelper.INSTANCE, layerId);
        Assert.A.equals(layerName, layer.getName().getOrNull());

        ListF<LayerUser> layerUsers = genericBeanDao.loadBeansByField(LayerUserHelper.INSTANCE, LayerUserFields.LAYER_ID, layerId);
        LayerUser layerUser = layerUsers.single();

        ListF<Notification> actual = notificationDbManager.getNotificationsByLayerUserId(layerUser.getId());
        Assert.equals(expected.unique(), actual.unique());
    }

    @Test
    public void isDefaultFlagIsUpdated() {
        PassportUid uid = new PassportUid(23520);
        testManager.cleanUser(uid);

        long layerId = layerRoutines.createUserLayer(uid);
        { // set this layer to be default one
            layerRoutines.updateDefaultLayer(uid, layerId, true);
            Settings settings = settingsRoutines.getSettingsByUid(uid).getCommon();
            Option<Long> defaultLayerIdO = settings.getLayerId();
            long defaultLayerId = defaultLayerIdO.get();
            Assert.assertEquals(layerId, defaultLayerId);
        }
        { // unset default flag for this layer
            layerRoutines.updateDefaultLayer(uid, layerId, false);
            Settings settings = settingsRoutines.getSettingsByUid(uid).getCommon();
            Option<Long> defaultLayerIdO = settings.getLayerId();
            Assert.none(defaultLayerIdO);
        }
    }

    @Test
    public void deleteAttendeeLayer() {
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-13120");
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-13121");

        Event event = testManager.createDefaultEvent(attendee.getUid(), "testDeleteAttendeeLayer event");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);

        EventLayer attendeeEventLayer =
                eventLayerDao.findEventLayersByEventId(event.getId())
                .filter(EventLayerFields.L_CREATOR_UID.getF().andThenEquals(attendee.getUid()))
                .single().copy();
        // In db could be data with incorrect is_primary_inst
        attendeeEventLayer.setIsPrimaryInst(true);
        genericBeanDao.updateBean(attendeeEventLayer);

        layerRoutines.deleteLayer(attendee.getUserInfo(), attendeeEventLayer.getLayerId(), ActionInfo.webTest());
        Assert.A.hasSize(1, eventDao.findEventsByIds(Cf.list(event.getId())));
    }

    @Test
    public void deleteOrganizerLayer() {
        TestUserInfo attendee = testManager.prepareUser("yandex-team-mm-13130");
        TestUserInfo organizer = testManager.prepareUser("yandex-team-mm-13131");

        Event event = testManager.createDefaultEvent(attendee.getUid(), "testDeleteAttendeeLayer event");
        testManager.addUserParticipantToEvent(event.getId(), organizer.getUid(), Decision.YES, true);
        testManager.addUserParticipantToEvent(event.getId(), attendee.getUid(), Decision.YES, false);

        EventLayer organizerEventLayer =
                eventLayerDao.findEventLayersByEventId(event.getId())
                .filter(EventLayerFields.L_CREATOR_UID.getF().andThenEquals(organizer.getUid()))
                .single();

        layerRoutines.deleteLayer(organizer.getUserInfo(), organizerEventLayer.getLayerId(), ActionInfo.webTest());
        Assert.A.equals(0, eventDao.findEventCount(EventFields.ID.column().eq(event.getId())));

        Assert.A.none(mainEventDao.findMainEventOById(event.getMainEventId())); // CAL-2803
    }

    @Test
    public void deleteFeedLayer() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-13141");

        Event event = testManager.createDefaultEvent(user.getUid(), "deleteFeedLayer event");

        long layerId = layerRoutines.createFeedLayer(user.getUid(), "deleteFeedLayer layer");

        IcsFeed icsFeed = new IcsFeed();
        icsFeed.setUid(user.getUid());
        icsFeed.setUrl("");
        icsFeed.setLayerId(layerId);
        icsFeed.setCreationTs(new Instant());
        icsFeed.setNextQueryTs(new Instant(0));
        icsFeed.setIntervalMin(100);
        icsFeedDao.saveIcsFeed(icsFeed);

        layerRoutines.deleteLayer(user.getUserInfo(), layerId, ActionInfo.webTest());
    }

    @Test
    public void evalNoNameLayerName() {
        PassportUid publicUid = testManager.prepareUser("yandex-team-mm-13151").getUid();
        PassportUid yaTeamUid = testManager.prepareRandomYaTeamUser(13151).getUid();

        Assert.A.equals("Мои события", evalName(publicUid, LayerType.USER, PassportSid.CALENDAR));
        Assert.A.equals("fake-user-1120000000013151", evalName(yaTeamUid, LayerType.USER, PassportSid.CALENDAR));

        Assert.A.equals(DbSvcRoutines.TV.getName(), evalName(publicUid, LayerType.SERVICE, PassportSid.TV));
    }

    @Test
    public void deleteWithNotification() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-13161");

        LayerUser layerUser = testManager.createLayers(Cf.list(user.getUid())).single().get2();
        ListF<Notification> notifications = testManager.sms25MinutesBefore();
        notificationDbManager.saveLayerNotifications(layerUser.getId(), notifications);

        layerRoutines.deleteLayer(user.getUserInfo(), layerUser.getLayerId(), ActionInfo.webTest());
        Assert.isEmpty(notificationDbManager.getNotificationsByLayerUserId(layerUser.getId()));
    }

    private String evalName(PassportUid uid, LayerType type, PassportSid sid) {
        Layer layer = new Layer();
        layer.setCreatorUid(uid);
        layer.setType(type);
        layer.setSid(sid);
        layer.setName(Option.<String>empty());

        return layerRoutines.evalLayerName(layer, Option.empty());
    }

    @Test
    public void defaultLayerIsCreatedClosedInPublic() {
        TestUserInfo user = testManager.prepareUser("yandex-team-mm-13181");
        layerRoutines.deleteLayer(user.getUserInfo(), user.getDefaultLayerId(), ActionInfo.webTest());

        Layer defaultLayer = layerRoutines.getLayerById(layerRoutines.getOrCreateDefaultLayer(user.getUid()));
        Assert.A.equals(true, defaultLayer.getIsEventsClosedByDefault());
    }

    @Test
    public void defaultLayerIsCreatedOpenInYt() {
        TestUserInfo user = testManager.prepareRandomYaTeamUser(13191);
        layerRoutines.deleteLayer(user.getUserInfo(), user.getDefaultLayerId(), ActionInfo.webTest());

        Layer defaultLayer = layerRoutines.getLayerById(layerRoutines.getOrCreateDefaultLayer(user.getUid()));
        Assert.A.equals(false, defaultLayer.getIsEventsClosedByDefault());
    }

    @Test
    public void updateNotificationChangesMainEventAndLayerTimestamps() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-13195").getUid();

        long layerId = layerRoutines.getOrCreateDefaultLayer(uid);
        long eventId = testManager.createDefaultEventWithEventLayerAndEventUser(uid, "updateNotification#1").getId();

        ListF<Notification> notification = notificationRoutines.getDefaultNotifications();

        Instant layerTs = layerDao.findLayerById(layerId).getCollLastUpdateTs();
        Instant mainEventTs = mainEventDao.findMainEventByEventId(eventId).getLastUpdateTs();

        boolean applyToEvents = false;

        Instant now = Instant.now().plus(Duration.standardSeconds(1));
        NotificationsData.Update notificationsData = NotificationsData.updateFromWeb(notification);

        layerRoutines.updateNotification(uid, layerId, notificationsData,
                applyToEvents, ActionInfo.webTest(now));
        Assert.equals(layerTs, layerDao.findLayerById(layerId).getCollLastUpdateTs());
        Assert.equals(mainEventTs, mainEventDao.findMainEventByEventId(eventId).getLastUpdateTs());

        applyToEvents = true;

        now = now.plus(Duration.standardSeconds(1));
        layerRoutines.updateNotification(uid, layerId, notificationsData, applyToEvents, ActionInfo.webTest(now));
        Assert.notEquals(layerTs, layerDao.findLayerById(layerId).getCollLastUpdateTs());
        Assert.notEquals(mainEventTs, mainEventDao.findMainEventByEventId(eventId).getLastUpdateTs());
    }

    @Test
    public void keepNullNameOnUpdate() {
        PassportUid publicUid = testManager.prepareUser("yandex-team-mm-13151").getUid();
        PassportUid yaTeamUid = testManager.prepareRandomYaTeamUser(13151).getUid();

        assertKeepNullNameOnUpdate(publicUid);
        assertKeepNullNameOnUpdate(yaTeamUid);
    }

    private void assertKeepNullNameOnUpdate(PassportUid uid) {
        Layer layerOverrides = new Layer();
        LayerType type = LayerType.USER;
        long layerId = layerRoutines.createLayer(uid, type, layerOverrides);

        Assert.isEmpty(layerDao.findLayerById(layerId).getName());

        String resolvedLayerName = evalName(uid, LayerType.USER, PassportSid.CALENDAR);

        Layer layerUpdate = new Layer();
        layerUpdate.setId(layerId);
        layerUpdate.setName(resolvedLayerName);

        layerRoutines.updateLayer(layerUpdate, uid, ActionInfo.webTest());

        Assert.isEmpty(layerDao.findLayerById(layerId).getName());
    }

    @Test
    public void keepNullNameOnUpdateWithDataProvider() {
        PassportUid uid = testManager.prepareRandomYaTeamUser(123).getUid();
        Layer layerOverrides = new Layer();
        LayerType type = LayerType.USER;
        long layerId = layerRoutines.createLayer(uid, type, layerOverrides);

        String resolvedLayerName = evalName(uid, LayerType.USER, PassportSid.CALENDAR);

        Tuple2List<String, String> paramsList = Tuple2List.fromPairs("u_css_class", "custom10", "u_process_ntf", "0",
                "u_head_bg_color", "#D54F8E", "u_n_s", "", "l_name", resolvedLayerName,
                "u_affects_availability", "1", "apply_to_events", "0", "l", "1",
                "l_is_default", "0", "uid", String.valueOf(uid),
                "u_body_bg_color", "#FCEFE2", "u", "1",
                "requestid", "1519890669740", "sk", "ya6cd1e03b1ce75048099b9dfa96e2f85",
                "u_head_fg_color", "#FFFFFF", "id", String.valueOf(layerId), "l_id", String.valueOf(layerId));

        ParamValue[] paramsArray = paramsList.map(ParamValue::new).toArray(ParamValue.class);

        RequestData requestData = new RequestData("127.0.0.1", "localhost", "?",
                paramsArray, null, null);
        RequestWrapper requestWrapper = new RequestWrapper(requestData);
        DataProvider dataProvider = new AliasedRequestDataProvider(requestWrapper);

        layerRoutines.updateLayer(userManager.getUserInfo(uid), dataProvider, ActionInfo.webTest());

        Assert.isEmpty(layerDao.findLayerById(layerId).getName());
    }
} //~
