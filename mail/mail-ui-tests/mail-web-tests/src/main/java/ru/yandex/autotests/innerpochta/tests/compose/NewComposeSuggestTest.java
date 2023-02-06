package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.empty;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.UNDEFINED;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.LAYOUT_2PANE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_PARAM_LAYOUT;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_STORED_COMPOSE_STATES;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Новый композ - Саджест контактов")
@Features(FeaturesConst.COMPOSE)
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class NewComposeSuggestTest extends BaseTest {

    private static final String CONTACT_WITH_TWO_EMAILS = "Два Адреса";
    private static final String CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL = "testbot3@yandex.ru";
    private static final String SELF_CONTACT = "Себе";

    private AccLockRule lock = AccLockRule.use().useTusAccount();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    private Contact contact;
    private Contact contact2;

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));

    @Before
    public void setUp() {
        contact = user.abookSteps().createContactWithParametrs(
            CONTACT_WITH_TWO_EMAILS,
            CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL
        );
        contact2 = user.abookSteps().createContactWithParametrs(
            getRandomName(),
            lock.firstAcc().getSelfEmail()
        );
        user.apiSettingsSteps().callWithListAndParams(
            "Включаем 2pane и сбрасываем свёрнутые композы",
            of(
                SETTINGS_PARAM_LAYOUT, LAYOUT_2PANE,
                SETTINGS_STORED_COMPOSE_STATES, UNDEFINED
            )
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().clicksOn(onMessagePage().composeButton());
    }

    @Test
    @Title("Показываем контакт «Себе» в саджесте популярных контактов и отправляем себе письмо")
    @TestCaseId("5711")
    public void shouldSendChangedEmailFromYabbleDropdownMenu() {
        user.apiAbookSteps().addNewContacts(contact2);
        String msgSubject = getRandomString();
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .clicksOn(onComposePopup().yabbleFrom())
            .shouldNotSeeElementInList(
                onComposePopup().fromSuggestList().waitUntil(not(empty())),
                SELF_CONTACT
            )
            .clicksOn(
                onComposePopup().expandedPopup().sbj(),
                onComposePopup().expandedPopup().popupBcc()
            )
            .shouldSeeThatElementHasText(
                onComposePopup().suggestList().waitUntil(not(empty())).get(0),
                SELF_CONTACT
            )
            .clicksOn(
                onComposePopup().expandedPopup().bodyInput(),
                onComposePopup().expandedPopup().popupCc()
            )
            .shouldSeeThatElementHasText(
                onComposePopup().suggestList().waitUntil(not(empty())).get(0),
                SELF_CONTACT
            )
            .clicksOn(
                onComposePopup().expandedPopup().bodyInput(),
                onComposePopup().expandedPopup().popupTo()
            )
            .shouldSeeThatElementHasText(
                onComposePopup().suggestList().waitUntil(not(empty())).get(0),
                SELF_CONTACT
            )
            .clicksOn(onComposePopup().suggestList().get(0));
        user.composeSteps().inputsSubject(msgSubject)
            .clicksOnSendBtn();
        user.defaultSteps().refreshPage();
        user.messagesSteps().shouldSeeMessageWithSubject(msgSubject);
    }

    @Test
    @Title("Саджест меняется при наборе контакта")
    @TestCaseId("5713")
    public void shouldChangeSuggestInCompose() {
        user.apiAbookSteps().addContact(CONTACT_WITH_TWO_EMAILS, contact);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().popupTo())
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), CONTACT_WITH_TWO_EMAILS)
            .shouldNotSeeElementInList(
                onComposePopup().suggestList().waitUntil(not(empty())),
                SELF_CONTACT
            )
            .shouldSeeElementInList(onComposePopup().suggestList(), CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .clicksOn(onComposePopup().expandedPopup().popupCc())
            .inputsTextInElement(onComposePopup().expandedPopup().popupCc(), CONTACT_WITH_TWO_EMAILS)
            .shouldNotSeeElementInList(
                onComposePopup().suggestList().waitUntil(not(empty())),
                SELF_CONTACT
            )
            .shouldSeeElementInList(onComposePopup().suggestList(), CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL);
        user.defaultSteps().clicksOn(
            onComposePopup().expandedPopup().bodyInput(),
            onComposePopup().expandedPopup().popupBcc()
        )
            .inputsTextInElement(onComposePopup().expandedPopup().popupBcc(), CONTACT_WITH_TWO_EMAILS)
            .shouldNotSeeElementInList(
                onComposePopup().suggestList().waitUntil(not(empty())),
                SELF_CONTACT
            )
            .shouldSeeElementInList(onComposePopup().suggestList(), CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL);
    }

}
