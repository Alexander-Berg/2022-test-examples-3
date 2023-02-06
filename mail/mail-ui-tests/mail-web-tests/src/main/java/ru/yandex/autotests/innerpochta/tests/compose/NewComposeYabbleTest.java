package ru.yandex.autotests.innerpochta.tests.compose;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import io.qameta.allure.junit4.Tag;
import io.qameta.atlas.webdriver.ElementsCollection;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebElement;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.ns.pages.composeblocks.blocks.YabbleBlock;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.rules.SendToStatAfterClassRule;
import ru.yandex.autotests.innerpochta.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.steps.AllureStepStorage;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Email;
import ru.yandex.autotests.innerpochta.tests.BaseTest;
import ru.yandex.autotests.innerpochta.util.FeaturesConst;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.ImmutableMap.of;
import static ru.yandex.autotests.innerpochta.data.GreetingMessageData.RU_MAIL_EN_LANG_SERVICES;
import static ru.yandex.autotests.innerpochta.rules.resources.RemoveAllContactsFromAbookRule.removeAllContactsFromAbookRule;
import static ru.yandex.autotests.innerpochta.util.ScriptConst.SCRIPT_FOR_SCROLLTOP;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;
import static ru.yandex.autotests.innerpochta.util.handlers.HandlersParamNameConstants.TRUE;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_FOLDER_THREAD_VIEW;
import static ru.yandex.autotests.innerpochta.util.handlers.SettingsConstants.SETTINGS_HEAD_FULL_EDITION;

/**
 * @author marchart
 */
@Aqua.Test
@Title("Новый композ - Ябблы")
@Features({FeaturesConst.COMPOSE, FeaturesConst.NOT_TUS})
@Tag(FeaturesConst.COMPOSE)
@Stories(FeaturesConst.HEAD)
@RunWith(DataProviderRunner.class)
public class NewComposeYabbleTest extends BaseTest {
    private static final String CONTACT_WITH_TWO_EMAILS = "Два Адреса";
    private static final String CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL = "testbot3@yandex.ru";
    private static final String CONTACT_WITH_TWO_EMAILS_SECOND_EMAIL = "testbot4@yandex.ru";
    private static final String LONG_BODY_TEXT = RU_MAIL_EN_LANG_SERVICES + "\n";
    private static final String MANY_RECIPIENTS =
        "testbot1@yandex.ru, testbot2@yandex.ru, testbot3@yandex.ru, testbot5@yandex.ru, testbot6@yandex.ru";
    private static final String HIDDEN_YABBLE_1 = "testbot5@yandex.ru";
    private static final String HIDDEN_YABBLE_2 = "testbot6@yandex.ru";
    private Contact contact, contact1, contact2;
    private String groupName;
    String msgSubject;
    String msgTo;

    List<Email> emails = Arrays.asList(
        new Email().withValue(CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL),
        new Email().withValue(CONTACT_WITH_TWO_EMAILS_SECOND_EMAIL)
    );

    private AccLockRule lock = AccLockRule.use().className();
    private RestAssuredAuthRule auth = RestAssuredAuthRule.auth(lock);
    private AllureStepStorage user = new AllureStepStorage(webDriverRule, auth);

    @ClassRule
    public static SendToStatAfterClassRule sendToStat = new SendToStatAfterClassRule();

    @Rule
    public RuleChain rules = RuleChain.outerRule(lock)
        .around(auth)
        .around(removeAllContactsFromAbookRule(user));

    @Before
    public void setUp() {
        user.apiAbookSteps().removeAllAbookGroups();
        contact = user.abookSteps().createContactWithParametrs(
            CONTACT_WITH_TWO_EMAILS,
            CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL
        ).withEmail(emails);
        user.apiAbookSteps().addContactWithTwoEmails(CONTACT_WITH_TWO_EMAILS, contact);
        msgSubject = getRandomString();
        msgTo = lock.firstAcc().getSelfEmail();
        user.apiSettingsSteps().callWithListAndParams(
            "Отключаем треды и сбрасываем свёрнутые композы",
            of(
                SETTINGS_FOLDER_THREAD_VIEW, false,
                SETTINGS_HEAD_FULL_EDITION, TRUE
            )
        );
        contact1 = user.abookSteps().createDefaultContact();
        contact2 = user.abookSteps().createDefaultContact();
        groupName = getRandomString();
        user.apiAbookSteps().addNewContacts(contact1, contact2);
        user.apiAbookSteps().addNewAbookGroupWithContacts(
            groupName,
            user.apiAbookSteps().getPersonalContacts().get(0),
            user.apiAbookSteps().getPersonalContacts().get(1)
        );
        user.loginSteps().forAcc(lock.firstAcc()).logins();
        user.defaultSteps().clicksOn(onMessagePage().composeButton());
    }

