package ru.yandex.autotests.directapi.steps;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.autotests.directapi.model.User;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by mariabye on 03.06.2015.
 */
public class SomeTest {
    @Ignore
    @Test
    public void userTest(){
        String login = "at-direct-api-test";
        User user = User.get(login);
        assertThat(user.getLogin(), equalTo(login));
    }
}
