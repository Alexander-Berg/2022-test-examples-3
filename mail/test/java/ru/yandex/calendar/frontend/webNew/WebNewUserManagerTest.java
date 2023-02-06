package ru.yandex.calendar.frontend.webNew;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.joda.time.ReadableInstant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.bolts.function.Function2;
import ru.yandex.calendar.frontend.webNew.actions.UserActions;
import ru.yandex.calendar.frontend.webNew.dto.in.ReplyData;
import ru.yandex.calendar.frontend.webNew.dto.in.UserSettingsData;
import ru.yandex.calendar.frontend.webNew.dto.in.UserTimezoneData;
import ru.yandex.calendar.frontend.webNew.dto.inOut.NoNotificationsRange;
import ru.yandex.calendar.frontend.webNew.dto.inOut.TodoEmailTimes;
import ru.yandex.calendar.frontend.webNew.dto.out.UserSettingsInfo;
import ru.yandex.calendar.frontend.webNew.dto.out.WebUserInfo;
import ru.yandex.calendar.logic.beans.generated.Event;
import ru.yandex.calendar.logic.beans.generated.Office;
import ru.yandex.calendar.logic.beans.generated.SettingsYt;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.sharing.Decision;
import ru.yandex.calendar.logic.sharing.participant.ParticipantId;
import ru.yandex.calendar.logic.user.Language;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.commune.a3.action.parameter.IllegalParameterException;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

public class WebNewUserManagerTest extends WebNewTestBase {
    @Autowired
    private UserActions userActions;
    @Autowired
    private SettingsRoutines settingsRoutines;
    @Autowired
    private WebNewUserManager webNewUserManager;
    @Autowired
    private WebNewEventManager webNewEventManager;

    private static final Email EXTERNAL_EMAIL = new Email("xxx@yyy");
    private static final DateTime NOW = MoscowTime.dateTime(2017, 12, 22, 16, 0);

    @Override
    protected ReadableInstant now() {
        return NOW;
    }

    @Test
    public void getUserSettings() {
        Office office = testManager.createDefaultOffice();

        SettingsYt data = new SettingsYt();
        data.setActiveOfficeId(office.getId());

        settingsRoutines.updateSettingsYtByUid(data, uid);
        UserSettingsInfo settings = userActions.getUserSettings(uid);

        Assert.equals(user.getEmail(), settings.getEmail());
        Assert.equals(user.getLoginRaw(), settings.getLogin());

        Assert.some(office.getId(), settings.getCurrentOfficeId());
        Assert.some(settings.getCurrentOfficeTz());

        Assert.none(userActions.getUserSettings(uid2).getCurrentOfficeId());
    }

    @Test
    public void updateUserTimezone() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-19200").getUid();

        UserTimezoneData data = new UserTimezoneData("Africa/Abidjan", "Asia/Yekaterinburg");

        userActions.updateUserTimezone(uid, data);
        UserSettingsInfo settings = userActions.getUserSettings(uid);

        Assert.equals(data.getTz(), settings.getTz());
        Assert.equals(data.getLastOfferedGeoTz(), settings.getLastOfferedGeoTz());
    }

    @Test
    public void updateUserSettings() {
        UserSettingsData data = new UserSettingsData(
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty(), Option.empty());

        NoNotificationsRange noNtfRange = new NoNotificationsRange(
                Option.of(MoscowTime.today()), Option.empty(), Option.empty(), Option.empty());

        data.setNoNotificationsRange(Option.of(noNtfRange));
        data.setTodoEmailTimes(Option.of(new TodoEmailTimes(Option.of(MoscowTime.now().toLocalTime()), Option.empty())));

        data.setEmail(Option.of(new Email("xxx@yyy.kz")));
        data.setLetParticipantsEdit(Option.of(true));

        userActions.updateUserSettings(uid, data);
        UserSettingsInfo settings = userActions.getUserSettings(uid);

        Assert.some(noNtfRange.getSinceDate().get(), settings.getNoNotificationsRange().getSinceDate());
        Assert.some(data.getTodoEmailTimes().get().getPlanned().get(), settings.getTodoEmailTimes().getPlanned());

        Assert.equals(data.getEmail().get(), settings.getEmail());
        Assert.some(data.getLetParticipantsEdit().get(), settings.getLetParticipantsEdit());
    }

    @Test
    public void getNowTime() {
        Function2<Option<PassportUid>, Option<String>, LocalDateTime> get = (uid, tz) ->
                userActions.getNowTime(uid, tz.map(DateTimeZone::forID)).getNow();

        Assert.equals(NOW.toLocalDateTime(), get.apply(Option.of(uid), Option.empty()));
        Assert.equals(NOW.toLocalDateTime(), get.apply(Option.of(uid), Option.of("Europe/Kiev")));

        Assert.equals(NOW.toLocalDateTime().plusHours(2), get.apply(Option.empty(), Option.of("Asia/Yekaterinburg")));
        Assert.assertThrows(() -> get.apply(Option.empty(), Option.empty()), IllegalParameterException.class);
    }

    @Test
    public void findFavoriteContactsOrder() {
        createUserEvent(NOW.minusDays(30), user, Option.of(EXTERNAL_EMAIL), user2);
        Event event1 = createUserEvent(NOW.minusDays(20), user, Option.of(EXTERNAL_EMAIL), user2);
        Event event2 = createUserEvent(NOW.minusDays(10), user2, Option.empty(), user);

        Assert.equals(Cf.list(user2.getEmail(), EXTERNAL_EMAIL),
                userActions.findFavoriteContacts(uid, Option.of(2), Option.empty()).getEmails());

        Tuple2List.fromPairs(event1, user2, event2, user)
                .forEach((event, user) -> webNewEventManager.handleReply(
                        user.getUid(), new ReplyData(event.getId(), Decision.NO), ActionInfo.webTest()));

        Assert.equals(Cf.list(EXTERNAL_EMAIL, user2.getEmail()),
                userActions.findFavoriteContacts(uid, Option.of(2), Option.empty()).getEmails());
    }

    @Test
    public void webUserInfo() {
        WebUserInfo info = webNewUserManager.getWebUserInfoByParticipantId(
                uid, ParticipantId.yandexUid(uid2), Language.RUSSIAN);

        Assert.equals(user2.getEmail(), info.getEmail());
        Assert.some(user2.getLoginRaw(), info.getLogin());
        Assert.equals("", info.getName());

        info = webNewUserManager.getWebUserInfoByParticipantId(
                uid, ParticipantId.invitationIdForExternalUser(EXTERNAL_EMAIL), Language.ENGLISH);

        Assert.equals(EXTERNAL_EMAIL, info.getEmail());
        Assert.none(info.getLogin());
        Assert.equals("", info.getName());
    }
}
