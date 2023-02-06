package ru.yandex.ace.ventura;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;

public class AceVenturaProxyTest extends AceVenturaTestBase {
    @Test
    public void testShareSearch() throws Exception {
        String main = "{\"contact_owner_user_id\":0,"
            + "\"contact_owner_user_type\":\"passport_user\","
            + "\"contact_id\":20,\"list_id\":1,\"revision\":\"178\","
            + "\"tag_ids\":[2,3],\"emails\":[{\"id\":21,\"last_usage\": 0,"
            + "\"value\":\"vasya@yandex.ru\",\"tags\":[2,3]}],"
            + "\"vcard\":{\"emails\":[{\"email\":\"vasya@yandex.ru\"}]}}";

        String shared =
            "{\"contact_owner_user_id\":1,"
                + "\"contact_owner_user_type\":\"connect_organization\","
                + "\"contact_id\":30,\"list_id\":1,\"revision\":\"178\",\"shared\":true,"
                + "\"tag_ids\":[2,3],\"emails\":[{\"id\":31,\"last_usage\": 0,"
                + "\"value\":\"vasya.shared@yandex.ru\",\"tags\":[2,3]}],"
                + "\"vcard\":{\"emails\":"
                + "[{\"email\":\"vasya.shared@yandex.ru\"}]}}";
        String expectedShared =
            "{\"contacts\": [" + main + ',' + shared +  "], \"next-page\": "
                + "false, \"total\": 2}";

        AceVenturaPrefix prefixClient =
            new AceVenturaPrefix(0L, UserType.PASSPORT_USER);
        AceVenturaPrefix prefixOwner =
            new AceVenturaPrefix(1L, UserType.CONNECT_ORGANIZATION);

        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            String baseUri =
                cluster.proxy().host().toString()
                    + "/v1/search?&transform=humannames%2Ctranslit"
                    + "&shared=include&offset=0&limit=10"
                    + "&user_id=0&user_type=passport_user&query=";

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

            String contactShared =
                addContact(
                    cluster,
                    prefixOwner,
                    30,
                    "Вася\tПупкин",
                    "Вася\nВасилий\nВасёныш",
                    "1\n2",
                    "+7100500");

            addEmail(
                cluster,
                prefixOwner,
                31,
                contactShared,
                "vasya.shared@yandex.ru",
                "vasya.shared",
                "yandex",
                "2\n3");

            HttpAssert.assertJsonResponse(
                client,
                baseUri + "vasya",
                expectedShared);
        }
    }

    @Test
    public void testSearchBaseAndTransforms() throws Exception {
        String expectedPure = "{\"contacts\": [{\"contact_owner_user_id\":0,"
            + "\"contact_owner_user_type\":\"passport_user\","
            + "\"revision\":\"178\", \"contact_id\":20,\"list_id\":1,"
            + "\"tag_ids\":[2,3],\"emails\":[{\"id\":21,\"last_usage\": 0,"
            + "\"value\":\"vasya@yandex.ru\",\"tags\":[2,3]}],"
            + "\"vcard\":{\"events\": [{\"day\": 12, \"type\": "
            + "[\"birthday\"], \"year\": 1960, \"month\": 1}],"
            + "\"emails\":[{\"email\":\"vasya@yandex.ru\"}]}}],"
            + "\"next-page\": false,\n"
            + "\"total\": 1}";

        String expectedByName = "{\"contacts\": [{\"contact_owner_user_id\":0,"
            + "\"contact_owner_user_type\":\"passport_user\","
            + "\"contact_id\":20,\"list_id\":1,\"revision\": \"\","
            + "\"tag_ids\":[1,2],\"emails\":[{\"id\":21,\"last_usage\": 0,"
            + "\"value\":\"vasya@yandex.ru\",\"tags\":[2,3]}],"
            + "\"vcard\":{\"events\": [{\"day\": 12, \"type\": "
            + "[\"birthday\"], \"year\": 1960, \"month\": 1}],"
            + "\"emails\":[{\"email\":\"vasya@yandex.ru\"}]}}],"
            + "\"next-page\": false,\n"
            + "\"total\": 1}";

        String empty = "{\"contacts\":[], \"next-page\": false,\"total\": 0}";

        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            AceVenturaPrefix prefix =
                new AceVenturaPrefix(0L, UserType.PASSPORT_USER);

            cluster.addStatus(prefix);

            String contact1 =
                addContact(
                    cluster,
                    prefix,
                    20,
                    "Вася\tПупкин",
                    "Вася\nВасилий\nВасёныш",
                    "1\n2",
                    "+7100500",
                    "{\\\"events\\\": [{\\\"day\\\": 12, \\\"type\\\": "
                        + "[\\\"birthday\\\"], \\\"year\\\": 1960, "
                        + "\\\"month\\\": 1}]}");
            addEmail(
                cluster,
                prefix,
                21,
                contact1,
                "vasya@yandex.ru",
                "vasya",
                "yandex",
                "2\n3");

            String baseUri =
                cluster.proxy().host().toString()
                    + "/v1/search?&transform=humannames%2Ctranslit"
                    + "&shared=include&offset=0&limit=10&debug=true"
                    + "&user_id=0&user_type=passport_user&query=";

            // by full email
            HttpAssert.assertJsonResponse(
                client,
                baseUri + "vasya@yandex.ru",
                expectedPure);
            HttpAssert.assertJsonResponse(
                client,
                baseUri + "vasya@yandex.r",
                expectedPure);
            HttpAssert.assertJsonResponse(
                client,
                baseUri + "vasya@yan",
                expectedPure);
            HttpAssert.assertJsonResponse(
                client,
                baseUri + "vasya@",
                expectedPure);

            baseUri =
                cluster.proxy().host().toString()
                    + "/v1/search?user_id=0&user_type=passport_user"
                    + "&shared=exclude&debug=true";

            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&query=%D0%B2%D0%B0%D1%81",
                expectedByName);

            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&query=dfcz",
                empty);

            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&query=dfcz&transform=translit",
                expectedByName);

            // е ё + alias check
            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&query=%D0%B2%D0%B0%D1%81%D1%91%D0%BD",
                empty);

            HttpAssert.assertJsonResponse(
                client,
                baseUri
                    + "&query=%D0%B2%D0%B0%D1%81%D1%91%D0%BD"
                    + "&transform=humannames",
                expectedByName);

            HttpAssert.assertJsonResponse(
                client,
                baseUri
                    + "&query=%D0%B2%D0%B0%D1%81%D0%B5%D0%BD"
                    + "&transform=humannames",
                expectedByName);

            //test offset and limits
            String contact2 =
                addContact(
                    cluster,
                    prefix,
                    120,
                    "Вася\tПупкин",
                    "Вася\nВасилий\nВасёныш",
                    "1\n2",
                    "+7100500");
            addEmail(
                cluster,
                prefix,
                121,
                contact2,
                "vasya@yandex.ru",
                "vasya",
                "yandex",
                "2\n3");

            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&query=%D0%B2%D0%B0%D1%81&limit=1",
                "{\"contacts\": [{\"contact_owner_user_id\":0,"
                    + "\"contact_owner_user_type\":\"passport_user\","
                    + "\"contact_id\":20,\"list_id\":1,\"revision\":\"\","
                    + "\"tag_ids\":[1,2],\"emails\":[{\"id\":21,"
                    + "\"last_usage\":0,"
                    + "\"value\":\"vasya@yandex.ru\",\"tags\":[2,3]}],"
                    + "\"vcard\":{\"events\": [{\"day\": 12, \"type\": "
                    + "[\"birthday\"], \"year\": 1960, \"month\": 1}],"
                    + "\"emails\":[{\"email\":\"vasya@yandex.ru\"}]}}],"
                    + "\"next-page\": true,\n"
                    + "\"total\": 1}");
        }
    }

    @Test
    public void testDeduplicateSearch() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            AceVenturaPrefix prefix =
                new AceVenturaPrefix(10L, UserType.PASSPORT_USER);
            cluster.addStatus(prefix);

            String contact579 = addContact(
                cluster,
                prefix,
                100,
                "Vasya\nPupkin",
                "",
                "",
                "");
            addEmail(
                cluster,
                prefix,
                101,
                contact579,
                "vasya@yandex.ru",
                "vasya",
                "yandex.ru",
                "");

            AceVenturaPrefix prefixOwner =
                new AceVenturaPrefix(2L, UserType.CONNECT_ORGANIZATION);
            cluster.addStatus(prefixOwner);

            String sharedContact1 = addContact(
                cluster,
                prefixOwner,
                1,
                "Pupin",
                "",
                "",
                "");
            addEmail(
                cluster,
                prefixOwner,
                2,
                sharedContact1,
                "vasya@yandex.ru",
                "vasya",
                "yandex.ru",
                "");
            share(cluster, prefix, prefixOwner, 1);

            String baseUri =
                cluster.proxy().host().toString()
                    + "/v1/search?&transform=humannames%2Ctranslit"
                    + "&shared=include&offset=0&limit=10&debug"
                    + "&user_id=10&user_type=passport_user&query=";

            String expected = "{\"contacts\":[{\"contact_owner_user_id\":10,"
                + "\"contact_owner_user_type\":\"passport_user\",\"revision\":\"\","
                + "\"contact_id\":100,\"list_id\":1,"
                + "\"tag_ids\":[],\"emails\":[{\"id\":101,"
                + "\"value\":\"vasya@yandex.ru\",\"last_usage\":0,"
                + "\"tags\":[]}],\"vcard\":{\"emails\":[{\"email\":"
                + "\"vasya@yandex.ru\"}]}}],\"total\":1,\"next-page\":false}";
            HttpAssert.assertJsonResponse(
                client,
                baseUri + "vasy",
                expected);
        }
    }

    @Test
    public void testSearchRequests() throws Exception {
        String expectedBoth = "{\"contacts\":[{\"contact_owner_user_id\":0,"
            + "\"contact_owner_user_type\":\"passport_user\",\"revision\":\"\","
            + "\"contact_id\":100,\"list_id\":1,\"revision\":\"178\","
            + "\"tag_ids\":[2,3],\"emails\":[{\"id\":11,"
            + "\"value\":\"kreng-pitsha@yandex.ru\",\"last_usage\":0,"
            + "\"tags\":[2,3]}],\"vcard\":{\"emails\":[{\"email\":"
            + "\"kreng-pitsha@yandex.ru\"}]}},{\"contact_owner_user_id\":0,"
            + "\"contact_owner_user_type\":\"passport_user\","
            + "\"contact_id\":101,\"list_id\":1,"
            + "\"revision\":\"178\",\"tag_ids\":[2,3],"
            + "\"emails\":[{\"id\":12,\"value\":\"kreng"
            + ".pitsha@yandex.ru\",\"last_usage\":0,\"tags\":[2,"
            + "3]}],\"vcard\":{\"emails\":[{\"email\":\"kreng.pitsha@yandex.ru"
            + "\"}]}}],\"total\":2,\"next-page\":false}";
        String expectedOneNoEmail =
            "{\"contacts\":[{\"contact_owner_user_id\":0,"
            + "\"contact_owner_user_type\":\"passport_user\",\"revision\":\"\","
            + "\"contact_id\":100,\"list_id\":1,"
            + "\"tag_ids\":[],\"emails\":[{\"id\":11,"
            + "\"value\":\"kreng-pitsha@yandex.ru\","
            + "\"last_usage\":0,\"tags\":[2,3]}],"
            + "\"vcard\":{\"emails\":[{\"email\":\"kreng-pitsha@yandex.ru\"}]}}"
            + ",{\"contact_owner_user_id\":0,"
            + "\"contact_owner_user_type\":\"passport_user\","
            + "\"contact_id\":101,\"list_id\":1,\"revision\":\"\","
            + "\"tag_ids\":[],\"emails\":[],\"vcard\":{}}],\"total\":2,"
            + "\"next-page\":false}";

        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            AceVenturaPrefix prefix =
                new AceVenturaPrefix(0L, UserType.PASSPORT_USER);

            String baseUri =
                cluster.proxy().host().toString()
                    + "/v1/search?&transform=humannames%2Ctranslit"
                    + "&shared=include&offset=0&limit=10&debug"
                    + "&user_id=0&user_type=passport_user&query=";

            cluster.addStatus(prefix);
            String contact1 =
                addContact(cluster, prefix, 100, "Вася");

            addEmail(
                cluster,
                prefix,
                11,
                contact1,
                "kreng-pitsha@yandex.ru",
                "kreng.pitsha",
                "yandex",
                "2\n3");

            String contact2 =
                addContact(cluster, prefix, 101, "Вася");

            HttpAssert.assertJsonResponse(
                client,
                baseUri + "вас",
                expectedOneNoEmail);
            addEmail(
                cluster,
                prefix,
                12,
                contact2,
                "kreng.pitsha@yandex.ru",
                "kreng.pitsha",
                "yandex",
                "2\n3");

            // normalized and search empty name
            HttpAssert.assertJsonResponse(
                client,
                baseUri + "kreng-pitsha",
                expectedBoth);

            // domain
            HttpAssert.assertJsonResponse(
                client,
                baseUri + "yandex",
                expectedBoth);
        }
    }

    @Test
    public void testSearchByEmail() throws Exception {
        try (AceVenturaCluster cluster = new AceVenturaCluster(this);
             CloseableHttpClient client = HttpClients.createDefault())
        {
            AceVenturaPrefix prefix =
                new AceVenturaPrefix(279740642L, UserType.PASSPORT_USER);
            cluster.addStatus(prefix);

            String contact579 = addContact(
                cluster,
                prefix,
                579,
                "There are\nPhone\nNo Emails",
                "",
                "",
                "+70001234567\n+13335550101");
            addEmail(
                cluster,
                prefix,
                1580,
                contact579,
                "vasya@yandex.ru",
                "vasya",
                "yandex.ru",
                "");
            String contact580 = addContact(
                cluster,
                prefix,
                580,
                "Emails\nAnd\nOne Phone",
                "",
                "",
                "+70001234567\n+13335550101");
            addEmail(
                cluster,
                prefix,
                1580,
                contact580,
                "vasya@yandex.ru",
                "vasya",
                "yandex.ru",
                "");

            String baseUri =
                cluster.proxy().host().toString()
                    + "/v1/search_by_email?user_id=" + prefix.uid()
                    + "&user_type=" + prefix.userType().lowName();

            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&email=vasya@yandex.ru",
                "{\"vasya@yandex.ru\":{\"contact_owner_user_id"
                    + "\":279740642,"
                    + "\"contact_owner_user_type\":\"passport_user\","
                    + "\"contact_id\":579,\"list_id\":1,"
                    + "\"revision\":\"\",\"tag_ids\":[],"
                    + "\"emails\":[{\"id\":1580,\"last_usage\": 0,"
                    + "\"value\":\"vasya@yandex.ru\",\"tags\":[]}],"
                    + "\"vcard\":{\"emails\":"
                    + "[{\"email\":\"vasya@yandex.ru\"}]}}}");

            AceVenturaPrefix prefixOwner =
                new AceVenturaPrefix(1L, UserType.CONNECT_ORGANIZATION);
            cluster.addStatus(prefixOwner);

            String sharedContact1 = addContact(
                cluster,
                prefixOwner,
                1,
                "Emails\nAnd\nOne Phone",
                "",
                "",
                "+70001234567\n+13335550101");
            addEmail(
                cluster,
                prefixOwner,
                2,
                sharedContact1,
                "petya@yandex.ru",
                "petya",
                "yandex.ru",
                "");

            share(cluster, prefix, prefixOwner, 1);

            String expected =
                "{\"vasya@yandex.ru\":{\"contact_owner_user_id\":279740642," +
                    "\"contact_owner_user_type\":\"passport_user\"," +
                    "\"contact_id\":579,\"list_id\":1,\"revision\":\"\"," +
                    "\"tag_ids\":[],\"emails\":[{\"id\":1580," +
                    "\"value\":\"vasya@yandex.ru\",\"last_usage\":0,\"tags\":[]}]," +
                    "\"vcard\":{\"emails\":[{\"email\":\"vasya@yandex.ru\"}]}}," +
                    "\"petya@yandex.ru\":{\"contact_owner_user_id\":1," +
                    "\"contact_owner_user_type\":\"connect_organization\"," +
                    "\"contact_id\":1,\"list_id\":1,\"revision\":\"\",\"tag_ids\":[]," +
                    "\"shared\": true, \"emails\":[{\"id\":2," +
                    "\"value\":\"petya@yandex.ru\",\"last_usage\":0,\"tags\":[]}]," +
                    "\"vcard\":{\"emails\":[{\"email\":\"petya@yandex.ru\"}]}}}";
            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&email=vasya@yandex.ru,petya@yandex.ru&shared=include",
                expected);
            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&email=vasya@yandex.ru,petya@yandex.ru&shared=include&merge=none",
                expected);

            String sharedContact2 = addContact(
                cluster,
                prefixOwner,
                2,
                "VasyaName",
                "",
                "",
                "+100500");
            addEmail(
                cluster,
                prefixOwner,
                3,
                sharedContact2,
                "vasya@yandex.ru",
                "vasya",
                "yandex.ru",
                "");

            expected = "{" +
                "\"vasya@yandex.ru\":{" +
                "\"contact_owner_user_id\":279740642," +
                "\"contact_owner_user_type\":\"passport_user\",\"contact_id\":579,\"list_id\":1,\"revision\":\"\"," +
                "\"tag_ids\":[],\"emails\":[{\"id\":1580,\"value\":\"vasya@yandex.ru\",\"last_usage\":0," +
                "\"tags\":[]}],\"vcard\":{\"emails\":[{\"email\":\"vasya@yandex.ru\"}]}}," +
                "\"petya@yandex.ru\":{" +
                "\"contact_owner_user_id\":1,\"contact_owner_user_type\":\"connect_organization\"," +
                "\"contact_id\":1,\"list_id\":1,\"revision\":\"\",\"shared\":true,\"tag_ids\":[]," +
                "\"emails\":[{\"id\":2,\"value\":\"petya@yandex.ru\",\"last_usage\":0,\"tags\":[]}]," +
                "\"vcard\":{\"emails\":[{\"email\":\"petya@yandex.ru\"}]}}," +
                "\"vasya@yandex.ru\":{" +
                "\"contact_owner_user_id\":1,\"contact_owner_user_type\":\"connect_organization\"," +
                "\"contact_id\":2,\"list_id\":1,\"revision\":\"\",\"shared\":true,\"tag_ids\":[]," +
                "\"emails\":[{\"id\":3,\"value\":\"vasya@yandex.ru\",\"last_usage\":0,\"tags\":[]}]," +
                "\"vcard\":{\"emails\":[{\"email\":\"vasya@yandex.ru\"}]}}}";

            HttpAssert.assertJsonResponse(
                client,
                baseUri + "&email=vasya@yandex.ru,petya@yandex.ru&shared=include&merge=none",
                expected);
        }
    }
}
