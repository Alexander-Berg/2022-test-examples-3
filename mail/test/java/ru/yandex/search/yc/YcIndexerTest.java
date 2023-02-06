package ru.yandex.search.yc;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.http.test.HttpAssert;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.msearch.FieldConfig;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;

public class YcIndexerTest extends TestBase {
    private static final String DEFAULT_LB_PARAMS = "&topic=topic&partition=1&offset=0&seqNo=1&message-create-time=0&message-write-time=0";

    private String loadResource(final String name) throws IOException {
        return IOStreamUtils.consume(
            new InputStreamReader(
                this.getClass().getResourceAsStream(name),
                StandardCharsets.UTF_8))
            .toString();
    }

    private void waitForIndexFullUri(
        final YcIndexerCluster cluster,
        final String uri,
        final JsonChecker checker)
        throws Exception
    {
        long enTs = System.currentTimeMillis() + 2000;
        Exception exception = null;
        String result = "Not runned";
        String output = "";
        while (System.currentTimeMillis() < enTs) {
            Thread.sleep(100);
            try {
                output = cluster.searchBackend().getSearchOutput(uri);
                result =
                    checker.check(output);
                if (result == null) {
                    return;
                }
            } catch (HttpException | IOException e) {
                exception = e;
            }
        }

        if (exception != null) {
            throw exception;
        }

        System.out.println(output);
        Assert.fail(result);
    }

    private void waitForIndex(
        final YcIndexerCluster cluster,
        final String text,
        final JsonChecker checker)
        throws Exception
    {
        waitForIndexFullUri(cluster, "/search?&text=" + text + "&get=*,-yc_mf_message_parse_error&length=100", checker);
    }

    @Test
    public void testZonedTs() throws Exception {
        ZonedDateTime deleteTs = ZonedDateTime.parse("2019-07-16T10:00:30+03:00");
        long deleteTsMicros = ChronoUnit.MICROS.between(Instant.EPOCH, deleteTs.toInstant());
        System.out.println("FIRST " + deleteTsMicros);

        deleteTs = ZonedDateTime.parse("2019-07-16T07:00:30+00:00");
        deleteTsMicros = ChronoUnit.MICROS.between(Instant.EPOCH, deleteTs.toInstant());
        System.out.println("FIRST " + deleteTsMicros);
    }

    @Test
    public void testTimestampWithZ() throws Exception {
        ZonedDateTime deleteTs = ZonedDateTime.parse("2021-02-10T13:49:31.054947Z");
        long deleteTsMicros = ChronoUnit.MICROS.between(Instant.EPOCH, deleteTs.toInstant());
        System.out.println("FIRST " + deleteTsMicros);
    }

