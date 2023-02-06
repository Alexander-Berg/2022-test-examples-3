package ru.yandex.autotests.innerpochta.tests.screentests;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Виджеты")
@Description("Юзерам каждый день приходят письма с нужным типом писем")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.WIDGET)
@RunWith(DataProviderRunner.class)
public class WidgetScreenTest {

    private TouchScreenRulesManager rules = touchScreenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Test
    @Title("Должны увидеть виджет авиабилетов: актуальных и протухших")
    @DataProvider({"7", "10"})
    @TestCaseId("288")
    public void shouldSeeWidgetAviatickets(String fid) {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageList().widgetElements().widgetAviatickets());

        parallelRun.withActions(act).withAcc(acc.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart(fid)).run();
    }

    @Test
    @Title("Должны увидеть виджет отелей")
    @TestCaseId("24")
    public void shouldSeeWidgetHotel() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageList().widgetElements().widgetHotel());

        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(FOLDER_ID.makeTouchUrlPart("8")).run();
    }

    @Test
    @Title("Должны свайпнуть письмо с виджетом и увидеть, что ничего не съехало")
    @TestCaseId("1087")
    public void shouldSwipeWidgetMsg() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().rightSwipe(st.pages().touch().messageList().messageBlock());
            st.user().defaultSteps().shouldSee(st.pages().touch().messageList().messageBlock().swipeFirstBtn());
        };
        parallelRun.withActions(act).withAcc(acc.firstAcc())
            .withUrlPath(FOLDER_ID.makeTouchUrlPart("9")).run();
    }
}
