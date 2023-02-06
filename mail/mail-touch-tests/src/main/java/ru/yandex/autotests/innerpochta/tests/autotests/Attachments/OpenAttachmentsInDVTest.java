package ru.yandex.autotests.innerpochta.tests.autotests.Attachments;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
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

import static ru.yandex.qatools.htmlelements.matchers.common.HasTextMatcher.hasText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.EXCEL_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на открытие аттачей в docviewer")
@Features(FeaturesConst.ATTACHES)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class OpenAttachmentsInDVTest {

    private static final String DOCVIEWER = "docviewer.yandex.ru/view/";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
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
            PDF_ATTACHMENT
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Открываем аттач по тапу в доквьюере из просмотра письма")
    @TestCaseId("740")
    @DataProvider({"0", "1"})
    public void shouldOpenAttachments(int num) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            MSG_FRAGMENT.makeTouchUrlPart(steps.user().apiMessagesSteps().getAllMessages().get(0).getMid())
        )
            .clicksOn(steps.pages().touch().messageView().attachments().waitUntil(not(empty())).get(num))
            .shouldSee(steps.pages().touch().messageView().viewer().viewerBody())
            .clicksOn(steps.pages().touch().messageView().viewer().openFileBtn().get(num))
            .switchOnJustOpenedWindow().shouldBeOnUrl(containsString(DOCVIEWER));
    }

    @Test
    @Title("Открываем аттач в доквьюере в списке писем")
    @TestCaseId("419")
    @DataProvider({"0", "1"})
    public void shouldOpenAttachmentsFromMsgListInDV(int num) {
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        clickAttachAndOpenItFromViewer(num);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().viewer().openFileBtn().get(num))
            .switchOnJustOpenedWindow().shouldBeOnUrl(containsString(DOCVIEWER));
    }

    @Test
    @Title("Открываем почтовый аттач в доквьюере по тапу из композа")
    @TestCaseId("652")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("QUINN-5282")
    public void shouldOpenMailAttachInCompose() {
        addMailAttachWithExtensionFromMail("pdf");
        steps.user().defaultSteps().shouldSee(steps.pages().touch().composeIframe().attachments().uploadedAttachment())
            .clicksOn(steps.pages().touch().composeIframe().attachments().attachments().waitUntil(not(empty())).get(0))
            .shouldSee(steps.pages().touch().messageView().viewer().viewerBody())
            .clicksOn(steps.pages().touch().messageView().viewer().openFileBtn().get(0))
            .switchOnJustOpenedWindow().shouldBeOnUrl(containsString(DOCVIEWER));
    }

    @Step("Добавляем в композ аттач из почты c нужным расширением")
    private void addMailAttachWithExtensionFromMail(String text) {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().header().clip())
            .shouldSee(steps.pages().touch().composeIframe().attachFilesPopup())
            .clicksOn(steps.pages().touch().composeIframe().attachFilesPopup().fromMail())
            .clicksOn(steps.pages().touch().composeIframe().diskAttachmentsPage().attachments().get(1))
            .shouldSee(steps.pages().touch().composeIframe().diskAttachmentsPage().attachments());
        int size = steps.pages().touch().composeIframe().diskAttachmentsPage().attachments().size();
        for (int i = 0; i <= size; i++) {
            if (hasText(CoreMatchers.containsString(text))
                .matches(steps.pages().touch().composeIframe().diskAttachmentsPage().attachments().get(i))
                ) {
                steps.user().defaultSteps()
                    .turnTrue(steps.pages().touch().composeIframe().diskAttachmentsPage().checkbox().get(i));
                break;
            } else
                steps.user().defaultSteps()
                    .scrollTo(steps.pages().touch().composeIframe().diskAttachmentsPage().attachments().get(i + 2));
        }
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().diskAttachmentsPage().attachBtn());
    }

    @Step("Кликаем в аттач и открываем его во вьюере")
    private void clickAttachAndOpenItFromViewer(int numOfAttach) {
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0)
                    .attachmentsInMessageList().waitUntil(not(empty())).get(numOfAttach)
            )
            .shouldSee(steps.pages().touch().messageView().viewer().viewerBody());
    }
}
