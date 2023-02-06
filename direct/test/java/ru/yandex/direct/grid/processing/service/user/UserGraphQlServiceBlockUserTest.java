package ru.yandex.direct.grid.processing.service.user;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.notification.NotificationService;
import ru.yandex.direct.core.entity.notification.container.BlockedClientNotification;
import ru.yandex.direct.core.entity.notification.container.Notification;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.validation.defects.RightsDefects;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.user.mutation.GdBlockUser;
import ru.yandex.direct.grid.processing.model.user.mutation.GdBlockUserAction;
import ru.yandex.direct.grid.processing.model.user.mutation.GdBlockUserPayload;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.defect.CommonDefects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UserGraphQlServiceBlockUserTest {

    private static final String MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    userIds \n"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.TemplateMutation<GdBlockUser, GdBlockUserPayload> BLOCK_USER_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>("blockUser", MUTATION_TEMPLATE,
                    GdBlockUser.class, GdBlockUserPayload.class);

    @Autowired
    private GraphQlTestExecutor processor;

    @Autowired
    private Steps steps;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    private User operator;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createClient(defaultClient().withRole(RbacRole.PLACER));
        operator = clientInfo.getChiefUserInfo().getUser();
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void testBlockUserBlock_Success() {
        ClientInfo clientInfo = steps.clientSteps().createClient(defaultClient().withRole(RbacRole.CLIENT));
        User userToBlock = clientInfo.getChiefUserInfo().getUser();
        createCampaigns(clientInfo, 10);

        var usersToBlock = List.of(userToBlock.getUid());
        var clientIds = List.of(userToBlock.getClientId());

        GdBlockUser input = new GdBlockUser()
                .withAction(GdBlockUserAction.BLOCK)
                .withUserIds(usersToBlock);

        Map<Long, List<Long>> campaignIdsByUser = userService.getCampaignIdsForStopping(clientIds);

        assertThat(campaignIdsByUser).isNotEmpty();

        GdBlockUserPayload gdBlockUserPayload = processor.doMutationAndGetPayload(BLOCK_USER_MUTATION, input, operator);

        assertThat(gdBlockUserPayload.getValidationResult()).isNull();
        assertThat(gdBlockUserPayload.getUserIds()).hasSize(1);

        campaignIdsByUser = userService.getCampaignIdsForStopping(clientIds);

        assertThat(campaignIdsByUser).isEmpty();

        checkUsersBlocked(usersToBlock, true, clientIds);
    }

    @Test
    public void testBlockUserBlock_TwoUsers() {
        UserInfo userToBlock = steps.userSteps().createUser(generateNewUser().withRole(RbacRole.CLIENT));
        ClientInfo clientInfo = steps.clientSteps().createClient(defaultClient().withRole(RbacRole.MANAGER));
        User userToBlock2 = clientInfo.getChiefUserInfo().getUser();
        createCampaigns(clientInfo, 1);
        var usersToBlock = List.of(userToBlock.getUid(), userToBlock2.getUid());
        var clientIds = List.of(userToBlock.getClientId(), userToBlock2.getClientId());

        Map<Long, List<Long>> campaignIdsByUser = userService.getCampaignIdsForStopping(clientIds);

        assertThat(campaignIdsByUser).isNotEmpty();

        GdBlockUser input = new GdBlockUser()
                .withAction(GdBlockUserAction.BLOCK)
                .withUserIds(usersToBlock);

        GdBlockUserPayload gdBlockUserPayload = processor.doMutationAndGetPayload(BLOCK_USER_MUTATION, input, operator);

        GdDefect expectedGdDefect = toGdDefect(
                path(field("users"), index(1)),
                RightsDefects.noRights());

        assertThat(gdBlockUserPayload.getValidationResult())
                .isNotNull();
        assertThat(gdBlockUserPayload.getValidationResult().getErrors())
                .containsExactly(expectedGdDefect);

        assertThat(gdBlockUserPayload.getUserIds()).hasSize(0);

        campaignIdsByUser = userService.getCampaignIdsForStopping(clientIds);

        assertThat(campaignIdsByUser).isNotEmpty();

        checkUsersBlocked(usersToBlock, false, clientIds);
    }

    @Test
    public void testBlockUserBlock_TwoUsers_FirstNonExist() {
        long nonExistentUid = Long.MAX_VALUE;
        UserInfo userToBlock = steps.userSteps().createUser(generateNewUser().withRole(RbacRole.CLIENT));
        var usersToBlock = List.of(nonExistentUid, userToBlock.getUid());

        GdBlockUser input = new GdBlockUser()
                .withAction(GdBlockUserAction.BLOCK)
                .withUserIds(usersToBlock);

        GdBlockUserPayload gdBlockUserPayload = processor.doMutationAndGetPayload(BLOCK_USER_MUTATION, input, operator);

        GdDefect expectedGdDefect = toGdDefect(
                path(field("users"), index(0)),
                CommonDefects.objectNotFound());

        assertThat(gdBlockUserPayload.getValidationResult())
                .isNotNull();
        assertThat(gdBlockUserPayload.getValidationResult().getErrors())
                .containsExactly(expectedGdDefect);
    }

    @Test
    public void testBlockUserUnBlock_Success() {
        UserInfo userToBlock = steps.clientSteps().createDefaultClient(
                generateNewUser()
                        .withRole(RbacRole.CLIENT)
                        .withStatusBlocked(true)).getChiefUserInfo();

        ClientInfo clientInfo = steps.clientSteps().createClient(
                new ClientInfo()
                        .withClient(defaultClient().withRole(RbacRole.CLIENT))
                        .withChiefUserInfo(new UserInfo().withUser(generateNewUser().withStatusBlocked(true)))
        );
        User userToBlock2 = clientInfo.getChiefUserInfo().getUser();
        var usersToUnBlock = List.of(userToBlock.getUid(), userToBlock2.getUid());

        GdBlockUser input = new GdBlockUser()
                .withAction(GdBlockUserAction.UNBLOCK)
                .withUserIds(usersToUnBlock);

        GdBlockUserPayload gdBlockUserPayload = processor.doMutationAndGetPayload(BLOCK_USER_MUTATION, input, operator);

        assertThat(gdBlockUserPayload.getValidationResult())
                .isNull();

        assertThat(gdBlockUserPayload.getUserIds()).hasSize(2);

        checkUsersBlocked(usersToUnBlock, false, Collections.emptyList());
    }

    private void createCampaigns(ClientInfo clientInfo, int count) {
        for (int i = 0; i < count; i++) {
            steps.campaignSteps().createActiveTextCampaign(clientInfo);
        }
    }

    private void checkUsersBlocked(Collection<Long> userIds, Boolean blocked, Collection<ClientId> clientIds) {
        userService.massGetUser(userIds)
                .forEach(user -> assertThat(user.getStatusBlocked()).isEqualTo(blocked));

        if (blocked) {
            var argumentCaptor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationService).addNotification(argumentCaptor.capture());

            var expectedNotifications = mapList(clientIds,
                    clientId -> new BlockedClientNotification().withClientId(clientId));
            assertThat(argumentCaptor.getAllValues())
                    .is(matchedBy(beanDiffer(expectedNotifications)));
        } else {
            verifyZeroInteractions(notificationService);
        }
    }
}
