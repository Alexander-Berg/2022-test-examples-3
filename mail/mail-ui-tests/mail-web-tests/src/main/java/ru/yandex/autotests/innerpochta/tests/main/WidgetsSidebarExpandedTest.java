package ru.yandex.autotests.innerpochta.tests.main;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.WidgetsSidebarCollapsed.IFRAME_WIDGETS;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_TO_EXPAND_WIDGETS;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_TO_UPDATE_WIDGETS;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_YES;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.IS_SIDEBAR_EXPANDED;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Развернутый сайдбар виджетов")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class WidgetsSidebarExpandedTest extends BaseTest {

    private static String TELEMOST_URL = "telemost.yandex.ru";
    private static String TELEMOST_MEETING_URL = "telemost.yandex.ru/";
    private static String DISK_URL = "disk.yandex.ru/client/disk";
    private static String DOCS_URL = "disk.yandex.ru/edit/disk/";
    private static String NOTES_URL = "disk.yandex.ru/notes/";

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
        user.apiSettingsSteps().callWithListAndParams(
            "Разворачиваем сайдбар виджетов",
            of(IS_SIDEBAR_EXPANDED, STATUS_YES)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().executesJavaScript(SCRIPT_TO_EXPAND_WIDGETS + SCRIPT_TO_UPDATE_WIDGETS)
            .refreshPage()
            .switchTo(IFRAME_WIDGETS);
    }

    @Test
    @Title("Сворачиваем сайдбар виджетов")
    @TestCaseId("6174")
    public void shouldCollapseWidgetsSidebar() {
        user.defaultSteps()
            .clicksOn(onMessagePage().widgetsSidebarExpanded().collapseBtn())
            .shouldSee(onMessagePage().widgetsSidebarCollapsed());
    }

    @Test
    @Title("Нажимаем на иконку виджета телемоста")
    @TestCaseId("6192")
    public void shouldClickOnTelemostWidgetIcon() {
        user.defaultSteps().clicksOn(onMessagePage().widgetsSidebarExpanded().telemostIcon())
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(TELEMOST_URL);
    }

    @Test
    @Title("Создаем видеовстречу")
    @TestCaseId("6196")
    public void shouldCreateVideoMeeting() {
        user.defaultSteps().clicksOn(onMessagePage().widgetsSidebarExpanded().telemostBtn())
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(TELEMOST_MEETING_URL);
    }

    @Test
    @Title("Нажимаем на иконку виджета диска")
    @TestCaseId("6194")
    public void shouldClickOnDiskWidgetIcon() {
        user.defaultSteps().clicksOn(onMessagePage().widgetsSidebarExpanded().diskIcon())
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(DISK_URL);
    }

    @Test
    @Title("Создаем документ")
    @TestCaseId("6211")
    public void shouldCreateDocument() throws UnsupportedEncodingException {
        user.defaultSteps().clicksOn(onMessagePage().widgetsSidebarExpanded().diskPlusBtn())
            .clicksOn(onMessagePage().widgetsSidebarExpanded().diskDocBtn())
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(DOCS_URL)
            .shouldContainTextInUrl(URLEncoder.encode("Документ", "UTF-8"));
    }

    @Test
    @Title("Создаем таблицу")
    @TestCaseId("6212")
    public void shouldCreateTable() throws UnsupportedEncodingException {
        user.defaultSteps().clicksOn(onMessagePage().widgetsSidebarExpanded().diskPlusBtn())
            .clicksOn(onMessagePage().widgetsSidebarExpanded().diskXlsxBtn())
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(DOCS_URL)
            .shouldContainTextInUrl(URLEncoder.encode("Таблица", "UTF-8"));
    }

    @Test
    @Title("Создаем презентацию")
    @TestCaseId("6213")
    public void shouldCreatePresentation() throws UnsupportedEncodingException {
        user.defaultSteps().clicksOn(onMessagePage().widgetsSidebarExpanded().diskPlusBtn())
            .clicksOn(onMessagePage().widgetsSidebarExpanded().diskPptxBtn())
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(DOCS_URL)
            .shouldContainTextInUrl(URLEncoder.encode("Презентация", "UTF-8"));
    }

    @Test
    @Title("Нажимаем на иконку виджета заметок")
    @TestCaseId("6193")
    public void shouldClickOnNotesWidgetIcon() {
        user.defaultSteps().clicksOn(onMessagePage().widgetsSidebarExpanded().notesIcon())
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(NOTES_URL);
    }

    @Test
    @Title("Нажимаем на крестик в углу виджета заметок")
    @TestCaseId("6197")
    public void shouldClickOnPlusButtonNotesWidget() {
        user.defaultSteps().clicksOn(onMessagePage().widgetsSidebarExpanded().notesPlusBtn())
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(NOTES_URL);
    }
}
