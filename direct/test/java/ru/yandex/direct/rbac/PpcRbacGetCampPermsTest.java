package ru.yandex.direct.rbac;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.model.RbacCampPerms;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class PpcRbacGetCampPermsTest {
    @Autowired
    Steps steps;
    @Autowired
    PpcRbac ppcRbac;
    @Autowired
    RbacClientsRelations rbacClientsRelations;

    private ClientInfo managerInfo;
    private ClientInfo limitedSupportInfo;
    private ClientInfo clientInfo;
    private long idmGroup;

    @Before
    public void setUp() throws Exception {
        managerInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);
        limitedSupportInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.LIMITED_SUPPORT);
        clientInfo = steps.clientSteps().createDefaultClient();
        idmGroup = steps.idmGroupSteps().createManagerIdmGroup();
    }

    @Test
    public void getCampPerms_success_manager() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        steps.idmGroupSteps().addGroupAccess(idmGroup, clientInfo.getClientId());
        steps.idmGroupSteps().addGroupMembership(idmGroup, managerInfo.getClientId());

        Long campaignId = campaignInfo.getCampaignId();
        Map<Long, RbacCampPerms> campPerms = ppcRbac.getCampPerms(managerInfo.getUid(),
                Collections.singletonList(campaignId));
        assertThat(campPerms)
                .hasSize(1)
                .containsValue(RbacCampPerms.ALL);
    }

    @Test
    public void getCampPerms_none_otherManagerWithGroupRoles() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        steps.idmGroupSteps().addGroupMembership(idmGroup, managerInfo.getClientId());

        Long campaignId = campaignInfo.getCampaignId();
        Map<Long, RbacCampPerms> campPerms = ppcRbac.getCampPerms(managerInfo.getUid(),
                Collections.singletonList(campaignId));
        assertThat(campPerms)
                .hasSize(1)
                .containsValue(RbacCampPerms.EMPTY);
    }

    @Test
    public void getCampPerms_none_otherManagerWithoutGroupRoles() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        Long campaignId = campaignInfo.getCampaignId();
        Map<Long, RbacCampPerms> campPerms = ppcRbac.getCampPerms(managerInfo.getUid(),
                Collections.singletonList(campaignId));
        assertThat(campPerms)
                .hasSize(1)
                .containsValue(RbacCampPerms.EMPTY);
    }

    @Test
    public void getCampPerms_readonly_limitedSupportHasAccessToClient() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        rbacClientsRelations.addSupportRelation(clientInfo.getClientId(), limitedSupportInfo.getClientId());

        Long campaignId = campaignInfo.getCampaignId();
        Map<Long, RbacCampPerms> campPerms = ppcRbac.getCampPerms(limitedSupportInfo.getUid(),
                Collections.singletonList(campaignId));
        assertThat(campPerms)
                .hasSize(1)
                .containsValue(RbacCampPerms.READONLY);
    }

    @Test
    public void getCampPerms_readonly_limitedSupportHasAccessToAgency() {
        ClientInfo agencyInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        ClientInfo subclientInfo = steps.clientSteps().createDefaultClientUnderAgency(agencyInfo);
        CampaignInfo agencyCampaignInfo = steps.campaignSteps().createCampaign(
                activeTextCampaign(subclientInfo.getClientId(), subclientInfo.getUid())
                        .withAgencyUid(agencyInfo.getUid()).withAgencyId(agencyInfo.getClientId().asLong()),
                subclientInfo);

        rbacClientsRelations.addSupportRelation(agencyInfo.getClientId(), limitedSupportInfo.getClientId());

        Long campaignId = agencyCampaignInfo.getCampaignId();
        Map<Long, RbacCampPerms> campPerms = ppcRbac.getCampPerms(limitedSupportInfo.getUid(),
                Collections.singletonList(campaignId));
        assertThat(campPerms)
                .hasSize(1)
                .containsValue(RbacCampPerms.READONLY);
    }

    @Test
    public void getCampPerms_none_limitedSupportHasAccessToAgency_campaignWithoutAgency() {
        ClientInfo agencyInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        ClientInfo subclientInfo = steps.clientSteps().createDefaultClientUnderAgency(agencyInfo);
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(subclientInfo);

        rbacClientsRelations.addSupportRelation(agencyInfo.getClientId(), limitedSupportInfo.getClientId());

        Long campaignId = campaignInfo.getCampaignId();
        Map<Long, RbacCampPerms> campPerms = ppcRbac.getCampPerms(limitedSupportInfo.getUid(),
                Collections.singletonList(campaignId));
        assertThat(campPerms)
                .hasSize(1)
                .containsValue(RbacCampPerms.READONLY);
    }

    @Test
    public void getCampPerms_none_limitedSupportHasNoAccessToClient() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);

        Long campaignId = campaignInfo.getCampaignId();
        Map<Long, RbacCampPerms> campPerms = ppcRbac.getCampPerms(limitedSupportInfo.getUid(),
                Collections.singletonList(campaignId));
        assertThat(campPerms)
                .hasSize(1)
                .containsValue(RbacCampPerms.EMPTY);
    }
}
