package ru.yandex.autotests.innerpochta.tests.autotests.Search;

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
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_RQST;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_TOUCH;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.SENT_RU;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на расширенный поиск")
@Features(FeaturesConst.SEARCH)
@Stories({FeaturesConst.FILTERS})
@RunWith(DataProviderRunner.class)
public class AdvancedSearchTest {

    private static final String SEARCH_INPUT = "test";
    private static final String SEARCH_PART_FOLDER = "&search_fid=4";

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
    public void prep() {
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), SEARCH_INPUT, "");
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Должны закрыть попапы с фильтрами")
    @TestCaseId("1490")
    @DoTestOnlyForEnvironment("Phone")
    @DataProvider({"1", "2", "3", "4", "7"})
    public void shouldCloseMoreFiltersPopup(int num) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))
        )
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(num))
            .clicksOn(steps.pages().touch().search().advancedSearchPopupClose())
            .shouldNotSee(
                steps.pages().touch().search().advancedSearchMorePopup(),
                steps.pages().touch().search().advancedSearchFolderPopup(),
                steps.pages().touch().search().advancedSearchDatesPopup(),
                steps.pages().touch().search().advancedSearchContactsPopup()
            );
    }

    @Test
    @Title("Должны закрыть попапы с фильтрами")
    @TestCaseId("1490")
    @DoTestOnlyForEnvironment("Tablet")
    @DataProvider({"1", "2", "3", "4", "5"})
    public void shouldCloseMoreFiltersPopupTablet(int num) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))
        )
            .clicksOn(steps.pages().touch().search().advancedSearchFiltersBtn())
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(num))
            .clicksOn(steps.pages().touch().search().advancedSearchPopupClose())
            .shouldNotSee(
                steps.pages().touch().search().advancedSearchMorePopup(),
                steps.pages().touch().search().advancedSearchFolderPopup(),
                steps.pages().touch().search().advancedSearchDatesPopup(),
                steps.pages().touch().search().advancedSearchContactsPopup()
            );
    }

    @Test
    @Title("Должны фильтровать по текущей папке")
    @TestCaseId("1494")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldChooseFolderByDefault() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            String.format(
                "%s%s",
                SEARCH_TOUCH.makeTouchUrlPart(),
                SEARCH_RQST.fragment(SEARCH_INPUT + SEARCH_PART_FOLDER)
            )
        )
            .shouldSeeThatElementHasText(
                steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(2),
                SENT_RU
            )
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(2))
            .shouldSeeThatElementHasText(steps.pages().touch().search().selectedFolderInFolderPopup(), SENT_RU);
    }

    @Test
    @Title("Должны фильтровать по текущей папке")
    @TestCaseId("1494")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldChooseFolderByDefaultTablet() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            String.format(
                "%s%s",
                SEARCH_TOUCH.makeTouchUrlPart(),
                SEARCH_RQST.fragment(SEARCH_INPUT + SEARCH_PART_FOLDER)
            )
        )
            .clicksOn(steps.pages().touch().search().advancedSearchFiltersBtn())
            .shouldSeeThatElementHasText(
                steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(2),
                SENT_RU
            )
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(2))
            .shouldSeeThatElementHasText(steps.pages().touch().search().selectedFolderInFolderPopup(), SENT_RU);
    }

    @Test
    @Title("Должны найти папку в фильтре по папкам")
    @TestCaseId("1493")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldFindFolderInFolderPopup() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))
        )
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(2))
            .clicksOn(steps.pages().touch().search().headerInFolderPopup())
            .inputsTextInElement(steps.pages().touch().search().inputInFolderPopup(), SENT_RU)
            .shouldSeeElementsCount(steps.pages().touch().search().foldersInPopupFolder(), 1)
            .clicksOn(steps.pages().touch().search().foldersInPopupFolder().get(0))
            .shouldSeeThatElementHasText(
                steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(2),
                SENT_RU
            );
    }

    @Test
    @Title("Должны найти папку в фильтре по папкам")
    @TestCaseId("1493")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldFindFolderInFolderPopupTablet() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))
        )
            .clicksOn(steps.pages().touch().search().advancedSearchFiltersBtn())
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(2))
            .clicksOn(steps.pages().touch().search().headerInFolderPopup())
            .inputsTextInElement(steps.pages().touch().search().inputInFolderPopup(), SENT_RU)
            .shouldSeeElementsCount(steps.pages().touch().search().foldersInPopupFolder(), 1)
            .clicksOn(steps.pages().touch().search().foldersInPopupFolder().get(0))
            .clicksOn(steps.pages().touch().search().advancedSearchFiltersBtn())
            .shouldSeeThatElementHasText(
                steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(2),
                SENT_RU
            );
    }
}
