package ru.yandex.direct.core.entity.campaign.service;

import java.util.EnumSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.Wallet;
import ru.yandex.direct.core.entity.campaign.model.WalletTypedCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.i18n.I18NBundle;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static ru.yandex.direct.core.entity.campaign.model.SmsFlag.PAUSED_BY_DAY_BUDGET_SMS;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_CAMPAIGN_WARNING_BALANCE;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_DAY_BUDGET;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_DAY_BUDGET_SHOW_MODE;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.DEFAULT_SMS_TIME_INTERVAL;
import static ru.yandex.direct.core.entity.campaign.utils.CampaignModifyTestUtils.getZero;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.defaultTimeTarget;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CommonCampaignServiceAddWalletTest {
    @Autowired
    public CampaignTypedRepository campaignTypedRepository;

    @Autowired
    public CommonCampaignService commonCampaignService;

    @Autowired
    public WalletService walletService;

    @Autowired
    public Steps steps;
    private UserInfo defaultUser;

    @Before
    public void before() {
        defaultUser = steps.userSteps().createDefaultUser();
    }

    @Test
    public void addWalletOnCampaignCreatingRu() {
        LocaleContextHolder.setLocale(I18NBundle.RU);
        addWalletOnCampaignCreating("Общий счет");
    }

    @Test
    public void addWalletOnCampaignCreatingEn() {
        LocaleContextHolder.setLocale(I18NBundle.EN);
        addWalletOnCampaignCreating("Shared account");
    }

    private void addWalletOnCampaignCreating(String expectedWalletName) {
        RestrictedCampaignsAddOperationContainer addCampaignParametersContainer =
                RestrictedCampaignsAddOperationContainer.create(defaultUser.getShard(), defaultUser.getUid(),
                        defaultUser.getClientId(), defaultUser.getUid(), defaultUser.getUid());
        CurrencyCode workCurrency = defaultUser.getClientInfo().getClient().getWorkCurrency();
        long walletId = commonCampaignService.addWalletOnCampaignCreating(addCampaignParametersContainer,
                defaultUser.getClientInfo().getClient(),
                null,
                workCurrency);

        List<Wallet> wallets = walletService.massGetWallets(List.of(defaultUser.getClientId()));
        assertThat(wallets).hasSize(1);

        List<? extends BaseCampaign> actualWallets =
                campaignTypedRepository.getTypedCampaigns(defaultUser.getShard(), List.of(walletId));

        WalletTypedCampaign expectedWallet = getExpectedWallet(expectedWalletName);

        assertThat(actualWallets).hasSize(1);
        assertThat(actualWallets.get(0)).isEqualToIgnoringGivenFields(expectedWallet,
                "id", "lastChange", "startDate", "walletInfoLastChange", "walletOnOffTime", "createTime", "source", "metatype");
    }

    private WalletTypedCampaign getExpectedWallet(String expectedWalletName) {
        return new WalletTypedCampaign()
                .withUid(defaultUser.getUid())
                .withClientId(defaultUser.getClientId().asLong())
                .withAgencyId(0L)
                .withOrderId(0L)
                .withProductId(503162L)
                .withWalletId(0L)
                .withName(expectedWalletName)
                .withType(CampaignType.WALLET)
                .withTimeZoneId(0L)
                .withHasExtendedGeoTargeting(true)
                .withUseCurrentRegion(false)
                .withUseRegularRegion(false)
                .withHasTitleSubstitution(true)
                .withEnableCompanyInfo(true)
                .withEnableCpcHold(false)
                .withHasTurboApp(false)
                .withSmsFlags(EnumSet.of(PAUSED_BY_DAY_BUDGET_SMS))
                .withEmail(defaultUser.getUser().getEmail())
                .withFio(defaultUser.getUser().getFio())
                .withWarningBalance(DEFAULT_CAMPAIGN_WARNING_BALANCE)
                .withEnableSendAccountNews(true)
                .withEnablePausedByDayBudgetEvent(true)
                .withSmsTime(DEFAULT_SMS_TIME_INTERVAL)
                .withCurrency(defaultUser.getClientInfo().getClient().getWorkCurrency())
                .withSumToPay(getZero(6))
                .withSum(getZero(6))
                .withSumSpent(getZero(6))
                .withSumLast(getZero(6))
                .withPaidByCertificate(false)
                .withTotalSum(getZero(6))
                .withTotalChipsCost(getZero(6))
                .withIsAutoPayOn(false)
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withStatusModerate(CampaignStatusModerate.YES)
                .withStatusPostModerate(CampaignStatusPostmoderate.ACCEPTED)
                .withStatusShow(true)
                .withStatusArchived(false)
                .withStatusActive(false)
                .withStatusEmpty(false)
                .withStatusArchived(false)
                .withIsServiceRequested(false)
                .withIsSumAggregated(true)
                .withTimeTarget(defaultTimeTarget())
                .withIsVirtual(false)
                .withIsSkadNetworkEnabled(false)
                .withDayBudget(DEFAULT_DAY_BUDGET)
                .withDayBudgetShowMode(DEFAULT_DAY_BUDGET_SHOW_MODE)
                .withDayBudgetDailyChangeCount(0);
    }

}
