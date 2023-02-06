package ru.yandex.ace.ventura;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.YandexAssert;

public class AceVenturaSuggestTest extends AceVenturaTestBase {
    @Test
    public void testMailSearchFallback() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            AceVenturaPrefix prefix =
                new AceVenturaPrefix(0L, UserType.PASSPORT_USER);
            cluster.addStatus(prefix);

            String uriBase =
                cluster.proxy().host().toString()
                    + "/v1/suggest?user_id=0&user_type=passport_user"
                    + "&shared=exclude&limit=10&debug";

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=vasya", "{\"contacts\": [], \"tags\":[]}");

            cluster.mailSearch().add(
                "/api/async/mail/suggest/contact?&request=vasya&timeout=250&uid=0&aceventura=1",
                "{\"contacts\":[{\"email\": \"vasya@gmail.com\"}]}");

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=vasya", "{\"contacts\": [" +
                    "{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\"," +
                    "\"contact_id\":-1,\"list_id\":-1,\"revision\":\"0\",\"tag_ids\":[],\"emails\":[{\"id\":-1," +
                    "\"value\":\"vasya@gmail.com\",\"last_usage\":0,\"tags\":[]}]," +
                    "\"vcard\":{\"emails\":[{\"email\":\"vasya@gmail.com\"}]}}], \"tags\":[]}");
        }
    }

    @Test
    public void testPopularDomains() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            AceVenturaPrefix prefix =
                new AceVenturaPrefix(0L, UserType.PASSPORT_USER);
            cluster.addStatus(prefix);

            String uriBase =
                cluster.proxy().host().toString()
                    + "/v1/suggest?user_id=0&user_type=passport_user"
                    + "&shared=exclude&limit=10&debug";
            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=vasya@",
                "{\"contacts\":[{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\"," +
                    "\"contact_id\":-1,\"list_id\":-1,\"revision\":\"0\",\"tag_ids\":[],\"emails\":[{\"id\":-1," +
                    "\"value\":\"vasya@yandex.ru\",\"last_usage\":0,\"tags\":[]}],\"vcard\":{\"emails\":[" +
                    "{\"email\":\"vasya@yandex.ru\"}]}}," +
                    "{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\",\"contact_id\":-1," +
                    "\"list_id\":-1,\"revision\":\"0\",\"tag_ids\":[],\"emails\":[{\"id\":-1,\"value\":\"vasya@yahoo.com\"," +
                    "\"last_usage\":0,\"tags\":[]}],\"vcard\":{\"emails\":[{\"email\":\"vasya@yahoo.com\"}]}}," +
                    "{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\",\"contact_id\":-1," +
                    "\"list_id\":-1,\"revision\":\"0\",\"tag_ids\":[],\"emails\":[{\"id\":-1,\"value\":\"vasya@gmail" +
                    ".com\",\"last_usage\":0,\"tags\":[]}],\"vcard\":{\"emails\":[{\"email\":\"vasya@gmail.com\"}]}}," +
                    "{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\",\"contact_id\":-1," +
                    "\"list_id\":-1,\"revision\":\"0\",\"tag_ids\":[],\"emails\":[{\"id\":-1,\"value\":\"vasya@mail" +
                    ".ru\",\"last_usage\":0,\"tags\":[]}],\"vcard\":{\"emails\":[{\"email\":\"vasya@mail.ru\"}]}}," +
                    "{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\",\"contact_id\":-1," +
                    "\"list_id\":-1,\"revision\":\"0\",\"tag_ids\":[],\"emails\":[{\"id\":-1," +
                    "\"value\":\"vasya@rambler.ru\",\"last_usage\":0,\"tags\":[]}]," +
                    "\"vcard\":{\"emails\":[{\"email\":\"vasya@rambler.ru\"}]}},{\"contact_owner_user_id\":0," +
                    "\"contact_owner_user_type\":\"passport_user\",\"contact_id\":-1,\"list_id\":-1," +
                    "\"revision\":\"0\",\"tag_ids\":[],\"emails\":[{\"id\":-1,\"value\":\"vasya@icloud.com\"," +
                    "\"last_usage\":0,\"tags\":[]}],\"vcard\":{\"emails\":[{\"email\":\"vasya@icloud.com\"}]}}," +
                    "{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\",\"contact_id\":-1," +
                    "\"list_id\":-1,\"revision\":\"0\",\"tag_ids\":[],\"emails\":[{\"id\":-1,\"value\":\"vasya@qip" +
                    ".ru\",\"last_usage\":0,\"tags\":[]}],\"vcard\":{\"emails\":[{\"email\":\"vasya@qip.ru\"}]}}," +
                    "{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\",\"contact_id\":-1," +
                    "\"list_id\":-1,\"revision\":\"0\",\"tag_ids\":[],\"emails\":[{\"id\":-1,\"value\":\"vasya@bk" +
                    ".ru\",\"last_usage\":0,\"tags\":[]}],\"vcard\":{\"emails\":[{\"email\":\"vasya@bk.ru\"}]}}," +
                    "{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\",\"contact_id\":-1," +
                    "\"list_id\":-1,\"revision\":\"0\",\"tag_ids\":[],\"emails\":[{\"id\":-1,\"value\":\"vasya@inbox" +
                    ".ru\",\"last_usage\":0,\"tags\":[]}],\"vcard\":{\"emails\":[{\"email\":\"vasya@inbox.ru\"}]}}]," +
                    "\"tags\":[]}");

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=vasya@ya",
                "{\"contacts\":[{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\"," +
                    "\"contact_id\":-1,\"list_id\":-1,\"revision\":\"0\",\"tag_ids\":[],\"emails\":[{\"id\":-1," +
                    "\"value\":\"vasya@yandex.ru\",\"last_usage\":0,\"tags\":[]}],\"vcard\":{\"emails\":[" +
                    "{\"email\":\"vasya@yandex.ru\"}]}}," +
                    "{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\",\"contact_id\":-1," +
                    "\"list_id\":-1,\"revision\":\"0\",\"tag_ids\":[],\"emails\":[{\"id\":-1,\"value\":\"vasya@yahoo.com\"," +
                    "\"last_usage\":0,\"tags\":[]}],\"vcard\":{\"emails\":[{\"email\":\"vasya@yahoo.com\"}]}}], \"tags\":[]}");
        }
    }

    @Test
    public void testNamesAliasSuggest() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            AceVenturaPrefix prefix =
                new AceVenturaPrefix(0L, UserType.PASSPORT_USER);
            cluster.addStatus(prefix);
            String contact15 = addContact(
                cluster,
                prefix,
                15,
                "Александр\tБелобородов",
                "александр\nшура\nсаша\nолександр\nсаня\nсашок",
                "72\n75",
                null);

            addEmail(
                cluster,
                prefix,
                51,
                contact15,
                "ivan@yandex.ru",
                "ivan",
                "yandex.ru",
                "71\n73\n74");

            String oneEmail =
                "{\"contacts\":[{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\","
                    + "\"emails\":[{\"id\": 51,\"tags\":[72,75],\"last_usage\":0,\"value\":\"ivan@yandex.ru\"}"
                    + "],\"contact_id\":15,\"list_id\":1,\"revision\":\"\",\"tag_ids\":[72,75],"
                    + "\"vcard\":{\"emails\":[{\"email\":\"ivan@yandex.ru\"}]}}],\"tags\":[]}";

            String uriBase =
                cluster.proxy().host().toString()
                    + "/v1/suggest?user_id=0&user_type=passport_user"
                    + "&shared=exclude&limit=10";

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=саш",
                oneEmail);
        }
    }

    @Test
    public void testNamesSuggest() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            AceVenturaPrefix prefix =
                new AceVenturaPrefix(0L, UserType.PASSPORT_USER);
            cluster.addStatus(prefix);
            String contact15 = addContact(
                cluster,
                prefix,
                15,
                "Вася\tСедой\nСедых\tВасилий\n",
                "вася\nвасилий\nвасилевс",
                "72\n75",
                null);
            addEmail(
                cluster,
                prefix,
                51,
                contact15,
                "ivan.dudinov@yandex.ru",
                "ivan.dudinov",
                "yandex.ru",
                "71\n73\n74");

            String oneEmail =
                "{\"contacts\":[{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\","
                    + "\"emails\":[{\"id\": 51,\"tags\":[72,75],\"last_usage\":0,\"value\":\"ivan.dudinov@yandex.ru\"}"
                    + "],\"contact_id\":15,\"list_id\":1,\"revision\":\"\",\"tag_ids\":[72,75],"
                    + "\"vcard\":{\"emails\":[{\"email\":\"ivan.dudinov@yandex.ru\"}]}}],\"tags\":[]}";

            String uriBase =
                cluster.proxy().host().toString()
                    + "/v1/suggest?user_id=0&user_type=passport_user"
                    + "&shared=exclude&limit=10";

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=ctljq+dfc",
                oneEmail);

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=Седой+Васил",
                oneEmail);
        }
    }

    @Test
    public void testSuggestParsingQueries() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String uriBase =
                cluster.proxy().host().toString()
                    + "/v1/suggest?user_id=0&user_type=passport_user"
                    + "&shared=exclude&limit=10";

            AceVenturaPrefix prefix =
                new AceVenturaPrefix(0L, UserType.PASSPORT_USER);
            cluster.addStatus(prefix);
            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(uriBase + "&query=%20"));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(uriBase
                    + "&query=i"));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(uriBase
                    + "&query=%3F"));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                client,
                new HttpGet(uriBase
                    + "&query=list65g%40list.ru%0A%0A%D0%92%D0%B0%D0%BC%20%D0%BF%D0%B5%D1%80%D0%B5%D0%B7%" +
                    "D0%B2%D0%BE%D0%BD%D0%B8%D1%82%D1%8C%3F%0Alist65g"));
        }
    }

    @Test
    public void testSuggest() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            try (CloseableHttpResponse response =
                    client.execute(
                        new HttpGet(cluster.proxy().host() + "/ping")))
            {
                CharsetUtils.toString(response.getEntity());
            }

            AceVenturaPrefix prefix =
                new AceVenturaPrefix(0L, UserType.PASSPORT_USER);
            cluster.addStatus(prefix);
            addContact(cluster, prefix, 14, "Vasya\\tPupkin");
            String contact15 = addContact(
                cluster,
                prefix,
                15,
                "Ivan\tDudinov\nВаня\tvonidu",
                "Ivan\nВаня\nИван",
                "72\n75",
                null);
            addEmail(
                cluster,
                prefix,
                51,
                contact15,
                "ivan.dudinov@yandex.ru",
                "ivan.dudinov",
                "yandex.ru",
                "71\n73\n74");
            addEmail(
                cluster,
                prefix,
                52,
                contact15,
                "dudinov.ivan@gmail.com",
                "dudinov.ivan",
                "gmail.com",
                "73\n74\n75");
            String contact16 = addContact(
                cluster,
                prefix,
                16,
                "Vasya\nВасилий\nПетя",
                "Vasya\nВасилий\nВася\nПетр\nПетруня",
                "7\n73",
                "926000005");
            addEmail(
                cluster,
                prefix,
                53,
                contact16,
                "vasya@vasya.com",
                "vasya",
                "vasya.com",
                "7\n3\n2");

            String uriBase =
                cluster.proxy().host().toString()
                    + "/v1/suggest?user_id=0&user_type=passport_user"
                    + "&shared=exclude&limit=10";

            String simpleResponse =
                "{\"contacts\":[{\"contact_owner_user_id\":0,"
                    + "\"contact_owner_user_type\":\"passport_user\","
                + "\"emails\": [{\"id\": 51,\"tags\": [72,75],\"last_usage\": 0,\"value\": \"ivan.dudinov@yandex.ru\"},"
                + "{\"id\": 52,\"tags\": [72,75],\"last_usage\": 0,\"value\": \"dudinov.ivan@gmail.com\"}],"
                + "\"contact_id\":15,\"list_id\":1,"
                + "\"revision\":\"\",\"tag_ids\":[72,75],"
                + "\"vcard\":{\"emails\":[{\"email\":\"ivan"
                + ".dudinov@yandex.ru\"},{\"email\":\"dudinov"
                + ".ivan@gmail.com\"}]}}],\"tags\":[]}";

            String withYandexEmail =
                "{\"contacts\":[{\"contact_owner_user_id\":0,"
                    + "\"contact_owner_user_type\":\"passport_user\","
                    + "\"emails\": [{\"id\": 51,\"tags\": [72,75],\n"
                    + "\"last_usage\": 0,"
                    + "\"value\": \"ivan.dudinov@yandex.ru\"}],"
                    + "\"contact_id\":15,\"list_id\":1,"
                    + "\"revision\":\"\",\"tag_ids\":[72,75],"
                    + "\"vcard\":{\"emails\":["
                    + "{\"email\":\"ivan.dudinov@yandex.ru\"}]}}],\"tags\":[]}";
            // testEmailSuggest

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=ivan",
                simpleResponse);

            HttpAssert.assertJsonResponse(
                    client,
                    uriBase + "&query=ivan+dudi",
                    simpleResponse);

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=ivan.",
                simpleResponse);

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=ivan.du",
                simpleResponse);

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=ivan.dudinov@",
                withYandexEmail);

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=ivan.dudinov@ya",
                withYandexEmail);

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=dudin",
                simpleResponse);

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=dudinov",
                simpleResponse);

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=dudinov&to=two@yandex-team.ru,one@yandex-team.ru,&cc=,&&bcc=,,,,",
                simpleResponse);

            addTag(cluster, prefix, "73", "ivan");

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=ivan",
                "{\"contacts\":[{\"contact_owner_user_id\":0,"
                    + "\"contact_owner_user_type\":\"passport_user\","
                    + "\"contact_id\":15,\"list_id\":1,"
                    + "\"revision\":\"\",\"tag_ids\":[72,75],"
                    + "\"emails\": [{\"id\": 51,\"tags\": [72,75],"
                    + "\"last_usage\": 0,"
                    + "\"value\":\"ivan.dudinov@yandex.ru\"},\n"
                    + "{\"id\": 52,\"tags\": [72,75],"
                    + "\"last_usage\": 0,"
                    + "\"value\": \"dudinov.ivan@gmail.com\"}],"
                    + "\"vcard\":{\"emails\":[{\"email\":\"ivan"
                    + ".dudinov@yandex.ru\"},{\"email\":\"dudinov"
                    + ".ivan@gmail.com\"}]}}],\"tags\":["
                    + "{\"contacts\": [{\"contact_id\": 15,"
                    + "\"contact_owner_user_id\": 0,\n"
                    + "\"contact_owner_user_type\":\"passport_user\","
                    + "\"list_id\": 1,\"emails\": ["
                    + "{\"id\": 51,\"last_usage\":0"
                    + ",\"tags\": [71,73,74],\"value\":"
                    + "\"ivan.dudinov@yandex.ru\"},"
                    + "{\"id\": 52,\"last_usage\":0"
                    + ",\"tags\": [73,74,75],\"value\":"
                    + "\"dudinov.ivan@gmail.com\"}"
                    + "],"
                    + "\"revision\": \"\",\"tag_ids\": [71,73,74],"
                    + "\"vcard\": {\"emails\":[{\"email\": \""
                    + "ivan.dudinov@yandex.ru\"},{\"email\": \""
                    + "dudinov.ivan@gmail.com\"}]}}],\n"
                    + "\"name\": \"ivan\",\n"
                    + "\"tag_id\": 73\n}]}");

        }
    }

    @Test
    public void testSuggestTags() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            AceVenturaPrefix prefix =
                new AceVenturaPrefix(0L, UserType.PASSPORT_USER);
            cluster.addStatus(prefix);

            addTag(cluster, prefix, "73", "ivan");
            addTag(cluster, prefix, "74", "иван василич");
            addTag(cluster, prefix, "75", "Ivan");

            String contact1 = addContact(
                cluster,
                prefix,
                1,
                "Имя",
                "Имя",
                "73\n75",
                null);
            addEmail(
                cluster,
                prefix,
                11,
                contact1,
                "email@email.com",
                "email",
                "email.com",
                "73\n75");

            String contact2 = addContact(
                cluster,
                prefix,
                2,
                "Имя",
                "Имя",
                "74",
                null);
            addEmail(
                cluster,
                prefix,
                22,
                contact2,
                "email@email.com",
                "email",
                "email.com",
                "74");
            String uriBase =
                cluster.proxy().host().toString()
                    + "/v1/suggest?user_id=0&user_type=passport_user"
                    + "&shared=exclude&limit=10";

            String respTag75 =
                "{\"tag_id\":75,\"name\":\"Ivan\"," +
                    "\"contacts\":[{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\"," +
                    "\"contact_id\":1,\"list_id\":1,\"revision\":\"\",\"tag_ids\":[73,75],\"emails\":[{\"id\":11," +
                    "\"value\":\"email@email.com\",\"last_usage\":0,\"tags\":[73,75]}]," +
                    "\"vcard\":{\"emails\":[{\"email\":\"email@email.com\"}]}}]}";
            String respTag74 =
                "{\"tag_id\":74,\"name\":\"иван " +
                    "василич\",\"contacts\":[{\"contact_owner_user_id\":0," +
                    "\"contact_owner_user_type\":\"passport_user\",\"contact_id\":2,\"list_id\":1,\"revision\":\"\"," +
                    "\"tag_ids\":[74],\"emails\":[{\"id\":22,\"value\":\"email@email.com\",\"last_usage\":0," +
                    "\"tags\":[74]}],\"vcard\":{\"emails\":[{\"email\":\"email@email.com\"}]}}]}";
            String respTag73 =
                "{\"tag_id\":73,\"name\":\"ivan\"," +
                    "\"contacts\":[{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\"," +
                    "\"contact_id\":1,\"list_id\":1,\"revision\":\"\",\"tag_ids\":[73,75],\"emails\":[{\"id\":11," +
                    "\"value\":\"email@email.com\",\"last_usage\":0,\"tags\":[73,75]}]," +
                    "\"vcard\":{\"emails\":[{\"email\":\"email@email.com\"}]}}]}";
            String fullResponse = "{\"contacts\":[],\"tags\":[" + respTag73 + ',' + respTag75 + ',' + respTag74 + "]}";

            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=ivan&wait-translit=true",
                fullResponse);

            // without translit
            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=i",
                "{\"contacts\":[],\"tags\":[" + respTag73 + ',' + respTag75 + "]}");

            //query=иван ва
            HttpAssert.assertJsonResponse(
                client,
                uriBase + "&query=%D0%B8%D0%B2%D0%B0%D0%BD%20%D0%B2%D0%B0",
                "{\"contacts\":[],\"tags\":[" + respTag74 + "]}");
        }
    }

    @Test
    public void testSuggestPopular() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            AceVenturaPrefix prefix =
                new AceVenturaPrefix(0L, UserType.PASSPORT_USER);
            cluster.addStatus(prefix);
            String contact15 = addContact(
                cluster,
                prefix,
                15,
                "Ivan\tDudinov\nВаня\tvonidu",
                "Ivan\nВаня\nИван",
                "72\n75",
                null);
            addEmail(
                cluster,
                prefix,
                51,
                contact15,
                "ivan.dudinov@yandex.ru",
                "ivan.dudinov",
                "yandex.ru",
                "71\n73\n74");

            String baseUri =
                cluster.proxy().host().toString()
                    + "/v1/suggest?user_id=0&user_type=passport_user"
                    + "&shared=exclude&limit=10";

            String expected =
                "{\"contacts\": [{"
                    + "\"contact_id\": 15,\n"
                    + "\"contact_owner_user_id\": 0,\n"
                    + "\"contact_owner_user_type\":\"passport_user\",\n"
                    + "\"emails\": [{\"id\": 51,\"last_usage\": 0,"
                    + "\"tags\": [72,75],\n"
                    + "\"value\": \"ivan.dudinov@yandex.ru\"\n}],\n"
                    + "\"list_id\": 1,\n"
                    + "\"revision\": \"\",\n"
                    + "\"tag_ids\": [72,75],\n"
                    + "\"vcard\": {\n"
                    + "\"emails\": [{\n"
                    + "\"email\": \"ivan.dudinov@yandex.ru\"\n"
                    + "}]}}],\"tags\": []}";

            HttpAssert.assertJsonResponse(
                client,
                baseUri,
                expected);

            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&query=%20",
                expected);

            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&query=%20",
                expected);
        }
    }

    @Test
    public void testSuggestOptions() throws Exception {
        String expectedPhones = "{\n"
            + "  \"contacts\": [\n"
            + "    {\n"
            + "      \"contact_owner_user_id\": 279740642,\n"
            + "      \"contact_owner_user_type\": \"passport_user\",\n"
            + "      \"contact_id\": 580,\n"
            + "      \"list_id\": 1,\n"
            + "      \"revision\": \"\",\n"
            + "      \"tag_ids\": [],\n"
            + "\"emails\":["
            + "{\"id\": 1580, \"last_usage\": 0,\"tags\": [],"
            + "\"value\": \"vasya@yandex.ru\"}],"
            + "      \"vcard\": {\n"
            + "        \"emails\": [\n"
            + "          {\n"
            + "            \"email\": \"vasya@yandex.ru\"\n"
            + "          }\n"
            + "        ]\n"
            + "      }\n"
            + "    },\n"
            + "    {\n"
            + "      \"contact_owner_user_id\": 279740642,\n"
            + "      \"contact_owner_user_type\": \"passport_user\",\n"
            + "      \"contact_id\": 581,\n"
            + "      \"list_id\": 1,\n"
            + "      \"revision\": \"\",\n"
            + "      \"tag_ids\": [],\n"
            + "\"emails\":["
            + "{\"id\": 1581,\"tags\":[],\"last_usage\": 0, "
            + "\"value\": \"phone@yandex.ru\"}],"
            + "      \"vcard\": {\n"
            + "        \"emails\": [\n"
            + "          {\n"
            + "            \"email\": \"phone@yandex.ru\"\n"
            + "          }\n"
            + "        ]\n"
            + "      }\n"
            + "    },\n"
            + "    {\n"
            + "      \"contact_owner_user_id\": 279740642,\n"
            + "      \"contact_owner_user_type\": \"passport_user\",\n"
            + "      \"contact_id\": 582,\n"
            + "      \"list_id\": 1,\n"
            + "      \"revision\": \"\",\n"
            + "      \"tag_ids\": [],\"emails\": ["
            + "{\"id\": 1582,\"tags\": [],\"last_usage\": 0,"
            + "\"value\": \"phone2@yandex.ru\"}],"
            + "      \"vcard\": {\n"
            + "        \"emails\": [\n"
            + "          {\n"
            + "            \"email\": \"phone2@yandex.ru\"\n"
            + "          }\n"
            + "        ]\n"
            + "      }\n"
            + "    }\n"
            + "  ],\n"
            + "  \"tags\": []\n"
            + "}";

        String expectedExclude = "{\n"
            + "  \"contacts\": [\n"
            + "    {\n"
            + "      \"contact_owner_user_id\": 279740642,\n"
            + "      \"contact_owner_user_type\": \"passport_user\",\n"
            + "      \"contact_id\": 580,\n"
            + "      \"list_id\": 1,\n"
            + "      \"revision\": \"\",\n"
            + "      \"tag_ids\": [],\n"
            + "\"emails\":["
            + "{\"id\": 1580,\"tags\": [],\"last_usage\": 0,"
            + "\"value\": \"vasya@yandex.ru\"}],"
            + "      \"vcard\": {\n"
            + "        \"emails\": [\n"
            + "          {\n"
            + "            \"email\": \"vasya@yandex.ru\"\n"
            + "          }\n"
            + "        ]\n"
            + "      }\n"
            + "    },\n"
            + "    {\n"
            + "      \"contact_owner_user_id\": 279740642,\n"
            + "      \"contact_owner_user_type\": \"passport_user\",\n"
            + "      \"contact_id\": 581,\n"
            + "      \"list_id\": 1,\n"
            + "      \"revision\": \"\",\n"
            + "      \"tag_ids\": [],\n"
            + "\"emails\":["
            + "{\"id\": 1581,\"tags\": [],\"last_usage\": 0, "
            + "\"value\": \"phone@yandex.ru\"}],"
            + "      \"vcard\": {\n"
            + "        \"emails\": [\n"
            + "          {\n"
            + "            \"email\": \"phone@yandex.ru\"\n"
            + "          }\n"
            + "        ]\n"
            + "      }\n"
            + "    },"
            + "{\n"
            + "\"contact_id\": 583,\n"
            + "\"contact_owner_user_id\": 279740642,\n"
            + "\"contact_owner_user_type\": "
            + "\"passport_user\",\n"
            + "\"list_id\": 1,\n"
            + "\"revision\": \"\",\n"
            + "\"emails\":["
            + "{\"id\": 1584,\"tags\": [],\"last_usage\": 0,\"value\": \"phone5@yandex.ru\"}],"
            + "\"tag_ids\": [],"
            + "\"vcard\": {\n"
            + "   \"emails\": [{\"email\": \"phone5@yandex.ru\"}]}"
            + "}],"
            + "\"tags\": []}";

        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            AceVenturaPrefix prefix =
                new AceVenturaPrefix(279740642L, UserType.PASSPORT_USER);
            cluster.addStatus(prefix);

            addContact(
                cluster,
                prefix,
                579,
                "There are\nPhone\nNo Emails",
                "",
                "",
                "70001234567\n+13335550101");
            String contact580 = addContact(
                cluster,
                prefix,
                580,
                "Emails\nAnd\nOne Phone",
                "",
                "",
                "70001234567\n+13335550101");
            addEmail(
                cluster,
                prefix,
                1580,
                contact580,
                "vasya@yandex.ru",
                "vasya",
                "yandex.ru",
                "");
            String contact581 = addContact(
                cluster,
                prefix,
                581,
                "Emails\nAnd\nPhones",
                "",
                "",
                "70001234567\n+13335550101");
            addEmail(
                cluster,
                prefix,
                1581,
                contact581,
                "phone@yandex.ru",
                "phone",
                "yandex.ru",
                "");
            String contact582 = addContact(
                cluster,
                prefix,
                582,
                "No\nPhones",
                "",
                "",
                null);
            addEmail(
                cluster,
                prefix,
                1582,
                contact582,
                "phone2@yandex.ru",
                "phone2",
                "yandex.ru",
                "");
            String contact583 = addContact(
                cluster,
                prefix,
                583,
                "Much emails",
                "",
                "",
                "812351235534");
            addEmail(
                cluster,
                prefix,
                1583,
                contact583,
                "phone4@yandex.ru",
                "phone4",
                "yandex.ru",
                "");
            addEmail(
                cluster,
                prefix,
                1584,
                contact583,
                "phone5@yandex.ru",
                "phone5",
                "yandex.ru",
                "");

            HttpAssert.assertJsonResponse(
                client,
                cluster.proxy().host().toString()
                    + "/v1/suggest?user_id=279740642&user_type=passport_user"
                    + "&query=Phon&shared=exclude&limit=3",
                expectedPhones);

            HttpAssert.assertJsonResponse(
                client,
                cluster.proxy().host().toString()
                    + "/v1/suggest?user_id=279740642&user_type=passport_user"
                    + "&query=Phon&shared=exclude&limit=3"
                    + "&to=phone4@yandex.ru&cc=phone2@yandex.ru",
                expectedExclude);

//            post= new HttpPost(
//                cluster.proxy().host().toString()
//                    + "/v1/suggest?user_id=279740642&user_type=passport_user"
//                    + "&query=Phon&shared=exclude&limit=2"
//                    + "&has_telephone_number=true");
//            post.setEntity(new StringEntity("{}"));
//
//            try (CloseableHttpResponse response = client.execute(post)) {
//                String responseStr =
//                    CharsetUtils.toString(response.getEntity());
//                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
//                YandexAssert.check(
//                    new JsonChecker(expectedExclude),
//                    responseStr);
//            }
        }
    }

    @Test
    public void testShareSuggest() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            AceVenturaPrefix prefixClient =
                new AceVenturaPrefix(0L, UserType.PASSPORT_USER);
            AceVenturaPrefix prefixOwner1 =
                new AceVenturaPrefix(1L, UserType.PASSPORT_USER);
            AceVenturaPrefix prefixOwner2 =
                new AceVenturaPrefix(2L, UserType.PASSPORT_USER);
            cluster.addStatus(prefixClient);
            cluster.addStatus(prefixOwner1);
            cluster.addStatus(prefixOwner2);
            String contact15 = addContact(
                cluster,
                prefixClient,
                15,
                "Ivan\tDudinov\nВаня\tvonidu",
                "Ivan\nВаня\nИван",
                "72\n75",
                null);
            addEmail(
                cluster,
                prefixClient,
                51,
                contact15,
                "ivan.dudinov@yandex.ru",
                "ivan.dudinov",
                "yandex.ru",
                "71\n73\n74");

            share(cluster, prefixClient, prefixOwner1, 1);

            String contact16 = addContact(
                cluster,
                prefixOwner1,
                15,
                "Ivan\tPopov\nВаня\tpopov",
                "Ivan\nВаня\nИван",
                "72\n75",
                null);
            addEmail(
                cluster,
                prefixOwner1,
                51,
                contact16,
                "ivan.popov@yandex.ru",
                "ivan.popov",
                "yandex.ru",
                "71\n73\n74");

            share(cluster, prefixClient, prefixOwner2, 1);

            String contact17 = addContact(
                cluster,
                prefixOwner2,
                18,
                "Ivan\tSedov\nВаня\tsedov",
                "Ivan\nВаня\nИван",
                "72\n75",
                null);
            addEmail(
                cluster,
                prefixOwner2,
                58,
                contact17,
                "ivan-sedov@yandex.ru",
                "ivan-sedov",
                "yandex.ru",
                "71\n73\n74");

            String contact18 = addContact(
                cluster,
                prefixOwner2,
                19,
                2,
                "Ivan\tPetrov\nВаня\tpetrov",
                "Ivan\nВаня\nИван",
                "72\n75",
                null);
            addEmail(
                cluster,
                prefixOwner2,
                59,
                contact18,
                "ivan.petrov@yandex.ru",
                "ivan.pertrov",
                "yandex.ru",
                "71\n73\n74");

            String baseUri =
                cluster.proxy().host().toString()
                    + "/v1/suggest?user_id=0&user_type=passport_user"
                    + "&shared=include&limit=10";
            HttpGet get = new HttpGet(baseUri);

            String expected =
                "{\"contacts\":[{\"contact_owner_user_id\":0,"
                    + "\"contact_owner_user_type\":\"passport_user\","
                    + "\"contact_id\":15,\"list_id\":1,\"revision\":\"\","
                    + "\"tag_ids\":[72,75],\"emails\":[{\"id\":51,"
                    + "\"last_usage\": 0,"
                    + "\"value\":\"ivan.dudinov@yandex.ru\",\"tags\":[72,"
                    + "75]}],\"vcard\":{\"emails\":[{\"email\":\"ivan"
                    + ".dudinov@yandex.ru\"}]}},{\"contact_owner_user_id\":1,"
                    + "\"contact_owner_user_type\":\"passport_user\","
                    + "\"contact_id\":15,\"list_id\":1,\"revision\":\"\","
                    + "\"tag_ids\":[72,75],\"emails\":[{\"id\":51,"
                    + "\"last_usage\": 0,"
                    + "\"value\":\"ivan.popov@yandex.ru\",\"tags\":[72,75]}],"
                    + "\"vcard\":{\"emails\":[{\"email\":\"ivan.popov@yandex"
                    + ".ru\"}]}},{\"contact_owner_user_id\":2,"
                    + "\"contact_owner_user_type\":\"passport_user\","
                    + "\"contact_id\":18,\"list_id\":1,\"revision\":\"\","
                    + "\"tag_ids\":[72,75],\"emails\":[{\"id\":58,"
                    + "\"last_usage\": 0,"
                    + "\"value\":\"ivan-sedov@yandex.ru\",\"tags\":[72,75]}],"
                    + "\"vcard\":{\"emails\":[{\"email\":\"ivan-sedov@yandex"
                    + ".ru\"}]}}],\"tags\":[]}";
            try (CloseableHttpResponse response = client.execute(get)) {
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                System.out.println("SuggestResponse " + responseStr);
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    responseStr);
            }

            // check cache
            try (CloseableHttpResponse response = client.execute(get)) {
                String responseStr =
                    CharsetUtils.toString(response.getEntity());
                System.out.println("SuggestResponse " + responseStr);
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
                YandexAssert.check(
                    new JsonChecker(expected),
                    responseStr);
            }
        }
    }

    @Test
    public void testCorpSuggest() throws Exception {
        AceVenturaPrefix prefixClient =
            new AceVenturaPrefix(1120000000040290L, UserType.PASSPORT_USER);
        AceVenturaPrefix prefixOwner =
            new AceVenturaPrefix(2L, UserType.CONNECT_ORGANIZATION);
        try (AceVenturaCluster cluster = new AceVenturaCluster(this)) {
            cluster.addStatus(prefixClient);
            cluster.addStatus(prefixOwner);

            share(cluster, prefixClient, prefixOwner, 1);

            String contact1 =
                addContact(
                    cluster,
                    prefixClient,
                    20,
                    "Вася\tПупкин",
                    "Вася\nВасилий\nВасёныш",
                    "1\n2",
                    "+7100500");
            addEmail(
                cluster,
                prefixClient,
                21,
                contact1,
                "vasya@yandex.ru",
                "vasya",
                "yandex",
                "2\n3");

            String baseUri =
                cluster.proxy().host().toString()
                    + "/v1/suggest?user_id=" + prefixClient.uid()
                    + "&user_type=" + prefixClient.userType().lowName()
                    + "&shared=include&limit=10";
            HttpGet get = new HttpGet(baseUri);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);

            get = new HttpGet(baseUri + "&query=vas");
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);

            addTag(cluster, prefixClient, "73", "ivan");

            get = new HttpGet(baseUri + "&query=i");
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);
        }
    }

    @Test
    public void testSuggestReport() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            AceVenturaPrefix prefix  =
                new AceVenturaPrefix(0L, UserType.PASSPORT_USER);

            String contact15 = addContact(
                cluster,
                prefix,
                15,
                "Ivan\tDudinov\nВаня\tvonidu",
                "Ivan\nВаня\nИван",
                "72\n75",
                null);
            addEmail(
                cluster,
                prefix,
                51,
                contact15,
                "ivan.dudinov@yandex.ru",
                "ivan.dudinov",
                "yandex.ru",
                "71\n73\n74");

            addTag(cluster, prefix, "73", "ivan");

            String uriBase =
                cluster.proxy().host() + "/v1/suggestReport?&user_type="
                + prefix.userType().lowName()
                + "&user_id=" + prefix.uid();

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                new HttpGet(
                    uriBase
                        + "&contact_id=15&request=vasya"
                        + "&title=ivan.dudinov@yandex.ru&ts=101"));

            HttpAssert.assertStatusCode(
                HttpStatus.SC_OK,
                new HttpGet(
                    uriBase
                        + "&tag_id=73&request=vasya&title=vasya&ts=101"));

            Thread.sleep(1000);
            cluster.searchBackend().checkSearch(
                "/search?get=*&prefix=" + prefix.toStringFast() + "&text="
                    + AceVenturaFields.RECORD_TYPE.global() + ':'
                    + AceVenturaRecordType.EMAIL.fieldValue(),
                new JsonChecker("{\"hitsArray\": [\n"
                    + "{\"av_domain\": \"yandex.ru\",\n"
                    + "\"av_domain_nt\": \"yandex.ru\",\n"
                    + "\"av_email\": \"ivan.dudinov@yandex.ru\",\n"
                    + "\"av_email_cid\": \"15\",\"av_email_id\": \"51\",\n"
                    + "\"av_email_type\": \"\",\"av_has_phones\": \"true\",\n"
                    + "\"av_last_requests\": \"vasya\\t1\",\n"
                    + "\"av_last_usage\": \"101\",\n"
                    + "\"av_list_id\": \"1\",\n"
                    + "\"av_login\": \"ivan.dudinov\",\n"
                    + "\"av_names\": \"Ivan\\tDudinov\\nВаня\\tvonidu\",\n"
                    + "\"av_names_alias\": \"Ivan\\nВаня\\nИван\",\n"
                    + "\"av_record_type\": \"email\",\n"
                    + "\"av_revision\": \"178\",\n"
                    + "\"av_tags\": \"71\\n73\\n74\",\n"
                    + "\"av_user_id\": \"0\",\n"
                    + "\"av_user_type\": \"passport_user\",\n"
                    + "\"id\": \"av_email_0_passport_user_51\"\n"
                    + "}],\"hitsCount\": 1}"));

            cluster.searchBackend().checkSearch(
                "/search?get=*&prefix=" + prefix.toStringFast() + "&text="
                    + AceVenturaFields.RECORD_TYPE.field() + ':'
                    + AceVenturaRecordType.TAG.fieldValue(),
                new JsonChecker("{\"hitsArray\":[{\"av_last_requests\": "
                    + "\"vasya\\t1\",\n"
                    + "\"av_last_usage\": \"101\",\n"
                    + "\"av_record_type\": \"tag\",\n"
                    + "\"av_tag_id\": \"73\",\n"
                    + "\"av_tag_name\": \"ivan\",\n"
                    + "\"id\": \"av_tag_0_passport_user_73\""
                    + "}], \"hitsCount\":1}"));

            HttpGet get = new HttpGet(
                uriBase
                    + "&contact_id=15&request=vasya"
                    + "&title=ivan.dudinov@yandex.ru&ts=101");
            get.addHeader(
                HttpHeaders.ACCEPT,
                ContentType.APPLICATION_JSON.getMimeType());
            HttpAssert.assertJsonResponse(client, get, "{}");
        }
    }

    @Test
    public void testMergeAndSort() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            AceVenturaPrefix prefixClient =
                new AceVenturaPrefix(0L, UserType.PASSPORT_USER);
            AceVenturaPrefix prefixOwner1 =
                new AceVenturaPrefix(1L, UserType.PASSPORT_USER);
