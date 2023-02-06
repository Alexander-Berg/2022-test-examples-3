package ru.yandex.direct.core.entity.campaign.service.type.add;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.Wallet;
import ru.yandex.direct.core.entity.campaign.model.WalletTypedCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.WalletService;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.i18n.I18NBundle;
import ru.yandex.direct.rbac.RbacRole;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.PAUSED_BY_DAY_BUDGET_SMS;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_DAY_BUDGET;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_DAY_BUDGET_SHOW_MODE;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_SMS_TIME_INTERVAL;
import static ru.yandex.direct.core.entity.campaign.utils.CampaignModifyTestUtils.getZero;
import static ru.yandex.direct.core.testing.data.TestCampaigns.TEXT_CAMPAIGN_PRODUCT_ID;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCampaignByCampaignType;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.defaultTimeTarget;

@CoreTest
@RunWith(Parameterized.class)
public class CommonCampaignBeforeExecutionTest {
    public static final String AGENCY_WALLET = "Общий счет (агентский)";

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private Steps steps;

    @Autowired
    private CommonCampaignAddOperationSupport support;

    @Autowired
    private WalletService walletService;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    private UserInfo clientUser;
    private UserInfo operatorUser;

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
        LocaleContextHolder.setLocale(I18NBundle.RU);
        clientUser = steps.clientSteps().createDefaultClientWithRole(RbacRole.EMPTY).getChiefUserInfo();

        operatorUser = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY).getChiefUserInfo();
    }

    @Test
    public void createWallet_FirstCampaign() {
        CommonCampaign commonCampaign = defaultCampaignByCampaignType(campaignType);
        RestrictedCampaignsAddOperationContainer addCampaignParametersContainer =
                createAddCampaignParametersContainer(clientUser, operatorUser);
        support.beforeExecution(addCampaignParametersContainer, List.of(commonCampaign));

        var agencyUidAndClientId = UidAndClientId.of(operatorUser.getUid(), operatorUser.getClientId());
        Wallet walletForNewCampaigns = walletService.getWalletForNewCampaigns(addCampaignParametersContainer,
                agencyUidAndClientId);
        assertThat(walletForNewCampaigns).isNotNull();
        List<? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaigns(clientUser.getShard(),
                List.of(walletForNewCampaigns.getWalletCampaignId()));

        WalletTypedCampaign walletTypedCampaign = StreamEx.of(typedCampaigns)
                .select(WalletTypedCampaign.class)
                .findFirst()
                .get();

        WalletTypedCampaign expectedWalletTypedCampaign = getExpectedWalletTypedCampaign();


        assertThat(walletTypedCampaign)
                .isEqualToIgnoringGivenFields(expectedWalletTypedCampaign,
                        "id",
                        "startDate",
                        "lastChange",
                        "walletInfoLastChange",
                        "walletOnOffTime",
                        "createTime",
                        "source",
                        "metatype");
    }

    private WalletTypedCampaign getExpectedWalletTypedCampaign() {
        return new WalletTypedCampaign()
                .withUid(clientUser.getUid())
                .withClientId(clientUser.getClientId().asLong())
                .withAgencyUid(operatorUser.getUid())
                .withAgencyId(operatorUser.getClientId().asLong())
                .withOrderId(0L)
                .withProductId(TEXT_CAMPAIGN_PRODUCT_ID)
                .withName(AGENCY_WALLET)
                .withType(ru.yandex.direct.core.entity.campaign.model.CampaignType.WALLET)
                .withTimeTarget(defaultTimeTarget())
                .withTimeZoneId(0L)
                .withWalletId(0L)
                .withHasExtendedGeoTargeting(true)
                .withUseCurrentRegion(false)
                .withUseRegularRegion(false)
                .withHasTitleSubstitution(true)
                .withEnableCompanyInfo(true)
                .withEnableCpcHold(false)
                .withHasTurboApp(false)
                .withIsVirtual(false)
                .withSmsFlags(EnumSet.of(PAUSED_BY_DAY_BUDGET_SMS))
                .withEmail(clientUser.getUser().getEmail())
                .withFio(clientUser.getUser().getFio())
                .withWarningBalance(20)
                .withEnableSendAccountNews(true)
                .withEnablePausedByDayBudgetEvent(true)
                .withSmsTime(DEFAULT_SMS_TIME_INTERVAL)
                .withCurrency(clientUser.getClientInfo().getClient().getWorkCurrency())
                .withSumToPay(getZero(6))
                .withSum(getZero(6))
                .withSumSpent(getZero(6))
                .withSumLast(getZero(6))
                .withPaidByCertificate(false)
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withStatusModerate(CampaignStatusModerate.YES)
                .withStatusPostModerate(CampaignStatusPostmoderate.ACCEPTED)
                .withStatusShow(true)
                .withStatusActive(false)
                .withStatusEmpty(false)
                .withStatusArchived(false)
                .withIsServiceRequested(false)
                .withTotalSum(getZero(6))
                .withIsAutoPayOn(false)
                .withTotalChipsCost(getZero(6))
                .withIsSumAggregated(true)
                .withIsSkadNetworkEnabled(false)
                .withDayBudget(DEFAULT_DAY_BUDGET)
                .withDayBudgetShowMode(DEFAULT_DAY_BUDGET_SHOW_MODE)
                .withDayBudgetDailyChangeCount(0);
    }

    @Test
    public void doNotCreateWallet_SecondCampaign() {
        createCampaign();

        CommonCampaign commonCampaign = defaultCampaignByCampaignType(campaignType);
        RestrictedCampaignsAddOperationContainer addCampaignParametersContainer =
                createAddCampaignParametersContainer(clientUser, operatorUser);
        support.beforeExecution(addCampaignParametersContainer, List.of(commonCampaign));

        var agencyUidAndClientId = UidAndClientId.of(operatorUser.getUid(), operatorUser.getClientId());
        Wallet walletForNewCampaigns = walletService.getWalletForNewCampaigns(addCampaignParametersContainer,
                agencyUidAndClientId);
        assertThat(walletForNewCampaigns).isNull();
    }

    public static RestrictedCampaignsAddOperationContainer createAddCampaignParametersContainer(UserInfo clientUser,
                                                                                                UserInfo operatorUser) {
        return RestrictedCampaignsAddOperationContainer.create(
                clientUser.getShard(),
                operatorUser.getUid(),
                clientUser.getClientId(),
                clientUser.getUid(),
                clientUser.getUid());
    }

    private void createCampaign() {
        if (campaignType == CampaignType.DYNAMIC) {
            steps.dynamicCampaignSteps().createDefaultAgencyCampaign(operatorUser, clientUser.getClientInfo());
        } else if (campaignType == CampaignType.TEXT) {
            steps.textCampaignSteps().createDefaultAgencyCampaign(operatorUser, clientUser.getClientInfo());
        } else if (campaignType == CampaignType.PERFORMANCE) {
            steps.smartCampaignSteps().createDefaultAgencyCampaign(operatorUser, clientUser.getClientInfo());
        } else if (campaignType == CampaignType.MOBILE_CONTENT) {
            steps.mobileContentCampaignSteps()
                    .createDefaultAgencyCampaign(operatorUser, clientUser.getClientInfo());
        } else if (campaignType == CampaignType.MCBANNER) {
            steps.mcBannerCampaignSteps().createDefaultAgencyCampaign(operatorUser, clientUser.getClientInfo());
        }
    }
}
