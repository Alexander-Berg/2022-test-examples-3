package ru.yandex.autotests.innerpochta.tests.messageslist;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SEARCH;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;

/**
 * @author cosmopanda
 */
@Aqua.Test
@Title("Тесты на виджеты")
@Features(FeaturesConst.MESSAGE_LIST)
@Tag(FeaturesConst.MESSAGE_LIST)
@Stories(FeaturesConst.WIDGET)
@Description("У юзера подготовлены папки с виджетами")
public class WidgetTest extends BaseTest {

    private static final String SEARCH_REQUEST = "?request=%s";
    private static final String SEARCH_RESULT = "Confirmation of";
    private static final String SUBJECT = "тема письма";
    private static final String BODY = "тело письма";

    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock).around(auth);

    @Before
    public void logIn() {
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Открываем письмо с виджетом")
    @TestCaseId("2988")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldOpenMsgWithWidget() {
        user.leftColumnSteps().opensCustomFolder(2);
        user.defaultSteps()
            .shouldSee(onMessagePage().displayedMessages().list().waitUntil(not(empty())).get(0).widgetTicket());
        user.messagesSteps().clicksOnMessageByNumber(0);
    }

    @Test
    @Title("В поиске должны рисоваться виджеты")
    @TestCaseId("2989")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldSeeWidgetInSearch() {
        user.defaultSteps().opensFragment(SEARCH.fragment() + String.format(SEARCH_REQUEST, SEARCH_RESULT))
            .shouldHasValue(onMessagePage().mail360HeaderBlock().searchInput(), SEARCH_RESULT)
            .shouldSee(onSearchPage().otherResultsHeader())
            .shouldSee(
                onMessagePage().displayedMessages().list().get(0).widgetTicket().widgetDecoration(),
                onMessagePage().displayedMessages().list().get(0).widgetTicket().widgetBtns().get(0)
            );
    }

    @Test
    @Title("Переходим в композ по кнопке «Исправить»")
    @TestCaseId("3446")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-66766")
    public void shouldOpenComposeFromWidget() {
        user.leftColumnSteps().opensCustomFolder(10);
        user.defaultSteps().shouldSee(onMessagePage().displayedMessages())
            .clicksOn(
                onMessagePage().displayedMessages().list().get(0).widget().composeButton().waitUntil(isPresent())
            )
            .shouldBeOnUrlWith(COMPOSE);
        user.composeSteps().shouldSeeEmptySendFieldTo()
            .shouldSeeSubject(SUBJECT)
            .shouldSeeTextAreaContains(BODY);
    }
}
