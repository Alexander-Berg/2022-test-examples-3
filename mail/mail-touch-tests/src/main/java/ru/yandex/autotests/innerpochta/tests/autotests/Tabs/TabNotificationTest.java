package ru.yandex.autotests.innerpochta.tests.autotests.Tabs;

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
import ru.yandex.autotests.innerpochta.util.MailConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.NEWS_TAB;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SOCIAL_TAB;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.NEWS_TAB_RU;
import static ru.yandex.autotests.innerpochta.util.MailConst.SOCIAL_TAB_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX_RU;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на плашки табов")
@Features(FeaturesConst.TABS)
@Stories(FeaturesConst.NOTIFICATION)
@RunWith(DataProviderRunner.class)
public class TabNotificationTest {

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
        steps.user().apiMessagesSteps().sendCoupleMessages(accLock.firstAcc(), 3)
            .moveMessagesToTab(MailConst.NEWS_TAB, steps.user().apiMessagesSteps().getAllMessages().get(0))
            .moveMessagesToTab(MailConst.SOCIAL_TAB, steps.user().apiMessagesSteps().getAllMessages().get(1));
        steps.user().apiSettingsSteps().callWithListAndParams(
            "Включаем табы",
            of(FOLDER_TABS, TRUE)
        );
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Должны перейти по плашке о новых письмах в таб Рассылки")
    @TestCaseId("1288")
    public void shouldGoToTabInboxAfterTapNotifications() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().newsTabNotify())
            .shouldBeOnUrl(containsString(NEWS_TAB.fragment()))
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOnElementWithText(steps.pages().touch().sidebar().tabs(), INBOX_RU)
            .shouldSee(steps.pages().touch().messageList().newsTabNotify());
    }

    @Test
    @Title("Должны перейти по плашке о новых письмах в таб Социальные сети")
    @TestCaseId("1288")
    public void shouldGoToTabSocialTapNotifications() {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().socialTabNotify())
            .shouldBeOnUrl(containsString(SOCIAL_TAB.fragment()))
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOnElementWithText(steps.pages().touch().sidebar().tabs(), INBOX_RU)
            .shouldSee(steps.pages().touch().messageList().socialTabNotify());
    }

    @Test
    @Title("Плашки о новых письмах не должны появляться в табе Рассылки и Социальные сети")
    @TestCaseId("952")
    @DataProvider({"1", "2"})
    public void shouldNotSeeTabNotifications(int numTab) {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().tabs().get(numTab))
            .shouldNotSee(steps.pages().touch().messageList().tabNotify());
    }

    @Test
    @Title("Плашки должна показывать наличие непрочитанных писем в табе Рассылки")
    @TestCaseId("1364")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeNewsNotifyReadAndUnread() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().newsTabNotifyUnread())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOnElementWithText(steps.pages().touch().sidebar().tabs(), NEWS_TAB_RU)
            .clicksOn(
                steps.pages().touch().messageList().messageBlock().unreadToggler(),
                steps.pages().touch().messageList().headerBlock().sidebar()
            )
            .clicksOnElementWithText(steps.pages().touch().sidebar().tabs(), INBOX_RU)
            .shouldSee(steps.pages().touch().messageList().newsTabNotify())
            .shouldNotSee(steps.pages().touch().messageList().newsTabNotifyUnread());
    }

    @Test
    @Title("Плашки должна показывать наличие непрочитанных писем в табе Рассылки")
    @TestCaseId("1364")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeNewsNotifyReadAndUnreadTablet() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().newsTabNotifyUnread())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOnElementWithText(steps.pages().touch().sidebar().tabs(), NEWS_TAB_RU)
            .shouldNotSee(steps.pages().touch().messageList().messageBlock().unreadToggler())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOnElementWithText(steps.pages().touch().sidebar().tabs(), INBOX_RU)
            .shouldSee(steps.pages().touch().messageList().newsTabNotify())
            .shouldNotSee(steps.pages().touch().messageList().newsTabNotifyUnread());
    }

    @Test
    @Title("Плашки должна показывать наличие непрочитанных писем в табе Социальные сети")
    @TestCaseId("1364")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldSeeSocialNotifyReadAndUnread() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().socialTabNotifyUnread())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOnElementWithText(steps.pages().touch().sidebar().tabs(), SOCIAL_TAB_RU)
            .clicksOn(
                steps.pages().touch().messageList().messageBlock().unreadToggler(),
                steps.pages().touch().messageList().headerBlock().sidebar()
            )
            .clicksOnElementWithText(steps.pages().touch().sidebar().tabs(), INBOX_RU)
            .shouldSee(steps.pages().touch().messageList().socialTabNotify())
            .shouldNotSee(steps.pages().touch().messageList().socialTabNotifyUnread());
    }

    @Test
    @Title("Плашки должна показывать наличие непрочитанных писем в табе Социальные сети")
    @TestCaseId("1364")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldSeeSocialNotifyReadAndUnreadTablet() {
        steps.user().defaultSteps().shouldSee(steps.pages().touch().messageList().socialTabNotifyUnread())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOnElementWithText(steps.pages().touch().sidebar().tabs(), SOCIAL_TAB_RU)
            .shouldNotSee(steps.pages().touch().messageList().messageBlock().unreadToggler())
            .clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOnElementWithText(steps.pages().touch().sidebar().tabs(), INBOX_RU)
            .shouldSee(steps.pages().touch().messageList().socialTabNotify())
            .shouldNotSee(steps.pages().touch().messageList().socialTabNotifyUnread());
    }
}
