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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * <p> Created by IntelliJ IDEA.
 * <p> User: lanwen
 * <p> Date: 22.05.12
 * <p> Time: 18:48
 */

@Aqua.Test
@Title("Количество сообщений на странице")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
public class OtherParametersStoryMsgCountTest extends BaseTest {

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    private String folderName;
    private static final String MSG_COUNT = "2";

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws InterruptedException {
        folderName = getRandomName();
        user.apiFoldersSteps().createNewFolder(folderName);
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), 6)
            .moveMessagesFromFolderToFolder(
                folderName,
                user.apiMessagesSteps().getAllMessages().get(0),
                user.apiMessagesSteps().getAllMessages().get(1),
                user.apiMessagesSteps().getAllMessages().get(2)
            );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_OTHER);
    }

    @Test
    @Title("Проверяем работу настройки количества писем на странице")
    @TestCaseId("2548")
    public void shouldSeeMsdPerPageEqualToSettings() {
        user.settingsSteps().entersMessagesPerPageCount(MSG_COUNT);
        user.defaultSteps().clicksOn(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .shouldNotSee(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .refreshPage()
            .opensFragment(QuickFragments.INBOX);
        user.messagesSteps().shouldSeeMsgCount(Integer.parseInt(MSG_COUNT));
        user.leftColumnSteps().openFolders()
            .opensCustomFolder(folderName);
        user.messagesSteps().shouldSeeMsgCount(Integer.parseInt(MSG_COUNT));
    }

    @Test
    @Title("Попап на сохранение настроек на странице «settings/other»")
    @TestCaseId("1797")
    public void testSaveChangesOnSettingsPagePopUp() {
        int count = user.settingsSteps().changesMsgPerPageTo(45);
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
            .opensFragment(QuickFragments.SETTINGS_OTHER)
            .shouldHasValue(onOtherSettings().blockSetupOther().topPanel()
                .messagesPerPage(), Integer.toString(count));
    }
}
