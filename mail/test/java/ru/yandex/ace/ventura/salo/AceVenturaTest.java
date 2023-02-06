package ru.yandex.ace.ventura.salo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.ace.ventura.AceVenturaPrefix;
import ru.yandex.ace.ventura.UserType;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.HttpResource;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class AceVenturaTest extends TestBase {
    /**
     * Adds Persistance response
     * @param cluster
     * @param opId
     * @param data
     * @throws Exception
     */
    private void addOpId(
        final AceVenturaSaloCluster cluster,
        final int opId,
        final String data)
        throws Exception
    {
        cluster.msal().add(
            "/operations-queue-envelopes?json-type=dollar&namespace"
                + "=operations_queue&mdb=pg&pgshard=xdb-tst166591&scope"
                + "=contacts&length=4000&op-id=" + opId,
            new StaticHttpResource(HttpStatus.SC_OK, new StringEntity(
                data,
                StandardCharsets.UTF_8)));
    }

    private void addOpId(
        final AceVenturaSaloCluster cluster,
        final int opId,
        final HttpResource envelopes)
        throws Exception
    {
        cluster.msal().add(
            "/operations-queue-envelopes?json-type=dollar&namespace"
            + "=operations_queue&mdb=pg&pgshard=xdb-tst166591&scope"
            + "=contacts&length=4000&op-id=" + opId,
            envelopes);
    }

    private String loadResource(final String name) throws IOException {
        return IOStreamUtils.consume(
            new InputStreamReader(
                this.getClass().getResourceAsStream(name),
                StandardCharsets.UTF_8))
            .toString();
    }

    protected void addToIndex(
            final AceVenturaSaloCluster cluster,
            final String prefix,
            final String resource)
            throws Exception
    {
        HttpPost post = new HttpPost(cluster.searchBackend().indexerUri() + "/add?prefix=" + prefix);
            post.setEntity(new InputStreamEntity(getClass().getResourceAsStream(resource)));
        HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);
    }

    @Test
    public void testShareList() throws Exception {
        try (AceVenturaSaloCluster cluster =
                 new AceVenturaSaloCluster(this))
        {
            addOpId(
                cluster,
                0,
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(getClass().getResourceAsStream(
                        "copy_abook_125.json"))));
            addOpId(cluster, 1, "{\"rows\":[]}");
        }
    }

    @Test
    public void testReindexOrganization() throws Exception {
        try (AceVenturaSaloCluster cluster =
                     new AceVenturaSaloCluster(this))
        {
            cluster.producer().add(
                    "/_producer_lock?service=change_log_0"
                            + "&session-timeout=600000&producer-name=test_xdb-tst166591",
                    new StaticHttpItem("xdb-tst166591@1"));
            cluster.producer().add(
                    "/_producer_position?service=change_log_0"
                            + "&producer-name=test_xdb-tst166591:0",
                    new StaticHttpItem("-1"));

            String base = "./reindex/organization/";
            addToIndex(cluster, "1", base + "owner_initial_state.json");
            addToIndex(cluster, "2", base + "client_1_initial_state.json");
            addToIndex(cluster, "3", base + "client_2_initial_state.json");

            cluster.msal().add("/get-user-shard?&orgId=1", "xdb-tst166591");
            cluster.msal().add(
                    "/get-contacts-list?&scope=contacts&uid=1&userType=connect_organization",
                    loadResource(base + "owner_lists.json"));
            cluster.msal().add(
                    "/get-user-emails?&scope=contacts&uid=1&userType=connect_organization",
                    loadResource(base + "owner_emails.json"));
            cluster.msal().add(
                    "/get-contacts-tags?&scope=contacts&uid=1&userType=connect_organization",
                    "[]");
            cluster.msal().add(
                    "/get-tagged-contacts?&scope=contacts&uid=1&userType=connect_organization",
                    "[]");
            cluster.msal().add(
                    "/get-tagged-emails?&scope=contacts&uid=1&userType=connect_organization",
                    "[]");
            cluster.msal().add("/get-user-contacts?&scope=contacts&listId=1&uid=1&userType=connect_organization",
                    loadResource(base + "owner_contacts.json"));
            cluster.msal().add(
                    "/get-shared-lists?&scope=contacts&uid=1&userType=connect_organization",
                    loadResource(base + "owner_shared_to.json"));

            cluster.start();
            int attempts = 0;
            int maxattempts = 5;
            while (true) {
                try {
                    Thread.sleep(500);
                    HttpGet get = new HttpGet(
                            cluster.salo().host()
                                    + "/reindex-user?uid=1&user_type=connect_organization&provider=test");
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);
                    break;
                } catch (Exception e) {
                    System.err.println("Mdb not locked");
                }

                attempts += 1;
                YandexAssert.assertLess(attempts, maxattempts);
            }

            Thread.sleep(3000);

            waitForIndex(
                    cluster,
                    "av_record_type_p:*&prefix=1$passport_user",
                    new JsonChecker(loadResource(base + "client_1_expected.json")));
            waitForIndex(
                    cluster,
                    "av_record_type_p:*&prefix=2$passport_user",
                    new JsonChecker(loadResource(base + "client_2_expected.json")));
            waitForIndex(
                    cluster,
                    "av_record_type_p:*&prefix=1$connect_organization",
                    new JsonChecker(loadResource(base + "owner_expected.json")));
            // now testing with shared
//            cluster.msal().add("/get-shared-lists?&scope=contacts&uid=1&userType=passport_user",
//                    loadResource("./reindex/shared.json"));
//            cluster.searchBackend().add(
//                    new AceVenturaPrefix(65535L, UserType.PASSPORT_USER),
//                    "\"av_record_type\": \"shared\",\n" +
//                            "\"av_shared_owner_uid\": \"1\",\n" +
//                            "\"av_shared_owner_utype\": \"passport_user\",\n" +
//                            "\"av_shared_list_id\": \"20\",\n" +
//                            "\"id\": \"av_share_1_passport_user_65535_passport_user_20\"\n");
//
//            HttpGet get = new HttpGet(
//                    cluster.salo().host()
//                            + "/reindex-user?uid=1&userType=passport_user&provider=test&shared");
//            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);
//
//            waitForIndex(
//                    cluster,
//                    "av_record_type:shared+AND+NOT+id:av_share_1_*",
//                    new JsonChecker(loadResource("./reindex/expected_shared.json")));
        }
    }

    @Test
    public void testReindexUser() throws Exception {
        try (AceVenturaSaloCluster cluster =
                 new AceVenturaSaloCluster(this))
        {
            cluster.producer().add(
                "/_producer_lock?service=change_log_0"
                    + "&session-timeout=600000&producer-name=test_xdb-tst166591",
                new StaticHttpItem("xdb-tst166591@1"));
            cluster.producer().add(
                "/_producer_position?service=change_log_0"
                    + "&producer-name=test_xdb-tst166591:0",
                new StaticHttpItem("-1"));

            HttpPost user1 = new HttpPost(cluster.searchBackend().indexerUri() + "/add?prefix=1");
            user1.setEntity(
                new InputStreamEntity(getClass().getResourceAsStream("./reindex/previous_state_1.json")));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, user1);
            HttpPost user2 = new HttpPost(cluster.searchBackend().indexerUri() + "/add?prefix=65535");
            user2.setEntity(
                new InputStreamEntity(getClass().getResourceAsStream("./reindex/previous_state_2.json")));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, user2);

            cluster.msal().add("/get-user-shard?&uid=1", "xdb-tst166591");
            cluster.msal().add(
                "/get-contacts-list?&scope=contacts&uid=1&userType=passport_user",
                loadResource("./reindex/lists.json"));
            cluster.msal().add(
                "/get-user-emails?&scope=contacts&uid=1&userType=passport_user",
                loadResource("./reindex/emails.json"));
            cluster.msal().add(
                "/get-contacts-tags?&scope=contacts&uid=1&userType=passport_user",
                loadResource("./reindex/tags.json"));
            cluster.msal().add(
                "/get-tagged-contacts?&scope=contacts&uid=1&userType=passport_user",
                loadResource("./reindex/tagged_contacts.json"));
            cluster.msal().add(
                "/get-tagged-emails?&scope=contacts&uid=1&userType=passport_user",
                loadResource("./reindex/tagged_emails.json"));
            cluster.msal().add("/get-user-contacts?&scope=contacts&listId=1&uid=1&userType=passport_user",
                loadResource("./reindex/contacts.json"));

            cluster.start();
            int attempts = 0;
            int maxattempts = 5;
            while (true) {
                try {
                    Thread.sleep(500);
                    HttpGet get = new HttpGet(
                        cluster.salo().host()
                            + "/reindex-user?uid=1&userType=passport_user&provider=test");
                    HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);
                    break;
                } catch (Exception e) {
                    System.err.println("Mdb not locked");
                }

                attempts += 1;
                YandexAssert.assertLess(attempts, maxattempts);
            }

            waitForIndex(
                cluster,
                "av_record_type:*",
                new JsonChecker(loadResource("./reindex/expected.json")));
            // now testing with shared
            cluster.msal().add("/get-shared-lists?&scope=contacts&uid=1&userType=passport_user",
                loadResource("./reindex/shared.json"));
            cluster.searchBackend().add(
                new AceVenturaPrefix(65535L, UserType.PASSPORT_USER),
                "\"av_record_type\": \"shared\",\n" +
                "\"av_shared_owner_uid\": \"1\",\n" +
                "\"av_shared_owner_utype\": \"passport_user\",\n" +
                "\"av_shared_list_id\": \"20\",\n" +
                "\"id\": \"av_share_1_passport_user_65535_passport_user_20\"\n");

            HttpGet get = new HttpGet(
                cluster.salo().host()
                    + "/reindex-user?uid=1&userType=passport_user&provider=test&shared");
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, get);

            waitForIndex(
                cluster,
                "av_record_type:shared+AND+NOT+id:av_share_1_*",
                new JsonChecker(loadResource("./reindex/expected_shared.json")));
        }
    }

    @Test
    public void testCopyAbook() throws Exception {
        try (AceVenturaSaloCluster cluster =
                 new AceVenturaSaloCluster(this, false, true))
        {
            HttpPost initState =
                new HttpPost(cluster.searchBackend().indexerUri() + "/add?prefix=155776543$passport_user");
            initState.setEntity(
                new InputStreamEntity(getClass().getResourceAsStream("copy_abook_125_init_state.json")));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, initState);

            addOpId(
                cluster,
                0,
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(getClass().getResourceAsStream(
                        "copy_abook_125.json"))));
            addOpId(cluster, 1, "{\"rows\":[]}");

            cluster.producer().add(
                "/_producer_lock?service=change_log_0"
                    + "&session-timeout=600000&producer-name=test_xdb-tst166591",
                new StaticHttpItem("xdb-tst166591@1"));
            cluster.producer().add(
                "/_producer_position?service=change_log_0"
                    + "&producer-name=test_xdb-tst166591:0",
                new StaticHttpItem("-1"));

            cluster.addBlackbox(155776543L, 255776543L);
            cluster.addMailIndexResponse(
                255776543L,
                "{\n" +
                    "    \"hitsCount\": 125,\n" +
                    "    \"hitsArray\": [\n" +
                    "        {\n" +
                    "            \"abook_suid\": \"707757770\",\n" +
                    "            \"abook_email\": \"1976g.rom@gmail.com\",\n" +
                    "            \"abook_source_type\": \"ya_sent\",\n" +
                    "            \"abook_last_contacted\": \"1572480000\",\n" +
                    "            \"abook_times_contacted\": \"102\",\n" +
                    "            \"url\": \"abook_707757770_8\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "            \"abook_suid\": \"707757770\",\n" +
                    "            \"abook_email\": \"vasya@yandex.ru\",\n" +
                    "            \"abook_source_type\": \"ya_sent\",\n" +
                    "            \"abook_last_contacted\": \"1572480000\",\n" +
                    "            \"abook_times_contacted\": \"2\",\n" +
                    "            \"url\": \"abook_707757770_127\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}");

            cluster.msal().add("/get-tagged-contacts?&scope=contacts&uid=155776543&userType=passport_user",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(getClass().getResourceAsStream(
                        "copy_abook_125_tagged_contacts.json"))));

            cluster.msal().add("/get-tagged-emails?&scope=contacts&uid=155776543&userType=passport_user",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(getClass().getResourceAsStream(
                        "copy_abook_125_tagged_emails.json"))));

            cluster.msal().add(
                "/get-contacts-list?&scope=contacts&listId=1&uid=155776543"
                    + "&userType=passport_user",
                "[\n"
                    + "    {\n"
                    + "        \"list_id\": 1,\n"
                    + "        \"list_name\": \"Personal\",\n"
                    + "        \"list_type\": \"personal\",\n"
                    + "        \"revision\": 178\n"
                    + "    }\n"
                    + "]");
            cluster.msal().add(
                "/get-user-contacts?&scope=contacts&listId=1&uid=155776543"
                    + "&userType=passport_user",
                "[\n"
                    + "    {\n"
                    + "        \"contact_id\": 14,\n"
                    + "        \"list_id\": 1,\"vcard\": {\"names\": "
                    + "[{\"last\": \"Pupkin\", \"first\": \"Vasya\"}]},"
                    + "        \"format\": \"vcard_v1\",\n"
                    + "        \"revision\": 179\n"
                    + "    }]");
            cluster.msal().add(
                "/get-user-emails?&scope=contacts&uid=155776543"
                    + "&userType=passport_user",
                "[\n"
                    + "    {\n"
                    + "        \"contact_id\": 14,\n"
                    + "        \"email_id\": 4,\n"
                    + "        \"email\": \"vasya@yandex.ru\",\n"
                    + "        \"revision\": 178\n"
                    + "    },"
                    + "    {\n"
                    + "        \"contact_id\": 14,\n"
                    + "        \"email_id\": 5,\n"
                    + "        \"email_type\": \"\",\n"
                    + "        \"email_label\": \"\",\n"
                    + "        \"email\": \"stepa.vasin@yandex.ru\",\n"
                    + "        \"revision\": 178\n"
                    + "    }\n"
                    + "]");

            cluster.msal().add(
                "/get-contacts-tags?&scope=contacts&uid=155776543"
                    + "&userType=passport_user",
                "[\n"
                    + "    {\n"
                    + "        \"tag_id\": 1,\n"
                    + "        \"tag_name\": \"работа\",\n"
                    + "        \"tag_type\": \"user\",\n"
                    + "        \"revision\": 178\n"
                    + "    },"
                    + "    {\n"
                    + "        \"tag_id\": 2,\n"
                    + "        \"tag_name\": \"друзья\",\n"
                    + "        \"tag_type\": \"user\",\n"
                    + "        \"revision\": 178\n"
                    + "    }\n"
                    + "]");

            cluster.start();

            waitForIndex(
                cluster,
                "id:av_contact_155776543_passport_user_14",
                new JsonChecker("{\"hitsArray\": [{"
                    + "\"av_cid\": \"14\",\n"
                    + "\"av_has_phones\": \"false\",\n"
                    + "\"av_list_id\": \"1\",\n"
                    + "\"av_list_name\": \"Personal\",\n"
                    + "\"av_list_type\": \"personal\",\n"
                    + "\"av_names\": \"Vasya\\tPupkin\",\n"
                    + "\"av_names_alias\": \"Vasya\",\n"
                    + "\"av_record_type\": \"contact\",\n"
                    + "\"av_tags\": \"1\\n2\",\n"
                    + "\"av_contact_tags\": \"1\\n2\",\n"
                    + "\"av_user_id\": \"155776543\",\n"
                    + "\"av_user_type\": \"passport_user\",\n"
                    + "\"av_vcard\": "
                    + "\"{\\\"names\\\":[{\\\"last\\\":\\\"Pupkin\\\","
                    + "\\\"first\\\":\\\"Vasya\\\"}]}\",\n"
                    + "\"id\": \"av_contact_155776543_passport_user_14\""
                    + "}],\"hitsCount\": 1}"));

            waitForIndex(
                cluster,
                "id:av_email_155776543_passport_user_4",
                new JsonChecker("{\"hitsArray\": [{"
                + "\"av_domain\": \"yandex.ru\",\n"
                    + "\"av_domain_nt\": \"yandex\",\n"
                    + "\"av_email\": \"vasya@yandex.ru\",\n"
                    + "\"av_email_cid\": \"14\",\n"
                    + "\"av_email_id\": \"4\",\n"
                    + "\"av_names\": \"Vasya\\tPupkin\",\n"
                    + "\"av_names_alias\": \"Vasya\",\n"
                    + "\"av_list_id\": \"1\",\n"
                    + "\"av_login\": \"vasya\",\n"
                    + "\"av_logins\": \"vasya\",\n"
                    + "\"av_record_type\": \"email\",\n"
                    + "\"av_last_usage\": \"1572480000000\",\n"
                    + "\"av_revision\": \"178\",\n"
                    + "\"av_tags\": \"1\",\n"
                    + "\"av_email_tags\": \"1\",\n"
                    + "\"av_contact_tags\": \"1\\n2\",\n"
                    + "\"av_user_id\": \"155776543\",\n"
                    + "\"av_user_type\": \"passport_user\",\n"
                    + "\"id\": \"av_email_155776543_passport_user_4\""
                    + "}],\"hitsCount\": 1}"));

            waitForIndex(
                cluster,
                "id:av_email_155776543_passport_user_5",
                new JsonChecker("{\"hitsArray\": [{"
                    + "\"av_domain\": \"yandex.ru\",\n"
                    + "\"av_domain_nt\": \"yandex\",\n"
                    + "\"av_email\": \"stepa.vasin@yandex.ru\",\n"
                    + "\"av_email_cid\": \"14\",\n"
                    + "\"av_names\": \"Vasya\\tPupkin\",\n"
                    + "\"av_names_alias\": \"Vasya\",\n"
                    + "\"av_list_id\": \"1\",\n"
                    + "\"av_email_id\": \"5\",\n"
                    + "\"av_login\": \"stepa.vasin\",\n"
                    + "\"av_logins\": \"stepa.vasin\nstepa-vasin\",\n"
                    + "\"av_record_type\": \"email\",\n"
                    + "\"av_last_usage\": \"0\",\n"
                    + "\"av_revision\": \"178\",\n"
                    + "\"av_tags\": \"1\\n2\",\n"
                    + "\"av_email_tags\": \"1\\n2\",\n"
                    + "\"av_contact_tags\": \"1\\n2\",\n"
                    + "\"av_email_type\": \"\","
                    + "\"av_email_label\": \"\","
                    + "\"av_user_id\": \"155776543\",\n"
                    + "\"av_user_type\": \"passport_user\",\n"
                    + "\"id\": \"av_email_155776543_passport_user_5\""
                    + "}],\"hitsCount\": 1}"));

            waitForIndex(
                cluster,
                "id:*",
                new JsonChecker(loadResource("copy_abook_125_expected.json")));
        }
    }

    private static String opQueue(final String... rows) {
        return "{\"rows\": [{" + String.join("},{", rows) + "}]}";
    }

    private static String opRow(
        final int opId,
        final String userId,
        final String userType,
        final String revision,
        final String type,
        final String args,
        final String chaged)
    {
        return "\"operation_id\": " + opId
            + ",\"user_id\": \"" + userId + "\",\n"
            + "\"user_type\": \"" + userType + "\",\n"
            + "\"revision\": \"" + revision + "\",\n"
            + "\"change_type\": \"" + type + "\",\n"
            + "\"operation_date\": \"1551114750.860455\",\n"
            + "\"select_date\": \"1555147693.539\",\n"
            + "\"arguments\": {" + args + "},\n"
            + "\"changed\": {" + chaged + "}";
    }

    private void waitForIndex(
        final AceVenturaSaloCluster cluster,
        final String text,
        final JsonChecker checker)
        throws Exception
    {
        waitForIndex(cluster, text, checker, 2000);
    }

    private void waitForIndex(
        final AceVenturaSaloCluster cluster,
        final String text,
        final JsonChecker checker,
        final long timeout)
        throws Exception
    {
        long enTs = System.currentTimeMillis() + timeout;
        Exception exception = null;
        String result = "Not runned";
        String lastOutput = "null";
        while (System.currentTimeMillis() < enTs) {
            Thread.sleep(100);
            try {
                lastOutput = cluster.searchBackend().getSearchOutput(
                        "/search?&text=" + text + "&get=*&early-interrupt=false&length=10");
                result = checker.check(lastOutput);
                if (result == null) {
                    return;
                }
            } catch (HttpException | IOException e) {
                exception = e;
            }
        }

        System.out.println("Last server output: " + lastOutput);
        if (exception != null) {
            throw exception;
        }

        Assert.fail(result);
    }

    @Test
    public void testDeleteUser() throws Exception {
        try (AceVenturaSaloCluster cluster =
                 new AceVenturaSaloCluster(this)) {
            AceVenturaPrefix prefix = new AceVenturaPrefix(801784130L, UserType.PASSPORT_USER);
            AceVenturaPrefix other = new AceVenturaPrefix(801784131L, UserType.PASSPORT_USER);
            addOpId(
                cluster,
                0,
                "{\"rows\":[{\"operation_id\":0,\"user_id\":801784130,\"user_type\":\"passport_user\"," +
                    "\"revision\":1,\"change_type\":\"delete_user\",\"operation_date\":1579184200.995791," +
                    "\"changed\":{},\"arguments\":{},\"pgshard\":2633,\"select_date\":1579186192.776}]}");
            addOpId(cluster, 1, "{\"rows\":[]}");
            cluster.searchBackend().add(
                prefix,
                "\"av_record_type\": \"contact\",\n" +
                    "\"id\": \"av_share_1_passport_user_801784130_passport_user_20\"\n");

            cluster.searchBackend().add(
                other,
                "\"av_record_type\": \"contact\",\n" +
                    "\"id\": \"av_share_1_passport_user_801784131_passport_user_20\"\n");

            cluster.producer().add(
                "/_producer_lock?service=change_log_0"
                    + "&session-timeout=600000&producer-name=test_xdb-tst166591",
                new StaticHttpItem("xdb-tst166591@1"));
            cluster.producer().add(
                "/_producer_position?service=change_log_0"
                    + "&producer-name=test_xdb-tst166591:0",
                new StaticHttpItem("-1"));

            cluster.msal().add("/*",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new StringEntity("[]", StandardCharsets.UTF_8)));

            cluster.start();

            waitForIndex(
                cluster,
                "av_record_type:*",
                new JsonChecker("{\"hitsArray\": [{\"av_record_type\": \"contact\",\n" +
                    "\"id\": \"av_share_1_passport_user_801784131_passport_user_20\"}],\"hitsCount\":1}"));
        }
    }

    @Test
    public void testCreateUpdateContacts() throws Exception {
        try (AceVenturaSaloCluster cluster =
                 new AceVenturaSaloCluster(this))
        {
            addOpId(cluster, 0, loadResource("./update/create_contacts_emails.json"));

            cluster.producer().add(
                "/_producer_lock?service=change_log_0"
                    + "&session-timeout=600000&producer-name=test_xdb-tst166591",
                new StaticHttpItem("xdb-tst166591@1"));
            cluster.producer().add(
                "/_producer_position?service=change_log_0"
                    + "&producer-name=test_xdb-tst166591:0",
                new StaticHttpItem("-1"));

            cluster.start();

            waitForIndex(
                cluster,
                "av_record_type:contact",
                new JsonChecker(loadResource("./update/expected_after_create_contacts.json")));

            waitForIndex(
                cluster,
                "av_record_type:email",
                new JsonChecker(loadResource("./update/expected_after_create_emails.json")));

            addOpId(cluster, 2, loadResource("./update/update_contacts.json"));


            waitForIndex(
                cluster,
                "av_record_type:contact",
                new JsonChecker(loadResource("./update/expected_after_update_contacts.json")));

            waitForIndex(
                cluster,
                "av_record_type:email",
                new JsonChecker(loadResource("./update/expected_after_update_emails.json")));

            addOpId(cluster, 3, "{\"rows\":[{"
                + "\"operation_id\":3,\"user_id\":279740642,"
                + "\"user_type\":\"passport_user\",\"revision\":4,"
                + "\"change_type\":\"delete_contacts\","
                + "\"operation_date\":1563465196.935626,\"changed\":"
                + "{\"contacts\":[{\"list_id\":1,\"format\":\"vcard_v1\","
                + "\"vcard\":{},\"contact_id\":588,\"uri\":null}]},"
                + "\"arguments\":{\"contact_ids\":[588]},"
                + "\"pgshard\":2358,\"select_date\":1563963169.329}]}");

            waitForIndex(
                cluster,
                "av_record_type:contact",
                new JsonChecker("{\"hitsCount\": 0, \"hitsArray\":[]}"));
        }
    }

    @Test
    public void testCreateSpecificEmails() throws Exception {
        String createEmails = "{\n" +
            "    \"rows\": [\n" +
            "        {\n" +
            "            \"operation_id\": \"0\",\n" +
            "            \"user_id\": \"194953659\",\n" +
            "            \"user_type\": \"passport_user\",\n" +
            "            \"revision\": \"8754\",\n" +
            "            \"change_type\": \"create_emails\",\n" +
            "            \"operation_date\": \"1599046354.356211\",\n" +
            "            \"changed\": {\n" +
            "                \"email_ids\": [\n" +
            "                    599\n" +
            "                ]\n" +
            "            },\n" +
            "            \"arguments\": {\n" +
            "                \"emails\": [\n" +
            "                    {\n" +
            "                        \"label\": null,\n" +
            "                        \"type\": null,\n" +
            "                        \"contact_id\": 645,\n" +
            "                        \"email\": \"dolina.bezmolviya@mail.ru\"\n" +
            "                    }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"pgshard\": \"2771\",\n" +
            "            \"select_date\": \"1599987991.639\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";

        try (AceVenturaSaloCluster cluster =
                 new AceVenturaSaloCluster(this)) {
            addOpId(cluster, 0, createEmails);

            cluster.producer().add(
                "/_producer_lock?service=change_log_0"
                    + "&session-timeout=600000&producer-name=test_xdb-tst166591",
                new StaticHttpItem("xdb-tst166591@1"));
            cluster.producer().add(
                "/_producer_position?service=change_log_0"
                    + "&producer-name=test_xdb-tst166591:0",
                new StaticHttpItem("-1"));

            cluster.start();
            Thread.sleep(15000);
        }
    }

    @Test
    public void testCreateContactsEmails() throws Exception {
        String createContacts = "{\n"
            + "\"rows\": [{\"operation_id\":\"0\","
            + "\"user_id\":\"279740642\",\"user_type\":\"passport_user\","
            + "\"revision\":\"104\",\"change_type\":\"create_contacts\","
            + "\"operation_date\":\"1562848494.668643\","
            + "\"changed\":{\"contact_ids\":[586]},"
            + "\"arguments\":{\"contacts\":[{\"list_id\":1,"
            + "\"format\":\"vcard_v1\","
            + "\"vcard\":{\"telephone_numbers\":[{\"telephone_number"
            + "\":\"+70001234567\"},{\"telephone_number\":\"+13335550101\"}],"
            + "\"names\":[{\"last\":\"Last\",\"prefix\":\"Mr.\","
            + "\"first\":\"First\"},{\"first\":\"Ooops\"}]},\"uri\":null}]},"
            + "\"pgshard\":\"2094\",\"select_date\":\"1562855571.967\"}]}";

        String expAfterCreate =
            " {\"hitsArray\": [{"
                + "\"av_cid\": \"586\","
                + "\"av_names\": \"First\\tLast\\nOoops\\t\","
                + "\"av_names_alias\": \"First\nOoops\","
                + "\"av_phones\": \"+70001234567\n+13335550101\","
                + "\"av_phones_n\": \"+70001234567\n0001234567\n+13335550101\","
                + "\"av_user_id\": \"279740642\","
                + "\"av_list_id\": \"1\","
                + "\"av_revision\": \"104\","
                + "\"av_record_type\": \"contact\","
                + "\"av_has_phones\": \"true\","
                + "\"av_user_type\": \"passport_user\","
                + "\"av_vcard\": \"{\\\"telephone_numbers"
                + "\\\":[{\\\"telephone_number\\\":\\\"+70001234567\\\"},"
                + "{\\\"telephone_number\\\":\\\"+13335550101\\\"}],"
                + "\\\"names\\\":[{\\\"last\\\":\\\"Last\\\","
                + "\\\"prefix\\\":\\\"Mr.\\\","
                + "\\\"first\\\":\\\"First\\\"},"
                + "{\\\"first\\\":\\\"Ooops\\\"}]}\","
                + "\"id\": \"av_contact_279740642_passport_user_586\""
                + "}],\"hitsCount\": 1}";

        String createEmails = "{\"rows\":[{\"operation_id\":\"1\","
            + "\"user_id\":\"279740642\",\"user_type\":\"passport_user\","
            + "\"revision\":\"105\",\"change_type\":\"create_emails\","
            + "\"operation_date\":\"1562848494.668643\","
            + "\"changed\":{\"email_ids\":[10]},"
            + "\"arguments\":{\"emails\":[{\"label\":null,\"type\":null,"
            + "\"contact_id\":586,\"email\":\"first-last@lynn.ru\"}]},"
            + "\"pgshard\":\"2094\",\"select_date\":\"1562855571.967\"}]}";

        String updateEmails = "{\"rows\":[{\"operation_id\":\"2\","
            + "\"user_id\":\"279740642\",\"user_type\":\"passport_user\","
            + "\"revision\":\"106\",\"change_type\":\"update_emails\","
            + "\"operation_date\":\"1562848494.668643\","
            + "\"changed\":{\"emails\":[{\n"
            + "      \"email\": \"s.pervukhin@cety-telekom.ru\","
            + "      \"contact_id\": 586,"
            + "      \"type\": null,"
            + "      \"label\": null,"
            + "      \"email_id\": 10}]},"
            + "\"arguments\":{\"emails\":[{"
            + "      \"email\": \"s.pervukhin@cety-telekom.ru\","
            + "      \"contact_id\": null,"
            + "      \"type\": null,"
            + "      \"label\": null,"
            + "      \"email_id\": 10}]},"
            + "\"pgshard\":\"2094\",\"select_date\":\"1562855571.967\"}]}";

        String expAfterCreateEmails =
            " {\"hitsArray\": [{"
                + "\"av_domain\": \"lynn.ru\",\n"
                + "\"av_domain_nt\": \"lynn\",\n"
                + "\"av_email\": \"first-last@lynn.ru\",\n"
                + "\"av_email_cid\": \"586\",\n"
                + "\"av_email_id\": \"10\",\n"
                + "\"av_list_id\": \"1\","
                + "\"av_last_usage\": \"0\","
                + "\"av_names\": \"First\\tLast\\nOoops\\t\","
                + "\"av_names_alias\": \"First\nOoops\","
                + "\"av_has_phones\": \"true\","
                + "\"av_email_label\": \"\",\n"
                + "\"av_email_type\": \"\",\n"
                + "\"av_user_id\": \"279740642\","
                + "\"av_login\": \"first-last\",\n"
                + "\"av_logins\": \"first-last\",\n"
                + "\"av_record_type\": \"email\",\n"
                + "\"av_user_type\": \"passport_user\","
                + "\"av_revision\": \"105\","
                + "\"id\": \"av_email_279740642_passport_user_10\""
                + "}],\"hitsCount\": 1}";

        String expAfterUpdateEmails =
            " {\"hitsArray\": [{"
                + "\"av_domain\": \"cety-telekom.ru\",\n"
                + "\"av_domain_nt\": \"cety-telekom\",\n"
                + "\"av_email\": \"s.pervukhin@cety-telekom.ru\",\n"
                + "\"av_email_cid\": \"586\",\n"
                + "\"av_email_id\": \"10\",\n"
                + "\"av_list_id\": \"1\","
                + "\"av_last_usage\": \"0\","
                + "\"av_names\": \"First\\tLast\\nOoops\\t\","
                + "\"av_names_alias\": \"First\nOoops\","
                + "\"av_has_phones\": \"true\","
                + "\"av_user_id\": \"279740642\","
                + "\"av_login\": \"s.pervukhin\",\n"
                + "\"av_logins\": \"s.pervukhin\",\n"
                + "\"av_record_type\": \"email\",\n"
                + "\"av_user_type\": \"passport_user\","
                + "\"av_revision\": \"106\","
                + "\"id\": \"av_email_279740642_passport_user_10\""
                + "}],\"hitsCount\": 1}";

        try (AceVenturaSaloCluster cluster =
                 new AceVenturaSaloCluster(this))
        {
            addOpId(cluster, 0, createContacts);

            cluster.producer().add(
                "/_producer_lock?service=change_log_0"
                    + "&session-timeout=600000&producer-name=test_xdb-tst166591",
                new StaticHttpItem("xdb-tst166591@1"));
            cluster.producer().add(
                "/_producer_position?service=change_log_0"
                    + "&producer-name=test_xdb-tst166591:0",
                new StaticHttpItem("-1"));

            cluster.start();

            waitForIndex(
                cluster,
                "id:av_contact_279740642_passport_user_586",
                new JsonChecker(expAfterCreate));

            addOpId(cluster, 1, createEmails);

            waitForIndex(
                cluster,
                "id:av_email_279740642_passport_user_10"
                    + "&prefix=279740642$passport_user",
                new JsonChecker(expAfterCreateEmails));

            addOpId(cluster, 2, updateEmails);

            waitForIndex(
                cluster,
                "id:av_email_279740642_passport_user_10"
                    + "&prefix=279740642$passport_user",
                new JsonChecker(expAfterUpdateEmails));

            addOpId(
                cluster,
                3,
                "{\"rows\":[{\"operation_id\":3,\"user_id\":279740642,"
                    + "\"user_type\":\"passport_user\",\"revision\":3,"
                    + "\"change_type\":\"delete_emails\","
                    + "\"operation_date\":1563465196.935626,"
                    + "\"changed\":{\"emails\":[{\"email_id\":10,\"label\":null,"
                    + "\"type\":null,\"contact_id\":586,"
                    + "\"email\":\"first-last@lynn.ru\"}]},"
                    + "\"arguments\":{\"email_ids\":[10]},"
                    + "\"pgshard\":2358,\"select_date\":1563963169.329}]}");

            String expected = "{\"hitsArray\": [], \"hitsCount\":0}";
            waitForIndex(
                cluster,
                "id:av_email_279740642_passport_user_10"
                    + "&prefix=279740642$passport_user",
                new JsonChecker(expected));
        }
    }

    @Test
    public void testCopyAbookBig() throws Exception {
        try (AceVenturaSaloCluster cluster =
                 new AceVenturaSaloCluster(this, true, true))
        {
            addOpId(
                cluster,
                0,
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(getClass().getResourceAsStream(
                        "copy_abook_126.json"))));
            addOpId(cluster, 1, "{\"rows\":[]}");

//            cluster.producer().add(
//                "/_producer_lock?service=change_log_0"
//                    + "&session-timeout=600000&producer-name=test_xdb-tst166591",
//                new StaticHttpItem("xdb-tst166591@1"));
//            cluster.producer().add(
//                "/_producer_position?service=change_log_0"
//                    + "&producer-name=test_xdb-tst166591:0",
//                new StaticHttpItem("-1"));

            cluster.msal().add(
                "/get-contacts-list?&scope=contacts&listId=1&uid=8134"
                    + "&userType=passport_user",
                "[\n"
                    + "    {\n"
                    + "        \"list_id\": 1,\n"
                    + "        \"list_name\": \"Personal\",\n"
                    + "        \"list_type\": \"personal\",\n"
                    + "        \"revision\": 88\n"
                    + "    }\n"
                    + "]");

            cluster.msal().add("/get-tagged-emails?&scope=contacts&uid=8134&userType=passport_user","[]");
            cluster.msal().add("/get-tagged-contacts?&scope=contacts&uid=8134&userType=passport_user","[]");

            cluster.msal().add(
                "/get-user-contacts?&scope=contacts&listId=1&uid=8134"
                    + "&userType=passport_user",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(getClass().getResourceAsStream(
                        "copy_abook_126_contacts.json"))));
            cluster.msal().add(
                "/get-user-emails?&scope=contacts&uid=8134"
                    + "&userType=passport_user",
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(getClass().getResourceAsStream(
                        "copy_abook_126_emails.json"))));

            cluster.msal().add(
                "/get-contacts-tags?&scope=contacts&uid=8134"
                    + "&userType=passport_user",
                "[\n"
                    + "    {\n"
                    + "        \"tag_id\": 2,\n"
                    + "        \"tag_name\": \"Invited\",\n"
                    + "        \"tag_type\": \"system\",\n"
                    + "        \"revision\": 88\n"
                    + "    },\n"
                    + "    {\n"
                    + "        \"tag_id\": 1,\n"
                    + "        \"tag_name\": \"Phone\",\n"
                    + "        \"tag_type\": \"system\",\n"
                    + "        \"revision\": 88\n"
                    + "    }\n"
                    + "]");

            cluster.addBlackbox(8134L, 3155633L);
            cluster.addMailIndexResponse(
                3155633L,
                "{\n" +
                    "    \"hitsCount\": 125,\n" +
                    "    \"hitsArray\": [\n" +
                    "        {\n" +
                    "            \"abook_suid\": \"3155633L\",\n" +
                    "            \"abook_email\": \"1976g.rom@gmail.com\",\n" +
                    "            \"abook_source_type\": \"ya_sent\",\n" +
                    "            \"abook_last_contacted\": \"1572480000\",\n" +
                    "            \"abook_times_contacted\": \"102\",\n" +
                    "            \"url\": \"abook_707757770_8\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "            \"abook_suid\": \"3155633L\",\n" +
                    "            \"abook_email\": \"vasya@yandex.ru\",\n" +
                    "            \"abook_source_type\": \"ya_sent\",\n" +
                    "            \"abook_last_contacted\": \"1572480000\",\n" +
                    "            \"abook_times_contacted\": \"2\",\n" +
                    "            \"url\": \"abook_707757770_127\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}");
            Thread.sleep(1000);
            cluster.start();

            waitForIndex(
                cluster,
                "id:av_contact_8134_passport_user_8",
                new JsonChecker(
                    "{\"hitsArray\": [{"
                        + "\"av_cid\": \"8\","
                        + "\"av_list_type\": \"personal\","
                        + "\"av_list_name\": \"Personal\","
                        + "\"av_list_id\": \"1\","
                        + "\"av_has_phones\": \"false\","
                        + "\"av_names\": \"Василиса\\tДубова\","
                        + "\"av_names_alias\": \"василиса\nвасилиска\nваселиса\nвася\","
                        + "\"av_user_id\": \"8134\","
                        + "\"av_record_type\": \"contact\","
                        + "\"av_user_type\": \"passport_user\","
                        + "\"av_vcard\": "
                        + "\"{\\\"names\\\":[{\\\"last\\\":\\\"Дубова\\\","
                        + "\\\"first\\\":\\\"Василиса\\\",\\\"middle\\\":"
                        + "\\\"\\\"}]}\","
                        + "\"id\": \"av_contact_8134_passport_user_8\"}],"
                        + "\"hitsCount\": 1}"),
                10000);

            waitForIndex(
                cluster,
                "id:av_email_8134_passport_user_9",
                new JsonChecker(
                    "{\"hitsArray\": [{"
                        + "\"av_domain\": \"yandex.ru\",\n"
                        + "\"av_domain_nt\": \"yandex\",\n"
                        + "\"av_email\": \"cherry-vassa@ya.ru\",\n"
                        + "\"av_email_cid\": \"8\",\n"
                        + "\"av_last_usage\": \"0\",\n"
                        + "\"av_email_id\": \"9\",\n"
                        + "\"av_email_type\": \"\",\n"
                        + "\"av_email_label\": \"\",\n"
                        + "\"av_names\": \"Василиса\\tДубова\","
                        + "\"av_names_alias\": \"василиса\nвасилиска\nваселиса\nвася\","
                        + "\"av_list_id\": \"1\","
                        + "\"av_tags\": \"\",\n"
                        + "\"av_email_tags\": \"\",\n"
                        + "\"av_login\": \"cherry-vassa\",\n"
                        + "\"av_logins\": \"cherry-vassa\",\n"
                        + "\"av_record_type\": \"email\",\n"
                        + "\"av_revision\": \"88\",\n"
                        + "\"av_user_id\": \"8134\",\n"
                        + "\"av_user_type\": \"passport_user\",\n"
                        + "\"id\": \"av_email_8134_passport_user_9\"}],"
                        + "\"hitsCount\": 1}"));

            String response = cluster.searchBackend().getSearchOutput(
                "/search?&text=av_record_type:email&get=id");
            System.out.println("Lucene Response " + response);
            long emails =
                TypesafeValueContentHandler.parse(response)
                    .asMap()
                    .getLong("hitsCount");

            // total is 55 but one is malformed
            Assert.assertEquals(
                "Emails indexed",
                54L,
                emails);

            long contacts = TypesafeValueContentHandler.parse(
                cluster.searchBackend().getSearchOutput(
                    "/search?&text=av_record_type:contact&get=id")).asMap()
                .getLong("hitsCount");

            Assert.assertEquals(
                "Contacts indexed",
                54L,
                contacts);

            long tags = TypesafeValueContentHandler.parse(
                cluster.searchBackend().getSearchOutput(
                    "/search?&text=av_record_type:tag&get=id")).asMap()
                .getLong("hitsCount");

            Assert.assertEquals(
                "Tags indexed",
                2L,
                tags);
        }
    }

    @Test
    public void testCreateEmails() throws Exception {
        String uid = "155776543";
        String userType = "passport_user";
        String createContacts = opQueue(opRow(0,
            uid,
            userType,
            "55",
            "create_contacts",
            "\"contacts\": [{"
                + " \"uri\": \"123\", "
                + " \"vcard\": {\"names\": [{\"last\": \"Банкетов\"}]},"
                + " \"format\": \"vcard_v1\", "
                + " \"list_id\": 1}]",
            "\"contact_ids\": [14]"
        ));

        String createEmails =
            opQueue(opRow(
                1,
                uid,
                userType,
                "56",
                "create_emails",
                "\"emails\": [{\"email_id\": 1, \"contact_id\": 14, "
                    + "\"email\": \"vasya@yandex.ru\", \"type\": \"\", "
                    + "\"label\": \"\"},"
                    + "{\"email_id\": 2, \"type\": \"\", \"label\": \"\","
                    + " \"contact_id\": 14,"
                    + "\"email\": \"petya.vasin@gmail.com\"}]",
                "\"email_ids\": [1, 2]"));

        try (AceVenturaSaloCluster cluster = new AceVenturaSaloCluster(this)) {
            addOpId(cluster, 0, createContacts);

            cluster.start();

            // no list_id list_name ?
            String expected =
                " {\"hitsArray\": [{"
                    + "\"av_names_alias\": \"\",\n"
                        + "\"av_cid\": \"14\",\n"
                        + "\"av_list_id\": \"1\",\n"
                        + "\"av_revision\": \"55\",\n"
                        + "\"av_names\": \"Банкетов\",\n"
                        + "\"av_vcard\": "
                        + "\"{\\\"names\\\":[{\\\"last\\\":\\\"Банкетов\\\"}]}\",\n"
                        + "\"id\": \"av_contact_155776543_passport_user_14\",\n"
                        + "\"av_has_phones\": \"false\",\n"
                        + "\"av_user_id\": \"155776543\",\n"
                        + "\"av_user_type\": \"passport_user\",\n"
                        + "\"av_record_type\": \"contact\"}],\"hitsCount\": 1}";
            waitForIndex(
                cluster,
                "id:av_contact_155776543_passport_user_14",
                new JsonChecker(expected));

            addOpId(cluster, 1, createEmails);

            expected =
                " {\"hitsArray\": [{"
                    + "\"av_domain\": \"yandex.ru\",\n"
                    + "\"av_domain_nt\": \"yandex\",\n"
                    + "\"av_email\": \"vasya@yandex.ru\",\n"
                    + "\"av_email_cid\": \"14\",\n"
                    + "\"av_email_id\": \"1\",\n"
                    + "\"av_list_id\": \"1\","
                    + "\"av_last_usage\": \"0\","
                    + "\"av_has_phones\": \"false\","
                    + "\"av_names\": \"Банкетов\","
                    + "\"av_names_alias\": \"\","
                    + "\"av_email_type\": \"\",\n"
                    + "\"av_email_label\": \"\",\n"
                    + "\"av_login\": \"vasya\",\n"
                    + "\"av_logins\": \"vasya\",\n"
                    + "\"av_record_type\": \"email\",\n"
                    + "\"av_revision\": \"56\",\n"
                    + "\"av_user_id\": \"155776543\",\n"
                    + "\"av_user_type\": \"passport_user\",\n"
                    + "\"id\": \"av_email_155776543_passport_user_1\""
                    + "}],\"hitsCount\": 1}";
            waitForIndex(
                cluster,
                "id:av_email_155776543_passport_user_1",
                new JsonChecker(expected));

            expected =
                " {\"hitsArray\": [{"
                    + "\"av_domain\": \"gmail.com\",\n"
                    + "\"av_domain_nt\": \"gmail\",\n"
                    + "\"av_email\": \"petya.vasin@gmail.com\",\n"
                    + "\"av_email_cid\": \"14\",\n"
                    + "\"av_email_id\": \"2\",\n"
                    + "\"av_email_type\": \"\",\n"
                    + "\"av_email_label\": \"\",\n"
                    + "\"av_list_id\": \"1\","
                    + "\"av_last_usage\": \"0\","
                    + "\"av_has_phones\": \"false\","
                    + "\"av_names\": \"Банкетов\","
                    + "\"av_names_alias\": \"\","
                    + "\"av_login\": \"petya.vasin\",\n"
                    + "\"av_logins\": \"petya.vasin\npetyavasin\",\n"
                    + "\"av_record_type\": \"email\",\n"
                    + "\"av_revision\": \"56\",\n"
                    + "\"av_user_id\": \"155776543\",\n"
                    + "\"av_user_type\": \"passport_user\",\n"
                    + "\"id\": \"av_email_155776543_passport_user_2\""
                    + "}],\"hitsCount\": 1}";
            waitForIndex(
                cluster,
                "id:av_email_155776543_passport_user_2",
                new JsonChecker(expected));
        }
    }

    @Test
    public void testTagApply() throws Exception {
        try (AceVenturaSaloCluster cluster =
                 new AceVenturaSaloCluster(this))
        {
            addOpId(
                cluster,
                0,
                changeRows(
                    changeItem(
                        "1234",
                        "create_tag",
                        0,
                        "\"tag_id\":\"12\"",
                        "\"name\": \"First tag\", \"type\":\"system\"",
                        0),
                    changeItem(
                        "1234",
                        "create_contacts",
                        1,
                        "\"contact_ids\":[51, 52]",
                        "\"contacts\":[{\"list_id\":1,\"format\":\"vcard_v1\","
                            + "\"vcard\":{}}, {\"list_id\":1,"
                            + "\"format\":\"vcard_v1\",\"vcard\":{}}]",
                        1),
                    changeItem(
                        "1234",
                        "create_emails",
                        2,
                        "\"email_ids\":[61]",
                        "\"emails\":[{\"label\":null,\"type\":null,"
                            + "\"contact_id\":51,\"email\":\"test@email.ru\"}]",
                        2)));
            addOpId(
                cluster,
                3,
                changeRow(
                    "1234",
                    "tag_contacts",
                    3,
                    "\"contact_ids\": [51, 52]",
                    "\"tag_id\": 12,\"contact_ids\": [51, 52]",
                    3));
            String expectedContacts =
                "{\"hitsArray\": [{\"av_cid\": \"51\",\"av_list_id\": \"1\",\"av_vcard\": \"{}\"," +
                    "\"av_has_phones\": \"false\",\"id\":\"av_contact_1234_passport_user_51\"," +
                    "\"av_user_id\": \"1234\",\"av_revision\":\"3\",\"av_user_type\": \"passport_user\"," +
                    "\"av_tags\":\"12\",\"av_contact_tags\":\"12\",\"av_record_type\": \"contact\"}," +
                    "{\"av_cid\": \"52\",\"av_list_id\": \"1\",\"av_vcard\": \"{}\",\"av_has_phones\": \"false\"," +
                    "\"id\": \"av_contact_1234_passport_user_52\",\"av_user_id\": \"1234\",\"av_revision\":\"3\"," +
                    "\"av_user_type\": \"passport_user\",\"av_tags\":\"12\",\"av_contact_tags\":\"12\"," +
                    "\"av_record_type\": \"contact\"}],\"hitsCount\": 2}";
            String expectedEmails =
                "{\"hitsArray\": [{\"av_list_id\": \"1\",\"av_last_usage\": \"0\",\"av_has_phones\": \"false\","
                    + "\"av_domain\": \"email.ru\",\"av_domain_nt\": \"email\","
                    + "\"av_email\": \"test@email.ru\",\"av_email_cid\":\"51\","
                    + "\"av_email_id\": \"61\",\"av_email_type\": \"\",\"av_email_label\": \"\","
                    + "\"av_login\": \"test\",\"av_logins\": \"test\",\"av_record_type\": \"email\","
                    + "\"av_revision\": \"2\",\"av_contact_tags\": \"12\",\"av_user_id\": \"1234\","
                    + "\"id\": \"av_email_1234_passport_user_61\",\"av_user_type\": \"passport_user\""
                    + "}],\"hitsCount\": 1}";
            cluster.start();
            waitForIndex(
                cluster,
                "av_tags:12&prefix=1234$passport_user",
                new JsonChecker(expectedContacts));

            waitForIndex(
                cluster,
                "av_record_type_p:email+AND+av_contact_tags:12&prefix=1234$passport_user",
                new JsonChecker(expectedEmails));
            addOpId(
                cluster,
                4,
                changeRow(
                    "1234",
                    "tag_emails",
                    4,
                    "",
                    "\"tag_id\": 12,\"email_ids\": [61]", 4));
            String expectedEmailsTag12 =
                " {\"hitsArray\": [{"
                    + "\"av_domain\": \"email.ru\",\"av_domain_nt\": \"email\","
                    + "\"av_email\":\"test@email.ru\",\"av_email_cid\": \"51\","
                    + "\"av_email_id\": \"61\",\"av_email_type\": \"\",\n"
                    + "\"av_email_label\": \"\",\"av_last_usage\": \"0\",\n"
                    + "\"av_list_id\": \"1\",\"av_has_phones\": \"false\","
                    + "\"av_logins\": \"test\",\"av_login\": \"test\",\"av_record_type\": \"email\",\n"
                    + "\"av_revision\": \"4\",\"av_user_id\": \"1234\",\"av_contact_tags\": \"12\","
                    + "\"av_user_type\": \"passport_user\",\"av_tags\":\"12\",\"av_email_tags\":\"12\","
                    + "\"id\": \"av_email_1234_passport_user_61\""
                    + "}],\"hitsCount\": 1}";
            waitForIndex(
                cluster,
                "av_tags:12+AND+av_record_type:email"
                    + "&prefix=1234$passport_user",
                new JsonChecker(expectedEmailsTag12));
            addOpId(
                cluster,
                5,
                changeRow(
                    "1234",
                    "untag_contacts",
                    5,
                    "",
                    "\"tag_id\": \"12\", \"contact_ids\":[51]",
                    5));
            // we removed tag 12 from cid 51, email still tagged and 52 cid also
            String expected =
                " {\"hitsArray\": [{"
                    + "\"av_cid\": \"52\",\"av_list_id\": \"1\","
                    + "\"av_vcard\": \"{}\",\"av_has_phones\": \"false\","
                    + "\"id\": \"av_contact_1234_passport_user_52\",\n"
                    + "\"av_user_id\": \"1234\",\"av_revision\":\"3\","
                    + "\"av_user_type\": \"passport_user\",\"av_tags\":\"12\",\"av_contact_tags\":\"12\","
                    + "\"av_record_type\": \"contact\"}, "
                    + "{\"av_tags\": \"12\",\"av_email_tags\":\"12\",\"av_contact_tags\":\"\",\"av_last_usage\": \"0\","
                    + "\"av_email_cid\":\"51\",\"av_email_id\": \"61\",\"av_record_type\": \"email\","
                    + "\"av_list_id\": \"1\",\"av_has_phones\": \"false\",\"av_domain\": \"email.ru\","
                    + "\"av_domain_nt\": \"email\",\"av_email\": \"test@email.ru\",\"av_email_type\": \"\","
                    + "\"av_email_label\": \"\",\"av_logins\": \"test\"," +
                    "\"av_login\": \"test\",\"av_revision\": \"4\",\"av_user_id\": \"1234\","
                    + "\"id\": \"av_email_1234_passport_user_61\",\"av_user_type\": \"passport_user\"}]," +
                    "\"hitsCount\": 2}";
            waitForIndex(
                cluster,
                "av_tags:12&prefix=1234$passport_user",
                new JsonChecker(expected));
            addOpId(
                cluster,
                6,
                changeRow(
                    "1234",
                    "untag_emails",
                    6,
                    "",
                    "\"tag_id\": \"12\", \"email_ids\":[61]",
                    6));
            expected =
                " {\"hitsArray\": [{"
                    + "\"av_cid\": \"52\",\"av_list_id\": \"1\","
                    + "\"av_vcard\": \"{}\",\"av_has_phones\": \"false\","
                    + "\"id\": \"av_contact_1234_passport_user_52\",\n"
                    + "\"av_user_id\": \"1234\",\"av_revision\":\"3\","
                    + "\"av_user_type\": \"passport_user\",\"av_tags\":\"12\",\"av_contact_tags\":\"12\","
                    + "\"av_record_type\": \"contact\"}],\"hitsCount\": 1}";
            waitForIndex(
                cluster,
                "av_tags:12+OR+av_emails_tags:12&prefix=1234$passport_user",
                new JsonChecker(expected));
        }
    }

    //test delete contact as it comes from collie
    // untag + delete emails + delete contact
    @Test
    public void testDeleteContact() throws Exception {
        try (AceVenturaSaloCluster cluster =
                 new AceVenturaSaloCluster(this))
        {
            addOpId(
                cluster,
                0,
                new StaticHttpResource(
                    HttpStatus.SC_OK,
                    new InputStreamEntity(
                        getClass().getResourceAsStream(
                            "delete_contact.json"))));
            cluster.start();
            String expected =
                "{\"hitsArray\": [{"
                    + "\"av_cid\": \"4\","
                    + "\"av_list_id\": \"1\","
                    + "\"av_revision\": \"13\","
                    + "\"av_has_phones\": \"false\","
                    + "\"av_names\": \"Contact\\tLast\","
                    + "\"av_names_alias\": \"Contact\","
                    + "\"av_user_id\": \"723042009\","
                    + "\"av_record_type\": \"contact\","
                    + "\"av_user_type\": \"passport_user\","
                    + "\"av_vcard\": "
                    + "\"{\\\"names\\\":[{\\\"last\\\":\\\"Last\\\","
                    + "\\\"first\\\":\\\"Contact\\\"}]}\","
                    + "\"id\": \"av_contact_723042009_passport_user_4\"}],"
                    + "\"hitsCount\": 1}";

            waitForIndex(
                cluster,
                "id:av_contact_723042009_passport_user_4",
                new JsonChecker(expected));
        }
    }


    @Test
    public void testTags() throws Exception {
        try (AceVenturaSaloCluster cluster =
                 new AceVenturaSaloCluster(this))
        {
            addOpId(
                cluster,
                0,
                changeRow(
                    "1234",
                    "create_tag",
                    0,
                    "\"tag_id\":\"12\"",
                    "\"name\": \"First tag\", \"type\":\"system\"",
                    1));
            cluster.start();
            String expected =
                " {\"hitsArray\": [{\"av_tag_name\": \"First tag\",\n"
                    + "\"av_tag_id\": \"12\",\n\"av_revision\": \"1\",\n"
                    + "\"av_tag_type\": \"system\",\"av_user_id\": \"1234\","
                    + "\"av_user_type\": \"passport_user\","
                    + "\"av_record_type\": \"tag\","
                    + "\"id\": \"av_tag_1234_passport_user_12\"}],\"hitsCount\": 1}";
            waitForIndex(
                cluster,
                "id:av_tag_1234_passport_user_12",
                new JsonChecker(expected));
            expected =
                " {\"hitsArray\": [{\"av_tag_name\": \"Second tag\",\n"
                    + "\"av_tag_id\": \"12\",\n\"av_revision\": \"2\",\n"
                    + "\"av_tag_type\": \"system\",\"av_user_id\": \"1234\","
                    + "\"av_user_type\": \"passport_user\","
                    + "\"av_record_type\": \"tag\","
                    + "\"id\": \"av_tag_1234_passport_user_12\"}],\"hitsCount\": 1}";
            addOpId(
                cluster,
                1,
                changeRow(
                    "1234",
                    "update_tag",
                    1,
                    "",
                    "\"name\": \"Second tag\", \"tag_id\":\"12\"",
                    2));
            waitForIndex(
                cluster,
                "id:av_tag_1234_passport_user_12",
                new JsonChecker(expected));
            addOpId(
                cluster,
                2,
                changeRow(
                    "1234",
                    "delete_tag",
                    2,
                    "\"name\": \"Second tag\", \"type\": \"system\"",
                    "\"tag_id\": 12",
                    3));
            expected = "{\"hitsArray\": [],\"hitsCount\": 0}";
            waitForIndex(
                cluster,
                "id:av_tag_1234_passport_user_12",
                new JsonChecker(expected));
        }
    }

    protected static String changeItem(
        final String prefix,
        final String changeType,
        final long opId,
        final String changed,
        final String args,
        final long revison)
    {
        return "{\n"
            + "\"operation_id\": " + opId + ","
            + "\"user_id\": \"" + prefix + "\","
            + "\"user_type\": \"passport_user\","
            + "\"revision\": \"" + revison + "\","
            + "\"change_type\": \"" + changeType + "\","
            + "\"operation_date\": \"1551114750.860455\","
            + "\"select_date\": \"1555147693.539\","
            + "\"arguments\": {" + args + "},"
            + "\"changed\": {" + changed + "},"
            + "\"x_request_id\": \"husky:copy_abook:59781\","
            + "\"revert_change_id\": \"\","
            + "\"db_user\": \"transfer\"}";
    }

    protected static String changeRow(
        final String prefix,
        final String changeType,
        final long opId,
        final String changed,
        final String args,
        final long revison)
    {
        return changeRows(changeItem(
            prefix,
            changeType,
            opId,
            changed,
            args,
            revison));
    }

    protected static String changeRows(final String... items) {
        StringBuilder result = new StringBuilder("{\"rows\": [");
        for (int i = 0; i < items.length; i++) {
            if (i != 0) {
                result.append(",");
            }
            result.append(items[i]);
        }

        result.append("]}");
        return result.toString();
    }
}
