package ru.yandex.market.mbo.reactui.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.common.gwt.security.AccessControlManager;
import ru.yandex.common.gwt.security.BlackboxManager;
import ru.yandex.common.gwt.shared.User;
import ru.yandex.common.gwt.shared.UserWithBlackboxFields;
import ru.yandex.common.gwt.shared.blackbox.BlackBoxField;
import ru.yandex.market.mbo.reactui.service.idm.IdmService;
import ru.yandex.market.mbo.security.MboRoles;
import ru.yandex.market.mbo.security.idm.IdmStatusResult;
import ru.yandex.market.mbo.security.idm.IdmUser;
import ru.yandex.market.mbo.user.MboUser;
import ru.yandex.market.mbo.user.MboUserWithRoles;
import ru.yandex.market.mbo.user.UserManager;
import ru.yandex.market.mbo.user.UserManagerMock;

/**
 * @author apluhin
 * @created 3/26/21
 */
public class IdmServiceTest {

    private static final MboUser MBO_USER = new MboUser("login", 1, "fullname", "", "login");
    private static final int SOME_ROLE = 7;

    private IdmService idmService;
    private BlackboxManager blackboxManager;
    private UserManager userManager;

    @Mock
    private AccessControlManager accessControlManager;

    @Before
    public void setUp() throws Exception {
        blackboxManager = Mockito.mock(BlackboxManager.class);
        userManager = new UserManagerMock();

        idmService = new IdmService(
                userManager,
                blackboxManager,
                accessControlManager
        );
    }

    @Test
    public void removeRoleForUnknownUser() throws Exception {
        IdmStatusResult idmStatusResult = idmService.removeRole("login", "user");
        Mockito.verify(blackboxManager, Mockito.times(0))
                .getUserInfo(Mockito.any(), Mockito.any(), Mockito.any());
        Assertions.assertThat(idmStatusResult.getCode()).isEqualTo(1);
    }

    @Test
    public void removeRoleForKnownUser() throws Exception {
        userManager.addUser(MBO_USER);
        userManager.setUserRole(MBO_USER.getUid(), MboRoles.ADMIN);
        IdmStatusResult idmStatusResult = idmService.removeRole(MBO_USER.getLogin(), "admin");
        Mockito.verify(blackboxManager, Mockito.times(0))
                .getUserInfo(Mockito.any(), Mockito.any());
        Assertions.assertThat(idmStatusResult.getCode()).isEqualTo(0);
        Assertions.assertThat(userManager.getAllRoleUsersWithRoles()).isEmpty();
    }

    @Test
    public void addRoleToUnknownUser() throws Exception {
        HashMap<BlackBoxField, String> dbfields = new HashMap<>();
        dbfields.put(BlackBoxField.FIO, "fio");
        dbfields.put(BlackBoxField.EMAIL, "");
        User someUser = new User(MBO_USER.getLogin(), MBO_USER.getUid(), System.currentTimeMillis());
        UserWithBlackboxFields userWithBlackboxFields = new UserWithBlackboxFields(someUser, dbfields);
        Mockito.when(blackboxManager.getUserInfo(
                Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any())
        ).thenReturn(userWithBlackboxFields);

        IdmStatusResult idmStatusResult = idmService.addRole("login", "admin");

        Assertions.assertThat(idmStatusResult.getCode()).isEqualTo(0);
        Mockito.verify(blackboxManager, Mockito.times(1))
                .getUserInfo(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Optional<MboUserWithRoles> user = userManager.getAllRoleUsersWithRoles().stream()
                .filter(it -> it.getStaffLogin().equals("login")).findFirst();
        Assertions.assertThat(user.isEmpty()).isFalse();
    }

    @Test
    public void addRoleToKnownUser() throws Exception {
        userManager.addUser(MBO_USER);

        IdmStatusResult idmStatusResult = idmService.addRole("login", "admin");

        Assertions.assertThat(idmStatusResult.getCode()).isEqualTo(0);
        Mockito.verify(blackboxManager, Mockito.times(0))
                .getUserInfo(Mockito.any(), Mockito.any());

        Optional<MboUserWithRoles> user = userManager.getAllRoleUsersWithRoles().stream()
                .filter(it -> it.getStaffLogin().equals("login")).findFirst();
        Assertions.assertThat(user.isEmpty()).isFalse();
    }

    @Test
    public void addUnknownRoleToKnownUser() {
        userManager.addUser(MBO_USER);
        IdmStatusResult idmStatusResult = idmService.addRole("login", "admin1");
        Assertions.assertThat(idmStatusResult.getCode()).isEqualTo(1);
    }

    @Test
    public void exportFilteredUser() {
        userManager.addUser(MBO_USER);
        userManager.addUser(new MboUser("login", 2, "fullname", "", null));

        userManager.setUserRole(MBO_USER.getUid(), MboRoles.ADMIN);
        userManager.setUserRole(2, MboRoles.ADMIN);
        userManager.setUserRole(MBO_USER.getUid(), SOME_ROLE); //non exported for idm

        List<IdmUser> usersWithRole = idmService.getUsersWithRole();
        Assertions.assertThat(usersWithRole.size()).isEqualTo(1);
        List<Map<String, String>> roles = usersWithRole.get(0).getRoles();
        Assertions.assertThat(roles.size()).isEqualTo(1);
        Assertions.assertThat(roles.get(0).values().iterator().next()).isEqualTo("admin");
    }
}
