package ru.yandex.autotests.innerpochta.tests.screentests.event;

import io.qameta.allure.junit4.Tag;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetCorpUrlRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.cal.rules.AddLayerIfNeedRule.addLayerIfNeed;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.IGNORED;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.EVENT;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Страница создания события на корпе")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.CORP)
public class CreateCorpEventPageTest {

    private static final String EMAIL_WITH_AVATAR = "robot-mailcorp-5@yandex-team.ru";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED).withClosePromo();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(addLayerIfNeed(() -> stepsTest.user()));

    @Test
    @Title("Должны видеть аватарки в саджесте участников")
    @TestCaseId("922")
    public void shouldSeeCorpAvatarsInSuggest() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().newEventPage().membersField())
            .inputsTextInElement(st.pages().cal().home().newEventPage().membersInput(), EMAIL_WITH_AVATAR)
            .shouldSee(st.pages().cal().home().suggestItem().waitUntil(not(empty())).get(0).contactAvatar());

        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).withUrlPath(EVENT.makeUrlPart("")).run();
    }

    @Test
    @Title("Должны видеть аватарки в яббле участников")
    @TestCaseId("922")
    public void shouldSeeCorpAvatarsInYabble() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().cal().home().newEventPage().membersField())
            .inputsTextInElement(st.pages().cal().home().newEventPage().membersInput(), EMAIL_WITH_AVATAR)
            .clicksOn(st.pages().cal().home().suggestItem().waitUntil(not(empty())).get(0))
            .shouldSee(st.pages().cal().home().newEventPage().membersList().waitUntil(not(empty())).get(0));

        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).withUrlPath(EVENT.makeUrlPart("")).run();
    }

}