    @Test
    public void testIndex() throws Exception {
        try (YcIndexerCluster cluster = new YcIndexerCluster(this)) {
            HttpPost add = new HttpPost(cluster.indexer().host() + "/api/yc/index?transfer_ts=1570450676326&add" + DEFAULT_LB_PARAMS);
            add.setEntity(new StringEntity("{\n" +
                "  \"deleted\": \"\",\n" +
                "  \"service\": \"managed-mongodb\",\n" +
                "  \"cloud_id\": \"foorkhlv2jt6khpv69ik\",\n" +
                "  \"folder_id\": \"mdb-junk\",\n" +
                "  \"timestamp\": \"2019-08-15T15:12:04.999834+00:00\",\n" +
                "  \"attributes\": {\n" +
                "    \"name\": \"vg-db1-nano-production-cluster-very-vip\",\n" +
                "    \"hosts\": [\n" +
                "      \"iva-o9l6shobn2md2owa.db.yandex.net\",\n" +
                "      \"myt-ssvx05ylxw6ej152.db.yandex.net\",\n" +
                "      \"sas-34uy45vvmnuejglc.db.yandex.net\"\n" +
                "    ],\n" +
                "    \"users\": [\n" +
                "      \"user1\"\n" +
                "    ],\n" +
                "    \"labels\": {},\n" +
                "    \"databases\": [\n" +
                "      \"db1\"\n" +
                "    ],\n" +
                "    \"description\": \"\"\n" +
                "  },\n" +
                "  \"permission\": \"mdb.all.read\",\n" +
                "  \"resource_id\": \"mdbo3hpj141vhabjk6rb\",\n" +
                "  \"resource_type\": \"cluster\"\n" +
                "}", StandardCharsets.UTF_8));

            cluster.start();

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, add);

            waitForIndex(
                cluster,
                "yc_doc_type:*",
                new JsonChecker(loadResource("expected_add.json")));

            HttpPost update =
                new HttpPost(cluster.indexer().host() + "/api/yc/index?&update&transfer_ts=1570450676500" + DEFAULT_LB_PARAMS);
            String updateJson = "{\n" +
                "  \"deleted\": \"\",\n" +
                "  \"service\": \"managed-mongodb\",\n" +
                "  \"cloud_id\": \"foorkhlv2jt6khpv69ik\",\n" +
                "  \"folder_id\": \"mdb-junk\",\n" +
                "  \"timestamp\": \"2019-08-15T15:12:04.999835+00:00\",\n" +
                "  \"attributes\": {\n" +
                "    \"name\": \"vg-db1-nano-production-cluster-very-vip-updated\",\n" +
                "    \"other_name\": \"other_name\",\n" +
                "    \"hosts\": [\n" +
                "      \"iva-o9l6shobn2md2owa.db.yandex.net\",\n" +
                "      \"myt-ssvx05ylxw6ej152.db.yandex.net\"" +
                "    ],\n" +
                "    \"users\": [\n" +
                "      \"user1\"\n" +
                "    ],\n" +
                "    \"labels\": {},\n" +
                "    \"description\": \"\"\n" +
                "  },\n" +
                "  \"permission\": \"mdb.vasya.read\",\n" +
                "  \"resource_id\": \"mdbo3hpj141vhabjk6rb\",\n" +
                "  \"resource_type\": \"cluster\"\n" +
                "}";

            update.setEntity(new StringEntity(updateJson, StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, update);

            waitForIndex(
                cluster,
                "yc_doc_type:*",
                new JsonChecker(loadResource("expected_update.json")));

            HttpPost delete = new HttpPost(
                cluster.indexer().host() + "/api/yc/index?transfer_ts=1570450676326&stale-delete" + DEFAULT_LB_PARAMS);
            delete.setEntity(new StringEntity("{\n" +
                "  \"deleted\": \"2019-08-15T15:12:04.999833+00:00\",\n" +
                "  \"service\": \"managed-mongodb\",\n" +
                "  \"cloud_id\": \"foorkhlv2jt6khpv69ik\",\n" +
                "  \"folder_id\": \"mdb-junk\",\n" +
                "  \"timestamp\": \"2019-08-15T15:12:04.999833+00:00\",\n" +
                "  \"attributes\": {\n" +
                "    \"name\": \"vg-db1-nano-production-cluster-very-vip\",\n" +
                "    \"hosts\": [\n" +
                "      \"iva-o9l6shobn2md2owa.db.yandex.net\",\n" +
                "      \"myt-ssvx05ylxw6ej152.db.yandex.net\",\n" +
                "      \"sas-34uy45vvmnuejglc.db.yandex.net\"\n" +
                "    ],\n" +
                "    \"users\": [\n" +
                "      \"user1\"\n" +
                "    ],\n" +
                "    \"labels\": {},\n" +
                "    \"databases\": [\n" +
                "      \"db1\"\n" +
                "    ],\n" +
                "    \"description\": \"\"\n" +
                "  },\n" +
                "  \"permission\": \"mdb.all.read\",\n" +
                "  \"resource_id\": \"mdbo3hpj141vhabjk6rb\",\n" +
                "  \"resource_type\": \"cluster\"\n" +
                "}", StandardCharsets.UTF_8));
            // check stale delete

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, delete);
            Thread.sleep(400);

            waitForIndex(
                cluster,
                "yc_doc_type:*",
                new JsonChecker(loadResource("expected_update.json")));

            delete = new HttpPost(cluster.indexer().host() + "/api/yc/index?transfer_ts=1570450676326&delete" + DEFAULT_LB_PARAMS);
            delete.setEntity(new StringEntity("{\n" +
                "  \"deleted\": \"2019-08-15T15:12:04.999837+00:00\",\n" +
                "  \"service\": \"managed-mongodb\",\n" +
                "  \"cloud_id\": \"foorkhlv2jt6khpv69ik\",\n" +
                "  \"folder_id\": \"mdb-junk\",\n" +
                "  \"timestamp\": \"2019-08-15T15:12:04.999837+00:00\",\n" +
                "  \"attributes\": {\n" +
                "    \"name\": \"vg-db1-nano-production-cluster-very-vip\",\n" +
                "    \"hosts\": [\n" +
                "      \"iva-o9l6shobn2md2owa.db.yandex.net\",\n" +
                "      \"myt-ssvx05ylxw6ej152.db.yandex.net\",\n" +
                "      \"sas-34uy45vvmnuejglc.db.yandex.net\"\n" +
                "    ],\n" +
                "    \"users\": [\n" +
                "      \"user1\"\n" +
                "    ],\n" +
                "    \"labels\": {},\n" +
                "    \"databases\": [\n" +
                "      \"db1\"\n" +
                "    ],\n" +
                "    \"description\": \"\"\n" +
                "  },\n" +
                "  \"permission\": \"mdb.all.read\",\n" +
                "  \"resource_id\": \"mdbo3hpj141vhabjk6rb\",\n" +
                "  \"resource_type\": \"cluster\"\n" +
                "}", StandardCharsets.UTF_8));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, delete);

