package ru.yandex.direct.core.entity.client.service;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientServiceBindClientsToManagerTest {
    @Autowired
    private ClientService clientService;

    @Autowired
    private TestClientRepository testClientRepository;

    @Autowired
    private Steps steps;

    private UserInfo manager;
    private UserInfo clientUser;

    @Before
    public void before() {
        manager = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER).getChiefUserInfo();
        clientUser = steps.userSteps().createDefaultUser();
    }

    @Test
    public void bindManagerToClient() {
        clientService.bindClientsToManager(manager.getUid(), List.of(clientUser.getClientId()));
        List<Long> clientsOfManagerWithoutCampaigns =
                testClientRepository.getBindedClientsToManager(clientUser.getShard(), manager.getUid());
        assertThat(clientsOfManagerWithoutCampaigns)
                .containsExactly(clientUser.getClientId().asLong());
    }
}
