package ru.yandex.autotests.innerpochta.tests.screentests;

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
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.MARKUNREAD;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.REPLY_ALL;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ARCHIVE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SPAM;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author puffyfloof
 */

@Aqua.Test
@Title("Скриночные тесты на просмотр треда")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.THREAD)
@RunWith(DataProviderRunner.class)
public class ThreadsScreenTest {

    private String subj;

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule accLock = rules.getLock();
    private RunAndCompare parallelRun = RunAndCompare.runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        subj = Utils.getRandomName();
        stepsProd.user().apiFoldersSteps().createArchiveFolder();
        stepsProd.user().apiMessagesSteps().sendThread(accLock.firstAcc(), subj, 3);
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
    }

    @Test
    @Title("Черновик в треде должен быть серым")
    @TestCaseId("418")
    public void shouldSeeGrayDraftInThread() {
        Consumer<InitStepsRule> actions = steps ->
            steps.user().defaultSteps()
                .shouldSee(steps.pages().touch().messageList().headerBlock())
                .clicksOn(steps.pages().touch().messageList().messages().get(0))
                .clicksOn(steps.pages().touch().messageView().msgInThread().waitUntil(not(empty())).get(0))
                .shouldSee(steps.pages().touch().messageView().draft());

        stepsProd.user().apiMessagesSteps().markAllMsgRead()
            .prepareDraftToThread("", subj, "");
        parallelRun.withAcc(accLock.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("Не должно быть тредного режима в Удалённых/Спаме/Архиве")
    @TestCaseId("253")
    @DataProvider({TRASH, SPAM, ARCHIVE})
    public void shouldNotSeeThreadInFolders(String folder) {
        Consumer<InitStepsRule> actions = steps ->
            steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().messageBlock())
                .shouldNotSee(steps.pages().touch().messageList().messageBlock().threadCounter());

        stepsTest.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, folder);
        parallelRun.withAcc(accLock.firstAcc()).withActions(actions).withUrlPath(
            FOLDER_ID.makeTouchUrlPart(stepsTest.user().apiFoldersSteps().getFolderBySymbol(folder).getFid())
        ).run();
    }

    @Test
    @Title("Цитирование при ответе на тред")
    @TestCaseId("245")
    public void shouldSeeQuoteThreadInCompose() {
        Consumer<InitStepsRule> actions = steps -> {
            steps.user().defaultSteps()
                .clicksOn(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0))
                .shouldSee(steps.pages().touch().messageView().threadHeader())
                .clicksOn(steps.pages().touch().messageView().moreBtn())
                .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), REPLY_ALL.btn());
            steps.user().touchSteps().switchToComposeIframe();
            steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().inputBody());
        };
        parallelRun.withAcc(accLock.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("Должны отметить одно из писем треда непрочитанным")
    @TestCaseId("279")
    public void shouldMarkOneThreadMsgUnread() {
        Consumer<InitStepsRule> actions = steps -> steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0))
            .shouldSee(steps.pages().touch().messageView().threadHeader())
            .clicksOn(steps.pages().touch().messageView().moreBtn())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), MARKUNREAD.btn())
            .shouldSee(steps.pages().touch().messageView().recentToggler());

        parallelRun.withAcc(accLock.firstAcc()).withActions(actions).run();
    }
}
