package ru.yandex.autotests.direct.cmd.daybuget.wallet;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.campaigns.AjaxSaveDayBudgetRequest;
import ru.yandex.autotests.direct.cmd.data.commons.CommonResponse;
import ru.yandex.autotests.direct.cmd.data.commons.ErrorResponse;
import ru.yandex.autotests.direct.cmd.data.commons.Wallet;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.DayBudget;
import ru.yandex.autotests.direct.cmd.data.wallet.DayBudgetErrorsEnum;
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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.direct.cmd.daybuget.wallet.WalletDayBudgetHelper.clearDayBudget;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;

@Aqua.Test
@Description("Проверка получения ошибок при сохранении дневного бюджета для ОС")
@Stories(TestFeatures.Wallet.DAY_BUDGET)
@Features(TestFeatures.WALLET)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
@Tag("DIRECT-65989")
public class AjaxSaveDayBudgetWalletValidationTest {

    private static final String CLIENT = "at-direct-daybudget-os";
    private static final String MAX_DAY_BUDGET = "1 000 000 000.00";
    private static final String WRONG_SUM = "-1.00";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule();

    private Long walletId;

    private DayBudget dayBudget;

    @Before
    public void before() {
        walletId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campaignsSteps()
                .getWallet(Long.valueOf(User.get(CLIENT).getClientID())).getCid();
        clearDayBudget(cmdRule, walletId, CLIENT);

        dayBudget = new DayBudget()
                .withShowMode(DayBudget.ShowMode.DEFAULT)
                .withSet(true);
    }

    @Test
    @Description("Проверяем сохранение дневного бюджета при сумме больше допустимого")
    @TestCaseId("10979")
    public void checkAjaxSaveEnabledDayBudgetMaxSum() {
        dayBudget.withSum(String.valueOf(Integer.MAX_VALUE));
        AjaxSaveDayBudgetRequest request = new AjaxSaveDayBudgetRequest()
                .withCid(String.valueOf(walletId))
                .withDayBudget(dayBudget)
                .withUlogin(CLIENT);
        ErrorResponse response = cmdRule.cmdSteps().campaignSteps().postAjaxSaveDayBudgetErrorResponse(request);
        assertThat("превышен дневной бюджет", response.getError(),
                containsString(String.format(DayBudgetErrorsEnum.MAX_DAY_BUDGET.getErrorText(), MAX_DAY_BUDGET)));
    }

    @Test
    @Description("Проверяем сохранение дневного бюджета при сумме меньше нуля")
    @TestCaseId("10978")
    public void checkAjaxSaveEnabledDayBudgetNegativeSum() {
        dayBudget.withSum(WRONG_SUM);
        AjaxSaveDayBudgetRequest request = new AjaxSaveDayBudgetRequest()
                .withCid(String.valueOf(walletId))
                .withDayBudget(dayBudget)
                .withUlogin(CLIENT);
        ErrorResponse response = cmdRule.cmdSteps().campaignSteps().postAjaxSaveDayBudgetErrorResponse(request);
        assertThat("превышен дневной бюджет", response.getError(),
                containsString(DayBudgetErrorsEnum.WRONG_DAY_DUBGET_SUM.getErrorText()));
    }
}
