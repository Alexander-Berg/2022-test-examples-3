package ru.yandex.calendar.frontend.webNew;

import lombok.val;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.function.Function0;
import ru.yandex.calendar.CalendarUtils;
import ru.yandex.calendar.frontend.web.cmd.run.CommandRunException;
import ru.yandex.calendar.frontend.web.cmd.run.Situation;
import ru.yandex.calendar.frontend.webNew.actions.LayerActions;
import ru.yandex.calendar.frontend.webNew.dto.in.ImportIcsData;
import ru.yandex.calendar.frontend.webNew.dto.in.LayerData;
import ru.yandex.calendar.frontend.webNew.dto.out.LayerIdInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.LayerInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.LayersInfo;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventLayer;
import ru.yandex.calendar.logic.beans.generated.Layer;
import ru.yandex.calendar.logic.beans.generated.LayerInvitation;
import ru.yandex.calendar.logic.event.EventDbManager;
import ru.yandex.calendar.logic.event.EventRoutines;
import ru.yandex.calendar.logic.event.avail.absence.AbsenceType;
import ru.yandex.calendar.logic.ics.iv5j.ical.component.IcsVEvent;
import ru.yandex.calendar.logic.ics.iv5j.ical.property.IcsXWrCalname;
import ru.yandex.calendar.logic.layer.LayerInvitationManager;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.layer.LayerType;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.notification.NotificationDbManager;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;

import static org.assertj.core.api.Assertions.assertThat;

public class WebNewLayerManagerTest extends WebNewTestBase {

    @Autowired
    private LayerActions layerActions;
    @Autowired
    private WebNewLayerManager webNewLayerManager;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private NotificationDbManager notificationDbManager;
    @Autowired
    private EventDbManager eventDbManager;
    @Autowired
    private LayerInvitationManager layerInvitationManager;
    @Autowired
    private EventRoutines eventRoutines;

    @Test
    public void createLayer() {
        LayerData data = createLayerData();

        data.setIsDefault(Option.of(true));

        LayerIdInfo id = layerActions.createLayer(uid, data);
        LayerInfo info = layerActions.getLayer(Option.of(uid), Option.of(id.getLayerId()), Option.empty(), Option.empty());

        Assert.equals(data.getName().get(), info.getName());
        Assert.isTrue(info.isDefault());

        Assert.isTrue(info.isAffectAvailability());
        Assert.isFalse(info.isNotifyAboutEventChanges());

        Assert.none(info.getIcsFeed());
        Assert.equals(LayerType.USER.toDbValue(), info.getType());
        Assert.equals(data.getColor().get(), info.getColor());

        Assert.none(info.getToken());

        Assert.equals(data.getNotifications(), info.getNotifications());
        LayerData.Participant dataParticipant = data.getParticipants().get().single();

        Assert.equals(dataParticipant.getEmail(), info.getParticipants().get().single().getInfo().getEmail());
        Assert.equals(dataParticipant.getPermission(), info.getParticipants().get().single().getPermission());
    }

    @Test
    public void updateLayerOwned() {
        LayerInfo before = layerActions.getLayer(Option.of(uid), Option.of(user.getDefaultLayerId()), Option.empty(), Option.empty());

        LayerData data = createLayerData();
        data.setIsDefault(Option.of(false));

        layerActions.updateLayer(uid, before.getId(), false, data);
        LayerInfo after = layerActions.getLayer(Option.of(uid), Option.of(before.getId()), Option.empty(), Option.empty());

        Assert.isEmpty(before.getNotifications());
        Assert.notEmpty(after.getNotifications());

        Assert.isEmpty(before.getParticipants().get());
        Assert.notEmpty(after.getParticipants().get());

        Assert.notEquals(data.getColor().get(), before.getColor());
        Assert.equals(data.getColor().get(), after.getColor());

        Assert.isTrue(after.isDefault());
    }

