package ru.yandex.direct.oneshot.oneshots.turnproviewforagencyclients;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.oneshot.configuration.OneshotTest;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;

@OneshotTest
@RunWith(SpringRunner.class)
public class TurnStrategyProViewForAgencyClientsOneshotTest {
    @Autowired
    Steps steps;

    @Autowired
    TurnStrategyProViewForAgencyClientsOneshot oneshot;

    @Autowired
    ClientRepository clientRepository;

    private ClientInfo agency;

    @Before
    public void before() {
        agency = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
    }

    @Test
    public void agencyClientWithoutProView_TurnProViewOn() {
        var client = steps.clientSteps().createDefaultClientUnderAgency(agency);
        oneshot.execute(null,
                new TurnStrategyProViewForAgencyClientsState(ClientId.fromLong(client.getClientId().asLong() - 1)),
                client.getShard());

        var actualClient = clientRepository.get(client.getShard(), Set.of(client.getClientId())).get(0);

        assertThat(actualClient.getIsProStrategyViewEnabled()).isTrue();
    }

    @Test
    public void simpleClientWithoutProView_DoNotChange() {
        var client = steps.clientSteps().createDefaultClient();
        oneshot.execute(null,
                new TurnStrategyProViewForAgencyClientsState(ClientId.fromLong(client.getClientId().asLong() - 1)),
                client.getShard());

        var actualClient = clientRepository.get(client.getShard(), Set.of(client.getClientId())).get(0);

        assertThat(actualClient.getIsProStrategyViewEnabled()).isFalse();
    }
}
