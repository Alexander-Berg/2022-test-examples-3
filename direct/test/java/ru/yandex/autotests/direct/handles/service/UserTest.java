package ru.yandex.autotests.direct.handles.service;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.autotests.direct.handles.beans.MongoUserBean;
import ru.yandex.autotests.directapi.model.User;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Created by mariabye on 01.06.2015.
 */
public class UserTest {

    @Ignore
    @Test
    public void UserTest(){
        MongoUserService mongoUserService = new MongoUserService();
        MongoUserBean user = new MongoUserBean();
        user.setLogin("at-test1");
        mongoUserService.saveUserInMongo(user);
    }

}
