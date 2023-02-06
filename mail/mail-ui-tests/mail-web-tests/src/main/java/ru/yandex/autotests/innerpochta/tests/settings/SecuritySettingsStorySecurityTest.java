package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.ns.pages.settings.pages.SecuritySettingsPage;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 18:48
 */

@Aqua.Test
@Title("Проверка настройки безопасного соединения и ссылок")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SECURITY)
public class SecuritySettingsStorySecurityTest extends BaseTest {

    private static final String CHANGE_PASS_URL = "passport.yandex.ru/passport?mode=changepass";
    private static final String CREATE_SECURE_PASS_URL = "https://yandex.ru/blog/mail/1063";
    private static final String PHONE_NUMBERS_URL = "https://passport.yandex.ru/profile/phones";
    private static final String PASS_EMAIL_URL = "https://passport.yandex.ru/profile/emails";

    private AllureStepStorage user = new AllureStepStorage(webDriverRule);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public AccLockRule lock = AccLockRule.use().useTusAccount();

    @Before
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_SECURITY);
    }

    @Test
    @Title("Проверка настройки безопасного соединения")
    @TestCaseId("1837")
    public void linksOnSecuritySettingsPage() {
        user.defaultSteps().shouldSee(onSecuritySettingsPage().blockSecurity())
            .clicksOn(onSecuritySettingsPage().blockSecurity().toggleHintAboutPhoneNumbers())
            .shouldSee(onSecuritySettingsPage().hintPopUp())
            .shouldSeeThatElementTextEquals(onSecuritySettingsPage().hintPopUp(), SecuritySettingsPage.PHONE_HINT_TEXT)
            .clicksOn(onSecuritySettingsPage().hintPopUp())
            .clicksOn(onSecuritySettingsPage().blockSecurity().editAliases())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(PASS_EMAIL_URL)
            .opensDefaultUrlWithPostFix(QuickFragments.SETTINGS_SECURITY.makeUrlPart())
            .clicksOn(onSecuritySettingsPage().blockSecurity().phoneNumbersLink())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrlNotDiffWith(PHONE_NUMBERS_URL)
            .opensDefaultUrlWithPostFix(QuickFragments.SETTINGS_SECURITY.makeUrlPart())
            .clicksOn(onSecuritySettingsPage().blockSecurity().changePasswordLink())
            .shouldBeOnUrl(containsString(CHANGE_PASS_URL));
    }


    @Test
    @Title("Проверка ссылок безопасности")
    @TestCaseId("1837")
    public void linksOnSecurityPage() {
        user.defaultSteps().shouldSee(onSecuritySettingsPage().blockSecurity())
            .clicksOn(onSecuritySettingsPage().blockSecurity().howtoCreateSecurePasswordLink())
            .waitInSeconds(2)
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(CREATE_SECURE_PASS_URL)
            .opensDefaultUrlWithPostFix(QuickFragments.SETTINGS_SECURITY.makeUrlPart())
            .clicksOn(onSecuritySettingsPage().blockSecurity().journalLink())
            .shouldBeOnUrl(containsString(QuickFragments.SETTINGS_JOURNAL.fragment()));
    }
}
