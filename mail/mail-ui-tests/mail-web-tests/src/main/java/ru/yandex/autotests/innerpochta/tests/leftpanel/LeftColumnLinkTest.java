package ru.yandex.autotests.innerpochta.tests.leftpanel;

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

import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.PASS_COLLECTOR;
import static ru.yandex.autotests.innerpochta.util.MailConst.SERVER_COLLECTOR;

/**
 * * @author yaroslavna
 */
@Aqua.Test
@Title("Тест на ссылки управления папками и метками")
@Features(FeaturesConst.LP)
@Tag(FeaturesConst.LP)
@Stories(FeaturesConst.LEFT_PANEL)
public class LeftColumnLinkTest extends BaseTest {

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
        user.apiCollectorSteps().createNewCollector(
            MAIL_COLLECTOR,
            PASS_COLLECTOR,
            SERVER_COLLECTOR
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Создание папки из левой колонки")
    @TestCaseId("1934")
    public void shouldCreateFolderFromLeftColumn() {
        String folder = Utils.getRandomString();
        user.defaultSteps().onMouseHoverAndClick(onMessagePage().createFolderBtn())
            .inputsTextInElement(onMessagePage().createFolderPopup().folderName(), folder)
            .clicksOn(onMessagePage().createFolderPopup().create());
        user.leftColumnSteps().openFolders()
            .shouldSeeFoldersWithName(folder);
    }

    @Test
    @Title("Создание метки из левой колонки")
    @TestCaseId("1018")
    public void shouldCreateLabelFromLeftColumn() {
        String label = Utils.getRandomString();
        user.defaultSteps().onMouseHoverAndClick(onMessagePage().createLabelBtn())
            .inputsTextInElement(onMessagePage().createLabelPopup().markNameInbox(), label)
            .clicksOn(onMessagePage().createLabelPopup().createMarkButton());
        user.leftColumnSteps().shouldSeeLabelOnHomePage(label);
    }

    @Test
    @Title("Создание метки с фильтром из левой колонки")
    @TestCaseId("1094")
    public void shouldCreateLabelWithFilter() {
        String label = Utils.getRandomString();
        String address = Utils.getRandomString();
        String subject = Utils.getRandomString();
        user.defaultSteps().onMouseHoverAndClick(onMessagePage().createLabelBtn())
            .inputsTextInElement(onMessagePage().createLabelPopup().markNameInbox(), label)
            .clicksOn(onMessagePage().createLabelPopup().filterLink())
            .shouldSee(onFiltersOverview().newFilterPopUp().fromInputBox())
            .inputsTextInElement(onFiltersOverview().newFilterPopUp().fromInputBox(), address)
            .shouldSee(onFiltersOverview().newFilterPopUp().subjectInputBox())
            .inputsTextInElement(onFiltersOverview().newFilterPopUp().subjectInputBox(), subject)
            .clicksOn(onMessagePage().createLabelPopup().createMarkButton());
        user.leftColumnSteps().shouldSeeLabelOnHomePage(label);
    }

    @Test
    @Title("Открываем страницу настроек папок из левой колонки")
    @TestCaseId("1935")
    public void shouldOpenFolderSettingsPage() {
        user.defaultSteps().rightClick(onMessagePage().foldersNavigation().inboxFolderLink())
            .clicksOn(onMessagePage().contextMenuFolder().setupFolders())
            .shouldBeOnUrlWith(QuickFragments.SETTINGS_FOLDERS);
    }

    @Test
    @Title("Открываем настройки сборщиков из КМ")
    @TestCaseId("1938")
    public void shouldOpenCollectorsSettingsFromBtn() {
        user.defaultSteps().rightClick(onMessagePage().collectorsNavigation().collectorsList().get(0))
            .clicksOn(onMessagePage().contextMenuCollectors().addNewCollector())
            .shouldBeOnUrlWith(QuickFragments.SETTINGS_COLLECTORS);
    }

    @Test
    @Title("По клику на «Добавить ящик» открываем настройки")
    @TestCaseId("2912")
    public void shouldOpenCollectorsSettingsFromLink() {
        user.defaultSteps().clicksOn(onMessagePage().addCollectorBtn())
            .shouldBeOnUrlWith(QuickFragments.SETTINGS_COLLECTORS);
    }
}
