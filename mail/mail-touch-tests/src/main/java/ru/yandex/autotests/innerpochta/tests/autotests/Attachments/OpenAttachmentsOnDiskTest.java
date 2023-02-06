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
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.qatools.htmlelements.matchers.common.HasTextMatcher.hasText;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE_MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MSG_FRAGMENT;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DISK_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на открытие аттачей на диске")
@Features(FeaturesConst.ATTACHES)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class OpenAttachmentsOnDiskTest {

    private static final String DISK = "disk.yandex.ru/mail?";
    private static final String WEBATTACH = "webattach.mail.yandex.net/message_part_real";

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
            IMAGE_ATTACHMENT
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Открываем аттач по тапу на диске из просмотра письма")
    @TestCaseId("740")
    public void shouldOpenAttachments() {
        steps.user().touchSteps().sendMsgWithDiskAttaches(accLock.firstAcc().getSelfEmail(), getRandomString(), 1);
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            MSG_FRAGMENT.makeTouchUrlPart(steps.user().apiMessagesSteps().getAllMessages().get(0).getMid())
        )
            .clicksOn(steps.pages().touch().messageView().attachments().waitUntil(not(empty())).get(0))
            .shouldSee(steps.pages().touch().messageView().viewer().viewerBody())
            .clicksOn(steps.pages().touch().messageView().viewer().openFileBtn().get(0))
            .switchOnJustOpenedWindow().shouldBeOnUrl(containsString(DISK));
    }

    @Test
    @Title("Открываем аттач на диске в списке писем")
    @TestCaseId("419")
    public void shouldOpenAttachmentsFromMsgListOnDisk() {
        steps.user().touchSteps().sendMsgWithDiskAttaches(accLock.firstAcc().getSelfEmail(), getRandomString(), 1);
        steps.user().defaultSteps().opensDefaultUrl();
        clickAttachAndOpenItFromViewer(0);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().viewer().openFileBtn().get(0))
            .switchOnJustOpenedWindow().shouldBeOnUrl(containsString(DISK));
    }

    @Test
    @Title("Открываем дисковый аттач на диске по тапу из композа")
    @TestCaseId("652")
    @DataProvider({"mp4", "jpg"})
    public void shouldOpenDiskAttachInCompose(String ext) {
        addMailAttachWithExtensionFromDisk(ext);
        assertThat(
            "Аттач не загрузился",
            steps.pages().touch().composeIframe().attachments().uploadedAttachment(),
            withWaitFor(hasSize(1), 15000)
        );
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().composeIframe().attachments().attachments().waitUntil(not(empty())).get(0))
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(DISK));
    }

    @Test
    @Title("Открываем аттачи с девайса по тапу в композе")
    @TestCaseId("652")
    public void shouldOpenAttachInCompose() {
        steps.user().defaultSteps()
            .opensDefaultUrlWithPostFix(
                COMPOSE_MSG_FRAGMENT.makeTouchUrlPart(steps.user().apiMessagesSteps().getAllMessages().get(0).getMid())
            );
        steps.user().touchSteps().switchToComposeIframe();
        steps.user().defaultSteps().clicksOn(
            steps.pages().touch().composeIframe().attachments().attachments().waitUntil(not(empty())).get(0)
        )
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(WEBATTACH));
    }

    @Step("Добавляем в композ аттач из диска c нужным расширением")
    private void addMailAttachWithExtensionFromDisk(String text) {
        steps.user().touchSteps().openComposeViaUrl();
        steps.user().defaultSteps().clicksOn(steps.pages().touch().composeIframe().header().clip())
            .shouldSee(steps.pages().touch().composeIframe().attachFilesPopup())
            .clicksOn(steps.pages().touch().composeIframe().attachFilesPopup().fromDisk())
            .shouldSee(steps.pages().touch().composeIframe().diskAttachmentsPage().attachment());
        int size = steps.pages().touch().composeIframe().diskAttachmentsPage().checkbox().size();
        for (int i = 0; i < size; i++) {
            if (hasText(CoreMatchers.containsString(text))
                .matches(steps.pages().touch().composeIframe().diskAttachmentsPage().attachments().get(i))
            ) {
                steps.user().defaultSteps()
                    .clicksOn(steps.pages().touch().composeIframe().diskAttachmentsPage().checkbox().get(i));
                break;
            } else
                steps.user().defaultSteps()
                    .scrollTo(steps.pages().touch().composeIframe().diskAttachmentsPage().attachments().get(i + 1));
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
