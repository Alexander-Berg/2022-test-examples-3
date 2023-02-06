package ru.yandex.direct.core.entity.campaign.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jdk.jfr.Description;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CpmPriceCampaign;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.DeleteCampaignValidationService;
import ru.yandex.direct.core.entity.campoperationqueue.CampOperationQueueRepository;
import ru.yandex.direct.core.entity.campoperationqueue.model.CampQueueOperation;
import ru.yandex.direct.core.entity.campoperationqueue.model.CampQueueOperationName;
import ru.yandex.direct.core.entity.client.repository.ClientManagersRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.rbac.RbacSubrole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.testing.data.TestCampaigns.emptyBalanceInfo;
import static ru.yandex.direct.core.testing.data.TestPricePackages.approvedPricePackage;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENT_MANAGERS;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.unableToDelete;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignDeleteOperationTest {

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private DeleteCampaignValidationService deleteCampaignValidationService;

    @Autowired
    private ClientService clientService;

    @Autowired
    ClientManagersRepository clientManagersRepository;

    @Autowired
    private TestClientRepository testClientRepository;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Mock
    private CampOperationQueueRepository campOperationQueueRepository;

    private Campaign newTextCampaign;

    private long operatorUid;
    private ClientId clientId;
    private int shard;
    private ClientInfo clientInfo;

    private UserInfo manager;

    @Before
    public void before() {
        initMocks(this);

        clientInfo = steps.clientSteps().createDefaultClient();

        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest1 =
                TestCampaigns.newTextCampaign(null, null);

        CampaignInfo campaignInfo1 = steps.campaignSteps().createCampaign(campaignTest1, clientInfo);

        operatorUid = campaignInfo1.getUid();
        clientId = campaignInfo1.getClientId();
        shard = campaignInfo1.getShard();

        newTextCampaign = getCampaign(shard, campaignInfo1.getCampaignId());

        manager = createManagerWithSubRole(null);
    }

    @Test
    public void prepareAndApply_OneValidItem_ResultIsExpected() {
        List<Long> campaignIds = List.of(newTextCampaign.getId());

        deleteAndAssertResult(Applicability.PARTIAL, campaignIds, true);

        List<Campaign> actualCampaigns = campaignRepository.getCampaigns(shard, campaignIds);

        assertThat(actualCampaigns, hasSize(1));
        assertThat(actualCampaigns.get(0).getStatusEmpty(), is(true));

        verify(campOperationQueueRepository, times(1)).addCampaignQueueOperations(shard,
                List.of(new CampQueueOperation()
                        .withCid(campaignIds.get(0))
                        .withCampQueueOperationName(CampQueueOperationName.DEL)
                        .withParams("{\"UID\":\"" + operatorUid + "\"}")));
        verifyNoMoreInteractions(campOperationQueueRepository);
    }

    @Test
    public void checkSubCampaignsDeletedWithMaster() {
        CampaignInfo subCampaignInfo = steps.campaignSteps().createSubCampaign(clientInfo, newTextCampaign.getId());
        List<Long> campaignIds = List.of(newTextCampaign.getId(), subCampaignInfo.getCampaignId());

        deleteAndAssertResult(Applicability.PARTIAL, List.of(newTextCampaign.getId()), true);

        List<Campaign> actualCampaigns = campaignRepository.getCampaigns(shard, campaignIds);

        assertThat(actualCampaigns, hasSize(2));
        assertThat(actualCampaigns.get(0).getStatusEmpty(), is(true));
        assertThat(actualCampaigns.get(1).getStatusEmpty(), is(true));
    }

    @Test
    public void prepareAndApply_OneValidOneInvalidItems_ResultIsExpected() {
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest =
                TestCampaigns.newTextCampaign(null, null)
                        .withBalanceInfo(emptyBalanceInfo(CurrencyCode.RUB)
                                .withSumToPay(BigDecimal.valueOf(50_000L).setScale(6, RoundingMode.DOWN)));

        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaignTest, clientInfo);
        List<Long> campaignIds = List.of(newTextCampaign.getId(), campaignInfo.getCampaignId());

        MassResult<Long> result = new CampaignsDeleteOperation(Applicability.PARTIAL,
                campaignIds,
                campaignRepository, campOperationQueueRepository, deleteCampaignValidationService,
                clientManagersRepository, rbacService, dslContextProvider, operatorUid,
                clientId,
                shard
        ).prepareAndApply();

        ValidationResult<List<Long>, Defect> vr = (ValidationResult<List<Long>, Defect>) result.getValidationResult();
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(1)), unableToDelete())));

        List<Campaign> actualCampaigns = campaignRepository.getCampaigns(shard, campaignIds);

        assertThat(actualCampaigns, hasSize(2));
        assertThat(actualCampaigns.get(0).getStatusEmpty(), is(true));
        assertThat(actualCampaigns.get(1).getStatusEmpty(), is(false));

        verify(campOperationQueueRepository, times(1)).addCampaignQueueOperations(shard,
                List.of(new CampQueueOperation()
                        .withCid(campaignIds.get(0))
                        .withCampQueueOperationName(CampQueueOperationName.DEL)
                        .withParams("{\"UID\":\"" + operatorUid + "\"}")));
        verifyNoMoreInteractions(campOperationQueueRepository);
    }

    @Test
    public void removeManager_whenLastServicedCampaignDeleted() {
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest =
                TestCampaigns.newTextCampaign(null, null)
                        .withManagerUid(manager.getUid());
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaignTest, clientInfo);

        List<Long> campaignIds = List.of(campaignInfo.getCampaignId());

        testClientRepository.bindManagerToClient(clientInfo.getShard(), clientInfo.getClientId(), manager.getUid());

        deleteAndAssertResult(Applicability.PARTIAL, campaignIds, true);

        Map<Long, List<Long>> clientsManagers =
                getClientsManagers(clientInfo.getShard(), singletonList(clientInfo.getClientId().asLong()));
        assertTrue(clientsManagers.isEmpty());
    }

    @Test
    public void dontRemoveManager_whenLastServicedCampaignNotDeleted() {
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest =
                TestCampaigns.newTextCampaign(null, null)
                        .withManagerUid(manager.getUid());
        steps.campaignSteps().createCampaign(campaignTest, clientInfo);

        List<Long> campaignIds = List.of(newTextCampaign.getId());

        testClientRepository.bindManagerToClient(clientInfo.getShard(), clientInfo.getClientId(), manager.getUid());

        deleteAndAssertResult(Applicability.PARTIAL, campaignIds, true);

        long clientUid = clientInfo.getClientId().asLong();
        Map<Long, List<Long>> clientsManagers =
                getClientsManagers(clientInfo.getShard(), singletonList(clientUid));
        assertTrue(clientsManagers.containsKey(clientUid));
        List<Long> actualClientManagers = clientsManagers.get(clientUid);

        assertTrue(actualClientManagers.equals(List.of(manager.getUid())));
    }

    @Test
    public void dontRemoveManager_whenManagerAlreadyHasNotCampaignsForClient() {
        List<Long> campaignIds = List.of(newTextCampaign.getId());

        testClientRepository.bindManagerToClient(clientInfo.getShard(), clientInfo.getClientId(), manager.getUid());

        deleteAndAssertResult(Applicability.PARTIAL, campaignIds, true);

        long clientUid = clientInfo.getClientId().asLong();
        Map<Long, List<Long>> clientsManagers =
                getClientsManagers(clientInfo.getShard(), singletonList(clientUid));
        assertTrue(clientsManagers.containsKey(clientUid));
        List<Long> actualClientManagers = clientsManagers.get(clientUid);

        assertTrue(actualClientManagers.equals(List.of(manager.getUid())));
    }

    @Test
    public void dontRemoveAnotherManager_whenLastServicedCampaignDeleted() {
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTest =
                TestCampaigns.newTextCampaign(null, null)
                        .withManagerUid(manager.getUid());
        steps.campaignSteps().createCampaign(campaignTest, clientInfo);

        UserInfo anotherManager = createManagerWithSubRole(null);
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign campaignTestAnotherManager =
                TestCampaigns.newTextCampaign(null, null)
                        .withManagerUid(anotherManager.getUid());
        steps.campaignSteps().createCampaign(campaignTestAnotherManager, clientInfo);

        List<Long> campaignIds = List.of(newTextCampaign.getId(), campaignTest.getId());

        testClientRepository.bindManagerToClient(clientInfo.getShard(), clientInfo.getClientId(), manager.getUid());
        testClientRepository.bindManagerToClient(clientInfo.getShard(), clientInfo.getClientId(),
                anotherManager.getUid());

        deleteAndAssertResult(Applicability.PARTIAL, campaignIds, true);

        long clientUid = clientInfo.getClientId().asLong();
        Map<Long, List<Long>> clientsManagers =
                getClientsManagers(clientInfo.getShard(), singletonList(clientUid));
        assertTrue(clientsManagers.containsKey(clientUid));
        List<Long> actualClientManagers = clientsManagers.get(clientUid);

        assertTrue(actualClientManagers.equals(List.of(anotherManager.getUid())));
    }

    @Test
    @Description("Можно удалить прайсовую кампанию с statusApprove = NEW")
    public void deletePriceCampaign_StatusApprove_New_Ok() {
        CpmPriceCampaign cpmPriceCampaign = createCpmPriceCampaign(PriceFlightStatusApprove.NEW);
        List<Long> campaignIds = List.of(cpmPriceCampaign.getId());

        deleteAndAssertResult(Applicability.PARTIAL, campaignIds, true);

        List<Campaign> actualCampaigns = campaignRepository.getCampaigns(shard, campaignIds);
        assertThat(actualCampaigns, hasSize(1));
        assertThat(actualCampaigns.get(0).getStatusEmpty(), is(true));
    }

    @Test
    @Description("Можно удалить прайсовую кампанию с statusApprove = NO")
    public void deletePriceCampaign_StatusApprove_No_Ok() {
        CpmPriceCampaign cpmPriceCampaign = createCpmPriceCampaign(PriceFlightStatusApprove.NO);
        List<Long> campaignIds = List.of(cpmPriceCampaign.getId());

        deleteAndAssertResult(Applicability.PARTIAL, campaignIds, true);

        List<Campaign> actualCampaigns = campaignRepository.getCampaigns(shard, campaignIds);
        assertThat(actualCampaigns, hasSize(1));
        assertThat(actualCampaigns.get(0).getStatusEmpty(), is(true));
    }

    @Test
    @Description("Нельзя удалить прайсовую кампанию с statusApprove = YES и без автоаппрува в пакете")
    public void deletePriceCampaign_StatusApprove_Yes_AutoApprove_Not_Error() {
        PricePackage pricePackageWithoutAutoApprove = approvedPricePackage().withCampaignAutoApprove(false);
        CpmPriceCampaign cpmPriceCampaign =
                createCpmPriceCampaign(pricePackageWithoutAutoApprove, PriceFlightStatusApprove.YES);
        List<Long> campaignIds = List.of(cpmPriceCampaign.getId());

        deleteAndAssertResult(Applicability.PARTIAL, campaignIds, false);

        List<Campaign> actualCampaigns = campaignRepository.getCampaigns(shard, campaignIds);
        assertThat(actualCampaigns, hasSize(1));
        assertThat(actualCampaigns.get(0).getStatusEmpty(), is(false));

    }

    @Test
    @Description("Можно удалить прайсовую кампанию с statusApprove = YES с автоаппрувом в пакете")
    public void deletePriceCampaign_StatusApprove_Yes_AutoApprove_Yes_Ok() {
        PricePackage autoApprovedPricePackage = approvedPricePackage().withCampaignAutoApprove(true);
        CpmPriceCampaign cpmPriceCampaign =
                createCpmPriceCampaign(autoApprovedPricePackage, PriceFlightStatusApprove.YES);
        List<Long> campaignIds = List.of(cpmPriceCampaign.getId());

        deleteAndAssertResult(Applicability.PARTIAL, campaignIds, true);

        List<Campaign> actualCampaigns = campaignRepository.getCampaigns(shard, campaignIds);
        assertThat(actualCampaigns, hasSize(1));
        assertThat(actualCampaigns.get(0).getStatusEmpty(), is(true));
    }

    private CpmPriceCampaign createCpmPriceCampaign(PriceFlightStatusApprove statusApprove) {
        return createCpmPriceCampaign(approvedPricePackage(), statusApprove);
    }

    private CpmPriceCampaign createCpmPriceCampaign(PricePackage pricePackageForCreate, PriceFlightStatusApprove statusApprove) {
        var pricePackage = steps.pricePackageSteps().createPricePackage(pricePackageForCreate).getPricePackage();
        var cpmPriceCampaign = TestCampaigns.defaultCpmPriceCampaignWithSystemFields(clientInfo, pricePackage)
                .withSum(BigDecimal.ZERO)
                .withSumToPay(BigDecimal.ZERO)
                .withSumLast(BigDecimal.ZERO)
                .withOrderId(0L)
                .withFlightStatusApprove(statusApprove);
        steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, cpmPriceCampaign);
        return cpmPriceCampaign;
    }


    private UserInfo createManagerWithSubRole(RbacSubrole subRole) {
        RbacRole role = RbacRole.MANAGER;
        UserInfo userInfo = steps.clientSteps().createDefaultClientWithRoleInAnotherShard(role).getChiefUserInfo();
        clientService.updateClientRole(userInfo.getClientInfo().getClientId(), role, subRole);
        userInfo.getUser().withSubRole(subRole);
        return userInfo;
    }

    private Map<Long, List<Long>> getClientsManagers(int shard, Collection<Long> clientIds) {
        return StreamEx.of(dslContextProvider.ppc(shard)
                .select(CLIENT_MANAGERS.CLIENT_ID, CLIENT_MANAGERS.MANAGER_UID)
                .from(CLIENT_MANAGERS)
                .where(CLIENT_MANAGERS.CLIENT_ID.in(clientIds))
                .stream())
                .mapToEntry(CLIENT_MANAGERS.CLIENT_ID::getValue, CLIENT_MANAGERS.MANAGER_UID::getValue)
                .collapseKeys()
                .toMap();
    }

    private void deleteAndAssertResult(Applicability applicability, List<Long> campaignIds,
                                       boolean itemResult) {
        CampaignsDeleteOperation deleteOperation = createDeleteOperation(applicability, campaignIds);
        MassResult<Long> result = deleteOperation.prepareAndApply();

        assertThat("результат операции должен быть положительный", result.isSuccessful(), is(true));
        assertThat("результат обновления элемента не соответствует ожидаемому",
                result.getResult().get(0).isSuccessful(), is(itemResult));
    }

    private CampaignsDeleteOperation createDeleteOperation(Applicability applicability,
                                                           List<Long> campaignIds) {
        return createDeleteOperation(applicability, campaignIds, operatorUid, clientId, shard);
    }

    private CampaignsDeleteOperation createDeleteOperation(Applicability applicability,
                                                           List<Long> campaignIds,
                                                           long operatorUid,
                                                           ClientId clientId, int shard) {
        return new CampaignsDeleteOperation(applicability, campaignIds,
                campaignRepository, campOperationQueueRepository, deleteCampaignValidationService,
                clientManagersRepository, rbacService, dslContextProvider, operatorUid,
                clientId,
                shard
        );
    }

    public Campaign getCampaign(int shard, long id) {
        return campaignRepository.getCampaigns(shard, singletonList(id)).iterator().next();
    }
}
