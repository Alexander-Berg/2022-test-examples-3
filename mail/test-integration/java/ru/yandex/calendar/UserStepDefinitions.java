package ru.yandex.calendar;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import one.util.streamex.StreamEx;
import ru.yandex.bolts.collection.CollectorsF;
import ru.yandex.bolts.collection.Either;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.definition.User;
import ru.yandex.calendar.logic.beans.generated.Settings;
import ru.yandex.calendar.logic.beans.generated.SettingsYt;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.user.Group;
import ru.yandex.calendar.logic.user.SettingsInfo;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.logic.user.UserDao;
import ru.yandex.calendar.logic.user.UserGroupsDao;
import ru.yandex.calendar.logic.user.UserRoutines;
import ru.yandex.calendar.util.idlent.YandexUser;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox.PassportAuthDomain;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.mail.cerberus.UserType;
import ru.yandex.misc.email.Email;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static ru.yandex.calendar.definition.Constants.ADMIN_LOGIN;

@Slf4j
public class UserStepDefinitions extends BaseStepDefinitions {
    @Inject
    SettingsRoutines settingsRoutines;

    @Inject
    UserRoutines userRoutines;

    @Inject
    UserDao userDao;

    @Inject
    UserGroupsDao userGroupsDao;

    private static final long FIRST_PUBLIC_UID = 1L;
    private static final long FIRST_CORP_UID = 1120000000000000L;

    private static final AtomicLong PUBLIC_UID = new AtomicLong(FIRST_PUBLIC_UID);
    private static final AtomicLong CORP_UID = new AtomicLong(FIRST_CORP_UID);

    private static PassportUid acquireUid(UserType type) {
        switch (type) {
            case BASIC: return PassportUid.cons(PUBLIC_UID.getAndIncrement());
            case YT:    return PassportUid.cons(CORP_UID.getAndIncrement());
            default:    throw new IllegalArgumentException("Unexpected user type: " + type);
        }
    }

    private static Email resolveEmail(User user) {
        val type = user.getType();
        switch (type) {
            case BASIC: return new Email(user.getLogin() + "@yandex.ru");
            case YT:    return new Email(user.getLogin() + "@yandex-team.ru");
            default:    throw new IllegalArgumentException("Unexpected user type: " + type);
        }
    }

    @Before(order = 0)
    public void before(Scenario scenario) {
        createUser(new User(ADMIN_LOGIN, UserType.YT));
        userGroupsDao.addGroup(Either.left(getUser(ADMIN_LOGIN).getUid()), Group.SUPER_USER);
    }

    @After(order = Integer.MIN_VALUE)
    public void after(Scenario scenario) {
        val allUids = StreamEx.of(userDao.findSettings())
            .map(SettingsInfo::getUid)
            .collect(CollectorsF.toList());
        userRoutines.deleteUsers(allUids, ActionInfo.webTest());
    }

    @Given("user")
    public void createUser(User user) {
        val uid = acquireUid(user.getType());
        val email = resolveEmail(user);
        val login = email.getLocalPart();
        val domain = user.getType() == UserType.BASIC
            ? PassportAuthDomain.PUBLIC
            : PassportAuthDomain.YANDEX_TEAM_RU;

        // test only bullshit, remove asap
        val testUser = new YandexUser(uid, PassportLogin.cons(login), Option.of(login), Option.of(email),
            Option.of(domain.getDomain().getDomain()), Option.empty(), Option.empty());
        userManager.registerYandexUserForTest(testUser);

        settingsRoutines.getOrCreateSettingsByUid(uid);

        val settings = new Settings();
        settings.setEmail(email);
        settings.setYandexEmail(email);
        settings.setUserName(login);
        settings.setUserLogin(login);
        settings.setDomain(domain.getDomain().getDomain());

        settingsRoutines.updateSettingsByUid(settings, uid);

        if (user.getType() == UserType.YT) {
            val settingsYt = new SettingsYt();
            settingsYt.setIsDismissed(false);
            settingsYt.setIsOutlooker(false);
            settingsRoutines.updateSettingsYtByUid(settingsYt, uid);
        }
    }

    @Given("users")
    public void createUsers(List<User> users) {
        users.forEach(this::createUser);
    }
}
