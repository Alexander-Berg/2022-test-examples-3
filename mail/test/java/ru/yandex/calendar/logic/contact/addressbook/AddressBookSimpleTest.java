package ru.yandex.calendar.logic.contact.addressbook;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.calendar.logic.contact.Contact;
import ru.yandex.misc.email.Email;

import static org.assertj.core.api.Assertions.assertThat;

public class AddressBookSimpleTest {
    @Test
    public void testParse() {
        final ListF<Contact> contacts = AddressBook.parseCollieSearchContactsResponse("{\"raporlama@xyz" +
                ".com\":{\"contact_owner_user_id\":1130000027135288,\"contact_owner_user_type\":\"passport_user\"," +
                "\"contact_id\":7,\"list_id\":1,\"revision\":\"\",\"tag_ids\":[],\"emails\":[{\"id\":7," +
                "\"value\":\"raporlama@xyz.com\",\"last_usage\":1590181951032,\"tags\":[]}]," +
                "\"vcard\":{\"emails\":[{\"email\":\"raporlama@xyz.com\"}]," +
                "\"names\":[{\"first\":\"raporlama\"}]}},\"yavuz.gokcel@xyx" +
                ".com\":{\"contact_owner_user_id\":1130000027135288,\"contact_owner_user_type\":\"passport_user\"," +
                "\"contact_id\":56,\"list_id\":1,\"revision\":\"\",\"tag_ids\":[],\"emails\":[{\"id\":52," +
                "\"value\":\"yavuz.gokcel@xyx.com\",\"last_usage\":1583989757965,\"tags\":[]}]," +
                "\"vcard\":{\"emails\":[{\"email\":\"yavuz.gokcel@xyx.com\"}]," +
                "\"names\":[{\"last\":\"GÖKÇEL\",\"first\":\"Yavuz\"}],\"directory_entries\":[{\"type\":[\"u\"]," +
                "\"entry_id\":1130000027135288}],\"events\":[{\"month\":1,\"year\":2000,\"type\":[\"birthday\"]," +
                "\"day\":1}]}},\"satis@xyz.com\":{\"contact_owner_user_id\":1130000027135288," +
                "\"contact_owner_user_type\":\"passport_user\",\"contact_id\":4,\"list_id\":1,\"revision\":\"\"," +
                "\"tag_ids\":[],\"emails\":[{\"id\":4,\"value\":\"satis@xyz.com\"," +
                "\"last_usage\":1578614400000,\"tags\":[]}],\"vcard\":{\"emails\":[{\"email\":\"satis@xyz" +
                ".com\"}],\"names\":[{\"first\":\"Analitik Çevre Laboratuvarı\"}]}},\"ahmet.kilic@xyx" +
                ".com\":{\"contact_owner_user_id\":1130000027135288,\"contact_owner_user_type\":\"passport_user\"," +
                "\"contact_id\":60,\"list_id\":1,\"revision\":\"\",\"tag_ids\":[],\"emails\":[{\"id\":56," +
                "\"value\":\"ahmet.kilic@xyx.com\",\"last_usage\":1578614400000,\"tags\":[]}]," +
                "\"vcard\":{\"emails\":[{\"email\":\"ahmet.kilic@xyx.com\"}],\"names\":[{\"middle\":\"\"," +
                "\"last\":\"KILIC\",\"first\":\"Ahmet\"}],\"directory_entries\":[{\"type\":[\"u\"]," +
                "\"entry_id\":1130000042276442}]}},\"ahmetdursun.k@xyy" +
                ".com\":{\"contact_owner_user_id\":1130000027135288,\"contact_owner_user_type\":\"passport_user\"," +
                "\"contact_id\":58,\"list_id\":1,\"revision\":\"\",\"tag_ids\":[],\"emails\":[{\"id\":54," +
                "\"value\":\"ahmetdursun.k@xyy.com\",\"last_usage\":1575244800000,\"tags\":[]}]," +
                "\"vcard\":{\"emails\":[{\"email\":\"ahmetdursun.k@xyy.com\"}]}},\"furkan.tiknas@xyx" +
                ".com\":{\"contact_owner_user_id\":1130000027135288,\"contact_owner_user_type\":\"passport_user\"," +
                "\"contact_id\":26,\"list_id\":1,\"revision\":\"\",\"tag_ids\":[],\"emails\":[{\"id\":24," +
                "\"value\":\"furkan.tiknas@xyx.com\",\"last_usage\":1578614400000,\"tags\":[]}]," +
                "\"vcard\":{\"emails\":[{\"email\":\"furkan.tiknas@xyx.com\"}]," +
                "\"names\":[{\"last\":\"TIKNAS\",\"first\":\"Furkan\"}],\"directory_entries\":[{\"type\":[\"u\"]," +
                "\"entry_id\":1130000029574511}],\"events\":[{\"month\":1,\"year\":2000,\"type\":[\"birthday\"]," +
                "\"day\":1}]}},\"hasan.sanlier@xyz.com\":{\"contact_owner_user_id\":1130000027135288," +
                "\"contact_owner_user_type\":\"passport_user\",\"contact_id\":3,\"list_id\":1,\"revision\":\"\"," +
                "\"tag_ids\":[],\"emails\":[{\"id\":3,\"value\":\"hasan.sanlier@xyz.com\"," +
                "\"last_usage\":1578268800000,\"tags\":[]}],\"vcard\":{\"emails\":[{\"email\":\"hasan" +
                ".sanlier@xyz.com\"}]}},\"yavuzgkcl@xyy" +
                ".com\":{\"contact_owner_user_id\":1130000027135288,\"contact_owner_user_type\":\"passport_user\"," +
                "\"contact_id\":24,\"list_id\":1,\"revision\":\"\",\"tag_ids\":[],\"emails\":[{\"id\":22," +
                "\"value\":\"yavuzgkcl@xyy.com\",\"last_usage\":1564444800000,\"tags\":[]}]," +
                "\"vcard\":{\"emails\":[{\"email\":\"yavuzgkcl@xyy.com\"}],\"names\":[{\"first\":\"yavuzgkcl\"}]}}}");
        final List<Contact> correct = Arrays.asList(
                new Contact(new Email("ahmet.kilic@xyx.com"), "Ahmet KILIC"),
                new Contact(new Email("furkan.tiknas@xyx.com"), "Furkan TIKNAS"),
                new Contact(new Email("satis@xyz.com"), "Analitik Çevre Laboratuvarı"),
                new Contact(new Email("yavuzgkcl@xyy.com"), "yavuzgkcl"),
                new Contact(new Email("yavuz.gokcel@xyx.com"), "Yavuz GÖKÇEL"),
                new Contact(new Email("raporlama@xyz.com"), "raporlama"));
        assertThat(correct.size()).isEqualTo(contacts.size());
        assertThat(correct).containsAll(contacts);
    }
}
