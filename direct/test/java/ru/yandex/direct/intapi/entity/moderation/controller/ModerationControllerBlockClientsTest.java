package ru.yandex.direct.intapi.entity.moderation.controller;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.entity.user.service.validation.BlockUserValidationService;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.moderation.model.BlockClientResult;
import ru.yandex.direct.intapi.entity.moderation.model.BlockClientsResponse;
import ru.yandex.direct.intapi.entity.moderation.model.ClientBlockStatus;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Collections.singletonList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;

/**
 * Тесты для проверки intapi ручки для блокировки пользователей по ClientID
 */
@IntApiTest
@ParametersAreNonnullByDefault
@RunWith(SpringJUnit4ClassRunner.class)
public class ModerationControllerBlockClientsTest {
    @Autowired
    private ModerationController moderationController;
    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;

    private MockMvc mockMvc;
    private ClientInfo clientInfo1;
    private ClientInfo clientInfo2;
    private ClientId clientId1;
    private ClientId clientId2;
    private Long clientId1AsLong;
    private Long clientId2AsLong;
    private UserInfo user1;
    private UserInfo user21;
    private UserInfo user22;
    private Long uid1;
    private Long uid21;
    private Long uid22;

    protected static final String EXPECTED_CONTROLLER_MAPPING = "/moderation/block_clients";

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(moderationController).build();
        clientInfo1 = steps.clientSteps().createClient(
                defaultClient().withCreateDate(LocalDateTime.now().minusMonths(4)));
        clientInfo2 = steps.clientSteps().createClient(defaultClient());
        clientId1 = clientInfo1.getClientId();
        clientId2 = clientInfo2.getClientId();
        clientId1AsLong = clientId1.asLong();
        clientId2AsLong = clientId2.asLong();
        user1 = new UserInfo().withUser(userService.getUser(clientInfo1.getUid())).withClientInfo(clientInfo1);
        user21 = new UserInfo().withUser(userService.getUser(clientInfo2.getUid())).withClientInfo(clientInfo2);
        user22 = steps.userSteps().createUser(
                new UserInfo().withUser(generateNewUser()).withClientInfo(clientInfo2));
        uid1 = user1.getUid();
        uid21 = user21.getUid();
        uid22 = user22.getUid();

    }

    @Test
    public void checkOneClientOneUserNotBlocked() {
        userService.unblockUser(clientId1, uid1);
        BlockClientsResponse expectedResponse = new BlockClientsResponse(singletonList(
                new BlockClientResult(clientId1AsLong, ClientBlockStatus.ACTIVE)));
        checkData(singletonList(clientId1AsLong), singletonList(uid1), expectedResponse);
    }

    @Test
    public void checkOneClientTwoUsersNotBlocked() {
        userService.unblockUser(clientId2, uid21);
        userService.unblockUser(clientId2, uid22);
        BlockClientsResponse expectedResponse = new BlockClientsResponse(singletonList(
                new BlockClientResult(clientId2AsLong, ClientBlockStatus.ACTIVE)));
        checkData(singletonList(clientId2AsLong), List.of(uid21, uid22), expectedResponse);
    }

    @Test
    public void checkOneClientOneUserBlocked() {
        userService.blockUser(clientId1, uid1);
        BlockClientsResponse expectedResponse = new BlockClientsResponse(singletonList(
                new BlockClientResult(clientId1AsLong, ClientBlockStatus.BLOCKED)));
        checkData(singletonList(clientId1AsLong), singletonList(uid1), expectedResponse);
    }

    @Test
    public void checkOneClientTwoUsersBlocked() {
        userService.blockUser(clientId2, uid21);
        userService.blockUser(clientId2, uid22);
        BlockClientsResponse expectedResponse = new BlockClientsResponse(singletonList(
                new BlockClientResult(clientId2AsLong, ClientBlockStatus.BLOCKED)));
        checkData(singletonList(clientId2AsLong), List.of(uid21, uid22), expectedResponse);
    }

    @Test
    public void checkOneClientTwoUsersMixedBlocked() {
        userService.blockUser(clientId2, uid21);
        userService.unblockUser(clientId2, uid22);
        BlockClientsResponse expectedResponse = new BlockClientsResponse(singletonList(
                new BlockClientResult(clientId2AsLong, ClientBlockStatus.ACTIVE)));
        checkData(singletonList(clientId2AsLong), List.of(uid21, uid22), expectedResponse);
    }

    @Test
    public void checkTwoClientsUsersNotBlocked() {
        userService.unblockUser(clientId1, uid1);
        userService.unblockUser(clientId2, uid21);
        userService.unblockUser(clientId2, uid22);
        BlockClientsResponse expectedResponse = new BlockClientsResponse(List.of(
                new BlockClientResult(clientId1AsLong, ClientBlockStatus.ACTIVE),
                new BlockClientResult(clientId2AsLong, ClientBlockStatus.ACTIVE)));
        checkData(List.of(clientId1AsLong, clientId2AsLong), List.of(uid1, uid21, uid22), expectedResponse);
    }

    @Test
    public void checkTwoClientsUsersMixedBlocked() {
        userService.blockUser(clientId1, uid1);
        userService.blockUser(clientId2, uid21);
        userService.unblockUser(clientId2, uid22);
        BlockClientsResponse expectedResponse = new BlockClientsResponse(List.of(
                new BlockClientResult(clientId1AsLong, ClientBlockStatus.BLOCKED),
                new BlockClientResult(clientId2AsLong, ClientBlockStatus.ACTIVE)));
        checkData(List.of(clientId1AsLong, clientId2AsLong), List.of(uid1, uid21, uid22), expectedResponse);
    }

    @Test
    public void checkTwoClientsUsersAllBlocked() {
        userService.blockUser(clientId1, uid1);
        userService.blockUser(clientId2, uid21);
        userService.blockUser(clientId2, uid22);
        BlockClientsResponse expectedResponse = new BlockClientsResponse(List.of(
                new BlockClientResult(clientId1AsLong, ClientBlockStatus.BLOCKED),
                new BlockClientResult(clientId2AsLong, ClientBlockStatus.BLOCKED)));
        checkData(List.of(clientId1AsLong, clientId2AsLong), List.of(uid1, uid21, uid22), expectedResponse);
    }

    @Test
    public void checkTwoClientsOneNotExists() {
        userService.unblockUser(clientId1, uid1);
        Long nonExistentClientId = clientId1AsLong + 10000L;
        BlockClientsResponse expectedResponse = new BlockClientsResponse(List.of(
                new BlockClientResult(clientId1AsLong, ClientBlockStatus.ACTIVE),
                new BlockClientResult(nonExistentClientId, "Client not found")));
        checkData(List.of(clientId1AsLong, nonExistentClientId), List.of(uid1), expectedResponse);
    }

    @Test
    public void checkTwoClientsOneNotValid() {
        userService.unblockUser(clientId1, uid1);
        userService.unblockUser(clientId2, uid21);
        userService.unblockUser(clientId2, uid22);
        for (int i = 0; i < BlockUserValidationService.MAX_CAMPAIGNS_COUNT_FOR_STOPPING + 1; ++i) {
            steps.campaignSteps().createCampaign(
                    TestCampaigns.activeTextCampaign(null, null),
                    clientInfo1);
        }
        String err = String.format(
                "Errors found for some users: %s ([Defect{defectId=SIZE_CANNOT_BE_MORE_THAN_MAX, " +
                "params=CollectionSizeDefectParams{maxSize=%d, minSize=null}",
                user1.getLogin(),
                BlockUserValidationService.MAX_CAMPAIGNS_COUNT_FOR_STOPPING);
        BlockClientsResponse expectedResponse = new BlockClientsResponse(List.of(
                new BlockClientResult(clientId1AsLong, ClientBlockStatus.ACTIVE, err),
                new BlockClientResult(clientId2AsLong, ClientBlockStatus.ACTIVE)));
        checkData(List.of(clientId1AsLong, clientId2AsLong), List.of(uid21, uid22), expectedResponse);
    }

    private void checkData(List<Long> clientIds, List<Long> uidsForCheck, BlockClientsResponse expectedResponse) {
        try {
            BlockClientsResponse response = getResponse(clientIds);
            Assertions.assertThat(response.getResults())
                    .as("Проверяем данные в ответе")
                    .containsExactlyInAnyOrder(expectedResponse.getResults().toArray(new BlockClientResult[0]));

            Assertions.assertThat(userService.massGetUser(uidsForCheck)
                    .stream()
                    .filter(User::getStatusBlocked)
                    .count())
                    .as("Смотрим результат в БД")
                    .isEqualTo(uidsForCheck.size());

        } catch (Exception e) {
            Assertions.fail("Не должно быть Exception: " + e.getMessage());
        }
    }

    private BlockClientsResponse getResponse(List<Long> clientIds) throws Exception {
        String input = String.format("{\"client_ids\": [%s],\"ignore_sanity_checks\": true}",
                StreamEx.of(clientIds).joining(","));
        String r = mockMvc
                .perform(post(EXPECTED_CONTROLLER_MAPPING)
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(input))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonUtils.fromJson(r, BlockClientsResponse.class);
    }
}