    @Test
    public void updateLayerShared() {
        layerRoutines.startNewSharing(uid, user2.getDefaultLayerId(), LayerActionClass.VIEW);

        LayerInfo before = layerActions.getLayer(Option.of(uid), Option.of(user2.getDefaultLayerId()), Option.empty(), Option.empty());

        LayerData data = createLayerData();

        layerActions.updateLayer(uid, before.getId(), false, data);
        LayerInfo after = layerActions.getLayer(Option.of(uid), Option.of(before.getId()), Option.empty(), Option.empty());

        Assert.isEmpty(before.getNotifications());
        Assert.notEmpty(after.getNotifications());

        Assert.notEquals(data.getName().get(), before.getName());
        Assert.equals(before.getName(), after.getName());

        Assert.notEquals(data.getColor().get(), before.getColor());
        Assert.equals(data.getColor().get(), after.getColor());

        Assert.none(after.getParticipants());

        Assert.isFalse(before.isDefault());
        Assert.isFalse(after.isDefault());
    }

    @Test
    public void updateLayerFeed() {
        LayerInfo before = layerActions.createIcsFeed(uid, "holidays://russia", Option.empty());

        LayerData data = createLayerData();
        data.setFeedUrl(Option.of("holidays://usa"));

        layerActions.updateLayer(uid, before.getId(), false, data);
        LayerInfo after = layerActions.getLayer(Option.of(uid), Option.of(before.getId()), Option.empty(), Option.empty());

        Assert.equals(data.getFeedUrl().get(), after.getIcsFeed().get().getUrl());
    }

    @Test
    public void updateLayerApplyNotifications() {
        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(uid, "To be notified");
        testManager.createDailyRepetitionAndLinkToEvent(event.getId());
        testManager.updateEventTimeIndents(event);

        Function0<ListF<Notification>> findNotifications = () ->
                notificationDbManager.getEventUserWithNotificationsByUidAndEventId(uid, event.getId()).get()
                        .getNotifications().getEventNotifications().map(Notification::of);

        LayerData data = createLayerData();

        data.setParticipants(Option.empty());

        Assert.isEmpty(findNotifications.apply());
        webNewLayerManager.updateLayer(uid, user.getDefaultLayerId(), true, data);

        Assert.notEmpty(data.getNotifications());
        Assert.equals(data.getNotifications(), findNotifications.apply());
    }

    @Test
    public void toggleLayer() {
        Assert.isTrue(layerActions.getUserLayers(uid, Option.empty()).getLayers().single().isToggledOn());

        layerActions.toggleLayer(uid, user.getDefaultLayerId(), false);
        Assert.isFalse(layerActions.getUserLayers(uid, Option.empty()).getLayers().single().isToggledOn());

        layerActions.toggleLayer(uid, user.getDefaultLayerId(), true);
        Assert.isTrue(layerActions.getUserLayers(uid, Option.empty()).getLayers().single().isToggledOn());
    }

    @Test
    public void obtainToken() {
        long layerId = user.getDefaultLayerId();

        Assert.none(layerActions.getLayer(Option.of(uid), Option.of(layerId), Option.empty(), Option.empty()).getToken());

        String token = layerActions.obtainLayerPrivateToken(uid, layerId, Option.empty()).getToken();
        Assert.some(token, layerActions.getLayer(Option.of(uid), Option.of(layerId), Option.empty(), Option.empty()).getToken());

        Assert.notEquals(token, layerActions.obtainLayerPrivateToken(uid, layerId, Option.of(true)).getToken());
    }

