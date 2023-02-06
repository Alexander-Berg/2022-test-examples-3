package ru.yandex.mail.so.logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.mail.so.logger.config.EnvironmentType;
import ru.yandex.mail.so.logger.config.MongoRulesStatDatabaseConfig;

public class SpLoggerRulesStatTest extends SpLoggerTestBase {

    private void testOnePutWithMongo(final String uri, final boolean lzoCompressed) throws Exception {
        try (SpLoggerRulesStatCluster cluster = new SpLoggerRulesStatCluster(this);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            mongoDbStartUp(cluster, Route.IN);
            String body = cluster.loadResource(DELIVERY_LOG1);
            long timeout = cluster.rulesStatDatabaseConfig().batchSavePeriod();
            cluster.sleep(timeout); // wait for finishing of mongo background startup process
            testOnePutRequest(cluster, client, uri, body, lzoCompressed);
            logger.info("testOnePutWithMongo: wait for " + timeout + "ms");
            cluster.sleep(cluster.logStorage.storageConfig().batchSavePeriod());
            testSignal(cluster, cluster.logStorageSectionName() + "-batch-save-requests_ammm", 1);
            logger.info("testOnePutWithMongo: wait for " + timeout + "ms");
            cluster.sleep(timeout);
            testMongoDbRequest(cluster, Route.IN, DELIVERY_LOG1);
        }
    }

    @Test
    public void testOnePutDeliveryInWithMongo() throws Exception {
        testOnePutWithMongo(URI_IN + URI_PUT_PARAMS1, false);
    }

    @Test
    public void testOneCheckWithMongo() throws Exception {
        testOnePutWithMongo(URI_PUT + URI_PUT_PARAMS1, false);
    }

    @Test
    public void testOneCompressedCheckWithMongo() throws Exception {
        testOnePutWithMongo(URI_PUT + URI_PUT_PARAMS1, true);
    }

