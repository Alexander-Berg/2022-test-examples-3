package ru.yandex.autotests.innerpochta.tests.autotests.Attachments;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
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

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.DISK_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.WRONG_EXTENSION;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на открытие аттачей во вьюере")
@Features(FeaturesConst.ATTACHES)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class OpenAttachmentsInViewerTest {

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
            IMAGE_ATTACHMENT,
            WRONG_EXTENSION
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[][] msgAndAttach() {
        return new Object[][]{
            {0, 0},
            {1, 0},
            {1, 1},
        };
    }

    @Test
    @Title("Открываем аттач по тапу во вьюере из просмотра письма")
    @TestCaseId("740")
    @UseDataProvider("msgAndAttach")
    public void shouldOpenAttachmentsInViewer(int msgNum, int attachNum) {
        steps.user().touchSteps().sendMsgWithDiskAttaches(accLock.firstAcc().getSelfEmail(), getRandomString(), 2);
        steps.user().defaultSteps().refreshPage()
            .clicksOn(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(msgNum).subject())
            .clicksOn(steps.pages().touch().messageView().attachments().waitUntil(not(empty())).get(attachNum))
            .shouldSee(steps.pages().touch().messageView().viewer().viewerBody());
    }

    @Test
    @Title("Открываем аттач по тапу во вьюере из списка писем")
    @TestCaseId("419")
    @UseDataProvider("msgAndAttach")
    public void shouldOpenAttachmentsFromMsgListInViewer(int msgNum, int attachNum) {
        steps.user().touchSteps().sendMsgWithDiskAttaches(accLock.firstAcc().getSelfEmail(), getRandomString(), 2);
        steps.user().defaultSteps().refreshPage();
        clickAttachAndOpenItFromViewer(msgNum, attachNum);
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageView().viewer().openFileBtn());
    }

    @Step("Кликаем в аттач и открываем его во вьюере")
    private void clickAttachAndOpenItFromViewer(int numOfMsg, int numOfAttach) {
        steps.user().defaultSteps()
            .clicksOn(
                steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(numOfMsg)
                    .attachmentsInMessageList().waitUntil(not(empty())).get(numOfAttach)
            )
            .shouldSee(steps.pages().touch().messageView().viewer().viewerBody());
    }
}
