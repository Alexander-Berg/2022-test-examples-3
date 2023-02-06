package ru.yandex.direct.useractionlog.writer.initdictionaries;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.DSLContext;
import org.jooq.InsertValuesStep3;
import org.jooq.Record3;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.dbschema.ppc.tables.records.OrgDetailsRecord;
import ru.yandex.direct.mysql.MySQLServerBuilder;
import ru.yandex.direct.mysql.TmpMySQLServerWithDataDir;
import ru.yandex.direct.test.mysql.DirectMysqlDb;
import ru.yandex.direct.test.mysql.TestMysqlConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.impl.DSL.constraint;
import static ru.yandex.direct.dbschema.ppc.Tables.ORG_DETAILS;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class JooqChunkReaderTest {
    private static final List<Record3<Long, Long, String>> ALL_RECORDS = LongStream.range(1L, 101L)
            .mapToObj(i -> new OrgDetailsRecord(i, i % 7L, String.format("name %d", i)))
            .collect(Collectors.toList());
    private static TmpMySQLServerWithDataDir mysql;
    private int chunkSize;

    public JooqChunkReaderTest(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    @BeforeClass
    public static void setUpClass() throws InterruptedException {
        MySQLServerBuilder serverBuilder = new DirectMysqlDb(TestMysqlConfig.directConfig())
                .useSandboxMysqlServerIfPossible(new MySQLServerBuilder());
        assertThat(serverBuilder.mysqldIsAvailable()).isTrue();
        mysql = TmpMySQLServerWithDataDir.create("master", serverBuilder);
    }

    @AfterClass
    public static void tearDownClass() {
        if (mysql != null) {
            mysql.close();
        }
    }

    @Parameterized.Parameters(name = "chunkSize={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {ALL_RECORDS.size() / 10},
                {23},  // prime number
                {ALL_RECORDS.size()},
                {ALL_RECORDS.size() + 1}
        });
    }

    private static void testWithSimpleTable(Consumer<DSLContext> body) throws InterruptedException, SQLException {
        try (Connection mysqlConnection = mysql.connect()) {
            mysqlConnection.createStatement().execute("CREATE DATABASE ppc");
        }

        try (Connection mysqlConnection = mysql.connect()) {
            mysqlConnection.setCatalog("ppc");
            DSLContext dsl = DSL.using(mysqlConnection, SQLDialect.MYSQL);
            dsl.createTable(ORG_DETAILS)
                    .column(ORG_DETAILS.ORG_DETAILS_ID)
                    .column(ORG_DETAILS.UID)
                    .column(ORG_DETAILS.OGRN)
                    .constraint(constraint("PK_ORG_DETAILS").primaryKey(ORG_DETAILS.ORG_DETAILS_ID))
                    .execute();
            InsertValuesStep3<OrgDetailsRecord, Long, Long, String> query = dsl.insertInto(ORG_DETAILS)
                    .columns(ORG_DETAILS.ORG_DETAILS_ID, ORG_DETAILS.UID, ORG_DETAILS.OGRN);
            for (Record3<Long, Long, String> record : ALL_RECORDS) {
                query.values(record.value1(), record.value2(), record.value3());
            }
            query.execute();

            body.accept(dsl);
        } finally {
            try (Connection finalizeConnection = mysql.connect()) {
                finalizeConnection.createStatement().execute("DROP DATABASE ppc");
            }
        }
    }

    /**
     * Проверяет, что {@link JooqChunkReader} может прочесть всю таблицу целиком с любым размером пачки.
     */
    @Test
    public void testFullTableFetch() throws InterruptedException, SQLException {
        testWithSimpleTable(dsl -> {
            List<Record3<Long, Long, String>> result = new ArrayList<>();
            JooqChunkReader<Record3<Long, Long, String>, Long> jooqChunkReader = new JooqChunkReader<>(
                    ORG_DETAILS.ORG_DETAILS_ID,
                    () -> dsl.select(ORG_DETAILS.ORG_DETAILS_ID, ORG_DETAILS.UID, ORG_DETAILS.OGRN).from(ORG_DETAILS),
                    null,
                    chunkSize);
            while (jooqChunkReader.hasNext()) {
                result.addAll(jooqChunkReader.next());
            }

            assertThat(result).isEqualTo(ALL_RECORDS);
        });
    }
}
