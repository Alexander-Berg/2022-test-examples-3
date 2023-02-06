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
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.DEFAULT_SIZE_LAYOUT_LEFT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIZE_LAYOUT_LEFT;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Шапка пользователей WS")
@Features({FeaturesConst.MESSAGE_LIST, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.HEAD)
public class WSHeaderLogoTest {

    private static String MAX_L_COLUMN_SIZE = "350";

    public static final String CREDS = "WSHeaderLogoTest";
    public static final String CREDS_2 = "WSHeaderTest";

    private ScreenRulesManager rules = screenRulesManager().withLock(AccLockRule.use().names(CREDS, CREDS_2));
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
            "Устанавливаем стандартную ширину левой колонки",
            of(
                SIZE_LAYOUT_LEFT, DEFAULT_SIZE_LAYOUT_LEFT
            )
        );
    }

    @Test
    @Title("Открываем поиск на юзере с кастомным лого")
    @TestCaseId("4185")
    public void shouldSeeSearchWithCustomLogo() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().mail().home().mail360HeaderBlock().searchInput())
                .shouldSee(st.pages().mail().search().mail360HeaderBlock().closeSearch());

        parallelRun.withActions(actions).withAcc(lock.acc(CREDS)).run();
    }

    @Test
    @Title("Ресайзим почту на юзере с кастомным лого")
    @TestCaseId("4183")
    public void shouldSeeShrinkedMailWithCustomLogo() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(
                st.pages().mail().search().mail360HeaderBlock().searchInput(),
                st.pages().mail().search().mail360HeaderBlock().wsLogo()
            );

        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Устанавливаем максимальную ширину левой колонки",
            of(
                SIZE_LAYOUT_LEFT, MAX_L_COLUMN_SIZE
            )
        );
        parallelRun.withActions(actions).withAcc(lock.acc(CREDS_2)).withUrlPath(SEARCH).run();
    }
}
