package ru.yandex.search.yc;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.devtools.test.Paths;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.json.dom.JsonMap;
import ru.yandex.json.dom.JsonObject;
import ru.yandex.json.dom.TypesafeValueContentHandler;
import ru.yandex.json.writer.JsonType;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.search.prefix.StringPrefix;
import ru.yandex.search.yc.config.YCProxyConfigBuilder;
import ru.yandex.search.yc.proto.LabelProto;
import ru.yandex.test.util.TestBase;

public class YcSearchTest extends TestBase {
    private static final String DEFAULT_TOKEN = "token";
    private static final String EMPTY_MARKETPLACE_SEARCH_RESP =
        "{\"has_next\": false,\"total\": 0,\"documents\": []}";

    private static HttpGet searchRequest(final String uri) {
        HttpGet get = new HttpGet(uri);
        get.addHeader(YcHeaders.SUBJECT_TOKEN, DEFAULT_TOKEN);
        return get;
    }

    private static HttpGet searchRequestWithReferer(final String uri, final String referer) {
        HttpGet get = new HttpGet(uri);
        get.addHeader(YcHeaders.SUBJECT_TOKEN, DEFAULT_TOKEN);
        //get.addHeader("referer", "https://console-preprod.cloud.yandex.ru/folders/aoeq9r0qt8v1up7a0lcm/managed-mysql/cluster/e4ur0e31dols4ukmqdeu?section=hosts");
        get.addHeader("referer", referer);
        return get;
    }

    @Test
    public void testStaleSearch() throws Exception {
        try (YcSearchProxyCluster cluster = new YcSearchProxyCluster(this);
             YcIndexerCluster indexerCluster = new YcIndexerCluster(this, cluster.searchBackend());
             CloseableHttpClient client = HttpClients.createDefault();) {
            HttpPost add =
                new HttpPost(
                    indexerCluster.indexer().host()
                        + "/api/yc/index?transfer_ts=1570450676326&add"
                        + YcIndexerCluster.DEFAULT_LB_PARAMS);
            add.setEntity(
                new StringEntity(
                    loadResource("v2/index_docs_1.json"),
                    StandardCharsets.UTF_8));

            indexerCluster.start();
            cluster.iamService().supplier(IamAllAllowHandler.INSTANCE);

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, add);

            String baseUri = cluster.proxy().host() + "/api/search?";

            StringPrefix prefix =
                new StringPrefix(YcFields.buildPrefix("aoenio4bf5tauqgr4tj4"));
            cluster.addStatus(prefix);

            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=aoenio4bf5tauqgr4tj4&request=a"),
                loadResource("v2/expected_search_1.json"));

