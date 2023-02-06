package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
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
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.OUTBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.UNDEFINED;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_STORED_COMPOSE_STATES;
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
public class NewComposeDiskAttachQRTest extends BaseTest {

    String msgSubject;

    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(removeAllMessages(() -> user, INBOX, DRAFT, OUTBOX, TRASH));

    @Before
    public void setUp() {
        msgSubject = getRandomString();
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 2pane, просмотр письма на отдельной странице и сбрасываем свёрнутые композы",
            of(
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE,
                SETTINGS_OPEN_MSG_LIST, EMPTY_STR,
                SETTINGS_STORED_COMPOSE_STATES, UNDEFINED
            )
        );
        user.apiMessagesSteps().sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), msgSubject, getRandomString());
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Прикрепить папку из Диска через QR")
    @TestCaseId("3038")
    public void shouldAttachDiskFolderFromQR() {
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.messageViewSteps().openQRAndInputText(getRandomString());
        openDiskPopupAndAttachFile(onMessageView().quickReply().addDiskAttach(), 0);
        user.defaultSteps()
            .shouldSee(onComposePopup().expandedPopup().attachPanel().linkedAttach().waitUntil(not(empty()))
                .get(0).folderIcon()
            );
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
