package ru.yandex.ace.ventura;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;

import static ru.yandex.ace.ventura.ContactTestingEnvironment.ContactData;
import static ru.yandex.ace.ventura.ContactTestingEnvironment.EmailData;
import static ru.yandex.ace.ventura.ContactTestingEnvironment.UserData;


public class AceVenturaSharedSuggestTest extends AceVenturaTestBase {

    @Test
    public void testSharedSuggestReport() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {

            int listId = 11;
            long uidTo = 3L;
            long uidOwner = 4L;

            int contactId = 16;
            int emailId = 16;
            String title = "sixteen@yandex.ru";
            String request = "contact16";
            int ts = 101;

            ContactData[] contactData = new ContactData[]{
                    new ContactData(
                            uidOwner,
                            contactId,
                            listId,
                            new String[]{"Sixteen", "sixteen"},
                            new String[]{"Sixteen", "Шестнадцать"},
                            new EmailData[]{new EmailData(emailId, "sixteen")}
                    )};
            UserData ownerData =
                    new UserData(uidOwner, contactData, UserType.PASSPORT_USER);
            UserData toData =
                    new UserData(uidTo, new ContactData[]{}, UserType.PASSPORT_USER);

            ContactTestingEnvironment env = new ContactTestingEnvironment(cluster, new UserData[]{ownerData, toData});

            env.share(uidOwner, uidTo, listId);

            HttpAssert.assertStat(
                    "shared-lists-cache-hit_ammm",
                    Integer.toString(0),
                    cluster.proxy().port());

            HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    new HttpGet(
                            suggestReportRequest(
                                    cluster.proxy().host().toString(),
                                    toData,
                                    contactId,
                                    request,
                                    title,
                                    ts
                            )));
            env.useContact(contactId, emailId, ts);
            Thread.sleep(1000);

            String expectedResponse = env.buildExpectedResponse(uidOwner);

            HttpGet getOwner = new HttpGet(suggestRequest(cluster.proxy().host().toString(), ownerData));

            try (CloseableHttpResponse response = client.execute(getOwner)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertJsonResponse(
                        client,
                        getOwner,
                        expectedResponse
                );
            }

            HttpGet getTo = new HttpGet(suggestRequest(cluster.proxy().host().toString(), toData));

            try (CloseableHttpResponse response = client.execute(getTo)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertJsonResponse(
                        client,
                        getTo,
                        expectedResponse
                );
            }

            HttpAssert.assertStat(
                    "shared-lists-cache-hit_ammm",
                    Integer.toString(3),
                    cluster.proxy().port());
        }
    }


    @Test
    public void testPersonalSuggestReport() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            long userId = 4L;
            int ts = 100;

            EmailData email = new EmailData("name");
            ContactData contact = new ContactData(
                    userId,
                    new String[]{"name"},
                    new String[]{"name"},
                    new EmailData[]{email});

            UserData user = new UserData(userId, new ContactData[]{contact});

            ContactTestingEnvironment env = new ContactTestingEnvironment(cluster, new UserData[]{user});

            HttpAssert.assertStat(
                    "shared-lists-cache-hit_ammm",
                    Integer.toString(0),
                    cluster.proxy().port());

            HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    new HttpGet(suggestReportRequest(
                            cluster.proxy().host().toString(),
                            user,
                            contact.contactId,
                            "name",
                            "name@yandex.ru",
                            ts
                    )));
            env.useContact(contact.contactId, email.id, ts);
            Thread.sleep(1000);

            String expectedResponse = env.buildExpectedResponse(userId);
            HttpGet getOwner = new HttpGet(suggestRequest(cluster.proxy().host().toString(), user));

            try (CloseableHttpResponse response = client.execute(getOwner)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertJsonResponse(
                        client,
                        getOwner,
                        expectedResponse
                );
            }

            HttpAssert.assertStat(
                    "shared-lists-cache-hit_ammm",
                    Integer.toString(2),
                    cluster.proxy().port());
        }
    }


    @Test
    public void testSharedSuggestSorted() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {

            int listId = 11;
            long uidTo = 3L;
            long uidOwner = 4L;

            int contactIdA = 16;
            int emailIdA = 8;
            int tsA = 200;

            int contactIdB = 7;
            int emailIdB = 6;

            int contactIdC = 5;
            int emailIdC = 4;

            ContactData contactDataA =
                    new ContactData(
                            uidOwner,
                            contactIdA,
                            listId,
                            new String[]{"Sixteen", "sixteen"},
                            new String[]{"Sixteen", "Шестнадцать"},
                            new EmailData[]{new EmailData(emailIdA, "sixteen")}
                    );

            ContactData contactDataB =
                    new ContactData(
                            uidOwner,
                            contactIdB,
                            new String[]{"Seven", "seven"},
                            new String[]{"Seven", "Семь"},
                            new EmailData[]{new EmailData(emailIdB, "seven")}
                    );

            ContactData contactDataC =
                    new ContactData(
                            uidTo,
                            contactIdC,
                            new String[]{"Five", "five"},
                            new String[]{"Five", "Пять"},
                            new EmailData[]{new EmailData(emailIdC, "five")}
                    );

            UserData ownerData =
                    new UserData(uidOwner, new ContactData[]{contactDataA, contactDataB});
            UserData toData =
                    new UserData(uidTo, new ContactData[]{contactDataC});

            ContactTestingEnvironment env = new ContactTestingEnvironment(cluster, new UserData[]{ownerData, toData});
            env.share(uidOwner, uidTo, listId);

            HttpAssert.assertStatusCode(
                    HttpStatus.SC_OK,
                    new HttpGet(
                            suggestReportRequest(
                                    cluster.proxy().host().toString(),
                                    toData,
                                    contactIdA,
                                    "sixteen",
                                    "sixteen@yandex.ru",
                                    tsA)));

            env.useContact(contactIdA, emailIdA, tsA);
            Thread.sleep(1000);

            String expectedResponseTo = env.buildExpectedResponse(uidTo);
            String expectedResponseOwner = env.buildExpectedResponse(uidOwner);

            HttpGet getOwner = new HttpGet(suggestRequest(cluster.proxy().host().toString(), ownerData));

            try (CloseableHttpResponse response = client.execute(getOwner)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertJsonResponse(
                        client,
                        getOwner,
                        expectedResponseOwner
                );
            }

            HttpGet getTo = new HttpGet(
                    suggestRequest(cluster.proxy().host().toString(), toData, "last_usage"));

            try (CloseableHttpResponse response = client.execute(getTo)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                HttpAssert.assertJsonResponse(
                        client,
                        getTo,
                        expectedResponseTo
                );
            }
        }
    }

}
