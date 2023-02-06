package ru.yandex.autotests.innerpochta.tests.autotests;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DISK_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.MailConst.EXCEL_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.TXT_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WORD_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WRONG_EXTENSION;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Вьюер")
@Features(FeaturesConst.ATTACHES)
@Stories(FeaturesConst.VIEWER)
public class ViewerTest {

    private static final String VIEWER_HEADER = " из ";

    private int numOfAttachments;

    //TODO: можно брать юзера без тега, когда удалятся тесты на старый композ
    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount(DISK_USER_TAG));
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        steps.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            accLock.firstAcc().getSelfEmail(),
            Utils.getRandomString(),
            Utils.getRandomString(),
            EXCEL_ATTACHMENT,
            PDF_ATTACHMENT,
            WORD_ATTACHMENT,
            TXT_ATTACHMENT,
            WRONG_EXTENSION
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Выходим из вьюера в просмотре письма")
    @TestCaseId("731")
    public void shouldCloseViewer() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().messageBlock().subject())
            .shouldSee(steps.pages().touch().messageView().attachmentsBlock())
            .clicksOn(steps.pages().touch().messageView().attachments().get(0))
            .clicksOn(steps.pages().touch().messageView().viewer().viewerClose())
            .shouldSee(steps.pages().touch().messageView().attachmentsBlock());
    }

    @Test
    @Title("Выходим из вьюера в списке писем")
    @TestCaseId("731")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("QUINN-7520")
    public void shouldCloseViewInMessageList() {
        steps.user().defaultSteps()
            .shouldSee(
                steps.pages().touch().messageList().messageBlock().attachmentsInMessageList().waitUntil(not(empty()))
                    .get(0)
            )
            .clicksOn(steps.pages().touch().messageView().viewer().viewerClose())
            .shouldSee(steps.pages().touch().messageList().messageBlock().attachmentsInMessageList());
    }

    @Test
    @Title("Пролистываем аттачи во вьюере")
    @TestCaseId("452")
    public void shouldListAttachmentsInViewer() {
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().touch().messageList().messageBlock().attachmentsInMessageList().waitUntil(not(empty()))
                    .get(0)
            );
        numOfAttachments = steps.pages().touch().messageList().messageBlock().attachmentsInMessageList().size();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().viewer());
        for (int i = 1; i < numOfAttachments; i++) {
            listAttachNext(i - 1, i + 1);
        }
        listAttachNext(numOfAttachments - 1, numOfAttachments);
        for (int i = numOfAttachments; i > 1; i--) {
            listAttachPrev(i - 1, i - 1);
        }
        listAttachPrev(0, 1);
    }

    @Step("Листаем аттачи вперёд")
    private void listAttachNext(int num, int numInHeader) {
        steps.user().touchSteps().rightSwipe(
            steps.pages().touch().messageView().viewer().attach().get(num)
        );
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().touch().messageView().viewer().attachCounter(),
            numInHeader + VIEWER_HEADER + numOfAttachments
        );
    }

    @Step("Листаем аттачи назад")
    private void listAttachPrev(int num, int numInHeader) {
        steps.user().touchSteps().leftSwipe(
            steps.pages().touch().messageView().viewer().attach().get(num)
        );
        steps.user().defaultSteps().shouldSeeThatElementHasText(
            steps.pages().touch().messageView().viewer().attachCounter(),
            numInHeader + VIEWER_HEADER + numOfAttachments
        );
    }
}
