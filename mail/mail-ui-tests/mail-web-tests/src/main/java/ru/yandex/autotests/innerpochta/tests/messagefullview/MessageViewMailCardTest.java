package ru.yandex.autotests.innerpochta.tests.messagefullview;

import io.qameta.allure.junit4.Tag;
import org.hamcrest.Matchers;
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
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.steps.beans.message.Message;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.core.IsNot.not;
import static ru.yandex.autotests.innerpochta.rules.ClearAccRule.clearAcc;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomName;

/**
 * Created by mabelpines
 */
@Aqua.Test
@Title("Проверяем карточку контактов в просмотре письма")
@Features(FeaturesConst.MESSAGE_FULL_VIEW)
@Tag(FeaturesConst.MESSAGE_FULL_VIEW)
@Stories(FeaturesConst.CONTACT_CARD)
public class MessageViewMailCardTest extends BaseTest {

    private static final String RECEIVER_EMAIL = "newtestbot6@yandex.ru";
    private static final String RECIPIENT = "Default-Имя Default Фамилия";
    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();
    private final AccLockRule lock = AccLockRule.use().useTusAccount();
    private final RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private final AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);
    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(clearAcc(() -> user));
    private Contact contact;
    private Message msg;

    @Before
    public void setUp() {
        contact = user.abookSteps().createContactWithParametrs(getRandomName(), lock.firstAcc().getSelfEmail());
        msg = user.apiMessagesSteps().addCcEmails(RECEIVER_EMAIL)
            .sendMailWithCcAndBcc(lock.firstAcc().getSelfEmail(), getRandomName(), "");
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.messagesSteps().shouldSeeMessageWithSubject(msg.getSubject());
        user.defaultSteps().opensFragment(QuickFragments.MSG_FRAGMENT.fragment(msg.getMid()))
            .clicksOn(onMessageView().messageHead().fromName())
            .shouldSee(onMessageView().mailCard());
    }

    @Test
    @Title("Карточка должна сворачиваться кликом по крестику.")
    @TestCaseId("2033")
    public void shouldCollapseByClickOnCloseBtn() {
        user.defaultSteps().clicksOn(onMessageView().mailCard().closeBtn())
            .shouldNotSee(onMessageView().mailCard());
    }

    @Test
    @Title("Проверяем кнопку «Написать письмо».")
    @TestCaseId("2034")
    public void shouldOpenCompose() {
        user.defaultSteps().clicksOn(onMessageView().mailCard().composeBtn())
            .shouldSee(onComposePopup().expandedPopup())
            .shouldHasText(onComposePopup().yabbleTo().yabbleText(), RECIPIENT);
    }

    @Test
    @Title("Должны добавить контакт по кнопке «В адресную книгу»")
    @TestCaseId("2035")
    public void shouldAddContactToAbook() {
        user.defaultSteps().clicksOn(onMessageView().mailCard().saveToAbookBtn())
            .refreshPage()
            .clicksOn(onMessageView().messageHead().fromName())
            .shouldNotSee(onMessageView().mailCard().saveToAbookBtn())
            .shouldSee(onMessageView().mailCard().editContactBtn())
            .opensFragment(QuickFragments.CONTACTS)
            .shouldSee(onAbookPage().contacts().waitUntil(not(Matchers.empty())).get(0))
            .shouldSeeElementInList(onAbookPage().contacts(), contact.getEmail().get(0).getValue());
    }

    @Test
    @Title("Проверяем кнопку «Создать правило».")
    @TestCaseId("2037")
    public void shouldCreateFilter() {
        user.defaultSteps().clicksOn(onMessageView().mailCard().createFilterBtn())
            .shouldBeOnUrl(lock.firstAcc(), QuickFragments.SETTINGS_FILTERS_CREATE, "message=" + msg.getMid())
            .shouldContainValue(onFiltersCreationPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(0).inputCondition(), lock.firstAcc().getSelfEmail())
            .shouldContainValue(onFiltersCreationPage().setupFiltersCreate().blockCreateConditions()
                .conditionsList().get(1).inputCondition(), msg.getSubject());
    }

    @Test
    @Title("Редактируем карточку и проверяем изменения в Абуке.")
    @TestCaseId("2038")
    public void shouldBeEditable() {
        user.defaultSteps().clicksOn(onMessageView().mailCard().saveToAbookBtn())
            .shouldSee(onMessageView().mailCard().editContactBtn())
            .clicksOn(onMessageView().mailCard().editContactBtn())
            .shouldSee(onMessageView().editMailCard())
            .inputsTextInElement(onMessageView().editMailCard().name(), contact.getName().getFirst())
            .inputsTextInElement(onMessageView().editMailCard().middleName(), contact.getName().getMiddle())
            .inputsTextInElement(onMessageView().editMailCard().lastName(), contact.getName().getLast())
            .inputsTextInElement(
                onMessageView().editMailCard().addPhoneNumber(), contact.getPhone().get(0).getValue()
            )
            .inputsTextInElement(onMessageView().editMailCard().description(), contact.getDescription())
            .clicksOn(onMessageView().editMailCard().submitContactButton());
        user.abookSteps().checksContactInAbook(contact);
    }
}
