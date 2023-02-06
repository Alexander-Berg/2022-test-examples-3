package ru.yandex.autotests.direct.cmd.daybuget.wallet;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.Wallet;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.DayBudget;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestEnvironment;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static ru.yandex.autotests.direct.cmd.daybuget.wallet.WalletDayBudgetHelper.clearDayBudget;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка получения дневного бюджета ОС")
@Stories(TestFeatures.Wallet.DAY_BUDGET)
@Features(TestFeatures.WALLET)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
@Tag("DIRECT-65989")
public class DayBudgetShowsWalletTest {

    private static final String CLIENT = "at-direct-daybudget-os2";
    private static final String DEFAULT_SUM = "1000.00";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private DayBudget dayBudget;

    @Before
    public void before() {
        Long walletId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campaignsSteps()
                .getWallet(Long.valueOf(User.get(CLIENT).getClientID())).getCid();
        clearDayBudget(cmdRule, walletId, CLIENT);

        cmdRule.apiSteps().campaignSteps().verifyHasNonArchivedCampaign(CLIENT);

        dayBudget = new DayBudget()
                .withSum(DEFAULT_SUM)
                .withShowMode(DayBudget.ShowMode.DEFAULT)
                .withSet(true);
        cmdRule.cmdSteps().campaignSteps().setDayBudget(walletId, dayBudget, CLIENT);
    }

    @Test
    @Description("Проверяем получение дневного бюджета в ответе clientWallet")
    @TestCaseId("10981")
    public void checkDayBudgetWalletAtClientWallet() {
        Wallet wallet = cmdRule.cmdSteps().walletSteps().getClientWallet(CLIENT).getWallet();

        check(wallet);
    }

    private void check(Wallet wallet) {
        assertThat("параметры бюджета соответствуют ожиданиям", wallet.getSelf().getDayBudget(),
                beanDiffer(dayBudget.withDailyChangeCount("1").withSet(null)).useCompareStrategy(onlyExpectedFields()));
    }
}
