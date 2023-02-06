package ru.yandex.direct.intapi.entity.idm.service;

import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.idm.model.AddRoleRequest;
import ru.yandex.direct.intapi.entity.idm.model.GetAllRolesResponseItem;
import ru.yandex.direct.intapi.entity.idm.model.IdmResponse;
import ru.yandex.direct.intapi.entity.idm.model.IdmSuccessResponse;
import ru.yandex.direct.intapi.entity.idm.model.RemoveRoleRequest;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.intapi.entity.idm.converter.GetAllRolesResponseConverter.convertMainManagerRoles;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IdmMainManagerServiceTest {

    @Autowired
    private IdmMainManagerService idmMainManagerService;
    @Autowired
    private Steps steps;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private CampaignRepository campaignRepository;

    private ClientId clientId;
    private Integer shard;
    private Long campaignId;
    private UserInfo startManagerInfo;

    @Before
    public void setUp() {
        startManagerInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER).getChiefUserInfo();
        Long managerUid = startManagerInfo.getUid();

        ClientInfo clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT);
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();

        campaignRepository.setManagerForAllClientCampaigns(shard, clientId, managerUid);
        updatePrimaryManagerUid(shard, clientId, managerUid);
    }

    @Test
    public void addRole_success() {
        UserInfo newManagerInfo =
                steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER).getChiefUserInfo();
        steps.userSteps().mockUserInExternalClients(newManagerInfo.getUser());
        AddRoleRequest request = new AddRoleRequest()
                .withRole("main_manager")
                .withClientId(clientId.asLong())
                .withDomainLogin(newManagerInfo.getUser().getDomainLogin())
                .withPassportLogin(newManagerInfo.getUser().getLogin());

        IdmResponse idmResponse = idmMainManagerService.addRole(request);

        Client actualClient = clientRepository.get(shard, singleton(clientId)).get(0);
        Campaign actualCampaign = campaignRepository.getCampaigns(shard, singleton(campaignId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(idmResponse)
                    .as("Response type")
                    .isInstanceOf(IdmSuccessResponse.class);
            soft.assertThat(actualClient.getPrimaryManagerUid())
                    .as("Client primaryManagerUid")
                    .isEqualTo(newManagerInfo.getUid());
            soft.assertThat(actualClient.getIsIdmPrimaryManager())
                    .as("Client primaryManagerSetByIdm flag")
                    .isTrue();
            soft.assertThat(actualCampaign.getManagerUserId())
                    .as("Campaign service manager")
                    .isEqualTo(newManagerInfo.getUid());
        });
    }

    @Test
    public void removeRole_success() {
        RemoveRoleRequest request = new RemoveRoleRequest()
                .withRole("main_manager_for_client")
                .withClientId(clientId.asLong())
                .withDomainLogin(startManagerInfo.getUser().getDomainLogin())
                .withPassportLogin(startManagerInfo.getUser().getLogin());

        IdmResponse idmResponse = idmMainManagerService.removeRole(request);

        Client actualClient = clientRepository.get(shard, singleton(clientId)).get(0);
        Campaign actualCampaign = campaignRepository.getCampaigns(shard, singleton(campaignId)).get(0);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(idmResponse)
                    .as("Response type")
                    .isInstanceOf(IdmSuccessResponse.class);
            soft.assertThat(actualClient.getPrimaryManagerUid())
                    .as("Client primaryManagerUid")
                    .isNull();
            soft.assertThat(actualClient.getIsIdmPrimaryManager())
                    .as("Client primaryManagerSetByIdm flag")
                    .isFalse();
            soft.assertThat(actualCampaign.getManagerUserId())
                    .as("Campaign service manager")
                    .isNull();
        });
    }

    @Test
    public void getAllRoles() {
        UserInfo newManagerInfo = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER)
                .getChiefUserInfo();
        updatePrimaryManagerUid(shard, clientId, newManagerInfo.getUid());
        User idmManager = newManagerInfo.getUser();

        GetAllRolesResponseItem expectedItem = new GetAllRolesResponseItem()
                .withLogin(idmManager.getDomainLogin())
                .withRoles(singletonList(List.of(
                        singletonMap("direct", "main_manager_for_client"),
                        Map.of("passport-login", idmManager.getLogin(),
                                "client_id", clientId.toString()))));

        List<GetAllRolesResponseItem> allRoles = convertMainManagerRoles(
                idmMainManagerService.getAllRoles());
        GetAllRolesResponseItem actualItem = StreamEx.of(allRoles)
                .findAny(r -> r.getLogin().equals(idmManager.getDomainLogin()))
                .orElse(null);

        assertThat(actualItem)
                .is(matchedBy(beanDiffer(expectedItem).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void getAllRoles_whenManagerDoesNotExistOrHasNoDomainLogin() {
        // primaryManagerUid не существует
        updatePrimaryManagerUid(shard, clientId, -1L);

        // y primaryManagerUid нет доменного логина
        ClientInfo newManagerInfo = steps.clientSteps().createDefaultClient();
        ClientInfo clientInfo2 = steps.clientSteps().createDefaultClient();
        ClientId clientId2 = clientInfo2.getClientId();
        updatePrimaryManagerUid(clientInfo2.getShard(), clientId2, newManagerInfo.getUid());

        String expectedDomainLogin = "-";
        List<Map<String, String>> expectedRole = List.of(
                singletonMap("direct", "main_manager_for_client"),
                Map.of("passport-login", "-", "client_id", clientId.toString()));
        List<Map<String, String>> expectedRole2 = List.of(
                singletonMap("direct", "main_manager_for_client"),
                Map.of("passport-login", newManagerInfo.getLogin(), "client_id", clientId2.toString()));

        List<GetAllRolesResponseItem> allRoles = convertMainManagerRoles(
                idmMainManagerService.getAllRoles());
        List<List<Map<String, String>>> actualRoles = StreamEx.of(allRoles)
                .findAny(r -> r.getLogin().equals(expectedDomainLogin))
                .map(GetAllRolesResponseItem::getRoles)
                .orElse(emptyList());

        MatcherAssert.assertThat(actualRoles, hasItem(beanDiffer(expectedRole)));
        MatcherAssert.assertThat(actualRoles, hasItem(beanDiffer(expectedRole2)));
    }

    private void updatePrimaryManagerUid(int shard, ClientId clientId, Long managerUid) {
        ModelChanges<Client> modelChanges = new ModelChanges<>(clientId.asLong(), Client.class);
        modelChanges.process(managerUid, Client.PRIMARY_MANAGER_UID);
        modelChanges.process(true, Client.IS_IDM_PRIMARY_MANAGER);
        Client client = clientRepository.get(shard, singleton(clientId)).get(0);
        AppliedChanges<Client> appliedChanges = modelChanges.applyTo(client);
        clientRepository.update(shard, singleton(appliedChanges));
    }
}