            cluster.searchBackend().update(
                new StringPrefix("aoenio4bf5tauqgr4tj4"),
                "\"id\":\"aoenio4bf5tauqgr4tj4_dns_zone_aet02kphp9qre1g2k76a\", \"yc_marked_stale_ts\": 10",
                "\"id\":\"aoenio4bf5tauqgr4tj4_dns_zone_aet02kphp9qre1g2k76a_zone\", \"yc_marked_stale_ts\": 10");
            cluster.searchBackend().flush();
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=aoenio4bf5tauqgr4tj4&request=a"),
                "{\"results\": []}");
        }
    }

    @Test
    public void testSearch() throws Exception {
        try (YcSearchProxyCluster cluster = new YcSearchProxyCluster(this);
             CloseableHttpClient client = HttpClients.createDefault()) {
            StringPrefix prefix =
                    new StringPrefix(YcFields.buildPrefix("foodnh1gbffklfion4rb"));

            cluster.addStatus(prefix);
            cluster.iamService().supplier(IamAllAllowHandler.INSTANCE);

            String mainDoc = "\"id\": \"foodnh1gbffklfion4rb_managed-redis_cluster_mdbuftkk264dqpm4oo14\",\n" +
                "\"yc_attributes\": \"{\\\"hosts\\\":[\\\"iva-bs4f2nar3m02duh2.db.yandex.net\\\"," +
                "\\\"myt-ow5uw04z3lcb0ohe.db.yandex.net\\\",\\\"sas-ezxquljvyhlpera8.db.yandex.net\\\"]," +
                "\\\"name\\\":\\\"dbaas_e2e_porto_qa\\\",\\\"description\\\":\\\"\\\",\\\"labels\\\":{}}\",\n" +
                "\"yc_doc_type\": \"main\",\n" +
                "\"yc_service\": \"managed-redis\",\n" +
                "\"yc_cloud_id\": \"foodnh1gbffklfion4rb\",\n" +
                "\"yc_folder_id\": \"foorv7rnqd9sfo4q6db4\",\n" +
                "\"yc_permission\": \"mdb.all.read\",\n" +
                "\"yc_resource_id\": \"mdbuftkk264dqpm4oo14\",\n" +
                "\"yc_resource_type\": \"cluster\",\n" +
                "\"yc_timestamp_str\": \"2019-10-08T07:51:48.080691+03:00\",\n" +
                "\"yc_timestamp\": 1570510308080691,\n" +
                "\"yc_transfer_ts\": 1570511554131";

            String descDoc =
                " \"id\": \"foodnh1gbffklfion4rb_managed-redis_cluster_mdbuftkk264dqpm4oo14_description\"," +
                    "\"yc_service\": \"managed-redis\",\n" +
                    "\"yc_cloud_id\": \"foodnh1gbffklfion4rb\",\n" +
                    "\"yc_folder_id\": \"foorv7rnqd9sfo4q6db4\",\n" +
                    "\"yc_permission\": \"mdb.all.read\",\n" +
                    "\"yc_resource_id\": \"mdbuftkk264dqpm4oo14\",\n" +
                    "\"yc_resource_type\": \"cluster\",\n" +
                    "\"yc_timestamp_str\": \"2019-10-08T07:51:48.080691+03:00\",\n" +
                    "\"yc_timestamp\": 1570510308080691,\n" +
                    "\"yc_transfer_ts\": 1570511554131,\n" +
                    "\"yc_main_doc_id\": \"foodnh1gbffklfion4rb_managed-redis_cluster_mdbuftkk264dqpm4oo14\"," +
                    "\n" +
                    "\"yc_doc_type\": \"attributes\",\n" +
                    "\"yc_atttype\": \"simple\",\n" +
                    "\"yc_attname\": \"description\",\n" +
                    "\"yc_attvalue\": \"porto is not Portable\"";
            String nameAttDoc =
                "\"id\": \"foodnh1gbffklfion4rb_managed-redis_cluster_mdbuftkk264dqpm4oo14_name\",\n" +
                    "\"yc_service\": \"managed-redis\",\n" +
                    "\"yc_cloud_id\": \"foodnh1gbffklfion4rb\",\n" +
                    "\"yc_folder_id\": \"foorv7rnqd9sfo4q6db4\",\n" +
                    "\"yc_permission\": \"mdb.all.read\",\n" +
                    "\"yc_resource_id\": \"mdbuftkk264dqpm4oo14\",\n" +
                    "\"yc_resource_type\": \"cluster\",\n" +
                    "\"yc_timestamp_str\": \"2019-10-08T07:51:48.080691+03:00\",\n" +
                    "\"yc_timestamp\": 1570510308080691,\n" +
                    "\"yc_transfer_ts\": 1570511554131,\n" +
                    "\"yc_main_doc_id\": " +
                    "\"foodnh1gbffklfion4rb_managed-redis_cluster_mdbuftkk264dqpm4oo14\",\n" +
                    "\"yc_doc_type\": \"attributes\",\n" +
                    "\"yc_atttype\": \"simple\",\n" +
                    "\"yc_attname\": \"name\",\n" +
                    "\"yc_attvalue\": \"dbaas_e2e_porto_qa\"\n";

            String hostsAttDoc =
                "\"id\": \"foodnh1gbffklfion4rb_managed-redis_cluster_mdbuftkk264dqpm4oo14_hosts\",\n" +
                    "\"yc_service\": \"managed-redis\",\n" +
                    "\"yc_cloud_id\": \"foodnh1gbffklfion4rb\",\n" +
                    "\"yc_folder_id\": \"foorv7rnqd9sfo4q6db4\",\n" +
                    "\"yc_permission\": \"mdb.all.read\",\n" +
                    "\"yc_resource_id\": \"mdbuftkk264dqpm4oo14\",\n" +
                    "\"yc_resource_type\": \"cluster\",\n" +
                    "\"yc_timestamp_str\": \"2019-10-08T07:51:48.080691+03:00\",\n" +
                    "\"yc_timestamp\": 1570510308080691,\n" +
                    "\"yc_transfer_ts\": 1570511554131,\n" +
                    "\"yc_main_doc_id\": " +
                    "\"foodnh1gbffklfion4rb_managed-redis_cluster_mdbuftkk264dqpm4oo14\",\n" +
                    "\"yc_doc_type\": \"attributes\",\n" +
                    "\"yc_atttype\": \"simple\",\n" +
                    "\"yc_attname\": \"hosts\",\n" +
                    "\"yc_attvalue\": \"iva-bs4f2nar3m02duh2.db.yandex.net\\nmyt-ow5uw04z3lcb0ohe.db" +
                    ".yandex.net\\nsas-ezxquljvyhlpera8.db.yandex.net\"";
            cluster.searchBackend().add(prefix, mainDoc, descDoc, hostsAttDoc, nameAttDoc);

            String baseUri = cluster.proxy().host() + "/api/search?&cloud_id=foodnh1gbffklfion4rb&highlight=false";

            String expectedItem1 =
                "    {\n" +
                    "      \"service\": \"managed-redis\",\n" +
                    "      \"permission\": \"mdb.all.read\",\n" +
                    "      \"attributes\": {\n" +
                    "        \"hosts\": [\n" +
                    "          \"iva-bs4f2nar3m02duh2.db.yandex.net\",\n" +
                    "          \"myt-ow5uw04z3lcb0ohe.db.yandex.net\",\n" +
                    "          \"sas-ezxquljvyhlpera8.db.yandex.net\"\n" +
                    "        ],\n" +
                    "        \"name\": \"dbaas_e2e_porto_qa\",\n" +
                    "        \"description\": \"\",\n" +
                    "        \"labels\": {}\n" +
                    "      },\n" +
                    "      \"resource_type\": \"cluster\",\n" +
                    "      \"folder_id\": \"foorv7rnqd9sfo4q6db4\",\n" +
                    "      \"timestamp\": \"1570510308080691\",\n" +
                    "      \"resource_id\": \"mdbuftkk264dqpm4oo14\",\n" +
                    "      \"cloud_id\": \"foodnh1gbffklfion4rb\",\n" +
                    "      \"timestamp\": \"2019-10-08T07:51:48.080691+03:00\"" +
                    "    }\n";
            String expected1 = "{\n" +
                "  \"results\": [\n" +
                expectedItem1 +
                "  ]\n" +
                "}";
            HttpAssert.assertJsonResponse(client,
                    searchRequest(baseUri + "&request=port&debug=true&folder_id=foorv7rnqd9sfo4q6db4"), expected1);
            HttpAssert.assertJsonResponse(client,
                    searchRequest(baseUri + "&request=port&debug=true&folder_id=foorv7rnqd9sfo4q6db5"), expected1);
            HttpAssert.assertJsonResponse(client, searchRequest(baseUri + "&request=port&debug=true"), expected1);
            HttpAssert.assertJsonResponse(client, searchRequest(baseUri + "&request=e2e"), expected1);
            HttpAssert.assertJsonResponse(client, searchRequest(baseUri + "&request=Portable"), expected1);

            String otherCloudDoc =
                "\"id\": \"othercluster_managed-redis_cluster_mdbuftkk264dqpm4oo14\",\n" +
                    "\"yc_attributes\": \"{\\\"hosts\\\":[\\\"iva-bs4f2nar3m02duh2.db.yandex.net\\\"," +
                    "\\\"myt-ow5uw04z3lcb0ohe.db.yandex.net\\\",\\\"sas-ezxquljvyhlpera8.db.yandex.net\\\"]," +
                    "\\\"name\\\":\\\"dbaas_e2e_porto_other\\\",\\\"description\\\":\\\"\\\",\\\"labels\\\":{}}\",\n" +
                    "\"yc_doc_type\": \"main\",\n" +
                    "\"yc_service\": \"managed-redis\",\n" +
                    "\"yc_cloud_id\": \"othercluster\",\n" +
                    "\"yc_folder_id\": \"foorv7rnqd9sfo4q6db4\",\n" +
                    "\"yc_permission\": \"mdb.all.read\",\n" +
                    "\"yc_resource_id\": \"mdbuftkk264dqpm4oo14\",\n" +
                    "\"yc_resource_type\": \"cluster\",\n" +
                    "\"yc_timestamp_str\": \"2019-10-08T07:51:48.080691+03:00\",\n" +
                    "\"yc_timestamp\": 1570510308080691,\n" +
                    "\"yc_transfer_ts\": 1570511554131";

            String otherNameAttDoc =
                "\"id\": \"othercluster_managed-redis_cluster_mdbuftkk264dqpm4oo14_name\",\n" +
                    "\"yc_service\": \"managed-redis\",\n" +
                    "\"yc_cloud_id\": \"othercluster\",\n" +
                    "\"yc_folder_id\": \"foorv7rnqd9sfo4q6db4\",\n" +
                    "\"yc_permission\": \"mdb.all.read\",\n" +
                    "\"yc_resource_id\": \"mdbuftkk264dqpm4oo14\",\n" +
                    "\"yc_resource_type\": \"cluster\",\n" +
                    "\"yc_timestamp_str\": \"2019-10-08T07:51:48.080691+03:00\",\n" +
                    "\"yc_timestamp\": 1570510308080691,\n" +
                    "\"yc_transfer_ts\": 1570511554131,\n" +
                    "\"yc_main_doc_id\": " +
                    "\"othercluster_managed-redis_cluster_mdbuftkk264dqpm4oo14\",\n" +
                    "\"yc_doc_type\": \"attributes\",\n" +
                    "\"yc_atttype\": \"simple\",\n" +
                    "\"yc_attname\": \"name\",\n" +
                    "\"yc_attvalue\": \"dbaas_e2e_porto_other\"\n";

            StringPrefix otherPrefix =
                    new StringPrefix(YcFields.buildPrefix("othercluster"));
            cluster.addStatus(otherPrefix);
            cluster.searchBackend().add(otherPrefix, otherCloudDoc, otherNameAttDoc);

            baseUri = cluster.proxy().host() + "/api/search?highlight=false";

            String expectedItem2 =
                "\n" +
                    "{\n" +
                    "            \"attributes\": {\n" +
                    "                \"description\": \"\",\n" +
                    "                \"hosts\": [\n" +
                    "                    \"iva-bs4f2nar3m02duh2.db.yandex.net\",\n" +
                    "                    \"myt-ow5uw04z3lcb0ohe.db.yandex.net\",\n" +
                    "                    \"sas-ezxquljvyhlpera8.db.yandex.net\"\n" +
                    "                ],\n" +
                    "                \"labels\": {\n" +
                    "                },\n" +
                    "                \"name\": \"dbaas_e2e_porto_other\"\n" +
                    "            },\n" +
                    "            \"cloud_id\": \"othercluster\",\n" +
                    "            \"folder_id\": \"foorv7rnqd9sfo4q6db4\",\n" +
                    "            \"permission\": \"mdb.all.read\",\n" +
                    "            \"resource_id\": \"mdbuftkk264dqpm4oo14\",\n" +
                    "            \"resource_type\": \"cluster\",\n" +
                    "            \"service\": \"managed-redis\",\n" +
                    "            \"timestamp\": \"2019-10-08T07:51:48.080691+03:00\"\n" +
                    "        }";
            String expected2 = "{\n" +
                "  \"results\": [\n" +
                expectedItem2 + ',' +
                expectedItem1 +
                "  ]\n" +
                "}";
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_id=foodnh1gbffklfion4rb&cloud_id=othercluster&request=e2e"),
                expected2);

            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=foodnh1gbffklfion4rb,othercluster&request=e2e"),
                expected2);

            String secondFolderCloudDoc =
                "\"id\": \"oneMoreCluster_managed-redis_cluster_mdbuftkk264dqpm4oo15\",\n" +
                        "\"yc_attributes\": \"{\\\"hosts\\\":[\\\"iva-bs4f2nar3m02duh2.db.yandex.net\\\"," +
                        "\\\"myt-ow5uw04z3lcb0ohe.db.yandex.net\\\",\\\"sas-ezxquljvyhlpera8.db.yandex.net\\\"]," +
                        "\\\"name\\\":\\\"dbaas_e2e_porto_other\\\",\\\"description\\\":\\\"\\\",\\\"labels\\\":{}}\",\n" +
                        "\"yc_doc_type\": \"main\",\n" +
                        "\"yc_service\": \"managed-redis\",\n" +
                        "\"yc_cloud_id\": \"oneMoreCluster\",\n" +
                        "\"yc_folder_id\": \"secondFolder\",\n" +
                        "\"yc_permission\": \"mdb.all.read\",\n" +
                        "\"yc_resource_id\": \"mdbuftkk264dqpm4oo15\",\n" +
                        "\"yc_resource_type\": \"cluster\",\n" +
                        "\"yc_timestamp_str\": \"2019-10-08T07:51:48.080691+03:00\",\n" +
                        "\"yc_timestamp\": 1570510308080691,\n" +
                        "\"yc_transfer_ts\": 1570511554131";

            String secondFolderNameAttDoc =
                "\"id\": \"oneMoreCluster_managed-redis_cluster_mdbuftkk264dqpm4oo15_name\",\n" +
                        "\"yc_service\": \"managed-redis\",\n" +
                        "\"yc_cloud_id\": \"oneMoreCluster\",\n" +
                        "\"yc_folder_id\": \"secondFolder\",\n" +
                        "\"yc_permission\": \"mdb.all.read\",\n" +
                        "\"yc_resource_id\": \"mdbuftkk264dqpm4oo15\",\n" +
                        "\"yc_resource_type\": \"cluster\",\n" +
                        "\"yc_timestamp_str\": \"2019-10-08T07:51:48.080691+03:00\",\n" +
                        "\"yc_timestamp\": 1570510308080691,\n" +
                        "\"yc_transfer_ts\": 1570511554131,\n" +
                        "\"yc_main_doc_id\": " +
                        "\"othercluster_managed-redis_cluster_mdbuftkk264dqpm4oo15\",\n" +
                        "\"yc_doc_type\": \"attributes\",\n" +
                        "\"yc_atttype\": \"simple\",\n" +
                        "\"yc_attname\": \"name\",\n" +
                        "\"yc_attvalue\": \"dbaas_e2e_porto_other\"\n";

            String thirdFolderCloudDoc =
                "\"id\": \"oneMoreCluster_managed-redis_cluster_mdbuftkk264dqpm4oo16\",\n" +
                        "\"yc_attributes\": \"{\\\"hosts\\\":[\\\"iva-bs4f2nar3m02duh2.db.yandex.net\\\"," +
                        "\\\"myt-ow5uw04z3lcb0ohe.db.yandex.net\\\",\\\"sas-ezxquljvyhlpera8.db.yandex.net\\\"]," +
                        "\\\"name\\\":\\\"dbaas_e2e_porto_other\\\",\\\"description\\\":\\\"\\\",\\\"labels\\\":{}}\",\n" +
                        "\"yc_doc_type\": \"main\",\n" +
                        "\"yc_service\": \"managed-redis\",\n" +
                        "\"yc_cloud_id\": \"oneMoreCluster\",\n" +
                        "\"yc_folder_id\": \"thirdFolder\",\n" +
                        "\"yc_permission\": \"mdb.all.read\",\n" +
                        "\"yc_resource_id\": \"mdbuftkk264dqpm4oo16\",\n" +
                        "\"yc_resource_type\": \"cluster\",\n" +
                        "\"yc_timestamp_str\": \"2019-10-08T07:51:48.080691+03:00\",\n" +
                        "\"yc_timestamp\": 1570510308080691,\n" +
                        "\"yc_transfer_ts\": 1570511554131";

            String thirdFolderNameAttDoc =
                "\"id\": \"oneMoreCluster_managed-redis_cluster_mdbuftkk264dqpm4oo16_name\",\n" +
                        "\"yc_service\": \"managed-redis\",\n" +
                        "\"yc_cloud_id\": \"oneMoreCluster\",\n" +
                        "\"yc_folder_id\": \"thirdFolder\",\n" +
                        "\"yc_permission\": \"mdb.all.read\",\n" +
                        "\"yc_resource_id\": \"mdbuftkk264dqpm4oo16\",\n" +
                        "\"yc_resource_type\": \"cluster\",\n" +
                        "\"yc_timestamp_str\": \"2019-10-08T07:51:48.080691+03:00\",\n" +
                        "\"yc_timestamp\": 1570510308080691,\n" +
                        "\"yc_transfer_ts\": 1570511554131,\n" +
                        "\"yc_main_doc_id\": " +
                        "\"othercluster_managed-redis_cluster_mdbuftkk264dqpm4oo16\",\n" +
                        "\"yc_doc_type\": \"attributes\",\n" +
                        "\"yc_atttype\": \"simple\",\n" +
                        "\"yc_attname\": \"name\",\n" +
                        "\"yc_attvalue\": \"dbaas_e2e_porto_other\"\n";

            String fourthFolderCloudDoc =
                "\"id\": \"oneMoreCluster_managed-redis_cluster_mdbuftkk264dqpm4oo17\",\n" +
                        "\"yc_attributes\": \"{\\\"hosts\\\":[\\\"iva-bs4f2nar3m02duh2.db.yandex.net\\\"," +
                        "\\\"myt-ow5uw04z3lcb0ohe.db.yandex.net\\\",\\\"sas-ezxquljvyhlpera8.db.yandex.net\\\"]," +
                        "\\\"name\\\":\\\"dbaas_e2e_porto_other\\\",\\\"description\\\":\\\"\\\",\\\"labels\\\":{}}\",\n" +
                        "\"yc_doc_type\": \"main\",\n" +
                        "\"yc_service\": \"managed-redis\",\n" +
                        "\"yc_cloud_id\": \"oneMoreCluster\",\n" +
                        "\"yc_folder_id\": \"fourthFolder\",\n" +
                        "\"yc_permission\": \"mdb.all.read\",\n" +
                        "\"yc_resource_id\": \"mdbuftkk264dqpm4oo17\",\n" +
                        "\"yc_resource_type\": \"cluster\",\n" +
                        "\"yc_timestamp_str\": \"2019-10-08T07:51:48.080691+03:00\",\n" +
                        "\"yc_timestamp\": 1570510308080691,\n" +
                        "\"yc_transfer_ts\": 1570511554131";

            String fourthFolderNameAttDoc =
                "\"id\": \"oneMoreCluster_managed-redis_cluster_mdbuftkk264dqpm4oo17_name\",\n" +
                        "\"yc_service\": \"managed-redis\",\n" +
                        "\"yc_cloud_id\": \"oneMoreCluster\",\n" +
                        "\"yc_folder_id\": \"fourthFolder\",\n" +
                        "\"yc_permission\": \"mdb.all.read\",\n" +
                        "\"yc_resource_id\": \"mdbuftkk264dqpm4oo17\",\n" +
                        "\"yc_resource_type\": \"cluster\",\n" +
                        "\"yc_timestamp_str\": \"2019-10-08T07:51:48.080691+03:00\",\n" +
                        "\"yc_timestamp\": 1570510308080691,\n" +
                        "\"yc_transfer_ts\": 1570511554131,\n" +
                        "\"yc_main_doc_id\": " +
                        "\"othercluster_managed-redis_cluster_mdbuftkk264dqpm4oo17\",\n" +
                        "\"yc_doc_type\": \"attributes\",\n" +
                        "\"yc_atttype\": \"simple\",\n" +
                        "\"yc_attname\": \"name\",\n" +
                        "\"yc_attvalue\": \"dbaas_e2e_porto_other\"\n";

            StringPrefix oneMorePrefix =
                new StringPrefix(YcFields.buildPrefix("oneMoreCluster"));
            cluster.addStatus(oneMorePrefix);
            cluster.searchBackend().add(oneMorePrefix, secondFolderCloudDoc, secondFolderNameAttDoc,
                thirdFolderCloudDoc, thirdFolderNameAttDoc, fourthFolderCloudDoc, fourthFolderNameAttDoc);

            String expectedItem3 =
                "{\n" +
                    "\"cloud_id\": \"oneMoreCluster\",\n" +
                    "\"folder_id\": \"secondFolder\",\n" +
                    "\"permission\": \"mdb.all.read\",\n" +
                    "\"resource_id\": \"mdbuftkk264dqpm4oo15\",\n" +
                    "\"resource_type\": \"cluster\",\n" +
                    " \"service\": \"managed-redis\",\n" +
                    "\"timestamp\": \"2019-10-08T07:51:48.080691+03:00\"\n" +
                "}\n";

            String expectedItem4 =
                "{\n" +
                    "\"cloud_id\": \"oneMoreCluster\",\n" +
                    "\"folder_id\": \"thirdFolder\",\n" +
                    "\"permission\": \"mdb.all.read\",\n" +
                    "\"resource_id\": \"mdbuftkk264dqpm4oo16\",\n" +
                    "\"resource_type\": \"cluster\",\n" +
                    " \"service\": \"managed-redis\",\n" +
                    "\"timestamp\": \"2019-10-08T07:51:48.080691+03:00\"\n" +
                "}\n";

            String expectedItem5 =
                "{\n" +
                    "\"cloud_id\": \"oneMoreCluster\",\n" +
                    "\"folder_id\": \"fourthFolder\",\n" +
                    "\"permission\": \"mdb.all.read\",\n" +
                    "\"resource_id\": \"mdbuftkk264dqpm4oo17\",\n" +
                    "\"resource_type\": \"cluster\",\n" +
                    " \"service\": \"managed-redis\",\n" +
                    "\"timestamp\": \"2019-10-08T07:51:48.080691+03:00\"\n" +
                    "}\n";

            String expected3 = "{\n" +
                "  \"results\": [\n" +
                expectedItem3 +
                ",\n" +
                expectedItem4 +
                ",\n" +
                expectedItem5 +
                "  ]\n" +
                "}";

            String expected4 = "{\n" +
                "  \"results\": [\n" +
                expectedItem4 +
                ",\n" +
                expectedItem3 +
                ",\n" +
                expectedItem5 +
                "  ]\n" +
                "}";

            String expected5 = "{\n" +
                "  \"results\": [\n" +
                expectedItem5 +
                ",\n" +
                expectedItem3 +
                ",\n" +
                expectedItem4 +
                "  ]\n" +
                "}";

            String expected6 = "{\n" +
                "  \"results\": [\n" +
                expectedItem2 +
                ",\n" +
                expectedItem1 +
                ",\n" +
                expectedItem3 +
                ",\n" +
                expectedItem4+
                ",\n" +
                expectedItem5 +
                "  ]\n" +
                "}";

            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri +
                        "&cloud_id=oneMoreCluster&request=e2e&folder_id=secondFolder"),
                expected3);
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri +
                        "&cloud_id=oneMoreCluster&request=e2e&folder_id=thirdFolder"),
                expected4);
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri +
                        "&cloud_id=oneMoreCluster&request=e2e&folder_id=fourthFolder"),
                expected5);
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri +
                        "&cloud_id=oneMoreCluster&request=e2e&folder_id=nonExistingFolder"),
                expected3);
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri +
                        "&cloud_ids=oneMoreCluster,othercluster,foodnh1gbffklfion4rb&request=e2e&folder_id=foorv7rnqd9sfo4q6db4"),
                expected6);

            HttpAssert.assertJsonResponse(
                client,
                searchRequestWithReferer(baseUri +
                                "&cloud_id=oneMoreCluster&request=e2e",
                    "https://console-preprod.cloud.yandex.ru/folders/secondFolder/" +
                        "managed-mysql/cluster/e4ur0e31dols4ukmqdeu?section=hosts"),
                expected3);
            HttpAssert.assertJsonResponse(
                client,
                searchRequestWithReferer(baseUri +
                                "&cloud_id=oneMoreCluster&request=e2e",
                    "https://console.cloud.yandex.ru/folders/thirdFolder/" +
                        "managed-mysql/cluster/e4ur0e31dols4ukmqdeu?section=hosts"),
                expected4);
            HttpAssert.assertJsonResponse(
                client,
                searchRequestWithReferer(baseUri +
                                "&cloud_id=oneMoreCluster&request=e2e",
                        "https://console.cloud.yandex.ru/folders/fourthFolder?section=dashboard"),
                expected5);
            HttpAssert.assertJsonResponse(
                client,
                searchRequestWithReferer(baseUri +
                                "&cloud_id=oneMoreCluster&request=e2e",
                        "https://console.cloud.yandex.ru/folders/nonExistingFolder?section=dashboard"),
                expected3);
            HttpAssert.assertJsonResponse(
                client,
                searchRequestWithReferer(baseUri +
                                "&cloud_ids=oneMoreCluster,othercluster,foodnh1gbffklfion4rb&request=e2e",
                        "https://console-preprod.cloud.yandex.ru/folders/foorv7rnqd9sfo4q6db4?section=dashboard"),
                expected6);
        }
    }

    @Test
    public void testV2SchemaSearch() throws Exception {
        try (YcSearchProxyCluster cluster = new YcSearchProxyCluster(this);
             YcIndexerCluster indexerCluster = new YcIndexerCluster(this, cluster.searchBackend());
             CloseableHttpClient client = HttpClients.createDefault();) {
            HttpPost add =
                new HttpPost(
                    indexerCluster.indexer().host()
                        + "/api/yc/index?transfer_ts=1570450676326&add"
                        + YcIndexerCluster.DEFAULT_LB_PARAMS);
            add.setEntity(
                new StringEntity(
                    loadResource("v2/index_docs_1.json"),
                    StandardCharsets.UTF_8));

            indexerCluster.start();
            cluster.iamService().supplier(IamAllAllowHandler.INSTANCE);

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, add);

            String baseUri = cluster.proxy().host() + "/api/search?";

            StringPrefix prefix =
                new StringPrefix(YcFields.buildPrefix("aoenio4bf5tauqgr4tj4"));
            cluster.addStatus(prefix);

            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=aoenio4bf5tauqgr4tj4&request=a"),
                loadResource("v2/expected_search_1.json"));

            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=aoenio4bf5tauqgr4tj4&request=zon"),
                loadResource("v2/expected_search_2.json"));
        }
    }

    @Test
    public void testSearchHighlight() throws Exception {
        try (YcSearchProxyCluster cluster = new YcSearchProxyCluster(this);
             YcIndexerCluster indexerCluster = new YcIndexerCluster(this, cluster.searchBackend());
             CloseableHttpClient client = HttpClients.createDefault();)
        {
            HttpPost add =
                    new HttpPost(
                        indexerCluster.indexer().host()
                            + "/api/yc/index?transfer_ts=1570450676326&add"
                            + YcIndexerCluster.DEFAULT_LB_PARAMS);
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
                    "    \"labels\": {\"project\":\"maildb\"},\n" +
                    "    \"databases\": [\n" +
                    "      \"db1\"\n" +
                    "    ],\n" +
                    "    \"description\": \"\"\n" +
                    "  },\n" +
                    "  \"permission\": \"mdb.all.read\",\n" +
                    "  \"resource_id\": \"mdbo3hpj141vhabjk6rb\",\n" +
                    "  \"resource_type\": \"cluster\"\n" +
                    "}", StandardCharsets.UTF_8));

            indexerCluster.start();
            cluster.iamService().supplier(IamAllAllowHandler.INSTANCE);

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, add);

            String baseUri = cluster.proxy().host() + "/api/search?";

            StringPrefix prefix =
                    new StringPrefix(YcFields.buildPrefix("foorkhlv2jt6khpv69ik"));
            cluster.addStatus(prefix);

            String baseExpected = "{\n" +
                    "  \"results\": [\n" +
                    "    {\n" +
                    "      \"service\": \"managed-mongodb\",\n" +
                    "      \"permission\": \"mdb.all.read\",\n" +
                    "      \"attributes\": {\n" +
                    "        \"databases\": [\n" +
                    "          \"db1\"\n" +
                    "        ],\n" +
                    "        \"hosts\": [\n" +
                    "          \"iva-o9l6shobn2md2owa.db.yandex.net\",\n" +
                    "          \"myt-ssvx05ylxw6ej152.db.yandex.net\",\n" +
                    "          \"sas-34uy45vvmnuejglc.db.yandex.net\"\n" +
                    "        ],\n" +
                    "        \"name\": \"vg-db1-nano-production-cluster-very-vip\",\n" +
                    "        \"description\": \"\",\n" +
                    "        \"users\": [\n" +
                    "          \"user1\"\n" +
                    "        ],\n" +
                    "        \"labels\": {\"project\":\"maildb\"}\n" +
                    "      },\n" +
                    "      \"resource_type\": \"cluster\",\n" +
                    "      \"folder_id\": \"mdb-junk\",\n" +
                    "      \"resource_id\": \"mdbo3hpj141vhabjk6rb\",\n" +
                    "      \"cloud_id\": \"foorkhlv2jt6khpv69ik\",\n" +
                    "      \"timestamp\": \"2019-08-15T15:12:04.999834+00:00\"" +
                    "HIGHLIGHTS" +
                    "    }\n" +
                    "  ]\n" +
                    "}";
            String expected1 =
                baseExpected.replace(
                    "HIGHLIGHTS",
                    ",\"search_highlights\":{\"attributes\": {\"hosts.1\": [[0,3]]}}, " +
                        "\"attributes_highlights\": {\"hosts.1\": [[0,3]]}");
            HttpAssert.assertJsonResponse(
                    client,
                searchRequest(baseUri + "&cloud_ids=foorkhlv2jt6khpv69ik&request=myt"),
                    expected1);

            String expected2 =
                baseExpected.replace(
                    "HIGHLIGHTS",
                    ",\"search_highlights\":{\"attributes\": {\"name\": [[7,11]]}}," +
                        "\"attributes_highlights\": {\"name\": [[7,11]]}");
            HttpAssert.assertJsonResponse(
                    client,
                searchRequest(baseUri + "&cloud_ids=foorkhlv2jt6khpv69ik&request=nano"),
                    expected2);

            String expected3 =
                baseExpected.replace(
                    "HIGHLIGHTS",
                    ",\"search_highlights\":{\"attributes\": {\"labels.project\": [[0,6]]}}," +
                        "\"attributes_highlights\": {\"labels.project\": [[0,6]]}");
            HttpAssert.assertJsonResponse(
                    client,
                    searchRequest(baseUri + "&cloud_ids=foorkhlv2jt6khpv69ik&request=maildb"),
                    expected3);
            String expected4 =
                baseExpected.replace(
                    "HIGHLIGHTS",
                    ",\"search_highlights\":{\"attributes\":" +
                        " {\"databases.0\":[[0,2]]," +
                        "\"hosts.0\":[[21,23]]," +
//                        "\"hosts.1\":[[21,23]]," +
//                        "\"hosts.2\":[[21,23]]," +
                        "\"name\":[[3,5]]}" + "}, " +
                        "\"attributes_highlights\":" +
                        " {\"databases.0\":[[0,2]]," +
                        "\"hosts.0\":[[21,23]]," +
//                        "\"hosts.1\":[[21,23]]," +
//                        "\"hosts.2\":[[21,23]]," +
                        "\"name\":[[3,5]]}");
            HttpAssert.assertJsonResponse(
                    client,
                    searchRequest(baseUri + "&cloud_ids=foorkhlv2jt6khpv69ik&request=db"),
                    expected4);

            StringPrefix prefixBigAttributesList =
                new StringPrefix(YcFields.buildPrefix("aoee4gvsepbo0ah4i2j6"));
            cluster.addStatus(prefixBigAttributesList);

            add = new HttpPost(
                    indexerCluster.indexer().host()
                        + "/api/yc/index?transfer_ts=1570450676326&add"
                        + YcIndexerCluster.DEFAULT_LB_PARAMS);

            add.setEntity(new FileEntity(
                new File(Paths.getSandboxResourcesRoot() + "/big_attribute_list_doc.json")));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, add);

            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=aoee4gvsepbo0ah4i2j6&request=hj"),
                Files.readString(Path.of(Paths.getSandboxResourcesRoot() + "/expected_big_attributes_list.json"),
                    StandardCharsets.UTF_8));

