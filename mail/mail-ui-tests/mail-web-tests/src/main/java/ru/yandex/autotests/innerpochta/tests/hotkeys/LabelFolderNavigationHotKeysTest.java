package ru.yandex.autotests.innerpochta.tests.hotkeys;


import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.folder.Folder;
import ru.yandex.autotests.innerpochta.steps.beans.label.Label;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;


/**
 * Created with IntelliJ IDEA.
 * User: arttimofeev
 * Date: 19.09.12
 * Time: 19:24
 */
@Aqua.Test
@Title("Тест на хоткеи для навигации по папкам и меткам для стандартного интерфейса")
@Features(FeaturesConst.HOT_KEYS)
@Tag(FeaturesConst.HOT_KEYS)
@Stories(FeaturesConst.GENERAL)
public class LabelFolderNavigationHotKeysTest extends BaseTest {

    private Message msg;
    private Label label;
    private Folder folder;

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
    public void logIn() throws IOException {
        label = user.apiLabelsSteps().addNewLabel(getRandomString(), LABELS_PARAM_GREEN_COLOR);
        folder = user.apiFoldersSteps().createNewFolder(getRandomName());
        msg = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), getRandomName(), "");
        user.apiSettingsSteps().callWithListAndParams(
                "Раскрываем все папки",
                of(FOLDERS_OPEN, user.apiFoldersSteps().getAllFids())
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().selectMessageWithSubject(msg.getSubject());
    }

    @Test
    @Title("m, up, return - навигация по папкам")
    @TestCaseId("1413")
    public void testFoldersNavigationHotKeys() {
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "m");
        user.defaultSteps().shouldSee(onMessagePage().moveMessageDropdownMenu());
        user.hotkeySteps().pressHotKeys(onMessagePage().moveMessageDropdownMenu().input(), Keys.ARROW_DOWN)
            .pressHotKeys(onMessagePage().moveMessageDropdownMenu().input(), Keys.RETURN);
        user.defaultSteps().shouldNotSee(onMessagePage().moveMessageDropdownMenu());
        user.leftColumnSteps().opensCustomFolder(folder.getName());
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());

    }

    @Test
    @Title("l, up, return - навигация по меткам")
    @TestCaseId("1414")
    public void testLabelsNavigationHotKeys() {
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "l");
        user.defaultSteps().shouldSee(onMessagePage().labelsDropdownMenu());
        user.hotkeySteps().pressHotKeys(onMessagePage().labelsDropdownMenu().searchField(), Keys.ARROW_DOWN)
            .pressHotKeys(onMessagePage().labelsDropdownMenu().searchField(), Keys.ARROW_DOWN)
            .pressHotKeys(onMessagePage().labelsDropdownMenu().searchField(), Keys.ARROW_DOWN)
            .pressHotKeys(onMessagePage().labelsDropdownMenu().searchField(), Keys.RETURN);
        user.defaultSteps().shouldNotSee(onMessagePage().labelsDropdownMenu());
        user.messagesSteps().shouldSeeThatMessageIsLabeledWith(label.getName(), msg.getSubject());
    }
}
