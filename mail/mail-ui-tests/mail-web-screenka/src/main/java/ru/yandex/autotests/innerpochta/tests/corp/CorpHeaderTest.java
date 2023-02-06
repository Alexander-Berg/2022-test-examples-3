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
import ru.yandex.autotests.innerpochta.rules.acclock.UseCreds;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Шапка на корпе")
@Features(FeaturesConst.CORP_HEADER)
@Tag(FeaturesConst.CORP_HEADER)
@Stories(FeaturesConst.CORP)
@UseCreds(CorpHeaderTest.CREDS)
public class CorpHeaderTest {

    public static final String CREDS = "CorpAttachTest";

    private ScreenRulesManager rules = screenRulesManager().withLock(AccLockRule.use().annotation());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

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
            "Выключаем компактную шапку и компактный вид писем",
            of(
                LIZA_MINIFIED, EMPTY_STR,
                LIZA_MINIFIED_HEADER, EMPTY_STR
            )
        );
    }

    private static List<String> MAIN_SERVICES = Arrays.asList(
        "Календарь",
        "Телемост",
        "Стафф",
        "Контакты",
        "Трекер",
        "Вики",
        "Мессенджер",
        "Этушка",
        "Рассылки",
        "Переговорки"
    );

    @Test
    @Title("Клик по жуку открывает форму обратной связи")
    @TestCaseId("5682")
    public void shouldOpenFeedbackFormReport() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().corpPage().mailBugReport())
            .shouldSee(st.pages().mail().corpPage().feedbackFormReport());

        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Клик по крестику закрывает форму обратной связи")
    @TestCaseId("5682")
    public void shouldCloseFeedbackFormReport() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().corpPage().mailBugReport())
            .shouldSee(st.pages().mail().corpPage().feedbackFormReport())
            .clicksOn(st.pages().mail().corpPage().mailBugReportClose())
            .shouldNotSee(st.pages().mail().corpPage().feedbackFormReport());

        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Клик по бургеру в шапке открывает меню корпоративных сервисов")
    @TestCaseId("3965")
    public void shouldSeeInternalSevicesInMenu() {
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().home().mail360HeaderBlock().moreServices())
            .shouldSee(st.pages().mail().home().allServices360Popup());

        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Список сервисов в компактном режиме")
    @TestCaseId("5643")
    public void shouldSeeServices() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем компактное меню",
            of(LIZA_MINIFIED_HEADER, STATUS_ON)
        );
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().home().moreServices())
            .shouldSeeAllElementsInList(st.pages().mail().home().services(), MAIN_SERVICES.toArray(new String[0]));

        parallelRun.withActions(actions).withCorpAcc(lock.firstAcc()).run();
    }
}