//            int BIG_DOCS_COUNT = 100;
//            for (int i = 0; i < BIG_DOCS_COUNT; i++) {
//                add.setEntity(new StringEntity(
//                    loadResource("v2/big_attribute_list_doc.json")
//                        .replace("e4umim6f3jr14ois49i9", "e4umim6f3jr14ois49i9" + i),
//                    StandardCharsets.UTF_8));
//                HttpAssert.assertStatusCode(HttpStatus.SC_OK, add);
//            }
//
//            Instant start = Instant.now();
//            client.execute(searchRequest(baseUri + "&cloud_ids=aoee4gvsepbo0ah4i2j6&request=hj&length=100"));
//            Instant finish = Instant.now();
//            long elapsed = Duration.between(start, finish).toMillis();
//            try (BufferedWriter writer = new BufferedWriter(new FileWriter("merged_ignored_no_morpho_list.txt", true))) {
//                writer.write(elapsed + "\n");
//            }
        }
    }

    @Test
    public void testPermissions() throws Exception {
        try (YcSearchProxyCluster cluster = new YcSearchProxyCluster(this);
             YcIndexerCluster indexerCluster =
                 new YcIndexerCluster(this, cluster.searchBackend());
             CloseableHttpClient client = HttpClients.createDefault();)
        {
            indexerCluster.start();

            JsonMap doc1 = indexerCluster.addDoc(
                YcIndexerCluster.doc("resource1", "cloud1", "folder1", "", ""));
            indexerCluster.searchBackend().flush();

            String baseUri = cluster.proxy().host() + "/api/search?highlight=false";
            StringPrefix prefix =
                new StringPrefix(YcFields.buildPrefix("cloud1"));
            cluster.addStatus(prefix);

            // iam respond with not implemented
            HttpAssert.assertStatusCode(
                HttpStatus.SC_BAD_GATEWAY,
                client,
                searchRequest(baseUri + "&cloud_ids=cloud1&request=res"));

            cluster.iamService().supplier(
                new IamFolderPermissionMapHandler(DEFAULT_TOKEN, "folder10", "none"));

            // iam respond with permission denied
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=cloud1&request=res"),
                "{\"results\":[]}");

            // if no permission in doc, we should asking for "get" permission
            cluster.iamService().supplier(
                new IamFolderPermissionMapHandler(DEFAULT_TOKEN, "folder1", "get"));

            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=cloud1&request=res"),
                "{\"results\":[" + JsonType.NORMAL.toString(doc1) + "]}");

            // same, but without token
            HttpAssert.assertStatusCode(
                HttpStatus.SC_BAD_REQUEST,
                client,
                new HttpGet(baseUri + "&cloud_ids=cloud1&request=res"));

            // add one more same cloud, but different folder
            JsonMap doc2 = indexerCluster.addDoc(
                YcIndexerCluster.doc("resource2", "cloud1", "folder2", "", "mdb.all.read"));
            indexerCluster.searchBackend().flush();

            // without perms, still 1 result
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=cloud1&request=res"),
                "{\"results\":[" + JsonType.NORMAL.toString(doc1) + "]}");

            Map<String, Set<String>> iamFolders = new LinkedHashMap<>();
            iamFolders.put("folder1", Collections.singleton("get"));
            iamFolders.put("folder2", Collections.singleton("get"));

            cluster.iamService().supplier(
                new IamFolderPermissionMapHandler(DEFAULT_TOKEN, new LinkedHashMap<>(iamFolders)));

            // wrong perm, still one result
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=cloud1&request=res"),
                "{\"results\":[" + JsonType.NORMAL.toString(doc1) + "]}");

            // now 2 results
            iamFolders = new LinkedHashMap<>();
            iamFolders.put("folder1", Collections.singleton("get"));
            iamFolders.put("folder2", Collections.singleton("mdb.all.read"));

            cluster.iamService().supplier(
                new IamFolderPermissionMapHandler(DEFAULT_TOKEN, new LinkedHashMap<>(iamFolders)));

            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=cloud1&request=res"),
                "{\"results\":[" + JsonType.NORMAL.toString(doc2)
                    + ',' + JsonType.NORMAL.toString(doc1)
                    + "]}");

            // and final add different cloud doc with some attributes
            StringPrefix prefix2 =
                new StringPrefix(YcFields.buildPrefix("cloud2"));
            cluster.addStatus(prefix2);

            JsonMap doc3 = indexerCluster.addDoc(
                YcIndexerCluster.doc(
                    "notresource2_1",
                    "cloud2",
                    "folder2_1",
                    "\"name\":\"resource21\",\"name2\":\"resource31\"",
                    "mdb.all.read"));
            indexerCluster.searchBackend().flush();

            // without perm, got nothing
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=cloud1,cloud2&request=res"),
                "{\"results\":[" + JsonType.NORMAL.toString(doc2)
                    + ',' + JsonType.NORMAL.toString(doc1)
                    + "]}");
            // other order of clouds in request
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=cloud2,cloud1&request=res"),
                "{\"results\":[" + JsonType.NORMAL.toString(doc2)
                    + ',' + JsonType.NORMAL.toString(doc1)
                    + "]}");

            iamFolders = new LinkedHashMap<>();
            iamFolders.put("folder1", Collections.singleton("get"));
            iamFolders.put("folder2", Collections.singleton("mdb.all.read"));
            iamFolders.put("folder2_1", Collections.singleton("mdb.all.read"));
            cluster.iamService().supplier(
                new IamFolderPermissionMapHandler(DEFAULT_TOKEN, new LinkedHashMap<>(iamFolders)));
            // other order of clouds in request
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=cloud2,cloud1&request=res"),
                "{\"results\":["
                    + JsonType.NORMAL.toString(doc2)
                    + ',' + JsonType.NORMAL.toString(doc1)
                    + ',' + JsonType.NORMAL.toString(doc3)
                    + "]}");
        }
    }

    private String loadResource(final String name) throws IOException {
        return IOStreamUtils.consume(
            new InputStreamReader(
                this.getClass().getResourceAsStream(name),
                StandardCharsets.UTF_8))
            .toString();
    }

    @Test
    public void testMarketplaceProjects() throws Exception {
        try (YcSearchProxyCluster searchCluster = new YcSearchProxyCluster(this);
             YcIndexerCluster indexerCluster = new YcIndexerCluster(this, searchCluster.searchBackend());
             CloseableHttpClient client = HttpClients.createDefault();) {
            indexerCluster.start();

            searchCluster.addStatus(YcSearchProxyCluster.MARKETPLACE_SERVICE, new LongPrefix(50966L));
            searchCluster.addStatus(YcSearchProxyCluster.MARKETPLACE_SERVICE, new LongPrefix(48765L));
            HttpPost update =
                new HttpPost(
                    indexerCluster.indexer().host()
                        + "/api/yc/marketplace/index?&update&transfer_ts=1570450676500");
            update.setEntity(
                new StringEntity(loadResource("marketplace_index_with_namespace.json"), StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, update);

            Thread.sleep(300);


            HttpPost post = new HttpPost(indexerCluster.searchBackend().indexerUri() + "/schema_update");
            post.setEntity(new FileEntity(new File(Paths.getSourcePath(
                "mail/search/yc/yc_backend/files/yc_marketplace_backend_fields.conf"))));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            String baseUri = searchCluster.proxy().host() + "/api/marketplace/search?";
            StringPrefix prefix =
                new StringPrefix(MarketplaceFieldType.prefix("products"));
            searchCluster.addStatus(prefix);
            HttpAssert.assertJsonResponse(
                client,
                baseUri
                    + "sort_order=asc&length=5&offset=0&filter=categories_id%3Adatabases" +
                    "+AND+language%3Aru+AND+%28type%3Acompute-image%29" +
                    "&sort=categories_rank&project=products",
                EMPTY_MARKETPLACE_SEARCH_RESP);

            HttpAssert.assertJsonResponse(
                client,
                baseUri
                    + "sort_order=asc&length=5&offset=0&filter=categories_id%3Addataset" +
                    "+AND+language%3Aru+AND+%28type%3Acompute-image%29" +
                    "&sort=categories_rank&project=products",
                EMPTY_MARKETPLACE_SEARCH_RESP);
        }
    }

    @Test
    public void testMarketplaceSearch() throws Exception {
        try (YcSearchProxyCluster searchCluster = new YcSearchProxyCluster(this);
             YcIndexerCluster indexerCluster = new YcIndexerCluster(this, searchCluster.searchBackend());
             CloseableHttpClient client = HttpClients.createDefault();)
        {
            indexerCluster.start();

            searchCluster.addStatus(YcSearchProxyCluster.MARKETPLACE_SERVICE, new LongPrefix(50966L));
            searchCluster.addStatus(YcSearchProxyCluster.MARKETPLACE_SERVICE, new LongPrefix(30948L));


//            HttpPost post = new HttpPost(indexerCluster.searchBackend().indexerUri() + "/schema_update");
//            post.setEntity(
//                new StringEntity(
//                    loadResourceAsString("marketplace_default_fields.conf"),
//                    StandardCharsets.UTF_8));
//            HttpAssert.assertStatusCode(HttpStatus.SC_OK, post);

            HttpPost update =
                new HttpPost(
                    indexerCluster.indexer().host()
                        + "/api/yc/marketplace/index?&update&transfer_ts=1570450676500");
            update.setEntity(
                new StringEntity(loadResource("marketplace1.json"), StandardCharsets.UTF_8));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, update);

            Thread.sleep(300);

            String baseUri = searchCluster.proxy().host() + "/api/marketplace/search?";
            StringPrefix prefix =
                new StringPrefix(YcFields.buildPrefix(YcConstants.MARKETPLACE_PREFIX));
            searchCluster.addStatus(prefix);
            HttpAssert.assertJsonResponse(
                client,
                baseUri + "filter=categories_id:dqntbd2qa8uansspfejc&sort=categories_rank&get=id",
                loadResource("expected_marketplace_sort_rank.json"));

            HttpAssert.assertJsonResponse(
                client,
                baseUri + "filter=versionId:dqnqa9v2qmnhet8jf618",
                loadResource("expected_marketplace_1.json"));

            HttpAssert.assertJsonResponse(
                client,
                baseUri
                    + "filter=versionId:dqnqa9v2qmnhet8jf618" +
                    "&text=янде&search_fields=publisher.marketingInfo.name&locale=ru",
                loadResource("expected_marketplace_1.json"));

            HttpAssert.assertJsonResponse(
                client,
                baseUri
                    + "filter=versionId:dqnqa9v2qmnhet8jf618" +
                    "&text=yand&search_fields=publisher.marketingInfo.name&locale=en",
                loadResource("expected_marketplace_2.json"));

            HttpAssert.assertJsonResponse(
                client,
                searchCluster.proxy().host() +
                    "/api/marketplace/aggregation?filter=versionId:dqnqa9v2qmnhet8jf618&field=pricing.type",
                "{\"data\": {\"FREE\": 2}}");

            HttpAssert.assertJsonResponse(
                client,
                searchCluster.proxy().host() +
                    "/api/marketplace/aggregation?filter=versionId:dqnqa9v2qmnhet8jf618" +
                    "&field=pricing.type&text=brbr&search_fields=marketingInfo.name",
                "{\"data\": {}}");

            HttpAssert.assertJsonResponse(
                client,
                searchCluster.proxy().host() +
                    "/api/marketplace/aggregation?filter=versionId:dqnqa9v2qmnhet8jf618" +
                    "&field=pricing.type&text=Cent&search_fields=marketingInfo.name",
                "{\"data\": {\"FREE\": 2}}");

            // вебзавод
            HttpAssert.assertJsonResponse(
                client,
                searchCluster.proxy().host() +
                    "/api/marketplace/aggregation?filter=language:ru+AND+publisher_id:dqn79i9gju0s28jjgdjr" +
                    "&field=pricing.type&text=%D1%86%D1%83%D0%B8%D1%8F%D1%84%D0%BC%D1%89%D0%B2" +
                    "&search_fields=publisher.name",
                "{\"data\": {\"FREE\": 2}}");
        }
    }

    @Test
    public void testIndexRaceDeleteUpdate() throws Exception {
        try (YcSearchProxyCluster cluster = new YcSearchProxyCluster(this);
             YcIndexerCluster indexerCluster = new YcIndexerCluster(this, cluster.searchBackend());
             CloseableHttpClient client = HttpClients.createDefault();)
        {
            indexerCluster.start();
            cluster.iamService().supplier(IamAllAllowHandler.INSTANCE);
            StringPrefix prefix1 =
                new StringPrefix(YcFields.buildPrefix("cloud1"));
            cluster.addStatus(prefix1);
            StringPrefix prefix2 =
                new StringPrefix(YcFields.buildPrefix("cloud2"));
            cluster.addStatus(prefix2);

            long ts = 1598508715000L;

            // first delete is faster than update
            JsonMap doc1 = indexerCluster.addDoc(
                YcIndexerCluster.doc(
                    "resource1",
                    "cloud1",
                    "folder1",
                    "\"name\":\"resource21\",\"name2\":\"resource31\"",
                    "",
                    ts,
                    true));
            indexerCluster.searchBackend().flush();

            String baseUri = cluster.proxy().host() + "/api/search?highlight=false";
            StringPrefix prefix =
                new StringPrefix(YcFields.buildPrefix("cloud1"));
            cluster.addStatus(prefix);

            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=cloud1,cloud2&request=res"),
                "{\"results\":[]}");

            // and now update
            JsonMap doc2 = indexerCluster.addDoc(
                YcIndexerCluster.doc(
                    "resource1",
                    "cloud1",
                    "folder1",
                    "\"name\":\"resource21\",\"name2\":\"resource31\"",
                    "",
                    ts - 1,
                    false));
            indexerCluster.searchBackend().flush();
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=cloud1,cloud2&request=res"),
                "{\"results\":[]}");

            JsonMap doc3 = indexerCluster.addDoc(
                YcIndexerCluster.doc(
                    "resource1",
                    "cloud1",
                    "folder1",
                    "\"name\":\"resource21\",\"name2\":\"resource31\"",
                    "",
                    ts + 2,
                    false));

            indexerCluster.searchBackend().flush();
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=cloud2,cloud1&request=res"),
                "{\"results\":["
                    + JsonType.NORMAL.toString(doc3)
                    + "]}");

            indexerCluster.addDoc(
                YcIndexerCluster.doc(
                    "resource1",
                    "cloud1",
                    "folder1",
                    "\"name\":\"resource21\",\"name2\":\"resource31\"",
                    "",
                    ts + 1,
                    true));
            indexerCluster.searchBackend().flush();
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=cloud2,cloud1&request=res"),
                "{\"results\":["
                    + JsonType.NORMAL.toString(doc3)
                    + "]}");
        }
    }

    @Ignore
    @Test
    public void generateSchema() throws Exception {
        Map<String, String> schema = new LinkedHashMap<>();
        String data = loadResource("marketplace_2.json");
        JsonMap root = TypesafeValueContentHandler.parse(data).asMap();
        for (JsonObject docObj: root.getList("documents")) {
            for (JsonObject fieldObj: docObj.asMap().getList("fields")) {
                JsonMap field = fieldObj.asMap();
                schema.put(field.getString("name"), field.getString("indexType"));
            }
        }

        for (Map.Entry<String, String> entry: schema.entrySet()) {
            System.out.print("[field.yc_mkpl_ext_" + entry.getKey().replaceAll("\\.", "_") + "]\n");
            if ("timestamp".equalsIgnoreCase(entry.getValue())
                || "keyword".equalsIgnoreCase(entry.getValue()))
            {
                System.out.print("tokenizer = keyword\n");
            } else if ("full_text".equalsIgnoreCase(entry.getValue())) {
                System.out.print("tokenizer = letter\n" +
                    "filters = lowercase|replace:ё:е|lemmer\n");
            } else {
                System.out.print("tokenizer = unknown\n");
            }

            System.out.print(
                "store = true\n" +
                "prefixed = true\n" +
                "attribute = true\n" +
                "analyze = true\n\n");
        }
    }

    @Test
    public void testMarketplaceSearchCases() throws Exception {
        try (YcSearchProxyCluster searchCluster = new YcSearchProxyCluster(this);
             YcIndexerCluster indexerCluster = new YcIndexerCluster(this, searchCluster.searchBackend());
             CloseableHttpClient client = HttpClients.createDefault();)
        {
            indexerCluster.start();

            HttpPost update =
                new HttpPost(
                    indexerCluster.indexer().host()
                        + "/api/yc/marketplace/index?&update&transfer_ts=1570450676500");
            update.setEntity(
                new FileEntity(
                    new File(Paths.getSandboxResourcesRoot() + "/test_marketplace_data")));
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, update);

//            HttpPost post = new HttpPost(indexerCluster.searchBackend().indexerUri() + "/add");
//            post.setEntity(
//                new StringEntity(
//                    loadResource(),
//                    ContentType.APPLICATION_JSON));
//            HttpAssert.assertStatusCode(HttpStatus.SC_OK, client, post);

            //Thread.sleep(300);

            String baseUri = searchCluster.proxy().host() + "/api/marketplace/search?project=products&";
            StringPrefix prefix =
                new StringPrefix(MarketplaceFieldType.prefix("products"));
            searchCluster.addStatus(YcConstants.YC_MARKETPLACE_QUEUE, prefix);

            HttpAssert.assertJsonResponse(
                client,
                baseUri
                    + "&length=100&offset=0&text=wordpres" +
                    "&search_fields=marketingInfo.name%2CmarketingInfo.description%2CmarketingInfo.shortDescription%2CmarketingInfo.useCases%2Cpublisher.marketingInfo.name" +
                    "&filter=language%3Aru+AND+pricing_type%3Afree+AND+%28type%3Acompute-image%29&get=id",
                "{\"has_next\":false,\"total\":2,\"documents\":[{\"id\":\"f2eak49isamh99q2kjce\"}," +
                    "{\"id\":\"f2eugfso8lh4ch2g2m9h\"}]}");
            HttpAssert.assertJsonResponse(
                client,
                baseUri
                    + "length=100&text=windows&offset=0" +
                    "&filter=language%3Aru+AND+%28type%3Acompute-image%29" +
                    "&search_fields=marketingInfo.name%2CmarketingInfo.description%2CmarketingInfo.shortDescription%2CmarketingInfo.useCases%2Cpublisher.marketingInfo.name" +
                    "&get=id",
                "{\"has_next\":false,\"total\":20,\"documents\":[{\"id\":\"f2eapru3a4ub52cpqanu\"}," +
                    "{\"id\":\"f2eddoiomdaot9lc52il\"},{\"id\":\"f2egp7ticiot6ot36tqn\"}," +
                    "{\"id\":\"f2eokakrkt08gc3iqnl6\"},{\"id\":\"f2er2p7a5hss0a2t610e\"}," +
                    "{\"id\":\"f2ese5fj6ot3vpb32rk9\"},{\"id\":\"f2etfsso99bl8ihpgok9\"}," +
                    "{\"id\":\"f2e0alp7qtespl4icl7b\"},{\"id\":\"f2e22gr96kkig9n7bdp2\"}," +
                    "{\"id\":\"f2e5os2n4qmi4lr5q8cb\"},{\"id\":\"f2ec8n42dl9csn38h6j6\"}," +
                    "{\"id\":\"f2ed48or3933dg5mkv8o\"},{\"id\":\"f2edrcrq3428m6k65dve\"}," +
                    "{\"id\":\"f2ent6cnb49sf5n9s1u2\"},{\"id\":\"f2eob03q1b62vg3fhe0t\"}," +
                    "{\"id\":\"f2ep1515h63fnvqh6hkl\"},{\"id\":\"f2eqqi8hsu5s05mtu4r5\"}," +
                    "{\"id\":\"f2erbj95lg662vvj3f0r\"},{\"id\":\"f2evussurs3m1v2turq6\"}," +
                    "{\"id\":\"f2e6gdh3e3jjt58rc1md\"}]}");

            HttpAssert.assertJsonResponse(
                client,
                baseUri
                    + "length=100&text=windows+server&offset=0" +
                    "&filter=language%3Aru+AND+%28type%3Acompute-image%29" +
                    "&search_fields=marketingInfo.name%2CmarketingInfo.description%2CmarketingInfo.shortDescription%2CmarketingInfo.useCases%2Cpublisher.marketingInfo.name" +
                    "&get=id",
                "{\"has_next\":false,\"total\":17,\"documents\":[{\"id\":\"f2e0alp7qtespl4icl7b\"}," +
                    "{\"id\":\"f2e22gr96kkig9n7bdp2\"},{\"id\":\"f2e5os2n4qmi4lr5q8cb\"}," +
                    "{\"id\":\"f2ec8n42dl9csn38h6j6\"},{\"id\":\"f2edrcrq3428m6k65dve\"}," +
                    "{\"id\":\"f2ent6cnb49sf5n9s1u2\"},{\"id\":\"f2eob03q1b62vg3fhe0t\"}," +
                    "{\"id\":\"f2ep1515h63fnvqh6hkl\"},{\"id\":\"f2erbj95lg662vvj3f0r\"}," +
                    "{\"id\":\"f2evussurs3m1v2turq6\"},{\"id\":\"f2eapru3a4ub52cpqanu\"}," +
                    "{\"id\":\"f2eddoiomdaot9lc52il\"},{\"id\":\"f2egp7ticiot6ot36tqn\"}," +
                    "{\"id\":\"f2eokakrkt08gc3iqnl6\"},{\"id\":\"f2er2p7a5hss0a2t610e\"}," +
                    "{\"id\":\"f2ese5fj6ot3vpb32rk9\"},{\"id\":\"f2etfsso99bl8ihpgok9\"}]}");

            HttpAssert.assertJsonResponse(
                client,
                searchCluster.proxy().host() +
                    "/api/marketplace/aggregation?&project=products&field=pricing.type&filter=language%3Aen+AND+%28type%3Acompute-image%29",
                "{\"data\":{\"free\":46,\"monthly\":17,\"payg\":17,\"byol\":8}}");
        }
    }


    @Test
    public void testSearchAllowedServices() throws Exception {
        Predicate<String> allowed =
            AllowedServicesParser.INSTANCE.apply(
                "whitelist_file:"
                    + Paths.getSourcePath("mail/search/yc/yc_search_proxy/debian/files/allowed_services_prod.txt"));
        Assert.assertTrue(allowed.test("compute"));
        Assert.assertTrue(allowed.test("vpc"));
        Assert.assertFalse(allowed.test("abirvalg"));

        YCProxyConfigBuilder config = new YCProxyConfigBuilder(
            YcSearchProxyCluster.loadConfig(YcSearchProxyCluster.CLOUD_PROXY_CONFIG));
        config.allowedServices(AllowedServicesParser.INSTANCE.apply("whitelist:dns,managed-redis"));
        try (YcSearchProxyCluster cluster = new YcSearchProxyCluster(this, config);
             CloseableHttpClient client = HttpClients.createDefault()) {
            StringPrefix prefix =
                new StringPrefix(YcFields.buildPrefix("foodnh1gbffklfion4rb"));

            cluster.addStatus(prefix);
            cluster.iamService().supplier(IamAllAllowHandler.INSTANCE);

            String docBase =
                "\"yc_attributes\": \"{\\\"hosts\\\":[\\\"iva-bs4f2nar3m02duh2.db.yandex.net\\\"," +
                "\\\"myt-ow5uw04z3lcb0ohe.db.yandex.net\\\",\\\"sas-ezxquljvyhlpera8.db.yandex.net\\\"]," +
                "\\\"name\\\":\\\"dbaas_e2e_porto_qa\\\",\\\"description\\\":\\\"\\\",\\\"labels\\\":{}}\",\n" +
                "\"yc_doc_type\": \"main\",\n" +
                "\"yc_cloud_id\": \"foodnh1gbffklfion4rb\",\n" +
                "\"yc_folder_id\": \"foorv7rnqd9sfo4q6db4\",\n" +
                "\"yc_permission\": \"mdb.all.read\",\n" +
                "\"yc_resource_id\": \"mdbuftkk264dqpm4oo14\",\n" +
                "\"yc_resource_type\": \"cluster\",\n" +
                "\"yc_timestamp_str\": \"2019-10-08T07:51:48.080691+03:00\",\n" +
                "\"yc_timestamp\": 1570510308080691,\n" +
                "\"yc_transfer_ts\": 1570511554131";

            String redisDoc =
                "\"id\": \"foodnh1gbffklfion4rb_managed-redis_cluster_mdbuftkk264dqpm4oo14\"," +
                    "\"yc_service\": \"managed-redis\",\n"
                    + docBase;
            String serverlessDoc =
                "\"id\": \"foodnh1gbffklfion4rb_serverless_cluster_mdbuftkk264dqpm4oo14\"," +
                    "\"yc_service\": \"serverless\",\n"
                    + docBase;
            String dnsDoc =
                "\"id\": \"foodnh1gbffklfion4rb_dns_cluster_mdbuftkk264dqpm4oo14\"," +
                    "\"yc_service\": \"dns\",\n"
                    + docBase;


            cluster.searchBackend().add(prefix, redisDoc, serverlessDoc, dnsDoc);

            cluster.searchBackend().flush();
            String baseUri = cluster.proxy().host() + "/api/search?&cloud_id=foodnh1gbffklfion4rb&highlight=false";

            String expectedBase =
                    "      \"permission\": \"mdb.all.read\",\n" +
                    "      \"attributes\": {\n" +
                    "        \"hosts\": [\n" +
                    "          \"iva-bs4f2nar3m02duh2.db.yandex.net\",\n" +
                    "          \"myt-ow5uw04z3lcb0ohe.db.yandex.net\",\n" +
                    "          \"sas-ezxquljvyhlpera8.db.yandex.net\"\n" +
                    "        ],\n" +
                    "        \"name\": \"dbaas_e2e_porto_qa\",\n" +
                    "        \"description\": \"\",\n" +
                    "        \"labels\": {}\n" +
                    "      },\n" +
                    "      \"resource_type\": \"cluster\",\n" +
                    "      \"folder_id\": \"foorv7rnqd9sfo4q6db4\",\n" +
                    "      \"timestamp\": \"1570510308080691\",\n" +
                    "      \"resource_id\": \"mdbuftkk264dqpm4oo14\",\n" +
                    "      \"cloud_id\": \"foodnh1gbffklfion4rb\",\n" +
                    "      \"timestamp\": \"2019-10-08T07:51:48.080691+03:00\"" +
                    "    }\n";
            String expectedRedis =
                "{\"service\": \"managed-redis\",\n" + expectedBase;
            String expectedDns =
                "{\"service\": \"dns\",\n" + expectedBase;
            String expected1 = "{\n" +
                "  \"results\": [\n" +
                expectedRedis + ',' + expectedDns + "]\n" +
                "}";

            HttpAssert.assertJsonResponse(client, searchRequest(baseUri + "&request=mdbuftkk264dqpm4oo14&debug=true"), expected1);
        }
    }

    @Test
    public void testResourceMeta() throws Exception {
        try (YcSearchProxyCluster cluster = new YcSearchProxyCluster(this);
             YcIndexerCluster indexerCluster = new YcIndexerCluster(this, cluster.searchBackend());
             CloseableHttpClient client = HttpClients.createDefault())
        {
            HttpPost add =
                new HttpPost(
                    indexerCluster.indexer().host()
                        + "/api/yc/index?transfer_ts=1570450676326&add"
                        + YcIndexerCluster.DEFAULT_LB_PARAMS);
            add.setEntity(
                new StringEntity(
                    loadResource("v2/index_docs_1.json"),
                    StandardCharsets.UTF_8));

            indexerCluster.start();
            cluster.iamService().supplier(IamAllAllowHandler.INSTANCE);

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, add);

            String baseUri = cluster.proxy().host() + "/api/resource/meta?";

            StringPrefix prefix =
                new StringPrefix(YcFields.buildPrefix("aoenio4bf5tauqgr4tj4"));
            cluster.addStatus(prefix);

            // 2 requested, 1 found, 1 not found
            String expected = "{\"resources\":[" +
                "{\"service\":\"dns\"," +
                "\"permission\":\"dns.zones.get\"," +
                "\"attributes\":{\"zone\":\"au.zone\"}," +
                "\"resource_type\":\"zone\"," +
                "\"folder_id\":\"aoeb1dcmdofeq4cpjpqe\"," +
                "\"name\":\"My name a auto-c64bp88p70p8lbb707ok-_\"," +
                "\"resource_id\":\"aet02kphp9qre1g2k76a\"," +
                "\"cloud_id\":\"aoenio4bf5tauqgr4tj4\"," +
                "\"timestamp\":\"2021-03-29T22:14:00.518Z\"," +
                "\"resource_path\":[{\"resource_type\":\"resource-manager.cloud\"," +
                "\"resource_id\":\"aoenio4bf5tauqgr4tj4\"},{\"resource_type\":\"resource-manager.folder\"," +
                "\"resource_id\":\"aoeb1dcmdofeq4cpjpqe\"},{\"resource_type\":\"dns.zone\"," +
                "\"resource_id\":\"aet02kphp9qre1g2k76a\"}]}]}";

            HttpAssert.assertJsonResponse(
                client,
                searchRequest(
                    baseUri
                        + "&cloud_id=aoenio4bf5tauqgr4tj4" +
                        "&res_ids=aet02kphp9qre1g2k76a,aaa02kphp9qre1g2k76a"),
                expected);
            cluster.iamService().supplier(
                new IamFolderPermissionMapHandler(
                    DEFAULT_TOKEN,
                    "aoeb1dcmdofeq4cpjpqe",
                    "dns.zones.get"));
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(
                    baseUri
                        + "&cloud_id=aoenio4bf5tauqgr4tj4" +
                        "&res_ids=aet02kphp9qre1g2k76a,aaa02kphp9qre1g2k76a"),
                expected);
            cluster.iamService().supplier(
                new IamFolderPermissionMapHandler(
                    DEFAULT_TOKEN,
                    "aoeb1dcmdofeq4cpjpqe",
                    "dns.zones.put"));
            HttpAssert.assertJsonResponse(
                client,
                searchRequest(
                    baseUri
                        + "&cloud_id=aoenio4bf5tauqgr4tj4" +
                        "&res_ids=aet02kphp9qre1g2k76a,aaa02kphp9qre1g2k76a"),
                "{\"resources\":[]}");

        }
    }

    @Test
    public void testLabelsHandler() throws Exception {
        try (YcSearchProxyCluster cluster = new YcSearchProxyCluster(this);
             YcIndexerCluster indexerCluster = new YcIndexerCluster(this, cluster.searchBackend());
             CloseableHttpClient client = HttpClients.createDefault())
        {
            indexerCluster.start();

            StringPrefix prefix1 =
                    new StringPrefix(YcFields.buildPrefix("aoe9shbqc2v314v7fp3d"));
            StringPrefix prefix2 =
                    new StringPrefix(YcFields.buildPrefix("aoenio4bf5tauqgr4tj4"));
            cluster.addStatus(prefix1);
            cluster.addStatus(prefix2);

            Map<String, Set<String>> iamFolders = new LinkedHashMap<>();
            iamFolders.put("aoeme1ci0qvbsjia4ks7", Collections.singleton("mdb.all.read"));
            iamFolders.put("aoeb1dcmdofeq4cpjpqe", Collections.singleton("dns.zones.get"));
            //iamFolders.put("not-allowed-folder", Collections.singleton("mdb.all.read"));

            cluster.iamService().supplier(
                    new IamFolderPermissionMapHandler(DEFAULT_TOKEN, new LinkedHashMap<>(iamFolders)));
            //cluster.iamService().supplier(IamAllAllowHandler.INSTANCE);

            HttpPost add1 =
                new HttpPost(
                    indexerCluster.indexer().host()
                        + "/api/yc/index?transfer_ts=1570450676326&add"
                        + YcIndexerCluster.DEFAULT_LB_PARAMS);
            add1.setEntity(
                new StringEntity(
                    loadResource("v2/index_docs_with_labels.json"),
                    StandardCharsets.UTF_8));

            HttpPost addNoPermission =
                new HttpPost(
                    indexerCluster.indexer().host()
                        + "/api/yc/index?transfer_ts=1570450676328&add"
                        + YcIndexerCluster.DEFAULT_LB_PARAMS);
            addNoPermission.setEntity(
                new StringEntity(
                    loadResource("v2/index_docs_with_labels3.json"),
                    StandardCharsets.UTF_8));

            HttpPost add2 =
                new HttpPost(
                    indexerCluster.indexer().host()
                        + "/api/yc/index?transfer_ts=1570450676327&add"
                        + YcIndexerCluster.DEFAULT_LB_PARAMS);
            add2.setEntity(
                new StringEntity(
                    loadResource("v2/index_docs_with_labels2.json"),
                    StandardCharsets.UTF_8));

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, add1);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, addNoPermission);
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, add2);

            String baseUri = cluster.proxy().host() + "/api/labels/list?";

            //making one more search request to lucene due to lack of rights for some items of the first result
            HttpUriRequest request1 = searchRequest(baseUri +
                "&cloud_ids=aoe9shbqc2v314v7fp3d,aoenio4bf5tauqgr4tj4&length=8&format=proto");

            LabelProto.TLabelResponse expected1 = LabelProto.TLabelResponse
                .newBuilder()
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("labelName5")
                    .setLabelValue("labelValue5")
                    .build())
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("labelName3")
                    .setLabelValue("labelValue3")
                    .build())
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("labelName4")
                    .setLabelValue("labelValue4")
                    .build())
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("labelName2")
                    .setLabelValue("labelValue2")
                    .build())
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("mdb-auto-purge")
                    .setLabelValue("off")
                    .build())
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("labelName6")
                    .setLabelValue("labelValue6")
                    .build())
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("labelName3")
                    .setLabelValue("labelValue3_")
                    .build())
                .setMore(false)
                .build();

            CloseableHttpResponse response1 = client.execute(request1);
            LabelProto.TLabelResponse labelResponse1 = LabelProto
                .TLabelResponse.parseFrom(response1.getEntity().getContent());

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response1);
            Assert.assertEquals(expected1, labelResponse1);

            //offset more = false
            HttpUriRequest request2 = searchRequest(baseUri +
                "&cloud_ids=aoe9shbqc2v314v7fp3d,aoenio4bf5tauqgr4tj4&length=6&offset=2&format=proto");

            LabelProto.TLabelResponse expected2 = LabelProto.TLabelResponse
                .newBuilder()
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("labelName4")
                    .setLabelValue("labelValue4")
                    .build())
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("labelName2")
                    .setLabelValue("labelValue2")
                    .build())
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("mdb-auto-purge")
                    .setLabelValue("off")
                    .build())
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("labelName6")
                    .setLabelValue("labelValue6")
                    .build())
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("labelName3")
                    .setLabelValue("labelValue3_")
                    .build())
                .setMore(false)
                .build();

            CloseableHttpResponse response2 = client.execute(request2);
            LabelProto.TLabelResponse labelResponse2 = LabelProto
                .TLabelResponse.parseFrom(response2.getEntity().getContent());

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response2);
            Assert.assertEquals(expected2, labelResponse2);

            //offset more = true
            HttpUriRequest request3 = searchRequest(baseUri +
                "&cloud_ids=aoe9shbqc2v314v7fp3d,aoenio4bf5tauqgr4tj4&length=2&offset=3&format=proto");

            LabelProto.TLabelResponse expected3 = LabelProto.TLabelResponse
                .newBuilder()
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("labelName2")
                    .setLabelValue("labelValue2")
                    .build())
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("mdb-auto-purge")
                    .setLabelValue("off")
                    .build())
                .setMore(true)
                .build();

            CloseableHttpResponse response3 = client.execute(request3);
            LabelProto.TLabelResponse labelResponse3 = LabelProto
                .TLabelResponse.parseFrom(response3.getEntity().getContent());

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response3);
            Assert.assertEquals(expected3, labelResponse3);

            //simple with one request to lucene
            HttpUriRequest request4 = searchRequest(baseUri +
                "&cloud_ids=aoe9shbqc2v314v7fp3d,aoenio4bf5tauqgr4tj4&length=3&format=proto");

            LabelProto.TLabelResponse expected4 = LabelProto.TLabelResponse
                .newBuilder()
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("labelName5")
                    .setLabelValue("labelValue5")
                    .build())
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("labelName3")
                    .setLabelValue("labelValue3")
                    .build())
                .addLabel(LabelProto.TLabel.newBuilder()
                    .setLabelName("labelName4")
                    .setLabelValue("labelValue4")
                    .build())
                .setMore(true)
                .build();

            CloseableHttpResponse response4 = client.execute(request4);
            LabelProto.TLabelResponse labelResponse4 = LabelProto
                .TLabelResponse.parseFrom(response4.getEntity().getContent());

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response4);
            Assert.assertEquals(expected4, labelResponse4);

            //no labels in response
            HttpUriRequest request5 = searchRequest(baseUri +
                "&cloud_ids=aoe9shbqc2v314v7fp3d,aoenio4bf5tauqgr4tj4&length=1&offset=7&format=proto");

            LabelProto.TLabelResponse expected5 = LabelProto.TLabelResponse
                .newBuilder()
                .setMore(false)
                .build();

            CloseableHttpResponse response5 = client.execute(request5);
            LabelProto.TLabelResponse labelResponse5 = LabelProto
                .TLabelResponse.parseFrom(response5.getEntity().getContent());

            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response5);
            Assert.assertEquals(expected5, labelResponse5);

            //String expectedJson = "{\"labels\":[{\"mdb-auto-purge\":\"off\"}], \"more\": false}";
            String expectedJson = "{\"labels\":[{\"labelName5\":\"labelValue5\"}, " +
                "{\"labelName3\":\"labelValue3\"}, {\"labelName4\":\"labelValue4\"}, " +
                "{\"labelName2\":\"labelValue2\"}, {\"mdb-auto-purge\":\"off\"}," +
                "{\"labelName6\":\"labelValue6\"}, {\"labelName3\":\"labelValue3_\"}], \"more\": false}";

            HttpAssert.assertJsonResponse(
                client,
                searchRequest(baseUri + "&cloud_ids=aoe9shbqc2v314v7fp3d,aoenio4bf5tauqgr4tj4&length=8"),
                expectedJson);
        }
    }
    //Field field = Paths.class.getDeclaredField("sourceRoot");
    //field.setAccessible(true);
    //field.set(null, "/home/vonidu/Projects/PS/repo/arc2/arcadia");
}
