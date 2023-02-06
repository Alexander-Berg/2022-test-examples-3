package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.MailConst.DISK_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.YA_DISK_URL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Новый композ - Аттачи")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.ATTACHES)
@RunWith(DataProviderRunner.class)
public class NewComposeAttachesTest extends BaseTest {

    private static final String ATTACH_LOCAL_PDF_SIZE = "324 КБ";
    private static final String ATTACH_DISK_TOOLTIP = "Горы.jpg1,7 МБ";
    private static final String ATTACH_MAIL_TOOLTIP = "doc.pdf324 КБ";
    private static final String ATTACH_EXT_EML = "EML";
    private static final int ATTACH_COUNT = 4;
    private static final int FOLDER_POSITION = 1;
    private static final int ATTACH_COUNT_IN_MSG_VIEW = 3;
    private static String TEXT = getRandomString() + "\n";

    private AccLockRule lock = AccLockRule.use().useTusAccount(DISK_USER_TAG);
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
        user.apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            lock.firstAcc().getSelfEmail(),
            getRandomString(),
            getRandomString(),
            PDF_ATTACHMENT
        );
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, FALSE)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensUrl(YA_DISK_URL)
            .opensDefaultUrlWithPostFix("/compose");
    }

    @Test
    @Title("Отправляем письмо с аттачами - Локальный/Диск/Почта")
    @TestCaseId("5664")
    public void shouldSendMsgWithAttach() {
        user.defaultSteps()
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail())
            .clicksOn(onComposePopup().suggestList().get(0))
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), getRandomString());
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
            .clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().refreshPage()
            .shouldSeeElementsCount(
                onMessagePage().displayedMessages().list().get(0).attachments().list(),
                ATTACH_COUNT_IN_MSG_VIEW
            );
    }

    @Test
    @Title("Разворачиваем/Скрываем залипающую панель аттачей")
    @TestCaseId("5678")
    public void shouldExpandAttachPanel() {
        user.composeSteps().inputsSendText(StringUtils.repeat(TEXT, 10));
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().minimizeBtn())
            .clicksOn(onComposePopup().expandedPopup().diskAttachBtn())
            .clicksOn(onComposePopup().addDiskAttachPopup().attachList().get(0));
        user.hotkeySteps().clicksOnElementWhileHolding(
            onComposePopup().addDiskAttachPopup().attachList().get(ATTACH_COUNT),
            key(Keys.SHIFT)
        );
        user.defaultSteps().clicksOn(onComposePopup().addDiskAttachPopup().addAttachBtn())
            .shouldNotSee(onComposePopup().addDiskAttachPopup())
            .shouldSee(
                onComposePopup().expandedPopup().attachPanel().attachCount(),
                onComposePopup().expandedPopup().attachPanel().attachSize()
            )
            .clicksOn(onComposePopup().expandedPopup().attachPanel().attachSize())
            .shouldSeeInViewport(
                onComposePopup().expandedPopup().attachPanel().linkedAttach().get(ATTACH_COUNT)
            );
    }

    @Test
    @Title("Прикрепляем аттач с компьютера")
    @TestCaseId("5652")
    public void shouldAddLocalAttach() {
        user.composeSteps().uploadLocalFile(onComposePopup().expandedPopup().localAttachInput(), PDF_ATTACHMENT);
        checkAttachElements(PDF_ATTACHMENT + ATTACH_LOCAL_PDF_SIZE);
        user.defaultSteps().shouldSeeThatElementHasText(
            onComposePopup().expandedPopup().attachPanel().linkedAttach().get(0).attachName(),
            PDF_ATTACHMENT
        )
            .shouldSee(onComposePopup().expandedPopup().attachPanel().linkedAttach().waitUntil(not(empty()))
                .get(0).attachPdf());
    }

    @Test
    @Title("Прикрепляем аттач с диска")
    @TestCaseId("5653")
    public void shouldAddDiskAttach() {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().diskAttachBtn())
            .clicksOn(
                onComposePopup().addDiskAttachPopup().attachList().get(0),
                onComposePopup().addDiskAttachPopup().addAttachBtn()
            )
            .shouldNotSee(
                onComposePopup().addDiskAttachPopup(),
                onComposePopup().expandedPopup().attachPanel().loadingAttach()
            );
        checkAttachElements(ATTACH_DISK_TOOLTIP);
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup().attachPanel().linkedAttach().waitUntil(not(empty()))
            .get(0).attachPreview());
    }

    @Test
    @Title("Прикрепляем аттач из почты")
    @TestCaseId("5654")
    public void shouldAddMailAttach() {
        user.defaultSteps()
            .clicksOn(onComposePopup().expandedPopup().mailAttachBtn())
            .doubleClick(onComposePopup().addDiskAttachPopup().attachList().get(FOLDER_POSITION))
            .clicksOn(
                onComposePopup().addDiskAttachPopup().attachList().get(0),
                onComposePopup().addDiskAttachPopup().addAttachBtn()
            )
            .shouldNotSee(
                onComposePopup().addDiskAttachPopup(),
                onComposePopup().expandedPopup().attachPanel().loadingAttach()
            );
        checkAttachElements(ATTACH_MAIL_TOOLTIP);
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup().attachPanel().linkedAttach().waitUntil(not(empty()))
            .get(0).attachPdf());
    }

    @Test
    @Title("Удаляем аттачи разных типов")
    @TestCaseId("5658")
    public void shouldDeleteAttach() {
        user.defaultSteps()
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail())
            .clicksOn(onComposePopup().suggestList().get(0))
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), getRandomString());
        user.composeSteps().uploadLocalFile(onComposePopup().expandedPopup().localAttachInput(), IMAGE_ATTACHMENT);
        user.defaultSteps()
            .clicksOn(onComposePopup().expandedPopup().diskAttachBtn())
            .clicksOn(onComposePopup().addDiskAttachPopup().attachList().get(0));
        user.hotkeySteps()
            .clicksOnElementHoldingCtrlKey(onComposePopup().addDiskAttachPopup().attachList().get(1));
        user.defaultSteps().clicksOn(onComposePopup().addDiskAttachPopup().addAttachBtn())
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
            );
        clickOnDeleteAttach(3);
        clickOnDeleteAttach(2);
        clickOnDeleteAttach(1);
        clickOnDeleteAttach(0);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn());
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().refreshPage()
            .shouldNotSee(onMessagePage().displayedMessages().list().get(0).attachments());
    }

    @Test
    @Title("Пересылка нескольких писем")
    @TestCaseId("5660")
    public void shouldAttachEml() {
        user.apiMessagesSteps().sendThread(lock.firstAcc(), getRandomString(), 2);
        user.defaultSteps().refreshPage();
        user.messagesSteps().selectMessageWithIndex(0).selectMessageWithIndex(1);
        forwardSelectedMsg();
        user.defaultSteps().refreshPage()
            .shouldSeeThatElementHasText(
                onMessagePage().displayedMessages().list().get(0).attachments().list().get(0).ext(),
                ATTACH_EXT_EML
            )
            .shouldSeeThatElementHasText(
                onMessagePage().displayedMessages().list().get(0).attachments().list().get(1).ext(),
                ATTACH_EXT_EML
            );
    }

    @Test
    @Title("Отправляем письмо с аттачем по хоткею")
    @TestCaseId("1045")
    public void shouldSendMsgWithAttachByHotkey() {
        user.defaultSteps()
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail())
            .clicksOn(onComposePopup().suggestList().waitUntil(not(empty())).get(0))
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), getRandomName());
        user.composeSteps().uploadLocalFile(onComposePopup().expandedPopup().localAttachInput(), IMAGE_ATTACHMENT);
        user.defaultSteps().shouldNotSee(onComposePopup().expandedPopup().attachPanel().loadingAttach());
        user.hotkeySteps().pressCombinationOfHotKeys(
            onComposePopup().expandedPopup().bodyInput(),
            key(Keys.CONTROL), key(Keys.ENTER)
        );
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().refreshPage()
            .shouldSeeElementsCount(
                onMessagePage().displayedMessages().list().get(0).attachments().list(),
                1
            );
    }

    @Step("Проверяем элементы аттача")
    private void checkAttachElements(String tooltip) {
        user.defaultSteps().shouldSee(onComposePopup().expandedPopup().attachPanel().linkedAttach().get(0))
            .onMouseHover(onComposePopup().expandedPopup().attachPanel().linkedAttach().get(0))
            .shouldSee(onComposePopup().expandedPopup().attachPanel().linkedAttach().get(0).attachDeteteBtn())
            .shouldSeeThatElementHasText(onComposePopup().composeTooltip(), tooltip);
    }

    @Step("Нажимаем на кнопку удаления аттача")
    private void clickOnDeleteAttach(int num) {
        user.defaultSteps().onMouseHover(onComposePopup().expandedPopup().attachPanel().linkedAttach().get(num))
            .clicksOn(onComposePopup().expandedPopup().attachPanel().linkedAttach().get(num).attachDeteteBtn());
    }

    @Step("Пересылаем несколько сообщений")
    private void forwardSelectedMsg() {
        user.defaultSteps().clicksOn(onMessagePage().toolbar().forwardButton())
            .shouldSee(onComposePopup().composePopup())
            .shouldSeeThatElementHasText(onComposePopup().expandedPopup().attachPanel().attachCount(), "2")
            .shouldSee(onComposePopup().expandedPopup().attachPanel().linkedAttach().waitUntil(not(empty()))
                .get(0).attachEml())
            .shouldSee(onComposePopup().expandedPopup().attachPanel().linkedAttach().waitUntil(not(empty()))
                .get(1).attachEml())
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), lock.firstAcc().getSelfEmail())
            .clicksOn(
                onComposePopup().suggestList().get(0),
                onComposePopup().expandedPopup().sendBtn()
            );
        user.composeSteps().waitForMessageToBeSend();
    }
}
