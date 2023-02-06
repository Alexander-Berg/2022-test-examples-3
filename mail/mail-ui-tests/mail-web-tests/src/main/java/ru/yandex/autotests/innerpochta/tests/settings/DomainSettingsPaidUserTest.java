package ru.yandex.autotests.innerpochta.tests.settings;

import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllMessagesRule.removeAllMessages;
import static ru.yandex.autotests.innerpochta.util.MailConst.DEV_NULL_EMAIL;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.DRAFT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.EMPTY_STR;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.FALSE;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.INBOX;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.SENT;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRASH;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.DISABLE_PROMO;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.DOMAIN_PROMO;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.FOLDER_TABS;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.PROMO_MANAGER;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_OPEN_MSG_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_CURRENT_LIST;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_PAGE_AFTER_SENT;

@Aqua.Test
@Title("Тесты на настройки домена")
@Features(FeaturesConst.SETTINGS)
@Tag(FeaturesConst.SETTINGS)
@Stories(FeaturesConst.DOMAIN_SETTINGS)
public class DomainSettingsPaidUserTest extends BaseTest {

    public static final String CREDS = "DomainSettingsPaidTest";
    private static final String BEAUTIFUL_EMAIL = "me@autotestdomain3.ru";
    private static final String ENABLED_STATUS = "Подключен и\nиспользуется как адрес отправки по умолчанию";
    private static final String DISABLE_TITLE = "Удалить красивый адрес?";

    private AccLockRule lock = AccLockRule.use().names(CREDS);
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(removeAllMessages(() -> user, INBOX, TRASH, SENT, DRAFT));

    @Before
    public void logIn() {
        user.apiSettingsSteps().callWithListAndParams(
            "Выключаем табы, включаем просмотр письма на отдельной странице",
            of(
                FOLDER_TABS, FALSE,
                SETTINGS_OPEN_MSG_LIST, EMPTY_STR,
                SETTINGS_PARAM_PAGE_AFTER_SENT, SETTINGS_PARAM_CURRENT_LIST,
                DISABLE_PROMO, TRUE
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
    }

    @Test
    @Title("Отправка писем с красивого адреса")
    @TestCaseId("6035")
    public void sendMailFromDomainTest() {
        String sbj = Utils.getRandomString();
        user.composeSteps().openComposePopup()
            .inputsAddressInFieldTo(DEV_NULL_EMAIL)
            .inputsSubject(sbj)
            .clicksOnSendBtn();
        user.leftColumnSteps().opensSentFolder();
        user.messagesSteps().clicksOnMessageWithSubject(sbj);
        user.defaultSteps().shouldSee(onMessageView().messageHead())
            .shouldContainText(onMessageView().messageHead().fromAddress(), BEAUTIFUL_EMAIL);
    }

    @Test
    @Title("Приход письма на красивый адрес")
    @TestCaseId("6035")
    public void sendMailToDomainTest() {
        String sbj = Utils.getRandomString();
        user.apiMessagesSteps().sendMailWithNoSave(BEAUTIFUL_EMAIL, sbj, "");
        user.messagesSteps().clicksOnMessageWithSubject(sbj);
        user.messageViewSteps().expandCcAndBccBlock();
        user.defaultSteps().shouldSee(onMessageView().messageHead())
            .shouldContainText(onMessageView().messageHead().contactsInTo().get(0), BEAUTIFUL_EMAIL);
    }

    @Test
    @Title("Красивый адрес в поле От кого в композе")
    @TestCaseId("6032")
    public void shouldSeeDomainInCompose() {
        user.composeSteps().openComposePopup()
            .expandCcBcc();
        user.defaultSteps().shouldSeeThatElementHasText(onComposePopup().yabbleFrom(), BEAUTIFUL_EMAIL);
    }

    @Test
    @Title("Красивый адрес в поле От кого в настройках")
    @TestCaseId("6032")
    public void shouldSeeDomainAliasInSettings() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_SENDER)
            .shouldSeeThatElementHasText(
                onSenderInfoSettingsPage().blockSetupSender().blockAliases().userAdresses().get(2),
                BEAUTIFUL_EMAIL
            );
    }

    @Test
    @Title("Видим статус подключенного красивого адреса")
    @TestCaseId("6032")
    public void shouldSeeEnabledDomainStatus() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_DOMAIN)
            .shouldSeeThatElementHasText(
                domainSettingsPage().domainStatus(),
                ENABLED_STATUS
            );
    }

    @Test
    @Title("Видим промку красивого адреса после подключения домена")
    @TestCaseId("6042")
    public void shouldSeeDomainPromoInMessageList() {
        user.apiSettingsSteps().callWithListAndParams(
            "Сбрасываем показ промки",
            of(
                DOMAIN_PROMO, EMPTY_STR,
                PROMO_MANAGER, EMPTY_STR,
                DISABLE_PROMO, FALSE
            )
        );
        user.defaultSteps().refreshPage()
            .shouldSee(onMessagePage().domainPromo());
    }

    @Test
    @Title("Отмена в попапе отключения домена")
    @TestCaseId("6054")
    public void shouldCloseDisableDomainPopup() {
        user.defaultSteps().opensFragment(QuickFragments.SETTINGS_DOMAIN)
            .clicksOn(domainSettingsPage().actionButtons().get(1))
            .shouldSeeThatElementHasText(
                onMessagePage().domainDisablePopupHeader(),
                DISABLE_TITLE
            )
            .clicksOn(onMessagePage().domainDisablePopupCancelButton())
            .shouldNotSee(onMessagePage().domainDisablePopupHeader());
    }
}
