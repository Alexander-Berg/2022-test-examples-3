package ru.yandex.autotests.innerpochta.tests.screentests.MessageList;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INFOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.MARKLABEL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на попапы в списке писем")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.POPUPS)
@RunWith(DataProviderRunner.class)
public class PopupsInMsgListScreenTest {

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule accLock = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        stepsProd.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), getRandomString(), "");
        stepsTest.user().apiMessagesSteps().markAllMsgRead();
        stepsProd.user().apiLabelsSteps().addNewLabel(getRandomString(), LABELS_PARAM_GREEN_COLOR);
        stepsProd.user().apiFoldersSteps().createNewSubFolder(
            getRandomName(),
            stepsProd.user().apiFoldersSteps().createNewFolder(getRandomName())
        );
    }

    @Test
    @Title("Должны увидеть попап действий с письмом")
    @TestCaseId("219")
    public void shouldSeePopupActionsForMessages() {
        Consumer<InitStepsRule> actions = this::openActionForMsgesPopup;

        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть попап меток для письма без меток")
    @TestCaseId("876")
    public void shouldSeeEmptyLabelsPopup() {
        Consumer<InitStepsRule> actions = steps -> {
            openActionForMsgesPopup(steps);
            steps.user().defaultSteps()
                .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), MARKLABEL.btn())
                .shouldSee(steps.pages().touch().messageList().popup());
        };
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть попап меток с проставленными метками")
    @TestCaseId("877")
    public void shouldSeeChangedPopupLabel() {
        Consumer<InitStepsRule> actions = steps -> {
            openActionForMsgesPopup(steps);
            steps.user().defaultSteps()
                .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), MARKLABEL.btn());
            clickOnTwoLabels(steps);
            steps.user().defaultSteps()
                .shouldSeeElementsCount(steps.pages().touch().messageList().popup().tick(), 2);
        };
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть попап меток с отменёнными проставленными метками")
    @TestCaseId("834")
    public void shouldSeeCanceledChangesPopupLabel() {
        Consumer<InitStepsRule> actions = steps -> {
            openActionForMsgesPopup(steps);
            steps.user().defaultSteps()
                .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), MARKLABEL.btn());
            clickOnTwoLabels(steps);
            steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().popup().activeDone());
            clickOnTwoLabels(steps);
            steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageList().popup().activeDone())
                .shouldSee(steps.pages().touch().messageList().popup().done());
        };
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть попап выбора папки")
    @TestCaseId("285")
    public void shouldSeePopupFolders() {
        Consumer<InitStepsRule> actions = steps -> {
            openActionForMsgesPopup(steps);
            steps.user().defaultSteps()
                .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), INFOLDER.btn())
                .shouldSee(steps.pages().touch().messageList().folderPopup())
                .clicksOn(steps.pages().touch().messageList().folderPopup().toggler())
                .shouldSee(steps.pages().touch().messageList().folderPopup().expandedFolders());
        };
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("В папках «Удаленные» и «Спам» по нажатию на кнопку «Очистить папку» появляется попап")
    @TestCaseId("403")
    @DataProvider({SPAM, TRASH})
    public void shouldSeeClearFolderPopup(String folder) {
        Consumer<InitStepsRule> actions = steps ->
            steps.user().defaultSteps()
                .shouldSee(steps.pages().touch().messageList().headerBlock())
                .clicksOn(steps.pages().touch().messageList().clearFolderButton())
                .shouldSee(steps.pages().touch().messageList().clearFolderPopup());

        stepsProd.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, folder);
        String fid = stepsProd.user().apiFoldersSteps().getFolderBySymbol(folder).getFid();
        parallelRun.withActions(actions).withAcc(accLock.firstAcc()).withUrlPath(FOLDER_ID.makeTouchUrlPart(fid)).run();
    }

    @Step("Открываем попап действий с письмом из свайп-меню")
    private void openActionForMsgesPopup(InitStepsRule st) {
        st.user().touchSteps()
            .rightSwipe(st.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        st.user().defaultSteps()
            .clicksOn(st.pages().touch().messageList().messageBlock().swipeFirstBtnDraft())
            .shouldSee(st.pages().touch().messageList().popup());
    }

    @Step("Кликаем на метку важности и первую пользовательскую метку в попапе")
    private void clickOnTwoLabels(InitStepsRule st) {
        st.user().defaultSteps().shouldSee(st.pages().touch().messageList().popup())
            .clicksOn(st.pages().touch().messageList().popup().labels().get(1))
            .shouldSeeElementsCount(st.pages().touch().messageList().popup().tick(), 1)
            .clicksOn(st.pages().touch().messageList().popup().labels().get(0)
            );
    }
}
