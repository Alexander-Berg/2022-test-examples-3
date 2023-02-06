package ru.yandex.autotests.innerpochta.tests.corp;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.annotations.ConditionalIgnore;
import ru.yandex.autotests.innerpochta.conditions.TicketInProgress;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.SetCorpUrlRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.data.QuickFragments.COMPOSE;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.MailConst.REMIND_LABEL_5DAYS;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.UNDEFINED;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LIZA_MINIFIED_HEADER;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_NO_REPLY_NOTIFY;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_STORED_COMPOSE_STATES;

/**
 * @author crafty
 */
@Aqua.Test
@Title("Общие тесты на особенности верстки на корпе")
@Features(FeaturesConst.CORP)
@Tag(FeaturesConst.CORP)
@Stories(FeaturesConst.CORP)
@RunWith(DataProviderRunner.class)
public class CorpFeaturesTest extends BaseTest {

    public String sbj = getRandomString();
    public String msjBody = getRandomString();

    public static final String CREDS = "CorpAttachTest";
    private static final String NDA_LINK = "https://wiki.yandex-team.ru/security/nda/";
    private static final String CONTACT_ROLE_1 = "Роботы Почты";
    private static final String CONTACT_ROLE_2 = "Робот";
    private static final String STAFF_URL = "https://staff.yandex-team.ru/settings";
    private static final String DELETE_EXP = "?experiments=12345,0,0";

    private AccLockRule lock = AccLockRule.use().names(CREDS);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SetCorpUrlRule setCorpUrlRule = new SetCorpUrlRule();

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(removeAllMessages(() -> user, SENT, DRAFT, TRASH));

    @DataProvider
    public static Object[][] lang() {
        return new Object[][]{
            {"English", "Inbox"},
            {"Русский", "Входящие"}
        };
    }

    @DataProvider
    public static Object[][] services() {
        return new Object[][]{
            {"https://calendar.yandex-team.ru/", 1},
            {"https://telemost.yandex.ru/", 2},
            {"https://staff.yandex-team.ru/", 3}
        };
    }

    @DataProvider
    public static Object[][] servicesMore() {
        return new Object[][]{
            {"#contacts", 2},
            {"https://st.yandex-team.ru/", 3},
            {"https://wiki.yandex-team.ru/", 4},
            {"https://q.yandex-team.ru/", 5},
            {"https://calendar.yandex-team.ru/invite", 8}
        };
    }

    @Before
    public void setUp() {
        user.apiSettingsSteps().callWithListAndParams(
            "Сбрасываем свёрнутые композы и выключаем компактную шапку",
            of(
                SETTINGS_STORED_COMPOSE_STATES, UNDEFINED,
                LIZA_MINIFIED_HEADER, EMPTY_STR
            )
        );
        user.loginSteps().forAcc(lock.acc(CREDS)).loginsToCorp();
    }

    @Test
    @Title("По клику на лого NDA переходим на вики")
    @TestCaseId("2667")
    public void shouldOpenNDA() {
        user.defaultSteps()
            .shouldSee(onCorpPage().logoNDA())
            .offsetClick(10, 10) //Бургер перекрывает центр лого NDА, приходится кликать по координатам
            .shouldBeOnUrl(containsString(NDA_LINK));
    }

    @Test
    @Title("Проверяем отсутствие рекламы в интерфейсе и опции в настройках")
    @TestCaseId("2100")
    public void shouldNotSeeAds() {
        user.defaultSteps().shouldNotSee(
                user.pages().MessagePage().directLine(),
                user.pages().MessagePage().directLeft()
            )
            .opensFragment(QuickFragments.CONTACTS)
            .shouldNotSee(
                user.pages().MessagePage().directLine(),
                user.pages().MessagePage().directLeft()
            )
            .opensFragment(COMPOSE)
            .shouldSee(user.pages().ComposePopup().expandedPopup());
        user.composeSteps().inputsAddressInFieldTo(DEV_NULL_EMAIL);
        user.composeSteps().inputsSubject(getRandomString());
        user.defaultSteps().clicksOn(user.pages().ComposePopup().expandedPopup().sendBtn())
            .shouldNotSee(user.pages().MessagePage().directDone())
            .opensFragment(QuickFragments.SETTINGS_OTHER)
            .shouldNotSee(onOtherSettings().blockSetupOther().topPanel().showAdvertisement());
    }

