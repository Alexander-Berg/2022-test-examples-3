package ru.yandex.market.pers.grade.admin.security;

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.admin.MockedPersGradeAdminTest;
import ru.yandex.market.pers.security.PersUser;

/**
 * @author dinyat
 *         05/05/2017
 */
public class JavaSecUserDetailsServiceTest extends MockedPersGradeAdminTest {

    @Autowired
    private JavaSecUserDetailsService userDetailsService;

    //Тест использует тестовый java-sec
    @Test
    @Ignore
    public void loadUserByUsername() throws Exception {
        String uid = "476356426";

        PersUser userDetails = (PersUser) userDetailsService.loadUserByUsername(uid);

        System.out.println(userDetails);
        List<String> roleList = userDetails.getRoleList();
        Assert.assertEquals(Long.parseLong(uid), userDetails.getUid());
        Assert.assertNull(userDetails.getLogin());
        Assert.assertEquals(3, roleList.size());
        Assert.assertTrue(roleList.contains("ROLE_MODERATOR"));
        Assert.assertTrue(roleList.contains("ROLE_ADMINISTRATOR"));
        Assert.assertTrue(roleList.contains("ROLE_READER"));
    }

}
