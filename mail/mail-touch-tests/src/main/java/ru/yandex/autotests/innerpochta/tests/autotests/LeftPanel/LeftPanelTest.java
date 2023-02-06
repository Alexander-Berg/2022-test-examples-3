package ru.yandex.autotests.innerpochta.tests.autotests.LeftPanel;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.DoTestOnlyForEnvironment;
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INARCHIVE;
import static ru.yandex.autotests.innerpochta.util.MailConst.ARCHIVE_RU;
import static ru.yandex.autotests.innerpochta.util.MailConst.MAIL_BASE_URL;
import static ru.yandex.autotests.innerpochta.util.MailConst.OUTBOX_RU;
import static ru.yandex.autotests.innerpochta.util.MailConst.PASSPORT_AUTH_URL;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.ARCHIVE;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Общие тесты в левой колонке")
@Features(FeaturesConst.LEFT_PANEL)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class LeftPanelTest {

    private static final String OTHER_SERV_TEXT_IN_LEFT_PANEL = "Другие сервисы";
    private static final String EXIT_TEXT = "Выход";
    private static final String OTHER_SERVICES_PAGE_URL = "https://yandex.ru/all";

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount(2));
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @Before
    public void prepare() {
        steps.user().loginSteps().forAcc(accLock.accNum(1)).logins();
    }

    @Test
    @Title("Должны открыть страницу других сервисов")
    @TestCaseId("1371")
    public void shouldOpenOtherServicesPage() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .scrollTo(steps.pages().touch().sidebar().leftPanelItems().get(0))
            .clicksOnElementWithText(steps.pages().touch().sidebar().leftPanelItems(), OTHER_SERV_TEXT_IN_LEFT_PANEL)
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(OTHER_SERVICES_PAGE_URL);
    }

    @Test
    @Title("Разлогиниваемся через Выход в футере")
    @TestCaseId("596")
    public void shouldExit() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .scrollTo(steps.pages().touch().sidebar().leftPanelItems().get(0))
            .clicksOnElementWithText(steps.pages().touch().sidebar().leftPanelItems(), EXIT_TEXT)
            .shouldBeOnUrl(containsString(MAIL_BASE_URL))
            .opensDefaultUrl()
            .shouldBeOnUrl(containsString(PASSPORT_AUTH_URL));
    }

    @Test
    @Title("Разлогиниваемся через Выход в футере при мультиавторизации")
    @TestCaseId("994")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldExitWhenMultiAuth() {
        steps.user().loginSteps().multiLoginWith(accLock.accNum(0));
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .scrollTo(steps.pages().touch().sidebar().leftPanelItems().get(0))
            .clicksOnElementWithText(steps.pages().touch().sidebar().leftPanelItems(), EXIT_TEXT)
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldContainText(steps.pages().touch().sidebar().userEmail(), accLock.accNum(1).getLogin());
    }

    @Test
    @Title("Разлогиниваемся через Выход в футере при мультиавторизации")
    @TestCaseId("994")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldExitWhenMultiAuthTablet() {
        steps.user().loginSteps().multiLoginWith(accLock.accNum(0));
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .scrollTo(steps.pages().touch().sidebar().leftPanelItems().get(0))
            .clicksOnElementWithText(steps.pages().touch().sidebar().leftPanelItems(), EXIT_TEXT)
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldContainText(steps.pages().touch().sidebar().userEmail(), accLock.accNum(1).getLogin());
    }

    @Test
    @Title("Сворачиваем список папок по тапу в свободную область")
    @TestCaseId("308")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldCloseLeftPanel() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().headerBlock())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldSee(steps.pages().touch().sidebar().sidebarAvatar())
            .offsetClick(steps.pages().touch().messageView().rightColumnBox(), 340, 0)
            .shouldNotSee(steps.pages().touch().sidebar().sidebarAvatar());
    }

    @Test
    @Title("Сворачиваем список папок по тапу в свободную область")
    @TestCaseId("308")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldCloseLeftPanelTablet() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().headerBlock())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldSee(steps.pages().touch().sidebar().sidebarAvatar())
            .offsetClick(steps.pages().touch().messageView().rightColumnBox(), 700, 0)
            .shouldNotSee(steps.pages().touch().sidebar().sidebarAvatar());
    }

    @Test
    @Title("Папка «Исходящие» не отображается, если пустая")
    @TestCaseId("1052")
    public void shouldSeeOutboxFolderIfNotEmpty() {
        steps.user().loginSteps().forAcc(accLock.accNum(0)).logins();
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldNotSeeElementInList(steps.pages().touch().sidebar().folderBlocks(), OUTBOX_RU);
        steps.user().apiMessagesSteps().sendMailWithSentTime(accLock.firstAcc(), Utils.getRandomName(), "");
        steps.user().defaultSteps().refreshPage()
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldSeeElementInList(steps.pages().touch().sidebar().folderBlocks(), OUTBOX_RU);
    }

    @Test
    @Title("Создаётся папка «Архив» при первом архивировании письма")
    @TestCaseId("291")
    public void shouldCreateArchive() {
        steps.user().loginSteps().forAcc(accLock.accNum(0)).logins();
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), getRandomString(), "");
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldNotSeeElementInList(steps.pages().touch().sidebar().folderBlocks(), ARCHIVE_RU)
            .refreshPage();
        steps.user().touchSteps().openActionsForMessages(0);
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), INARCHIVE.btn())
            .shouldNotSee(steps.pages().touch().messageList().messageBlock())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .shouldSeeElementInList(steps.pages().touch().sidebar().folderBlocks(), ARCHIVE_RU);
        steps.user().apiMessagesSteps().shouldGetMsgCountViaApi(ARCHIVE, 1);
    }

    @Test
    @Title("Сворачиваем левую колонку при клике в текущую/другую папку")
    @TestCaseId("1125")
    @DataProvider({"0", "1"})
    public void shouldHideLeftPanelByClickOnFolder(int folderNum) {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().folderBlocks().get(folderNum))
            .shouldNotSee(steps.pages().touch().sidebar().folderBlocks());
    }
}
