package ru.yandex.autotests.innerpochta.tests.autotests;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_TOUCH_PART;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на форму обратной связи")
@Features(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
public class FeedbackFormTest {

    private static final String TEXT_20_LENGHT = "01234567890123456789";
    private static final String TEXT_19_LENGHT = "0123456789012345678";
    private static final String SUPPORT_URLPART = "support";
    private static final String FEEDBACK = "Обратная связь";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Кнопка отправки в фос активируется после 20 введённых символов")
    @TestCaseId("612")
    public void shouldSeeSendBtnActive() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(QuickFragments.FEEDBACK.makeTouchUrlPart())
            .shouldSee(steps.pages().touch().compose().composeTitle())
            .shouldNotSee(steps.pages().touch().compose().sendBtn())
            .clicksAndInputsText(steps.pages().touch().compose().inputBody(), TEXT_20_LENGHT)
            .shouldSee(steps.pages().touch().compose().sendBtn());
    }

    @Test
    @Title("Кнопка отправки в фос неактививна, если введено <20 символов")
    @TestCaseId("612")
    public void shouldNotSeeSendBtnActive() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(QuickFragments.FEEDBACK.makeTouchUrlPart())
            .shouldSee(steps.pages().touch().compose().composeTitle())
            .shouldNotSee(steps.pages().touch().compose().sendBtn())
            .clicksAndInputsText(steps.pages().touch().compose().inputBody(), TEXT_19_LENGHT)
            .shouldNotSee(steps.pages().touch().compose().sendBtn());
    }

    @Test
    @Title("Должны закрыть форму обратной связи")
    @TestCaseId("1268")
    public void shouldCloseFeedbackForm() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(SUPPORT_URLPART))
            .clicksOnElementWithText(
                steps.pages().touch().settings().settingSectionItems().waitUntil(not(empty())),
                FEEDBACK
            )
            .shouldSee(steps.pages().touch().compose().composeTitle())
            .clicksOn(steps.pages().touch().compose().closeBtn())
            .shouldNotSee(steps.pages().touch().compose().composeTitle())
            .shouldBeOnUrl(containsString(SUPPORT_URLPART));
    }
}
