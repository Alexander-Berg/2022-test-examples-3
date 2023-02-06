package ru.yandex.search.migrations_worker;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.ProxyMultipartHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.msearch.proxy.MsearchProxyCluster;
import ru.yandex.msearch.proxy.MsearchProxyTestBase;
import ru.yandex.msearch.proxy.api.async.mail.subscriptions.update.dao.pojo.MigrationTask;
import ru.yandex.msearch.proxy.api.async.mail.subscriptions.update.dao.pojo.MigrationTaskStatus;
import ru.yandex.search.prefix.LongPrefix;

public class TasksPlannerTest extends MsearchProxyTestBase {
    private static final long DB_LAG_TIME_MS = 1000;

    @Test
    public void testOptInWithMoveExisting() throws Exception {
        MsearchProxyCluster.MproxyClusterContext context =
                new MsearchProxyCluster.MproxyClusterContext();
        context.producer(true, true);
        context.useMops(true);
        context.usePostgres(true);

        try (MsearchProxyCluster cluster =
                     new MsearchProxyCluster(
                             this,
                             MsearchProxyCluster.PROD_CONFIG,
                             context))
        {
            cluster.start();

            cluster.producer().add("/_status*", "[{$localhost\0:100500}]");
            cluster.producer().add(
                    "/update*",
                    new StaticHttpResource(
                            new ProxyMultipartHandler(cluster.backend().indexerPort())));

            LongPrefix prefix = new LongPrefix(379079136L);
            cluster.backend().add(
                    prefix,
                    "\"mid\": 1, \"hid\": 0, \"fid\":10, \"message_type\": \"13\", \"url\":1, \"hdr_from_normalized\":\"news@beru.ru\"",
                    "\"mid\": 2, \"url\":2, \"hid\": 0, \"fid\":11, \"message_type\": \"13\",\"hdr_from_normalized\":\"news@beru.ru\"",
                    "\"mid\": 3, \"url\":3, \"hid\": 0, \"fid\":10, \"message_type\": \"13\",\"hdr_from_normalized\":\"news@beru.ru\"");

            cluster.backend().add(
                    prefix,
                    "\"mid\": 4, \"url\":4,\"hid\": 0,  \"fid\": 12, \"message_type\": \"13\", \"hdr_from_normalized\":\"news@petya.ru\"");

            String foldersResponse =
                    "{\n" +
                            "  \"folders\": {\n" +
                            "    \"1\": {\n" +
                            "      \"name\": \"Inbox\",\n" +
                            "      \"isUser\": false,\n" +
                            "      \"isSystem\": true,\n" +
                            "      \"type\": {\n" +
                            "        \"code\": 3,\n" +
                            "        \"title\": \"system\"\n" +
                            "      },\n" +
                            "      \"symbolicName\": {\n" +
                            "        \"code\": 1,\n" +
                            "        \"title\": \"inbox\"\n" +
                            "      }\n" +
                            "    },\n" +
                            "    \"10\": {\n" +
                            "      \"symbolicName\": {\n" +
                            "        \"title\": \"pending\",\n" +
                            "        \"code\": 11\n" +
                            "      },\n" +
                            "      \"type\": {\n" +
                            "        \"title\": \"system\",\n" +
                            "        \"code\": 3\n" +
                            "      },\n" +
                            "      \"isSystem\": true,\n" +
                            "      \"isUser\": false,\n" +
                            "      \"name\": \"pending\"\n" +
                            "    }\n" +
                            "  }\n" +
                            "}";

            String furitaResponse =
                    "{\n" +
                            "  \"session\": \"LPYcA11pZ4Y1\",\n" +
                            "  \"rules\": [\n" +
                            "    {\n" +
                            "      \"id\": \"103178\",\n" +
                            "      \"name\": \"Письма из ящика vonidu@gmail.com\",\n" +
                            "      \"priority\": 0,\n" +
                            "      \"stop\": false,\n" +
                            "      \"enabled\": true,\n" +
                            "      \"created\": 1517224936,\n" +
                            "      \"type\": \"user\",\n" +
                            "      \"actions\": [\n" +
                            "        {\n" +
                            "          \"type\": \"movel\",\n" +
                            "          \"parameter\": \"127\",\n" +
                            "          \"verified\": true\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"query\": \"headers:X-yandex-rpop-id\\\\:\\\\ *2286471* AND NOT folder_type: spam\"\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"id\": \"269253\",\n" +
                            "      \"name\": \"\",\n" +
                            "      \"priority\": 3,\n" +
                            "      \"stop\": false,\n" +
                            "      \"enabled\": true,\n" +
                            "      \"created\": 1628528406,\n" +
                            "      \"type\": \"user\",\n" +
                            "      \"actions\": [\n" +
                            "        {\n" +
                            "          \"type\": \"move\",\n" +
                            "          \"parameter\": \"40\",\n" +
                            "          \"verified\": true\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"query\": \"((hdr_from_email:\\\"noreply@utkonos.ru\\\" OR " +
                            "hdr_from_display_name:\\\"noreply@utkonos.ru\\\") OR (hdr_from_email:\\\"auto@utkonos.ru\\\" OR " +
                            "hdr_from_display_name:\\\"auto@utkonos.ru\\\")) AND NOT folder_type: spam\"\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"id\": \"269254\",\n" +
                            "      \"name\": \"\",\n" +
                            "      \"priority\": 1,\n" +
                            "      \"stop\": true,\n" +
                            "      \"enabled\": true,\n" +
                            "      \"created\": 1628528410,\n" +
                            "      \"type\": \"user\",\n" +
                            "      \"actions\": [\n" +
                            "        {\n" +
                            "          \"type\": \"move\",\n" +
                            "          \"parameter\": \"30\",\n" +
                            "          \"verified\": true\n" +
                            "        },\n" +
                            "        {\n" +
                            "          \"type\": \"status\",\n" +
                            "          \"parameter\": \"RO\",\n" +
                            "          \"verified\": true\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"query\": \"(hdr_from_email:\\\"no\\\\-reply@taxi.yandex.ru\\\" OR " +
                            "hdr_from_display_name:\\\"no\\\\-reply@taxi.yandex.ru\\\") AND NOT folder_type: spam\"\n" +
                            "    },\n" +
                            "    {\n" +
                            "      \"id\": \"270156\",\n" +
                            "      \"name\": \"\",\n" +
                            "      \"priority\": 4,\n" +
                            "      \"stop\": false,\n" +
                            "      \"enabled\": true,\n" +
                            "      \"created\": 1630956280,\n" +
                            "      \"type\": \"user\",\n" +
                            "      \"actions\": [\n" +
                            "        {\n" +
                            "          \"type\": \"move\",\n" +
                            "          \"parameter\": \"40\",\n" +
                            "          \"verified\": true\n" +
                            "        }\n" +
                            "      ],\n" +
                            "      \"query\": \"hdr_subject_keyword:*Нет\\\\ таких\\\\ тем\\\\ ну\\\\ вот\\\\ совсем\\\\\\\\?* " +
                            "AND NOT folder_type: spam\"\n" +
                            "    }\n" +
                            "  ]\n" +
                            "}\n";

            cluster.furita().add("/api/list.json?db=pg&detailed=1&uid=379079136", furitaResponse);

            cluster.filterSearch().add(
                    "/folders?caller=msearch&mdb=pg&uid=379079136",
                    foldersResponse);

            String fsUri =
                    "/filter_search?&uid=379079136&mdb=pg&full_folders_and_labels=1&incl_folders=pending&order=default";
            filterSearch(cluster, fsUri, "1", "3");
            cluster.tupita().add(
                    "/check?subscriptions&uid=379079136",
                    "{\"result\":[{\"uid\":379079136, \"matched_queries\":[\"270156\"]}]}");
            cluster.mops().add(
                    "/complex_move?&subscription_activate_subs_optin&dest_fid=40&with_sent=0" +
                            "&uid=379079136&request_mids_count=2&source=mail_search&mids=1,3",
                    HttpStatus.SC_OK,
                    "{\"result\": \"ok\"}");

            TasksPlanner tasksPlanner = new TasksPlanner(
                    cluster.proxy(),
                    new MigrationsWorkerLock(cluster.proxy()),
                    cluster.proxy().migrationsTasksDao()
            );

            cluster.proxy().migrationsTasksDao().insert(Stream.of(
                    "news@beru.ru",
                    "news@petya.ru",
                    "news@empty.ru"
            ).map(email ->
                    new MigrationTask(
                            System.currentTimeMillis(),
                            379079136L,
                            email,
                            "activate",
                            "created",
                            "6wr53kUrLswisXh",
                            true,
                            "100,13",
                            System.currentTimeMillis(),
                            0
                    )
            ).collect(Collectors.toList()));
            Assert.assertTrue(tasksPlanner.tryToRun());
            Thread.sleep(DB_LAG_TIME_MS); // due to internal table update lag
            cluster.proxy()
                    .migrationsTasksDao()
                    .findAll()
                    .forEach(task -> Assert.assertEquals(MigrationTaskStatus.FINISHED, task.getStatus()));

            cluster.mops().add("/remove?&subscription_hide_with_remove&nopurge=1&uid=379079136&request_mids_count=3&source=mail_search&mids=1,2,3", HttpStatus.SC_OK, "{\"result\": \"ok\"}");
            cluster.mops().add("/remove?&subscription_hide_with_remove&nopurge=1&uid=379079136&request_mids_count=1&source=mail_search&mids=4", HttpStatus.SC_OK, "{\"result\": \"ok\"}");

            cluster.proxy().migrationsTasksDao().insert(Stream.of(
                    "news@beru.ru",
                    "news@petya.ru",
                    "news@empty.ru"
            ).map(email ->
                    new MigrationTask(
                            System.currentTimeMillis(),
                            379079136L,
                            email,
                            "hide",
                            "created",
                            "6wr53kUrLswisXh",
                            true,
                            "100,13",
                            System.currentTimeMillis(),
                            0
                    )
            ).collect(Collectors.toList()));
            Assert.assertTrue(tasksPlanner.tryToRun());
            Thread.sleep(DB_LAG_TIME_MS); // due to internal table update lag
            cluster.proxy()
                    .migrationsTasksDao()
                    .findAll()
                    .forEach(task -> Assert.assertEquals(MigrationTaskStatus.FINISHED, task.getStatus()));

        }
    }