    @Test
    public void deleteLayerOwned() {
        Event event = testManager.createDefaultEventWithEventLayerAndEventUser(uid, "To be moved");

        long targetLayerId = layerActions.createLayer(uid, createLayerData()).getLayerId();
        long defaultLayerId = layerActions.createLayer(uid, createLayerData()).getLayerId();

        layerActions.deleteLayer(uid, user.getDefaultLayerId(), Option.of(targetLayerId), Option.of(defaultLayerId));

        Assert.some(targetLayerId, eventDbManager.getEventLayerForEventAndUser(event.getId(), uid).map(EventLayer::getLayerId));
        Assert.isTrue(layerActions.getLayer(Option.of(uid), Option.of(defaultLayerId), Option.empty(), Option.empty()).isDefault());

        layerActions.deleteLayer(uid, defaultLayerId, Option.empty(), Option.empty());
        Assert.isTrue(layerActions.getLayer(Option.of(uid), Option.of(targetLayerId), Option.empty(), Option.empty()).isDefault());

        layerActions.deleteLayer(uid, targetLayerId, Option.empty(), Option.empty());
        Assert.none(eventDbManager.getEventLayerForEventAndUser(event.getId(), uid));
    }

    @Test
    public void deleteLayerShared() {
        long layerId = user2.getDefaultLayerId();

        layerRoutines.startNewSharing(uid, layerId, LayerActionClass.EDIT);

        Assert.some(layerRoutines.getLayerUser(layerId, uid));

        layerActions.deleteLayer(uid, layerId, Option.empty(), Option.empty());

        Assert.none(layerRoutines.getLayerUser(layerId, uid));

        Assert.some(layerRoutines.findLayerById(layerId));
        Assert.some(layerRoutines.getLayerUser(layerId, uid2));
    }

    @Test
    public void getIcsFeedLayer() {
        LayerInfo feed = layerActions.getLayer(Option.of(uid), Option.of(layerActions.createIcsFeed(
                uid, "holidays://russia", Option.empty()).getId()), Option.empty(), Option.empty());

        Assert.some(feed.getIcsFeed());
    }

    @Test
    public void getOwnerByUserInfo() {
        val layerId = layerActions.createLayer(uid, createLayerData()).getLayerId();
        val owner = webNewLayerManager.getOwnerByLayerId(layerId);
        assertThat(owner.getUserId()).isEqualTo(uid.getUid());
    }

    @Test
    public void getUserLayers() {
        layerRoutines.startNewSharing(uid, user2.getDefaultLayerId(), LayerActionClass.VIEW);

        long ownedLayerId = user.getDefaultLayerId();
        long sharedLayerId = user2.getDefaultLayerId();
        long feedLayerId = layerActions.createIcsFeed(uid, "holidays://russia", Option.empty()).getId();

        ListF<LayersInfo.LayerInfo> layers = layerActions.getUserLayers(uid, Option.empty()).getLayers();

        LayersInfo.LayerInfo owned = layers.find(l -> l.getId() == ownedLayerId).get();
        LayersInfo.LayerInfo shared = layers.find(l -> l.getId() == sharedLayerId).get();
        LayersInfo.LayerInfo feed = layers.find(l -> l.getId() == feedLayerId).get();

        Assert.none(owned.getOwner());
        Assert.isTrue(owned.isOwner());

        Assert.some(0, owned.getParticipantsCount());
        Assert.isTrue(owned.isCanAddEvent());

        Assert.some(shared.getOwner());
        Assert.isFalse(shared.isOwner());

        Assert.none(shared.getParticipantsCount());
        Assert.isFalse(shared.isCanAddEvent());

        Assert.none(feed.getOwner());
        Assert.isTrue(feed.isOwner());

        Assert.isFalse(feed.isCanAddEvent());
    }

