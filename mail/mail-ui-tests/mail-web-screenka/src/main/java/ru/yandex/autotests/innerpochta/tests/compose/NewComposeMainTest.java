package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Email;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collections;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.data.GreetingMessageData.RU_COLLECTOR_TEXT;
import static ru.yandex.autotests.innerpochta.data.GreetingMessageData.RU_MAIL_EN_LANG_SERVICES;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_FOR_SCROLLTOP;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_TODO;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.TIMELINE_COLLAPSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.TIMELINE_ENABLE;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Новый композ - Общие тесты")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class NewComposeMainTest {
    private static final String LONG_SBJ = RU_COLLECTOR_TEXT;
    private static final String LONG_BODY_TEXT = RU_MAIL_EN_LANG_SERVICES;
    private static final String SELF_CONTACT = "Себе";
    private static final String EMAIL_WITH_AVATAR = "testbot2@yandex.ru";
    private static final int COMPOSES_FOR_STACK = 7;

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

    @DataProvider
    public static Object[][] layouts() {
        return new Object[][]{
            {LAYOUT_2PANE},
            {LAYOUT_3PANE_VERTICAL}
        };
    }

    @Before
    public void setUp() {
        Contact contact = stepsProd.user().abookSteps().createDefaultContact()
            .withEmail(Collections.singletonList(new Email().withValue(lock.firstAcc().getSelfEmail())));
        stepsProd.user().apiAbookSteps().addNewContacts(contact);
        stepsProd.user().apiSettingsSteps().callWithListAndParams("Выключаем todo", of(SHOW_TODO, FALSE));
        stepsProd.user().apiTodoSteps().todoSettingsSetCloseTodoList();
    }

    @Test
    @Title("Окрываем окно нового композа")
    @TestCaseId("5502")
    public void shouldSeeComposePopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .shouldSee(
                    st.pages().mail().composePopup().composePopup(),
                    st.pages().mail().composePopup().expandedPopup().hideBtn(),
                    st.pages().mail().composePopup().expandedPopup().closeBtn(),
                    st.pages().mail().composePopup().expandedPopup().sendBtn()
                )
                .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().sbjInput(), LONG_SBJ);
            st.user().hotkeySteps().pressHotKeysWithDestination(
                st.pages().mail().composePopup().expandedPopup().sbjInput(),
                Keys.chord(Keys.CONTROL, "s")
            );
            st.user().defaultSteps().shouldSee(st.pages().mail().composePopup().expandedPopup().savedAt());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Наличие собственного скролла у композа")
    @TestCaseId("5502")
    public void shouldSeeSignatureDropdown() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().setsWindowSize(1200, 600)
                .clicksOn(st.pages().mail().home().composeButton())
                .shouldSee(st.pages().mail().composePopup().composePopup())
                .appendTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), LONG_BODY_TEXT)
                .executesJavaScript(SCRIPT_FOR_SCROLLTOP)
                .waitInSeconds(1);

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Сворачивание попапа композа по кнопке")
    @TestCaseId("5504")
    @UseDataProvider("layouts")
    public void shouldCollapseComposePopup(String layout) {
        String msgSubject = getRandomString();
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams("Включаем " + layout, of(SETTINGS_PARAM_LAYOUT, layout));
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .inputsTextInElement(
                    st.pages().mail().composePopup().expandedPopup().popupTo(),
                    lock.firstAcc().getSelfEmail()
                )
                .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().sbjInput(), msgSubject)
                .clicksOn(st.pages().mail().composePopup().expandedPopup().hideBtn())
                .shouldNotSee(st.pages().mail().composePopup().composePopup());
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Вёрстка свёрнутого композа")
    @TestCaseId("5505")
    @UseDataProvider("layouts")
    public void shouldSeeCollapsedCompose(String layout) {
        String msgSubject = getRandomString();
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams("Включаем " + layout, of(SETTINGS_PARAM_LAYOUT, layout));
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
                .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().sbjInput(), msgSubject)
                .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().popupTo(), EMAIL_WITH_AVATAR)
                .clicksOn(st.pages().mail().composePopup().expandedPopup().hideBtn())
                .shouldNotSee(st.pages().mail().composePopup().composePopup())
                .shouldSee(st.pages().mail().composePopup().composeThumb().get(0))
                .shouldSeeThatElementHasText(
                    st.pages().mail().composePopup().composeThumb().get(0).theme(),
                    msgSubject
                );
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем саджест контактов")
    @TestCaseId("5712")
    public void shouldSeeSuggestInCompose() {
        Consumer<InitStepsRule> actions = this::shouldSeeSuggestInCompose;
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Уменьшаем экран браузера при открытом саджесте контактов")
    @TestCaseId("5712")
    public void shouldSeeSuggestInComposeWithResizeWindow() {
        Consumer<InitStepsRule> actions = st -> {
            shouldSeeSuggestInCompose(st);
            st.user().defaultSteps().setsWindowSize(600, 600)
                .waitInSeconds(1);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Свернутый Todo и свернутый Timeline не заезжают на стек свернутых композов")
    @TestCaseId("5662")
    public void shouldSeeCollapsedTodoAndCollapsedTimelineWithCompose() {
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams(
                "Включаем отображение TODO и TIMELINE (в развернутом виде)",
                of(
                    TIMELINE_ENABLE, TRUE,
                    TIMELINE_COLLAPSE, TRUE,
                    SHOW_TODO, TRUE
                )
            );
        Consumer<InitStepsRule> actions = st -> {
            st.user().composeSteps().fillComposeStack(COMPOSES_FOR_STACK);
            st.user().defaultSteps().clicksOn(st.pages().mail().home().timelineBlock().collapse())
                .shouldNotSee(st.pages().mail().home().timelineBlock().content())
                .shouldSee(
                    st.pages().mail().composePopup().composeStack(),
                    st.pages().mail().home().timelineBlock().expand(),
                    st.pages().mail().home().timelineBlock().newEvent(),
                    st.pages().mail().home().toDoWindow()
                );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Развернутый Todo и свернутый Timeline не заезжают на стек свернутых композов")
    @TestCaseId("5662")
    public void shouldSeeExpandedTodoAndCollapsedTimelineWithCompose() {
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams(
                "Включаем отображение TODO и TIMELINE (в развернутом виде)",
                of(
                    TIMELINE_ENABLE, TRUE,
                    TIMELINE_COLLAPSE, TRUE,
                    SHOW_TODO, TRUE
                )
            );
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiTodoSteps().todoSettingsSetCloseTodoList();
            st.user().composeSteps().fillComposeStack(COMPOSES_FOR_STACK);
            st.user().defaultSteps().refreshPage()
                .clicksOn(
                    st.pages().mail().home().timelineBlock().collapse(),
                    st.pages().mail().home().toDoWindow()
                )
                .shouldNotSee(st.pages().mail().home().timelineBlock().content())
                .shouldSee(
                    st.pages().mail().composePopup().composeStack(),
                    st.pages().mail().home().timelineBlock().expand(),
                    st.pages().mail().home().timelineBlock().newEvent(),
                    st.pages().mail().home().todoListBlock()
                );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Развернутый Todo и развернутый Timeline не заезжают на стек свернутых композов")
    @TestCaseId("5662")
    public void shouldSeeExpandedTodoAndExpandedTimelineWithCompose() {
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams(
                "Включаем отображение TODO и TIMELINE (в развернутом виде)",
                of(
                    TIMELINE_ENABLE, TRUE,
                    TIMELINE_COLLAPSE, TRUE,
                    SHOW_TODO, TRUE
                )
            );
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiTodoSteps().todoSettingsSetCloseTodoList();
            st.user().composeSteps().fillComposeStack(COMPOSES_FOR_STACK);
            st.user().defaultSteps().refreshPage()
                .clicksOn(st.pages().mail().home().toDoWindow())
                .shouldSee(
                    st.pages().mail().composePopup().composeStack(),
                    st.pages().mail().home().timelineBlock().collapse(),
                    st.pages().mail().home().timelineBlock().newEvent(),
                    st.pages().mail().home().timelineBlock().content(),
                    st.pages().mail().home().todoListBlock()
                );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Вызываем саджест контактов")
    private void shouldSeeSuggestInCompose(InitStepsRule st) {
        st.user().defaultSteps().clicksOn(st.pages().mail().home().composeButton())
            .clicksOn(st.pages().mail().composePopup().expandedPopup().popupTo())
            .shouldSeeThatElementHasText(
                st.pages().mail().composePopup().suggestList().waitUntil(not(empty())).get(0),
                SELF_CONTACT
            );
    }
}
