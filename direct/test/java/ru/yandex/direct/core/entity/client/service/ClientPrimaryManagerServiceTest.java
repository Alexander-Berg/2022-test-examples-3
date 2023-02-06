package ru.yandex.direct.core.entity.client.service;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.container.PrimaryManagersQueryFilter;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.model.ClientPrimaryManager;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.ClientPrimaryManagerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.IdmGroupSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.Result;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientPrimaryManagerServiceTest {

    @Autowired
    private Steps steps;
    @Autowired
    private ClientPrimaryManagerService service;
    @Autowired
    private ClientRepository clientRepository;

    private ClientPrimaryManager firstPrimaryManager;
    private ClientPrimaryManager secondPrimaryManager;

    @Before
    public void setUp() {
        ClientSteps clientSteps = steps.clientSteps();
        IdmGroupSteps idmGroupSteps = steps.idmGroupSteps();
        ClientPrimaryManagerInfo firstPrimaryManagerInfo = idmGroupSteps.createIdmPrimaryManager();
        firstPrimaryManager = firstPrimaryManagerInfo.getClientPrimaryManager();
        UserInfo managerInfo = clientSteps.createClient(new ClientInfo()
                .withShard(ClientSteps.DEFAULT_SHARD)
                .withClient(defaultClient().withRole(RbacRole.MANAGER))).getChiefUserInfo();
        ClientInfo clientInfo = clientSteps.createClient(new ClientInfo()
                .withShard(ClientSteps.ANOTHER_SHARD));
        ClientPrimaryManagerInfo secondPrimaryManagerInfo = idmGroupSteps.addIdmPrimaryManager(managerInfo, clientInfo);
        secondPrimaryManager = secondPrimaryManagerInfo.getClientPrimaryManager();
    }

    @Test
    public void getAllIdmPrimaryManagers_success() {
        PrimaryManagersQueryFilter getAllFilter = PrimaryManagersQueryFilter.allIdmPrimaryManagers();
        List<ClientPrimaryManager> primaryManagers = service.getAllIdmPrimaryManagers();
        assertThat(primaryManagers).contains(firstPrimaryManager, secondPrimaryManager);
    }

    @Test
    public void getNextPageIdmPrimaryManagers_success() {
        List<ClientPrimaryManager> primaryManagers =
                service.getNextPageIdmPrimaryManagers(firstPrimaryManager.getSubjectClientId(), 1);
        assertThat(primaryManagers).containsOnly(secondPrimaryManager);
    }

    @Test
    public void getNextPageIdmPrimaryManagers_getAllWithPaging_success() {
        ArrayList<ClientPrimaryManager> primaryManagers = new ArrayList<>();
        for (List<ClientPrimaryManager> managers =
             service.getNextPageIdmPrimaryManagers(firstPrimaryManager.getSubjectClientId(), 1);
             isNotEmpty(managers);
             managers = service.getNextPageIdmPrimaryManagers(managers.get(0).getSubjectClientId(), 1)) {
            checkState(managers.size() == 1);
            primaryManagers.addAll(managers);
        }
        assertThat(primaryManagers)
                .as("all roles start from firstPrimaryManager except for it")
                .contains(secondPrimaryManager)
                .doesNotContain(firstPrimaryManager);
    }

    @Test
    public void updatePrimaryManager() {
        // Подготавливаем начальное состояние
        ClientPrimaryManagerInfo startPrimaryManagerInfo = steps.idmGroupSteps().createIdmPrimaryManager();
        Integer shard = startPrimaryManagerInfo.getSubjectClientInfo().getShard();
        ClientId subjectClientId = startPrimaryManagerInfo.getSubjectClientId();
        UserInfo newManagerInfo =
                steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER).getChiefUserInfo();
        Long managerUid = newManagerInfo.getUid();
        boolean newIsIdmPrimaryManager = false;
        steps.userSteps().mockUserInExternalClients(newManagerInfo.getUser());

        //Выполянем запрос
        Result<Long> result = service.updatePrimaryManager(new ClientPrimaryManager()
                .withSubjectClientId(subjectClientId)
                .withPrimaryManagerUid(managerUid)
                .withIsIdmPrimaryManager(newIsIdmPrimaryManager));
        checkState(result.isSuccessful(), "Unexpected error");

        //Проверяем результат
        Client client = clientRepository.get(shard, singletonList(subjectClientId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(result.getResult())
                    .as("Result")
                    .isEqualTo(subjectClientId.asLong());
            soft.assertThat(client.getPrimaryManagerUid())
                    .as("PrimaryManagerUid")
                    .isEqualTo(managerUid);
            soft.assertThat(client.getIsIdmPrimaryManager())
                    .as("IsIdmPrimaryManager")
                    .isEqualTo(newIsIdmPrimaryManager);
        });
    }

}
