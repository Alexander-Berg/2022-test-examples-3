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

/**
 * @author : Alex Samokhin (alex-samo@yandex-team.ru)
 *         Date: 19.09.14
 *         https://st.yandex-team.ru/TESTIRT-2748
 */

@Aqua.Test
@Description("Cохранение дневного бюджета при редактировании стратегии существующей кампании")
@Stories(TestFeatures.AjaxSave.AJAX_SAVE_AUTOBUDGET)
@Features(TestFeatures.AJAX_SAVE)
@RunWith(Parameterized.class)
@Tag(TrunkTag.YES)
@Tag(CmdTag.AJAX_SAVE_AUTOBUDGET)
@Tag(OldTag.YES)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class AjaxEditStrategyCheckDayBudgetSavingTest extends SaveDayBudgetUsingAutobudgetCMDTestBase {

    @Before
    @Override
    public void before() {
        super.before();
        ajaxStrategy = CmdStrategyBeans.getStrategyBean(strategy, User.get(CLIENT).getCurrency());
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10451")
    public void checkAjaxSaveEnabledDayBudget() {
        super.checkAjaxSaveEnabledDayBudget();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10452")
    public void checkAjaxSaveEnabledDayBudgetStretched() {
        super.checkAjaxSaveEnabledDayBudgetStretched();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("10450")
    public void checkAjaxSaveDisabledDayBudget() {
        super.checkAjaxSaveDisabledDayBudget();
    }

}
