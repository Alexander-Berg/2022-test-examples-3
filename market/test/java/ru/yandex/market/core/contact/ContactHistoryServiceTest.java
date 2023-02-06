package ru.yandex.market.core.contact;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.contact.model.Contact;
import ru.yandex.market.core.contact.model.ContactEmail;
import ru.yandex.market.core.contact.model.ContactLink;
import ru.yandex.market.core.contact.model.ContactWithEmail;

import static java.util.Collections.singletonList;

@DbUnitDataSet(before = "db/ContactHistoryServiceTest.before.csv")
public class ContactHistoryServiceTest extends FunctionalTest {

    @Autowired
    private ContactHistoryService tested;

    @Test
    @DbUnitDataSet(after = "db/ContactHistoryServiceTest.contactEmailHistory.after.csv")
    void testLogContactEmails() {
        ContactEmail email = new ContactEmail(1L, "barabosh@yandex.ru", true, true);

        tested.logCreatedEmails(1, singletonList(email));
        tested.logUpdatedEmails(2, singletonList(email));
        tested.logDeletedEmails(3, singletonList(email));
    }

    @Test
    @DbUnitDataSet(after = "db/ContactHistoryServiceTest.contactHistory.after.csv")
    void testLogContactWithEmail() {
        ContactWithEmail contact = new ContactWithEmail();
        contact.setId(1L);
        contact.setFirstName("Baraboshka");
        contact.setEmails(Set.of(new ContactEmail(1L, "barabosh@yandex.ru", true, true)));
        ContactWithEmail contactToLog = ContactWithEmail.copyOf(contact);
        contactToLog.setEmails(new HashSet<>());

        tested.logCreatedContact(1, contactToLog);
        tested.logUpdatedContact(2, contactToLog);
        tested.logDeletedContact(3, contactToLog);
    }

    @Test
    @DbUnitDataSet(after = "db/ContactHistoryServiceTest.contactHistory.after.csv")
    void testLogContact() {
        Contact contact = new Contact();
        contact.setId(1L);
        contact.setFirstName("Baraboshka");

        tested.logCreatedContact(1, contact);
        tested.logUpdatedContact(2, contact);
        tested.logDeletedContact(3, contact);
    }

    @Test
    @DbUnitDataSet(after = "db/ContactHistoryServiceTest.contactLinkHistory.after.csv")
    void testLogContactLinks() {
        ContactLink link = new ContactLink(1, 1, null);

        tested.logCreatedLinks(1, singletonList(link));
        tested.logUpdatedLinks(2, singletonList(link));
        tested.logDeletedLinks(3, singletonList(link));
    }
}
