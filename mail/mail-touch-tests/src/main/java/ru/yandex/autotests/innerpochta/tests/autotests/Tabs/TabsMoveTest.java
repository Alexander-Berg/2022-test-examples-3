package ru.yandex.autotests.innerpochta.tests.autotests.Tabs;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
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
import ru.yandex.autotests.innerpochta.util.MailConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.NEWS_TAB;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.RELEVANT_TAB;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.SENT_FOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INFOLDER;
import static ru.yandex.autotests.innerpochta.util.MailConst.NEWS_TAB_RU;
import static ru.yandex.autotests.innerpochta.util.MailConst.SENT_RU;
import static ru.yandex.autotests.innerpochta.util.MailConst.SOCIAL_TAB;
import static ru.yandex.autotests.innerpochta.util.MailConst.SOCIAL_TAB_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на перемещение писем в табах")
@Features(FeaturesConst.TABS)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class TabsMoveTest {

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount());
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
        steps.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), 2)
            .moveMessagesToTab(SOCIAL_TAB, steps.user().apiMessagesSteps().getAllMessages().get(1))
            .moveMessagesToTab(MailConst.NEWS_TAB, steps.user().apiMessagesSteps().getAllMessages().get(0))
            .sendMail(accLock.firstAcc(), Utils.getRandomName(), "");
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Включаем табы",
            of(FOLDER_TABS, TRUE)
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[][] tabsMove() {
        return new Object[][]{
            {RELEVANT_TAB.makeTouchUrlPart(), NEWS_TAB_RU, 1},
            {NEWS_TAB.makeTouchUrlPart(), SENT_RU, 4},
            {FOLDER_ID.makeTouchUrlPart(SENT_FOLDER), SOCIAL_TAB_RU, 2}
        };
    }

    @Test
    @Title("Должны переместить письмо между табами и папками через свайп-меню")
    @TestCaseId("910")
    @UseDataProvider("tabsMove")
    public void shouldMoveMsgInTabsBySwipe(String url, String folder, int num) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(url);
        steps.user().touchSteps().openActionsForMessages(0);
        steps.user().defaultSteps()
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), INFOLDER.btn());
        moveMsg(folder);
        checkMsgs(num);
    }

    @Test
    @Title("Должны переместить письмо между табами и папками через ГО")
    @TestCaseId("963")
    @UseDataProvider("tabsMove")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldMoveMsgInTabsByGroupOperation(String url, String folder, int num) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(url)
            .clicksOn(steps.pages().touch().messageList().messageBlock().avatar())
            .clicksOn(steps.pages().touch().messageList().groupOperationsToolbarPhone().more())
            .clicksOn(steps.pages().touch().messageList().groupOperationsToast().folder());
        moveMsg(folder);
        checkMsgs(num);
    }

    @Test
    @Title("Должны переместить письмо между табами и папками через ГО")
    @TestCaseId("963")
    @UseDataProvider("tabsMove")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldMoveMsgInTabsByGroupOperationTablet(String url, String folder, int num) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(url)
            .clicksOn(steps.pages().touch().messageList().messageBlock().avatar())
            .clicksOn(steps.pages().touch().messageView().groupOperationsToolbarTablet().folder())
            .shouldSee(steps.pages().touch().messageList().folderPopup());
        moveMsg(folder);
        checkMsgs(num);
    }

    @Test
    @Title("Должны переместить письмо между табами и папками из просмотра письма")
    @TestCaseId("964")
    @UseDataProvider("tabsMove")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldMoveMsgInTabsFromMsgView(String url, String folder, int num) {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем треды у пользователя",
            of(SETTINGS_FOLDER_THREAD_VIEW, false)
        );
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(url)
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .clicksOn(steps.pages().touch().messageView().moreBtn())
            .shouldSee(steps.pages().touch().messageView().popup())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), INFOLDER.btn());
        moveMsg(folder);
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageView().header().backToListBtn());
        checkMsgs(num);
    }

    @Test
    @Title("Должны переместить письмо между табами из просмотра письма")
    @TestCaseId("964")
    @UseDataProvider("tabsMove")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldMoveMsgInTabsFromMsgViewTablet(String url, String folder, int num) {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем треды у пользователя",
            of(SETTINGS_FOLDER_THREAD_VIEW, false)
        );
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(url)
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .clicksOn(steps.pages().touch().messageView().moreBtn())
            .shouldSee(steps.pages().touch().messageView().popup())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), INFOLDER.btn());
        moveMsg(folder);
        checkMsgs(num);
    }

    @Step("Перемещаем письмо")
    private void moveMsg(String folder) {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().folderPopup())
            .clicksOnElementWithText(steps.pages().touch().messageList().folderPopup().folders(), folder);
    }

    @Step("Проверяем кол-во писем в папках")
    private void checkMsgs(int num) {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().emptyFolderImg())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().folderBlocks().get(num))
            .shouldSeeElementsCount(steps.pages().touch().messageList().messages().waitUntil(not(empty())), 2);
    }
}
