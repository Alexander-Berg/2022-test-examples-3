package ru.yandex.passport;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.json.dom.JsonList;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class ContactsTest extends TestBase {
    private static final String BLACKBOX_BASE_URI =
        "/blackbox/?format=json&method=userinfo&userip=127.0.0.1&dbfields=" +
            "userinfo.firstname.uid,userinfo.lastname.uid&emails=getdefault&getphones=bound&phone_attributes=101&sid=2";
    @Test
    public void testCrudContact() throws Exception {
        try (AddressProxyCluster cluster = new AddressProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String uid = "227356512";
            HttpPost create = new HttpPost(
                cluster.proxy().host().toString() + "/contact/create?user_id=227356512&user_type=uid");
            create.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            create.setEntity(
                new StringEntity(
                    "{\n" +
                        "  \"first_name\": \"Ivan\",\n" +
                        "  \"last_name\": \"Dudinov\",\n" +
                        "  \"email\": \"ivan.dudinov@yandex.ru\",\n" +
                        "  \"phone_number\": \"+7 926 111 11 11\"\n" +
                        "}\n"));

            String expContact =
                "\"id\":\"<any value>\"," +
                    "\"owner_service\":\"passport\",\"first_name\":\"Ivan\",\"second_name\":null," +
                    "\"last_name\":\"Dudinov\",\"email\":\"ivan.dudinov@yandex.ru\"," +
                    "\"phone_number\":\"+79261111111\"";
            String expByBb =
                "\"id\":\"<any value>\"," +
                    "\"owner_service\":\"passport\",\"first_name\":\"Ivan\",\"second_name\":null," +
                    "\"last_name\":\"Dudinov\",\"email\":\"ivan.dudinov@yandex.ru\",\"phone_number\":\"+79261111232\"";


            String cid = null;
            try (CloseableHttpResponse response = client.execute(create)) {
                String responseStr =
                    CharsetUtils.toString(response.getEntity());

                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                cid = TypesafeValueContentHandler.parse(responseStr).asMap().getString("id");
                YandexAssert.check(new JsonChecker("{\"status\":\"ok\"," + expContact + "}"), responseStr);
            }


            cluster.blackbox().add(
                BLACKBOX_BASE_URI + "&uid=" + uid,
                blackboxResponse(uid, "Ivan", "Dudinov", "+79261111232", "ivan.dudinov"));
            HttpGet get = new HttpGet(
                cluster.proxy().host().toString() + "/contact/list?user_id=227356512&user_type=uid");

            get.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            HttpAssert.assertJsonResponse(
                client,
                get,
                "{\"contacts\": [{" + expByBb + "},{" + expContact + "}], \"more\":false, \"status\": \"ok\"}");

            get = new HttpGet(
                cluster.proxy().host().toString() + "/contact/get?user_id=227356512&user_type=uid&id=" + cid);

            get.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            HttpAssert.assertJsonResponse(
                client,
                get,
                "{\"status\": \"ok\", " + expContact + "}");

            HttpPost update = new HttpPost(
                cluster.proxy().host().toString()
                    + "/contact/update?user_id=227356512&user_type=uid&id=" + cid);
            update.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            update.setEntity(
                new StringEntity(
                    "{\n" +
                        " \"id\": \"" + cid + "\"," +
                        "  \"first_name\": \"Ivan\",\n" +
                        "  \"last_name\": \"Dudinov\",\n" +
                        "  \"email\": \"ivan.dudinov@yandex.ru\",\n" +
                        "  \"phone_number\": \"+7 926 222 11 11\"\n" +
                        "}\n"));

            HttpAssert.assertJsonResponse(
                client,
                update,
                "{\"status\":\"ok\",\"id\":\"" + cid  +
                    "\",\"owner_service\":\"passport\",\"first_name\":\"Ivan\",\"second_name\":null," +
                    "\"last_name\":\"Dudinov\",\"email\":\"ivan.dudinov@yandex.ru\",\"phone_number\":\"+79262221111\"}");

            expContact =
                "\"id\":\"<any value>\"," +
                    "\"owner_service\":\"passport\",\"first_name\":\"Ivan\",\"second_name\":null," +
                    "\"last_name\":\"Dudinov\",\"email\":\"ivan.dudinov@yandex.ru\"," +
                    "\"phone_number\":\"+79262221111\"";
            HttpAssert.assertJsonResponse(
                client,
                get,
                "{\"status\": \"ok\", " + expContact + "}");

            HttpGet delete = new HttpGet(
                cluster.proxy().host().toString() + "/contact/delete?user_id=227356512&user_type=uid&id=" + cid);

            delete.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PASSP_PASSP);
            HttpAssert.assertJsonResponse(
                client,
                delete,
                "{\"status\": \"ok\"}");


            HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND,
                client,
                get);
        }
    }

    @Test
    public void testHideContact() throws Exception {
        try (AddressProxyCluster cluster = new AddressProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            String uid = "227356512";
            cluster.blackbox().add(
                BLACKBOX_BASE_URI + "&uid=" + uid,
                blackboxResponse(uid, "Ivan", "Dudinov", "+79261111232", "ivan.dudinov"));


            HttpGet list = new HttpGet(
                cluster.proxy().host().toString() + "/contact/list?user_id=227356512&user_type=uid");
            list.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PAY_PASSP);

            String bbId = "756964-227356512-70617373706f7274-blackbox-1";
            try (CloseableHttpResponse response = client.execute(list)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr = CharsetUtils.toString(response.getEntity());
                JsonList addreses = TypesafeValueContentHandler.parse(responseStr).asMap().getList("contacts");
                Assert.assertEquals(1, addreses.size());
                Assert.assertEquals(addreses.get(0).asMap().getString("id"), bbId);
            }

            HttpGet delete = new HttpGet(
                cluster.proxy().host().toString() + "/contact/delete?user_id=227356512&user_type=uid&id=" + bbId);
            delete.addHeader(YandexHeaders.X_YA_SERVICE_TICKET, AddressProxyCluster.TICKET_PAY_PASSP);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, delete);

            try (CloseableHttpResponse response = client.execute(list)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                String responseStr = CharsetUtils.toString(response.getEntity());
                JsonList addreses = TypesafeValueContentHandler.parse(responseStr).asMap().getList("contacts");
                Assert.assertEquals(0, addreses.size());
            }
        }
    }

    public static String blackboxResponse(
        final String uid,
        final String firstName,
        final String lastName,
        final String phoneNum,
        final String login)
    {
        return "{\"users\":[{\"id\":\"" + uid
                   + "\",\"uid\":{\"value\":\"" + uid
                   + "\",\"lite\":false,\"hosted\":false},\"login\":\"" + login
                   + "\",\"have_password\":true,\"have_hint\":true,\"karma\":{"
                   + "\"value\":0},\"karma_status\":{\"value\":6000},\"phones\": [\n" +
                   "        {\n" +
                   "          \"attributes\": {\n" +
                   "            \"101\": \"" + phoneNum + "\"\n" +
                   "          },\n" +
                   "          \"id\": \"256590733\"\n" +
                   "        }\n" +
                   "      ]," +
                   "" +
                   "\"address-list\":[{\"address\":\"" + login + "@yandex.ru\",\"validated\":true,\"default\":true," +
                   "\"rpop\":false,\"silent\":false,\"unsafe\":false,\"native\":true,\"born-date\":\"2009-02-17 " +
                   "16:47:59\"}],"
                   + "\"dbfields\":{"
                   + "\"userinfo.firstname.uid\":\"" + firstName
                   + "\",\"userinfo.lastname.uid\":\"" + lastName + "\"}}]}";
    }
}