    @Test
    public void testUnsubscribeWithMoveExisting() throws Exception {
        MsearchProxyCluster.MproxyClusterContext context =
                new MsearchProxyCluster.MproxyClusterContext();
        context.producer(true, true);
        context.useMops(true);
        context.usePostgres(true);

        try (MsearchProxyCluster cluster =
                     new MsearchProxyCluster(
                             this,
                             MsearchProxyCluster.PROD_CONFIG,
                             context))
        {
            cluster.start();

            cluster.producer().add("/_status*", "[{$localhost\0:100500}]");
            cluster.producer().add(
                    "/update*",
                    new StaticHttpResource(
                            new ProxyMultipartHandler(cluster.backend().indexerPort())));

            LongPrefix prefix = new LongPrefix(379079136L);
            cluster.backend().add(
                    prefix,
                    "\"mid\": 1, \"hid\": 0, \"message_type\": \"13\", \"url\":1, \"hdr_from_normalized\":\"news@beru.ru\"",
                    "\"mid\": 2, \"url\":2, \"hid\": 0, \"message_type\": \"13\",\"hdr_from_normalized\":\"news@beru.ru\"",
                    "\"mid\": 3, \"url\":3, \"hid\": 0, \"message_type\": \"13\",\"hdr_from_normalized\":\"news@beru.ru\"");

            cluster.backend().add(
                    prefix,
                    "\"mid\": 4, \"url\":4,\"hid\": 0, \"message_type\": \"13\", \"hdr_from_normalized\":\"news@petya.ru\"");

            cluster.mops().add("/remove?&subscription_hide_with_remove&nopurge=1&uid=379079136&request_mids_count=3&source=mail_search&mids=1,2,3", HttpStatus.SC_OK, "{\"result\": \"ok\"}");
            cluster.mops().add("/remove?&subscription_hide_with_remove&nopurge=1&uid=379079136&request_mids_count=1&source=mail_search&mids=4", HttpStatus.SC_OK, "{\"result\": \"ok\"}");

            TasksPlanner tasksPlanner = new TasksPlanner(
                    cluster.proxy(),
                    new MigrationsWorkerLock(cluster.proxy()),
                    cluster.proxy().migrationsTasksDao()
            );

            cluster.proxy().migrationsTasksDao().insert(Stream.of(
                    "news@beru.ru",
                    "news@petya.ru",
                    "news@empty.ru"
            ).map(email ->
                    new MigrationTask(
                            System.currentTimeMillis(),
                            379079136L,
                            email,
                            "hide",
                            "created",
                            "6wr53kUrLswisXh",
                            false,
                            "13",
                            System.currentTimeMillis(),
                            0
                    )
            ).collect(Collectors.toList()));
            Assert.assertTrue(tasksPlanner.tryToRun());
            Thread.sleep(DB_LAG_TIME_MS); // due to internal table update lag
            cluster.proxy()
                    .migrationsTasksDao()
                    .findAll()
                    .forEach(task -> Assert.assertEquals(MigrationTaskStatus.FINISHED, task.getStatus()));
        }
    }
}
