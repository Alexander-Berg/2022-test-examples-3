package ru.yandex.autotests.innerpochta.steps;

import org.hamcrest.Matchers;
import ru.yandex.autotests.innerpochta.annotations.SkipIfFailed;
import ru.yandex.autotests.innerpochta.atlas.MailElement;
import ru.yandex.autotests.innerpochta.data.QuickFragments;
import ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.contact.ContactBlock;
import ru.yandex.autotests.innerpochta.ns.pages.abook.blocks.group.EveryGroupBlock;
import ru.yandex.autotests.innerpochta.rules.WebDriverRule;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Email;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Name;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Phone;
import ru.yandex.autotests.innerpochta.util.Utils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.Collections;
import java.util.List;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.filter;
import static ch.lambdaj.Lambda.forEach;
import static ch.lambdaj.Lambda.having;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.autotests.innerpochta.matchers.ContactExistsMatcher.contactExists;
import static ru.yandex.autotests.innerpochta.matchers.ContactExistsMatcher.contactNotExists;
import static ru.yandex.autotests.innerpochta.matchers.PostRefreshMatcherDecorator.withPostRefresh;
import static ru.yandex.autotests.innerpochta.util.SkipStep.SkipStepMethods.assumeStepCanContinue;
import static ru.yandex.autotests.innerpochta.util.Utils.isPresent;
import static ru.yandex.autotests.innerpochta.util.Utils.withWaitFor;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

public class AbookSteps {

    private AllureStepStorage user;
    private WebDriverRule webDriverRule;

    AbookSteps(WebDriverRule webDriverRule, AllureStepStorage user) {
        this.webDriverRule = webDriverRule;
        this.user = user;
    }

    @Step("Создаём рандомный контакт")
    public Contact createDefaultContact() {
        return new Contact()
            .withName(new Name()
                .withFirst(Utils.getRandomString())
                .withMiddle(Utils.getRandomString())
                .withLast(Utils.getRandomString())
            )
            .withDescription(Utils.getRandomName())
            .withEmail(Collections.singletonList(new Email().withValue(Util.getRandomAddress())))
            .withPhone(Collections.singletonList(new Phone().withValue("1111111")));
    }

    @Step("Создаём контакт с именем и почтой")
    public Contact createContactWithParametrs(String name, String email) {
        return new Contact()
            .withDescription("")
            .withName(new Name().withFirst(name).withMiddle("").withLast(""))
            .withEmail(Collections.singletonList(new Email().withValue(email)))
            .withPhone(Collections.singletonList(new Phone().withValue("")));
    }

    @Step("Получаем полное имя")
    public String getFullName(Contact contact) {
        if (contact.getName().getMiddle().equals("")) {
            return (contact.getName().getFirst() + " " + contact.getName().getLast()).trim();
        }
        return (contact.getName().getFirst() + " " + contact.getName().getMiddle() + " " + contact.getName().getLast())
            .trim();
    }

    @Step("Получаем полную дату рождения")
    private String getFullBDay(Contact contact) {
        return (contact.getBirthdate().getDay() + " " + contact.getBirthdate().getMonth() + " "
            + contact.getBirthdate().getYear()).trim();
    }

    @Step("Добавляем контакт {0}")
    public AbookSteps addsContact(Contact contact) {
        user.defaultSteps().shouldSee(user.pages().AbookPage().toolbarBlock().addContactButton())
            .clicksOn(user.pages().AbookPage().toolbarBlock().addContactButton());
        assertThat(
            "Окно создания нового контакта не появилось",
            user.pages().AbookPage().addContactPopup(),
            withWaitFor(isPresent())
        );
        user.defaultSteps().inputsTextInElement(user.pages().AbookPage().addContactPopup().name(), contact.getName().getFirst())
            .inputsTextInElement(user.pages().AbookPage().addContactPopup().middleName(), contact.getName().getMiddle())
            .inputsTextInElement(user.pages().AbookPage().addContactPopup().lastName(), contact.getName().getLast())
            .inputsTextInElement(user.pages().AbookPage().addContactPopup().addNewAddress(), contact.getEmail().get(0).getValue())
            .inputsTextInElement(user.pages().AbookPage().addContactPopup().addPhoneNumber(), contact.getPhone().get(0).getValue())
            .inputsTextInElement(user.pages().AbookPage().addContactPopup().description(), contact.getDescription());
        inputsBDay(contact);
        user.defaultSteps().clicksOn(user.pages().AbookPage().addContactPopup().addContactButton())
            .shouldNotSee(user.pages().AbookPage().addContactPopup());
        return this;
    }

