package ru.yandex.calendar.logic.notification;

import java.util.Map;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.EventNotification;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.event.dao.EventLayerDao;
import ru.yandex.calendar.logic.event.dao.EventUserDao;
import ru.yandex.calendar.logic.ics.TestDateTimes;
import ru.yandex.calendar.logic.layer.LayerRoutines;
import ru.yandex.calendar.logic.user.UserInfo;
import ru.yandex.calendar.logic.user.UserManager;
import ru.yandex.calendar.test.auto.db.AbstractDbDataTest;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.util.data.AliasedMapDataProvider;
import ru.yandex.calendar.util.data.DataProvider;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class NotificationRoutinesRecalcUpdateLayerTest extends AbstractDbDataTest {

    @Autowired
    protected NotificationRoutines notificationRoutines;
    @Autowired
    protected LayerRoutines layerRoutines;
    @Autowired
    protected EventLayerDao eventLayerDao;
    @Autowired
    private UserManager userManager;
    @Autowired
    private TestManager testManager;
    @Autowired
    private EventUserDao eventUserDao;
    @Autowired
    private NotificationDbManager notificationDbManager;

    private DataProvider createDataProvider(boolean withNtf, long layerId) {
        Map<String, String> m = Cf.hashMap();
        m.put("l", "1");
        m.put("l_id", Long.toString(layerId));
        m.put("l_name", "new layer name");
        m.put("u", "1");
        m.put("u_css_class", "any");
        String[] colorParams = new String[] {
            "u_head_bg_color", "u_head_fg_color", "u_body_bg_color"
        };
        for (String colorParam : colorParams) {
            m.put(colorParam, "#FFFFFF");
        }
        m.put("u_is_default", "1");
        m.put("u_process_ntf", "1");
        if (withNtf) {
            m.put("u_n", "1");
            m.put("u_n_offsets_min", Long.toString(TestManager.DEF_NTF_LAYER_OFFSET));
            m.put("u_n_is_email_notify", "1");
            m.put("u_n_is_sms_notify", "0");
        }
        m.put("apply_to_events", "1");
        return new AliasedMapDataProvider(m);
    }

    private ListF<EventNotification> getNotifications(PassportUid uid, long eventId) {
        ListF<Long> euIds = Cf.list(eventUserDao.findEventUserByEventIdAndUid(eventId, uid).get().getId());
        return notificationDbManager.getNotificationsByEventUserIds(euIds).single().getEventNotifications();
    }

    @Test
    public void singleEvent() {
        UserInfo user = testManager.prepareUser("yandex-team-mm-10221").getUserInfo();
        PassportUid uid = user.getUid();

        final ActionInfo actionInfo = ActionInfo.webTest();
        Instant nextDatyTs = TestDateTimes.addDaysMoscow(actionInfo.getNow(), 1);
        Event event = testManager.createSingleEventWithNotification(uid, nextDatyTs);
        long layerId = eventLayerDao.findEventLayersByEventId(event.getId()).single().getLayerId();

        testManager.updateEventTimeIndents(event);

        // test update with new notification
        DataProvider dp = createDataProvider(true, layerId);
        layerRoutines.updateLayer(user, dp, actionInfo);

        EventNotification n = getNotifications(uid, event.getId()).first();
        Instant actual = n.getNextSendTs().get().minus(Duration.standardMinutes(n.getOffsetMinute()));
        Assert.equals(nextDatyTs, actual);

        // test update with removing notification
        dp = createDataProvider(false, layerId);
        layerRoutines.updateLayer(user, dp, actionInfo);
        Assert.isEmpty(getNotifications(uid, event.getId()));
    }

}
