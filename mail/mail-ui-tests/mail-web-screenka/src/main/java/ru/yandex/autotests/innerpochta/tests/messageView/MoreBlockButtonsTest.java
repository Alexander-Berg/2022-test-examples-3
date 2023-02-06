package ru.yandex.autotests.innerpochta.tests.messageView;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.ScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static com.google.common.collect.ImmutableSortedMap.of;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ScreenRulesManager.screenRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.FORWARD_PREFIX;
import static ru.yandex.autotests.innerpochta.util.MailConst.REPLY_PREFIX;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_OFF;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_TRANSLATE;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Кнопки за выпадушкой «Ещё» в тулбаре")
@Features(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Tag(FeaturesConst.MESSAGE_COMPACT_VIEW)
@Stories(FeaturesConst.TOOLBAR)
public class MoreBlockButtonsTest {

    private static final String TEST_NAME = "test";
    private static final String URL_MSG_EML = "/yandex_email.eml";
    private static final String RECEIVER_EMAIL = "yandex-team-31613.23887@yandex.ru";

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
            "Выключаем бажный переводчик, включаем открытие письма в списке писем",
            of(
                SETTINGS_PARAM_TRANSLATE, STATUS_OFF,
                SETTINGS_OPEN_MSG_LIST, STATUS_TRUE
            )
        );
        stepsProd.user().apiFoldersSteps().createNewFolder(Utils.getRandomName());
        stepsProd.user().apiLabelsSteps().addNewLabel(Utils.getRandomName(), LABELS_PARAM_GREEN_COLOR);
        stepsProd.user().apiMessagesSteps().addCcEmails(RECEIVER_EMAIL)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), "subject", Utils.getRandomName());
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
    }

    @Test
    @Title("Нажимаем на кнопку «Переслать»")
    @TestCaseId("2707")
    public void shouldSeeComposeWithForwardMsg() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().contentToolbarBlock().forwardButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .shouldContainValue(st.pages().mail().composePopup().expandedPopup().sbjInput(), FORWARD_PREFIX);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем на кнопку «Создать правило»")
    @TestCaseId("2708")
    public void shouldSeeCorrectFieldsInCreateFilter() {
        Consumer<InitStepsRule> actions = st -> {
            openMoreBlock(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().miscField().createFilter())
                .shouldBeOnUrlWith(QuickFragments.SETTINGS_FILTERS_CREATE)
                .onMouseHover(st.pages().mail().createFilters().setupFiltersCreate().submitFilterButton());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должна показаться нотификация об ошибке")
    @TestCaseId("2709")
    public void shouldSeeInvalidNameNotify() {
        Consumer<InitStepsRule> actions = st -> {
            openMoreBlock(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().miscField().folder())
                .onMouseHoverAndClick(st.pages().mail().msgView().moveMessageDropdownMenu().createNewFolder())
                .shouldSee(st.pages().mail().home().createFolderPopup())
                .clicksOn(st.pages().mail().home().createFolderPopup().create())
                .shouldSee(st.pages().mail().home().createFolderPopup().invalidNameNotify());
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны быть папки из фильтра")
    @TestCaseId("2710")
    public void shouldSeeCorrectFolder() {
        stepsProd.user().apiFoldersSteps().createNewFolder(TEST_NAME);
        Consumer<InitStepsRule> actions = st -> {
            openMoreBlock(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().miscField().folder())
                .inputsTextInElement(st.pages().mail().msgView().moveMessageDropdownMenu().input(), TEST_NAME);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Должны быть метки из фильтра")
    @TestCaseId("2711")
    public void shouldSeeCorrectLabel() {
        stepsProd.user().apiLabelsSteps().addNewLabel(TEST_NAME, LABELS_PARAM_GREEN_COLOR);
        Consumer<InitStepsRule> actions = st -> {
            openMoreBlock(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().miscField().label())
                .inputsTextInElement(st.pages().mail().msgView().labelsDropdownMenu().searchField(), TEST_NAME);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем на кнопку «Ответить всем»")
    @TestCaseId("2712")
    public void shouldBeInComposeWithQuoting() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().messagesSteps().clicksOnMessageByNumber(0);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().contentToolbarBlock().replyToAllBtn())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .shouldContainValue(st.pages().mail().composePopup().expandedPopup().sbjInput(), REPLY_PREFIX);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем на кнопку «Ответить»")
    @TestCaseId("2713")
    public void shouldBeInComposeWithNotify() {
        Consumer<InitStepsRule> actions = st -> {
            openMoreBlock(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().miscField().replyButton())
                .shouldSee(st.pages().mail().composePopup().expandedPopup())
                .shouldContainValue(st.pages().mail().composePopup().expandedPopup().sbjInput(), REPLY_PREFIX);
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Test
    @Title("Нажимаем на кнопку «Свойства письма»")
    @TestCaseId("929")
    public void shouldSeeMsgHeaders() {
        Consumer<InitStepsRule> actions = st -> {
            openMoreBlock(st);
            st.user().defaultSteps().clicksOn(st.pages().mail().msgView().miscField().messageInfo())
                .switchOnJustOpenedWindow()
                .shouldBeOnUrl(containsString(URL_MSG_EML));
        };
        parallelRun.withActions(actions).withAcc(lock.firstAcc()).run();
    }

    @Step("Открываем выпадушку «Ещё» в просмотре письма")
    private void openMoreBlock(InitStepsRule st) {
        st.user().messagesSteps().clicksOnMessageByNumber(0);
        st.user().defaultSteps().clicksOn(st.pages().mail().msgView().contentToolbarBlock().moreBtn())
            .shouldSee(st.pages().mail().msgView().miscField());
    }
}
