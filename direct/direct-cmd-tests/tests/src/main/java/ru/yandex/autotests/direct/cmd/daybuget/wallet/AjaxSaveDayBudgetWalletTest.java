package ru.yandex.autotests.direct.cmd.daybuget.wallet;

import org.junit.Before;
import org.junit.BeforeClass;
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
@Description("Проверка сохранения дневного бюджета для ОС")
@Stories(TestFeatures.Wallet.DAY_BUDGET)
@Features(TestFeatures.WALLET)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
@Tag("DIRECT-65989")
public class AjaxSaveDayBudgetWalletTest {

    private static final String CLIENT = "at-direct-daybudget-os10";
    private static final String DEFAULT_SUM = "1000.00";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private Long walletId;

    private DayBudget dayBudget;

    @BeforeClass
    public static void fixClient() {
        defaultClassRule.apiSteps().verifyHasActiveCampaign(CLIENT);
    }

    @Before
    public void before() {
        walletId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campaignsSteps()
                .getWallet(Long.valueOf(User.get(CLIENT).getClientID())).getCid();
        clearDayBudget(cmdRule, walletId, CLIENT);

        dayBudget = new DayBudget()
                .withSum(DEFAULT_SUM)
                .withShowMode(DayBudget.ShowMode.DEFAULT)
                .withSet(true);
    }

    @Test
    @Description("Проверяем сохранение дневного бюджета в режиме 'стандартный'")
    @TestCaseId("10976")
    public void checkAjaxSaveEnabledDayBudgetWallet() {
        cmdRule.cmdSteps().campaignSteps().setDayBudget(walletId, dayBudget, CLIENT);

        check();
    }

    @Test
    @Description("Проверяем сохранение дневного бюджета в режиме 'распределенный'")
    @TestCaseId("10975")
    public void checkAjaxSaveEnabledDayBudgetStretched() {
        cmdRule.cmdSteps().campaignSteps().setDayBudget(walletId, dayBudget, CLIENT);

        check();
    }

    @Test
    @Description("Проверяем выключение дневного бюджета")
    @TestCaseId("10977")
    public void checkAjaxSaveDisabledDayBudget() {
        cmdRule.cmdSteps().campaignSteps().setDayBudget(walletId, dayBudget, CLIENT);

        dayBudget.withSum(null)
                .withShowMode(null)
                .withSet(false);

        cmdRule.cmdSteps().campaignSteps().setDayBudget(walletId, dayBudget, CLIENT);

        dayBudget.withSum("0.00");
        check();
    }

    private void check() {
        Wallet wallet = cmdRule.cmdSteps().walletSteps().getClientWallet(CLIENT).getWallet();

        assertThat("параметры бюджета соответствуют ожиданиям", wallet.getSelf().getDayBudget(),
                beanDiffer(dayBudget.withSet(null)).useCompareStrategy(onlyExpectedFields()));
    }
}
