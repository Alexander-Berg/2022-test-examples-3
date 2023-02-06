package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebElement;
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
import static ru.yandex.autotests.innerpochta.util.MailConst.DISK_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.MailConst.PDF_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.util.MailConst.YA_DISK_URL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Новый композ - Открытие из QR")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class NewComposeQRTest extends BaseTest {
    private static final int FOLDER_POSITION = 1;
    String msgSubject;
    String bodyText;

    private AccLockRule lock = AccLockRule.use().useTusAccount(DISK_USER_TAG);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @DataProvider
    public static Object[][] layouts() {
        return new Object[][]{
            {LAYOUT_2PANE},
            {LAYOUT_3PANE_VERTICAL}
        };
    }

    @Before
    public void setUp() {
        msgSubject = getRandomString();
        bodyText = getRandomString();
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма в списке писем",
            of(
                SETTINGS_OPEN_MSG_LIST, STATUS_ON
            )
        );
        user.apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            lock.firstAcc().getSelfEmail(),
            getRandomString(),
            getRandomString(),
            PDF_ATTACHMENT
        );
        user.apiMessagesSteps().sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), msgSubject, getRandomString());
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().opensUrl(YA_DISK_URL)
            .opensDefaultUrl();
    }

    @Test
    @Title("Открываем новый композ из QR в списке писем")
    @TestCaseId("5677")
    @UseDataProvider("layouts")
    public void shouldOpenComposePopupFromQRInMessageListPage(String layout) {
        user.apiSettingsSteps()
            .callWithListAndParams("Включаем " + layout, of(SETTINGS_PARAM_LAYOUT, layout));
        user.defaultSteps().refreshPage();
        user.messagesSteps().clicksOnMessageWithSubject(msgSubject);
        user.messageViewSteps().openQRAndInputText(bodyText);
        user.defaultSteps()
            .clicksOn(onMessageView().quickReply().openCompose())
            .shouldSee(onComposePopup().expandedPopup())
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), bodyText);
    }

    @Test
    @Title("Открываем новый композ из QR на отдельной странице")
    @TestCaseId("5674")
    public void shouldOpenComposePopupFromQROnMessagePage() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма на отдельной странице",
            of(SETTINGS_OPEN_MSG_LIST, EMPTY_STR)
        );
        user.defaultSteps().refreshPage();
        user.messagesSteps().clicksOnMessageWithSubject(msgSubject);
        user.messageViewSteps().openQRAndInputText(bodyText);
        user.defaultSteps().clicksOn(onMessageView().quickReply().openCompose())
            .shouldSee(onComposePopup().composePopup())
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), bodyText);
    }

    @Test
    @Title("Прикрепить аттач из Диска через QR")
    @TestCaseId("3038")
    public void shouldAttachDiskFileFromQR() {
        setSettingAndOpenQR();
        openDiskPopupAndAttachFile(onMessageView().quickReply().addDiskAttach(), 1);
    }

    @Test
    @Title("Прикрепить аттач из Диска через QR двойным кликом")
    @TestCaseId("3038")
    public void shouldAttachDiskFileFromQRWithDoubleClick() {
        setSettingAndOpenQR();
        user.defaultSteps().clicksOn(onMessageView().quickReply().addDiskAttach())
            .doubleClick(onComposePopup().addDiskAttachPopup().attachList().get(1))
            .shouldNotSee(onComposePopup().addDiskAttachPopup())
            .shouldSee(onComposePopup().expandedPopup().attachPanel().linkedAttach().waitUntil(not(empty())).get(0));
    }

    @Test
    @Title("Прикрепить аттач из почты через QR")
    @TestCaseId("3038")
    public void shouldAttachMailFromQR() {
        setSettingAndOpenQR();
        user.defaultSteps().clicksOn(onMessageView().quickReply().addMailAttach())
            .doubleClick(onComposePopup().addDiskAttachPopup().attachList().get(FOLDER_POSITION))
            .clicksOn(
                onComposePopup().addDiskAttachPopup().attachList().waitUntil(not(empty())).get(0),
                onComposePopup().addDiskAttachPopup().addAttachBtn()
            )
            .shouldNotSee(onComposePopup().addDiskAttachPopup())
            .shouldSee(onComposePopup().expandedPopup().attachPanel().linkedAttach().get(0));
    }

    @Test
    @Title("Нажимаем кнопку «Отменить» в попапе загрузки аттача с диска/из почты открытого через QR")
    @TestCaseId("3038")
    public void shouldNotSeeDiskAttachPopup() {
        setSettingAndOpenQR();
        user.composeSteps().checkCloseDiskPopup(onMessageView().quickReply().addDiskAttach())
            .checkCloseDiskPopup(onMessageView().quickReply().addMailAttach());
    }

    @Step("Включаем просмотр письма на отдельной странице и открываем QR")
    private void setSettingAndOpenQR() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем просмотр письма на отдельной странице",
            of(SETTINGS_OPEN_MSG_LIST, EMPTY_STR)
        );
        user.defaultSteps().refreshPage();
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.messageViewSteps().openQRAndInputText(getRandomString());
    }

    @Step("Открываем дисковый попап и прикрепляем файл")
    private void openDiskPopupAndAttachFile(WebElement button, int num) {
        user.defaultSteps().clicksOn(button)
            .clicksOn(
                onComposePopup().addDiskAttachPopup().attachList().get(num),
                onComposePopup().addDiskAttachPopup().addAttachBtn()
            )
            .shouldNotSee(onComposePopup().addDiskAttachPopup())
            .shouldSee(onComposePopup().expandedPopup().attachPanel().linkedAttach().waitUntil(not(empty())).get(0));
    }
}
