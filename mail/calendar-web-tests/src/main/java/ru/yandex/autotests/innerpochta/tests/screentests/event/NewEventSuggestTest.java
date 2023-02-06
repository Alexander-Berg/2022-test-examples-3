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

import static ru.yandex.autotests.innerpochta.cal.rules.RemoveAllOldAndCreateNewLayer.removeAllOldAndCreateNewLayer;
import static ru.yandex.autotests.innerpochta.cal.util.CalConsts.IGNORED;
import static ru.yandex.autotests.innerpochta.cal.util.CalFragments.EVENT;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Саджест контактов при создании события")
@Features(FeaturesConst.CAL)
@Tag(FeaturesConst.CAL)
@Stories(FeaturesConst.NEW_EVENT_POPUP)
public class NewEventSuggestTest {

    private static final String CONTACT = "robbiter-";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest)
        .withIgnoredAreas(IGNORED);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createCalendarRuleChain()
        .around(removeAllOldAndCreateNewLayer(() -> stepsProd.user()));

    @Test
    @Title("Показываем ховер на саджест контактов")
    @TestCaseId("722")
    public void shouldSeeHoverOnContactsSuggest() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(
                st.pages().cal().home().leftPanel().createEvent(),
                st.pages().cal().home().newEventPage().membersField()
            )
                .inputsTextInElement(st.pages().cal().home().newEventPage().membersInput(), CONTACT)
                .shouldSee(st.pages().cal().home().suggest())
                .waitInSeconds(1)
                .onMouseHover(st.pages().cal().home().suggestItem().get(1));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть саджест контактов")
    @Description("Юзеру добавлены контакты")
    @TestCaseId("55")
    public void shouldSeeContactsSuggest() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(
                    st.pages().cal().home().newEventPage().membersField(),
                    st.pages().cal().home().newEventPage().membersInput()
                )
                .inputsTextInElement(st.pages().cal().home().newEventPage().membersInput(), "a")
                .shouldSee(st.pages().cal().home().suggest());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(EVENT.makeUrlPart("")).run();
    }
}
