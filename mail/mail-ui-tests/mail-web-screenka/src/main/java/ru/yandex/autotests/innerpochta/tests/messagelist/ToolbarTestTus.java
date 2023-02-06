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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.DRAFT;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMPORTANT_LABEL_NAME_RU;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.CUSTOM_BUTTONS;
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
public class ToolbarTestTus {

    private static final int CUSTOM_COUNT_MSG = 5;

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
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Устанавливаем ширину почты 1000px, лк 300px",
            of(
                SETTINGS_SIZE_VIEW_APP, 1000,
                SIZE_LAYOUT_LEFT, 300
            )
        );
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 1);
    }

    @Test
    @Title("Жмем «Выбрать все письма» а затем «Еще письма»")
    @TestCaseId("2816")
    public void shouldSeeAllMessagesChecked() {
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 6);
        stepsProd.user().apiSettingsSteps()
            .callWithListAndParams(
                "Включаем показ 5 писем на странице",
                of(SETTINGS_PARAM_MESSAGES_PER_PAGE, CUSTOM_COUNT_MSG)
            );
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().selectsAllDisplayedMessagesInFolder();
            st.user().messagesSteps().loadsMoreMessages();
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем на «Ещё» в тулбаре")
    @TestCaseId("2746")
    public void shouldSeeMoreDropdown() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().setsWindowSize(1200, 600);
            st.user().messagesSteps().clicksOnMessageCheckBoxByNumber(0);
            st.user().defaultSteps().clicksOn(st.pages().mail().home().toolbar().moreBtn())
                .shouldSee(st.pages().mail().home().toolbarMoreMenu());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем на «Создать шаблон»")
    @TestCaseId("3030")
    public void shouldSeeTemplateCompose() {
        String text = getRandomString();
        Consumer<InitStepsRule> actions = st -> st.user().defaultSteps()
            .clicksOn(st.pages().mail().home().toolbar().createTemplateButton())
            .shouldSee(st.pages().mail().composePopup().expandedPopup())
            .inputsTextInElement(st.pages().mail().composePopup().expandedPopup().bodyInput(), text)
            .shouldSee(st.pages().mail().composePopup().expandedPopup().templatesNotif());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).withUrlPath(DRAFT).run();
    }

    @Test
    @Title("Должен быть залипающий тулбар и пейджер")
    @TestCaseId("3031")
    public void shouldSeeStickyToolbarAndPager() {
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 6);
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().setsWindowSize(1600, 500);
            st.user().messagesSteps().scrollDownPage();
            st.user().defaultSteps().shouldSee(
                st.pages().mail().home().stickyToolBar(),
                st.pages().mail().home().inboxPager()
            );
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Открываем залипающий тулбар в спаме")
    @TestCaseId("3032")
    public void shouldSeeStickyToolbarInSpam() {
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 2);
        List<Message> messages = stepsProd.user().apiMessagesSteps().getAllMessagesInFolder(INBOX);
        stepsProd.user().apiMessagesSteps().moveMessagesToSpam(messages.get(0), messages.get(1), messages.get(2));
        Consumer<InitStepsRule> actions = st -> {
            st.user().defaultSteps().setsWindowSize(1600, 400);
            st.user().leftColumnSteps().opensSpamFolder();
            st.user().messagesSteps().scrollDownPage();
            st.user().defaultSteps().shouldSee(st.pages().mail().home().stickyToolBar());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть серые иконки в тулбаре")
    @TestCaseId("4206")
    public void shouldSeeDisabledIconsOnToolbar() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(st.pages().mail().home().disabledToolbar());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть цветные иконки в тулбаре")
    @TestCaseId("4206")
    public void shouldSeeEnabledIconsOnToolbar() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().displayedMessages().list().get(0).checkBox())
                .shouldSee(st.pages().mail().home().toolbar());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть попап настроек пользовательских кнопок тулбара")
    @TestCaseId("4206")
    public void shouldSeeConfigureCustomButtonsPopupOnToolbar() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().onMouseHoverAndClick(st.pages().mail().home().toolbar().configureCustomButtons())
                .shouldSee(st.pages().mail().customButtons().overview());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть цветные иконки настроенных пользовательских кнопок")
    @TestCaseId("4206")
    public void shouldSeeEnabledCustomButtonOnToolbar() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().apiSettingsSteps().callWithListAndParams(
                "Убираем все пользовательские кнопки",
                of(CUSTOM_BUTTONS, EMPTY_STR)
            );
            st.user().defaultSteps().refreshPage()
                .onMouseHoverAndClick(st.pages().mail().home().toolbar().configureCustomButtons())
                .clicksOn(st.pages().mail().customButtons().overview().label())
                .selectsOption(
                    st.pages().mail().customButtons().configureLabelButton().labelSelect(),
                    IMPORTANT_LABEL_NAME_RU
                )
                .clicksOn(st.pages().mail().customButtons().configureFoldersButton().saveButton())
                .waitInSeconds(2)
                .clicksOn(st.pages().mail().customButtons().overview().saveChangesButton());
            st.user().messagesSteps().clicksOnMessageCheckBoxByNumber(0);
            st.user().defaultSteps().shouldSee(st.pages().mail().home().toolbar().autoLabelButtonIcon());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).runSequentially();
    }

    @Test
    @Title("Должны видеть кнопку «Создать шаблон» в тулбаре для черновиков")
    @TestCaseId("4221")
    public void shouldSeeAddTemplateButtonOnToolbar() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().mail().home().foldersNavigation().draftFolder())
                .shouldSee(st.pages().mail().home().toolbar().createTemplateButton());

        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }
}
