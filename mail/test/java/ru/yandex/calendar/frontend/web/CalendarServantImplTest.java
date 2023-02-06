package ru.yandex.calendar.frontend.web;

import java.util.Optional;

import Yandex.RequestPackage.ParamValue;
import Yandex.RequestPackage.RequestData;
import lombok.val;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.web.cmd.generic.CommandExecutor;
import ru.yandex.calendar.frontend.webNew.WebNewTestWithResourcesBase;
import ru.yandex.calendar.frontend.webNew.actions.LayerActions;
import ru.yandex.calendar.frontend.webNew.dto.in.LayerData;
import ru.yandex.calendar.logic.event.dao.EventDao;
import ru.yandex.calendar.logic.event.reminders.RemindersClient;
import ru.yandex.calendar.logic.notification.Notification;
import ru.yandex.calendar.logic.sharing.perm.LayerActionClass;
import ru.yandex.inside.passport.PassportUidOrZero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.calendar.logic.sharing.perm.EventActionClass.NONE;

@ContextConfiguration(classes = {CalendarServantA3Configuration.class, TestConfiguration.class})
public class CalendarServantImplTest extends WebNewTestWithResourcesBase {
    @Autowired
    private LayerActions layerActions;
    @Autowired
    private EventDao eventDao;
    @Autowired
    private CalendarServantImpl calendarServant;
    private AuthInfo authInfo;

    @Before
    public void prepare() {
        authInfo = new AuthInfo(PassportUidOrZero.fromUid(uid), Option.empty(), Option.empty());
    }

    @Test
    public void createEventClosedLayerClosedRequest() {
        val layerId = createLayer(true);

        val requestData = constructRequestData(true, layerId);
        calendarServant.uiCreateEvent(authInfo, requestData);

        checkEventIsClosed(layerId);
    }

    @Test
    public void createEventClosedLayerOpenedRequest() {
        val layerId = createLayer(true);

        val requestData = constructRequestData(false, layerId);
        calendarServant.uiCreateEvent(authInfo, requestData);

        checkEventIsClosed(layerId);
    }

    @Test
    public void createEventOpenedLayerClosedRequest() {
        val layerId = createLayer(false);

        val requestData = constructRequestData(true, layerId);
        calendarServant.uiCreateEvent(authInfo, requestData);

        checkEventIsClosed(layerId);
    }

    @Test
    public void createEventOpenedLayerOpenedRequest() {
        val layerId = createLayer(false);

        val requestData = constructRequestData(false, layerId);
        calendarServant.uiCreateEvent(authInfo, requestData);

        checkEventIsClosed(layerId);
    }

    private RequestData constructRequestData(boolean closed, long layerId) {
        val requestData = new RequestData();
        requestData.queryArgs = new ParamValue[]{
                new ParamValue("uid", uid.toString()),
                new ParamValue("e", "5"),
                new ParamValue("e_organizer", user.getEmail().toString()),
                new ParamValue("e_perm_all", closed ? "none" : "view"),
                new ParamValue("e_is_all_day", "0"),
                new ParamValue("e_attendees", "conf_rr_7_9@yandex-team.ru"),
                new ParamValue("e_location", ""),
                new ParamValue("e_name", "Без названия"),
                new ParamValue("e_description", ""),
                new ParamValue("e_timezone", "Europe/Moscow"),
                new ParamValue("e_start_ts", "2019-09-13T09:30:00"),
                new ParamValue("e_end_ts", "2019-09-13T10:00:00"),
                new ParamValue("e_perm_participants", "view"),
                new ParamValue("e_l", "1"),
                new ParamValue("e_l_layer_id", Long.toString(layerId)),
                new ParamValue("u", "1"),
                new ParamValue("u_availability", "busy"),
                new ParamValue("u_decision", "yes"),
        };
        return requestData;
    }

    private void checkEventIsClosed(long layerId) {
        val events = eventDao.findEventsByLayerId(layerId);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getCreatorUid()).isEqualTo(uid);
        assertThat(events.get(0).getName()).isEqualTo("Без названия");
        assertThat(events.get(0).getPermAll()).isEqualTo(NONE);
    }

    private long createLayer(boolean isClosed) {
        val notifications =
                Cf.list(Notification.email(Duration.standardMinutes(-15)), Notification.sms(Duration.ZERO));

        val participant = new LayerData.Participant(user2.getEmail(), LayerActionClass.EDIT);

        val layerData = LayerData.empty().toBuilder()
                .color(Option.of("#89abcd"))
                .notifications(notifications)
                .isEventsClosedByDefault(Optional.of(isClosed))
                .name(Option.of("Название"))
                .participants(Option.of(Cf.list(participant)))
                .build();
        return layerActions.createLayer(uid, layerData).getLayerId();
    }
}

@Configuration
class TestConfiguration {
    @Bean
    public CommandExecutor commandExecutor() {
        return new CommandExecutor();
    }

    @Bean
    public RemindersClient remindersClient() {
        return mock(RemindersClient.class);
    }
}
