package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.cthul.matchers.CthulMatchers.and;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 18:48
 */

@Aqua.Test
@Title("Изменение имени отправителя для пользователя с . и - в логине")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SENDER_SETTINGS)
public class SenderInfoStoryAliasTest extends BaseTest {

    private String subj;
    private String alias;

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем треды",
            of(SETTINGS_FOLDER_THREAD_VIEW, FALSE)
        );
        subj = Utils.getRandomName();
    }

    @Test
    @Title("Изменение имени отправителя на имя с точкой")
    @TestCaseId("1846")
    public void testLoginWithDotAlias() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_SENDER);
        alias = changesAlias(0, lock.firstAcc());
        user.defaultSteps().shouldContainText(onHomePage().mail360HeaderBlock().userMenu(), getLogin(alias));
        user.messagesSteps().clicksOnMessageWithSubject(subj);
        shouldSeeFromEmail(containsString(alias));
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_SENDER);
        alias = changesAlias(1, lock.firstAcc());
        user.defaultSteps().shouldContainText(onHomePage().mail360HeaderBlock().userMenu(), getLogin(alias));
        user.messagesSteps().clicksOnMessageWithSubject(subj);
        shouldSeeFromEmail(containsString(alias));
    }

    @Test
    @Title("Изменение имени отправителя на имя с тире")
    @TestCaseId("1847")
    public void testLoginWithHyphenAlias() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_SENDER);
        alias = changesAlias(2, lock.firstAcc());
        user.defaultSteps().shouldContainText(onHomePage().mail360HeaderBlock().userMenu(), getLogin(alias));
        user.messagesSteps().clicksOnMessageWithSubject(subj);
        shouldSeeFromEmail(containsString(alias));
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_SENDER);
        alias = changesAlias(0, lock.firstAcc());
        user.defaultSteps().shouldContainText(onHomePage().mail360HeaderBlock().userMenu(), getLogin(alias));
        user.messagesSteps().clicksOnMessageWithSubject(subj);
        shouldSeeFromEmail(containsString(alias));
    }

    @Test
    @Title("Отмена в попапе сохранения настроек на странице информации об отправителе")
    @TestCaseId("2657")
    public void testCancelChangesOnUserSettingsPagePopUp() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_SENDER);
        alias = changesAlias(0, lock.firstAcc());
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_SENDER);
        user.settingsSteps().selectsEmailAddressFromAlternatives(1);
        user.defaultSteps().opensFragment(QuickFragments.INBOX)
            .shouldSee(onSettingsPage().saveSettingsPopUp())
            .clicksOn(onSettingsPage().saveSettingsPopUp().cancelBtn())
            .shouldNotSee(onSettingsPage().saveSettingsPopUp())
            .opensFragment(QuickFragments.INBOX)
            .clicksOn(onSettingsPage().saveSettingsPopUp().closePopUpBtn())
            .shouldNotSee(onSettingsPage().saveSettingsPopUp())
            .opensFragment(QuickFragments.INBOX)
            .clicksOn(onSettingsPage().saveSettingsPopUp().dontSaveBtn())
            .shouldBeOnUrl(containsString("inbox"))
            .clicksOn(onHomePage().mail360HeaderBlock().userMenu())
            .shouldContainText(onHomePage().userMenuDropdown().currentUserName(), alias);
    }

    @Test
    @Title("Сохранение настроек на странице информации об отправителе")
    @TestCaseId("2657")
    public void testSaveChangesOnUserSettingsPagePopUp() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_SENDER);
        changesAlias(0, lock.firstAcc());
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_SENDER);
        String alias = user.settingsSteps().selectsEmailAddressFromAlternatives(1);
        user.defaultSteps().opensFragment(QuickFragments.INBOX)
            .shouldSee(onSettingsPage().saveSettingsPopUp())
            .clicksOn(onSettingsPage().saveSettingsPopUp().saveAndContinueBtn())
            .shouldNotSee(onSettingsPage().saveSettingsPopUp())
            .refreshPage()
            .clicksOn(onHomePage().mail360HeaderBlock().userMenu())
            .shouldContainText(onHomePage().userMenuDropdown().currentUserName(), alias);
    }

    private String changesAlias(int index, Account acc) {
        alias = user.settingsSteps().selectsEmailAddressFromAlternatives(index);
        user.settingsSteps().saveSettingsIfCanAndClicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(0));
        user.apiMessagesSteps().sendMail(acc, subj, Utils.getRandomString());
        user.defaultSteps().opensDefaultUrl().refreshPage();
        return alias;
    }

    private String getLogin(String alias) {
        String login = alias.substring(0, alias.length() - 6);
        return login;
    }

    @Step("Email должжен удовлетворять формату «{0}»")
    private void shouldSeeFromEmail(Matcher<String> matcher) {
        assertThat(
            "Ожидался иной email отправителя",
            onMessageView().messageHead().fromAddress(),
            and(Utils.isPresent(),
                hasText(matcher))
        );
    }
}
