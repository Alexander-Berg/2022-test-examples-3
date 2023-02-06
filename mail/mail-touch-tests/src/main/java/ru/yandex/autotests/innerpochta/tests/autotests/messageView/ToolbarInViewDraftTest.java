package ru.yandex.autotests.innerpochta.tests.autotests.messageView;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.DELETE;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.WRITE;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TEMPLATE;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на тулбар в просмотре черновика/шаблона")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.TOOLBAR)
public class ToolbarInViewDraftTest {

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    private String subj;

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prep() {
        subj = getRandomString();
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, "");
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Перейти в композ из тулбара черновика")
    @TestCaseId("1073")
    public void shouldAppendDraftInThread() {
        steps.user().apiMessagesSteps().prepareDraftToThread("", subj, "");
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .clicksOn(steps.pages().touch().messageView().toolbar())
            .clicksOn(steps.pages().touch().messageView().toolbar().openDraft());
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps().shouldHasValue(steps.pages().touch().composeIframe().inputSubject(), subj);
    }

    @Test
    @Title("Перейти в композ из тулбара шаблона")
    @TestCaseId("1073")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldAppendTemplate() {
        steps.user().apiMessagesSteps().createTemplateMessage(accLock.firstAcc());
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            FOLDER_ID.makeTouchUrlPart(steps.user().apiFoldersSteps().getFolderBySymbol(TEMPLATE).getFid()))
            .clicksOn(steps.pages().touch().messageView().toolbar().openDraft());
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().touch().composeIframe().yabble(),
            accLock.firstAcc().getSelfEmail()
        );
    }

    @Test
    @Title("Должны удалить развернутый в треде черновик из тулбара")
    @TestCaseId("1074")
    public void shouldDeleteDraftInThread() {
        steps.user().apiMessagesSteps().prepareDraftToThread("", subj, "");
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .clicksOn(steps.pages().touch().messageView().toolbar())
            .clicksOn(steps.pages().touch().messageView().toolbar().delete())
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 1);
    }

    @Test
    @Title("Должны перейти в композ из меню в нижнем тулбаре черновика")
    @TestCaseId("1084")
    @DoTestOnlyForEnvironment("Android")
    public void shouldAppendDraftFromMore() {
        steps.user().apiMessagesSteps().prepareDraftToThread("", subj, "");
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .clicksOn(steps.pages().touch().messageView().toolbar())
            .clicksOn(steps.pages().touch().messageView().moreBtnLow())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), WRITE.btn());
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps().shouldHasValue(steps.pages().touch().composeIframe().inputSubject(), subj);
    }

    @Test
    @Title("Должны удалить черновик из меню в нижнем тулбаре")
    @TestCaseId("1084")
    @DoTestOnlyForEnvironment("Android")
    public void shouldDeleteDraftFromMore() {
        steps.user().apiMessagesSteps().prepareDraftToThread("", subj, "");
        steps.user().defaultSteps().opensDefaultUrl()
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .clicksOn(steps.pages().touch().messageView().toolbar())
            .clicksOn(steps.pages().touch().messageView().moreBtnLow())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), DELETE.btn())
            .shouldSeeElementsCount(steps.pages().touch().messageView().msgInThread(), 1);
    }
}
