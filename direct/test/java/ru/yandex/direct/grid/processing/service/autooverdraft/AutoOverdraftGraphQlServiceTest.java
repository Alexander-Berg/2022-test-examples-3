package ru.yandex.direct.grid.processing.service.autooverdraft;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.balance.client.exception.BalanceClientException;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.balance.container.PaymentMethodInfo;
import ru.yandex.direct.core.entity.balance.container.PersonInfo;
import ru.yandex.direct.core.entity.balance.container.PersonPaymentMethodInfo;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.client.repository.ClientOptionsRepository;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.BalanceInfo;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.autooverdraft.GdSetAutoOverdraftParams;
import ru.yandex.direct.grid.processing.model.client.GdClient;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeWalletCampaign;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AutoOverdraftGraphQlServiceTest {

    private static final long BALANCE_PERSON_ID = 1L;
    private static final long UNKNOWN_BALANCE_PERSON_ID = -1L;
    private static final String UNLIMITED_PAYMENT_METHOD_CODE = "UNLIMITED_PAYMENT_METHOD_CODE";
    private static final String PAYMENT_METHOD_CODE_WITH_LIMIT_7 = "PAYMENT_METHOD_CODE_WITH_LIMIT_7";
    private static final String UNKNOWN_PAYMENT_METHOD_CODE = "UNKNOWN_PAYMENT_METHOD_CODE";
    private static final List<PersonPaymentMethodInfo> PERSON_PAYMENT_METHOD_INFOS =
            Collections.singletonList(
                    new PersonPaymentMethodInfo()
                            .withPersonInfo(new PersonInfo().withId(BALANCE_PERSON_ID))
                            .withPaymentMethods(
                                    Arrays.asList(
                                            new PaymentMethodInfo()
                                                    .withCode(UNLIMITED_PAYMENT_METHOD_CODE),
                                            new PaymentMethodInfo()
                                                    .withLimit(BigDecimal.valueOf(7))
                                                    .withCode(PAYMENT_METHOD_CODE_WITH_LIMIT_7))));

    private static final BigDecimal UNACCEPTABLY_BIG_POROG = BigDecimal.TEN;
    private Integer shard;
    private ClientId clientId;
    private GdClient client;

    private GridGraphQLContext gridGraphQLContext;

    @Autowired
    private Steps steps;

    @Autowired
    private ClientOptionsRepository clientOptionsRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private AutoOverdraftDataService autoOverdraftDataService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private BalanceService balanceService;

    private AutoOverdraftGraphQlService autoOverdraftGraphQlService;
    private User user;
    private CampaignInfo walletInfo;

    @Before
    public void before() {
        walletInfo = steps.campaignSteps().createCampaign(activeWalletCampaign(null, null));
        user = UserHelper.getUser(walletInfo.getClientInfo().getClient());
        client = new GdClient().withInfo(ContextHelper.toGdClientInfo(user));

        when(balanceService.getPaymentOptions(walletInfo.getUid(), walletInfo.getClientId(),
                walletInfo.getCampaignId())).thenReturn(PERSON_PAYMENT_METHOD_INFOS);

        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign camp = activeTextCampaign(null, null)
                .withName("Name 2");
        camp.getBalanceInfo()
                .withWalletCid(walletInfo.getCampaignId())
                .withSum(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO);
        steps.campaignSteps().createCampaign(
                new CampaignInfo()
                        .withClientInfo(walletInfo.getClientInfo())
                        .withCampaign(camp));

        clientId = walletInfo.getClientInfo().getClientId();
        shard = walletInfo.getShard();
        steps.clientSteps().setOverdraftOptions(shard, clientId, BigDecimal.ONE, BigDecimal.ZERO, null);

        gridGraphQLContext = ContextHelper.buildContext(user);
        autoOverdraftGraphQlService = new AutoOverdraftGraphQlService(autoOverdraftDataService, mock(AutoOverdraftDataLoader.class));
    }

    @Test
    public void successOnGetPaymentOptions() {
        autoOverdraftGraphQlService.getAutoOverdraftPaymentOptions(gridGraphQLContext, client);
    }

    @Test
    public void setPaymentOptionsCausesBsStatusSyncNo() {
        assumeThat(walletInfo.getCampaign().getStatusBsSynced(), is(StatusBsSynced.YES));
        BigDecimal autoOverdraftLimit = BigDecimal.ONE;
        GdSetAutoOverdraftParams setAutoOverdraftParamsRequest =
                new GdSetAutoOverdraftParams().withAutoOverdraftLimit(autoOverdraftLimit)
                        .withPaymentMethodCode(UNLIMITED_PAYMENT_METHOD_CODE).withPersonId(BALANCE_PERSON_ID);

        steps.clientSteps().setOverdraftOptions(shard, clientId, BigDecimal.TEN, BigDecimal.ZERO, null);
        autoOverdraftGraphQlService.setAutoOverdraftLimit(gridGraphQLContext, setAutoOverdraftParamsRequest);
        List<Campaign> campaigns =
                campaignService.getCampaigns(clientId, Collections.singletonList(walletInfo.getCampaignId()));
        assertThat(campaigns, hasItem(hasProperty("statusBsSynced", is(StatusBsSynced.NO))));
    }

    @Test
    public void setTheSameAutoOverdraftLimitDoesNotCauseBsStatusSyncNo() {
        assumeThat(walletInfo.getCampaign().getStatusBsSynced(), is(StatusBsSynced.YES));

        BigDecimal autoOverdraftLimit = BigDecimal.ONE;
        clientOptionsRepository.updateAutoOverdraftLimit(shard, clientId, autoOverdraftLimit);

        GdSetAutoOverdraftParams setAutoOverdraftParamsRequest =
                new GdSetAutoOverdraftParams().withAutoOverdraftLimit(autoOverdraftLimit)
                        .withPaymentMethodCode(UNLIMITED_PAYMENT_METHOD_CODE).withPersonId(BALANCE_PERSON_ID);
        steps.clientSteps().setOverdraftOptions(shard, clientId, BigDecimal.TEN, BigDecimal.ZERO, null);

        autoOverdraftGraphQlService.setAutoOverdraftLimit(gridGraphQLContext, setAutoOverdraftParamsRequest);
        List<Campaign> campaigns =
                campaignService.getCampaigns(clientId, Collections.singletonList(walletInfo.getCampaignId()));
        assertThat(campaigns, hasItem(hasProperty("statusBsSynced", is(StatusBsSynced.YES))));
    }

    @Test(expected = IllegalStateException.class)
    public void failOnZeroOverdraftLimitOnGetPaymentOptions() {
        steps.clientSteps().setOverdraftOptions(shard, clientId, BigDecimal.ZERO, BigDecimal.ZERO, null);
        autoOverdraftGraphQlService.getAutoOverdraftPaymentOptions(gridGraphQLContext, client);
    }

    @Test(expected = GridValidationException.class)
    public void autoOverdraftLimitOutOfRange() {
        GdSetAutoOverdraftParams setAutoOverdraftParamsRequest =
                new GdSetAutoOverdraftParams()
                        .withAutoOverdraftLimit(UNACCEPTABLY_BIG_POROG)
                        .withPaymentMethodCode(UNLIMITED_PAYMENT_METHOD_CODE)
                        .withPersonId(BALANCE_PERSON_ID);
        autoOverdraftGraphQlService.setAutoOverdraftLimit(gridGraphQLContext, setAutoOverdraftParamsRequest);
    }

    @Test(expected = GridValidationException.class)
    public void unknownPerson() {
        GdSetAutoOverdraftParams setAutoOverdraftParamsRequest =
                new GdSetAutoOverdraftParams()
                        .withAutoOverdraftLimit(BigDecimal.ONE)
                        .withPaymentMethodCode(UNLIMITED_PAYMENT_METHOD_CODE)
                        .withPersonId(UNKNOWN_BALANCE_PERSON_ID);
        autoOverdraftGraphQlService.setAutoOverdraftLimit(gridGraphQLContext, setAutoOverdraftParamsRequest);
    }

    @Test(expected = GridValidationException.class)
    public void unknownPaymentMethodCode() {
        GdSetAutoOverdraftParams setAutoOverdraftParamsRequest =
                new GdSetAutoOverdraftParams()
                        .withAutoOverdraftLimit(BigDecimal.ONE)
                        .withPaymentMethodCode(UNKNOWN_PAYMENT_METHOD_CODE)
                        .withPersonId(BALANCE_PERSON_ID);
        autoOverdraftGraphQlService.setAutoOverdraftLimit(gridGraphQLContext, setAutoOverdraftParamsRequest);
    }

    @Test(expected = GridValidationException.class)
    public void paymentMethodCodeLimitExceeded() {
        steps.clientSteps().setOverdraftOptions(shard, clientId, BigDecimal.TEN, BigDecimal.ZERO, null);
        GdSetAutoOverdraftParams setAutoOverdraftParamsRequest =
                new GdSetAutoOverdraftParams()
                        .withAutoOverdraftLimit(BigDecimal.valueOf(8))
                        .withPaymentMethodCode(PAYMENT_METHOD_CODE_WITH_LIMIT_7)
                        .withPersonId(BALANCE_PERSON_ID);
        autoOverdraftGraphQlService.setAutoOverdraftLimit(gridGraphQLContext, setAutoOverdraftParamsRequest);
    }

    @Test(expected = GridValidationException.class)
    public void shouldFailOnAutoOverdraftLimitLessThanOverspendingPlusDebt() {
        // лимит овердрафта из баланса = 10
        BigDecimal overdraftLimit = BigDecimal.TEN;
        // остаток на ОС = -1 (перепотрачено 1)
        BigDecimal overspending = BigDecimal.ONE;
        // у клиента есть долг = 9 (клиенту выставлен акт на сумму 9)
        BigDecimal debt = overdraftLimit.subtract(BigDecimal.ONE);
        // система не должна дать клиенту задать порог овердрафта меньше значения [долг] - [остаток на ОС] = 10
        // в рассматриваемом случае проверим [значение порога] = [долг] = 9 < [долг] - [остаток на ОС] = 10
        BigDecimal autoOverdraftLimit = debt;

        BalanceInfo walletBalance = walletInfo.getCampaign().getBalanceInfo();
        BigDecimal sumSpent = walletBalance.getSum().subtract(walletBalance.getSumSpent()).add(overspending);
        // заводим новую кампанию под общим счетом (ОС)
        // в результате чего общая сумма потраченного на кампаниях под ОС минус зачисленное на ОС
        // должно стать равным -overspending = -1
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign camp = activeTextCampaign(null, null)
                .withName("Name 3");
        camp.getBalanceInfo()
                .withWalletCid(walletInfo.getCampaignId())
                .withSum(BigDecimal.ZERO)
                .withSumSpent(sumSpent);
        steps.campaignSteps().createCampaign(
                new CampaignInfo()
                        .withClientInfo(walletInfo.getClientInfo())
                        .withCampaign(camp));

        steps.clientSteps()
                .setOverdraftOptions(shard, clientId, overdraftLimit, debt, null);
        GdSetAutoOverdraftParams setAutoOverdraftParamsRequest =
                new GdSetAutoOverdraftParams()
                        .withAutoOverdraftLimit(autoOverdraftLimit)
                        .withPaymentMethodCode(UNLIMITED_PAYMENT_METHOD_CODE)
                        .withPersonId(BALANCE_PERSON_ID);
        autoOverdraftGraphQlService.setAutoOverdraftLimit(gridGraphQLContext, setAutoOverdraftParamsRequest);
    }

    @Test
    public void paymentMethodCodeLimitNotExceeded() {
        steps.clientSteps().setOverdraftOptions(shard, clientId, BigDecimal.TEN, BigDecimal.ZERO, null);
        GdSetAutoOverdraftParams setAutoOverdraftParamsRequest =
                new GdSetAutoOverdraftParams()
                        .withAutoOverdraftLimit(BigDecimal.valueOf(6))
                        .withPaymentMethodCode(PAYMENT_METHOD_CODE_WITH_LIMIT_7)
                        .withPersonId(BALANCE_PERSON_ID);
        autoOverdraftGraphQlService.setAutoOverdraftLimit(gridGraphQLContext, setAutoOverdraftParamsRequest);
    }

    @Test
    public void successResetAutoOverdraftLimitWhenOverdraftLimitIsZero() {
        BigDecimal autoOverdraftLimit = BigDecimal.ONE;
        GdSetAutoOverdraftParams setAutooverdraftParamsRequest =
                new GdSetAutoOverdraftParams().withAutoOverdraftLimit(autoOverdraftLimit)
                        .withPaymentMethodCode(UNLIMITED_PAYMENT_METHOD_CODE).withPersonId(BALANCE_PERSON_ID);
        autoOverdraftGraphQlService.setAutoOverdraftLimit(gridGraphQLContext, setAutooverdraftParamsRequest);
        assumeThat(clientService.getClient(clientId).getAutoOverdraftLimit(), comparesEqualTo(autoOverdraftLimit));

        // эмуляция обнуления лимита овердрафта балансом
        steps.clientSteps().setOverdraftOptions(shard, clientId, BigDecimal.ZERO, BigDecimal.ZERO, null);
        autoOverdraftGraphQlService.resetAutoOverdraftLimit(gridGraphQLContext);
        assertThat(clientService.getClient(clientId).getAutoOverdraftLimit(), comparesEqualTo(BigDecimal.ZERO));
    }

    @Test(expected = IllegalStateException.class)
    public void operatorWithFeatureCannotSetLimitToFeaturelessClient() {
        User operatorWithFeature = user;
        CampaignInfo walletInfo = steps.campaignSteps().createCampaign(activeWalletCampaign(null, null));
        User featureLessUser = UserHelper.getUser(walletInfo.getClientInfo().getClient());
        GdClient featurelessClient = new GdClient().withInfo(ContextHelper.toGdClientInfo(featureLessUser));
        steps.clientSteps().setOverdraftOptions(shard, walletInfo.getClientInfo().getClientId(), BigDecimal.TEN,
                BigDecimal.ZERO, null);
        autoOverdraftGraphQlService.getAutoOverdraftPaymentOptions(ContextHelper.buildContext(operatorWithFeature,
                featureLessUser), featurelessClient);
    }

    @Test
    public void errorFromBalanceCauseTransactionRollbackInDirect() {
        BigDecimal defaultAutoOverdraftLimit = BigDecimal.ONE;
        when(balanceService.getPaymentOptions(walletInfo.getUid(), walletInfo.getClientId(),
                walletInfo.getCampaignId()))
                .thenThrow(BalanceClientException.class);

        steps.clientSteps().setOverdraftOptions(shard, clientId, BigDecimal.TEN, BigDecimal.ZERO, null);
        clientOptionsRepository.updateAutoOverdraftLimit(shard, clientId, defaultAutoOverdraftLimit);

        GdSetAutoOverdraftParams setAutoOverdraftParamsRequest =
                new GdSetAutoOverdraftParams()
                        .withAutoOverdraftLimit(BigDecimal.TEN)
                        .withPaymentMethodCode(UNLIMITED_PAYMENT_METHOD_CODE)
                        .withPersonId(BALANCE_PERSON_ID);
        try {
            autoOverdraftGraphQlService.setAutoOverdraftLimit(gridGraphQLContext, setAutoOverdraftParamsRequest);
        } catch (Exception e) {
            assumeThat(e.getClass().isAssignableFrom(BalanceClientException.class), comparesEqualTo(true));
        }
        assertThat(clientService.getClient(clientId).getAutoOverdraftLimit(),
                comparesEqualTo(defaultAutoOverdraftLimit));
    }

}
