package ru.yandex.direct.core.entity.client.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.container.PrimaryManagersQueryFilter;
import ru.yandex.direct.core.entity.client.model.ClientPrimaryManager;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientPrimaryManagerInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientRepositoryPrimaryManagerTest {

    @Autowired
    private Steps steps;
    @Autowired
    private ClientRepository clientRepository;

    private ClientPrimaryManager firstPrimaryManager;
    private ClientPrimaryManager secondPrimaryManager;
    private ClientPrimaryManager thirdPrimaryManager;

    @Before
    public void setUp() {
        ClientPrimaryManagerInfo firstPrimaryManagerInfo = steps.idmGroupSteps().createIdmPrimaryManager();
        firstPrimaryManager = new ClientPrimaryManager()
                .withSubjectClientId(firstPrimaryManagerInfo.getSubjectClientId())
                .withPrimaryManagerUid(firstPrimaryManagerInfo.getManagerUid())
                .withIsIdmPrimaryManager(true);
        ClientPrimaryManagerInfo secondPrimaryManagerInfo = steps.idmGroupSteps().createIdmPrimaryManager();
        secondPrimaryManager = new ClientPrimaryManager()
                .withSubjectClientId(secondPrimaryManagerInfo.getSubjectClientId())
                .withPrimaryManagerUid(secondPrimaryManagerInfo.getManagerUid())
                .withIsIdmPrimaryManager(true);
        ClientPrimaryManagerInfo thirdPrimaryManagerInfo = steps.idmGroupSteps().createIdmPrimaryManager();
        thirdPrimaryManager = new ClientPrimaryManager()
                .withSubjectClientId(thirdPrimaryManagerInfo.getSubjectClientId())
                .withPrimaryManagerUid(thirdPrimaryManagerInfo.getManagerUid())
                .withIsIdmPrimaryManager(true);
    }

    @Test
    public void testGetPrimaryManagers_getAll_success() {
        PrimaryManagersQueryFilter getAllFilter = PrimaryManagersQueryFilter.allIdmPrimaryManagers();
        List<ClientPrimaryManager> primaryManagers =
                clientRepository.getIdmPrimaryManagers(ClientSteps.DEFAULT_SHARD, getAllFilter);
        assertThat(primaryManagers).contains(firstPrimaryManager, secondPrimaryManager, thirdPrimaryManager);
    }

    @Test
    public void testGetPrimaryManagers_getSecondManager_success() {
        PrimaryManagersQueryFilter getNextFilter =
                PrimaryManagersQueryFilter.getNextIdmManagersPageFilter(firstPrimaryManager.getSubjectClientId(), 1);
        List<ClientPrimaryManager> primaryManagers =
                clientRepository.getIdmPrimaryManagers(ClientSteps.DEFAULT_SHARD, getNextFilter);
        assertThat(primaryManagers).containsOnly(secondPrimaryManager);
    }

}
