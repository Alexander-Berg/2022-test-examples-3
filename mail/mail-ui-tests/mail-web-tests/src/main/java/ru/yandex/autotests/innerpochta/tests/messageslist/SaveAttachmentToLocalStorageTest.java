package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.EXCEL_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

@Aqua.Test
@Title("Сохранение аттачей на компьютер")
@Features(FeaturesConst.DOWNLOAD_FILE)
@Tag(FeaturesConst.DOWNLOAD_FILE)
@Stories(FeaturesConst.ATTACHES)
public class SaveAttachmentToLocalStorageTest extends BaseTest {

    private static final String MESSAGE_WITH_ATTACHES = "attachments";
    private static final String ARCHIVE_NAME = "Attachments_";
    private static final String ARCHIVE_SIZE = "740 kB";
    private static final String IMAGE_SIZE = "418 kB";
    private static final String ATTACH_SIZE = "324 kB";
    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiMessagesSteps().sendMailWithAttachmentsAndHTMLBodyNoSave(lock.firstAcc().getSelfEmail(),
            MESSAGE_WITH_ATTACHES, "", IMAGE_ATTACHMENT, PDF_ATTACHMENT, EXCEL_ATTACHMENT
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Сохраняем один аттач на компьютер из списка писем")
    @TestCaseId("1074")
    public void shouldSaveAttachmentFromMessageList() {
        user.defaultSteps().onMouseHoverAndClick(
            onMessagePage().displayedMessages().firstMessageWithSubject(MESSAGE_WITH_ATTACHES).attachments().list()
                .get(1).download()
        )
            .checkDownloadedFileNameAndSize(PDF_ATTACHMENT, ATTACH_SIZE);
    }

    @Test
    @Title("Сохраняем все аттачи архивом на компьютер из списка писем")
    @TestCaseId("1074")
    public void shouldSaveArchiveFromMessageList() {
        user.defaultSteps().clicksOn(
            onMessagePage().displayedMessages().firstMessageWithSubject(MESSAGE_WITH_ATTACHES)
                .attachments().download()
        )
            .checkDownloadedArchiveNameAndSize(ARCHIVE_NAME + lock.firstAcc().getSelfEmail(), ARCHIVE_SIZE);
    }

    @Test
    @Title("Сохраняем один аттач на компьютер из просмотрщика картинок")
    @TestCaseId("3109")
    public void shouldSaveAttachmentFromImageViewer() {
        user.defaultSteps().onMouseHoverAndClick(
            onMessagePage().displayedMessages().firstMessageWithSubject(MESSAGE_WITH_ATTACHES).attachments().list()
                .get(0).show()
        )
            .clicksOn(onMessageView().imageDownload())
            .checkDownloadedFileNameAndSize(IMAGE_ATTACHMENT, IMAGE_SIZE);
    }
}
