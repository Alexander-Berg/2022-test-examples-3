package ru.yandex.autotests.innerpochta.tests.settings;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static java.lang.Integer.parseInt;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.SETTINGS_FILTERS;
import static ru.yandex.autotests.innerpochta.touch.pages.UnsubscribeIframe.IFRAME_SUBS_LIZA;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasClass;

/**
 * @author a-zoshchuk
 */
@Aqua.Test
@Title("Управление рассылками")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.SUBSCRIPTIONS)
@RunWith(DataProviderRunner.class)
public class SubscriptionsTest extends BaseTest {

    private static final String LAMODA_MAILLIST = "[{\"displayName\": \"Lamoda\", \"messageType\": 13, \"email\": " +
        "\"newsletter@info.lamoda.ru\", \"folderId\": \"3\"}]";
    private static final String DNS_MAILLIST = "[{\"displayName\": \"DNS цифровая и бытовая техника\", " +
        "\"messageType\": 13, \"email\": \"no-reply.dns@email.dns-shop.ru\", \"folderId\": \"3\"}]";
    private static final String YAM_MAILLIST = "[{\"displayName\": \"Яндекс.Маркет\", \"messageType\": 13, \"email\": " +
        "\"mailer@market.yandex.ru\", \"folderId\": \"3\"}]";
    private static final String HIDE_1_ML_TEXT = "Скрыть 1 рассылку";
    private static final String HIDE_2_ML_TEXT = "Скрыть 2 рассылки";
    private static final String SEARCH_TEXT = "info";

    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth);

    @Before
    public void setUp() {
        user.apiFiltersSteps().deleteAllUnsubscribeFilters();
        user.apiMessagesSteps().moveAllMessagesFromFolderToFolder(TRASH, INBOX);
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Закрываем попап управления рассылками")
    @TestCaseId("5004")
    public void shouldCloseUnsubscribePopup() {
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps().clicksOn(onUnsubscribePopupPage().closeSubs())
            .shouldNotSee(onUnsubscribePopupPage().closeSubs());
    }

    @Test
    @Title("Корректность каунтеров в попапе рассылок")
    @TestCaseId("5006")
    public void shouldSeeCorrectCountersInUnsubscribe() {
        user.apiFiltersSteps().createUnsubscribeFilters(LAMODA_MAILLIST, DNS_MAILLIST);
        user.defaultSteps().refreshPage();
        user.settingsSteps().openSubscriptionsSettings();
        int currentCounter = parseInt(onUnsubscribePopupPage().counter().getText());
        user.defaultSteps().shouldSeeElementsCount(onUnsubscribePopupPage().subscriptions(), currentCounter)
            .clicksOn(onUnsubscribePopupPage().tabHidden());
        tabShouldBeActive(onUnsubscribePopupPage().tabHidden());
        currentCounter = parseInt(onUnsubscribePopupPage().counter().getText());
        user.defaultSteps().shouldSeeElementsCount(onUnsubscribePopupPage().subscriptions(), currentCounter);
    }

    @Test
    @Title("Фильтры для скрытых рассылок не отображаются в настройках")
    @TestCaseId("5007")
    public void shouldNotSeeUnsubscribeFiltersInSettings() {
        user.apiFiltersSteps().createUnsubscribeFilters(LAMODA_MAILLIST);
        user.defaultSteps().opensFragment(SETTINGS_FILTERS)
            .shouldBeOnUrlWith(SETTINGS_FILTERS)
            .shouldNotSee(onFiltersOverview().createdFilterBlocks());
    }

    @Test
    @Title("Отписаться от {0} рассылки(ок), не удаляя старые письма")
    @TestCaseId("5008")
    @DataProvider({"1", "2"})
    public void shouldSeeOldMessagesIfUnsubscribeWithoutDelete(String numOfMLToUnsubscribe) {
        int messagesInInbox = user.apiMessagesSteps().getAllMessagesInFolder(INBOX).size();
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps().turnTrue(
                onUnsubscribePopupPage().subsCheckboxes().subList(0, parseInt(numOfMLToUnsubscribe))
                    .toArray(new MailElement[0])
            )
            .clicksOn(onUnsubscribePopupPage().subsListBtn())
            .waitInSeconds(1)
            .clicksOn(onUnsubscribePopupPage().confirmPopupBtn())
            .shouldNotSee(onUnsubscribePopupPage().loaderUnsubscribing())
            .clicksOn(onUnsubscribePopupPage().successPopupBtn())
            .refreshPage();
        user.messagesSteps().shouldSeeCorrectNumberOfMessages(messagesInInbox);
    }

    @Test
    @Title("Отписаться от {0} рассылки(ок), удаляя старые письма")
    @TestCaseId("5009")
    @DataProvider({"1", "2"})
    public void shouldNotSeeOldMessagesIfUnsubscribeWithDelete(String numOfMLToUnsubscribe) {
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps().turnTrue(
                onUnsubscribePopupPage().subsCheckboxes().subList(0, parseInt(numOfMLToUnsubscribe))
                    .toArray(new MailElement[0])
            )
            .clicksOn(onUnsubscribePopupPage().subsListBtn())
            .waitInSeconds(2)
            .clicksOn(
                onUnsubscribePopupPage().deleteMsgesCheckbox(),
                onUnsubscribePopupPage().confirmPopupBtn()
            )
            .shouldNotSee(onUnsubscribePopupPage().loaderUnsubscribing())
            .clicksOn(onUnsubscribePopupPage().successPopupBtn());
        assertThat(
            "Письма не удалились!",
            user.apiMessagesSteps().getAllMessagesInFolder(TRASH),
            withWaitFor(hasSize(parseInt(numOfMLToUnsubscribe)))
        );
    }

    @Test
    @Title("В кнопке «Скрыть рассылки» меняется количество меняемых рассылок")
    @TestCaseId("5017")
    public void shouldSeeNumOfSelectedML() {
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps().turnTrue(onUnsubscribePopupPage().subsCheckboxes().get(0))
            .shouldSeeThatElementTextEquals(onUnsubscribePopupPage().subsListBtn(), HIDE_1_ML_TEXT)
            .turnTrue(onUnsubscribePopupPage().subsCheckboxes().get(1))
            .shouldSeeThatElementTextEquals(onUnsubscribePopupPage().subsListBtn(), HIDE_2_ML_TEXT);
    }

    @Test
    @Title("Выделение не должно сбрасываться при переходе между табами")
    @TestCaseId("5019")
    public void checkboxesShouldRemainSelectedAfterTabChange() {
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps().turnTrue(
                onUnsubscribePopupPage().subsCheckboxes().subList(0, 3).toArray(new MailElement[0])
            )
            .clicksOn(onUnsubscribePopupPage().tabHidden())
            .clicksOn(onUnsubscribePopupPage().inactiveTab())
            .shouldSee(onUnsubscribePopupPage().subsCheckboxes().waitUntil(not(empty())).get(0));
        checkboxesShouldBeInState(
            true,
            onUnsubscribePopupPage().subsCheckboxes().subList(0, 3).toArray(new MailElement[0])
        );
    }

    @Test
    @Title("Рассылки должны остаться выделенными после того, как передумали")
    @TestCaseId("5020")
    @DataProvider({"1", "3"})
    public void checkboxesShouldRemainSelectedAfterChangingMind(String numOfMLToUnsubscribe) {
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps().turnTrue(
                onUnsubscribePopupPage().subsCheckboxes().subList(0, parseInt(numOfMLToUnsubscribe))
                    .toArray(new MailElement[0])
            )
            .clicksOn(onUnsubscribePopupPage().subsListBtn())
            .clicksOn(onUnsubscribePopupPage().confirmPopupClose());
        checkboxesShouldBeInState(
            true,
            onUnsubscribePopupPage().subsCheckboxes().subList(0, parseInt(numOfMLToUnsubscribe))
                .toArray(new MailElement[0])
        );
    }

    @Test
    @Title("Сбрасываем состояние попапа рассылок при выходе их него")
    @TestCaseId("5025")
    public void shouldReopenMLPopupInDefaultState() {
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps()
            .turnTrue(onUnsubscribePopupPage().subsCheckboxes().subList(0, 3).toArray(new MailElement[0]))
            .clicksOn(onUnsubscribePopupPage().tabHidden())
            .clicksOn(onUnsubscribePopupPage().closeSubs());
        user.settingsSteps().openSubscriptionsSettings();
        tabShouldBeActive(onUnsubscribePopupPage().tabs().waitUntil(not(empty())).get(0));
        checkboxesShouldBeInState(
            false,
            onUnsubscribePopupPage().subsCheckboxes().subList(0, 3).toArray(new MailElement[0])
        );
    }

    @Test
    @Title("Возврат из рассылочного списка писем")
    @TestCaseId("5029")
    public void shouldSeeInactiveMLAfterReturningFromMLPage() {
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps().clicksOn(onUnsubscribePopupPage().subscriptions().get(0))
            .clicksOn(onUnsubscribePopupPage().backFromSubsView());
        checkboxesShouldBeInState(
            false,
            onUnsubscribePopupPage().subsCheckboxes().get(0)
        );
    }

    @Test
    @Title("Активируем одну рассылку из просмотра рассылки")
    @TestCaseId("5023")
    public void shouldActivateOnePromo() {
        user.apiFiltersSteps().createUnsubscribeFilters(LAMODA_MAILLIST, DNS_MAILLIST);
        user.settingsSteps().openSubscriptionsSettings();
        int activeSubscriptions = parseInt(onUnsubscribePopupPage().counter().getText());
        int hiddenSubscriptions = parseInt(onUnsubscribePopupPage().tabCounter().getText());
        user.defaultSteps().clicksOn(onUnsubscribePopupPage().tabHidden())
            .onMouseHoverAndClick(onUnsubscribePopupPage().subscriptions().get(0))
            .clicksOn(onUnsubscribePopupPage().subsViewBtn())
            .waitInSeconds(1)
            .clicksOn(onUnsubscribePopupPage().confirmSubscribePopupBtn())
            .clicksOn(onUnsubscribePopupPage().successSubscribePopupBtn());
        tabShouldBeActive(onUnsubscribePopupPage().tabHidden());
        assertEquals("Фильтр отписки не удалился", 1, user.apiFiltersSteps().getAllUnsubscribeFilters().size());
        assertEquals(
            "Количество скрытых рассылок не изменилось",
            hiddenSubscriptions - 1,
            parseInt(onUnsubscribePopupPage().counter().getText())
        );
        assertEquals(
            "Количество активных рассылок не изменилось",
            activeSubscriptions + 1,
            parseInt(onUnsubscribePopupPage().tabCounter().getText())
        );
    }

    @Test
    @Title("Активируем насколько рассылок из списка скрытых рассылок")
    @TestCaseId("5024")
    public void shouldActivateSeveralPromo() {
        user.apiFiltersSteps().createUnsubscribeFilters(LAMODA_MAILLIST, DNS_MAILLIST, YAM_MAILLIST);
        user.settingsSteps().openSubscriptionsSettings();
        int activeSubscriptions = parseInt(onUnsubscribePopupPage().counter().getText());
        int hiddenSubscriptions = parseInt(onUnsubscribePopupPage().tabCounter().getText());
        user.defaultSteps().clicksOn(onUnsubscribePopupPage().tabHidden())
            .turnTrue(onUnsubscribePopupPage().subsCheckboxes().subList(0, 2).toArray(new MailElement[0]))
            .clicksOn(onUnsubscribePopupPage().subsListBtn())
            .clicksOn(onUnsubscribePopupPage().confirmSubscribePopupBtn())
            .clicksOn(onUnsubscribePopupPage().successSubscribePopupBtn());
        assertEquals("Фильтр отписки не удалился", 1, user.apiFiltersSteps().getAllUnsubscribeFilters().size());
        assertEquals(
            "Количество скрытых рассылок не изменилось",
            hiddenSubscriptions - 2,
            parseInt(onUnsubscribePopupPage().counter().getText())
        );
        assertEquals(
            "Количество активных рассылок не изменилось",
            activeSubscriptions + 2,
            parseInt(onUnsubscribePopupPage().tabCounter().getText())
        );
    }

    @Test
    @Title("Не должны активировать рассылку, если отменили на последнем шаге")
    @TestCaseId("5026")
    public void shouldNotActivateSubscriptionIfCanceled() {
        user.apiFiltersSteps().createUnsubscribeFilters(LAMODA_MAILLIST);
        int currentUnsubscribeFiltersNum = user.apiFiltersSteps().getAllUnsubscribeFilters().size();
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps().clicksOn(onUnsubscribePopupPage().tabHidden())
            .onMouseHoverAndClick(onUnsubscribePopupPage().subscriptions().get(0))
            .clicksOn(onUnsubscribePopupPage().subsViewBtn())
            .clicksOn(onUnsubscribePopupPage().confirmPopupClose());
        assertEquals(
            "Отписка активировалась",
            currentUnsubscribeFiltersNum,
            user.apiFiltersSteps().getAllUnsubscribeFilters().size()
        );
    }

    @Test
    @Title("Не должны активировать рассылки, если отменили на последнем шаге")
    @TestCaseId("5027")
    public void shouldNotActivateSubscriptionsIfCanceled() {
        user.apiFiltersSteps().createUnsubscribeFilters(LAMODA_MAILLIST, DNS_MAILLIST);
        int currentUnsubscribeFiltersNum = user.apiFiltersSteps().getAllUnsubscribeFilters().size();
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps().clicksOn(onUnsubscribePopupPage().tabHidden())
            .turnTrue(onUnsubscribePopupPage().subsCheckboxes().subList(0, 2).toArray(new MailElement[0]))
            .clicksOn(onUnsubscribePopupPage().subsListBtn())
            .clicksOn(onUnsubscribePopupPage().confirmPopupClose());
        assertEquals(
            "Отписка активировалась",
            currentUnsubscribeFiltersNum,
            user.apiFiltersSteps().getAllUnsubscribeFilters().size()
        );
    }

    @Test
    @Title("Должен активироваться чекбокс «Выделить все» после выделения всех рассылок по одной")
    @TestCaseId("6313")
    public void shouldActivateAllSubsOneByOne() {
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps().turnTrue(onUnsubscribePopupPage().subsCheckboxes())
            .shouldSee(onUnsubscribePopupPage().selectAllChecked())
            .clicksOn(onUnsubscribePopupPage().subsCheckboxes().get(0))
            .shouldNotSee(onUnsubscribePopupPage().selectAllChecked());
    }

    @Test
    @Title("Список рассылок меняется при поиске")
    @TestCaseId("6317")
    public void shouldSearchSubs() {
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps().inputsTextInElement(onUnsubscribePopupPage().search(), SEARCH_TEXT)
            .shouldSeeElementsCount(onUnsubscribePopupPage().subscriptions(), 5)
            .inputsTextInElement(onUnsubscribePopupPage().search(), SEARCH_TEXT + "@")
            .shouldSeeElementsCount(onUnsubscribePopupPage().subscriptions(), 2);
    }

    @Test
    @Title("Выделение рассылок сбрасывается при поиске")
    @TestCaseId("6315")
    public void shouldClearCheckboxesAfterSearching() {
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps().clicksOn(onUnsubscribePopupPage().selectAll())
            .shouldSee(onUnsubscribePopupPage().selectAllChecked())
            .inputsTextInElement(onUnsubscribePopupPage().search(), SEARCH_TEXT)
            .shouldNotSee(onUnsubscribePopupPage().selectAllChecked());
    }

    @Test
    @Title("Поиск сохраняется при смене таба")
    @TestCaseId("6316")
    public void shouldSaveSearchingAfterSwitchingTab() {
        user.settingsSteps().openSubscriptionsSettings();
        user.defaultSteps().inputsTextInElement(onUnsubscribePopupPage().search(), SEARCH_TEXT)
            .clicksOn(onUnsubscribePopupPage().tabHidden())
            .shouldHasValue(onUnsubscribePopupPage().search(), SEARCH_TEXT);
    }

    @Test
    @Title("Открывается таб Активные, если перейти в УР из настроек")
    @TestCaseId("6347")
    public void shouldOpenActiveSubsTab() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS)
            .clicksOn(onSettingsPage().subscriptions())
            .switchTo(IFRAME_SUBS_LIZA)
            .shouldSee(onUnsubscribePopupPage().tabActive());
    }

    @Test
    @Title("Открываем попап УР по урлу с параметром")
    @TestCaseId("6343")
    public void shouldOpenActiveSubsTabFromSettings() {
        user.defaultSteps().opensDefaultUrlWithPostFix("/?unsubscribe-popup=1")
            .switchTo(IFRAME_SUBS_LIZA)
            .shouldSee(onUnsubscribePopupPage().tabNew());
    }

    @Step("Проверяем, что чекбоксы остались выделены")
    private void checkboxesShouldBeInState(Boolean state, MailElement... checkboxes) {
        for (MailElement checkbox : checkboxes) {
            if (state)
                assertThat("Чекбокс не выделен", checkbox, hasClass(containsString("checkboxChecked")));
            if (!state)
                assertThat(
                    "Чекбокс не выделен",
                    checkbox,
                    not(hasClass(containsString("checkboxChecked")))
                );
        }
    }

    @Step("Таб {0} должен быть активен")
    private void tabShouldBeActive(MailElement tab) {
        assertThat("Таб не активен", tab, hasClass(containsString("isActive")));
    }
}