    @Step("Проверяем, что контакт добавился правильно и удаляем его")
    public AbookSteps checksContactInAbook(Contact contact) {
        user.defaultSteps().opensFragment(QuickFragments.CONTACTS);
        shouldSeeContact(contact);
        clicksOnContact(contact);
        shouldSeeContactDetails(contact);
        deleteContact(contact);
        return this;
    }

    @SkipIfFailed
    @Step("Вводим дату рождения")
    public AbookSteps inputsBDay(Contact contact) {
        assumeStepCanContinue(contact.getBirthdate().getDay(), not(equalTo("")));
        user.defaultSteps()
            .clicksOn(user.pages().AbookPage().addContactPopup().contactBDay())
            .clicksOnElementWithText(
                user.pages().AbookPage().selectConditionDropdown().conditionsList(), contact.getBirthdate().getDay())
            .clicksOn(user.pages().AbookPage().addContactPopup().contactBMonth())
            .clicksOnElementWithText(
                user.pages().AbookPage().selectConditionDropdown().conditionsList(), contact.getBirthdate().getMonth())
            .clicksOn(user.pages().AbookPage().addContactPopup().contactBYear())
            .clicksOnElementWithText(
                user.pages().AbookPage().selectConditionDropdown().conditionsList(), contact.getBirthdate().getYear()
            );
        return this;
    }

    @Step("Должен существовать контакт")
    public AbookSteps shouldSeeContact(Contact cont) {
        assertThat(
            "Контакт не создался",
            user.pages().AbookPage(),
            withWaitFor(withPostRefresh(contactExists(cont.getName().getFirst()), webDriverRule.getDriver()))
        );
        ContactBlock contact = findContactByName(cont.getName().getFirst());
        assertThat("Имя контакта добавилось неверно", contact.name(), hasText(getFullName(cont)));
        return this;
    }

    @Step("Должен существовать контакт")
    public AbookSteps shouldSeeContactWithWaiting(String name, int timeout) {
        assertThat(
            "Контакт не найден на странице",
            user.pages().AbookPage(),
            withWaitFor(withPostRefresh(contactExists(name), webDriverRule.getDriver()), timeout)
        );
        return this;
    }

    @Step("Клик по контакту")
    public AbookSteps clicksOnContact(Contact contact) {
        findContactByName(contact.getName().getFirst()).name().click();
        return this;
    }

    @Step("Ищем в списке контактов: «{0}»")
    public ContactBlock findContactByName(String name) {
        for (ContactBlock contact : user.pages().AbookPage().contacts().waitUntil(not(empty()))) {
            if (contact.name().getText().contains(name)) {
                return contact;
            }
        }
        return null;
    }

    @Step("Проверяем, что данные контакта верны")
    public AbookSteps shouldSeeContactDetails(Contact contact) {
        assertThat(
            "Неверное имя контакта",
            user.pages().AbookPage().contactPopup().contactName().waitUntil("Имя контакта не появилось", isDisplayed(), 10).getText(),
            is(getFullName(contact))
        );
        shouldSeeCorrectEmail(contact);
        shouldSeeCorrectDescription(contact);
        shouldSeeCorrectPhoneNumber(contact);
        shouldSeeContactFullBDay(contact);
        return this;
    }

    @Step("Проверяем почту контакта")
    public AbookSteps shouldSeeCorrectEmail(Contact contact) {
        if (isPresent().matches(user.pages().AbookPage().contactPopup().contactEmail())) {
            List<String> emails = extract(user.pages().AbookPage().contactPopup().contactEmail(), ch.lambdaj.Lambda.on(MailElement.class)
                .getText());
            assertThat("Неверная почта в деталях контакта", emails, hasItems(contact.getEmail().get(0).getValue()));
        }
        return this;
    }

    @Step("Проверяем описание контакта")
    public AbookSteps shouldSeeCorrectDescription(Contact contact) {
        if (isPresent().matches(user.pages().AbookPage().contactPopup().description())) {
            assertThat("Неверное описание контакта", user.pages().AbookPage().contactPopup()
                .description().getText(), is(contact.getDescription()));
        }
        return this;
    }

    @Step("Проверяем номер телефона")
    public AbookSteps shouldSeeCorrectPhoneNumber(Contact contact) {
        if (isPresent().matches(user.pages().AbookPage().contactPopup().contactPhone())) {
            List<String> phones = extract(user.pages().AbookPage().contactPopup().contactPhone(), ch.lambdaj.Lambda.on(MailElement.class)
                .getText());
            assertThat("Неверный номер телефона контакта", phones, hasItems(contact.getPhone().get(0).getValue()));
        }
        return this;
    }

    @Step("Проверяем дату рождения")
    public AbookSteps shouldSeeContactFullBDay(Contact contact) {
        if (isPresent().matches(user.pages().AbookPage().contactPopup().contactBDay())) {
            assertThat("Неверный день рождения в деталях контакта", user.pages().AbookPage().contactPopup()
                .contactBDay(), hasText(getFullBDay(contact)));

        }
        return this;
    }

