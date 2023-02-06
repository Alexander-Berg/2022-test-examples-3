package ru.yandex.direct.intapi.entity.moderation.controller;

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

import ru.yandex.direct.core.entity.client.model.ClientFlags;
import ru.yandex.direct.core.entity.client.repository.ClientOptionsRepository;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.moderation.model.ClientBlockStatus;
import ru.yandex.direct.intapi.entity.moderation.model.UnblockClientResult;
import ru.yandex.direct.intapi.entity.moderation.model.UnblockClientsResponse;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Collections.singletonList;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;

@IntApiTest
@ParametersAreNonnullByDefault
@RunWith(SpringJUnit4ClassRunner.class)
public class ModerationControllerUnblockClientsTest {
    @Autowired
    private ModerationController moderationController;
    @Autowired
    private ClientOptionsRepository clientOptionsRepository;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;

    private MockMvc mockMvc;
    private ClientInfo clientInfo1;
    private ClientInfo clientInfo2;
    private ClientId clientId1;
    private ClientId clientId2;
    private UserInfo user1;
    private UserInfo user21;
    private UserInfo user22;
    private Long uid1;
    private Long uid21;
    private Long uid22;

    @Before
    public void before() {
        mockMvc = MockMvcBuilders.standaloneSetup(moderationController).build();
        clientInfo1 = steps.clientSteps().createClient(defaultClient());
        clientInfo2 = steps.clientSteps().createClient(defaultClient());
        clientId1 = clientInfo1.getClientId();
        clientId2 = clientInfo2.getClientId();
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
        UnblockClientsResponse expectedResponse = new UnblockClientsResponse(singletonList(
                new UnblockClientResult(clientId1.asLong(), ClientBlockStatus.ACTIVE)));
        checkData(singletonList(clientId1.asLong()), singletonList(uid1), expectedResponse);
    }

    @Test
    public void checkOneClientTwoUsersNotBlocked() {
        userService.unblockUser(clientId2, uid21);
        userService.unblockUser(clientId2, uid22);
        UnblockClientsResponse expectedResponse = new UnblockClientsResponse(singletonList(
                new UnblockClientResult(clientId2.asLong(), ClientBlockStatus.ACTIVE)));
        checkData(singletonList(clientId2.asLong()), List.of(uid21, uid22), expectedResponse);
    }

    @Test
    public void checkOneClientOneUserBlocked() {
        userService.blockUser(clientId1, uid1);
        UnblockClientsResponse expectedResponse = new UnblockClientsResponse(singletonList(
                new UnblockClientResult(clientId1.asLong(), ClientBlockStatus.BLOCKED)));
        checkData(singletonList(clientId1.asLong()), singletonList(uid1), expectedResponse);
    }

    @Test
    public void checkOneClientTwoUsersBlocked() {
        userService.blockUser(clientId2, uid21);
        userService.blockUser(clientId2, uid22);
        UnblockClientsResponse expectedResponse = new UnblockClientsResponse(singletonList(
                new UnblockClientResult(clientId2.asLong(), ClientBlockStatus.BLOCKED)));
        checkData(singletonList(clientId2.asLong()), List.of(uid21, uid22), expectedResponse);
    }

    @Test
    public void checkOneClientTwoUsersMixedBlocked() {
        userService.blockUser(clientId2, uid21);
        userService.unblockUser(clientId2, uid22);
        UnblockClientsResponse expectedResponse = new UnblockClientsResponse(singletonList(
                new UnblockClientResult(clientId2.asLong(), ClientBlockStatus.ACTIVE)));
        checkData(singletonList(clientId2.asLong()), List.of(uid21, uid22), expectedResponse);
    }

    @Test
    public void checkTwoClientsUsersNotBlocked() {
        userService.unblockUser(clientId1, uid1);
        userService.unblockUser(clientId2, uid21);
        userService.unblockUser(clientId2, uid22);
        UnblockClientsResponse expectedResponse = new UnblockClientsResponse(List.of(
                new UnblockClientResult(clientId1.asLong(), ClientBlockStatus.ACTIVE),
                new UnblockClientResult(clientId2.asLong(), ClientBlockStatus.ACTIVE)));
        checkData(List.of(clientId1.asLong(), clientId2.asLong()), List.of(uid1, uid21, uid22), expectedResponse);
    }

