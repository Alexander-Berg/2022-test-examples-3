package ru.yandex.autotests.innerpochta.tests.contextmenu;

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

@Aqua.Test
@Title("Проверяем пункт “Открыть в новой вкладке“ для писем от сборщика")
@Features("ContextMenu")
@Tag(FeaturesConst.CONTEXT_MENU)
@Stories("Общие тесты")
public class ContextOpenInNewWindowFromCollectorTest extends BaseTest {

    private static final String OPEN_IN_NEW_TAB_MENU_ITEM = "Открыть в новой вкладке";
    private static final String COLLECTOR_FOLDER_TITLE = "Письма из ящика «ContextForward@yandex.ru»";
    private static final String SEARCH_PARAM = "request=ContextForward@imap.yandex.ru";
    private static final String ACCOUNT_WITH_COLLECTOR = "ContextReplyTestCollector";
    private static final String CREDS = "ContextOpenInNewWindowFromCollectorTest";

    public AccLockRule lock = AccLockRule.use().names(CREDS, ACCOUNT_WITH_COLLECTOR);
    public RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth);

    @Before
    public void login() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Открываем письма от сборщика")
    @TestCaseId("1260")
    public void shouldOpenCollector() {
        user.defaultSteps().rightClick(onMessagePage().collectorsNavigation().collectorsList().get(0));
        user.messagesSteps().shouldSeeContextMenu();
        user.defaultSteps().clicksOnElementWithText(
            onMessagePage().allMenuList().get(0).itemList(),
            OPEN_IN_NEW_TAB_MENU_ITEM
        )
            .switchOnJustOpenedWindow();
        user.messagesSteps().shouldSeeTitleOfCollectorFolder(COLLECTOR_FOLDER_TITLE);
        user.defaultSteps().shouldBeOnUrlWith(QuickFragments.SEARCH)
            .shouldBeOnUrl(containsString(SEARCH_PARAM));
    }

    @Test
    @Title("Пункт “Ответить“ в КМ для письма из сборщика подставляет нужного отправителя")
    @TestCaseId("2478")
    public void shouldOpenReplyPageForCollectorMessage() {
        user.loginSteps().forAcc(lock.acc(ACCOUNT_WITH_COLLECTOR)).logins();
        String collectorAddress = onMessagePage().collectorsNavigation().collectorsList().get(0).getText();
        user.defaultSteps().clicksOn(onMessagePage().collectorsNavigation().collectorsList().get(0))
            .waitInSeconds(3); // ждём загрузки нового списка писем
        user.messagesSteps().rightClickOnMessageWithSubject(
            onMessagePage().displayedMessages().list().get(0).subject().getText()
        )
            .shouldSeeContextMenuInMsgList();
        user.defaultSteps().clicksOn(onMessagePage().allMenuListInMsgList().get(0).reply())
            .shouldSee(user.pages().ComposePopup().expandedPopup())
            .clicksOn(user.pages().ComposePopup().expandedPopup().expandCollapseBtn())
            .clicksOn(user.pages().ComposePopup().yabbleFrom())
            .shouldSeeThatElementTextEquals(onComposePopup().yabbleDropdown().yabbleEmail(), collectorAddress);
    }
}
