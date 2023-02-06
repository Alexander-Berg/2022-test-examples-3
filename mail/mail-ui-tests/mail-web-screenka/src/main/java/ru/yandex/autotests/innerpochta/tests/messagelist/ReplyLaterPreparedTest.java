package ru.yandex.autotests.innerpochta.tests.messagelist;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
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

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.experiments.ExperimentsConstants.REPLY_LATER_EXP;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на фичу «Напомнить позже»")
@Description("У юзера подготовлены запиненные письма с напоминанием")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.REPLY_LATER)
public class ReplyLaterPreparedTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().className();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Вёрстка запиненных писем с напоминанием")
    @Description("Прочитанное/непрочитанное письмо, тред, важное, с аттачами")
    @TestCaseId("6400")
    public void shouldSeeReplyLaterMenu() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().addExperimentsWithYexp(REPLY_LATER_EXP)
                .clicksOn(st.pages().mail().home().displayedMessages().list().get(0).expandThread());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка плашки в просмотре письма после закрепа")
    @TestCaseId("6382")
    public void shouldSeeReplyLaterBarInPin() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().addExperimentsWithYexp(REPLY_LATER_EXP)
                .clicksOn(st.pages().mail().home().displayedMessages().list().get(2))
                .shouldSee(st.pages().mail().msgView().deleteReminderBtn());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
