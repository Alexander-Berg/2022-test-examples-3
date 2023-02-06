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
import ru.yandex.autotests.innerpochta.data.QuickFragments;
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
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.INBOX_FOLDER;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.NEWS_TAB;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.RELEVANT_TAB;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_TOUCH_PART;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.SENT_FOLDER;
import static ru.yandex.autotests.innerpochta.util.MailConst.NEWS_TAB_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.STATUS_ON;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Общие тесты на табы")
@Features(FeaturesConst.TABS)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class TabsTest {

    private static final String GENERAL = "general";

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
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Включаем табы",
            of(FOLDER_TABS, TRUE)
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[][] msgDetailText() {
        return new Object[][]{
            {STATUS_ON, NEWS_TAB, NEWS_TAB_RU},
            {EMPTY_STR, INBOX_FOLDER, INBOX_RU}
        };
    }

    @Test
    @Title("Должны отключить настройку табов")
    @TestCaseId("998")
    public void shouldTurnOffTabs() {
        switchTabsSetting();
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().settings().tabsTogglerOn())
            .clicksOn(steps.pages().touch().settings().closeBtn())
            .clicksIfCanOn(steps.pages().touch().settings().closeBtn())
            .shouldNotSee(steps.pages().touch().sidebar().tabsBlock());
    }

    @Test
    @Title("Должны включить настройку табов")
    @TestCaseId("998")
    public void shouldTurnOnTabs() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем табы",
            of(FOLDER_TABS, FALSE)
        );
        switchTabsSetting();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().settings().tabsTogglerOn())
            .clicksOn(steps.pages().touch().settings().closeBtn())
            .clicksIfCanOn(steps.pages().touch().settings().closeBtn())
            .shouldSee(steps.pages().touch().sidebar().tabsBlock());
    }

    @Test
    @Title("С touch/folder/1 редиректит на touch/tab/relevant")
    @TestCaseId("948")
    @DoTestOnlyForEnvironment("Phone") //QUINN-6627
    public void shouldRedirectToDefaultTab() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(INBOX_FOLDER.makeTouchUrlPart())
            .shouldBeOnUrl(containsString(RELEVANT_TAB.fragment()));
    }

    @Test
    @Title("С touch/tab/relevant редиректит на touch/folder/1")
    @TestCaseId("992")
    public void shouldRedirectToInbox() {
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Выключаем табы",
            of(FOLDER_TABS, FALSE)
        );
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(RELEVANT_TAB.makeTouchUrlPart())
            .shouldBeOnUrl(containsString(INBOX_FOLDER.fragment()));
    }

    @Test
    @Title("Указываем верный таб в деталях письма")
    @TestCaseId("1140")
    @UseDataProvider("msgDetailText")
    public void shouldSeeTabInMsgDetails(String setting, QuickFragments urlPart, String text) {
        steps.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), 3)
            .moveMessagesToTab(MailConst.NEWS_TAB, steps.user().apiMessagesSteps().getAllMessages().get(0))
            .moveMessagesToTab(MailConst.SOCIAL_TAB, steps.user().apiMessagesSteps().getAllMessages().get(1));
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Включаем/Выключаем табы",
            of(FOLDER_TABS, setting)
        );
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(urlPart.makeTouchUrlPart())
            .clicksOn(steps.pages().touch().messageList().messageBlock())
            .clicksOn(steps.pages().touch().messageView().toolbar())
            .shouldContainText(steps.pages().touch().messageView().msgDetailsFields().get(2), text);
    }

    @Test
    @Title("Должны открыть письмо в табе из статуслайна о новом письме")
    @TestCaseId("949")
    public void shouldOpenMsgInTabFromNewMsgStatusline() {
        String subj = Utils.getRandomName();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(FOLDER_ID.makeTouchUrlPart(SENT_FOLDER));
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), subj, "");
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().statusLineNewMsg())
            .shouldSeeThatElementHasText(steps.pages().touch().messageView().threadHeader(), subj)
            .shouldBeOnUrl(containsString(RELEVANT_TAB.fragment()));
    }

    @Step("Открываем настройку, переключаем тумблер табов")
    private void switchTabsSetting() {
        steps.user().defaultSteps().opensCurrentUrlWithPostFix(SETTINGS_TOUCH_PART.makeTouchUrlPart(GENERAL))
            .clicksOn(steps.pages().touch().settings().tabsToggler());
    }
}