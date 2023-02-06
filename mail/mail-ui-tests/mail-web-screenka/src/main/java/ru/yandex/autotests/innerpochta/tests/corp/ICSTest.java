package ru.yandex.autotests.innerpochta.tests.corp;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
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
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_DISABLE_EVENTS;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Виджет ICS на корпе")
@Features(FeaturesConst.ICS)
@Tag(FeaturesConst.ICS)
@Stories(FeaturesConst.CORP)
@Description("У пользователя заранее подготовлены встречи")
public class ICSTest {
    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    private static final String NEW = "Новая встреча";
    private static final String ACCEPTED = "Принятая встречка";
    private static final String SELF = "Встреча у организатора";

    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем компактный вид писем, включаем выделение писем от календаря",
            of(
                LIZA_MINIFIED, EMPTY_STR,
                SETTINGS_PARAM_DISABLE_EVENTS, FALSE
            )
        );
    }

    @Test
    @Title("Виджет ICS на корпе")
    @TestCaseId("1970")
    public void shouldSeeReceiverICSWidget() {
        Consumer<InitStepsRule> actions = st -> st.user().messagesSteps()
            .shouldSeeMessageWithSubject(NEW)
            .shouldSeeMessageWithSubject(ACCEPTED)
            .shouldSeeMessageWithSubject(SELF);

        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Виджет ICS на корпе в компактном режиме")
    @TestCaseId("2155")
    public void shouldNotSeeICSWidgetWithCompactMode() {
        Consumer<InitStepsRule> actions = st -> st.user().messagesSteps()
            .shouldSeeMessageWithSubject(NEW)
            .shouldSeeMessageWithSubject(ACCEPTED)
            .shouldSeeMessageWithSubject(SELF);

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем компактный вид писем",
            of(LIZA_MINIFIED, STATUS_ON)
        );
        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть виджет ICS при включенной настройке выделения писем от календаря")
    @TestCaseId("3970")
    public void shouldSeeICSWidgetButton() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(
                st.pages().mail().home().displayedMessages().list().get(4).icsButton()
            );

        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Не должны видеть виджет ICS при выключенной настройке выделения писем от календаря")
    @TestCaseId("3970")
    public void shouldNotSeeICSWidgetButton() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldNotSee(
                st.pages().mail().home().displayedMessages().list().get(4).icsButton()
            )
                .shouldSee(st.pages().mail().home().displayedMessages().list().get(4).attachments().download());

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем выделение писем от календаря",
            of(SETTINGS_PARAM_DISABLE_EVENTS, TRUE)
        );
        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).run();
    }
}
