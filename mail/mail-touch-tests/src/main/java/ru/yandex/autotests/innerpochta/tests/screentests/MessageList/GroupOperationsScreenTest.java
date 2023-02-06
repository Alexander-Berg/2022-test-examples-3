package ru.yandex.autotests.innerpochta.tests.screentests.MessageList;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
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

import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.SPAM_FOLDER;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ARCHIVE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;

/**
 * @author puffyfloof
 */

@Aqua.Test
@Title("Групповые операции в списке писем")
@Features(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.TOOLBAR)
public class GroupOperationsScreenTest {

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        stepsProd.user().apiFoldersSteps().createArchiveFolder();
        stepsProd.user().apiMessagesSteps().sendMail(acc.firstAcc(), Utils.getRandomName(), "");
        stepsProd.user().apiMessagesSteps().moveAllMessagesFromFolderToFolder(INBOX, ARCHIVE)
            .sendMail(acc.firstAcc().getSelfEmail(), Utils.getRandomString(), "");
        stepsProd.user().apiMessagesSteps().moveMessagesToSpam(
            stepsProd.user().apiMessagesSteps().sendMail(acc.firstAcc(), Utils.getRandomString(), "")
        );
        stepsProd.user().apiLabelsSteps().markImportant(
            stepsProd.user().apiMessagesSteps().sendMail(acc.firstAcc(), Utils.getRandomString(), "")
        );
        stepsProd.user().apiMessagesSteps().markAllMsgRead();
    }

    @Test
    @Title("По клику на аватарку должны видеть тулбар")
    @TestCaseId("17")
    public void shouldOpenGroupOperationsToolbar() {
        Consumer<InitStepsRule> actions = steps ->
            openGroupOperationsToolbar(steps, 0);


        parallelRun.withAcc(acc.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("Должны выбирать все письма кликом по Выделить всё")
    @TestCaseId("390")
    public void shouldSelectAllMessages() {
        Consumer<InitStepsRule> actions = steps -> {
            openGroupOperationsToolbar(steps, 0);
            steps.user().defaultSteps()
                .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarHeader().selectAll())
                .waitInSeconds(1);
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).run();
    }

    @Test
    @Title("В папке «Архив» кнопка «Архив» должна быть неактивна (или отсутствовать)")
    @TestCaseId("354")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeInactiveArchiveButton() {
        Consumer<InitStepsRule> actions = steps -> {
            openGroupOperationsToolbar(steps, 0);
            steps.user().defaultSteps()
                .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().more())
                .shouldSee(steps.pages().touch().messageList().groupOperationsToast().folder());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(
            FOLDER_ID.makeTouchUrlPart(stepsProd.user().apiFoldersSteps().getFolderBySymbol(ARCHIVE).getFid())
        ).run();
    }

    @Test
    @Title("В папке «Архив» кнопка «Архив» должна быть неактивна")
    @TestCaseId("354")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeInactiveArchiveButtonTablet() {
        Consumer<InitStepsRule> actions = steps -> {
            openGroupOperationsToolbar(steps, 0);
            steps.user().defaultSteps()
                .shouldSee(steps.pages().touch().messageView().groupOperationsToolbarTablet().folder());
        };
        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(
            FOLDER_ID.makeTouchUrlPart(stepsProd.user().apiFoldersSteps().getFolderBySymbol(ARCHIVE).getFid())
        ).run();
    }

    @Test
    @Title("В папке «Спам» кнопка «Спам» должна меняться на «Не спам»")
    @TestCaseId("402")
    public void shouldSeeUnspamButton() {
        Consumer<InitStepsRule> actions = steps ->
            openGroupOperationsToolbar(steps, 0);

        parallelRun.withAcc(acc.firstAcc()).withActions(actions).withUrlPath(FOLDER_ID.makeTouchUrlPart(SPAM_FOLDER))
            .run();
    }

    @Test
    @Title("Должны увидеть попап меток с метками, стоящими не на всех письмах")
    @TestCaseId("284")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeLabelPopupOnPartiallyLabeledMsgs() {
        Consumer<InitStepsRule> actions = steps -> {
            openGroupOperationsToolbar(steps, 0);
            steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().messages().get(1).avatar())
                .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().more())
                .clicksOn(steps.pages().touch().messageList().groupOperationsToast().label())
                .shouldSee(steps.pages().touch().messageList().popup().halfactiveTick());
        };
        parallelRun.withActions(actions).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть попап меток с метками, стоящими не на всех письмах")
    @TestCaseId("284")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeLabelPopupOnPartiallyLabeledMsgsTablet() {
        Consumer<InitStepsRule> actions = steps -> {
            openGroupOperationsToolbar(steps, 0);
            steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().messages().get(1).avatar())
                .clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().label())
                .shouldSeeElementsCount(steps.pages().touch().messageList().popup().halfactiveTick(), 1);
        };
        parallelRun.withActions(actions).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Должны выделять все новые подгруженные письма")
    @TestCaseId("391")
    public void shouldSelectAllLoadedMessages() {
        Consumer<InitStepsRule> actions = steps -> {
            openGroupOperationsToolbar(steps, 0);
            steps.user().defaultSteps()
                .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarHeader().selectAll());
            steps.user().touchSteps().scrollMsgListDown();
        };
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(acc.firstAcc(), 11)
            .markAllMsgRead();
        parallelRun.withActions(actions).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Должны увидеть тост с дополнительными действиями")
    @TestCaseId("1373")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeGroupOperationToast() {
        Consumer<InitStepsRule> actions = steps -> {
            openGroupOperationsToolbar(steps, 0);
            steps.user().defaultSteps()
                .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().more())
                .shouldSee(steps.pages().touch().messageList().groupOperationsToast());
        };
        parallelRun.withActions(actions).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Должны сохранить скролл после действия из меню групповых операций")
    @TestCaseId("1475")
    public void shouldSaveScrollAfterAction() {
        Consumer<InitStepsRule> actions = steps -> {
            int msgNum = steps.user().touchSteps().scrollMsgListDown();
            openGroupOperationsToolbar(steps, msgNum);
            steps.user().defaultSteps()
                .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().delete())
                .waitInSeconds(2);
        };
        stepsProd.user().apiMessagesSteps().sendCoupleMessages(acc.firstAcc(), 11)
            .markAllMsgRead();
        parallelRun.withActions(actions).withAcc(acc.firstAcc()).run();
    }

    @Step("[Телефон]Открываем тулбар групповых операций")
    private void openGroupOperationsToolbar(InitStepsRule steps, int num) {
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().messageBlock())
            .clicksOn(steps.pages().touch().messageList().messages().get(num).avatar())
            .shouldSee(steps.pages().touch().messageList().groupOperationsToolbarHeader());
    }
}
