package ru.yandex.market.tsum.core.auth;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.core.TestMongo;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 30/06/2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class, TsumUserDao.class})
public class TsumUserDaoTest {

    @Autowired
    private TsumUserDao dao;

    @Test
    public void testAddRoleToEmptyUser() throws Exception {
        dao.addRole("user42", "admin");
        TsumUser user = dao.getUser("user42");
        Assert.assertTrue(user.isAdmin());
    }

    @Test
    public void testRoles() throws Exception {
        TsumUser user = new TsumUser("user21", new HashSet<>(Arrays.asList("a", "b", "c")));
        dao.saveUser(user);
        Assert.assertEquals(user, dao.getUser("user21"));

        dao.addRole("user21", "d");
        dao.removeRole("user21", "b");
        Assert.assertEquals(new HashSet<>(Arrays.asList("a", "c", "d")), dao.getUser("user21").getRoles());
    }

    @Test
    public void testComplicatedRole() {
        TsumUser user = new TsumUser("login",
            new HashSet<>(Arrays.asList("a/admin", "a/b/admin", "b/c/user", "b/manager", "c/d/e/user", "owner")));
        dao.saveUser(user);
        Assert.assertTrue(user.hasRole("a/admin"));
        Assert.assertTrue(user.hasRole("a/b/admin"));
        Assert.assertTrue(user.hasRole("b/c/d/user"));
        Assert.assertTrue(user.hasRole("b/manager"));
        Assert.assertTrue(user.hasRole("b/c/d/e/manager"));
        Assert.assertTrue(user.hasRole("b/c/d/e/manager"));
        Assert.assertTrue(user.hasRole("b/c/d/e/owner"));
        Assert.assertTrue(user.hasRole("owner"));

        Assert.assertFalse(user.hasRole("a/manager"));
        Assert.assertFalse(user.hasRole("b/user"));
        Assert.assertFalse(user.hasRole("a/manager"));
        Assert.assertFalse(user.hasRole("c/d/user"));
        Assert.assertFalse(user.hasRole("c/d/e1/user"));
    }
}
