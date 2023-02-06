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
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ALIAS_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_DEFAULT_EMAIL;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 18:48
 */

@Aqua.Test
@Title("Выбор различных алиасов")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SENDER_SETTINGS)
public class SenderInfoStorySelectDefaultAddressTest extends BaseTest {

    private String currentEmail;

    public AccLockRule lock = AccLockRule.use().useTusAccount(COLLECTOR);
    public RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        currentEmail = getCurrentEmail();
        user.apiSettingsSteps().callWith(of(SETTINGS_PARAM_DEFAULT_EMAIL, currentEmail));
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Смена адреса на дефолтный")
    @TestCaseId("1852")
    public void testSelectDefaultEmailAddress() {
        user.defaultSteps()
            .shouldContainText(onHomePage().mail360HeaderBlock().userMenu(), lock.firstAcc().getLogin());
        Message msg = user.apiMessagesSteps().sendMailWithNoSave(
            lock.firstAcc(),
            Utils.getRandomString(),
            EMPTY_STR
        );
        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeAddressOnMessageWithSubject(currentEmail, msg.getSubject());
    }

    @Test
    @Title("Смена адреса на адрес из списка")
    @TestCaseId("1851")
    public void testSelectEmailAddressFromCollectors() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_SENDER);
        String alias = user.settingsSteps().selectsEmailAddressFromAlternatives(3);
        user.defaultSteps().clicksOn(onSenderInfoSettingsPage().blockSetupSender().saveButton())
            .refreshPage()
            .opensFragment(QuickFragments.INBOX)
            .clicksOn(onHomePage().mail360HeaderBlock().userMenu())
            .shouldContainText(onHomePage().userMenuDropdown().currentUserName(), alias);
        Message msg = user.apiMessagesSteps().sendMailWithNoSave(
            lock.firstAcc(),
            Utils.getRandomString(),
            EMPTY_STR
        );
        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeAddressOnMessageWithSubject(alias, msg.getSubject());
    }

    private String getCurrentEmail() {
        return lock.firstAcc().getLogin() +
            "@" + ALIAS_LIST.get(1);
    }
}
