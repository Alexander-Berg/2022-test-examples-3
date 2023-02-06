package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCampaignWithSystemFieldsByCampaignType;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;

@CoreTest
@RunWith(Parameterized.class)
public class CommonCampaignAddOperationSupportAfterExecutionClientModificationTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private ClientService clientService;

    @Autowired
    private CommonCampaignAddOperationSupport support;

    private UserInfo clientUser;
    private UserInfo operatorUser;

    private long campaignId;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        var clientInfo = steps.clientSteps()
                .createDefaultClientWithRole(RbacRole.CLIENT);
        clientUser = clientInfo.getChiefUserInfo();

        operatorUser = steps.userSteps().createUser(new UserInfo()
                .withClientInfo(new ClientInfo().withClient(defaultClient().withRole(RbacRole.AGENCY))));

        campaignId = RandomNumberUtils.nextPositiveInteger();
    }

    @Test
    public void afterExecution_AgenciesCampaign() {
        RestrictedCampaignsAddOperationContainer addCampaignParametersContainer =
                createAddCampaignParametersContainer(operatorUser);

        CommonCampaign commonCampaign = defaultCampaignWithSystemFieldsByCampaignType(campaignType);
        commonCampaign.withId(campaignId);

        support.afterExecution(addCampaignParametersContainer, List.of(commonCampaign));

        Client client = clientService.getClient(clientUser.getClientId());

        assertThat(client.getRole()).isEqualTo(RbacRole.CLIENT);
        assertThat(client.getAgencyUserId()).isEqualTo(operatorUser.getUid());
        assertThat(client.getAgencyClientId()).isEqualTo(operatorUser.getClientId().asLong());
    }

    private RestrictedCampaignsAddOperationContainer createAddCampaignParametersContainer(UserInfo operatorUser) {
        return RestrictedCampaignsAddOperationContainer.create(
                clientUser.getShard(),
                operatorUser.getUid(),
                clientUser.getClientId(),
                clientUser.getUid(),
                clientUser.getUid());
    }

}
