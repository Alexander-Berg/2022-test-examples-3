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
import ru.yandex.autotests.innerpochta.rules.InitStepsRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.TouchRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MESSAGES;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.NEWS_TAB;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.RELEVANT_TAB;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SOCIAL_TAB;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Тесты на открытие табов из списка папок")
@Features(FeaturesConst.TABS)
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class OpenTabsTest {

    private static final String ATTACH_URL_PART = "all/only_atta";

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
    public static Object[][] diffTabs() {
        return new Object[][]{
            {0, RELEVANT_TAB.fragment()},
            {1, NEWS_TAB.fragment()},
            {2, SOCIAL_TAB.fragment()},
            {3, MESSAGES.fragment(ATTACH_URL_PART)}
        };
    }

    @Test
    @Title("Должны перейти в таб из списка папок")
    @TestCaseId("907")
    @UseDataProvider("diffTabs")
    public void shouldGoToTabFromLeftPanel(int tabNum, String url) {
        steps.user().defaultSteps().clicksOn(steps.pages().touch().messageList().headerBlock().sidebar())
            .clicksOn(steps.pages().touch().sidebar().tabs().get(tabNum))
            .shouldBeOnUrl(containsString(url));
    }
}