    @Test
    public void findLayersCanListForOwner() {
        long defaultLayerId = user.getDefaultLayerId();

        LayerData data = createLayerData();
        data.setAffectAvailability(Option.of(false));

        long availableLayerId = layerActions.createLayer(uid, data).getLayerId();

        long toggledOffLayerId = user2.getDefaultLayerId();

        layerRoutines.startNewSharing(uid, toggledOffLayerId, LayerActionClass.VIEW);
        layerActions.toggleLayer(uid, toggledOffLayerId, false);

        long absenceLayerId = layerRoutines.getOrCreateAbsenceLayer(uid, AbsenceType.ABSENCE);


        ListF<Layer> layers = webNewLayerManager.findLayersCanList(
                Option.of(uid), Cf.list(), Option.empty(), Option.of(true), true, true).get1();

        Assert.equals(Cf.set(defaultLayerId, absenceLayerId), layers.map(Layer::getId).unique());

        layers = webNewLayerManager.findLayersCanList(
                Option.of(uid), Cf.list(), Option.empty(), Option.of(true), false, true).get1();

        Assert.equals(Cf.set(defaultLayerId), layers.map(Layer::getId).unique());

        layers = webNewLayerManager.findLayersCanList(
                Option.of(uid), Cf.list(), Option.empty(), Option.of(false), false, true).get1();

        Assert.equals(Cf.set(toggledOffLayerId), layers.map(Layer::getId).unique());

        layers = webNewLayerManager.findLayersCanList(
                Option.of(uid), Cf.list(), Option.empty(), Option.empty(), false, false).get1();

        Assert.assertContains(layers.map(Layer::getId), availableLayerId);
        Assert.isFalse(layers.map(Layer::getId).containsTs(absenceLayerId));

        layers = webNewLayerManager.findLayersCanList(
                Option.of(uid), Cf.list("absences"), Option.empty(), Option.empty(), false, false).get1();

        Assert.equals(Cf.set(absenceLayerId), layers.map(Layer::getId).unique());
    }

    @Test
    public void findLayersCanListForGuests() {
        LayerData data = createLayerData();

        data.setParticipants(Option.of(Cf.list()));

        long privateLayerId = layerActions.createLayer(uid2, data).getLayerId();
        String privateToken = layerActions.obtainLayerPrivateToken(uid2, privateLayerId, Option.empty()).getToken();

        layerActions.updateLayer(uid2, user2.getDefaultLayerId(), false, data);

        long publicLayerId = user2.getDefaultLayerId();

        Tuple2<ListF<Layer>, ListF<String>> layers =  webNewLayerManager.findLayersCanList(
                Option.of(uid2), Cf.list(publicLayerId + "", "absences"),
                Option.of(privateToken), Option.empty(), false, false);

        Assert.equals(Cf.set(publicLayerId, privateLayerId), layers.get1().map(Layer::getId).unique());
        Assert.isEmpty(layers.get2());

        layers =  webNewLayerManager.findLayersCanList(
                Option.empty(), Cf.list(publicLayerId + "", "absences"),
                Option.of(privateToken), Option.empty(), false, false);

        Assert.equals(Cf.set(publicLayerId, privateLayerId), layers.get1().map(Layer::getId).unique());
        Assert.equals(Cf.set("absences"), layers.get2().unique());
    }

    @Test
    public void handleLayerReply() {
        LayerData data = createLayerData();
        data.setParticipants(Option.of(Cf.list(new LayerData.Participant(new Email("x@x"), LayerActionClass.VIEW))));

        long layerId = layerActions.createLayer(uid, data).getLayerId();

        String externalToken = layerInvitationManager.findLayerInvitations(layerId).single().getPrivateToken().get();

        Assert.none(layerRoutines.getLayerUser(layerId, uid2));

        layerActions.handleLayerReply(uid2, externalToken, Decision.YES, Option.empty());
        Assert.some(layerRoutines.getLayerUser(layerId, uid2));

        String user2Token = layerInvitationManager.findLayerInvitations(layerId)
                .filterMap(LayerInvitation::getPrivateToken).unique().minus1(externalToken).single();

        Assert.assertThrows(() -> layerActions.handleLayerReply(uid, user2Token, Decision.YES, Option.empty()),
                CommandRunException.class, e -> e.isSituation(Situation.NON_CORRESPONDING_UID));

        layerActions.handleLayerReply(uid2, user2Token, Decision.NO, Option.empty());
        Assert.none(layerRoutines.getLayerUser(layerId, uid2));

        Assert.assertThrows(() -> layerActions.handleLayerReply(uid, user2Token, Decision.YES, Option.empty()),
                CommandRunException.class, e -> e.isSituation(Situation.INV_IS_MISSING));

        layerActions.handleLayerReply(uid2, externalToken, Decision.NO, Option.empty());
        Assert.isEmpty(layerInvitationManager.findLayerInvitations(layerId));
    }

