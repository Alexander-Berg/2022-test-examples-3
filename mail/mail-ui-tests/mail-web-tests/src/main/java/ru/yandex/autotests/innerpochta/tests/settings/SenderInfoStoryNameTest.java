package ru.yandex.autotests.innerpochta.tests.settings;

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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 18:48
 */

@Aqua.Test
@Title("Изменение имени, номера и портрета отправителя")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SENDER_SETTINGS)
public class SenderInfoStoryNameTest extends BaseTest {

    private static final String PASSPORT_EMAILS = "https://passport.yandex.ru/profile/emails";
    private static final String PASSPORT_AVATAR = "https://passport.yandex.ru/profile/avatars";
    private static final String PASSPORT_PHONE = "https://passport.yandex.ru/profile/phones";
    private static final String PASSPORT_PASSWORD = "https://passport.yandex.ru/profile/password?retpath=";
    private static final String PASSPORT_INFO = "https://passport.yandex.ru/profile?from=mail";
    private static final String PASSPORT_PERSONAL = "https://passport.yandex.ru/profile/personal-info?retpath=";
    private final static String SENDER_NAME = "aBcАбс123\",./;'[]<>?:|{}!@#$%^&*()_+-=!№";
    private final static String NEW_BTN_NAME = "Изменить портрет";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_SENDER);
    }

    @Test
    @Title("Отображаем имя отправителя")
    @TestCaseId("1850")
    public void testSenderNameIsOn() {
        inputSenderName(SENDER_NAME);
        sendMessageAndCheckSenderName(SENDER_NAME);
    }

    @Test
    @Title("Не отображаем имя отправителя")
    @TestCaseId("1849")
    public void testSenderNameIsOff() {
        inputSenderName("");
        sendMessageAndCheckSenderName(lock.firstAcc().getLogin() + "@ya.ru");
    }

    @Test
    @Title("Проверка ссылки редактирования адресов")
    @TestCaseId("1848")
    public void testValidatorLinkOnSenderInfoPage() {
        user.defaultSteps().clicksOn(onSenderInfoSettingsPage().blockSetupSender()
            .blockAliases().addAddressLink())
            .shouldBeOnUrl(PASSPORT_EMAILS);
    }

    @Test
    @Title("Переходим в паспорт для изменения портрета")
    @TestCaseId("3896")
    public void shouldOpenPassport() {
        user.defaultSteps()
            .shouldHasText(onSenderInfoSettingsPage().blockSetupSender().blockAvatar().loadLink(), NEW_BTN_NAME)
            .clicksOn(onSenderInfoSettingsPage().blockSetupSender().blockAvatar().loadLink())
            .shouldBeOnUrl(containsString(PASSPORT_AVATAR));
    }

    @Test
    @Title("Проверка ссылки Сделать адресом номер телефона")
    @TestCaseId("985")
    public void shouldOpenPassportPhone() {
        user.defaultSteps().clicksOn(onSenderInfoSettingsPage().blockSetupSender().blockAliases().addPhoneLink())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrlNotDiffWith(PASSPORT_PHONE);
    }

    @Test
    @Title("Проверка кнопки Поменять пароль")
    @TestCaseId("2532")
    public void shouldOpenPassportPassword() {
        user.defaultSteps().clicksOn(onSettingsPage().blockSettingsNav().changePassword())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(PASSPORT_PASSWORD));

    }

    @Test
    @Title("Проверка кнопки Указать данные")
    @TestCaseId("2532")
    public void shouldOpenPassportInfo() {
        user.defaultSteps().clicksOn(onSettingsPage().blockSettingsNav().changeInfo())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(PASSPORT_INFO));

    }

    @Test
    @Title("Проверка кнопки Изменить часовой пояс")
    @TestCaseId("2531")
    public void shouldOpenPassportChangeTimeZone() {
        user.defaultSteps().clicksOn(onSettingsPage().blockSettingsNav().changeTimeZone())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(PASSPORT_PERSONAL));

    }

    private void sendMessageAndCheckSenderName(String name) {
        String subject = Utils.getRandomName();
        user.apiMessagesSteps().sendMailFromName(lock.firstAcc().getLogin() + "@yandex.ru", subject, "", name);
        user.defaultSteps().opensDefaultUrl();
        user.messagesSteps().shouldSeeNameOnMessageWithSubject(name, subject);
    }

    private void inputSenderName(String name) {
        user.settingsSteps().entersSenderName(name)
            .saveSettingsIfCanAndClicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(0));
    }
}
