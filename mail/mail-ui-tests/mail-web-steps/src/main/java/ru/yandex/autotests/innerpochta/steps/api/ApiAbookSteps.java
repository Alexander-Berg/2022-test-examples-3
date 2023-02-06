package ru.yandex.autotests.innerpochta.steps.api;

import edu.emory.mathcs.backport.java.util.Arrays;
import ru.yandex.autotests.innerpochta.rules.RestAssuredAuthRule;
import ru.yandex.autotests.innerpochta.steps.beans.AbookGroup;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Contact;
import ru.yandex.autotests.innerpochta.steps.beans.contact.Phone;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Step;

import java.util.List;
import java.util.stream.Stream;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.selectFirst;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.innerpochta.api.abook.AbookContactsHandler.abookContactsHandler;
import static ru.yandex.autotests.innerpochta.api.abook.AbookGroupsHandler.abookGroupsHandler;
import static ru.yandex.autotests.innerpochta.api.abook.DoAbookGroupAddHandler.doAbookGroupAddHandler;
import static ru.yandex.autotests.innerpochta.api.abook.DoAbookGroupRemoveHandler.doAbookGroupRemoveHandler;
import static ru.yandex.autotests.innerpochta.api.abook.DoAbookPersonAddHandler.doAbookPersonAddHandler;
import static ru.yandex.autotests.innerpochta.api.abook.DoAbookPersonDeleteHandler.doAbookPersonDeleteHandler;
import static ru.yandex.autotests.innerpochta.api.abook.DoAbookPersonUpdateHandler.doAbookPersonUpdateHandler;
import static ru.yandex.autotests.innerpochta.steps.api.ApiDefaultSteps.getJsonPathConfig;
import static ru.yandex.autotests.innerpochta.util.Utils.getRandomString;

/**
 * Created by mabelpines
 */
public class ApiAbookSteps {

    public RestAssuredAuthRule auth;

    public ApiAbookSteps withAuth(RestAssuredAuthRule auth) {
        this.auth = auth;
        return this;
    }

    @Step("Вызов api-метода: abook-groups. Получаем список групп контактов.")
    public List<AbookGroup> getAllAbookGroups() {
        return Arrays.asList(abookGroupsHandler().withAuth(auth).callAbookGroupsHandler().then().extract()
            .jsonPath(getJsonPathConfig()).getObject("models[0].data.tag", AbookGroup[].class));
    }

    @Step("Вызов api-метода: do-abook-group-add. Добавляем группу контактов - “{0}“")
    public AbookGroup addNewAbookGroup(String title) {
        doAbookGroupAddHandler().withAuth(auth).withTitle(title).callDoAbookGroupAddHandler();
        return selectFirst(getAllAbookGroups(), having(on(AbookGroup.class).getTitle(), equalTo(title)));
    }

    @Step("Вызов api-метода: do-abook-group-add. Добавляем группу с контактами")
    public AbookGroup addNewAbookGroupWithContacts(String title, Contact... contacts) {
        doAbookGroupAddHandler().withAuth(auth)
            .withContacts(java.util.Arrays.asList(contacts))
            .withTitle(title)
            .callDoAbookGroupAddHandler();
        return selectFirst(getAllAbookGroups(), having(on(AbookGroup.class).getTitle(), equalTo(title)));
    }

    @Step("Удаляем все группы контактов пользователя.")
    public ApiAbookSteps removeAllAbookGroups() {
        getAllAbookGroups().forEach(aGroup -> {
            if (!aGroup.getType().equals("system")) {
                removeAbookGroup(
                    aGroup.getTitle(),
                    aGroup
                );
            }
        });
        return this;
    }

    @Step("Вызов api-метода: do-abook-group-remove. Удаляем группу - “{0}“")
    public ApiAbookSteps removeAbookGroup(String titile, AbookGroup aGroup) {
        if (aGroup.getTid() != null)
            doAbookGroupRemoveHandler().withAuth(auth).withTid(Long.toString(aGroup.getTid())).callDoAbookGroupRemoveHandler();
        return this;
    }

