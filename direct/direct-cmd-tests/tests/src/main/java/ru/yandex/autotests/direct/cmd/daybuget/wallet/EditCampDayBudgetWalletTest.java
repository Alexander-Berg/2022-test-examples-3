package ru.yandex.autotests.direct.cmd.daybuget.wallet;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.commons.campaign.DayBudget;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
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

@Aqua.Test
@Description("Проверка получения дневного бюджета ОС при просмотре/редактировании кампании")
@Stories(TestFeatures.Wallet.EDIT_CAMP)
@Features(TestFeatures.WALLET)
@Tag(ObjectTag.WALLET)
@Tag(TrunkTag.YES)
@Tag("DIRECT-65989")
@Ignore("Старое редактирование выключено на 100% пользователей")
public class EditCampDayBudgetWalletTest {

    private static final String CLIENT = "at-direct-daybudget-os8";
    private static final String DEFAULT_SUM = "1000.00";

    @ClassRule
    public static DirectCmdRule defaultClassRule = DirectCmdRule.defaultClassRule();

    private BannersRule bannersRule = new TextBannersRule().withUlogin(CLIENT);

    @Rule
    public DirectCmdRule cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);

    private DayBudget dayBudget;

    @Before
    public void before() {

        Long walletId = TestEnvironment.newDbSteps().useShardForLogin(CLIENT).campaignsSteps()
                .getWallet(Long.valueOf(User.get(CLIENT).getClientID())).getCid();
        clearDayBudget(cmdRule, walletId, CLIENT);

        dayBudget = new DayBudget()
                .withSum(DEFAULT_SUM)
                .withShowMode(DayBudget.ShowMode.DEFAULT)
                .withSet(true);
        cmdRule.cmdSteps().campaignSteps().setDayBudget(walletId, dayBudget, CLIENT);
    }

    @Test
    @Description("Проверяем получение дневного бюджета в ответе editCamp")
    @TestCaseId("10982")
    public void checkDayBudgetWalletAtEditCamp() {
        DayBudget actualDayBudget =
                cmdRule.cmdSteps().campaignSteps().getEditCamp(bannersRule.getCampaignId(), CLIENT).getWallet()
                        .getDayBudget();

        assertThat("параметры бюджета соответствуют ожиданиям", actualDayBudget,
                beanDiffer(dayBudget.withSet(null)));
    }

}
