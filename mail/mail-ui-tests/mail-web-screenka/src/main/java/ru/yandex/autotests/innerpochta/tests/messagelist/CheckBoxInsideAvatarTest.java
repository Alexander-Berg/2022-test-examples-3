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
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_MESSAGE_UNION_AVATARS;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Список писем - различные чекбоксы")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
public class CheckBoxInsideAvatarTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 5);
    }

    @Test
    @Title("Проверяем появление квадратных чекбоксов")
    @TestCaseId("2383")
    public void shouldSeeCheckBoxAndAvatar() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps().turnTrue(st.pages().mail().home()
            .displayedMessages().list().get(0).checkBox());
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем объединение чекбоксов с аватарками",
            of(SETTINGS_PARAM_MESSAGE_UNION_AVATARS, FALSE)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем чекбоксы в аватарках")
    @TestCaseId("2287")
    public void shouldSeeOnlyAvatar() {
        Consumer<InitStepsRule> actions = st -> st.user().messagesSteps().clicksOnMessageCheckBox();
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем объединение чекбоксов с аватарками",
            of(SETTINGS_PARAM_MESSAGE_UNION_AVATARS, TRUE)
        );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
