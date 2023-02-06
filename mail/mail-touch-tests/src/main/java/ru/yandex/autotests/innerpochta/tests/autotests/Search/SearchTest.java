package ru.yandex.autotests.innerpochta.tests.autotests.Search;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
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
import static ru.yandex.autotests.innerpochta.data.QuickFragments.FOLDER_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.LABEL_ID;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_TOUCH;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.IMPORTANT_LABEL;
import static ru.yandex.autotests.innerpochta.touch.data.FidsAndLids.SENT_FOLDER;

/**
 * @author puffyfloof
 */

@Aqua.Test
@Title("Все про поиск")
@Features(FeaturesConst.SEARCH)
@Stories({FeaturesConst.GENERAL})
@RunWith(DataProviderRunner.class)
public class SearchTest {

    //Подготовленные у пользователя письма, папки, метки
    private static final String MESSAGE_SUBJECT = "Let's search this";
    private static final String SPECIAL_SYMBOLS = "!@#$%^&(*)№;%:?/\\|";

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
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), MESSAGE_SUBJECT, "");
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @DataProvider
    public static Object[] urls() {
        return new Object[][]{
            {FOLDER_ID, SENT_FOLDER},
            {LABEL_ID, IMPORTANT_LABEL}
        };
    }

    @Test
    @Title("Должны перейти в поиск из шапки списка писем")
    @TestCaseId("841")
    public void shouldOpenSearch() {
        steps.user().defaultSteps()
            .clicksOn(steps.pages().touch().messageList().headerBlock().search())
            .shouldSee(steps.pages().touch().search().header());
    }

    @Test
    @Title("При повторном заходе в поиск предыдущий сбрасывается")
    @TestCaseId("382")
    public void shouldNotSeeRemainsOfPreviousSearch() {
        String query = Utils.getRandomString();
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SEARCH_TOUCH.makeTouchUrlPart())
            .inputsTextInElement(steps.pages().touch().search().header().input(), query)
            .clicksOn(
                steps.pages().touch().searchResult().header().back(),
                steps.pages().touch().messageList().headerBlock().search()
            )
            .shouldNotHasText(steps.pages().touch().search().header().input(), query);
    }

    @Test
    @Title("При выходе из поиска должны вернуться в ту же папку")
    @TestCaseId("302")
    @UseDataProvider("urls")
    public void shouldBackFromSearchToExactFolder(QuickFragments fragment ,String urlPart) {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(fragment.makeTouchUrlPart(urlPart))
            .clicksOn(steps.pages().touch().messageList().headerBlock().search())
            .clicksOn(steps.pages().touch().search().header().back())
            .shouldBeOnUrl(containsString(fragment.makeTouchUrlPart(urlPart)));
    }

    @Test
    @Title("Должен появиться саджест для запроса вставленного из буфера обмена")
    @TestCaseId("831")
    public void shouldSeeSuggestAfterInsert() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SEARCH_TOUCH.makeTouchUrlPart())
            .inputsTextInElement(steps.pages().touch().search().header().input(), MESSAGE_SUBJECT);
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().touch().search().header().input(),
            Keys.chord(Keys.CONTROL, "a")
        );
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().touch().search().header().input(),
            Keys.chord(Keys.CONTROL, "x")
        );
        steps.user().defaultSteps().shouldNotSee(steps.pages().touch().search().groupSuggestTitle());
        steps.user().hotkeySteps().pressHotKeysWithDestination(
            steps.pages().touch().search().header().input(),
            Keys.chord(Keys.CONTROL, "v")
        );
        steps.user().defaultSteps().shouldSee(steps.pages().touch().search().groupSuggestTitle());
    }

    @Test
    @Title("Должны получить выдачу по поисковому запросу без использования саджеста")
    @TestCaseId("98")
    public void shouldSearchWithoutSuggest() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SEARCH_TOUCH.makeTouchUrlPart())
            .inputsTextInElement(steps.pages().touch().search().header().input(), MESSAGE_SUBJECT)
            .clicksOn(steps.pages().touch().search().header().find())
            .shouldSeeThatElementHasText(steps.pages().touch().search().messageBlock(), MESSAGE_SUBJECT);
    }

    @Test
    @Title("Спецсимволы не ломают поиск")
    @TestCaseId("1121")
    public void shouldFindMsgWithSymbol() {
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), Utils.getRandomString(), "");
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SEARCH_TOUCH.makeTouchUrlPart())
            .inputsTextInElement(steps.pages().touch().search().header().input(), SPECIAL_SYMBOLS)
            .clicksOn(steps.pages().touch().search().header().find())
            .shouldSee(steps.pages().touch().search().messageBlock());
    }

    @Test
    @Title("Back возвращает из поисковой выдачи к zs")
    @TestCaseId("960")
    public void shouldBackToZS() {
        steps.user().apiMessagesSteps().sendMailWithNoSave(accLock.firstAcc(), MESSAGE_SUBJECT, "");
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(SEARCH_TOUCH.makeTouchUrlPart())
            .inputsTextInElement(steps.pages().touch().search().header().input(), MESSAGE_SUBJECT)
            .clicksOn(steps.pages().touch().search().header().find())
            .shouldSee(steps.pages().touch().search().messageBlock());
        steps.getDriver().navigate().back();
        steps.user().defaultSteps().shouldSee(steps.pages().touch().search().suggest());
    }
}
