package ru.yandex.autotests.innerpochta.tests.autotests.Search;

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

import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_RQST;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH_TOUCH;
import static ru.yandex.autotests.innerpochta.rules.TouchRulesManager.touchRulesManager;
import static ru.yandex.autotests.innerpochta.util.MailConst.IMAGE_ATTACHMENT;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author oleshko
 */

@Aqua.Test
@Title("Тесты на применение фильтров расширенного поиска")
@Features(FeaturesConst.SEARCH)
@Stories({FeaturesConst.FILTERS})
@RunWith(DataProviderRunner.class)
public class AdvancedSearchApplyFIltersTest {

    private static final String SEARCH_INPUT = "test";

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
        steps.user().apiMessagesSteps().sendMailWithAttachmentsAndHTMLBody(
            accLock.firstAcc().getSelfEmail(),
            Utils.getRandomString(),
            "",
            IMAGE_ATTACHMENT
        );
        steps.user().apiLabelsSteps().markImportant(
            steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), Utils.getRandomString(), "")
        );
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), Utils.getRandomString(), SEARCH_INPUT);
        steps.user().apiMessagesSteps().sendMail(accLock.firstAcc(), SEARCH_INPUT, "");
        steps.user().apiMessagesSteps().markAllMsgRead()
            .sendMailWithNoSave(accLock.firstAcc(), Utils.getRandomString(), "unread");
        steps.user().loginSteps().forAcc(accLock.firstAcc()).logins();
    }

    @Test
    @Title("Должны применить фильтр «С вложениями»")
    @TestCaseId("1480")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldUseFilterWithAttachment() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(String.format(
            "%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(accLock.firstAcc().getSelfEmail())
            )
        )
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(0))
            .shouldSeeElementsCount(steps.pages().touch().searchResult().messages(), 1)
            .shouldSee(steps.pages().touch().searchResult().messages().get(0).attachmentsInMessageList());
    }

    @Test
    @Title("Должны применить фильтр «С вложениями»")
    @TestCaseId("1480")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldUseFilterWithAttachmentTablet() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(String.format(
            "%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(accLock.firstAcc().getSelfEmail())
            )
        )
            .clicksOn(steps.pages().touch().search().advancedSearchFiltersBtn())
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(0))
            .shouldSeeElementsCount(steps.pages().touch().searchResult().messages(), 1)
            .shouldSee(steps.pages().touch().searchResult().messages().get(0).attachmentsInMessageList());
    }

    @Test
    @Title("Должны применить фильтр «Важные»")
    @TestCaseId("1485")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldUseFilterImportant() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(String.format(
            "%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(accLock.firstAcc().getSelfEmail())
            )
        )
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(5))
            .shouldSeeElementsCount(steps.pages().touch().searchResult().messages(), 1)
            .shouldSee(steps.pages().touch().searchResult().messages().get(0).importantLabel());
    }

    @Test
    @Title("Должны применить фильтр «Важные»")
    @TestCaseId("1485")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldUseFilterImportantTablet() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(String.format(
            "%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(accLock.firstAcc().getSelfEmail())
            )
        )
            .clicksOn(steps.pages().touch().search().advancedSearchFiltersBtn())
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(5))
            .clicksOn(steps.pages().touch().search().advancedSearchMorePopupItems().get(3))
            .shouldSeeElementsCount(steps.pages().touch().searchResult().messages(), 1)
            .shouldSee(steps.pages().touch().searchResult().messages().get(0).importantLabel());
    }

    @Test
    @Title("Должны применить фильтр «Непрочитанные»")
    @TestCaseId("1486")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldUseFilterUnread() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(String.format(
            "%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(accLock.firstAcc().getSelfEmail())
            )
        )
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(6))
            .shouldSeeElementsCount(steps.pages().touch().searchResult().messages(), 1)
            .shouldSee(steps.pages().touch().searchResult().messages().get(0).unreadToggler());
    }

    @Test
    @Title("Должны применить фильтр «Непрочитанные»")
    @TestCaseId("1486")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldUseFilterUnreadTablet() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(String.format(
            "%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(accLock.firstAcc().getSelfEmail())
            )
        )
            .clicksOn(
                steps.pages().touch().searchResult().messages().waitUntil(not(empty())).get(0).toggler(),
                steps.pages().touch().search().advancedSearchFiltersBtn()
            )
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(5))
            .clicksOn(steps.pages().touch().search().advancedSearchMorePopupItems().get(4))
            .shouldSeeElementsCount(steps.pages().touch().searchResult().messages(), 1);
    }

    @Test
    @Title("Должны применить фильтр «По теме письма»")
    @TestCaseId("1488")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldUseFilterInTheme() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))
        )
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(7))
            .clicksOn(steps.pages().touch().search().advancedSearchMorePopupItems().get(2))
            .shouldSeeElementsCount(steps.pages().touch().searchResult().messages(), 1)
            .shouldSeeThatElementHasText(
                steps.pages().touch().searchResult().messages().get(0).subject(),
                SEARCH_INPUT
            );
    }

    @Test
    @Title("Должны применить фильтр «По теме письма»")
    @TestCaseId("1488")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldUseFilterInThemeTablet() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))
        )
            .clicksOn(steps.pages().touch().search().advancedSearchFiltersBtn())
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(5))
            .clicksOn(steps.pages().touch().search().advancedSearchMorePopupItems().get(2))
            .shouldSeeElementsCount(steps.pages().touch().searchResult().messages(), 1)
            .shouldSeeThatElementHasText(
                steps.pages().touch().searchResult().messages().get(0).subject(),
                SEARCH_INPUT
            );
    }

    @Test
    @Title("Должны применить фильтр «В тексте письма»")
    @TestCaseId("1489")
    @DoTestOnlyForEnvironment("Phone")
    public void shouldUseFilterInText() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))
        )
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(7))
            .clicksOn(steps.pages().touch().search().advancedSearchMorePopupItems().get(1))
            .shouldSeeElementsCount(steps.pages().touch().searchResult().messages(), 1)
            .shouldSeeThatElementHasText(
                steps.pages().touch().searchResult().messages().get(0).firstline(),
                SEARCH_INPUT
            );
    }

    @Test
    @Title("Должны применить фильтр «В тексте письма»")
    @TestCaseId("1489")
    @DoTestOnlyForEnvironment("Tablet")
    public void shouldUseFilterInTextTablet() {
        steps.user().defaultSteps().opensDefaultUrlWithPostFix(
            String.format("%s%s", SEARCH_TOUCH.makeTouchUrlPart(), SEARCH_RQST.fragment(SEARCH_INPUT))
        )
            .clicksOn(steps.pages().touch().search().advancedSearchFiltersBtn())
            .clicksOn(steps.pages().touch().search().advancedSearchFilters().waitUntil(not(empty())).get(5))
            .clicksOn(steps.pages().touch().search().advancedSearchMorePopupItems().get(1))
            .shouldSeeElementsCount(steps.pages().touch().searchResult().messages(), 1)
            .shouldSeeThatElementHasText(
                steps.pages().touch().searchResult().messages().get(0).firstline(),
                SEARCH_INPUT
            );
    }
}
