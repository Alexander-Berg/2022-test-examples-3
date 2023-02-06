package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.balance.model.BalanceInfoQueueItem;
import ru.yandex.direct.core.entity.balance.model.BalanceInfoQueueObjType;
import ru.yandex.direct.core.entity.balance.model.BalanceInfoQueuePriority;
import ru.yandex.direct.core.entity.balance.model.BalanceInfoQueueSendStatus;
import ru.yandex.direct.core.entity.balance.model.BalanceNotificationInfo;
import ru.yandex.direct.core.entity.balance.repository.BalanceInfoQueueRepository;
import ru.yandex.direct.core.entity.balance.service.BalanceInfoQueueService;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithSendingToBalance;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignBalanceService;
import ru.yandex.direct.core.entity.campaign.service.CommonCampaignService;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.idm.model.IdmGroup;
import ru.yandex.direct.core.entity.idm.model.IdmRequiredRole;
import ru.yandex.direct.core.entity.notification.NotificationService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.IdmGroupRoleInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestClientRepository;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.IdmGroupSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCampaignByCampaignType;
import static ru.yandex.direct.core.testing.data.TestIdmGroups.DEFAULT_IDM_GROUP_ID;
import static ru.yandex.direct.dbschema.ppc.Tables.CLIENT_MANAGERS;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithSendingToBalanceAddOperationSupportAfterExecutionTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Mock
    private NotificationService notificationService;

    @Mock
    private CampaignBalanceService campaignBalanceService;

    @Autowired
    private TestClientRepository testClientRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private BalanceInfoQueueRepository balanceInfoQueueRepository;

    @Autowired
    private AddServicedCampaignService addServicedCampaignService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserService userService;

    @Autowired
    private BalanceInfoQueueService balanceInfoQueueService;

    @Autowired
    private DslContextProvider ppcDslContextProvider;

    private CampaignWithSendingToBalanceAddOperationSupport support;

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private IdmGroupSteps idmGroupSteps;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CommonCampaignService commonCampaignService;

    private RestrictedCampaignsAddOperationContainer addCampaignParametersContainer;

    private long campaignId;

    private DSLContext dslContext;

    private ClientInfo clientInfo;

    private IdmGroup idmGroup;

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
        MockitoAnnotations.initMocks(this);

        support = new CampaignWithSendingToBalanceAddOperationSupport(campaignRepository,
                campaignBalanceService,
                addServicedCampaignService,
                clientService,
                walletService,
                commonCampaignService,
                userService,
                balanceInfoQueueService,
                ppcDslContextProvider);

        clientInfo = clientSteps.createDefaultClient();
        campaignId = RandomNumberUtils.nextPositiveLong();
        dslContext = dslContextProvider.ppc(clientInfo.getShard());

        idmGroup = idmGroupSteps.addIfNotExistIdmGroup(DEFAULT_IDM_GROUP_ID, IdmRequiredRole.MANAGER);
    }

    @Test
    public void afterExecution_CommonClient() {
        when(campaignBalanceService.createOrUpdateOrdersOnFirstCampaignCreation(any(), any(), any(), any()))
                .thenReturn(true);

        addCampaignParametersContainer = createAddCampaignParametersContainer(clientInfo.getUid());

        var campaignWithSendingToBalance = (CampaignWithSendingToBalance) defaultCampaignByCampaignType(campaignType)
                .withId(campaignId);

        support.afterExecution(addCampaignParametersContainer, List.of(campaignWithSendingToBalance));
        List<BalanceInfoQueueItem> existingRecordsInWaitStatus = getBalanceInfoQueueItems();

        assertThat(existingRecordsInWaitStatus).hasSize(1);

        BalanceInfoQueueItem expectedBalanceInfoQueueItem = new BalanceInfoQueueItem()
                .withOperatorUid(clientInfo.getUid())
                .withCidOrUid(clientInfo.getUid())
                .withObjType(BalanceInfoQueueObjType.UID)
                .withSendStatus(BalanceInfoQueueSendStatus.WAIT)
                .withPriority(Long.valueOf(BalanceInfoQueuePriority.USER_ON_SAVING_NEW_CAMPAIGN.getTypedValue()));

        assertThat(existingRecordsInWaitStatus.get(0)).isEqualToIgnoringGivenFields(expectedBalanceInfoQueueItem,
                "addTime");

        verifyZeroInteractions(notificationService);
    }

    @Test
    public void afterExecution_CommonClient_BalanceReturnsError() {
        when(campaignBalanceService.createOrUpdateOrdersOnFirstCampaignCreation(any(), any(), any(), any()))
                .thenReturn(false);

        addCampaignParametersContainer = createAddCampaignParametersContainer(clientInfo.getUid());

        var campaignWithSendingToBalance = (CampaignWithSendingToBalance) defaultCampaignByCampaignType(campaignType)
                .withId(campaignId);

        support.afterExecution(addCampaignParametersContainer, List.of(campaignWithSendingToBalance));
        List<BalanceInfoQueueItem> existingRecordsInWaitStatus = getBalanceInfoQueueItems();

        assertThat(existingRecordsInWaitStatus).hasSize(2);

        BalanceInfoQueueItem expectedBalanceInfoQueueItem = new BalanceInfoQueueItem()
                .withOperatorUid(clientInfo.getUid())
                .withCidOrUid(clientInfo.getUid())
                .withObjType(BalanceInfoQueueObjType.UID)
                .withSendStatus(BalanceInfoQueueSendStatus.WAIT)
                .withPriority(Long.valueOf(BalanceInfoQueuePriority.USER_ON_SAVING_NEW_CAMPAIGN.getTypedValue()));

        BalanceInfoQueueItem expectedBalanceInfoQueueItemForBrokenBalance = new BalanceInfoQueueItem()
                .withOperatorUid(clientInfo.getUid())
                .withCidOrUid(campaignWithSendingToBalance.getId())
                .withObjType(BalanceInfoQueueObjType.CID)
                .withSendStatus(BalanceInfoQueueSendStatus.WAIT)
                .withPriority(Long.valueOf(BalanceInfoQueuePriority.PRIORITY_CAMPS_ON_ENABLE_WALLET.getTypedValue()));

        assertThat(existingRecordsInWaitStatus.get(0)).isEqualToIgnoringGivenFields(expectedBalanceInfoQueueItem,
                "addTime");

        assertThat(existingRecordsInWaitStatus.get(1)).isEqualToIgnoringGivenFields(
                expectedBalanceInfoQueueItemForBrokenBalance, "addTime");

        verifyZeroInteractions(notificationService);
    }


    @Test
    public void afterExecution_ServicedCampaign() {
        when(campaignBalanceService.createOrUpdateOrdersOnFirstCampaignCreation(any(), any(), any(), any()))
                .thenReturn(true);

        UserInfo managerInfo =
                clientSteps.createDefaultClientWithRoleInAnotherShard(RbacRole.MANAGER).getChiefUserInfo();
        idmGroupSteps.addIdmGroupRole(
                new IdmGroupRoleInfo()
                        .withClientInfo(clientInfo)
                        .withIdmGroup(idmGroup));
        idmGroupSteps.addGroupMembership(DEFAULT_IDM_GROUP_ID, managerInfo.getClientId());
        idmGroupSteps.addIdmPrimaryManager(managerInfo, clientInfo);

        addCampaignParametersContainer = createAddCampaignParametersContainer(managerInfo.getUid());
        addToClientManagers(clientInfo.getShard(), clientInfo.getClientId().asLong(), managerInfo.getUid());

        var campaignWithSendingToBalance = (CampaignWithSendingToBalance) defaultCampaignByCampaignType(campaignType)
                .withId(campaignId);

        support.afterExecution(addCampaignParametersContainer, List.of(campaignWithSendingToBalance));
        List<BalanceInfoQueueItem> existingRecordsInWaitStatus = getBalanceInfoQueueItems();

        assertThat(existingRecordsInWaitStatus).hasSize(2);

        BalanceInfoQueueItem expectedBalanceInfoQueueUidItem = new BalanceInfoQueueItem()
                .withOperatorUid(managerInfo.getUid())
                .withCidOrUid(clientInfo.getUid())
                .withObjType(BalanceInfoQueueObjType.UID)
                .withSendStatus(BalanceInfoQueueSendStatus.WAIT)
                .withPriority(Long.valueOf(BalanceInfoQueuePriority.USER_ON_SAVING_NEW_CAMPAIGN.getTypedValue()));

        assertThat(existingRecordsInWaitStatus.get(0)).isEqualToIgnoringGivenFields(expectedBalanceInfoQueueUidItem,
                "addTime");

        BalanceInfoQueueItem expectedBalanceInfoQueueCidItem = new BalanceInfoQueueItem()
                .withOperatorUid(managerInfo.getUid())
                .withCidOrUid(campaignId)
                .withObjType(BalanceInfoQueueObjType.CID)
                .withSendStatus(BalanceInfoQueueSendStatus.WAIT)
                .withPriority(Long.valueOf(BalanceInfoQueuePriority.PRIORITY_CAMP_ON_MANAGER_CHANGED.getTypedValue()));

        assertThat(existingRecordsInWaitStatus.get(1)).isEqualToIgnoringGivenFields(expectedBalanceInfoQueueCidItem,
                "addTime");

        checkClientOfManagerIsBound(managerInfo.getUid());
    }

    private void addToClientManagers(int shard, Long clientId, Long managerUid) {
        dslContextProvider.ppc(shard)
                .insertInto(CLIENT_MANAGERS)
                .values(clientId, managerUid)
                .execute();
    }

    private List<BalanceInfoQueueItem> getBalanceInfoQueueItems() {
        BalanceNotificationInfo balanceNotificationInfoCid = new BalanceNotificationInfo()
                .withCidOrUid(campaignId)
                .withObjType(BalanceInfoQueueObjType.CID);
        BalanceNotificationInfo balanceNotificationInfoUid = new BalanceNotificationInfo()
                .withCidOrUid(clientInfo.getUid())
                .withObjType(BalanceInfoQueueObjType.UID);
        return balanceInfoQueueRepository.getExistingRecordsInWaitStatus(dslContext,
                List.of(balanceNotificationInfoCid, balanceNotificationInfoUid));
    }

    private RestrictedCampaignsAddOperationContainer createAddCampaignParametersContainer(Long operatorUid) {
        return RestrictedCampaignsAddOperationContainer.create(
                clientInfo.getShard(),
                operatorUid,
                clientInfo.getClientId(),
                clientInfo.getUid(),
                clientInfo.getUid());
    }

    private void checkClientOfManagerIsBound(Long managerUid) {
        List<Long> clientsOfManagerWithoutCampaigns =
                testClientRepository.getBindedClientsToManager(clientInfo.getShard(), managerUid);
        assertThat(clientsOfManagerWithoutCampaigns).hasSize(1);
        assertThat(clientsOfManagerWithoutCampaigns.get(0)).isEqualTo(clientInfo.getClientId().asLong());
    }

}
