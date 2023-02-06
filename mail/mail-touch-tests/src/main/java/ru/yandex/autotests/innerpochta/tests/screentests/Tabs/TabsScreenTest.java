package ru.yandex.autotests.innerpochta.tests.screentests.Tabs;

import com.google.common.collect.Sets;
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
import ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager;
import ru.yandex.autotests.innerpochta.rules.ZombieProxyEnableRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.MailConst;
import ru.yandex.autotests.innerpochta.util.RunAndCompare;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;
import ru.yandex.qatools.ashot.coordinates.Coords;

import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.MESSAGES;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.NEWS_TAB;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.RELEVANT_TAB;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SOCIAL_TAB;
import static ru.yandex.autotests.innerpochta.rules.TouchScreenRulesManager.touchScreenRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.SENT_FOLDER;
import static ru.yandex.autotests.innerpochta.touch.data.ToolbarBtns.INFOLDER;
import static ru.yandex.autotests.innerpochta.util.RunAndCompare.runAndCompare;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;

/**
 * @author oleshko
 */
@Aqua.Test
@Title("Скриночные тесты на табы")
@Features({FeaturesConst.TABS})
@Stories(FeaturesConst.GENERAL)
@RunWith(DataProviderRunner.class)
public class TabsScreenTest {

    private static final String WITH_ATTACHMENTS_LABEL_POSTFIX = "all/only_atta";

    private static final Set<Coords> IGNORED_AREA = Sets.newHashSet(
        new Coords(500, 1950, 460, 60)
    );

    private TouchScreenRulesManager rules = touchScreenRulesManager().withLock(AccLockRule.use().useTusAccount());
    private InitStepsRule stepsProd = rules.getStepsProd();
    private InitStepsRule stepsTest = rules.getStepsTest();
    private AccLockRule acc = rules.getLock();
    private RunAndCompare parallelRun = runAndCompare().withProdSteps(stepsProd).withTestSteps(stepsTest);

    @ClassRule
    public static ZombieProxyEnableRule zomb = new ZombieProxyEnableRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain chain = rules.createTouchRuleChain();

    @DataProvider
    public static Object[][] tabs() {
        return new Object[][]{
            {0, RELEVANT_TAB.fragment()},
            {1, NEWS_TAB.fragment()},
            {2, SOCIAL_TAB.fragment()},
            {3, MESSAGES.fragment(WITH_ATTACHMENTS_LABEL_POSTFIX)},
        };
    }

    @Before
    public void prepare() {
        stepsProd.user().apiSettingsSteps().callWithListAndParams(
            "Включаем табы",
            of(FOLDER_TABS, TRUE)
        );
    }

    @Test
    @Title("Должны увидеть табы в списке папок")
    @TestCaseId("906")
    public void shouldSeeTabsInLeftPanel() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
                .shouldSee(st.pages().touch().sidebar().tabsBlock());

        parallelRun.withActions(actions).withAcc(acc.firstAcc()).withIgnoredAreas(IGNORED_AREA).run();
    }

    @Test
    @Title("Должны увидеть плашки о новых письмах в табах")
    @TestCaseId("913")
    public void shouldSeeTabsNotifications() {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().shouldSee(
                st.pages().touch().messageList().newsTabNotify(),
                st.pages().touch().messageList().socialTabNotify()
            );

        stepsProd.user().apiMessagesSteps().sendCoupleMessages(acc.firstAcc(), 5)
            .moveMessagesToTab(MailConst.NEWS_TAB, stepsProd.user().apiMessagesSteps().getAllMessages().get(1))
            .moveMessagesToTab(MailConst.SOCIAL_TAB, stepsProd.user().apiMessagesSteps().getAllMessages().get(3));
        parallelRun.withActions(actions).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Заглушки в пустых табах")
    @TestCaseId("1066")
    @UseDataProvider("tabs")
    public void shouldSeeEmptyTab(int num, String url) {
        Consumer<InitStepsRule> actions = st ->
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().headerBlock().sidebar())
                .clicksOn(st.pages().touch().sidebar().tabs().get(num))
                .shouldBeOnUrl(containsString(url))
                .shouldSee(st.pages().touch().messageList().emptyFolderImg());

        parallelRun.withActions(actions).withAcc(acc.firstAcc()).run();
    }

    @Test
    @Title("Табы в списке выбора папки")
    @TestCaseId("947")
    public void shouldSeeTabsInFolderPopup() {
        Consumer<InitStepsRule> actions = st -> {
            st.user().touchSteps().rightSwipe(st.pages().touch().messageList().messageBlock());
            st.user().defaultSteps().clicksOn(st.pages().touch().messageList().messageBlock().swipeFirstBtn())
                .shouldSee(st.pages().touch().messageList().popup())
                .clicksOnElementWithText(st.pages().touch().messageView().btnsList(), INFOLDER.btn())
                .shouldSee(st.pages().touch().messageList().folderPopup());
        };
        stepsProd.user().apiMessagesSteps().sendMail(acc.firstAcc(), Utils.getRandomName(), "");
        parallelRun.withActions(actions).withAcc(acc.firstAcc())
            .withUrlPath(FOLDER_ID.makeTouchUrlPart(SENT_FOLDER)).run();
    }
}