    private void testOnePrefixedGetWithMongo(final String getUri, final boolean lzoCompressed) throws Exception {
        try (SpLoggerRulesStatCluster cluster = new SpLoggerRulesStatCluster(this, MDS_BATCH_SIZE_MIN);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            mongoDbStartUp(cluster, Route.IN);
            String body = cluster.loadResource(DELIVERY_LOG1);
            long timeout = cluster.rulesStatDatabaseConfig().batchSavePeriod();
            logger.info("testOnePrefixedGetWithMongo: wait for " + timeout + "ms");
            cluster.sleep(timeout); // wait for finishing of mongo background startup process
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS1, body, lzoCompressed);
            logger.info("testOnePrefixedGetWithMongo: wait for "
                + cluster.logStorage.storageConfig().batchSavePeriod() + "ms");
            cluster.sleep(cluster.logStorage.storageConfig().batchSavePeriod());
            testSignal(cluster, cluster.logStorageSectionName() + "-batch-save-requests_ammm", 1);
            testSearchRequest(cluster, client, getUri, Set.of(body));
            logger.info("testOnePrefixedGetWithMongo: wait for " + timeout + "ms");
            cluster.sleep(timeout);
            testMongoDbRequest(cluster, Route.IN, DELIVERY_LOG1);
        }
    }

    @Test
    public void testOnePrefixedGetDeliveryInWithMongo() throws Exception {
        testOnePrefixedGetWithMongo(URI_IN + "queueid=" + QUEUEID1, false);
    }

    @Test
    public void testOnePrefixedSearchWithMongo() throws Exception {
        testOnePrefixedGetWithMongo(URI_SEARCH_IN + "queueid=" + QUEUEID1, false);
    }

    @Test
    public void testOnePrefixedSearchForGetByIdWithMongo() throws Exception {
        testOnePrefixedGetWithMongo(URI_SEARCH_GETBYID + "queueid=" + QUEUEID1, false);
    }

    @Test
    public void testOneCompressedPrefixedSearchWithMongo() throws Exception {
        testOnePrefixedGetWithMongo(URI_SEARCH_IN + "queueid=" + QUEUEID1, true);
    }

    @Test
    public void testTwoPrefixedGetDeliveryInWithMongo() throws Exception {
        try (SpLoggerRulesStatCluster cluster = new SpLoggerRulesStatCluster(this, MDS_BATCH_SIZE_MIDDLE);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            mongoDbStartUp(cluster, Route.IN);
            String body1 = cluster.loadResource(DELIVERY_LOG1);
            String body2 = cluster.loadResource(DELIVERY_LOG2);
            Set<String> bodies = Set.of(body1, body2);
            //long timeout = cluster.rulesStatDatabaseConfig().batchSavePeriod();
            //logger.info("testTwoPrefixedGetDeliveryInWithMongo: wait for " + timeout + "ms");
            //cluster.sleep(timeout); // wait for finishing of mongo background startup process
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS1, body1, false);
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS2, body2, false);
            logger.info("testTwoPrefixedGetDeliveryIn: wait for "
                + cluster.logStorage.storageConfig().batchSavePeriod() + "ms");
            cluster.sleep(cluster.logStorage.storageConfig().batchSavePeriod());
            testSignal(cluster, cluster.logStorageSectionName() + "-batch-save-requests_ammm", 1);
            testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID1, Set.of(body1));
            testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID2, Set.of(body2));
            testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID1 + "," + QUEUEID2, bodies);
            testMongoDbRequest(cluster, Route.IN, DELIVERY_LOG1, DELIVERY_LOG2);
        }
    }

    @Test
    public void testTwoPrefixedGetDeliveryInExceededBatchSizeWithMongo() throws Exception {
        try (SpLoggerRulesStatCluster cluster = new SpLoggerRulesStatCluster(this, MDS_BATCH_SIZE_EXCEEDED);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            mongoDbStartUp(cluster, Route.IN);
            String body1 = cluster.loadResource(DELIVERY_LOG1);
            String body2 = cluster.loadResource(DELIVERY_LOG2);
            long timeout = cluster.rulesStatDatabaseConfig().batchSavePeriod();
            //logger.info("testTwoPrefixedGetDeliveryInExceededBatchSizeWithMongo: wait for " + timeout + "ms");
            //cluster.sleep(timeout); // wait for finishing of mongo background startup process
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS1, body1, false);
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS2, body2, false);
            testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID1, Set.of());
            testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID2, Set.of());
            testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID1 + "," + QUEUEID2, Set.of());
            logger.info("testTwoPrefixedGetDeliveryInExceededBatchSizeWithMongo: wait for "
                 + cluster.logStorage.storageConfig().batchSavePeriod() + "ms");
            cluster.sleep(cluster.logStorage.storageConfig().batchSavePeriod());
            testSignal(cluster, cluster.logStorageSectionName()+ "-batch-save-requests_ammm", 1);
            testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID1, Set.of(body1));
            testSearchRequest(cluster, client, URI_IN + "queueid=" + QUEUEID2, Set.of(body2));
            testSearchRequest(
                cluster,
                client,
                URI_IN + "queueid=" + QUEUEID1 + "," + QUEUEID2,
                Set.of(body1, body2));
            logger.info("testTwoPrefixedGetDeliveryInExceededBatchSizeWithMongo: wait for " + timeout + "ms");
            cluster.sleep(timeout);
            testMongoDbRequest(cluster, Route.IN, DELIVERY_LOG1, DELIVERY_LOG2);
        }
    }

    @Test
    public void testUnprefixedHandleGetDeliveryInWithMongo() throws Exception {
        try (SpLoggerRulesStatCluster cluster = new SpLoggerRulesStatCluster(this, MDS_BATCH_SIZE_MIDDLE);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            mongoDbStartUp(cluster, Route.IN);
            String logBody = cluster.loadResource(DELIVERY_LOG1);
            long timeout = cluster.rulesStatDatabaseConfig().batchSavePeriod();
            logger.info("testUnprefixedHandleGetDeliveryInWithMongo: wait for " + timeout + "ms");
            cluster.sleep(timeout); // wait for finishing of mongo background startup process
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
            // put data into storage & index
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS1, logBody, false);
            logger.info("testUnprefixedHandleGetDeliveryIn: wait for "
                + cluster.logStorage.storageConfig().batchSavePeriod() + "ms");
            cluster.sleep(cluster.logStorage.storageConfig().batchSavePeriod());
            testSignal(cluster, cluster.logStorageSectionName() + "-batch-save-requests_ammm", 1);
            // test presence of data in staorage & index
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
            testMongoDbRequest(cluster, Route.IN, DELIVERY_LOG1);
        }
    }

    @Test
    public void testTwoUnprefixedGetDeliveryInWithMongo1() throws Exception {
        try (SpLoggerRulesStatCluster cluster = new SpLoggerRulesStatCluster(this, MDS_BATCH_SIZE_MIDDLE);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            mongoDbStartUp(cluster, Route.IN);
            String body1 = cluster.loadResource(DELIVERY_LOG1);
            String body2 = cluster.loadResource(DELIVERY_LOG2);
            long timeout = cluster.rulesStatDatabaseConfig().batchSavePeriod();
            //logger.info("testTwoUnprefixedGetDeliveryInWithMongo1: wait for " + timeout + "ms");
            //cluster.sleep(timeout); // wait for finishing of mongo background startup process
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS1, body1, false);
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS2, body2, false);
            logger.info("testTwoUnprefixedGetDeliveryInWithMongo1: wait for "
                + cluster.logStorage.storageConfig().batchSavePeriod() + "ms");
            cluster.sleep(cluster.logStorage.storageConfig().batchSavePeriod());
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
            logger.info("testTwoUnprefixedGetDeliveryInWithMongo1: wait for " + timeout + "ms");
            cluster.sleep(timeout);
            testMongoDbRequest(cluster, Route.IN, DELIVERY_LOG1, DELIVERY_LOG2);
        }
    }

    @Test
    public void testTwoUnprefixedGetDeliveryInWithMongo2() throws Exception {
        try (SpLoggerRulesStatCluster cluster = new SpLoggerRulesStatCluster(this, MDS_BATCH_SIZE_MIDDLE);
             CloseableHttpClient client = Configs.createDefaultClient())
        {
            cluster.start();
            mongoDbStartUp(cluster, Route.IN);
            String body1 = cluster.loadResource(DELIVERY_LOG1);
            String body2 = cluster.loadResource(DELIVERY_LOG2);
            //long timeout = cluster.rulesStatDatabaseConfig().batchSavePeriod();
            //logger.info("testTwoUnprefixedGetDeliveryInWithMongo2: wait for " + timeout + "ms");
            //cluster.sleep(timeout); // wait for finishing of mongo background startup process
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS1, body1, false);
            testOnePutRequest(cluster, client, URI_IN + URI_PUT_PARAMS2, body2, false);
            logger.info("testTwoUnprefixedGetDeliveryInWithMongo2: wait for "
                + cluster.logStorage.storageConfig().batchSavePeriod() + "ms");
            cluster.sleep(cluster.logStorage.storageConfig().batchSavePeriod());
            testSignal(cluster, cluster.logStorageSectionName() + "-batch-save-requests_ammm", 1);
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
            testMongoDbRequest(cluster, Route.IN, DELIVERY_LOG1, DELIVERY_LOG2);
        }
    }

    void testMongoDbRequest(final SpLoggerRulesStatCluster cluster, final Route route, final String... deliveryLogPaths)
        throws Exception
    {
        Map<Long, Map<String, Map<String, Long>>> rulesStat = new HashMap<>();
        for (String deliveryLogPath : deliveryLogPaths) {
            SpDaemonRulesStatBatch.parseRulesStatistics(cluster.loadResource(deliveryLogPath), route, rulesStat);
        }
        MongoRulesStatDatabase rulesDbClient =
            (MongoRulesStatDatabase) cluster.spLogger().rulesStatDatabase(cluster.rulesStatSectionName());
        //logger.info("testMongoDbRequest: start waiting for clear currentBatch");
        synchronized (rulesDbClient.currentBatch()) {
            while (!rulesDbClient.currentBatch().isEmpty()) {
                try {
                    rulesDbClient.currentBatch().wait(TIMEOUT << 2);
                } catch (InterruptedException e) {
                    break;
                }
            }
            //logger.info("testMongoDbRequest: rulesDbClient's currentBatch cleared");
        }
        testMongoDbRulesStatistics(cluster, route, rulesStat);
    }

    @SuppressWarnings("unused")
    void testMongoDbRequestBodies(final SpLoggerRulesStatCluster cluster, final Route route, final String... bodies)
        throws Exception
    {
        Map<Long, Map<String, Map<String, Long>>> rulesStat = new HashMap<>();
        for (String body : bodies) {
            SpDaemonRulesStatBatch.parseRulesStatistics(body, route, rulesStat);
        }
        testMongoDbRulesStatistics(cluster, route, rulesStat);
    }

    void testMongoDbRulesStatistics(
        final SpLoggerRulesStatCluster cluster,
        final Route route,
        final Map<Long, Map<String, Map<String, Long>>> rulesStat)
        throws IOException
    {
        MongoRulesStatDatabaseConfig mongoRulesStatDatabaseConfig = (MongoRulesStatDatabaseConfig)
            cluster.spLogger().config().rulesStatDatabasesConfig().rulesStatDatabases()
                .get(cluster.rulesStatSectionName());
        try (MongoClient mongoClient = mongoRulesStatDatabaseConfig.createMongoClient()) {
            logger.info("testMongoDbRulesStatistics: mongodb cluster description = "
                + mongoClient.getClusterDescription() + ", port = " + cluster.db().port());
            long counter;
            MongoCollection<Document> collection;
            MongoDatabase db = mongoClient.getDatabase(mongoRulesStatDatabaseConfig.dbName());
            OperationSubscriber<Document> findSubscriber;
            Map<String, Map<String, Map<String, Long>>> dailyInfo = new HashMap<>();
            collection = db.getCollection(
                "detailed_" + MongoRulesStatDatabase.tableName(route, EnvironmentType.TESTING));
            OperationSubscriber<Document> findAllSubscriber = new OperationSubscriber<>("find all records");
            collection.find(new Document()).subscribe(findAllSubscriber);
            List<Document> data = findAllSubscriber.get();
            logger.info("testMongoDbRulesStatistics: all records = " + data);
            for (Map.Entry<Long, Map<String, Map<String, Long>>> entry : rulesStat.entrySet()) {
                String date = MongoRulesStatDatabase.isoDate(entry.getKey());
                dailyInfo.computeIfAbsent(date, x -> new HashMap<>());
                for (Map.Entry<String, Map<String, Long>> ruleStat : entry.getValue().entrySet()) {
                    dailyInfo.get(date).computeIfAbsent(ruleStat.getKey(), x -> new HashMap<>());
                    for (Map.Entry<String, Long> info : ruleStat.getValue().entrySet()) {
                        if (dailyInfo.get(date).get(ruleStat.getKey()).containsKey(info.getKey())) {
                            counter = dailyInfo.get(date).get(ruleStat.getKey()).get(info.getKey());
                            dailyInfo.get(date).get(ruleStat.getKey()).put(info.getKey(), counter + info.getValue());
                        } else {
                            dailyInfo.get(date).get(ruleStat.getKey()).put(info.getKey(), info.getValue());
                        }
                    }
                    findSubscriber =
                        new OperationSubscriber<>("find {rule=" + ruleStat.getKey() + ", time=" + entry.getKey() + '}');
                    collection.find(Filters.and(
                        Filters.eq("rule", ruleStat.getKey()),
                        Filters.eq("time", (int) (long) entry.getKey()))
                    ).projection(Projections.fields(
                        Projections.exclude("rule", "time"),
                        Projections.excludeId()
                    )).subscribe(findSubscriber);
                    List<Document> findResults = findSubscriber.get();
                    logger.info("testMongoDbRulesStatistics: findResults=" + findResults + " (size="
                        + findResults.size() + ") for rule=" + ruleStat.getKey() + ", time=" + entry.getKey());
                    Assert.assertEquals(1, findResults.size());
                    assertMapsEquals(
                        "Unequal statistics for rule " + ruleStat.getKey(),
                        ruleStat.getValue(), findResults.get(0));
                }
            }
            collection = db.getCollection(MongoRulesStatDatabase.tableName(route, EnvironmentType.TESTING));
            for (Map.Entry<String, Map<String, Map<String, Long>>> dayStat : dailyInfo.entrySet()) {
                for (Map.Entry<String, Map<String, Long>> ruleStat : dayStat.getValue().entrySet()) {
                    findSubscriber =
                        new OperationSubscriber<>(
                            "find {rule=" + ruleStat.getKey() + ", date=" + dayStat.getKey() + '}');
                    collection.find(Filters.and(
                        Filters.eq("rule", ruleStat.getKey()),
                        Filters.eq("date", dayStat.getKey()))
                    ).projection(Projections.fields(
                        Projections.exclude("rule", "date"),
                        Projections.excludeId()
                    )).subscribe(findSubscriber);
                    List<Document> findResults = findSubscriber.get();
                    logger.info("testMongoDbRulesStatistics: findResults=" + findResults + " (size="
                        + findResults.size() + ") for rule=" + ruleStat.getKey() + ", date=" + dayStat.getKey());
                    Assert.assertEquals(1, findResults.size());
                    assertMapsEquals(
                        "Unequal statistics for rule " + ruleStat.getKey(),
                        ruleStat.getValue(), findResults.get(0));
                }
            }
        }
    }

    protected void mongoDbStartUp(final SpLoggerRulesStatCluster cluster, final Route route) {
        MongoRulesStatDatabaseConfig mongoRulesStatDatabaseConfig = (MongoRulesStatDatabaseConfig)
            cluster.spLogger().config().rulesStatDatabasesConfig().rulesStatDatabases()
                .get(cluster.rulesStatSectionName());
        try (MongoClient mongoClient = mongoRulesStatDatabaseConfig.createMongoClient()) {
            logger.info("mongoDbStartUp: start");
            Map<String, String> tablePeculiarity = Map.of(
                "detailed_", "time",
                "", "date"
            );
            MongoDatabase db = mongoClient.getDatabase(mongoRulesStatDatabaseConfig.dbName());
            MongoCollection<Document> collection;
            List<String> indexNames;
            for (Map.Entry<String, String> tp : tablePeculiarity.entrySet()) {
                collection = db.getCollection(
                    tp.getKey() + MongoRulesStatDatabase.tableName(route, EnvironmentType.TESTING));
                OperationSubscriber<String> indexNameSubscriber = new OperationSubscriber<>("get index name");
                collection.createIndex(
                    new Document().append("rule", 1).append(tp.getValue(), 1),
                    new IndexOptions().unique(true))
                    .subscribe(indexNameSubscriber);
                indexNames = indexNameSubscriber.get();
                logger.info("mongoDbStartUp: index names = " + indexNames);
            }
            logger.info("mongoDbStartUp: finished");
        }
    }
}
