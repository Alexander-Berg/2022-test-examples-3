package ru.yandex.market.mbo.tms.report;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.market.mbo.user.MboUser;
import ru.yandex.market.mbo.user.Role;
import ru.yandex.market.mbo.user.UserManagerMock;
import ru.yandex.market.mbo.yt.TestYtWrapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static ru.yandex.market.mbo.tms.report.MboUsersUploaderYtService.ID;
import static ru.yandex.market.mbo.tms.report.MboUsersUploaderYtService.LINK_NAME;
import static ru.yandex.market.mbo.tms.report.MboUsersUploaderYtService.LOGIN;
import static ru.yandex.market.mbo.tms.report.MboUsersUploaderYtService.NAME;
import static ru.yandex.market.mbo.tms.report.MboUsersUploaderYtService.ROLES;
import static ru.yandex.market.mbo.tms.report.MboUsersUploaderYtService.STAFF_LOGIN;
import static ru.yandex.market.mbo.tms.report.MboUsersUploaderYtService.SUPERVISORS_IDS;

public class MboUsersUploaderExecutorTest {

    private static final MboUser USER_1 = new MboUser("login1", 1L, "fullname1", "email",
            null);
    private static final MboUser USER_2 = new MboUser("login2", 2L, "fullname2", null,
            "staffLogin1");
    private static final MboUser USER_3 = new MboUser("login3", 3L, "fullname3", "email",
            "staffLogin2");
    private static final MboUser USER_4 = new MboUser("login4", 4L, "fullname4", null,
            null);
    private static final MboUser USER_5 = new MboUser("login5", 5L, "fullname5", "email",
            null);

    private UserManagerMock userManager;
    private MboUsersUploaderExecutor mboUsersUploaderExecutor;
    private MboUsersUploaderYtService mboUsersUploaderYtService;
    private TestYtWrapper yt;
    private final YPath folder = YPath.simple("//home/test");

    @Before
    public void setup() {
        userManager = new UserManagerMock();
        yt = new TestYtWrapper();
        mboUsersUploaderYtService = spy(new MboUsersUploaderYtService(yt, folder));
        mboUsersUploaderExecutor = new MboUsersUploaderExecutor(mboUsersUploaderYtService, userManager);
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    @Test
    public void generalTest() throws Exception {
        userManager.addUserWithRole(USER_1, 2);
        userManager.addUserWithRole(USER_2, 2);
        userManager.addUserWithRole(USER_3, 3);
        userManager.addRole(USER_3.getUid(), 2);
        userManager.addUserWithRole(USER_4, 2);
        userManager.addRole(USER_4.getUid(), 10);
        userManager.addUserWithRole(USER_5, 6);
        userManager.setUserSubordinates(USER_3.getUid(), Arrays.asList(USER_1.getUid(), USER_4.getUid()));
        userManager.setUserSubordinates(USER_4.getUid(), Collections.singletonList(USER_5.getUid()));
        userManager.setUserSubordinates(USER_2.getUid(), Collections.singletonList(USER_1.getUid()));

        mboUsersUploaderExecutor.doRealJob(null);

        doReturn("0").when(mboUsersUploaderYtService).getCurrentTableName();

        assertThat(yt.cypress().list(folder).size()).isEqualTo(2);
        assertThat(yt.cypress().list(folder).stream().filter(this::filterIsLink).count()).isEqualTo(1);
        YPath table = folder.child(yt.cypress().list(folder)
            .stream().filter(node -> !filterIsLink(node)).findFirst().get().getValue());
        List<YTreeMapNode> rows = Lists.newArrayList(yt.tables().read(table, YTableEntryTypes.YSON));
        assertThat(rows.size()).isEqualTo(5);
        Map<Long, YTreeMapNode> usersInYt = rows.stream()
                .collect(Collectors.toMap(row -> row.getLong(ID), Function.identity()));
        assertRowInYt(usersInYt, USER_1, Collections.singletonList(2), Arrays.asList(USER_3.getUid(), USER_2.getUid()));
        assertRowInYt(usersInYt, USER_2, Collections.singletonList(2), Collections.emptyList());
        assertRowInYt(usersInYt, USER_3, Arrays.asList(2, 3), Collections.emptyList());
        assertRowInYt(usersInYt, USER_4, Arrays.asList(2, 10), Collections.singletonList(USER_3.getUid()));
        assertRowInYt(usersInYt, USER_5, Collections.singletonList(6), Collections.singletonList(USER_4.getUid()));

        for (int i = 0; i < MboUsersUploaderYtService.MAX_LIVING_SESSION + 1; i++) {
            mboUsersUploaderExecutor.doRealJob(null);
            doReturn(String.valueOf(i + 1)).when(mboUsersUploaderYtService).getCurrentTableName();
        }

        assertThat(yt.cypress().list(folder).size()).isEqualTo(MboUsersUploaderYtService.MAX_LIVING_SESSION + 1);
        assertThat(yt.cypress().list(folder).stream().filter(this::filterIsLink).count()).isEqualTo(1);
    }

    private void assertRowInYt(Map<Long, YTreeMapNode> rows, MboUser mboUser, Collection<Integer> roles,
                               Collection<Long> supervisorsIds) {
        assertThat(rows).containsKey(mboUser.getUid());
        YTreeMapNode row = rows.get(mboUser.getUid());
        assertThat(row.getString(NAME)).isEqualTo(mboUser.getFullname());
        assertThat(row.getString(LOGIN)).isEqualTo(mboUser.getLogin());
        assertThat(row.getString(STAFF_LOGIN)).isEqualTo(mboUser.getStaffLogin());
        assertThat(row.get(ROLES).get().asList().stream().map(YTreeNode::stringValue))
                .containsExactlyInAnyOrder(decodeRoles(roles));
        assertThat(row.get(SUPERVISORS_IDS).get().asList().stream().map(YTreeNode::longValue))
                .containsExactlyInAnyOrder(supervisorsIds.toArray(new Long[0]));
    }

    private String[] decodeRoles(Collection<Integer> rolesIds) {
        Map<Integer, String> roleIdToName = userManager.getAllRoles().stream()
                .collect(Collectors.toMap(Role::getId, Role::getName));
        return rolesIds.stream()
                .map(roleId ->  roleIdToName.getOrDefault(roleId, "unknown"))
                .toArray(String[]::new);
    }

    private boolean filterIsLink(YTreeStringNode node) {
        return node.stringValue().equals(LINK_NAME + "&");
    }

}