    @Step("Вызов api-метода: do-abook-person-delete. Удаляем из Адресной книги контакт - “{0}“")
    public ApiAbookSteps removeAbookContact(String cid) {
        if (cid != null)
            doAbookPersonDeleteHandler().withAuth(auth).withCid(cid).callDoAbookPersonDeleteHandler();
        return this;
    }

    @Step("Удаляем все контакты из адресной книги")
    public ApiAbookSteps removeAllAbookContacts() {
        getPersonalContacts().forEach(contact -> removeAbookContact(contact.getCid().toString()));
        return this;
    }

    @Step("Вызов api-метода: abook-contacts. Получаем список личных контактов пользователя.")
    public List<Contact> getPersonalContacts() {
        return Arrays.asList(abookContactsHandler().withAuth(auth).callAbookContactsHandler().then().extract()
            .jsonPath(getJsonPathConfig()).getObject("models[0].data.contact", Contact[].class));
    }

    @Step("Вызов api-метода: abook-contacts. Получаем список общих контактов пользователя.")
    public List<Contact> getSharedContacts() {
        return Arrays.asList(abookContactsHandler().withAuth(auth).withSharedContacts().callAbookContactsHandler()
            .then().extract().jsonPath(getJsonPathConfig()).getObject("models[0].data.contact", Contact[].class));
    }

    @Step("Вызов api-метода: do-abook-person-add. Добавляем контакты - “{0}“")
    public ApiAbookSteps addNewContacts(Contact... contacts) {
        Stream.of(contacts).forEach(contact -> addContact(contact.getName().getFirst(), contact));
        return this;
    }

    @Step("Добавляем контакт: {0}")
    public ApiAbookSteps addContact(String contactName, Contact contact) {
        doAbookPersonAddHandler().withAuth(auth)
            .withDescr(contact.getDescription())
            .withFirstName(contact.getName().getFirst())
            .withMiddleName(contact.getName().getMiddle())
            .withLastName(contact.getName().getLast())
            .withEmail(contact.getEmail().get(0).getValue())
            .withTelList(contact.getPhone().get(0).getValue()).callDoAbookPersonAddHandler();
        return this;
    }

    @Step("Добавляем контакт: {0} с двумя адресами")
    public ApiAbookSteps addContactWithTwoEmails(String contactName, Contact contact) {
        doAbookPersonAddHandler().withAuth(auth)
            .withDescr(contact.getDescription())
            .withFirstName(contact.getName().getFirst())
            .withMiddleName(contact.getName().getMiddle())
            .withLastName(contact.getName().getLast())
            .withEmail(contact.getEmail().get(0).getValue())
            .withEmail(contact.getEmail().get(1).getValue())
            .withTelList(contact.getPhone().get(0).getValue()).callDoAbookPersonAddHandler();
        return this;
    }

    @Step("Добавляем {0} новых контактов")
    public ApiAbookSteps addCoupleOfContacts(int numOfContacts) {
        for  (int i = 0; i < numOfContacts; i++) {
            doAbookPersonAddHandler().withAuth(auth)
                .withDescr(getRandomString())
                .withFirstName(getRandomString())
                .withMiddleName(getRandomString())
                .withLastName(getRandomString())
                .withEmail(Util.getRandomAddress())
                .withTelList(new Phone().withValue("1111111").getValue())
                .callDoAbookPersonAddHandler();
        }
        return this;
    }

    @Step("Редактируем общий контакт: {0}")
    public ApiAbookSteps editSharedContact(Contact editedContact) {
        doAbookPersonUpdateHandler().withAuth(auth).withSharedFlag()
            .withPersonID(editedContact.getCid().toString())
            .withDescr(editedContact.getDescription())
            .withFirstName(editedContact.getName().getFirst())
            .withLastName(editedContact.getName().getLast())
            .withEmail(editedContact.getEmail().get(0).getValue())
            .callDoAbookPersonUpdateHandler();
        return this;
    }
}
