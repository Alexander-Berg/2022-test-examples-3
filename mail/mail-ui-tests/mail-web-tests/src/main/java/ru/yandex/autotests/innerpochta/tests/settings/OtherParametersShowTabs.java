package ru.yandex.autotests.innerpochta.tests.settings;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
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

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_OTHER;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;

/**
 * @author mariya-murm
 */
@Aqua.Test
@Title("Показывать категории")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.OTHER_SETTINGS)
@RunWith(DataProviderRunner.class)
public class OtherParametersShowTabs extends BaseTest {

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
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 2pane и табы",
            of(
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE,
                FOLDER_TABS, STATUS_ON
            )
        );
    }

    @DataProvider
    public static Object[][] userInterface() {
        return new Object[][]{
            {SETTINGS_LAYOUT_3PANE_HORIZONTAL},
            {LAYOUT_3PANE_VERTICAL}
        };
    }

    @Test
    @Title("Должны видеть табы")
    @TestCaseId("2790")
    public void shouldSeeTabs() {
        user.apiSettingsSteps().callWithListAndParams("Выключаем табы", of(FOLDER_TABS, EMPTY_STR));
        user.loginSteps().forAcc(lock.firstAcc()).logins(SETTINGS_OTHER);
        user.defaultSteps().turnTrue(onOtherSettings().blockSetupOther().topPanel().showFoldersTabs())
            .clicksOn(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .shouldBeSelected(onOtherSettings().blockSetupOther().topPanel().showFoldersTabs())
            .opensFragment(INBOX)
            .shouldSee(onMessagePage().newsTab());
    }

    @Test
    @Title("Не должны видеть табы")
    @TestCaseId("2790")
    public void shouldNotSeeTabs() {
        user.loginSteps().forAcc(lock.firstAcc()).logins(SETTINGS_OTHER);
        user.defaultSteps().deselects(onOtherSettings().blockSetupOther().topPanel().showFoldersTabs())
            .clicksOn(onOtherSettings().blockSetupOther().topPanel().saveButton())
            .shouldBeDeselected(onOtherSettings().blockSetupOther().topPanel().showFoldersTabs())
            .opensFragment(INBOX)
            .shouldNotSee(onMessagePage().newsTab());
    }

    @Test
    @Title("Настройка включения табов есть во всех интерфейсах")
    @TestCaseId("5124")
    @UseDataProvider("userInterface")
    public void shouldSeeTabsSetting(String layout) {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем " + layout,
            of(SETTINGS_PARAM_LAYOUT, layout)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins(SETTINGS_OTHER);
        user.defaultSteps().shouldSee(onOtherSettings().blockSetupOther().topPanel().showFoldersTabs());
    }

}