    @Test
    @Title("Проверяем инфо со стаффа в карточке контакта")
    @TestCaseId("5490")
    public void shouldSeeStaffInfo() {
        user.defaultSteps().opensDefaultUrlWithPostFix(DELETE_EXP);
        user.messagesSteps().clicksOnMessageByNumber(0);
        user.defaultSteps()
            .clicksOn(onMessageView().messageHead().fromName())
            .shouldSee(onMessageView().mailCard())
            .shouldHasText(onMessageView().mailCard().contactPost().get(0), CONTACT_ROLE_1)
            .shouldHasText(onMessageView().mailCard().contactPost().get(1), CONTACT_ROLE_2);
    }

    @Test
    @Title("Проверяем переключение языка на корпе")
    @TestCaseId("5491")
    public void shouldChangeLang() {
        user.defaultSteps().clicksOn(onMessagePage().footerLineBlock().languageSwitchCorp())
            .switchOnJustOpenedWindow()
            .shouldBeOnUrl(containsString(STAFF_URL));
    }

    @Test
    @Title("Нет пересылки в фильтрах")
    @TestCaseId("3963")
    public void shouldNotSeeForward() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_FILTERS_CREATE)
            .shouldNotSee(onFiltersCreationPage().setupFiltersCreate()
                .blockPasswordProtectedActions().forwardToCheckBox());
    }

    @Test
    @Title("Есть кнопка напоминания о неответе в композе при включенной настройке")
    @TestCaseId("3968")
    @ConditionalIgnore(condition = TicketInProgress.class)
    @Issue("DARIA-68249")
    public void shouldSeeRememberButton() {
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем настройку напоминания о неответе",
            of(SETTINGS_PARAM_NO_REPLY_NOTIFY, TRUE)
        );
        user.defaultSteps().opensFragment(QuickFragments.COMPOSE)
            .shouldSeeThatElementTextEquals(onComposePage().footerSendBlock().remindBtn(), REMIND_LABEL_5DAYS);
    }

    @Test
    @Title("Отменяем отправку сообщения")
    @TestCaseId("4740")
    public void shouldUndoSending() {
        fillCompose();
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn())
            .shouldSee(onMessagePage().statusLineBlock())
            .clicksOn(onMessagePage().statusLineBlock().unDoBtn())
            .shouldSee(onComposePopup().expandedPopup())
            .shouldContainText(onComposePopup().expandedPopup().bodyInput(), msjBody);
    }

    @Test
    @Title("Повторно отправляем сообщение после отмены")
    @TestCaseId("4740")
    public void shouldSendLetterAfterUndo() {
        fillCompose();
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn())
            .clicksOn(onMessagePage().statusLineBlock().unDoBtn())
            .clicksOn(onComposePopup().expandedPopup().sendBtn())
            .opensFragment(QuickFragments.SENT);
        user.messagesSteps().shouldSeeMessageWithSubject(sbj);
    }

    @Step("Заполняем композ")
    private void fillCompose() {
        user.defaultSteps().clicksOn(onHomePage().composeButton())
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), DEV_NULL_EMAIL)
            .inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), sbj)
            .inputsTextInElement(onComposePopup().expandedPopup().bodyInput(), msjBody);
    }

    @Test
    @Title("Переходим в сервисы")
    @TestCaseId("3964")
    @UseDataProvider("services")
    public void shouldMoveToService(String url, int number) {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().serviceIcons().get(number))
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(url);
    }

    @Test
    @Title("Переходим в сервисы в выпадушке «Еще»")
    @TestCaseId("3964")
    @UseDataProvider("servicesMore")
    public void shouldMoveToServicesInMore(String url, int number) {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().moreServices())
            .clicksOn(onMessagePage().allServices360Popup().serviceIcons().get(number))
            .switchOnJustOpenedWindow()
            .shouldContainTextInUrl(url);
    }

    @Test
    @Title("Должны видеть сервисы «Этушка» и «Рассылки» в выпадушке «Еще»")
    @TestCaseId("3964")
    public void shouldSeeServicesInMore() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().moreServices())
            .shouldSee(
                onMessagePage().allServices360Popup().serviceMyAt(),
                onMessagePage().allServices360Popup().serviceMl()
            );
    }

    @Test
    @Title("Нажимаем на иконку «Я»")
    @TestCaseId("5968")
    public void shouldClickOnYandexLogo() {
        user.defaultSteps().clicksOn(onMessagePage().mail360HeaderBlock().yandexLogoMainPage())
            .shouldBeOnUrl(webDriverRule.getBaseUrl() + "/#inbox");
    }
}
