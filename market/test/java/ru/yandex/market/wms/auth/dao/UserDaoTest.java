package ru.yandex.market.wms.auth.dao;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.dao.entity.SsoUserRole;
import ru.yandex.market.wms.auth.dao.entity.User;
import ru.yandex.market.wms.auth.dao.entity.UserPref;
import ru.yandex.market.wms.auth.dao.querygenerator.user.UserApiField;
import ru.yandex.market.wms.auth.model.dto.RoleDto;
import ru.yandex.market.wms.shared.libs.querygenerator.EnumerationOrder;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserDaoTest extends AuthIntegrationTest {

    @Autowired
    private UserDao userDao;

    @Test
    @DatabaseSetup(value = "/db/dao/user/create/user/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/create/user/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void createUser() {
        userDao.createUser(
                "0x02996E9DF2B24E",
                "0x02996E9BF2310",
                "cn=ad1,ou=users,ou=vms,dc=mast,dc=local",
                "ru_RU",
                "AD1",
                "AD1"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/create/sso-user/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/create/sso-user/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void createSsoUser() {
        userDao.createSsoUser(
                "0x02996E7DF2B",
                "cn=ad1,ou=users,ou=vms,dc=mast,dc=local",
                "66E9C951D43B7A1DC75235E07CA71176A1493C684D0F354A450E3B627030F7BD",
                "AD1",
                "AD1",
                "AD1"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/create/user-data/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/create/user-data/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void createUserData() {
        userDao.createUserData(
                "0x02996E9BF",
                "AD1",
                "hello@yandex.ru",
                "AD1",
                "AD1"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/create/sso-user-role/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/create/sso-user-role/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void createSsoUserRoles() {
        userDao.createSsoUserRoles(
                List.of(
                        new SsoUserRole("0x03996E7A", "0x3935393500000E"),
                        new SsoUserRole("0x04996E7D", "0x39353935000000F")
                ),
                "0x02996E7DF",
                "AD1"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/delete/user/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/delete/user/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void deleteUser() {
        userDao.deleteUser("AD1");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/delete/sso-user/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/delete/sso-user/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void deleteSsoUser() {
        userDao.deleteSsoUser("AD1");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/delete/user-data/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/delete/user-data/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void deleteUserData() {
        userDao.deleteUserData("AD1");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/delete/sso-user-role/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/delete/sso-user-role/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void deleteSsoUserRoles() {
        userDao.deleteSsoUserRoles("AD1");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/delete/sso-user-role/batch-before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/delete/sso-user-role/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void batchDeleteSsoUserRoles() {
        userDao.deleteSsoUserRoles(List.of("AD1", "AD2"));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/get/user/users.xml", connection = "scprdd1Connection")
    public void findAll() {
        List<User> expected = List.of(
                User.builder()
                        .userId("0x02996E9")
                        .active(true)
                        .ssoName("cn=ad1,ou=users,ou=vms,dc=mast,dc=local")
                        .login("AD1")
                        .locale("ru_RU")
                        .tenant("INFOR")
                        .externLogin("smbd1")
                        .userDataId("0x02996F")
                        .build(),
                User.builder()
                        .userId("0x03096E9")
                        .active(false)
                        .ssoName("cn=ad1,ou=users,ou=vms,dc=mast,dc=local")
                        .login("AD1")
                        .locale("ru_RU")
                        .tenant("INFOR")
                        .externLogin("smbd2")
                        .userDataId("0x02996E")
                        .build()
        );
        List<User> actual = userDao.findAll(
                UserApiField.USER_ID,
                EnumerationOrder.ASC,
                "login==AD1",
                1000,
                0
        );
        assertTrue(actual.size() == expected.size() && actual.containsAll(expected));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/get/role/roles.xml", connection = "scprdd1Connection")
    public void getRoleNamesByUsername() {
        Set<RoleDto> expected = Set.of(new RoleDto("SCE-waveplanner"), new RoleDto("SCE-Administrator"));
        Set<RoleDto> actual = userDao.getRoleNamesByUsername("AD1");
        assertTrue(actual.size() == expected.size() && actual.containsAll(expected));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/status/activate/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/status/activate/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void activateUser() {
        userDao.activate("0x02996E");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/status/deactivate/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/status/deactivate/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void deactivateUser() {
        userDao.deactivate("0x02996E");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/patch/user/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/patch/user/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void updateUser() {
        userDao.updateUser("0x02996E", "en_US");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/patch/user-data/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/patch/user-data/after-email.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void updateUserDataEmail() {
        userDao.updateUserData("0xD28E1CF4F2B24297567FC05E05A5B5C2", null, "hello@yandex.ru");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/patch/user-data/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/patch/user-data/after-fullname.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void updateUserDataFullName() {
        userDao.updateUserData("0xD28E1CF4F2B24297567FC05E05A5B5C2", "Lionel Messi", null);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/get/user/users.xml", connection = "scprdd1Connection")
    public void getUserPref() {
        List<UserPref> expected = List.of(UserPref.builder()
                .userPrefId("0xD06F9AB70868DA2A298BC084AC739AF0")
                .userDataId("0x02996F")
                .tmplName("time_zone")
                .value("Europe/Moscow")
                .build());
        List<UserPref> actual = userDao.getUserPrefInstances("0x02996F", "time_zone");
        assertEquals(expected.size(), actual.size());
        assertTrue(expected.containsAll(actual));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/session/before.xml", connection = "scprdd1Connection")
    public void getSessionId() {
        assertEquals("12345678", userDao.getSessionId("AD15"));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/session/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/session/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void updateSessionId() {
        userDao.updateSessionId("AD15", "87654321");
    }


}
