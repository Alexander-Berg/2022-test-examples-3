package ru.yandex.direct.dbutil;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.impl.DSL;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.dbutil.testing.DbUtilTest;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.dbutil.SqlUtils.mysqlConvertFromUtc;
import static ru.yandex.direct.dbutil.SqlUtils.mysqlUtcTimestamp;

@DbUtilTest
@RunWith(SpringRunner.class)
@ExtendWith(SpringExtension.class)
public class SqlUtilsTest {
    private static final String DEFAULT_VALUE = "No value";
    private static final long TIME_GAP_MILLIS = Duration.ofSeconds(3).toMillis();
    @Autowired
    DslContextProvider dslContextProvider;

    @Test
    public void utcTimestampReturnsCorrectTime() throws Exception {
        Timestamp ts = dslContextProvider.ppcdict()
                .select(mysqlUtcTimestamp())
                .fetchOne()
                .value1();
        assertThat(ts)
                .isCloseTo(Instant.now().toString(), TIME_GAP_MILLIS);
    }

    @Test
    public void mysqlConvertFromUtcForDatabaseUtcTimestamp() throws Exception {
        Record2<Timestamp, Timestamp> row = dslContextProvider.ppcdict()
                .select(mysqlConvertFromUtc(mysqlUtcTimestamp()), DSL.currentTimestamp())
                .fetchOne();

        assertThat(row.value1()).isCloseTo(row.value2().toString(), TIME_GAP_MILLIS);
    }

    @Test
    public void localDateTimeAdd_works() throws Exception {
        Record2<LocalDateTime, LocalDateTime> row = dslContextProvider.ppcdict()
                .select(
                        DSL.field("NOW() - INTERVAL 1 HOUR", LocalDateTime.class),
                        SqlUtils.localDateTimeAdd(DSL.currentLocalDateTime(), Duration.ofHours(-1))
                )
                .fetchOne();

        assertEquals(row.value1(), row.value2());
    }

    private Map<Long, String> caseTestMap(long num) {
        return LongStream.range(0, num).boxed()
                .collect(Collectors.toMap(Long::valueOf, this::getNumName));
    }

    private String getNumName(long num) {
        return String.format("Test %s", num);
    }

    @Test
    public void sqlCaseReturnsCorrectValue() {
        int elementsNum = 1000;
        for (long i = 0; i < elementsNum; i++) {
            Field<String> caseStatement =
                    JooqMapperUtils.makeCaseStatementNoTable(DSL.val(i), DSL.val(DEFAULT_VALUE), caseTestMap(elementsNum));
            Record1<String> row = dslContextProvider.ppcdict()
                    .select(
                            caseStatement
                    )
                    .fetchOne();
            assertEquals(getNumName(i), row.value1());
        }
    }

    @Test
    public void sqlCaseReturnsDefault() {
        Field<String> caseStatement =
                JooqMapperUtils.makeCaseStatementNoTable(DSL.val(2000L), DSL.val(DEFAULT_VALUE), caseTestMap(2000L));
        Record1<String> row = dslContextProvider.ppcdict()
                .select(
                        caseStatement
                )
                .fetchOne();
        assertEquals(DEFAULT_VALUE, row.value1());
    }
}
