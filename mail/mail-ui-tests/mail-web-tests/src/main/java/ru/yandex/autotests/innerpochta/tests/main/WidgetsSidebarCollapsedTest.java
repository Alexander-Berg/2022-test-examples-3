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

import static ru.yandex.autotests.innerpochta.ns.pages.homepageblocks.WidgetsSidebarCollapsed.IFRAME_WIDGETS;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_TO_COLLAPSE_WIDGETS;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_TO_UPDATE_WIDGETS;

/**
 * @author eremin-n-s
 */
@Aqua.Test
@Title("Свернутый сайдбар виджетов")
@Features(FeaturesConst.MAIN)
@Tag(FeaturesConst.MAIN)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class WidgetsSidebarCollapsedTest extends BaseTest {

    private static String TELEMOST_URL = "telemost.yandex.ru/";
    private static String DISK_ULR = "disk.yandex.ru/edit/disk/disk";

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
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().executesJavaScript(SCRIPT_TO_COLLAPSE_WIDGETS + SCRIPT_TO_UPDATE_WIDGETS)
            .refreshPage()
            .switchTo(IFRAME_WIDGETS);
    }

    @Test
    @Title("Разворачиваем сайдбар виджетов")
    @TestCaseId("6173")
    public void shouldExpandWidgetsSidebar() {
        user.defaultSteps().clicksOn(onMessagePage().widgetsSidebarCollapsed().expandBtn())
            .shouldSee(onMessagePage().widgetsSidebarExpanded());
    }

    @Test
    @Title("Нажимаем на виджет заметок")
    @TestCaseId("6201")
    public void shouldClickOnNotesWidget() {
        user.defaultSteps().clicksOn(onMessagePage().widgetsSidebarCollapsed().notes())
            .shouldSee(onMessagePage().widgetsSidebarExpanded());
    }

    @Test
    @Title("Нажимаем на виджет телемоста")
    @TestCaseId("6200")
    public void shouldClickOnTelemostWidget() {
        user.defaultSteps().clicksOn(onMessagePage().widgetsSidebarCollapsed().telemost())
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(TELEMOST_URL);
    }

    @Test
    @Title("Нажимаем на создание документа в виджете диска")
    @TestCaseId("6202")
    public void shouldClickOnCreateDoc() throws UnsupportedEncodingException {
        user.defaultSteps().clicksOn(onMessagePage().widgetsSidebarCollapsed().diskBtns().get(0))
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(DISK_ULR)
            .shouldContainTextInUrl(URLEncoder.encode("Документ", "UTF-8"));
    }

    @Test
    @Title("Нажимаем на создание таблицы в виджете диска")
    @TestCaseId("6202")
    public void shouldClickOnCreateTable() throws UnsupportedEncodingException {
        user.defaultSteps().clicksOn(onMessagePage().widgetsSidebarCollapsed().diskBtns().get(1))
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(DISK_ULR)
            .shouldContainTextInUrl(URLEncoder.encode("Таблица", "UTF-8"));
    }

    @Test
    @Title("Нажимаем на создание презентации в виджете диска")
    @TestCaseId("6202")
    public void shouldClickOnCreatePresentation() throws UnsupportedEncodingException {
        user.defaultSteps().clicksOn(onMessagePage().widgetsSidebarCollapsed().diskBtns().get(2))
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(DISK_ULR)
            .shouldContainTextInUrl(URLEncoder.encode("Презентация", "UTF-8"));
    }
}