//            AceVenturaPrefix prefixOwner2 =
//                new AceVenturaPrefix(2L, UserType.PASSPORT_USER);

            cluster.addStatus(prefixClient);
            cluster.addStatus(prefixOwner1);
            String sharedContact1 = addContact(
                cluster,
                prefixOwner1,
                1,
                "Иван\tСедов",
                "Иван",
                "72\n75",
                null);
            addEmail(
                cluster,
                prefixOwner1,
                100,
                sharedContact1,
                "ivan-sedov@yandex.ru",
                "ivan-sedov",
                "yandex.ru",
                "71\n73\n74",
                100);

            String personalContact1 = addContact(
                cluster,
                prefixClient,
                1,
                "Ivan\tSedov\nВаня\tsedov",
                "Ivan\nВаня\nИван",
                "72\n75",
                null);
            addEmail(
                cluster,
                prefixClient,
                200,
                personalContact1,
                "ivan-sedov@yandex.ru",
                "ivan-sedov",
                "yandex.ru",
                "71\n73\n74",
                200);

            share(cluster, prefixClient, prefixOwner1, 1);

            String baseUri =
                cluster.proxy().host().toString()
                    + "/v1/suggest?user_id=0&user_type=passport_user"
                    + "&shared=include&limit=10";
            HttpGet get = new HttpGet(baseUri + "&query=iva");
            HttpAssert.assertJsonResponse(
                client,
                get,
                "{\"contacts\":[{\"contact_owner_user_id\":0," +
                    "\"contact_owner_user_type\":\"passport_user\",\"contact_id\":1,\"list_id\":1," +
                    "\"revision\":\"\",\"tag_ids\":[72,75],\"emails\":[" +
                    "{\"id\":200,\"value\":\"ivan-sedov@yandex.ru\"," +
                    "\"last_usage\":200,\"tags\":[72,75]}]," +
                    "\"vcard\":{\"emails\":[{\"email\":\"ivan-sedov@yandex.ru\"}]}}],\"tags\":[]}");

            addEmail(
                cluster,
                prefixClient,
                201,
                personalContact1,
                "ivan-sedov@google.ru",
                "ivan-sedov",
                "google.ru",
                "71\n73\n74",
                50);

            HttpAssert.assertJsonResponse(
                client,
                get,
                "{\"contacts\":[" +
                    // first
                    "{\"contact_owner_user_id\":0,\"contact_owner_user_type\":\"passport_user\"," +
                    "\"contact_id\":1,\"list_id\":1,\"revision\":\"\",\"tag_ids\":[72,75],\"emails\":[" +
                    "{\"id\":200,\"value\":\"ivan-sedov@yandex.ru\",\"last_usage\":200,\"tags\":[72,75]}," +
                    "{\"id\":201,\"value\":\"ivan-sedov@google.ru\",\"last_usage\":50,\"tags\":[72,75]}]," +
                    "\"vcard\":{\"emails\":[" +
                    "{\"email\":\"ivan-sedov@yandex.ru\"}," +
                    "{\"email\":\"ivan-sedov@google.ru\"}]}}," +
                    // second contact
                    "{\"contact_owner_user_id\":1,\"contact_owner_user_type\":\"passport_user\"," +
                    "\"contact_id\":1,\"list_id\":1,\"revision\":\"\",\"tag_ids\":[72,75],\"emails\":[{\"id\":100," +
                    "\"value\":\"ivan-sedov@yandex.ru\",\"last_usage\":100,\"tags\":[72,75]}]," +
                    "\"vcard\":{\"emails\":[{\"email\":\"ivan-sedov@yandex.ru\"}]}}],\"tags\":[]}");
        }
    }
}
