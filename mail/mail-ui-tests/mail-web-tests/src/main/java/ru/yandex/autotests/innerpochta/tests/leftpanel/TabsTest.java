package ru.yandex.autotests.innerpochta.tests.leftpanel;

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
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.LEFT_PANEL_COMPACT_SIZE;
import static ru.yandex.autotests.innerpochta.util.MailConst.LEFT_PANEL_FULL_SIZE;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.LabelsConstants.LABELS_PARAM_GREEN_COLOR;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDERS_OPEN;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_3PANE_VERTICAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_LAYOUT_3PANE_HORIZONTAL;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SIZE_LAYOUT_LEFT;

/**
 * * @author mariya-murm
 */
@Aqua.Test
@Title("Тесты на табы в ЛК")
@Features(FeaturesConst.LP)
@Tag(FeaturesConst.LP)
@Stories(FeaturesConst.TABS)
@RunWith(DataProviderRunner.class)
public class TabsTest extends BaseTest {

    private static final String NEWS_TAB_NAME = "Рассылки";
    private static final String CUSTOM_FOLDER = "customFolder";

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
            "Включаем 2pane и разворачиваем ЛК",
            of(
                SIZE_LAYOUT_LEFT, LEFT_PANEL_FULL_SIZE,
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE,
                FOLDER_TABS, TRUE,
                FOLDERS_OPEN, user.apiFoldersSteps().getAllFids()
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.apiFoldersSteps().deleteAllCustomFolders()
            .createNewFolder(CUSTOM_FOLDER);
    }

    @DataProvider
    public static Object[][] userInterface() {
        return new Object[][]{
            {SETTINGS_LAYOUT_3PANE_HORIZONTAL},
            {LAYOUT_3PANE_VERTICAL}
        };
    }

    @Test
    @Title("В ЛК есть табы без эксперимента скролла")
    @TestCaseId("5100")
    public void shouldSeeTabsWithoutScroll() {
        shouldSeeTabs();
        user.defaultSteps().shouldSee(onMessagePage().attachmentsTab());
    }

    @Test
    @Title("Табы есть в 3пейн")
    @TestCaseId("2761")
    @UseDataProvider("userInterface")
    public void shouldSeeTabsIn3Pane(String layout) {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем " + layout,
            of(SETTINGS_PARAM_LAYOUT, layout)
        );
        user.defaultSteps().refreshPage();
        shouldSeeTabs();
        user.defaultSteps().shouldSee(onMessagePage().attachmentsTab());
    }

    @Test
    @Title("Табы есть в свернутой ЛК 2пейн")
    @TestCaseId("5104")
    public void shouldSeeTabsInCompactLC() {
        user.apiLabelsSteps().addNewLabel(getRandomName(), LABELS_PARAM_GREEN_COLOR);
        user.defaultSteps().refreshPage();
        enableCompactLeftPanel();
        shouldSeeTabs();
        user.defaultSteps().shouldSee(
            onMessagePage().attachmentsTab(),
            onMessagePage().compactLeftPanel(),
            onMessagePage().foldersNavigation(),
            onMessagePage().labelsNavigation()
        );
    }

    @Test
    @Title("Табы есть в свернутой ЛК 3пейн")
    @TestCaseId("5104")
    @UseDataProvider("userInterface")
    public void shouldSeeTabsInCompactLC3Pane(String layout) {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем " + layout,
            of(SETTINGS_PARAM_LAYOUT, layout)
        );
        user.apiLabelsSteps().addNewLabel(getRandomName(), LABELS_PARAM_GREEN_COLOR);
        user.defaultSteps().refreshPage()
            .clicksOn(onMessagePage().toolbar().layoutSwitchBtn())
            .turnTrue(onMessagePage().layoutSwitchDropdown().compactLeftColumnSwitch())
            .shouldSee(onMessagePage().compactLeftPanel());
        shouldSeeTabs();
        user.defaultSteps().shouldSee(
            onMessagePage().compactLeftPanel(),
            onMessagePage().foldersNavigation(),
            onMessagePage().labelsNavigation()
        )
            .shouldSee(onMessagePage().attachmentsTab());
    }

    @Test
    @Title("Нажимаем на кол-во непрочитанных писем в табе")
    @TestCaseId("5489")
    public void shoudlSeeUnreadMessagesInTab() {
        fillTab(2);
        user.messagesSteps().selectMessageWithSubject("subj 1");
        user.defaultSteps().clicksOn(onMessagePage().toolbar().markAsReadButton())
            .clicksOn(onMessagePage().inboxTabOnlyNew());
        user.messagesSteps().shouldNotSeeMessageWithSubject("subj 1")
            .shouldSeeMessageWithSubject("subj 2");
    }

    @Test
    @Title("Выключаем табы в настройках")
    @TestCaseId("5496")
    public void shouldTurnOffTabsThroughSettings() {
        shouldSeeTabs();
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_OTHER)
            .deselects(onOtherSettings().blockSetupOther().topPanel().showFoldersTabs())
            .opensFragment(QuickFragments.INBOX)
            .clicksOn(onSettingsPage().saveSettingsPopUp().saveAndContinueBtn())
            .shouldNotSee(onMessagePage().inboxTab());
    }

    @Step("Должны видеть табы «Входящие», «Рассылки», «Соцсети»")
    private void shouldSeeTabs() {
        user.defaultSteps().shouldSee(
            onMessagePage().inboxTab(),
            onMessagePage().newsTab(),
            onMessagePage().socialTab()
        );
    }

    @Step("Заполняем таб письмами")
    private void fillTab(int count) {
        user.apiMessagesSteps().sendCoupleMessages(lock.firstAcc(), count);
        user.defaultSteps().refreshPage();
    }

    @Step("Включаем компактную левую колонку")
    private void enableCompactLeftPanel() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем компактную левую колонку",
            of(SIZE_LAYOUT_LEFT, LEFT_PANEL_COMPACT_SIZE)
        );
        user.defaultSteps().refreshPage();
    }
}
