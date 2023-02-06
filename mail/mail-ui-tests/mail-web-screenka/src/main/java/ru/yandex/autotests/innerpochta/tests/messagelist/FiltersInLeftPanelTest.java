package ru.yandex.autotests.innerpochta.tests.messagelist;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
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

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Системные метки в левой колонке")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.SIMPLE_FILTERS)
@Description("Подготовлен юзер со всеми письмами по фильтрам")
public class FiltersInLeftPanelTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Открываем фильтр важных из ЛК")
    @TestCaseId("2820")
    public void shouldSeeImportantMessagesList() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().msgFiltersBlock().showImportant())
                .shouldBeOnUrlWith(QuickFragments.IMPORTANT);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем фильтр непрочитанных из ЛК")
    @TestCaseId("2821")
    public void shouldSeeUnreadMessagesList() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().msgFiltersBlock().showUnread())
                .shouldBeOnUrlWith(QuickFragments.UNREAD);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем фильтр с вложениями из ЛК")
    @TestCaseId("1003")
    public void shouldSeeMessagesWithAttachList() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().msgFiltersBlock().showWithAttach())
                .shouldBeOnUrlWith(QuickFragments.ATTACHMENTS);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем фильтр «Ждут ответа» из ЛК")
    @TestCaseId("2849")
    public void shouldSeeWaitingForReplyMessagesList() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().msgFiltersBlock().waitForAnswer())
                .shouldBeOnUrl(containsString("#remind"));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
