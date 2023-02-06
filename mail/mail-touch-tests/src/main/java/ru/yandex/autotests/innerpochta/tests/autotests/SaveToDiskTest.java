package ru.yandex.autotests.innerpochta.tests.autotests;

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

import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.SAVE_TO_DISK_TAG;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на сохранение на диск")
@Features(FeaturesConst.SAVE_TO_DISK)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class SaveToDiskTest {

    private static final String SUBJECT_DISK = "Attachments from disk";
    private static final String SUBJECT_DEVICE = "Attachments from device";
    private static final String DISK_URL = "https://disk.yandex.ru/";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount(SAVE_TO_DISK_TAG));
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
            SUBJECT_DEVICE,
            Utils.getRandomString(),
            IMAGE_ATTACHMENT,
            PDF_ATTACHMENT
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
        steps.user().defaultSteps().opensUrl(DISK_URL).waitInSeconds(2).opensDefaultUrl();
        steps.user().touchSteps().sendMsgWithDiskAttaches(accLock.firstAcc().getSelfEmail(), SUBJECT_DISK, 2);
    }

    @Test
    @Title("Сохраняем один дисковой аттач на диск повторно")
    @TestCaseId("318")
    public void shouldSaveDiskAttachmentsTwice() {
        openMsg(SUBJECT_DISK);
        saveAttachmentToDisk();
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().messageView().statusLineError());
    }

    @Test
    @Title("Сохраняем однин почтовый/девайсный аттач на диск повторно")
    @TestCaseId("319")
    public void shouldNotSaveAttachmentsTwice() {
        openMsg(SUBJECT_DEVICE);
        saveAttachmentToDisk();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().statusLineError());
    }

    @Test
    @Title("Сохраняем все аттачи на диск повторно")
    @TestCaseId("723")
    @DataProvider({SUBJECT_DISK, SUBJECT_DEVICE})
    public void shouldSaveAllAttachmentsToDisk(String subject) {
        openMsg(subject);
        saveAllAttachmentsToDisk();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().attachmentsBlock().saveToDisk());
        saveAllAttachmentsToDisk();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageView().attachmentsBlock().saveToDisk())
            .shouldNotSee(steps.pages().touch().messageView().statusLineError());
    }

    @Test
    @Title("Должны видеть троббер во время загрузки всех аттачей на диск")
    @TestCaseId("700")
    public void shouldSeeTrobberWhileSaving() {
        openMsg(SUBJECT_DISK);
        saveAllAttachmentsToDisk();
        steps.user().defaultSteps().shouldSee(
            steps.pages().touch().messageView().attachmentsBlock().trobber(),
            steps.pages().touch().messageView().attachmentsBlock().saveToDisk()
        );
    }

    @Step("Дважды открываем аттач во вьюере и сохраняем его на диск")
    private void saveAttachmentToDisk() {
        for (int i = 0; i < 2; i++)
            steps.user().defaultSteps()
                .shouldSee(steps.pages().touch().messageView().attachmentsBlock())
                .clicksOn(steps.pages().touch().messageView().attachmentsBlock().attachments().get(0))
                .clicksOn(steps.pages().touch().messageView().viewer().saveToDisk())
                .clicksOn(steps.pages().touch().messageView().saveToDisk().saveBtn())
                .shouldSee(steps.pages().touch().messageView().attachmentsBlock());
    }

    @Step("Сохраняем все аттачи на диск n раз")
    private void saveAllAttachmentsToDisk() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().attachmentsBlock().saveToDisk());
    }

    @Step("Открываем письмо с темой")
    private void openMsg(String subject) {
        steps.user().defaultSteps().refreshPage()
            .shouldSee(steps.pages().touch().messageList().headerBlock())
            .clicksOnElementWithText(steps.pages().touch().messageList().subjectList(), subject);
    }
}
