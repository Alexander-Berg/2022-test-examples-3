package ru.yandex.autotests.innerpochta.tests.messagelist;

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
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGES_PER_PAGE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_SIZE_VIEW_APP;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIZE_LAYOUT_LEFT;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тулбар")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class ToolbarTest {

    private static final int COUNT_MSG = 30;

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

    @Before
    public void setUp() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Устанавливаем ширину почты 1000px, лк 300px, 30 писем на странице и выкл комп. шапку",
            of(
                SETTINGS_SIZE_VIEW_APP, 1000,
                SIZE_LAYOUT_LEFT, 300,
                SETTINGS_PARAM_MESSAGES_PER_PAGE, COUNT_MSG,
                LIZA_MINIFIED_HEADER, EMPTY_STR
            )
        );
    }

    @Test
    @Title("Должны видеть кнопку «Отписаться» в тулбаре для рассылок")
    @TestCaseId("4218")
    public void shouldSeeUnsubscribeButtonOnToolbar() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().shouldSee(st.pages().mail().home().toolbar().unsubscribeButton());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
