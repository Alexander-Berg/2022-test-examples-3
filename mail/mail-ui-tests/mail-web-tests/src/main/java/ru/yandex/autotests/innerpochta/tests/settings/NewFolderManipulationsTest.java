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

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * Created by mabelpines on 23.03.16.
 */
@Aqua.Test
@Title("Манипуляции с папкой из настроек")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.FOLDERS_LABELS)
public class NewFolderManipulationsTest extends BaseTest {

    private String folderName = Utils.getRandomString();
    private String newFolderName = Utils.getRandomString();

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
        String foldersFids = user.apiFoldersSteps().getAllFids();
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 3pane-horizontal, раскрываем папки",
            of(
                SETTINGS_PARAM_LAYOUT, SETTINGS_LAYOUT_3PANE_HORIZONTAL,
                FOLDERS_OPEN, foldersFids
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(QuickFragments.SETTINGS_FOLDERS);
    }

    @Test
    @Title("Создаем, переименовываем и удаляем папку из настроек")
    @TestCaseId("1090")
    public void shouldCreateRenameDeleteFolder() {
        user.settingsSteps().createNewFolder(folderName);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.leftColumnSteps().shouldSeeFoldersWithName(folderName);
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS);
        user.settingsSteps().renamesFolder(folderName, newFolderName);
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.leftColumnSteps().shouldNotSeeFoldersWithName(folderName)
            .shouldSeeFoldersWithName(newFolderName);
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FOLDERS);
        user.settingsSteps().clicksOnFolder(newFolderName)
            .clicksOnDeleteFolder();
        user.defaultSteps().opensFragment(QuickFragments.INBOX);
        user.leftColumnSteps().shouldNotSeeFoldersWithName(newFolderName);
    }
}
