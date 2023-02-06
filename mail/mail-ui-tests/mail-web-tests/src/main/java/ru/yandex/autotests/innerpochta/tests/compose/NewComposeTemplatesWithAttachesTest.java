package ru.yandex.autotests.innerpochta.tests.compose;

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
import ru.yandex.qatools.allure.annotations.Step;

import static com.google.common.collect.ImmutableMap.of;
import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.YA_DISK_URL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Новый композ - Применение шаблонов с аттачами")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.COMPOSE)
public class NewComposeTemplatesWithAttachesTest extends BaseTest {

    private String template_body = getRandomString();
    private String template_subject = getRandomString();
    private String messsage_subject = getRandomString();
    private static final int FOLDER_POSITION = 1;
    private String FOLDER_NAME = "Загрузки";
    private static final String CORRECT_TOOLTIP = "Скачать • 418 КБ";

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
    public void logIn() {
        user.apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
                lock.firstAcc().getSelfEmail(),
                messsage_subject,
                getRandomString(),
                PDF_ATTACHMENT
        );
        user.apiSettingsSteps().callWithListAndParams(
                "Отключаем тредный режим",
                of(SETTINGS_FOLDER_THREAD_VIEW, false)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensUrl(YA_DISK_URL)
                .opensDefaultUrl();
    }

    @Test
    @Title("Отправляем шаблон со всеми видами аттачей")
    @TestCaseId("4714")
    public void shouldSendTemplateWithAttaches() {
        createTemplateWithAttaches();
        user.defaultSteps().clicksOn(onHomePage().composeButton());
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().templatesBtn())
                .clicksOn(onComposePopup().expandedPopup().templatePopup().templateList().get(0))
                .clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.messagesSteps().clicksOnMessageWithSubject(template_subject);
        user.defaultSteps().shouldContainText(onMessageView().attachments().list().get(0), FOLDER_NAME)
                .shouldContainText(onMessageView().attachments().list().get(1), PDF_ATTACHMENT)
                .onMouseHover(onMessageView().attachments().list().get(2));
        assertEquals(
            "Тултип содержит неверный текст",
            CORRECT_TOOLTIP,
            onMessageView().attachmentToolTip().getText()
        );
    }

    @Step("Создаем шаблон с аттачами")
    private void createTemplateWithAttaches(){
        user.defaultSteps().clicksOn(onHomePage().composeButton())
                .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail())
                .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), template_subject)
                .appendTextInElement(onComposePopup().expandedPopup().bodyInput(), template_body);
        user.composeSteps().uploadLocalFile(onComposePopup().expandedPopup().localAttachInput(), IMAGE_ATTACHMENT);
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup().attachPanel().linkedAttach().get(0))
                .clicksOn(onComposePopup().expandedPopup().diskAttachBtn())
                .clicksOn(
                        onComposePopup().addDiskAttachPopup().attachList().get(0),
                        onComposePopup().addDiskAttachPopup().addAttachBtn()
                )
                .shouldNotSee(onComposePopup().addDiskAttachPopup())
                .clicksOn(onComposePopup().expandedPopup().mailAttachBtn())
                .doubleClick(onComposePopup().addDiskAttachPopup().attachList().get(FOLDER_POSITION))
                .clicksOn(
                        onComposePopup().addDiskAttachPopup().attachList().get(0),
                        onComposePopup().addDiskAttachPopup().addAttachBtn()
                )
                .shouldNotSee(
                        onComposePopup().addDiskAttachPopup(),
                        onComposePopup().expandedPopup().attachPanel().loadingAttach()
                )
                .clicksOn(onComposePopup().expandedPopup().templatesBtn())
                .clicksOn(onComposePopup().expandedPopup().templatePopup().saveBtn())
                .shouldSeeWithWaiting(onMessagePage().statusLineBlock(), 20)
                .clicksOn(onComposePopup().expandedPopup().closeBtn());
    }

}
