package ru.yandex.client.pg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.junit.Test;

import ru.yandex.http.util.EmptyFutureCallback;
import ru.yandex.http.util.FilterFutureCallback;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;
import ru.yandex.util.timesource.TimeSource;

public class PgClientTest extends TestBase {
    @Test
    public void test() throws Exception {
        try (PgClientCluster cluster = new PgClientCluster(this)) {
            cluster.client().executeOnMaster(
                new SqlQuery("init", "CREATE SCHEMA IF NOT EXISTS myschema"))
                .get();
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "create-table",
                    "CREATE TABLE myschema.mytable ("
                    + "id BIGINT NOT NULL PRIMARY KEY,"
                    + "age INTEGER NOT NULL,"
                    + "name TEXT NOT NULL)"))
                .get();
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "insert-data",
                    "INSERT INTO myschema.mytable VALUES"
                    + "(1, 5, 'toddler'),"
                    + "(2, 15, 'teenager'),"
                    + "(3, 99, 'elder')"))
                .get();
            logger.info("Sending sleep statements");
            long startSleep = TimeSource.INSTANCE.currentTimeMillis();
            List<Future<RowSet<Row>>> futures = new ArrayList<>();
            for (int i = 0; i < 90; ++i) {
                final int n = i;
                futures.add(
                    cluster.client().executeOnMaster(
                        new SqlQuery("sleep", "SELECT pg_sleep(5)"),
                        new FilterFutureCallback<>(
                            EmptyFutureCallback.INSTANCE)
                        {
                            @Override
                            public void completed(final RowSet<Row> rowSet) {
                                logger.info(
                                    "Sleep statement #" + n + " completed");
                            }
                        }));
            }
            Thread.sleep(1000L);
            logger.info("Sending select");
            long start = TimeSource.INSTANCE.currentTimeMillis();
            RowSet<Row> result =
                cluster.client().executeOnMaster(
                    new SqlQuery(
                        "select-data",
                        "SELECT name, age FROM myschema.mytable WHERE age > $1"
                        + " ORDER BY age DESC"),
                    Tuple.of(10),
                    new FilterFutureCallback<>(EmptyFutureCallback.INSTANCE) {
                        @Override
                        public void completed(final RowSet<Row> rowSet) {
                            logger.info("Select completed");
                        }
                    })
                    .get();
            long end = TimeSource.INSTANCE.currentTimeMillis();
            Thread.sleep(1000L);
            logger.info("Sending more sleep statements");
            for (int i = 0; i < 90; ++i) {
                final int n = i + 90;
                futures.add(
                    cluster.client().executeOnMaster(
                        new SqlQuery("sleep", "SELECT pg_sleep(5)"),
                        new FilterFutureCallback<>(
                            EmptyFutureCallback.INSTANCE)
                        {
                            @Override
                            public void completed(final RowSet<Row> rowSet) {
                                logger.info(
                                    "Sleep statement #" + n + " completed");
                            }
                        }));
            }
            for (Future<?> future: futures) {
                future.get();
            }
            long endSleep = TimeSource.INSTANCE.currentTimeMillis();
            YandexAssert.check(
                new JsonChecker(
                    "[[{\"age\":99,\"name\":\"elder\"},"
                    + "{\"age\":15,\"name\":\"teenager\"}]]"),
                PgClientCluster.toJsonString(result));
            YandexAssert.assertLess(1000L, end - start);
            YandexAssert.assertLess(13000L, endSleep - startSleep);
            YandexAssert.assertGreater(8000L, endSleep - startSleep);
        }
    }

    @Test
    public void testUnion() throws Exception {
        try (PgClientCluster cluster = new PgClientCluster(this)) {
            cluster.client().executeOnMaster(
                new SqlQuery("init", "CREATE SCHEMA IF NOT EXISTS myschema2"))
                .get();
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "create-table",
                    "CREATE TABLE myschema2.person ("
                    + "id BIGSERIAL PRIMARY KEY,"
                    + "age INTEGER NOT NULL,"
                    + "name TEXT NOT NULL)"))
                .get();
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "create-another-table",
                    "CREATE TABLE myschema2.structure ("
                    + "person_id BIGINT NOT NULL UNIQUE,"
                    + "manager_id BIGINT NOT NULL,"
                    + "FOREIGN KEY (person_id) "
                    + "REFERENCES myschema2.person (id)"
                    + "ON DELETE CASCADE,"
                    + "FOREIGN KEY (manager_id) "
                    + "REFERENCES myschema2.person (id)"
                    + "ON DELETE CASCADE,"
                    + "PRIMARY KEY(person_id, manager_id))"))
                .get();
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "insert-data",
                    "INSERT INTO myschema2.person (age, name) VALUES"
                    + "(20, 'junior'),"
                    + "(25, 'middle'),"
                    + "(35, 'senior'),"
                    + "(45, 'CTO')"))
                .get();
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "insert-more-data",
                    "INSERT INTO myschema2.structure (person_id,manager_id) "
                    + "VALUES (1, 3), (2, 3), (3, 4)"))
                .get();
            RowSet<Row> result =
                cluster.client().executeOnMaster(
                    new SqlQuery(
                        "select-data",
                        "SELECT id, name, NULL AS pid, NULL AS mid"
                        + " FROM myschema2.person"
                        + " WHERE age = 45"
                        + " UNION"
                        + " SELECT NULL AS id, NULL AS name, person_id AS pid,"
                        + " manager_id AS mid FROM myschema2.structure"
                        + " ORDER BY id, pid NULLS LAST"))
                .get();
            YandexAssert.check(
                new JsonChecker(
                    "[[{\"id\":4,\"name\":\"CTO\",\"pid\":null,\"mid\":null},"
                    + "{\"id\":null,\"name\":null,\"pid\":1,\"mid\":3},"
                    + "{\"id\":null,\"name\":null,\"pid\":2,\"mid\":3},"
                    + "{\"id\":null,\"name\":null,\"pid\":3,\"mid\":4}]]"),
                PgClientCluster.toJsonString(result));

            result =
                cluster.client().executeOnMaster(
                    new SqlQuery(
                        "join",
                        "SELECT p.name as p_name, m.name as m_name FROM"
                        + " myschema2.person p left join myschema2.structure s"
                        + " on p.id = s.person_id left join myschema2.person m"
                        + " on s.manager_id = m.id order by p_name desc"))
                .get();
            YandexAssert.check(
                new JsonChecker(
                    "[[{\"p_name\":\"senior\",\"m_name\":\"CTO\"},"
                    + "{\"p_name\":\"middle\",\"m_name\":\"senior\"},"
                    + "{\"p_name\":\"junior\",\"m_name\":\"senior\"},"
                    + "{\"p_name\":\"CTO\",\"m_name\":null}]]"),
                PgClientCluster.toJsonString(result));
        }
    }

    @Test
    public void testArrays() throws Exception {
        try (PgClientCluster cluster = new PgClientCluster(this)) {
            cluster.client().executeOnMaster(
                new SqlQuery("init", "CREATE SCHEMA IF NOT EXISTS myschema3"))
                .get();
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "create-table",
                    "CREATE TABLE myschema3.person ("
                    + "id BIGSERIAL PRIMARY KEY,"
                    + "name TEXT NOT NULL,"
                    + "aliases TEXT[] NOT NULL)"))
                .get();
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "insert-data",
                    "INSERT INTO myschema3.person (name, aliases) "
                    + "VALUES ('Dima', '{\"Dmitry\",\"Diman\"}')"))
                .get();
            SqlQuery selectAll =
                new SqlQuery(
                    "select-all",
                    "SELECT * FROM myschema3.person ORDER BY id");
            YandexAssert.check(
                new JsonChecker(
                    "[[{\"id\":1,\"name\":\"Dima\","
                    + "\"aliases\":[\"Dmitry\",\"Diman\"]}]]"),
                PgClientCluster.toJsonString(
                    cluster.client().executeOnMaster(selectAll).get()));
            // check select on any
            YandexAssert.check(
                new JsonChecker(
                    "[[{\"id\":1,\"name\":\"Dima\","
                        + "\"aliases\":[\"Dmitry\",\"Diman\"]}]]"),
                PgClientCluster.toJsonString(
                    cluster.client().executeOnAny(selectAll).get()));

            Tuple tuple = Tuple.tuple();
            tuple.addArrayOfString(
                new String[] {"Michael", "Mike", "Mishanya"});
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "insert-more-data",
                    "INSERT INTO myschema3.person (name, aliases) "
                    + "VALUES ('Misha', $1::TEXT[])"),
                tuple)
                .get();
            YandexAssert.check(
                new JsonChecker(
                    "[[{\"id\":1,\"name\":\"Dima\","
                    + "\"aliases\":[\"Dmitry\",\"Diman\"]},"
                    + "{\"id\":2,\"name\":\"Misha\","
                    + "\"aliases\":[\"Michael\",\"Mike\",\"Mishanya\"]}]]"),
                PgClientCluster.toJsonString(
                    cluster.client().executeOnMaster(selectAll).get()));
        }
    }

    @Test
    public void testAlterDefault() throws Exception {
        try (PgClientCluster cluster = new PgClientCluster(this)) {
            cluster.client().executeOnMaster(
                new SqlQuery("init", "CREATE SCHEMA IF NOT EXISTS myschema4"))
                .get();
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "create-table",
                    "CREATE TABLE myschema4.record ("
                    + "id BIGSERIAL PRIMARY KEY, value BIGINT NOT NULL)"))
                .get();
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "insert-data",
                    "INSERT INTO myschema4.record (value) "
                    + "VALUES (11)"))
                .get();
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "alter-table",
                    "ALTER TABLE myschema4.record "
                    + "ADD COLUMN number BIGINT"))
                .get();
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "alter-table",
                    "ALTER TABLE myschema4.record "
                    + "ALTER COLUMN number SET DEFAULT 5"))
                .get();
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "insert-more-data",
                    "INSERT INTO myschema4.record (value) "
                    + "VALUES (12)"))
                .get();
            SqlQuery selectAll =
                new SqlQuery(
                    "select-all",
                    "SELECT * FROM myschema4.record ORDER BY id");
            YandexAssert.check(
                new JsonChecker(
                    "[[{\"id\":1,\"value\":11,\"number\":null},"
                    + "{\"id\":2,\"value\":12,\"number\":5}]]"),
                PgClientCluster.toJsonString(
                    cluster.client().executeOnMaster(selectAll).get()));
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "fill-nulls",
                    "UPDATE myschema4.record "
                    + "SET number = 4 "
                    + "WHERE number is null"))
                .get();
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "alter-table",
                    "ALTER TABLE myschema4.record "
                    + "ALTER COLUMN number SET NOT NULL"))
                .get();
            cluster.client().executeOnMaster(
                new SqlQuery(
                    "insert-event-more-data",
                    "INSERT INTO myschema4.record (value) "
                    + "VALUES (13)"))
                .get();
            YandexAssert.check(
                new JsonChecker(
                    "[[{\"id\":1,\"value\":11,\"number\":4},"
                    + "{\"id\":2,\"value\":12,\"number\":5},"
                    + "{\"id\":3,\"value\":13,\"number\":5}]]"),
                PgClientCluster.toJsonString(
                    cluster.client().executeOnMaster(selectAll).get()));
        }
    }
}