    @Test
    public void checkTwoClientsUsersMixedBlocked() {
        userService.blockUser(clientId1, uid1);
        userService.blockUser(clientId2, uid21);
        userService.unblockUser(clientId2, uid22);
        UnblockClientsResponse expectedResponse = new UnblockClientsResponse(List.of(
                new UnblockClientResult(clientId1.asLong(), ClientBlockStatus.BLOCKED),
                new UnblockClientResult(clientId2.asLong(), ClientBlockStatus.ACTIVE)));
        checkData(List.of(clientId1.asLong(), clientId2.asLong()), List.of(uid1, uid21, uid22), expectedResponse);
    }

    @Test
    public void checkTwoClientsUsersAllBlocked() {
        userService.blockUser(clientId1, uid1);
        userService.blockUser(clientId2, uid21);
        userService.blockUser(clientId2, uid22);
        UnblockClientsResponse expectedResponse = new UnblockClientsResponse(List.of(
                new UnblockClientResult(clientId1.asLong(), ClientBlockStatus.BLOCKED),
                new UnblockClientResult(clientId2.asLong(), ClientBlockStatus.BLOCKED)));
        checkData(List.of(clientId1.asLong(), clientId2.asLong()), List.of(uid1, uid21, uid22), expectedResponse);
    }

    @Test
    public void checkTwoClientsOneNotExists() {
        userService.unblockUser(clientId1, uid1);
        Long nonExistentClientId = clientId1.asLong() + 10000L;
        UnblockClientsResponse expectedResponse = new UnblockClientsResponse(List.of(
                new UnblockClientResult(clientId1.asLong(), ClientBlockStatus.ACTIVE),
                new UnblockClientResult(nonExistentClientId, "Client not found")));
        checkData(List.of(clientId1.asLong(), nonExistentClientId), List.of(uid1), expectedResponse);
    }

    @Test
    public void checkTwoClientsOneNotValid() {
        userService.blockUser(clientId1, uid1);
        userService.blockUser(clientId2, uid21);
        userService.blockUser(clientId2, uid22);
        clientOptionsRepository.setClientFlag(
                shardHelper.getShardByClientId(clientId1),
                clientId1,
                ClientFlags.CANT_UNBLOCK);
        String err = String.format("Errors found for some users: %s " +
                "([Defect{defectId=USER_CANT_BE_UNBLOCKED, params=null}])", user1.getLogin());
        UnblockClientsResponse expectedResponse = new UnblockClientsResponse(List.of(
                new UnblockClientResult(clientId1.asLong(), ClientBlockStatus.BLOCKED, err),
                new UnblockClientResult(clientId2.asLong(), ClientBlockStatus.BLOCKED)));
        checkData(List.of(clientId1.asLong(), clientId2.asLong()), List.of(uid21, uid22), expectedResponse);
    }

    private void checkData(List<Long> clientIds, List<Long> uidsForCheck, UnblockClientsResponse expectedResponse) {
        try {
            UnblockClientsResponse response = getResponse(clientIds);
            Assertions.assertThat(response.getResults())
                    .as("Проверяем данные в ответе")
                    .containsExactlyInAnyOrder(expectedResponse.getResults().toArray(new UnblockClientResult[0]));

            Assertions.assertThat(userService.massGetUser(uidsForCheck)
                            .stream()
                            .filter(u -> !u.getStatusBlocked())
                            .count())
                    .as("Смотрим результат в БД")
                    .isEqualTo(uidsForCheck.size());

        } catch (Exception e) {
            Assertions.fail("Не должно быть Exception: " + e.getMessage());
        }
    }

    private UnblockClientsResponse getResponse(List<Long> clientIds) throws Exception {
        String input = String.format("{\"client_ids\": [%s]}", StreamEx.of(clientIds).joining(","));
        String r = mockMvc
                .perform(post("/moderation/unblock_clients")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(input))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonUtils.fromJson(r, UnblockClientsResponse.class);
    }
}
