package ru.yandex.client.pg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.http.HttpHost;

import ru.yandex.client.pg.config.PgClientConfigBuilder;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.json.writer.JsonType;
import ru.yandex.logger.PrefixedLogger;
import ru.yandex.test.util.TestBase;

public class PgClientCluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    private final Vertx vertx;
    private final PgClient client;

    public PgClientCluster(final TestBase testBase) throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            vertx =
                Vertx.vertx();
            chain.get().add(vertx::close);

            client =
                new PgClient(
                    vertx,
                    new PgClientConfigBuilder()
                        .hosts(
                            Collections.singletonList(
                                new HttpHost(
                                    "localhost",
                                    Integer.parseInt(
                                        System.getenv("PG_LOCAL_PORT")))))
                        .connections(100)
                        .database(System.getenv("PG_LOCAL_DATABASE"))
                        .user(System.getenv("PG_LOCAL_USER"))
                        .password(System.getenv("PG_LOCAL_PASSWORD"))
                        .queueSize(200)
                        .preparedStatementsCacheSize(10)
                        .timeout(20000)
                        .poolTimeout(20000)
                        .idleTimeout(20000)
                        .healthCheckInterval(1000L)
                        .build(),
                    testBase.logger());
            chain.get().add(client);

            reset(chain.release());
        }
    }

    public PgClient client() {
        return client;
    }

    public void executeScript(
        final String script,
        final PrefixedLogger logger)
        throws Exception
    {
        int pos = 0;
        for (String subscript: script.split(";")) {
            if (!subscript.trim().isEmpty()) {
                logger.info(
                    "Executing subscript at position " + pos
                    + ':' + '\n' + subscript.trim() + '\n');
                client.executeOnMaster(new SqlQuery("subscript", subscript))
                    .get();
            }
            pos += subscript.length();
            // semicolon
            ++pos;
        }
    }

    public static String toJsonString(RowSet<Row> rowSet) {
        List<Object> rowSets = new ArrayList<>();
        while (rowSet != null) {
            List<Object> rows = new ArrayList<>();
            for (Row row: rowSet) {
                rows.add(row.toJson().getMap());
            }
            rowSets.add(rows);
            rowSet = rowSet.next();
        }
        return JsonType.HUMAN_READABLE.toString(rowSets);
    }
}

