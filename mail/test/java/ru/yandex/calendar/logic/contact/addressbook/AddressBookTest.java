package ru.yandex.calendar.logic.contact.addressbook;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.val;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.calendar.frontend.caldav.proto.PutResponse;
import ru.yandex.calendar.frontend.caldav.proto.facade.ContactEtag;
import ru.yandex.calendar.frontend.caldav.proto.facade.ContactVcard;
import ru.yandex.calendar.logic.contact.Contact;
import ru.yandex.calendar.logic.ics.iv5j.vcard.VcfVCard;
import ru.yandex.calendar.logic.ics.iv5j.vcard.property.VcfEmail;
import ru.yandex.calendar.logic.ics.iv5j.vcard.property.VcfFn;
import ru.yandex.calendar.logic.ics.iv5j.vcard.property.VcfN;
import ru.yandex.calendar.logic.ics.iv5j.vcard.property.VcfVersion;
import ru.yandex.calendar.test.CalendarTestBase;
import ru.yandex.calendar.test.generic.CalendarSpringJUnit4ClassRunner;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.db.embedded.ActivateEmbeddedPg;
import ru.yandex.misc.email.Email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;


@Value
@AllArgsConstructor
class ContactVcardInfo {
    String fileName;
    VcfVCard vcard;

    ContactVcardInfo(ContactVcard contactVcard) {
        this(contactVcard.getFileName(), contactVcard.getVcard());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ContactVcardInfo)) {
            return false;
        }

        val that = (ContactVcardInfo) obj;
        return that.fileName.equals(fileName) &&
                that.vcard.getEmail().equals(vcard.getEmail());
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, vcard.getEmail());
    }
}

@RunWith(CalendarSpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AddressBookTestConfiguration.class)
@ActivateEmbeddedPg
public final class AddressBookTest extends CalendarTestBase {
    private static final PassportUid COLLIE_UID = PassportUid.cons(723042009); // caltst@yandex.ru - transferred to collie
    private static final String FILE_NAME = "forrest-gump";
    private static final String USER_FULLNAME = "Forrest Gump";
    private static final Email EMAIL = new Email("running@man.nn");
    private static final List<Email> EXTERNAL_EMAILS = List.of(new Email("one@yandex.ru"), new Email("two@yandex.ru"));
    private static final VcfVCard VCARD = new VcfVCard(Cf.list(
            VcfVersion.VERSION_3_0,
            new VcfN("Gump", "Forrest", "", "", ""),
            new VcfFn(USER_FULLNAME),
            new VcfEmail(EMAIL)
    ));

    @Autowired
    private AddressBook addressBook;

    private String[] getFileNames(List<ContactEtag> etags) {
        return StreamEx.of(etags)
                .map(ContactEtag::getFileName)
                .toArray(String[]::new);
    }

    private static Map<String, Optional<ContactVcardInfo>> extractContactsInfo(Tuple2List<String, Option<ContactVcard>> contacts) {
        return EntryStream.of(contacts.toMap())
                .mapValues(optContactVcard -> optContactVcard.map(ContactVcardInfo::new).toOptional())
                .toMap();
    }

    @Before
    public void cleanAddressBookBeforeTest() {
        addressBook.cleanAddressBook(COLLIE_UID);
    }

    private PutResponse putCarddavContact() {
        return addressBook.putCarddavContact(COLLIE_UID, FILE_NAME, Optional.empty(), VCARD);
    }

    @Test
    public void testPutResponse() {
        val colliePutResult = putCarddavContact();
        assertThat(colliePutResult.getStatusCode())
                .isBetween(200, 299);
        assertThat(colliePutResult.getDescription()).isEmpty();
        assertThat(colliePutResult.getEtag()).isNotEmpty();
    }

    @Test
    public void testCollieEtags() {
        putCarddavContact();
        val collieEtags = addressBook.getCarddavContactEtags(COLLIE_UID);
        assertThat(collieEtags).isNotEmpty();
        assertThat(getFileNames(collieEtags)).containsExactly(FILE_NAME);
    }

    @Test
    public void testCollieContacts() {
        putCarddavContact();
        val collieContacts = addressBook.getCarddavContactsByFileNames(COLLIE_UID, List.of(FILE_NAME));
        assertThat(extractContactsInfo(collieContacts).keySet()).containsExactly(FILE_NAME);
    }

    @Test
    public void testCollieQueries() {
        putCarddavContact();

        val expected = new Contact(EMAIL, USER_FULLNAME);

        val query1 = "running";

        // await is required, because findUserContacts queries aceventura, that may not fetch results from Collie immediately
        await().pollInterval(1, TimeUnit.SECONDS).atMost(30, TimeUnit.SECONDS).until(() -> addressBook.findUserContacts(COLLIE_UID, query1, 10).isNotEmpty());

        try {
            val collieContactsByQuery1 = addressBook.findUserContacts(COLLIE_UID, query1, 10);
            assertThat(collieContactsByQuery1).containsExactly(expected);

            val query2 = "forrest";
            val collieContactsByQuery2 = addressBook.findUserContacts(COLLIE_UID, query2, 10);
            assertThat(collieContactsByQuery2).containsExactly(expected);

            val collieContactsByEmail = addressBook.findUserContactsByEmails(COLLIE_UID, List.of(EMAIL));
            assertThat(collieContactsByEmail).containsExactly(expected);
        } catch (Exception e) {
            if (!e.getMessage().contains("Gateway time out")) {
                fail("e", e);
            }
        }
    }

    @Test
    public void testExport() {
        putCarddavContact();
        addressBook.exportUserContactsEmails(COLLIE_UID, EXTERNAL_EMAILS);
        val emails = addressBook.suggestContacts(COLLIE_UID);
        assertThat(emails).containsAll(EXTERNAL_EMAILS);
    }

    @Test
    public void testCleanup() {
        putCarddavContact();
        addressBook.cleanAddressBook(COLLIE_UID);
        assertThat(addressBook.getCarddavContactEtags(COLLIE_UID)).isEmpty();
    }
}
