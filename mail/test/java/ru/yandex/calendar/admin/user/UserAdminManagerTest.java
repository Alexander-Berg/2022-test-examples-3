package ru.yandex.calendar.admin.user;

import java.util.NoSuchElementException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
public class UserAdminManagerTest extends AbstractConfTest {

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private TestManager testManager;


    @Test
    public void getUserByAny() {
        UserAdminManager userAdminManager = new UserAdminManager();
        applicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(
                userAdminManager, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, true);

        TestUserInfo user = testManager.prepareUser("yandex-team-mm-19200");
        Assert.equals(user.getUid(), userAdminManager.getUserByAny(String.valueOf(user.getUid())).getUid());
        Assert.equals(user.getUid(), userAdminManager.getUserByAny(user.getEmail().getEmail()).getUid());
        Assert.equals(user.getUid(), userAdminManager.getUserByAny(user.getLogin().getRawValue()).getUid());

        testManager.cleanUser(user.getUid());
        try {
            userAdminManager.getUserByAny(String.valueOf(user.getUid()));
        } catch (NoSuchElementException e) {
            // ok
        }
    }

}
