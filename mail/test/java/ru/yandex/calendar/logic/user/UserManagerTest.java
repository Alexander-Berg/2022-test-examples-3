package ru.yandex.calendar.logic.user;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class UserManagerTest extends CalendarTestBase {

    // ssytnik@: bug caused when user with public uid has validated yandex-team email,
    // and his email != "login@yandex.ru", because login contains hyphens and email contains dots
    @Test
    public void dontChooseYandexTeamEmailForPublicUid() {
        PassportUid uid = PassportUid.cons(999999999999L);
        PassportLogin login = PassportLogin.cons("fake-login-yandex-public");
        ListF<Email> emails = Cf.<String>list(
            "fake.login.yandex.public@narod.ru",
            "fake-login-yandex-team@yandex-team.ru",
            "fake.login.yandex.public@yandex.ru"
        ).map(Email.consF());

        Assert.equals(new Email("fake.login.yandex.public@yandex.ru"), UserManager.chooseEmail(uid, login, emails).get());
    }

}
