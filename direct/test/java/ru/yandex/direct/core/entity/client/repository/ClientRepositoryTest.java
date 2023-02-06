package ru.yandex.direct.core.entity.client.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.model.ClientWithUsers;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestClients.TEST_TIN;
import static ru.yandex.direct.core.testing.data.TestClients.TEST_TIN_TYPE;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientRepositoryTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private TestClientRepository testClientRepository;

    @Autowired
    private Steps steps;

    private ClientInfo firstClientInfo;
    private ClientInfo secondClientInfo;
    private ClientInfo deletedClientInfo;
    private UserInfo userWithFakeClient;

    @Before
    public void before() {
        firstClientInfo = steps.clientSteps().createDefaultClient();
        secondClientInfo = steps.clientSteps().createDefaultClient();

        checkState(firstClientInfo.getShard() == secondClientInfo.getShard(),
                "клиенты должны быть на одном шарде для удобства тестирования");

        deletedClientInfo = steps.clientSteps().createDefaultClient();
        userWithFakeClient = deletedClientInfo.getChiefUserInfo();
        testClientRepository.deleteClient(deletedClientInfo.getShard(), deletedClientInfo.getClientId());
        checkState(clientRepository.get(deletedClientInfo.getShard(), singletonList(deletedClientInfo.getClientId()))
                .isEmpty(), "новосозданный клиент должен быть удален");
    }

    @Test
    public void getClientData_ByOneId_ReturnsOne() {
        UserInfo userInfo = steps.userSteps().createDefaultUser();

        Collection<ClientId> clientIds = singletonList(userInfo.getClientInfo().getClientId());

        Collection<ClientWithUsers> clients = clientRepository.getClientData(userInfo.getShard(), clientIds);
        assertThat("количество объектов в ответе не совпадает с запросом", clients.size(), is(clientIds.size()));
    }

    @Test
    public void getClientData_ByEmptyList_ReturnsEmpty() {
        Collection<ClientWithUsers> clients = clientRepository.getClientData(1, emptyList());
        assertThat("результат ответа непустой", clients, empty());
    }

    @Test
    public void getClientData_BySeveralIds() {
        Collection<ClientId> clientIds = asList(firstClientInfo.getClientId(), secondClientInfo.getClientId());

        Collection<ClientWithUsers> clients = clientRepository.getClientData(firstClientInfo.getShard(), clientIds);
        Collection<Long> retrievedIds = mapList(clients, ClientWithUsers::getId);
        assertThat("ответ не содержит запрошенные id", retrievedIds,
                containsInAnyOrder(firstClientInfo.getClientId().asLong(), secondClientInfo.getClientId().asLong()));
    }

    @Test
    public void getClientData_ByClientIdWithoutRecordInClientTable() {
        Collection<ClientId> clientIds = singletonList(deletedClientInfo.getClientId());

        Collection<ClientWithUsers> clients = clientRepository.getClientData(deletedClientInfo.getShard(), clientIds);
        assertThat("количество объектов в ответе не совпадает с запросом", clients, hasSize(clientIds.size()));

        ClientWithUsers client = clients.iterator().next();
        assertThat("clientId должен быть проставлен несмотря на отсутствие записи в clients", client.getId(),
                is(deletedClientInfo.getClientId().asLong()));

        Collection<User> users = client.getUsers();
        assertThat("количество юзеров клиента неверно", users.size(), is(1));
        assertThat("uid юзера не совпадает с ожидаемым", users.iterator().next().getUid(),
                is(userWithFakeClient.getUid()));
    }

    @Test
    public void getClientData_CheckMarkChiefFlagSameChiefUser() {
        Collection<ClientId> clientIds = singletonList(firstClientInfo.getClientId());

        Collection<ClientWithUsers> clients = clientRepository.getClientData(firstClientInfo.getShard(), clientIds);
        User user = clients.iterator().next().getUsers().iterator().next();
        assertThat("chief id должен совпадать c uid", user.getChiefUid(), is(user.getUid()));
    }

    @Test
    public void getClientData_SeveralUsersOnOneClient() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        UserInfo userInfo1 = clientInfo.getChiefUserInfo();
        UserInfo userInfo2 = steps.userSteps().createRepresentative(clientInfo);

        Collection<ClientWithUsers> clients = clientRepository
                .getClientData(clientInfo.getShard(), singletonList(clientInfo.getClientId()));
        assumeThat("ожидается только 1 клиент", clients, hasSize(1));

        ClientWithUsers client = clients.iterator().next();
        assumeThat("ожидается клиент с  указанным id", client.getId(),
                is(clientInfo.getClientId().asLong())); // assume!

        Collection<Long> uids = mapList(client.getUsers(), User::getUid);
        assertThat("ожидается 2 юзера с указанными uid", uids,
                containsInAnyOrder(userInfo1.getUid(), userInfo2.getUid()));
    }

    @Test
    public void getClientData_ReturnDataSortedByLogins() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        UserInfo userInfo1 = clientInfo.getChiefUserInfo();
        UserInfo userInfo2 = steps.userSteps().createRepresentative(clientInfo);

        List<ClientWithUsers> clients = clientRepository
                .getClientData(clientInfo.getShard(), singletonList(clientInfo.getClientId()));
        ClientWithUsers client = clients.get(0);

        List<String> insertedSortedLogins =
                Arrays.asList(userInfo1.getUser().getLogin(), userInfo2.getUser().getLogin());
        Collections.sort(insertedSortedLogins);
        List<String> logins = mapList(client.getUsers(), User::getLogin);
        assertThat("логины упорядочены по алфавиту", logins, equalTo(insertedSortedLogins));
    }

    @Test
    public void getClientsDataForNds_successPath() {
        var agencyClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        var clientInfo = steps.clientSteps().createDefaultClientUnderAgency(agencyClientInfo);

        var retList = clientRepository.getClientsDataForNds(
                clientInfo.getShard(),
                List.of(
                        clientInfo.getClientId(),
                        ClientId.fromLong(clientInfo.getClientId().asLong() + 100000000)
                ));
        assertThat(retList.size(), is(1));

        var ret = retList.get(0);
        assertThat(ret.getId(), is(clientInfo.getClientId().asLong()));
        assertThat(ret.getAgencyClientId(), is(agencyClientInfo.getClientId().asLong()));
        assertThat(ret.getNonResident(), is(false));
    }

    @Test
    public void getTinAndTinType() {
        List<Client> actual = clientRepository.get(firstClientInfo.getShard(), List.of(firstClientInfo.getClientId()));
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(actual).hasSize(1);
            var client = actual.get(0);
            softAssertions.assertThat(client.getTinType()).isEqualTo(TEST_TIN_TYPE);
            softAssertions.assertThat(client.getTin()).isEqualTo(TEST_TIN);
        });
    }
}
