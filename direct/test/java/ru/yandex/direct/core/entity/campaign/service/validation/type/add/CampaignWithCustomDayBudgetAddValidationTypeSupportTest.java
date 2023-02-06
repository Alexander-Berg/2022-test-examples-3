package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomDayBudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsDayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomEnumUtils;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.DAY_BUDGET_OVERRIDEN_BY_WALLET;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.DAY_BUDGET_SHOW_MODE_OVERRIDEN_BY_WALLET;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasWarningWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.ids.NumberDefectIds.MUST_BE_IN_THE_INTERVAL_INCLUSIVE;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithCustomDayBudgetAddValidationTypeSupportTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ClientService clientService;

    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private CampaignWithCustomDayBudgetAddValidationTypeSupport validationTypeSupport;

    private ClientId clientId;
    private Long operatorUid;
    private CampaignValidationContainer container;
    private Long campaignId;
    private Integer shard;
    private CurrencyCode currencyCode;

    @Parameterized.Parameter
    public CampaignType campaignType;
    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        campaignId = RandomNumberUtils.nextPositiveLong();
        operatorUid = RandomNumberUtils.nextPositiveLong();
        shard = RandomNumberUtils.nextPositiveInteger();
        container = CampaignValidationContainer.create(shard, operatorUid, clientId);
        currencyCode = RandomEnumUtils.getRandomEnumValue(CurrencyCode.class);
        Client client = new Client()
                .withClientId(clientId.asLong())
                .withWorkCurrency(currencyCode);
        doReturn(client)
                .when(clientService).getClient(clientId);
    }

    @Test
    public void validate_Successfully() {
        var campaign = createCampaign(BigDecimal.ZERO, DayBudgetShowMode.DEFAULT_, 0L);
        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_expectDayBudgetCannotBeNull() {
        var campaign = createCampaign(null, null, 0L);

        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithCustomDayBudget.DAY_BUDGET)),
                DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void validate_expectDayBudgetShowModeCannotBeNull() {
        var campaign = createCampaign(null, null, 0L);

        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithCustomDayBudget.DAY_BUDGET_SHOW_MODE)),
                DefectIds.CANNOT_BE_NULL)));
    }

    @Test
    public void validate_expectMustBeInTheIntervalInclusive() {
        Long walletId = 1L;
        var campaign = createCampaign(currencyCode.getCurrency().getMaxDailyBudgetAmount().add(BigDecimal.ONE),
                DayBudgetShowMode.DEFAULT_, walletId);
        var wallet = new Campaign()
                .withStrategy((DbStrategy) new DbStrategy().withDayBudget(BigDecimal.ONE))
                .withId(walletId);

        doReturn(List.of(wallet))
                .when(campaignRepository).getCampaigns(eq(shard), anyList());

        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithCustomDayBudget.DAY_BUDGET)),
                MUST_BE_IN_THE_INTERVAL_INCLUSIVE)));
    }

    @Test
    public void validate_expectDayBudgetOverridenByWallet() {
        Long walletId = 1L;
        var campaign = createCampaign(currencyCode.getCurrency().getMaxDailyBudgetAmount().subtract(BigDecimal.ONE),
                DayBudgetShowMode.DEFAULT_, walletId);
        var wallet = new Campaign()
                .withStrategy((DbStrategy) new DbStrategy()
                        .withDayBudget(BigDecimal.TEN)
                        .withDayBudgetShowMode(CampaignsDayBudgetShowMode.DEFAULT_))
                .withId(walletId);

        doReturn(List.of(wallet))
                .when(campaignRepository).getCampaigns(eq(shard), anyList());

        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasWarningWithDefinition(validationError(
                path(index(0), field(CampaignWithCustomDayBudget.DAY_BUDGET)),
                DAY_BUDGET_OVERRIDEN_BY_WALLET)));

        assertThat(vr, hasNoErrors());
    }

    @Test
    public void validate_expectDayBudgetShowModeOverridenByWallet() {
        Long walletId = 1L;
        var campaign = createCampaign(currencyCode.getCurrency().getMaxDailyBudgetAmount().subtract(BigDecimal.ONE),
                DayBudgetShowMode.DEFAULT_, walletId);
        var wallet = new Campaign()
                .withStrategy((DbStrategy) new DbStrategy()
                        .withDayBudget(BigDecimal.TEN)
                        .withDayBudgetShowMode(CampaignsDayBudgetShowMode.STRETCHED))
                .withId(walletId);
        doReturn(List.of(wallet))
                .when(campaignRepository).getCampaigns(eq(shard), anyList());
        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasWarningWithDefinition(validationError(
                path(index(0), field(CampaignWithCustomDayBudget.DAY_BUDGET_SHOW_MODE)),
                DAY_BUDGET_SHOW_MODE_OVERRIDEN_BY_WALLET)));

        assertThat(vr, hasNoErrors());
    }

    private CampaignWithCustomDayBudget createCampaign(BigDecimal dayBudget,
                                                       DayBudgetShowMode dayBudgetShowMode,
                                                       Long walletId) {
        CommonCampaign campaign = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withId(campaignId)
                .withClientId(clientId.asLong())
                .withWalletId(walletId)
                .withName("valid_campaign_name")
                .withUid(operatorUid);
        return ((CampaignWithCustomDayBudget) campaign)
                .withDayBudget(dayBudget)
                .withDayBudgetShowMode(dayBudgetShowMode);
    }
}