            waitForIndex(
                cluster,
                "yc_doc_type:*+AND+NOT+yc_deleted:1",
                new JsonChecker(loadResource("expected_delete.json")));
        }
    }

    @Test
    public void testV1SchemaAndNullAttribute() throws Exception {
        try (YcIndexerCluster cluster = new YcIndexerCluster(this)) {
            cluster.start();
            HttpPost add = new HttpPost(cluster.indexer().host() + "/api/yc/index?topic=rt3.sas--yandexcloud@preprod--search-infrastructure&partition=1&offset=10709&seqNo=8133073&message-create-time=1592083505278&message-write-time=1592083513296");
            add.setEntity(new StringEntity("{\"service\":\"compute\",\"name\":\"vasya\", \"resource_type\":\"disk\"," +
                "\"resource_id\":\"fhmmj2ers6djfd69r915\",\"permission\":\"compute.disks.get\"," +
                "\"attributes\":{\"instance_ids\":\"vasyaids\",\"name\":\"disk-test-t8gk2z1bsq\",\"description\":\"\"}," +
                "\"cloud_id\":\"b1gk0l3ut03ubms4jp5q\",\"folder_id\":\"b1gha9fdta8426uol7pq\"," +
                "\"timestamp\":\"2021-04-07T03:43:58.128701+00:00\"}", StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, add);
            waitForIndexFullUri(
                cluster,
                "/search?text=(yc_doc_type:main+OR+id:*_instance_ids)+AND+NOT+yc_deleted:1&get=id,yc_name,yc_attvalue",
                new JsonChecker(
                    "{\"hitsCount\": 2, \"hitsArray\": [" +
                        "{\"id\":\"b1gk0l3ut03ubms4jp5q_compute_disk_fhmmj2ers6djfd69r915\"," +
                        " \"yc_name\": \"vasya\", \"yc_attvalue\": null}," +
                        "{\"id\":\"b1gk0l3ut03ubms4jp5q_compute_disk_fhmmj2ers6djfd69r915_instance_ids\"," +
                        "\"yc_name\": \"vasya\",\"yc_attvalue\": \"vasyaids\"}" +
                        "]}"));

            add.setEntity(new StringEntity("{\"service\":\"compute\",\"name\":\"vasya\", \"resource_type\":\"disk\"," +
                "\"resource_id\":\"fhmmj2ers6djfd69r915\",\"permission\":\"compute.disks.get\"," +
                "\"attributes\":{\"instance_ids\":null,\"name\":\"disk-test-t8gk2z1bsq\",\"description\":\"\"}," +
                "\"cloud_id\":\"b1gk0l3ut03ubms4jp5q\",\"folder_id\":\"b1gha9fdta8426uol7pq\"," +
                "\"timestamp\":\"2021-04-07T03:43:58.128702+00:00\"}", StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, add);

            waitForIndexFullUri(
                cluster,
                "/search?text=(yc_doc_type:main+OR+id:*_instance_ids)+AND+NOT+yc_deleted:1&get=id,yc_name",
                new JsonChecker(
                    "{\"hitsCount\": 1, \"hitsArray\": [" +
                        "{\"id\":\"b1gk0l3ut03ubms4jp5q_compute_disk_fhmmj2ers6djfd69r915\"," +
                        " \"yc_name\": \"vasya\"}]}"));
        }
    }

    @Ignore
    @Test
    public void testSkipComputeEmpty() throws Exception {
        try (YcIndexerCluster cluster = new YcIndexerCluster(this)) {
            cluster.start();
            HttpPost add = new HttpPost(cluster.indexer().host() + "/api/yc/index?topic=rt3.sas--yandexcloud@preprod--search-infrastructure&partition=1&offset=10709&seqNo=8133073&message-create-time=1592083505278&message-write-time=1592083513296");
            add.setEntity(new StringEntity("{\"service\":\"compute\",\"cloud_id\":\"\"," +
                "\"folder_id\":\"aoeg1t1sj8v6ni4g6vqg\",\"resource_id\":\"c8ru5nqv1lq5g1dat32m\"," +
                "\"resource_type\":\"instance\",\"permission\":\"compute.instances.get\"," +
                "\"timestamp\":\"2020-06-13T21:25:05.278252+00:00\",\"deleted\":\"2020-06-13T21:25:05.278252+00:00\"," +
                "\"attributes\":{}}", StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, add);
        }
    }

    @Test
    public void testFields() throws Exception {
        try (YcIndexerCluster cluster = new YcIndexerCluster(this)) {
            for (YcFields field: YcFields.values()) {
                if (field.hasPrefixedAlias()) {
                    FieldConfig unprefixedConfig =
                        cluster.searchBackend().lucene().defaultDatabase().config().fieldConfig(field.storeField());
                    Assert.assertNotNull("Missing in lucene config " + field.name(), unprefixedConfig);
                    Assert.assertFalse(
                        "Expected field to be unprefixed " + field.name(),
                        unprefixedConfig.prefixed());
                    Assert.assertTrue("Expected field to be stored " + field.name(), unprefixedConfig.store());

                    FieldConfig prefixedConfig =
                        cluster.searchBackend().lucene().defaultDatabase().config().fieldConfig(field.prefixedField());
                    Assert.assertNotNull("Missing in lucene config " + field.name(),prefixedConfig);
                    Assert.assertTrue("Expected field to be prefixed " + field.name(),prefixedConfig.prefixed());
                    Assert.assertFalse("Expected field to be stored " + field.name(), prefixedConfig.store());
                } else {
                    FieldConfig prefixedConfig =
                        cluster.searchBackend().lucene().defaultDatabase().config().fieldConfig(field.field());
                    Assert.assertNotNull("Missing in lucene config " + field.name(), prefixedConfig);
                    if (field == YcFields.ID) {
                        continue;
                    }
                    if (prefixedConfig.index()) {
                        Assert.assertTrue(
                            "Expected field to be prefixed " + field.name(),
                            prefixedConfig.prefixed());
                    }

                    Assert.assertTrue("Expected field to be stored " + field.name(), prefixedConfig.store());
                }
            }
        }
    }

    @Test
    public void testMarketplace() throws Exception {
        try (YcIndexerCluster cluster = new YcIndexerCluster(this)) {
            cluster.start();
            HttpPost update =
                new HttpPost(
                    cluster.indexer().host()
                        + "/api/yc/marketplace/index?&update&transfer_ts=1570450676500");
            update.setEntity(
                new StringEntity(loadResource("marketplace1.json"), StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, update);
            System.out.println(cluster.searchBackend().getSearchOutput("/search?&text=id:*&get=*&length=1000"));
            waitForIndex(
                cluster,
                "id:*",
                new JsonChecker(loadResource("marketplace_expected1.json")));
        }
    }

    @Test
    public void testIndexV2Schema() throws Exception {
        try (YcIndexerCluster cluster = new YcIndexerCluster(this)) {
            HttpPost add = new HttpPost(
                cluster.indexer().host()
                    + "/api/yc/index?transfer_ts=1570450676326&add"
                    + DEFAULT_LB_PARAMS);

            add.setEntity(
                new StringEntity(
                    loadResource("v2/search_doc1.json"),
                    StandardCharsets.UTF_8));
            cluster.start();

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, add);
            Thread.sleep(300);
            waitForIndex(
                cluster,
                "yc_doc_type:*",
                new JsonChecker(loadResource("v2/expected_after_doc1.json")));
        }
    }

    @Test
    public void testSaveMalformedDocs() throws Exception {
        try (YcIndexerCluster cluster = new YcIndexerCluster(this)) {
            HttpPost add = new HttpPost(
                cluster.indexer().host()
                    + "/api/yc/index?transfer_ts=1570450676326&add"
                    + DEFAULT_LB_PARAMS);

            add.setEntity(
                new StringEntity(
                    loadResource("v2/invalid_search_docs.json"),
                    StandardCharsets.UTF_8));
                     cluster.start();

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, add);

            // check that monitoring uuids works
            add.setEntity(
                new StringEntity(
                    "{\"service\":\"monitoring\"," +
                        "\"resource_path\":[{\"resource_type\":\"resource-manager.cloud\",\"resource_id\":\"b1gph5jh6bg8mjocuo4t\"},{\"resource_type\":\"resource-manager.folder\",\"resource_id\":\"b1gi94cgo99b9jhurpkl\"},{\"resource_type\":\"monitoring.alert\",\"resource_id\":\"2f0afe40-f0f6-4cee-b177-5f60b8985bca\"}]," +
                        "\"resource_type\":\"alert\",\"name\":\"Alert on VM CPU Usage\",\"resource_id\":\"2f0afe40-f0f6-4cee-b177-5f60b8985bca\",\"reindex_timestamp\":\"2022-01-23T09:46:39.909Z\",\"attributes\":{\"description\":\"\"},\"permission\":\"monitoring.configs.get\",\"cloud_id\":\"b1gph5jh6bg8mjocuo4t\"," +
                        "\"folder_id\":\"b1gi94cgo99b9jhurpkl\",\"timestamp\":\"2020-07-16T16:02:42Z\"}"));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, add);
            Thread.sleep(300);

            waitForIndex(
                cluster,
                "id:*&db=malformed_search_docs",
                new JsonChecker(loadResource("v2/expected_in_malformed_after_invalid.json")));
        }
    }
}
