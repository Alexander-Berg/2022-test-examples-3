package ru.yandex.autotests.innerpochta.tests.screentests.MessageList;

import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Set;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static org.openqa.selenium.By.cssSelector;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.LABEL_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.IMPORTANT_LABEL;
import static ru.yandex.autotests.innerpochta.touch.data.Scrips.PTR_SCRIPT;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.MARKLABEL;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.FREEZE_DONE_SCRIPT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;

/**
 * @author puffyfloof
 */

@Aqua.Test
@Title("Общие тесты на список писем")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class MessageListScreenTest {

    private static final String LONG_NAME = "Very looooong name for a custom folder";
    private static final Set<By> IGNORE_THIS = Sets.newHashSet(cssSelector(".messages__empty__link"));

    private String subj;

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
        String longText = getRandomString() + getRandomString() + getRandomString() + getRandomString();
        Message msg = stepsTest.user().apiMessagesSteps().sendMailWithNoSave(
            accLock.firstAcc(),
            longText,
            longText + longText
        );
        stepsTest.user().apiMessagesSteps().sendThread(accLock.firstAcc(), longText, 2);
        stepsTest.user().apiMessagesSteps().markAllMsgRead();
        stepsTest.user().apiLabelsSteps().markImportant(msg)
            .markWithLabel(
                msg,
                stepsProd.user().apiLabelsSteps().addNewLabel(longText, LABELS_PARAM_GREEN_COLOR)
            );
    }

    @Test
    @Title("Должны видеть все элементы инбокса")
    @TestCaseId("19")
    public void shouldSeeAllMessageElements() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps()
                .clicksOn(st.pages().touch().messageList().messages().waitUntil(not(empty())).get(1).labelMore())
                .shouldSee(st.pages().touch().messageList().messages().get(1).label());

        parallelRun.withActions(act).withAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("В важных письма не должны быть красными")
    @TestCaseId("554")
    public void shouldNotSeeRedMsgesInImportant() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageList().headerBlock());

        parallelRun.withActions(act).withAcc(accLock.firstAcc()).withUrlPath(LABEL_ID.makeTouchUrlPart(IMPORTANT_LABEL)).run();
    }

    @Test
    @Title("Вёрстка области ptr")
    @TestCaseId("223")
    @DoTestOnlyForEnvironment("iOS")
    public void shouldSeePtr() {
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps().shouldSee(st.pages().touch().messageList().headerBlock())
                .executesJavaScript(PTR_SCRIPT)
                .shouldSee(st.pages().touch().messageList().ptr());

        parallelRun.withActions(act).withAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("Скрол списка писем сохраняется после просмотра письма")
    @TestCaseId("327")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSaveScrollAfterOpenMsg() {
        Consumer<InitStepsRule> act = st -> {
            int msgNum = st.user().touchSteps().scrollMsgListDown();
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().messages().get(msgNum - 1))
                .shouldSee(st.pages().touch().messageView().toolbar())
                .clicksOn(st.pages().touch().messageView().header().backToListBtn())
                .shouldSee(st.pages().touch().messageList().headerBlock());
        };
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), 12)
            .markAllMsgRead();
        parallelRun.withActions(act).withAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("Скрол списка писем сохраняется после просмотра письма")
    @TestCaseId("327")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSaveScrollAfterOpenMsgTablet() {
        Consumer<InitStepsRule> act = st -> {
            int msgNum = st.user().touchSteps().scrollMsgListDown();
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().messages().get(msgNum - 1))
                .shouldSee(st.pages().touch().messageView().toolbar());
        };
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), 12)
            .markAllMsgRead();
        parallelRun.withActions(act).withAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("Должны видеть статуслайн о проставлении метки")
    @TestCaseId("151")
    public void shouldSeeStatuslineWhenLabelMsg() {
        Consumer<InitStepsRule> act = st -> {
            openActionForMsgesPopup(st);
            st.user().defaultSteps()
                .clicksOnElementWithText(st.pages().touch().messageView().btnsList(), MARKLABEL.btn());
            clickOnTwoLabels(st);
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().popup().activeDone())
                .shouldSee(st.pages().touch().messageList().statusLineInfo())
                .executesJavaScript(FREEZE_DONE_SCRIPT)
                .shouldSee(st.pages().touch().messageList().statusLineInfo());
        };
        parallelRun.withActions(act).withAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("Переход между письмом и тредом не сбрасывает скролл списка писем")
    @TestCaseId("646")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSaveScrollAfterOpenDiffMsges() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().scrollMsgListDown();
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().messages().get(8))
                .shouldSee(st.pages().touch().messageView().toolbar())
                .clicksOn(st.pages().touch().messageView().header().backToListBtn())
                .shouldSee(st.pages().touch().messageList().headerBlock())
                .clicksOnElementWithText(st.pages().touch().messageList().subjectList(), subj)
                .clicksOn(st.pages().touch().messageView().header().backToListBtn())
                .shouldSee(st.pages().touch().messageList().headerBlock());
        };
        subj = Utils.getRandomName();
        stepsProd.user().apiMessagesSteps().sendThread(accLock.firstAcc(), subj, 2);
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), 12)
            .markAllMsgRead();
        parallelRun.withActions(act).withAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("Переход между письмом и тредом не сбрасывает скролл списка писем")
    @TestCaseId("646")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSaveScrollAfterOpenDiffMsgesTablet() {
        Consumer<InitStepsRule> act = st -> {
            st.user().touchSteps().scrollMsgListDown();
            st.user().defaultSteps().scrollTo(st.pages().touch().messageList().messages().get(12))
                .clicksOn(st.pages().touch().messageList().messages().get(8))
                .shouldSee(st.pages().touch().messageView().toolbar())
                .clicksOnElementWithText(st.pages().touch().messageList().subjectList(), subj)
                .shouldSee(st.pages().touch().messageView().threadHeader())
                .shouldSee(st.pages().touch().messageList().headerBlock());
        };
        subj = Utils.getRandomName();
        stepsProd.user().apiMessagesSteps().sendThread(accLock.firstAcc(), subj, 2);
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), 12)
            .markAllMsgRead();
        parallelRun.withActions(act).withAcc(accLock.firstAcc()).run();
    }

    @Test
    @Title("Все элементы помещаются в шапку при длинном имени папки")
    @TestCaseId("1327")
    public void shouldSeeHeaderWithLongFolderName() {
        Folder folder = stepsProd.user().apiFoldersSteps().createNewFolder(LONG_NAME);
        Consumer<InitStepsRule> act = st ->
            st.user().defaultSteps()
                .opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(folder.getFid()))
                .shouldSee(st.pages().touch().messageList().headerBlock().compose());

        parallelRun.withActions(act).withAcc(accLock.firstAcc()).withIgnoredElements(IGNORE_THIS).run();
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
