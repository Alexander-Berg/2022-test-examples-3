package ru.yandex.passport.contact;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.test.util.TestBase;

import static ru.yandex.passport.contact.ContactTestUtils.Contact;
import static ru.yandex.passport.contact.ContactTestUtils.Pair;
import static ru.yandex.passport.contact.ContactTestUtils.User;

public class ContactTest extends TestBase {
    private static final String URI = "/contacts/plus/list/?";

    private static final List<String> CONTACT_NAMES =
        List.of(
            "-___-",
            "0__0",
            "ಠ⌣ಠ",
            "^_-_^",
            ">_>",
            "~_^");

    @Test
    public void testMutuallyConnectedPair() throws Exception {
        try (ContactCluster cluster = new ContactCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            cluster.start();

            User userA = new User("0", "guid-1-a");
            cluster.addUser(userA);

            User userB = new User("2", "guid-1-b");
            cluster.addUser(userB);

            Pair<Contact, Contact> contacts = userA.connectMutually(CONTACT_NAMES.get(0), CONTACT_NAMES.get(1), userB);
            cluster.addContacts(contacts);

            cluster.flush();

            String request = cluster.proxy().host() + URI + "user_id=" + userA.puid();
            HttpAssert.assertJsonResponse(
                client,
                request,
                response(userA.mutualConnections())
            );
        }
    }

    @Test
    public void testMutuallyConnected() throws Exception {
        try (ContactCluster cluster = new ContactCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            cluster.start();

            User userA = new User("0", "guid-a-11", "guid-a-21", "guid-a-31", "guid-a-41");
            cluster.addUser(userA);

            User userB = new User("2", "guid-b-1", "guid-b-2");
            cluster.addUser(userB);

            User userC = new User("81", "guid-c");
            cluster.addUser(userC);

            Pair<Contact, Contact> contactsB =
                userA.connectMutually(CONTACT_NAMES.get(0), CONTACT_NAMES.get(1), userB);
            cluster.addContacts(contactsB);

            Pair<Contact, Contact> contactC =
                userA.connectMutually(CONTACT_NAMES.get(2), CONTACT_NAMES.get(3), userC);
            cluster.addContacts(contactC);

            cluster.flush();

            String request = cluster.proxy().host() + URI + "user_id=" + userA.puid();
            List<Contact> contacts = new ArrayList<>(userA.mutualConnections());
            String validResponseA = response(contacts);
            Collections.reverse(contacts);
            String validResponseB = response(contacts);

            try {
                HttpAssert.assertJsonResponse(client, request, validResponseA);
            } catch (Error e) {
                HttpAssert.assertJsonResponse(client, request, validResponseB);
            }
        }
    }

    @Test
    public void testConnected() throws Exception {
        try (ContactCluster cluster = new ContactCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            cluster.start();

            User userA = new User("0", "guid-14-a", "guid-24-a");
            cluster.addUser(userA);

            User userB = new User("2", "guid-1-b");
            cluster.addUser(userB);

            User userC = new User("81", "guid-1-c");
            cluster.addUser(userC);

            Contact contactB = userA.connect(CONTACT_NAMES.get(0), userB);
            cluster.addContact(contactB);

            Contact contactC = userB.connect(CONTACT_NAMES.get(2), userC);
            cluster.addContact(contactC);

            cluster.flush();

            String request = cluster.proxy().host() + URI + "user_id=" + userA.puid();
            HttpAssert.assertJsonResponse(
                client,
                request,
                response(userA.mutualConnections())
            );
        }
    }

    private static final String CONTACT_CACHE_STAT_FIELD = "contact-cache-hit_ammm";

    @Test
    public void testCacheHits() throws Exception {
        try (ContactCluster cluster = new ContactCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            cluster.start();

            User userA = new User("0", "guid-a-15");
            cluster.addUser(userA);

            User userB = new User("2", "guid-b-15");
            cluster.addUser(userB);

            Pair<Contact, Contact> contacts = userA.connectMutually(CONTACT_NAMES.get(0), CONTACT_NAMES.get(1), userB);
            cluster.addContacts(contacts);

            cluster.flush();

            String request = cluster.proxy().host() + URI + "user_id=" + userA.puid();
            HttpAssert.assertJsonResponse(client, request, response(userA.mutualConnections()));
            HttpAssert.assertStat(CONTACT_CACHE_STAT_FIELD, "0", cluster.proxy().port());

            HttpAssert.assertJsonResponse(client, request, response(userA.mutualConnections()));
            HttpAssert.assertStat(CONTACT_CACHE_STAT_FIELD, "1", cluster.proxy().port());
        }
    }

    public String responseObject(Contact contact) {
        return String.format("{\"guid\":\"%s\", \"contact_name\":\"%s\"}", contact.contactGuid(), contact.name());
    }

    public String response(List<Contact> contacts) {
        return String.format(
            "{\"hitsArray\":[%s], \"hitsCount\": %d}",
            contacts.stream()
                .map(this::responseObject)
                .collect(Collectors.joining(",")),
            contacts.size()
        );
    }
}
