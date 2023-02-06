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
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static ru.yandex.autotests.innerpochta.util.KeysOwn.key;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;

@Aqua.Test
@Title("Тест на хоткеи для навигации по папкам и меткам для 3х панельного интерфейса")
@Features(FeaturesConst.HOT_KEYS)
@Tag(FeaturesConst.HOT_KEYS)
@Stories(FeaturesConst.GENERAL)
public class LabelFolderNavigationHotKeys3PaneTest extends BaseTest {

    private String sbj;
    private Label label;
    private Folder folder;

    public AccLockRule lock = AccLockRule.use().useTusAccount();
    public RestAssuredAuthRule auth = ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void logIn() throws InterruptedException, IOException {
        label = user.apiLabelsSteps().addNewLabel(Utils.getRandomString(), LABELS_PARAM_GREEN_COLOR);
        folder = user.apiFoldersSteps().createNewFolder(Utils.getRandomName());
        sbj = user.apiMessagesSteps().sendMailWithNoSave(lock.firstAcc(), Utils.getRandomName(), "").getSubject();
        user.apiSettingsSteps().callWithListAndParams(
                "Раскрываем все папки, включаем 3-PANE",
                of(
                        FOLDERS_OPEN, user.apiFoldersSteps().getAllFids(),
                        SETTINGS_PARAM_LAYOUT, LAYOUT_3PANE_VERTICAL
                )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().clicksOnMessageWithSubject(sbj);
        user.hotkeySteps().pressSimpleHotKey(key(Keys.ARROW_UP));
        user.defaultSteps().shouldSee(onMessagePage().toolbar());
    }

    @Test
    @Title("m, up, return - навигация по папкам в 3pane")
    @TestCaseId("1411")
    public void testFoldersNavigationHotKeys3Pane() {
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "m")
            .setSearchFieldForLabelsAsDestination()
            .pressSimpleHotKey2(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey2(key(Keys.RETURN));
        user.leftColumnSteps().opensCustomFolder(folder.getName());
        user.messagesSteps().shouldSeeMessageWithSubject(sbj);
    }

    @Test
    @Title("l, up, return - навигация по меткам в 3pane") //фокус не на списке писем
    @TestCaseId("1412")
    public void testLabelsNavigationHotKeys3Pane() {
        user.hotkeySteps().pressCombinationOfHotKeys(key(Keys.SHIFT), "l")
            .setSearchFieldForLabelsAsDestination()
            .pressSimpleHotKey2(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey2(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey2(key(Keys.ARROW_DOWN))
            .pressSimpleHotKey2(key(Keys.RETURN));
        user.messagesSteps().shouldSeeThatMessageIsLabeledWith(label.getName(), sbj);
    }
}