    @Test
    public void importIcsFromData() {
        val layerId = user.getDefaultLayerId();
        val layerData = createLayerData();
        layerData.setName(Option.empty());

        var vevent = new IcsVEvent()
                .withUid(CalendarUtils.generateExternalId())
                .withSummary("Ics event")
                .withDtStart(new LocalDate(2017, 12, 18))
                .withDtEnd(new LocalDate(2017, 12, 18));

        var data = new ImportIcsData(Option.of(vevent.makeCalendar().serializeToBytes()),
                Option.empty(), new ImportIcsData.Layer(Long.toString(layerId), layerData));
        var imported = layerActions.importIcs(uid, data);

        assertThat(layerId).isEqualTo(imported.getId());

        vevent = vevent.withUid(CalendarUtils.generateExternalId());
        val calendar = vevent.makeCalendar().addProperty(new IcsXWrCalname("Name from ics"));

        data = new ImportIcsData(Option.of(calendar.serializeToBytes()),
                Option.empty(), new ImportIcsData.Layer("new", layerData));
        imported = layerActions.importIcs(uid, data);

        assertThat(layerId).isNotEqualTo(imported.getId());

        assertThat(calendar.getXWrCalname().get()).isEqualTo(imported.getName());
    }

    @Test
    public void importIcsFromUrl() {
        testManager.runWithFileResourceJetty(port -> {
            val layerId = user.getDefaultLayerId();
            val layerData = createLayerData();

            var url = "http://localhost:" + port + "/unittest_data/db/ics/import/exdate.ics";
            var data = new ImportIcsData(Option.empty(),
                    Option.of(url), new ImportIcsData.Layer(Long.toString(layerId), layerData));
            var imported = layerActions.importIcs(uid, data);

            assertThat(layerId).isEqualTo(imported.getId());

            url = "http://localhost:" + port + "/unittest_data/db/ics/import/recurrence_id_tz.ics";

            data = new ImportIcsData(Option.empty(),
                    Option.of(url), new ImportIcsData.Layer("new", layerData));
            imported = layerActions.importIcs(uid, data);

            assertThat(layerId).isNotEqualTo(imported.getId());

            assertThat(layerData.getName().get()).isEqualTo(imported.getName());
        });
    }

    @Test
    public void importIcsToFeed() {
        LayerData data = createLayerData();

        LayerInfo imported = layerActions.importIcs(uid, new ImportIcsData(
                Option.empty(), Option.of("holidays://russia"),
                new ImportIcsData.Layer("feed", createLayerData())));

        Assert.equals(data.getName().get(), imported.getName());
        Assert.some(layerActions.getLayer(Option.of(uid), Option.of(imported.getId()), Option.empty(), Option.empty()).getIcsFeed());
    }


    private LayerData createLayerData() {
        ListF<Notification> notifications =
                Cf.list(Notification.email(Duration.standardMinutes(-15)), Notification.sms(Duration.ZERO));

        LayerData.Participant participant = new LayerData.Participant(user2.getEmail(), LayerActionClass.EDIT);

        return LayerData.empty().toBuilder()
                .color(Option.of("#89abcd"))
                .notifications(notifications)
                .name(Option.of("Название"))
                .participants(Option.of(Cf.list(participant)))
                .build();
    }
}
