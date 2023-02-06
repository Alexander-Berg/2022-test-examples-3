package ru.yandex.autotests.innerpochta.tests.autotests.Advertisement;

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
import ru.yandex.autotests.innerpochta.rules.resources.AddFolderIfNeedRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.DELETE;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INFOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INSPAM;
import static ru.yandex.autotests.innerpochta.util.MailConst.OLD_USER_TAG;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SHOW_ADVERTISEMENT_TOUCH;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на рекламу")
@Features(FeaturesConst.ADVERTISEMENT)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class AdAndActWithMsgesTest {

    private TouchRulesManager rules = touchRulesManager().withLock(AccLockRule.use().useTusAccount(OLD_USER_TAG));
    private AccLockRule accLock = rules.getLock();
    private InitStepsRule steps = rules.getSteps();
    private AddFolderIfNeedRule addFolder = AddFolderIfNeedRule.addFolderIfNeed(() -> steps.user());

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain()
        .around(addFolder);

    @Before
    public void prep() {
        steps.user().apiMessagesSteps()
            .sendCoupleMessages(accLock.firstAcc(), 2);
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Включаем показ рекламы",
            of(SHOW_ADVERTISEMENT_TOUCH, TRUE)
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[] buttons() {
        return new Object[][]{
            {INSPAM.btn()},
            {DELETE.btn()}
        };
    }

    @Test
    @Title("Реклама не исчезает при удалении/пометке как спам письма из просмотра письма")
    @TestCaseId("579")
    @UseDataProvider("buttons")
    public void shouldSeeAdvertAfterActionInMsgView(String btnName) {
        openActionsForMsgPopup();
        steps.user().defaultSteps().clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), btnName)
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .shouldSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Реклама не исчезает при перемещении письма из просмотра письма")
    @TestCaseId("625")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeAdvertAfterChangingFolderInMsgView() {
        openActionsForMsgPopup();
        steps.user().defaultSteps().clicksOnElementWithText(
            steps.pages().touch().messageView().btnsList(),
            INFOLDER.btn()
        )
            .clicksOn(steps.pages().touch().messageList().folderPopup().folders().get(0))
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .clicksOn(steps.pages().touch().messageView().header().backToListBtn())
            .shouldSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Реклама не исчезает при перемещении письма из просмотра письма")
    @TestCaseId("625")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeAdvertAfterChnangingFolderInMsgViewTablet() {
        openActionsForMsgPopup();
        steps.user().defaultSteps().clicksOnElementWithText(
            steps.pages().touch().messageView().btnsList(),
            INFOLDER.btn()
        )
            .clicksOn(steps.pages().touch().messageList().folderPopup().folders().get(0))
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .shouldSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Реклама не исчезает при удалении письма из ГО")
    @TestCaseId("580")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeAdvertAfterGroupDelete() {
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().advertisement())
            .clicksOn(
                steps.pages().touch().messageList().messageBlock().avatar(),
                steps.pages().touch().messageList().groupOperationsToolbarPhone().delete()
            )
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .shouldSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Реклама не исчезает при удалении письма из ГО")
    @TestCaseId("580")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeAdvertAfterGroupDeleteTablet() {
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().advertisement())
            .clicksOn(
                steps.pages().touch().messageList().messageBlock().avatar(),
                steps.pages().touch().messageView().groupOperationsToolbarTablet().delete()
            )
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .shouldSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Реклама не исчезает при пометке как спам письма из ГО")
    @TestCaseId("659")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeAdvertAfterGroupSpam() {
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().advertisement())
            .clicksOn(
                steps.pages().touch().messageList().messageBlock().avatar(),
                steps.pages().touch().messageList().groupOperationsToolbarPhone().spam()
            )
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .shouldSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Реклама не исчезает при пометке как спам письма из ГО")
    @TestCaseId("659")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeAdvertAfterGroupSpamTablet() {
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().advertisement())
            .clicksOn(
                steps.pages().touch().messageList().messageBlock().avatar(),
                steps.pages().touch().messageView().groupOperationsToolbarTablet().spam()
            )
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .shouldSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Реклама не исчезает при перемещении письма из ГО")
    @TestCaseId("623")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeAdvertAfterGroupInfolder() {
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().advertisement())
            .clicksOn(
                steps.pages().touch().messageList().messageBlock().avatar(),
                steps.pages().touch().messageList().groupOperationsToolbarPhone().more(),
                steps.pages().touch().messageList().groupOperationsToast().folder()
            )
            .clicksOn(steps.pages().touch().messageList().folderPopup().folders().get(0))
            .shouldSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Реклама не исчезает при перемещении письма из ГО")
    @TestCaseId("623")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeAdvertAfterGroupInfolderTablet() {
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().advertisement())
            .clicksOn(
                steps.pages().touch().messageList().messageBlock().avatar(),
                steps.pages().touch().messageView().groupOperationsToolbarTablet().folder()
            )
            .clicksOn(steps.pages().touch().messageList().folderPopup().folders().get(0))
            .shouldNotSee(steps.pages().touch().messageList().folderPopup())
            .shouldSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Реклама не исчезает при удалении/пометке как спам письма из свайп-меню")
    @TestCaseId("577")
    @UseDataProvider("buttons")
    public void shouldSeeAdvertAfterSwipeAction(String btnName) {
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().advertisement());
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps()
            .offsetClick(steps.pages().touch().messageList().messageBlock().swipeFirstBtn(), 11, 11)
            .shouldSee(steps.pages().touch().messageList().popup())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), btnName)
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .shouldSee(steps.pages().touch().messageList().advertisement());
    }

    @Test
    @Title("Реклама не исчезает при перемещении письма из свайп-меню")
    @TestCaseId("628")
    public void shouldSeeAdvertAfterSwipeActionInFolder() {
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().advertisement());
        steps.user().touchSteps()
            .rightSwipe(steps.pages().touch().messageList().messages().waitUntil(not(empty())).get(0));
        steps.user().defaultSteps()
            .offsetClick(steps.pages().touch().messageList().messageBlock().swipeFirstBtn(), 11, 11)
            .shouldSee(steps.pages().touch().messageList().popup())
            .clicksOnElementWithText(steps.pages().touch().messageView().btnsList(), INFOLDER.btn())
            .clicksOn(steps.pages().touch().messageList().folderPopup().folders().get(0))
            .shouldNotSee(steps.pages().touch().messageList().popup())
            .shouldSee(steps.pages().touch().messageList().advertisement());
    }

    @Step("Открываем попап «Действия с письмом» из просмотра письма")
    private void openActionsForMsgPopup() {
        steps.user().defaultSteps()
            .shouldSee(steps.pages().touch().messageList().messageBlock())
            .clicksOn(steps.pages().touch().messageList().messages().get(0))
            .shouldSee(steps.pages().touch().messageView().toolbar())
            .clicksOn(steps.pages().touch().messageView().moreBtn())
            .shouldSee(steps.pages().touch().messageList().popup());
    }
}