    @Step("Кликаем на кнопку “Удалить контакт“")
    public AbookSteps deleteContact(Contact contact) {
        selectContact(contact);
        user.defaultSteps().clicksOn(user.pages().AbookPage().toolbarBlock().deleteContactButton())
            .shouldNotSee(user.pages().AbookPage().toolbarBlock().deleteContactButton());
        shouldNotSeeContact(contact);
        return this;
    }

    @Step("Не должен существовать контакт")
    public AbookSteps shouldNotSeeContact(Contact contact) {
        assertThat("Контакт не удалился", user.pages().AbookPage(), withWaitFor(contactNotExists(contact.getName().getFirst())));
        return this;
    }

    @Step("Добавляем контакты {0} к группе")
    public AbookSteps addsContactsToGroup(Integer... indexes) {
        assertThat("Количество контактов меньше, чем нужно", user.pages().AbookPage().createNewGroupPopup().contacts(),
            withWaitFor(hasSize(greaterThanOrEqualTo(indexes.length))));
        for (int i : indexes) {
            user.pages().AbookPage().createNewGroupPopup().contacts().get(i).click();
        }
        return this;
    }

    @Step("Клик по группе с названием «{0}»")
    public AbookSteps clicksOnGroup(final String name) {
        List<EveryGroupBlock> groups = filter(having(ch.lambdaj.Lambda.on(EveryGroupBlock.class).groupName(), hasText(name)),
            user.pages().AbookPage().groupsBlock().groups());
        assertThat("Группа не существует", groups, hasSize(greaterThan(0)));
        user.defaultSteps().clicksOn(groups.get(0).groupName());
        return this;
    }

    @Step("Количество контактов должно быть “{0}“")
    public AbookSteps shouldSeeNumberOfContacts(int number) {
        assertThat("Неверное количество контактов!", user.pages().AbookPage().contacts(), hasSize(number));
        return this;
    }

    @Step("Количество контактов в группе «{0}» должно быть «{1}»")
    public AbookSteps shouldSeeGroupCounter(String name, int number) {
        user.defaultSteps().waitInSeconds(1);
        List<EveryGroupBlock> groups = filter(
            hasText(Matchers.containsString(name)),
            user.pages().AbookPage().groupsBlock().groups()
        );
        assertThat("Нет группы с нужным именем", groups, hasSize(greaterThan(0)));
        assertThat(
            "Неверное значение счетчика контактов группы",
            groups.get(0).groupCounter(),
            withWaitFor(hasText(Integer.toString(number)))
        );
        return this;
    }

    @Step("Выделяем все контакты (чекбоксы)")
    public AbookSteps checksAllContactsCheckBoxes() {
        forEach(user.pages().AbookPage().contacts().waitUntil(not(empty()))).contactCheckBox().setChecked(true);
        return this;
    }

    @Step("Добавляем выделенные контакты к группе «{0}»")
    public AbookSteps addsContactToGroup(String name) {
        user.defaultSteps().clicksOn(user.pages().AbookPage().toolbarBlock().addContactToGroupButton())
            .shouldSee(user.pages().AbookPage().groupSelectDropdown())
            .clicksOnElementWithText(user.pages().AbookPage().groupSelectDropdown().groupsNames(), name);
        return this;
    }

    @Step("Ищем контакт: “{0}“ Поиском по контактам")
    public AbookSteps searchesForContact(String name) {
        assertThat("Поля поиска контактов нет на странице", user.pages().AbookPage().toolbarBlock().searchInput(),
            withWaitFor(isPresent()));
        user.pages().AbookPage().toolbarBlock().searchInput().sendKeys(name);
        return this;
    }

    @Step("Кликаем по Аватару контакта")
    public AbookSteps selectContact(Contact contact) {
        findContactByName(contact.getName().getFirst()).contactCheckBox().click();
        return this;
    }

    @Step("Счетчик контактов в личных контактах должен быть «{0}»")
    public AbookSteps shouldSeePersonalContactsCounter(int number) {
        assertThat(
            "Личных контактов нет на странице",
            user.pages().AbookPage().groupsBlock().personalContacts(),
            withWaitFor(isPresent())
        );
        assertThat(
            "Неверное значение счетчика личных контактов группы",
            user.pages().AbookPage().groupsBlock().personalContactsCounter(),
            withWaitFor(hasText(Integer.toString(number)))
        );
        return this;
    }

    @Step("Email должен совпадать с адресом {0}")
    public AbookSteps shouldSeeEmail(String email) {
        assertThat(
            "Неверный адрес в деталях контакта",
            user.pages().AbookPage().contactPopup().contactEmail().get(0), hasText(email)
        );
        return this;
    }
}
