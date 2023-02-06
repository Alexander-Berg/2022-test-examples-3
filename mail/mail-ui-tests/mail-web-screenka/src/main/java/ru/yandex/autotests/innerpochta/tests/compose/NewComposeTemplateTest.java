package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
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

import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Шаблоны")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class NewComposeTemplateTest {

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    private static final String BODY_MSG = getRandomString();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Before
    public void setUp() {
        stepsProd.user().apiMessagesSteps().createTemplateMessage(lock.firstAcc(), 5);
    }

    @Test
    @Title("Проверяем верстку попапа шаблонов")
    @TestCaseId("5916")
    public void shouldSeeTemplatePopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().templatesBtn())
                .shouldSee(st.pages().mail().composePopup().expandedPopup().templatePopup());
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Проверяем верстку попапа шаблонов после изменения шаблона")
    @TestCaseId("5916")
    public void shouldSeeTemplatePopupAfterEdit() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().templatesBtn())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().templatePopup().templateList().get(0))
                .inputsTextInElementClearingThroughHotKeys(
                    st.pages().mail().composePopup().expandedPopup().bodyInput(),
                    BODY_MSG
                )
                .shouldSee(st.pages().mail().composePopup().expandedPopup().templatesNotif())
                .clicksOn(st.pages().mail().composePopup().expandedPopup().templatesBtn())
                .shouldSee(st.pages().mail().composePopup().expandedPopup().templatePopup());
        };

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
