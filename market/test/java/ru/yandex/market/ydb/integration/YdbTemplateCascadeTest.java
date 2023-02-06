package ru.yandex.market.ydb.integration;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ydb.integration.model.YdbField;
import ru.yandex.market.ydb.integration.query.QFrom;
import ru.yandex.market.ydb.integration.query.QSelect;
import ru.yandex.market.ydb.integration.query.TableReadOperation;
import ru.yandex.market.ydb.integration.query.YdbCascade;
import ru.yandex.market.ydb.integration.query.YdbDelete;
import ru.yandex.market.ydb.integration.query.YdbInsert;
import ru.yandex.market.ydb.integration.query.YdbSelect;
import ru.yandex.market.ydb.integration.query.YdbUpdate;
import ru.yandex.market.ydb.integration.table.Primary;
import ru.yandex.market.ydb.integration.utils.Converters;
import ru.yandex.market.ydb.integration.utils.DaoUtils;
import ru.yandex.market.ydb.integration.utils.YdbUtils;

import static com.yandex.ydb.core.StatusCode.GENERIC_ERROR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.ydb.integration.YdbTemplate.inTransaction;
import static ru.yandex.market.ydb.integration.YdbTemplate.txRead;
import static ru.yandex.market.ydb.integration.YdbTemplate.txWrite;

public class YdbTemplateCascadeTest extends ServiceTestBase {

    @Autowired
    private YdbTemplate ydbTemplate;
    @Autowired
    private SimpleYdbTableDescription simpleTable;

    @BeforeEach
    void configure() {
        ydbTemplate.createTable(simpleTable.toCreate());
    }

    @AfterEach
    void clean() {
        ydbTemplate.dropTable(simpleTable.tableName());
    }

    @Test
    void shouldInsertDataInCascadeQuery() {
        var qb1 = YdbInsert.replace(simpleTable, simpleTable.id, simpleTable.name);
        for (long i = 1; i < 100; i++) {
            qb1.row(i, "name");
        }
        var qb2 = YdbInsert.replace(simpleTable, simpleTable.id, simpleTable.name);
        for (long i = 1; i < 100; i++) {
            qb2.row(i, "name");
        }
        ydbTemplate.update(YdbCascade.cascadeOf(qb1.toQuery(), qb2.toQuery()), YdbTemplate.txWrite());
    }

    @Test
    void shouldUpsertDataInCascadeQuery() {
        var qb = YdbInsert.replace(simpleTable, simpleTable.id, simpleTable.name);
        for (long i = 1; i < 100; i++) {
            qb.row(i, "name");
        }
        ydbTemplate.update(qb, YdbTemplate.txWrite());

        var qb1 = YdbInsert.upsert(simpleTable, simpleTable.id, simpleTable.name)
                .row(1L, "name2")
                .row(2L, "name2")
                .row(3L, "name2")
                .row(5L, "name2");

        var qb2 = YdbInsert.upsert(simpleTable, simpleTable.id, simpleTable.name)
                .row(6L, "name2")
                .row(7L, "name2")
                .row(8L, "name2")
                .row(10L, "name2");

        ydbTemplate.update(YdbCascade.cascadeOf(qb1.toQuery(), qb2.toQuery()), YdbTemplate.txWrite());
        List<Map.Entry<Long, String>> entries = ydbTemplate.selectList(YdbSelect.selectFrom(simpleTable)
                        .where(simpleTable.getId().in(1L, 2L, 3L, 5L, 6L, 7L, 8L, 10L)), YdbTemplate.txRead(),
                Converters.convertEachRowToList((collector, rdr) -> collector.yield(Map.entry(
                        DaoUtils.toLong(rdr.getColumn(simpleTable.getId().alias())),
                        DaoUtils.toString(rdr.getColumn(simpleTable.getName().alias()))
                ))));

        assertThat(entries, everyItem(hasProperty("value", is("name2"))));
    }

    @DatabaseModel(value = "cascade_table", alias = "t")
    public static class SimpleYdbTableDescription extends YdbTableDescription {

        @Primary
        private final YdbField<Long> id = bigint("id");
        private final YdbField<String> name = text("name");

        public YdbField<Long> getId() {
            return id;
        }

        public YdbField<String> getName() {
            return name;
        }
    }

}