    @Test
    @Title("Отправка письма после выбора второго адреса из меню яббла")
    @TestCaseId("3089")
    public void shouldSendChangedEmailFromYabbleDropdownMenu() {
        user.defaultSteps().inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), CONTACT_WITH_TWO_EMAILS)
            .shouldSee(onComposePopup().suggestList().get(0))
            .clicksOnElementWithText(onComposePopup().suggestList(), CONTACT_WITH_TWO_EMAILS_FIRST_EMAIL)
            .clicksOn(onComposePopup().yabbleTo())
            .clicksOnElementWithText(
                onComposePopup().yabbleDropdown().changeEmail(),
                CONTACT_WITH_TWO_EMAILS_SECOND_EMAIL
            )
            .clicksOn(onComposePopup().expandedPopup().sendBtn());
        waitForMessageToBeSendAndCheckRecipient(CONTACT_WITH_TWO_EMAILS_SECOND_EMAIL, msgSubject);
    }

    @Test
    @Title("Открытие попапа яббла")
    @TestCaseId("5696")
    public void shouldSeeYabbleMenu() {
        user.defaultSteps().inputsTextInElement(onComposePopup().expandedPopup().popupTo(), CONTACT_WITH_TWO_EMAILS)
            .clicksOn(onComposePopup().suggestList().get(0))
            .clicksOn(onComposePopup().yabbleTo())
            .shouldSee(
                onComposePopup().yabbleDropdown().changeEmail().get(0),
                onComposePopup().yabbleDropdown().copyEmail(),
                onComposePopup().yabbleDropdown().editYabble(),
                onComposePopup().yabbleDropdown().singleTarget()
            )
            .clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .clicksOn(onComposePopup().yabbleFrom())
            .shouldSee(
                onComposePopup().yabbleDropdown().changeEmail().get(0),
                onComposePopup().yabbleDropdown().copyEmail(),
                onComposePopup().yabbleDropdown().editYabble(),
                onComposePopup().yabbleDropdown().singleTarget()
            );
    }

    @Test
    @Title("Удаляем добавленный яббл")
    @TestCaseId("5702")
    public void shouldDeleteYabble() {
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().expandCollapseBtn())
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), CONTACT_WITH_TWO_EMAILS)
            .clicksOn(onComposePopup().suggestList().get(0))
            .clicksOn(onComposePopup().yabbleTo().yabbleDeleteBtn())
            .shouldNotSee(onComposePopup().yabbleTo())
            .inputsTextInElement(onComposePopup().expandedPopup().popupCc(), CONTACT_WITH_TWO_EMAILS)
            .clicksOn(onComposePopup().suggestList().get(0))
            .clicksOn(onComposePopup().yabbleCc().yabbleDeleteBtn())
            .shouldNotSee(onComposePopup().yabbleCc())
            .inputsTextInElement(onComposePopup().expandedPopup().popupBcc(), CONTACT_WITH_TWO_EMAILS)
            .clicksOn(onComposePopup().suggestList().get(0))
            .clicksOn(onComposePopup().yabbleBcc().yabbleDeleteBtn())
            .shouldNotSee(onComposePopup().yabbleBcc())
            .shouldNotSee(onComposePopup().yabbleFrom().yabbleDeleteBtn());
    }

    @Test
    @Title("Яббл «Ещё» формируется в адресных полях")
    @TestCaseId("5701")
    public void shouldSeeMoreYabble() {
        user.defaultSteps().setsWindowSize(1200, 800)
            .clicksOn(onComposePopup().expandedPopup().expandCollapseBtn());
        checkMoreYabble(onComposePopup().expandedPopup().popupTo(), onComposePopup().yabbleToList());
        checkMoreYabble(onComposePopup().expandedPopup().popupCc(), onComposePopup().yabbleCcList());
        checkMoreYabble(onComposePopup().expandedPopup().popupBcc(), onComposePopup().yabbleBccList());
    }

    @Test
    @Title("Формируем групповой яббл")
    @TestCaseId("5724")
    public void shouldSeeGroupYabble() {
        user.defaultSteps().inputsTextInElement(onComposePopup().expandedPopup().sbjInput(), msgSubject)
            .inputsTextInElement(onComposePopup().expandedPopup().popupTo(), groupName)
            .clicksOn(onComposePopup().suggestList().get(0))
            .clicksOn(onComposePopup().yabbleTo())
            .shouldNotSee(
                onComposePopup().yabbleDropdown().copyEmail(),
                onComposePopup().yabbleDropdown().editYabble(),
                onComposePopup().yabbleDropdown().singleTarget()
            )
            .clicksOn(onComposePopup().yabbleDropdown().changeEmail().get(1))
            .shouldSeeElementInList(onComposePopup().yabbleToList(), groupName);
        user.defaultSteps().clicksOn(onComposePopup().expandedPopup().sendBtn())
            .opensFragment(QuickFragments.SENT)
            .refreshPage();
        user.messagesSteps().clicksOnMessageWithSubject(msgSubject);
        user.defaultSteps()
            .clicksOn(onMessageView().messageHead().recipientsCount())
            .shouldSeeElementInList(onMessageView().messageHead().contactsInTo(), contact1.getName().getFirst());
        user.defaultSteps()
            .shouldSeeElementInList(onMessageView().messageHead().contactsInTo(), contact2.getName().getFirst());
    }

    @Step("Ждём отправки сообщения и проверяем получателя в отправленных")
    private void waitForMessageToBeSendAndCheckRecipient(String recipient, String subject) {
        user.composeSteps().waitForMessageToBeSend();
        user.defaultSteps().opensFragment(QuickFragments.SENT).refreshPage();
        user.messagesSteps().shouldSeeMessageWithSubject(subject)
            .shouldSeeAddressOnMessageWithSubject(recipient, subject);
    }

    @Step("Проверяем формирование яббла «Ещё» в поле «{0}»")
    private void checkMoreYabble(WebElement input, ElementsCollection<YabbleBlock> yabbleList) {
        user.defaultSteps().inputsTextInElement(input, MANY_RECIPIENTS)
            .appendTextInElement(onComposePopup().expandedPopup().bodyInput(), LONG_BODY_TEXT)
            .shouldNotSeeElementInList(yabbleList, HIDDEN_YABBLE_1)
            .shouldNotSeeElementInList(yabbleList, HIDDEN_YABBLE_2)
            .executesJavaScript(SCRIPT_FOR_SCROLLTOP)
            .clicksOn(onComposePopup().yabbleMore())
            .shouldSeeElementInList(yabbleList, HIDDEN_YABBLE_1);
        user.defaultSteps().shouldSeeElementInList(yabbleList, HIDDEN_YABBLE_2);
        user.defaultSteps().clearTextInput(input);
    }
}
