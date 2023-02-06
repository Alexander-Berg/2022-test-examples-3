package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.balance.service.BalanceInfoQueueService;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithSendingToBalance;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignBalanceService;
import ru.yandex.direct.core.entity.campaign.service.CommonCampaignService;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCampaignByCampaignType;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithSendingToBalanceAddOperationSupportAfterExecutionSendToBalanceIfNonWalletTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignBalanceService campaignBalanceService;

    @Autowired
    private AddServicedCampaignService addServicedCampaignService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private CommonCampaignService commonCampaignService;

    @Autowired
    private UserService userService;

    @Autowired
    private BalanceInfoQueueService balanceInfoQueueService;

    @Autowired
    private DslContextProvider ppcDslContextProvider;

    @Autowired
    private ClientSteps clientSteps;

    private CampaignWithSendingToBalanceAddOperationSupport support;

    private final CampaignType campaignType;
    private final boolean expectOrderSentToBalance;

    private long campaignId;
    private ClientInfo clientInfo;

    public CampaignWithSendingToBalanceAddOperationSupportAfterExecutionSendToBalanceIfNonWalletTest(
            CampaignType campaignType,
            boolean expectOrderSentToBalance) {
        this.campaignType = campaignType;
        this.expectOrderSentToBalance = expectOrderSentToBalance;
    }

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Collection<Object[]> parameters() {
        return List.of(
                new Object[]{CampaignType.TEXT, false},
                new Object[]{CampaignType.MCBANNER, false},
                new Object[]{CampaignType.INTERNAL_AUTOBUDGET, true}
        );
    }

    @Before
    public void setUp() {
        when(campaignBalanceService.createOrUpdateOrdersOnFirstCampaignCreation(any(), any(), any(), any()))
                .thenReturn(false);
        when(campaignRepository.isFirstCampaignsUnderWallet(anyInt(), any(), any(), any())).thenReturn(false);

        support = new CampaignWithSendingToBalanceAddOperationSupport(campaignRepository,
                campaignBalanceService,
                addServicedCampaignService,
                clientService,
                walletService,
                commonCampaignService,
                userService,
                balanceInfoQueueService,
                ppcDslContextProvider);

        clientInfo = clientSteps.createDefaultClientWithRole(RbacRole.CLIENT);
        campaignId = RandomNumberUtils.nextPositiveLong();
    }

    @Test
    public void testSendToBalanceIfNonWallet() {
        var campaignWithSendingToBalance = (CampaignWithSendingToBalance) defaultCampaignByCampaignType(campaignType)
                .withId(campaignId);

        RestrictedCampaignsAddOperationContainer addCampaignParametersContainer =
                createAddCampaignParametersContainer(clientInfo.getUid());

        support.afterExecution(addCampaignParametersContainer, List.of(campaignWithSendingToBalance));

        if (expectOrderSentToBalance) {
            verify(campaignBalanceService).createOrUpdateOrdersOnFirstCampaignCreation(any(), any(), any(), any());
            verifyNoMoreInteractions(campaignBalanceService);
        } else {
            verifyZeroInteractions(campaignBalanceService);
        }
    }

    private RestrictedCampaignsAddOperationContainer createAddCampaignParametersContainer(Long operatorUid) {
        return RestrictedCampaignsAddOperationContainer.create(
                clientInfo.getShard(),
                operatorUid,
                clientInfo.getClientId(),
                clientInfo.getUid(),
                clientInfo.getUid());
    }
}
