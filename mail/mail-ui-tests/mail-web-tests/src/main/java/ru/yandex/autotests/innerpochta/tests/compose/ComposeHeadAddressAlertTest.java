package ru.yandex.autotests.innerpochta.tests.compose;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;


/**
 * Created by kurau on 13.05.14.
 */
@Aqua.Test
@Title("Новый композ - Тест на алерты при неправильно заполненых полях кому...")
@Description("Тесты на композ")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.HEAD)
public class ComposeHeadAddressAlertTest extends BaseTest {

    private static final String EMPTY_DESCRIPTION = "Пожалуйста, укажите адрес получателя";
    private static final String EMPTY_TITLE = "Письмо не отправлено";
    private static final String WRONG_MAIL = "@@..sadsadda";
    private static final String INVALID_MAIL_TITLE = "Проверьте получателя";
    private static final String INVALID_MAIL_DESCRIPTION = "Похоже, что-то не так с адресом: " + WRONG_MAIL;
    private static final String WRONG_MAIL_FOR_SEND = "nekorrrektiy342@адрес.тест";
    private static final String WRONG_MAIL_SUBJECT = "Письмо не доставлено на адрес";

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
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.COMPOSE);
    }

    @Test
    @Title("Проверяем алерты при пустом поле кому, тема")
    @TestCaseId("1209")
    public void composeHeadFieldToEmptyAlert() {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn())
            .shouldSee(onComposePopup().confirmClosePopup())
            .shouldContainText(onComposePopup().confirmClosePopup().title(), EMPTY_TITLE)
            .shouldContainText(onComposePopup().confirmClosePopup().description(), EMPTY_DESCRIPTION);
    }

    @Test
    @Title("Проверяем алерт при неправильном адресе в поле кому")
    @TestCaseId("1210")
    public void composeHeadFieldInvalidToAlert() {
        user.defaultSteps().inputsTextInElement(onComposePopup().expandedPopup().popupTo(), WRONG_MAIL)
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), Utils.getRandomString())
            .clicksOn(onComposePopup().expandedPopup().sendBtn())
            .shouldSee(onComposePopup().confirmClosePopup())
            .shouldContainText(onComposePopup().confirmClosePopup().title(), INVALID_MAIL_TITLE)
            .shouldContainText(onComposePopup().confirmClosePopup().description(), INVALID_MAIL_DESCRIPTION);
    }

    @Test
    @Title("Проверяем письмо о несуществующем адресе")
    @TestCaseId("2550")
    public void composeHeadFieldInvalidToAlertEmail() {
        user.composeSteps().inputsAddressInFieldTo(WRONG_MAIL_FOR_SEND)
            .inputsSubject(getRandomString())
            .clicksOnSendButtonInHeader()
            .waitForMessageToBeSend();
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMessageWithSubject(WRONG_MAIL_SUBJECT);
    }
}
