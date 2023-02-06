package ru.yandex.autotests.innerpochta.tests.homer;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.props.UrlProps;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на тачёвого гомера")
@Features(FeaturesConst.HOMER)
@Stories(FeaturesConst.SHORTCUTS)
public class HomerTouchScreenTest {

    private static final String ID_FOR_PROMO = "#mobile";
    private static final String COMPOSE_SHORTCUT = "?to=foo@boo.ru&subject=lol&body=olo";

    private TouchScreenRulesManager rules = touchScreenRulesManager();
    private AccLockRule lock = rules.getLock();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd)
        .withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Test
    @Title("Должны увидеть композ с заполненными полями Кому, тема и телом")
    @TestCaseId("10")
    public void shouldSeeFilledCompose() {
        UrlProps.urlProps().setProject("touch");
        Consumer<InitStepsRule> act = st -> {
            st.user().defaultSteps().opensDefaultUrlWithPostFix(COMPOSE.makeTouchUrlPart() + COMPOSE_SHORTCUT);
            st.user().touchSteps().switchToComposeIframe();
            st.user().defaultSteps().shouldSee(st.pages().touch().composeIframe().inputTo());
        };
        parallelRun.withActions(act).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть первую страницу Гомера")
    @TestCaseId("182")
    public void shouldSeeHomer() {
        Consumer<InitStepsRule> act = st -> st.user().defaultSteps()
            .opensDefaultUrl()
            .shouldSee(st.pages().homer().createAccountBtnHeadBanner());

        parallelRun.withActions(act).run();
    }
}
