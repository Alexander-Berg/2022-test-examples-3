package ru.yandex.market.wms.auth.service;

import java.util.List;
import java.util.Set;

import javax.validation.ValidationException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.query.LdapQuery;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.config.filters.SerialKeyColumnFilter;
import ru.yandex.market.wms.auth.config.filters.UserColumnFilter;
import ru.yandex.market.wms.auth.core.model.LdapUser;
import ru.yandex.market.wms.auth.dao.UserDao;
import ru.yandex.market.wms.auth.dao.entity.Role;
import ru.yandex.market.wms.auth.dao.entity.User;
import ru.yandex.market.wms.auth.dao.querygenerator.user.UserApiField;
import ru.yandex.market.wms.auth.model.request.UserPatchRequest;
import ru.yandex.market.wms.shared.libs.querygenerator.EnumerationOrder;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserServiceTest extends AuthIntegrationTest {

    @Autowired
    @MockBean
    private LdapTemplate ldapTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    @MockBean
    private GuidService guidService;

    @Autowired
    @SpyBean
    private UserDao userDao;

    @AfterEach
    public void reset() {
        Mockito.reset(ldapTemplate);
        Mockito.reset(guidService);
        Mockito.reset(userDao);
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
        List<User> actual = userService.findAll(UserApiField.USER_ID, EnumerationOrder.ASC, "login==AD1", 10, 0);
        assertEquals(actual.size(), expected.size());
        assertTrue(actual.containsAll(expected));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DatabaseSetup(value = "/db/dao/user/clone/before.xml", connection = "scprdd1Connection")
    @DatabaseSetup(value = "/db/dao/user/clone/before-scprd.xml")
    @DatabaseSetup(value = "/db/dao/mobile-user/insert/before.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(
            value = "/db/dao/user/clone/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT_UNORDERED,
            columnFilters = {UserColumnFilter.class}
    )
    @ExpectedDatabase(
            value = "/db/dao/user/clone/after-scprd.xml",
            assertionMode = NON_STRICT,
            columnFilters = {SerialKeyColumnFilter.class}
    )
    public void cloneUser() {
        String copyLogin = "AD2";
        List<LdapUser> expected = List.of(new LdapUser(copyLogin, "cn=" + copyLogin, copyLogin));
        when(ldapTemplate.search(any(), (AttributesMapper<Object>) any())).thenAnswer(
                (InvocationOnMock invocationOnMock) -> {
                    LdapQuery argument = invocationOnMock.getArgument(0);
                    assertEquals(argument.filter(), new EqualsFilter("sAMAccountName", copyLogin));
                    return expected;
                }
        );
        when(guidService.getGuid()).thenReturn(
                "0000001",
                "0000002",
                "0000003",
                "0000004",
                "0000005",
                "0000006",
                "0000007",
                "0000008"
        );
        doNothing().when(userDao).putUserPrefInstances(any());
        userService.cloneUser("0xD28E1CF4F2B24297567FC05E05A5B5C0", Set.of(copyLogin), null, null);
        verify(guidService, times(8)).getGuid();
    }

    @Test
    @DatabaseSetup(value = "/db/service/user/delete/scprdd1-before.xml", connection = "scprdd1Connection")
    @DatabaseSetup(value = "/db/service/user/delete/enterprise-before.xml", connection = "enterpriseConnection")
    @ExpectedDatabase(
            value = "/db/service/user/delete/scprdd1-after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    @ExpectedDatabase(
            value = "/db/service/user/delete/enterprise-after.xml",
            connection = "enterpriseConnection",
            assertionMode = NON_STRICT
    )
    public void deleteUser() {
        userService.delete("0x02996E9D");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/status/activate/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/status/activate/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void activateUser() {
        userService.activate("0x02996E");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/status/deactivate/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/status/deactivate/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void deactivateUser() {
        userService.deactivate("0x02996E");
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/get/role/roles.xml", connection = "scprdd1Connection")
    public void getRoles() {
        List<Role> expected = List.of(
                Role.builder().roleId("0x49353935E").roleName("SCE-waveplanner").tenant("INFOR").build(),
                Role.builder().roleId("0x39353935F").roleName("SCE-Administrator").tenant("INFOR").build(),
                Role.builder().roleId("0x34553935F").roleName("SCE-RF User").tenant("INFOR").build()
        );
        Set<Role> actual = userService.getRoles();
        assertTrue(actual.size() == expected.size() && actual.containsAll(expected));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/put/role/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/put/role/after.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void setRoles() {
        when(guidService.getGuid()).thenReturn("0000001", "0000002", "0000003", "0000004");
        userService.setRoles(List.of("AD1", "AD15"), List.of("SCE-Administrator", "SCE-RF User"));
        verify(guidService, times(4)).getGuid();
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/put/role/before.xml", connection = "scprdd1Connection")
    public void setNonexistentRole() {
        assertThrows(
                ValidationException.class,
                () -> userService.setRoles(List.of("AD1", "AD15"), List.of("SCE-Administrator", "SCE-RF User2")),
                "Expected no such role exception, but it didn't"
        );
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/patch/user-data/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/patch/user-data/after-email.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void patchEmail() {
        userService.patchUser("0x02996E", UserPatchRequest.builder().emailAddress("hello@yandex.ru").build());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/patch/user-data/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/patch/user-data/after-fullname.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void patchFullName() {
        userService.patchUser("0x02996E", UserPatchRequest.builder().fullName("Lionel Messi").build());
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/patch/user-data/before.xml", connection = "scprdd1Connection")
    @ExpectedDatabase(
            value = "/db/dao/user/patch/user-data/after-locale.xml",
            connection = "scprdd1Connection",
            assertionMode = NON_STRICT
    )
    public void patchLocale() {
        userService.patchUser("0x02996E", UserPatchRequest.builder().locale("aus").build());
    }
}
