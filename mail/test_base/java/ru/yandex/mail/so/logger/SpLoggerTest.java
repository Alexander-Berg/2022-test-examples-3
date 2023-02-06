package ru.yandex.mail.so.logger;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.mail.so.logger.config.SpLoggerConfigBuilder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.YandexAssert;

public class SpLoggerTest extends SpLoggerTestBase {
    private static final String QUEUEID3 = "DXdaF6VCwB-ETNucp9j";
    private static final String QUEUEID4 = "Mnx4hTu8OQ-8bMSZ9nq";
    private static final String QUEUEID5 = "TKkohS0fHCg1-KVfq1Upf";
    public static final String STID5 = "4887768/bba0f861e0c943fe9ff720b0bf7b3bf2";
    private static final String DELIVERYLOG_BATCH1 = "delivery_log_batch1.txt";
    private static final String DELIVERYLOG_BATCH2 = "delivery_log_batch2.txt";
    private static final String DELIVERYLOG_BATCH5 = "delivery_log_batch5.txt";
    private static final String FROMADDR3 = "sales@ips-intermedical.de";

    // CSOFF: MethodLength
    @Test
    public void testSimpleStat() throws Exception {
        try (SpLoggerCluster cluster = new SpLoggerCluster(this);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            try (CloseableHttpResponse response = client.execute(new HttpGet(cluster.spLogger().host() + STAT))) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            }
        }
    }
    // CSON: MethodLength

    @Test
    public void testConfigOfProd() throws Exception {
        try (SpLoggerCluster cluster = new SpLoggerCluster(this)) {
            cluster.start();
            Path searchMap = Files.createFile(java.nio.file.Paths.get("", System.getProperty("SEARCHMAP_PATH")));
            Path deliveryLog = Files.createFile(java.nio.file.Paths.get("", System.getProperty("DELIVERYLOG_PATH")));
            Path spareDeliveryLog =
                Files.createFile(java.nio.file.Paths.get("", System.getProperty("SPARE_DELIVERYLOG_PATH")));
            String configProdContent =
                cluster.loadSource("mail/so/daemons/sp_logger/sp_logger_config/files/sp-logger-prod.conf");
            IniConfig configProdIni = new IniConfig(new StringReader(configProdContent));
            new SpLoggerConfigBuilder(configProdIni);
            configProdIni.checkUnusedKeys();
            Files.delete(searchMap);
            Files.delete(deliveryLog);
            Files.delete(spareDeliveryLog);
        }
    }

    public void testNotFound(
        final Route route,
        final LogStorageType logStorageType,
        final AuxiliaryStorageType auxiliaryStorageType,
        final RulesStatDatabaseType rulesStatDatabaseType)
        throws Exception
    {
        try (SpLoggerCluster cluster = new SpLoggerCluster(
                this,
                SpLoggerCluster.DEFAULT_BATCH_MIN_SIZE,
                route,
                logStorageType,
                auxiliaryStorageType,
                rulesStatDatabaseType);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            try (CloseableHttpResponse response = client.execute(new HttpGet(cluster.spLogger().host() + "/"))) {
                HttpAssert.assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
            }
        }
    }

    @Test
    public void testNullHandler() throws Exception {
        testNotFound(null, LogStorageType.MDS, AuxiliaryStorageType.LOGS_CONSUMER, RulesStatDatabaseType.MONGODB);
    }

    @Test
    public void testNullLogStorage() throws Exception {
        testNotFound(Route.IN, null, AuxiliaryStorageType.LOGS_CONSUMER, RulesStatDatabaseType.MONGODB);
    }

    @Test
    public void testNullAuxiliaryStorage() throws Exception {
        testNotFound(Route.IN, LogStorageType.MDS, null, RulesStatDatabaseType.MONGODB);
    }

    @Test
    public void testNullRulesStatDB() throws Exception {
        testNotFound(Route.IN, LogStorageType.MDS, AuxiliaryStorageType.LOGS_CONSUMER, null);
    }

    private void testOnePut(final String uri, final boolean lzoCompressed) throws Exception {
        try (SpLoggerCluster cluster = new SpLoggerCluster(this);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            String body = cluster.loadResource(DELIVERY_LOG1);
            testOnePutRequest(cluster, client, uri, body, lzoCompressed);
            logger.info("testOnePut: wait for " + TIMEOUT + "ms");
            cluster.sleep(TIMEOUT);
            testSignal(cluster, cluster.logStorageSectionName() + "-batch-save-requests_ammm", 1);
        }
    }

    @Test
    public void testOnePutDeliveryIn() throws Exception {
        testOnePut(URI_IN + URI_PUT_PARAMS1, false);
    }

    @Test
    public void testOneCheck() throws Exception {
        testOnePut(URI_PUT + URI_PUT_PARAMS1, false);
    }

    @Test
    public void testOneCompressedCheck() throws Exception {
        testOnePut(URI_PUT + URI_PUT_PARAMS1, true);
    }

    private void testOnePrefixedGet(final String getUri, final boolean lzoCompressed) throws Exception {
        try (SpLoggerCluster cluster = new SpLoggerCluster(this, MDS_BATCH_SIZE_MIN);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            String body = cluster.loadResource(DELIVERY_LOG1);
            long timeout = cluster.logStorage.storageConfig().batchSavePeriod();
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS1, body, lzoCompressed);
            logger.info("testOnePrefixedGet: wait for " + timeout + "ms");
            cluster.sleep(timeout);
            testSignal(cluster, cluster.logStorageSectionName() + "-batch-save-requests_ammm", 1);
            testSearchRequest(cluster, client, getUri, Set.of(body));
        }
    }

    @Test
    public void testOnePrefixedGetDeliveryIn() throws Exception {
        testOnePrefixedGet(URI_IN + "queueid=" + QUEUEID1, false);
    }

    @Test
    public void testOnePrefixedSearch() throws Exception {
        testOnePrefixedGet(URI_SEARCH_IN + "queueid=" + QUEUEID1, false);
    }

    @Test
    public void testOnePrefixedSearchForGetById() throws Exception {
        testOnePrefixedGet(URI_SEARCH_GETBYID + "queueid=" + QUEUEID1, false);
    }

    @Test
    public void testOneCompressedPrefixedSearch() throws Exception {
        testOnePrefixedGet(URI_SEARCH_IN + "queueid=" + QUEUEID1, true);
    }

    @Test
    public void testTwoPrefixedGetDeliveryIn() throws Exception {
        try (SpLoggerCluster cluster = new SpLoggerCluster(this, MDS_BATCH_SIZE_MIDDLE);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            String body1 = cluster.loadResource(DELIVERY_LOG1);
            String body2 = cluster.loadResource(DELIVERY_LOG2);
            Set<String> bodies = Set.of(body1, body2);
            long timeout = cluster.logStorage.storageConfig().batchSavePeriod();
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS1, body1, false);
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS2, body2, false);
            logger.info("testTwoPrefixedGetDeliveryIn: wait for " + timeout + "ms");
            cluster.sleep(timeout);
            testSignal(cluster, cluster.logStorageSectionName() + "-batch-save-requests_ammm", 1);
            testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID1, Set.of(body1));
            testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID2, Set.of(body2));
            testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID1 + "," + QUEUEID2, bodies);
        }
    }

    @Test
    public void testTwoPrefixedGetDeliveryInExceededBatchSize() throws Exception {
        try (SpLoggerCluster cluster = new SpLoggerCluster(this, MDS_BATCH_SIZE_EXCEEDED);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            String body1 = cluster.loadResource(DELIVERY_LOG1);
            String body2 = cluster.loadResource(DELIVERY_LOG2);
            long timeout = cluster.logStorage.storageConfig().batchSavePeriod();
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS1, body1, false);
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS2, body2, false);
            //testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID1, Set.of());
            //testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID2, Set.of());
            //testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID1 + "," + QUEUEID2, Set.of());
            logger.info("testTwoPrefixedGetDeliveryInExceededBatchSize: wait for " + timeout + "ms");
            cluster.sleep(timeout);
            testSignal(cluster, cluster.logStorageSectionName()+ "-batch-save-requests_ammm", 1);
            logger.info("testTwoPrefixedGetDeliveryInExceededBatchSize: wait for " + TIMEOUT + "ms");
            cluster.sleep(TIMEOUT);
            testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID1, Set.of(body1));
            testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID2, Set.of(body2));
            testSearchRequest(
                cluster,
                client,
                URI_IN + "queueid=" + QUEUEID1 + "," + QUEUEID2,
                Set.of(body1, body2));
        }
    }

    @Test
    public void testUnprefixedHandleGetDeliveryIn() throws Exception {
        try (SpLoggerCluster cluster = new SpLoggerCluster(this, MDS_BATCH_SIZE_MIDDLE);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            String logBody = cluster.loadResource(DELIVERY_LOG1);
            String indexBody = "[{\n" +
                    "  \"hitsArray\": [\n" +
                    "    {\n" +
                    "      \"log_bytes_offset\": \"0\",\n" +
                    "      \"log_bytes_size\": \"23865\",\n" +
                    "      \"log_code\": \"" + CODE1 + "\",\n" +
                    "      \"log_fromaddr\": \"" + FROMADDR1 + "\",\n" +
                    "      \"log_locl\": \"" + LOCL1 + "\",\n" +
                    "      \"log_msgid\": \"<ED1031D3-92B8-11AB-0004-DC790017629A@promo.wildberries.ru>\",\n" +
                    "      \"log_mx\": \"" + MX_FRONT + "\",\n" +
                    "      \"log_offset\": \"0\",\n" +
                    "      \"log_queueid\": \"" + QUEUEID1 + "\",\n" +
                    "      \"log_rcpt_uid\": \"" + RCPT_UIDS1 + "\",\n" +
                    "      \"log_route\": \"" + cluster.route().lowerName() + "\",\n" +
                    "      \"log_size\": \"23778\",\n" +
                    "      \"log_source_ip\": \"" + SOURCE_IP1 + "\",\n" +
                    "      \"log_stid\": \"" + MdsRequestHandler.STID1 + "\",\n" +
                    "      \"log_ts\": \"" + TS1 + "\",\n" +
                    "      \"log_type\": \"" + LOG_TYPE + "\",\n" +
                    "      \"log_uid\": \"0\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"hitsCount\": 1\n" +
                    "}]";
            long timeout = cluster.logStorage.storageConfig().batchSavePeriod();
            // put data into storage & index
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS1, logBody, false);
            logger.info("testUnprefixedHandleGetDeliveryIn: wait for " + timeout + "ms");
            cluster.sleep(timeout);
            testSignal(cluster, cluster.logStorageSectionName() + "-batch-save-requests_ammm", 1);
            // test presence of data in storage & index
            cluster.sleep(TIMEOUT);
            logger.info("testUnprefixedHandleGetDeliveryIn: wait for " + TIMEOUT + "ms");
            testSearchRequest(
                cluster,
                client,
                URI_UNPREFIXED_SEARCH1 + IndexField.MSGID.fieldName() + ':' + MSGID1,
                Set.of(indexBody));
            testSearchRequest(
                cluster,
                client,
                URI_UNPREFIXED_SEARCH1 + IndexField.FROMADDR.fieldName() + ':' + FROMADDR1,
                Set.of(indexBody));
            testSearchRequest(
                cluster,
                client,
                URI_UNPREFIXED_SEARCH1 + IndexField.LOCL.fieldName() + ':' + LOCL1,
                Set.of(indexBody));
            testSearchRequest(
                cluster,
                client,
                URI_UNPREFIXED_SEARCH1 + IndexField.RCPT_UID.fieldName() + ':' + RCPT_UIDS1,
                Set.of(indexBody));
            testSearchRequest(
                cluster,
                client,
                URI_UNPREFIXED_SEARCH1 + IndexField.SOURCE_IP.fieldName() + ':' + SOURCE_IP1,
                Set.of(indexBody));
            testSearchRequest(
                cluster,
                client,
                URI_UNPREFIXED_SEARCH1 + IndexField.MX.fieldName() + ':' + MX_FRONT,
                Set.of(indexBody));
            testSearchRequest(
                cluster,
                client,
                URI_UNPREFIXED_SEARCH1 + IndexField.CODE.fieldName() + ':' + CODE1,
                Set.of(indexBody));
        }
    }

    @Test
    public void testTwoUnprefixedGetDeliveryIn1() throws Exception {
        try (SpLoggerCluster cluster = new SpLoggerCluster(this, MDS_BATCH_SIZE_MIDDLE);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            String body1 = cluster.loadResource(DELIVERY_LOG1);
            String body2 = cluster.loadResource(DELIVERY_LOG2);
            long timeout = cluster.logStorage.storageConfig().batchSavePeriod();
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS1, body1, false);
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS2, body2, false);
            logger.info("testTwoUnprefixedGetDeliveryIn1: wait for " + timeout + "ms");
            cluster.sleep(timeout);
            testSignal(cluster, cluster.logStorageSectionName() + "-batch-save-requests_ammm", 1);
            testSearchRequest(cluster, client, URI_IN + SearchParam.MSGID.paramName() + '=' + MSGID1, Set.of(body1));
            testSearchRequest(cluster, client, URI_IN + SearchParam.MSGID.paramName() + '=' + MSGID2, Set.of(body2));
            testSearchRequest(
                cluster,
                client,
                URI_IN + SearchParam.FROMADDR.paramName() + '=' + FROMADDR1,
                Set.of(body1));
            testSearchRequest(
                cluster,
                client,
                URI_IN + SearchParam.FROMADDR.paramName() + '=' + FROMADDR2,
                Set.of(body2));
            testSearchRequest(cluster, client, URI_IN + SearchParam.LOCL.paramName() + '=' + LOCL1, Set.of(body1));
            testSearchRequest(cluster, client, URI_IN + SearchParam.LOCL.paramName() + '=' + LOCL2, Set.of(body2));
            testSearchRequest(
                cluster,
                client,
                URI_IN + SearchParam.RCPT_UID.paramName() + '=' + RCPT_UIDS1,
                Set.of(body1));
            testSearchRequest(
                cluster,
                client,
                URI_IN + SearchParam.RCPT_UID.paramName() + '=' + RCPT_UIDS2,
                Set.of(body2));
            testSearchRequest(
                cluster,
                client,
                URI_IN + SearchParam.SOURCE_IP.paramName() + '=' + SOURCE_IP1,
                Set.of(body1));
            testSearchRequest(
                cluster,
                client,
                URI_IN + SearchParam.SOURCE_IP.paramName() + '=' + SOURCE_IP2,
                Set.of(body2));
            testSearchRequest(
                cluster,
                client,
                URI_IN + SearchParam.MX.paramName() + '=' + MX_FRONT,
                Set.of(body1, body2));
            testSearchRequest(cluster, client, URI_IN + SearchParam.CODE.paramName() + '=' + CODE1, Set.of(body1));
            testSearchRequest(cluster, client, URI_IN + SearchParam.CODE.paramName() + '=' + CODE2, Set.of(body2));
            testSearchRequest(cluster, client, URI_IN + SearchParam.TS.paramName() + '=' + TS1, Set.of(body1));
            testSearchRequest(cluster, client, URI_IN + SearchParam.TS.paramName() + '=' + TS2, Set.of(body2));
        }
    }

    @Test
    public void testTwoUnprefixedGetDeliveryIn2() throws Exception {
        try (SpLoggerCluster cluster = new SpLoggerCluster(this, MDS_BATCH_SIZE_MIDDLE);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            String body1 = cluster.loadResource(DELIVERY_LOG1);
            String body2 = cluster.loadResource(DELIVERY_LOG2);
            long timeout = cluster.logStorage.storageConfig().batchSavePeriod();
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS1, body1, false);
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS2, body2, false);
            logger.info("testTwoUnprefixedGetDeliveryIn2: wait for " + timeout + "ms");
            cluster.sleep(timeout);
            testSignal(cluster, cluster.logStorageSectionName() + "-batch-save-requests_ammm", 1);
            logger.info("testTwoUnprefixedGetDeliveryIn2: wait for " + TIMEOUT + "ms");
            cluster.sleep(TIMEOUT);
            testSearchRequest(
                cluster,
                client,
                URI_IN + SearchParam.MSGID.paramName() + '=' + MSGID1 + ',' + MSGID2,
                Set.of(body1, body2));
            testSearchRequest(
                cluster,
                client,
                URI_IN + SearchParam.FROMADDR.paramName() + '=' + FROMADDR1 + ',' + FROMADDR2,
                Set.of(body1, body2));
            testSearchRequest(
                cluster,
                client,
                URI_IN + SearchParam.LOCL.paramName() + '=' + LOCL1 + ',' + LOCL2,
                Set.of(body1, body2));
            testSearchRequest(
                cluster,
                client,
                URI_IN + SearchParam.RCPT_UID.paramName() + '=' + RCPT_UIDS1 + "," + RCPT_UIDS2,
                Set.of(body1, body2));
            testSearchRequest(
                cluster,
                client,
                URI_IN + SearchParam.SOURCE_IP.paramName() + '=' + SOURCE_IP1 + "," + SOURCE_IP2,
                Set.of(body1, body2));
            testSearchRequest(
                cluster,
                client,
                URI_IN + SearchParam.CODE.paramName() + '=' + CODE1 + ',' + CODE2,
                Set.of(body1, body2));
            testSearchRequest(
                cluster,
                client,
                URI_IN + SearchParam.CODE.paramName() + "=1,2,4,8,127,256&" + SearchParam.RCPT_UID.paramName()
                    + '=' + RCPT_UIDS1 + '&'+ SearchParam.SKIP + "=0&" + SearchParam.MINTIME + "=0",
                Set.of(body1));
            testSearchRequest(
                cluster,
                client,
                URI_IN + SearchParam.TS.paramName() + '=' + TS1 + '&' + SearchParam.MSGID.paramName() + '=' + MSGID2,
                Set.of());
        }
    }

    @Test
    public void testMultiRangeByOneStidGetDeliveryCorp() throws Exception {
        try (SpLoggerCluster cluster = new SpLoggerCluster(this, MDS_BATCH_SIZE_MIDDLE, Route.CORP);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            // mock MDS storage
            cluster.logStorage().addBlock(STID1, cluster.loadResource(DELIVERYLOG_BATCH1));
            cluster.logStorage().addBlock(STID2, cluster.loadResource(DELIVERYLOG_BATCH2));
            String body1 = readFilesLine(DELIVERYLOG_BATCH1, 15);
            logger.info("testMultiRangeByOneStidGetDeliveryCorp: prefix1=" + cluster.prefix(QUEUEID3) + ", body1="
                + body1);
            String body2 = readFilesLine(DELIVERYLOG_BATCH1, 19);
            logger.info("testMultiRangeByOneStidGetDeliveryCorp: prefix2=" + cluster.prefix(QUEUEID3) + ", body2="
                + body2);
            String body3 = readFilesLine(DELIVERYLOG_BATCH2, 27);
            logger.info("testMultiRangeByOneStidGetDeliveryCorp: prefix3=" + cluster.prefix(QUEUEID4) + ", body3="
                + body3);
            // mock search backend
            cluster.lucene().add(
                cluster.prefix(QUEUEID3),
                "\"log_stid\":\"" + STID1 + "\"," + "\"log_fromaddr\":\"" + FROMADDR3 + "\",\"log_bytes_offset\":"
                    + "\"458017\"," + "\"log_offset\":\"455869\",\"log_queueid\":\"" + QUEUEID3 + "\",\"log_size\":"
                    + "\"20359\"," + "\"log_bytes_size\":\"20359\",\"log_route\":\"corp\",\"id\":"
                    + "\"log_delivery_corp_DXdaF6VCwB_ETNucp9j_1120000000026174_61afd3c9\",\"log_ts\":\"1638785671\"",
                "\"log_stid\":\"" + STID1 + "\"," + "\"log_fromaddr\":\"" + FROMADDR3 + "\",\"log_bytes_offset\":"
                    + "\"371257\",\"log_offset\":\"369306\",\"log_queueid\":\"" + QUEUEID3 + "\",\"log_size\":"
                    + "\"20347\",\"log_bytes_size\":\"20347\",\"log_route\":\"corp\",\"id\":"
                    + "\"log_delivery_corp_DXdaF6VCwB_ETNucp9j_1120000000001046_61afd3c7\",\"log_ts\":\"1638785670\"");
            cluster.lucene().add(
                cluster.prefix(QUEUEID4),
                "\"log_stid\":\"" + STID2 + "\",\"log_fromaddr\":\"" + FROMADDR3 + "\",\"log_bytes_offset\":"
                    + "\"585528\",\"log_offset\":\"583178\",\"log_queueid\":\"" + QUEUEID4 + "\",\"log_size\":"
                    + "\"19574\",\"log_bytes_size\":\"19574\",\"log_route\":\"corp\",\"id\":"
                    + "\"log_delivery_corp_Mnx4hTu8OQ_8bMSZ9nq_1120000000000441_61afdc61\",\"log_ts\":\"1638785321\"");
            logger.info("testMultiRangeByOneStidGetDeliveryCorp: executing test");
            //cluster.lucene().checkSearch("/printkeys?field=log_queueid&get=*", "{}");
            // do prefixed requests
            testSearchRequest(
                cluster,
                client,
                URI_SEARCH_CORP + SearchParam.QUEUEID.paramName() + "=" + QUEUEID3,
                Set.of(body1, body2));
            testSearchRequest(
                cluster,
                client,
                URI_SEARCH_CORP + SearchParam.QUEUEID.paramName() + "=" + QUEUEID4,
                Set.of(body3));
            // do unprefixed request
            testSearchRequest(
                cluster,
                client,
                URI_SEARCH_CORP + SearchParam.MINTIME.paramName() + "=1638784800&"
                    + SearchParam.MAXTIME.paramName() + "=1638785700&" + SearchParam.FROMADDR.paramName() + "="
                    + FROMADDR3.replace("@", "%40"),
                Set.of(body1, body2, body3));
        }
    }

    @Test
    public void testMultiDocsWithSingleRangeAndStidGetDeliveryOut() throws Exception {
        try (SpLoggerCluster cluster = new SpLoggerCluster(this, MDS_BATCH_SIZE_MIDDLE, Route.OUT);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            // mock MDS storage
            cluster.logStorage().addBlock(STID5, cluster.loadResource(DELIVERYLOG_BATCH5));
            String body = readFilesLine(DELIVERYLOG_BATCH5, 11);
            logger.info("testMultiDocsWithSingleRangeAndStidGetDeliveryOut: prefix=" + cluster.prefix(QUEUEID5)
                + ", body=" + body);
            // mock search backend
            Map<String, String> rcptDocIds = Map.of(
                "1130000023970753", "log_delivery_out_TKkohS0fHCg1_KVfq1Upf_1130000023970753_61cb44d2_2d2",
                "112955874", "log_delivery_out_TKkohS0fHCg1_KVfq1Upf_112955874_61cb44d3_355",
                "191834251", "log_delivery_out_TKkohS0fHCg1_KVfq1Upf_191834251_61cb44d4_76",
                "671304291", "log_delivery_out_TKkohS0fHCg1_KVfq1Upf_671304291_61cb44d5_1af",
                "1415463741", "log_delivery_out_TKkohS0fHCg1_KVfq1Upf_1415463741_61cb44d6_31b"
            );
            int i = 0;
            String[] docs = new String[rcptDocIds.size()];
            for (Map.Entry<String, String> entry : rcptDocIds.entrySet()) {
                docs[i++] = "\"log_stid\":\"" + STID5 + "\",\"log_bytes_offset\":\"147817\",\"log_offset\":\"146726\","
                    + "\"log_queueid\":\"" + QUEUEID5 + "\",\"log_size\":\"26233\",\"log_bytes_size\":\"26339\","
                    + "\"id\":\"" + entry.getValue() + "\",\"log_rcpt_uid\":\"" + entry.getKey() + "\","
                    + "\"log_ts\":\"1640186431\",\"log_route\":\"" + ROUTE_OUT + "\"";
            }
            cluster.lucene().add(cluster.prefix(QUEUEID5), docs);
            logger.info("testMultiDocsWithSingleRangeAndStidGetDeliveryOut: executing test");
            // do prefixed requests
            testSearchRequest(
                cluster,
                client,
                URI_SEARCH_OUT + SearchParam.QUEUEID.paramName() + "=" + QUEUEID5,
                Set.of(body));
            // do unprefixed request
            testSearchRequest(
                cluster,
                client,
                URI_SEARCH_OUT + SearchParam.RCPT_UIDS.paramName() + "=" + String.join(",", rcptDocIds.keySet()),
                Set.of(body));
        }
    }

    @Test
    public void testDeletesOneFromMds() throws Exception {
        try (SpLoggerCluster cluster = new SpLoggerCluster(this);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            String body = cluster.loadResource(DELIVERY_LOG1);
            long timeout = cluster.logStorage.storageConfig().batchSavePeriod();
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS1, body, false);
            logger.info("testDeletesOneFromMds: wait for " + timeout + "ms for upload block to MDS");
            cluster.sleep(timeout);
            String uri = SpLogger.DELETE_FROM_MDS + "?shard=11&stids=" + MdsRequestHandler.STID1;
            HttpGet get = new HttpGet(cluster.spLogger().host() + uri);
            logger.info("testDeletesOneFromMds: " + cluster.spLogger().host() + uri);
            try (CloseableHttpResponse httpResponse = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, httpResponse);
            }
            logger.info("testDeletesOneFromMds: wait for " + TIMEOUT + "ms for delete block from MDS");
            cluster.sleep(TIMEOUT);
            uri = URI_IN + "queueid=" + QUEUEID1;
            get = new HttpGet(cluster.spLogger().host() + uri);
            logger.info("testDeletesOneFromMds: " + cluster.spLogger().host() + uri);
            try (CloseableHttpResponse httpResponse = client.execute(get)) {
                String response = CharsetUtils.toString(httpResponse.getEntity());
                logger.info("testDeletesOneFromMds: GET response = '" + response + "', status = "
                    + httpResponse.getStatusLine().getStatusCode());
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, httpResponse);
                YandexAssert.check(new StringChecker(""), response);
            }
        }
    }

    @Test
    public void testDeletesTwoFromMds() throws Exception {
        try (SpLoggerCluster cluster = new SpLoggerCluster(this);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            String body1 = cluster.loadResource(DELIVERY_LOG1);
            String body2 = cluster.loadResource(DELIVERY_LOG2);
            long timeout = cluster.logStorage.storageConfig().batchSavePeriod();
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS1, body1, false);
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS2, body2, false);
            logger.info("testDeletesTwoFromMds: wait for " + timeout + "ms for upload blocks to MDS");
            cluster.sleep(timeout);
            String uri = SpLogger.DELETE_FROM_MDS + "?shard=0&stids=" + MdsRequestHandler.STID1 + ','
                + MdsRequestHandler.STID2;
            HttpGet get = new HttpGet(cluster.spLogger().host() + uri);
            logger.info("testDeletesTwoFromMds: " + cluster.spLogger().host() + uri);
            try (CloseableHttpResponse httpResponse = client.execute(get)) {
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, httpResponse);
            }
            logger.info("testDeletesTwoFromMds: wait for " + TIMEOUT + "ms for delete blocks from MDS");
            cluster.sleep(TIMEOUT);
            uri = URI_IN + "queueid=" + QUEUEID1 + "," + QUEUEID2;
            get = new HttpGet(cluster.spLogger().host() + uri);
            logger.info("testDeletesTwoFromMds: " + cluster.spLogger().host() + uri);
            try (CloseableHttpResponse httpResponse = client.execute(get)) {
                String response = CharsetUtils.toString(httpResponse.getEntity());
                logger.info("testDeletesTwoFromMds: GET response = '" + response + "', status = "
                    + httpResponse.getStatusLine().getStatusCode());
                HttpAssert.assertStatusCode(HttpStatus.SC_OK, httpResponse);
                YandexAssert.check(new StringChecker(""), response);
            }
        }
    }
}
