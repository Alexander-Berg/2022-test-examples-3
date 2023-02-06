package ru.yandex.direct.core.entity.client.service;

import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class GetClientsManagersFromCampaignsTest {

    private static final ClientId NON_EXISTENT_CLIENT_ID = ClientId.fromLong(Long.MAX_VALUE);

    @Autowired
    private ClientService clientService;

    @Autowired
    private Steps steps;

    private ClientInfo client1;
    private ClientInfo client2;
    private ClientInfo clientWithoutCampaigns;
    private Set<Long> managerUidsOfClient1;
    private Set<Long> managerUidsOfClient2;

    @Before
    public void setUp() {
        client1 = steps.clientSteps().createDefaultClient();

        Campaign campaign = TestCampaigns.newTextCampaign(client1.getClientId(), client1.getUid())
                .withManagerUid(RandomNumberUtils.nextPositiveLong());
        steps.campaignSteps().createCampaign(campaign, client1);
        Campaign emptyAndArchivedCampaign = TestCampaigns.newTextCampaign(client1.getClientId(), client1.getUid())
                .withManagerUid(campaign.getManagerUid() + 1)
                .withArchived(true)
                .withStatusEmpty(true);
        steps.campaignSteps().createCampaign(emptyAndArchivedCampaign, client1);
        steps.campaignSteps().createActiveTextCampaign(client1);
        managerUidsOfClient1 = Set.of(campaign.getManagerUid(), emptyAndArchivedCampaign.getManagerUid());

        client2 = steps.clientSteps().createDefaultClientAnotherShard();
        Campaign campaign2 = TestCampaigns.newTextCampaign(client2.getClientId(), client2.getUid())
                .withManagerUid(campaign.getManagerUid() - 1);
        steps.campaignSteps().createCampaign(campaign2, client2);
        managerUidsOfClient2 = Set.of(campaign2.getManagerUid());

        clientWithoutCampaigns =
                steps.clientSteps().createDefaultClientAnotherShard();
    }


    @Test
    public void massUpdate_empty() {
        Set<ClientId> clientIds = Set.of(client1.getClientId(), client2.getClientId(),
                clientWithoutCampaigns.getClientId(), NON_EXISTENT_CLIENT_ID);

        Map<Long, Set<Long>> clientsManagersFromCampaigns = clientService.getClientsManagersFromCampaigns(clientIds);
        Map<Long, Set<Long>> expectedClientsManagersFromCampaigns = Map.ofEntries(
                entry(client1.getClientId().asLong(), managerUidsOfClient1),
                entry(client2.getClientId().asLong(), managerUidsOfClient2)
        );

        assertThat(clientsManagersFromCampaigns)
                .is(matchedBy(beanDiffer(expectedClientsManagersFromCampaigns)));
    }

}
