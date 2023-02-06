package ru.yandex.autotests.direct.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.autotests.direct.utils.model.MongoUser;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Roman Kuhta (kuhtich@yandex-team.ru)
 */
public class UsersTest {

    private String userLogin = "testlogin";
    private String userPass = "testpass";
    @Before
    public void setUp() {
        MongoUser.saveUser(userLogin, userPass);
    }

    @Test
    public void getUserTest() {
        MongoUser user = MongoUser.get(userLogin);
        assertThat("Пароль верный", user, having(on(MongoUser.class).getLogin(), equalTo(userLogin)));
    }

    @After
    public void tearDown() {
        MongoUser.removeUser(userLogin);
    }
}
