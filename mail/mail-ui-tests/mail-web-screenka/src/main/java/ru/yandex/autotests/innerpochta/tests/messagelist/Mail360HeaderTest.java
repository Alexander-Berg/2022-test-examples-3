package ru.yandex.autotests.innerpochta.tests.messagelist;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
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
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL360_PAID;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Почта 360 - шапка")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class Mail360HeaderTest {

    private static final String DARK_THEME_QUERY_PARAM = "?theme=lamp";

    private ScreenRulesManager rules = screenRulesManager();
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule lock = rules.getLock().useTusAccount(MAIL360_PAID);
    private RunAndCompare parallelRun = runAndCompare()
        .withProdSteps(stepsProd)
        .withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createRuleChain();

    @Test
    @Title("Верстка шапки")
    @TestCaseId("5970")
    public void shouldSeeMail360Header() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().home().mail360HeaderBlock());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ховер на кнопку сервиса в шапке")
    @TestCaseId("5970")
    public void shouldHoverOnServiceIcon() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().onMouseHover(st.pages().mail().home().mail360HeaderBlock().serviceIcons().get(1));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Отображение шапки в темной теме")
    @TestCaseId("5970")
    public void shouldSeeMail360HeaderInDarkTheme() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().home().mail360HeaderBlock());

        parallelRun.withActions(actions).withUrlPath(DARK_THEME_QUERY_PARAM).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Верстка выпадушки Еще")
    @TestCaseId("5962")
    public void shouldSeeMoreServicesPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().moreServices())
                .shouldSee(st.pages().mail().home().allServices360Popup());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Ховер на кнопку сервиса в выпадушке Еще")
    @TestCaseId("5970")
    public void shouldHoverOnServiceIconInMoreServicesPopup() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().mail360HeaderBlock().moreServices())
                .onMouseHover(st.pages().mail().home().allServices360Popup().serviceIcons().get(5));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Не должны видеть кнопку «Улучшить Почту 360»")
    @TestCaseId("5966")
    public void shouldNotSeeUpgradeMail360Btn() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldNotSee(st.pages().mail().home().mail360HeaderBlock().upgradeMail360());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

}
