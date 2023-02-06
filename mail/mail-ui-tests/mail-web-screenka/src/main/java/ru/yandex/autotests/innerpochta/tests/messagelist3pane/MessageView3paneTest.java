package ru.yandex.autotests.innerpochta.tests.messagelist3pane;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
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

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_HEAD_FULL_EDITION;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Просмотр письма 3пейн")
@Features(FeaturesConst.THREE_PANE)
@Tag(FeaturesConst.THREE_PANE)
@Stories(FeaturesConst.COMPACT_VIEW)
public class MessageView3paneTest {

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
            "Включаем 3pane-vertical",
            of(SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL)
        );
    }

    @Test
    @Title("Должны видеть всех получателей")
    @Description("Заранее заготовлено письмо с несколькими получателями")
    @TestCaseId("3165")
    public void shouldSeeMoreInfoAboutReceivers() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiSettingsSteps().callWithListAndParams("Сворачиваем получателей письма",
                of(SETTINGS_HEAD_FULL_EDITION, false));
            st.user().defaultSteps().refreshPage();
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().messageSubject())
                .clicksOn(st.pages().mail().msgView().messageHead().showFieldToggler());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть все аттачи")
    @Description("Заранее заготовлено письмо с многими аттачами")
    @TestCaseId("3166")
    public void shouldSeeAllAttaches() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().setsWindowSize(1200, 1200);
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().onMouseHoverAndClick(st.pages().mail().msgView().attachments().infoBtn());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть выпадушку «Еще» в тулбаре письма")
    @TestCaseId("3167")
    public void shouldSeeMoreDropdown() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().contentToolbarBlock().moreBtn());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть развернутую цитату")
    @Description("Заранее заготовлено письмо с цитатой")
    @TestCaseId("3168")
    public void shouldSeeFullQuote() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().clicksOn(
                st.pages().mail().msgView().messageTextBlock().quotes().get(0).showFullQuote()
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны перейти в пустую папку")
    @TestCaseId("3169")
    public void shouldSeeEmptyFolder() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().foldersNavigation().customFolders().get(2));

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть горизонтальный скролл в широком письме")
    @Description("Заранее заготовлено письмо с широким инлайн-аттачем")
    @TestCaseId("3170")
    public void shouldSeeScrollUnderMessage() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(2);
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().messageHead());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть развернутый QR")
    @TestCaseId("3171")
    @DoTestOnlyForEnvironment("Not FF")
    public void shouldSeeQRActivated() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().quickReplyPlaceholder())
                .waitInSeconds(1);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть текстовое письмо отформатированное пробелами")
    @TestCaseId("1067")
    public void shouldSeeTextWithSpacesMessage() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(1);
            st.user().defaultSteps().shouldSee(st.pages().mail().msgView().messageHead());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
