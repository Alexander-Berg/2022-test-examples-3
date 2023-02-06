package ru.yandex.autotests.direct.httpclient.daybudget.autobudgetcmd;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.OldTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.CmdStrategyBeans;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.model.User;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Сохранение дневного бюджета с помощью ajaxSaveAutobudget")
@Stories(TestFeatures.AjaxSave.AJAX_SAVE_AUTOBUDGET)
@Features(TestFeatures.AJAX_SAVE)
@RunWith(Parameterized.class)
@Tag(TrunkTag.YES)
@Tag(CmdTag.AJAX_SAVE_AUTOBUDGET)
@Tag(OldTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class AjaxSaveDayBudgetUsingAutoBudgetCMDTest extends SaveDayBudgetUsingAutobudgetCMDTestBase {

    @Before
    @Override
    public void before() {
        super.before();
        ajaxStrategy = CmdStrategyBeans.getStrategyBean(strategy, User.get(CLIENT).getCurrency());
        cmdRule.cmdSteps().strategySteps().saveAutobudget(bannersRule.getCampaignId().toString(), ajaxStrategy);
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10454")
    public void checkAjaxSaveEnabledDayBudget() {
        super.checkAjaxSaveEnabledDayBudget();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10455")
    public void checkAjaxSaveEnabledDayBudgetStretched() {
        super.checkAjaxSaveEnabledDayBudgetStretched();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10453")
    public void checkAjaxSaveDisabledDayBudget() {
        super.checkAjaxSaveDisabledDayBudget();
    }


}
