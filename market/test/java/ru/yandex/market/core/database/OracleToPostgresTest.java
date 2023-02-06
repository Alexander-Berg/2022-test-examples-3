package ru.yandex.market.core.database;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Set;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mbi.util.db.DbUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class OracleToPostgresTest extends FunctionalTest {
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Test
    void validatePostgresLiquibaseMigrations() {
        PostgreMigrationsValidator.validate(
                jdbcTemplate,
                Set.of(
                        EmbeddedPostgresConfig.CHANGELOG_VALIDATED
                )
        ).ifPresent(Assertions::fail);
    }

    static Stream<Arguments> selectFromDualData() {
        return Stream.of(
                arguments("select 1 from dual", 1),
                arguments("select nvl2(cast('test' as varchar), 5, 6) from dual", 5),
                arguments("select nvl2(cast(null as varchar), 5, 6) from dual", 6),
                arguments("select nvl2(cast(null as integer), 'var 1 is not null', 'var1 is null') from dual",
                        "var1 is null"),
                arguments("select nvl2(cast('test' as text), 2.89, 23.123) from dual", BigDecimal.valueOf(2.89)),
                arguments("select nvl2(cast(null as date), '2016-06-23', '2021-06-23') from dual", "2021-06-23"),
                arguments("select nvl2(timestamp '2011-05-16 15:36:38', '2016-06-23', " +
                        "'2021-06-23') from dual", "2016-06-23"),
                arguments("select nvl2(cast(null as character), 'A', 'B') from dual", "B"),
                arguments("select nvl2(timestamp '2011-05-16 15:36:38', " +
                                "timestamp '2016-06-23 00:00:00', " +
                                "timestamp '2021-06-23 00:00:00') from dual",
                        Timestamp.valueOf("2016-06-23 00:00:00")),
                arguments("select nvl2(timestamp '2011-05-16 15:36:38', " +
                                "timestamp '2016-06-23 00:00:00', " +
                                "timestamp '2021-06-23 00:00:00') from dual",
                        Timestamp.valueOf("2016-06-23 00:00:00")),
                arguments("select nvl2(0, " +
                                "timestamp '2016-06-23 00:00:00', " +
                                "timestamp '2021-06-23 00:00:00') from dual",
                        Timestamp.valueOf("2016-06-23 00:00:00")),
                arguments("select nvl2(cast(null as integer), " +
                                "timestamp '2016-06-23 00:00:00', " +
                                "timestamp '2021-06-23 00:00:00') from dual",
                        Timestamp.valueOf("2021-06-23 00:00:00")),
                arguments("select decode('test', 'test', 1, 0) from dual", 1),
                arguments("select decode('test', 'tast', 1, 12345) from dual", 12345),
                arguments("select trunc(timestamp '2020-05-05 01:12:47') from dual",
                        Timestamp.valueOf("2020-05-05 00:00:00")),
                arguments("select trunc(timestamp '2020-05-05 01:12:47', 'hour') from dual",
                        Timestamp.valueOf("2020-05-05 01:00:00")),
                arguments("select decode(10, 11, 1, 10, 0, 3) from dual", 0),
                arguments("select decode(10, 10, 1, 11, 0, 3) from dual", 1),
                arguments("select decode(10, 11, 1, 20, 0, 3) from dual", 3),
                arguments("select decode(cast(10 as numeric), cast(11 as numeric), 1, cast(10 as numeric), 0, 3) from" +
                        " dual", 0),
                arguments("select decode(cast(10 as numeric), cast(10 as numeric), 1, cast(11 as numeric), 0, 3) from" +
                        " dual", 1),
                arguments("select decode(cast(10 as numeric), cast(11 as numeric), 1, cast(20 as numeric), 0, 3) from" +
                        " dual", 3),
                arguments("select decode(123, 123, 33, 22) from dual", 33),
                arguments("select decode(cast('test' as varchar(4)), 'test', 1, 'test2', 2, 'test3', 3, 4) from dual"
                        , 1),
                arguments("select decode(cast('test' as varchar(4)), 'test1', 1, 'test', 2, 'test3', 3, 4) from dual"
                        , 2),
                arguments("select decode(cast('test' as varchar(4)), 'test1', 1, 'test2', 2, 'test', 3, 4) from dual"
                        , 3),
                arguments("select decode(cast('test' as varchar(4)), 'test1', 1, 'test2', 2, 'test3', 3, 4) from " +
                        "dual", 4),
                arguments("select decode(10, 10, 1, 30, 2, 40, 3, 4) from dual", 1),
                arguments("select decode(10, 20, 1, 10, 2, 40, 3, 4) from dual", 2),
                arguments("select decode(10, 20, 1, 30, 2, 10, 3, 4) from dual", 3),
                arguments("select decode(10, 20, 1, 30, 2, 40, 3, 4) from dual", 4),
                arguments("select decode(0, 0, timestamp '2004-10-19 10:23:54', " +
                        "timestamp '2001-08-03 19:20:00') from dual", Timestamp.valueOf(
                        "2004-10-19 10:23:54")),
                arguments("select decode(1, 0, timestamp '2004-10-19 10:23:54', " +
                        "timestamp '2001-08-03 19:20:00') from dual", Timestamp.valueOf(
                        "2001-08-03 19:20:00")),
                arguments("select decode(cast('a' as varchar(1)), 'a', 1, 'b', 2) from dual", 1),
                arguments("select decode(cast('b' as varchar(1)), 'a', 1, 'b', 2) from dual", 2),
                arguments("select decode(cast('c' as varchar(1)), 'a', 1, 'b', 2) from dual", null),
                arguments("select 1 from dual where regexp_like(cast('12' as text), '\\d+')", 1),
                arguments("select to_char(cast(1 as numeric)) from dual", "1"),
                arguments("select regexp_instr('aababc', 'abc') from dual", BigDecimal.valueOf(4)),
                arguments("select shops_web.t_number_tbl(0, 1) from dual",
                        new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ONE}),
                arguments("select shops_web.t_number_tbl(cast('9223372036854775807' as bigint)) from dual",
                        new BigDecimal[]{BigDecimal.valueOf(Long.MAX_VALUE)}),
                arguments(/*language=sql*/
                        "select shops_web.compare_tuples(t1.arr, t1.arr) " +
                                "   from (select shops_web.t_number_tbl(0, 1) arr from dual) t1", BigDecimal.ZERO),
                arguments(/*language=sql*/
                        "select shops_web.compare_tuples(t1.arr2, t1.arr1) " +
                                "   from (select " +
                                "             shops_web.t_number_tbl(0, 1) arr1," +
                                "             shops_web.t_number_tbl(1, 1) arr2 " +
                                "       from dual) t1", BigDecimal.ONE),
                arguments(/*language=sql*/
                        "select shops_web.compare_tuples(t1.arr1, t1.arr2) " +
                                "   from (select " +
                                "             shops_web.t_number_tbl(0, 1) arr1," +
                                "             shops_web.t_number_tbl(1, 1) arr2 " +
                                "       from dual) t1", BigDecimal.ONE.negate()),
                arguments(/*language=sql*/
                        "select shops_web.compare_tuples(t1.arr1, t1.arr2) " +
                                "   from (select " +
                                "             shops_web.t_number_tbl(null, 1) arr1," +
                                "             shops_web.t_number_tbl(1, null) arr2 " +
                                "       from dual) t1", BigDecimal.ONE.negate()),
                arguments(/*language=sql*/
                        "select shops_web.compare_tuples(t1.arr1, t1.arr2, true) " +
                                "   from (select " +
                                "             shops_web.t_number_tbl(null, 1) arr1," +
                                "             shops_web.t_number_tbl(1, null) arr2 " +
                                "       from dual) t1", BigDecimal.ONE),
                arguments(/*language=sql*/
                        "select decode(cast(null as varchar(1)), null, cast('null' as varchar(4)), 'not null') from " +
                                "dual",
                        "null"),
                arguments(/*language=sql*/
                        "select decode(cast(null as number), null, cast('null' as varchar(4)), 'not null') from dual",
                        "null"),
                arguments(/*language=sql*/
                        "select decode(cast(null as varchar(1)), null, 1, 0) from dual",
                        1),
                arguments(/*language=sql*/
                        "select decode(cast(null as varchar(1)), null, cast('9223372036854775807' as bigint), cast" +
                                "('0' as bigint)) from dual",
                        Long.MAX_VALUE),
                arguments(/*language=sql*/
                        "select decode(cast(null as varchar(1)), null, cast('null' as varchar(4)), 'not null') from " +
                                "dual",
                        "null"),
                arguments(/*language=sql*/
                        "select decode(cast(null as number), null, timestamp '2001-08-03 19:20:00', timestamp " +
                                "'2000-01-01 00:00:00') from dual",
                        Timestamp.valueOf("2001-08-03 19:20:00")),
                arguments(/*language=sql*/
                        "select decode(cast(null as varchar(1)), null, 1, 'a', 2, 3) from dual",
                        1),
                arguments(/*language=sql*/
                        "select decode(cast(null as varchar(1)), 'a', 1, null, 2, 3) from dual",
                        2),
                arguments(/*language=sql*/
                        "select decode(cast(null as varchar(1)), 'a', 1, 'b', 2, 3) from dual",
                        3),
                arguments(/*language=sql*/
                        "select decode(cast(null as varchar(1)), null, 1, 'a', 2, 'b', 3, 4) from dual",
                        1),
                arguments(/*language=sql*/
                        "select decode(cast(null as varchar(1)), 'a', 1, null, 2, 'b', 3, 4) from dual",
                        2),
                arguments(/*language=sql*/
                        "select decode(cast(null as varchar(1)), 'a', 1, 'b', 2, null, 3, 4) from dual",
                        3),
                arguments(/*language=sql*/
                        "select decode(cast(null as varchar(1)), 'c', 1, 'a', 2, 'b', 3, 4) from dual",
                        4),
                arguments(/*language=sql*/
                        "select decode(cast(null as number), cast(1 as number), cast('null' as varchar(4)), 'not " +
                                "null') from dual",
                        "not null"),
                arguments(/*language=sql*/
                        "select decode(cast(null as varchar(4)), 'null', 1, 0) from dual",
                        0),
                arguments(/*language=sql*/
                        "select decode(cast(null as varchar(1)), 'null', cast('9223372036854775807' as bigint), cast" +
                                "('-9223372036854775808' as bigint)) from dual",
                        Long.MIN_VALUE),
                arguments(/*language=sql*/
                        "select decode(cast(null as varchar(4)), 'null', cast('null' as varchar(4)), 'not null') from" +
                                " dual",
                        "not null"),
                arguments(/*language=sql*/
                        "select decode(cast(null as number), cast(1 as number), timestamp '2001-08-03 19:20:00', " +
                                "timestamp '2000-01-01 00:00:00') from dual",
                        Timestamp.valueOf("2000-01-01 00:00:00")),
                arguments(/*language=sql*/
                        "select mbi_core.convert_date_for_oracle(86400000) from dual",
                        Timestamp.valueOf("1970-01-02 10:00:00.000000")),
                arguments(/*language=sql*/
                        "select mbi_core.convert_date_for_oracle(604800000) from dual",
                        Timestamp.valueOf("1970-01-08 10:00:00.000000")),
                arguments(/*language=sql*/
                        "select mbi_core.convert_date_for_oracle(31536000000) from dual",
                        Timestamp.valueOf("1971-01-01 10:00:00.000000")),
                arguments(/*language=sql*/
                        "select mbi_core.convert_date_for_oracle(946080000000) from dual",
                        Timestamp.valueOf("1999-12-25 10:00:00.000000")),
                arguments(/*language=sql*/
                        "select to_number(sysdate + interval '43200' second - sysdate) from dual",
                        BigDecimal.valueOf(0.5)),
                arguments(/*language=sql*/
                        "select replace(translate(to_char(trunc(timestamp '2001-01-08 00:00:00'), 'dd fmMonth YYYY', " +
                                "'nls_date_language = RUSSIAN'), 'ьй', 'яя'), 'т ', 'та ') from dual",
                        "08 Января 2001"
                ),
                arguments(/*language=sql*/
                        "select replace(translate(to_char(trunc(timestamp '2001-02-08 00:00:00'), 'dd fmMonth YYYY', " +
                                "'nls_date_language = RUSSIAN'), 'ьй', 'яя'), 'т ', 'та ') from dual",
                        "08 Февраля 2001"
                ),
                arguments(/*language=sql*/
                        "select replace(translate(to_char(trunc(timestamp '2001-03-08 00:00:00'), 'dd fmMonth YYYY', " +
                                "'nls_date_language = RUSSIAN'), 'ьй', 'яя'), 'т ', 'та ') from dual",
                        "08 Марта 2001"
                ),
                arguments(/*language=sql*/
                        "select replace(translate(to_char(trunc(timestamp '2001-04-08 00:00:00'), 'dd fmMonth YYYY', " +
                                "'nls_date_language = RUSSIAN'), 'ьй', 'яя'), 'т ', 'та ') from dual",
                        "08 Апреля 2001"
                ),
                arguments(/*language=sql*/
                        "select replace(translate(to_char(trunc(timestamp '2001-05-08 00:00:00'), 'dd fmMonth YYYY', " +
                                "'nls_date_language = RUSSIAN'), 'ьй', 'яя'), 'т ', 'та ') from dual",
                        "08 Мая 2001"
                ),
                arguments(/*language=sql*/
                        "select replace(translate(to_char(trunc(timestamp '2001-06-08 00:00:00'), 'dd fmMonth YYYY', " +
                                "'nls_date_language = RUSSIAN'), 'ьй', 'яя'), 'т ', 'та ') from dual",
                        "08 Июня 2001"
                ),
                arguments(/*language=sql*/
                        "select replace(translate(to_char(trunc(timestamp '2001-07-08 00:00:00'), 'dd fmMonth YYYY', " +
                                "'nls_date_language = RUSSIAN'), 'ьй', 'яя'), 'т ', 'та ') from dual",
                        "08 Июля 2001"
                ),
                arguments(/*language=sql*/
                        "select replace(translate(to_char(trunc(timestamp '2001-08-08 00:00:00'), 'dd fmMonth YYYY', " +
                                "'nls_date_language = RUSSIAN'), 'ьй', 'яя'), 'т ', 'та ') from dual",
                        "08 Августа 2001"
                ),
                arguments(/*language=sql*/
                        "select replace(translate(to_char(trunc(timestamp '2001-09-08 00:00:00'), 'dd fmMonth YYYY', " +
                                "'nls_date_language = RUSSIAN'), 'ьй', 'яя'), 'т ', 'та ') from dual",
                        "08 Сентября 2001"
                ),
                arguments(/*language=sql*/
                        "select replace(translate(to_char(trunc(timestamp '2001-10-08 00:00:00'), 'dd fmMonth YYYY', " +
                                "'nls_date_language = RUSSIAN'), 'ьй', 'яя'), 'т ', 'та ') from dual",
                        "08 Октября 2001"
                ),
                arguments(/*language=sql*/
                        "select replace(translate(to_char(trunc(timestamp '2001-11-08 00:00:00'), 'dd fmMonth YYYY', " +
                                "'nls_date_language = RUSSIAN'), 'ьй', 'яя'), 'т ', 'та ') from dual",
                        "08 Ноября 2001"
                ),
                arguments(/*language=sql*/
                        "select replace(translate(to_char(trunc(timestamp '2001-12-08 00:00:00'), 'dd fmMonth YYYY', " +
                                "'nls_date_language = RUSSIAN'), 'ьй', 'яя'), 'т ', 'та ') from dual",
                        "08 Декабря 2001"
                ),
                arguments(/*language=sql*/
                        "select market_billing.numbertoip(99999999)",
                        "5.245.224.255"
                ),
                arguments(/*language=sql*/
                        "select coalesce(to_number(''), 0)",
                        BigDecimal.valueOf(0)
                )
        );
    }

    @ParameterizedTest
    @MethodSource("selectFromDualData")
    void selectFromDual(String query, Object expectedResult) throws SQLException {
        var result = jdbcTemplate.queryForObject(query, Object.class);
        if (result instanceof Array) {
            result = ((Array) result).getArray();
        }
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    void sysdateQueryTest() {
        var result = jdbcTemplate.queryForObject("select sysdate from dual", Object.class);
        assertThat(result).isInstanceOf(Timestamp.class);
    }

    @Test
    void failArgumentsCompareTuples() {
        var query = /*language=sql*/
                "select shops_web.compare_tuples(t1.arr1, t1.arr2) " +
                        "   from (select " +
                        "             shops_web.t_number_tbl(0, 1, 1) arr1," +
                        "             shops_web.t_number_tbl(1, 1) arr2 " +
                        "       from dual) t1";
        assertThatExceptionOfType(BadSqlGrammarException.class)
                .isThrownBy(() -> jdbcTemplate.queryForObject(query, Object.class))
                .withMessageContaining("ERROR: unrecognized exception condition \"-20001\"")
                .withMessageEndingWith("function shops_web.compare_tuples(anyarray,anyarray,boolean) line 6 at RAISE");
    }

    @Test
    void failArgumentsToChar() {
        var queryBadFormatDate = /*language=sql*/
                "select to_char(timestamp '2001-03-08 00:00:00', 'dd month yy', 'nls_date_language = RUSSIAN') from " +
                        "dual";
        assertThatExceptionOfType(UncategorizedSQLException.class)
                .isThrownBy(() -> jdbcTemplate.queryForObject(queryBadFormatDate, Object.class))
                .withMessageContaining("date format must contain fmMonth, got dd month yy");

        var queryBadLanguage = /*language=sql*/
                "select to_char(timestamp '2001-03-08 00:00:00', 'dd fmMonth YYYY', 'nls_date_language = EGYPT') from" +
                        " dual";
        assertThatExceptionOfType(UncategorizedSQLException.class)
                .isThrownBy(() -> jdbcTemplate.queryForObject(queryBadLanguage, Object.class))
                .withMessageContaining("non-yet-supported language nls_date_language = EGYPT");
    }

    @Test
    void checkOgrn() {
        assertThat(callCheckOgrn(null)).isZero();
        assertThat(callCheckOgrn("")).isZero();
        assertThat(callCheckOgrn("not-a-number")).as("less than 13 symbols").isZero();
        assertThat(callCheckOgrn("100000000000")).as("too short").isZero();
        assertThat(callCheckOgrn("1027700229193")).as("valid огрн").isOne();
        assertThat(callCheckOgrn("1027700229194")).as("invalid crc").isZero();
        assertThat(callCheckOgrn("420869030411138")).as("valid огрнип").isOne();
        assertThat(callCheckOgrn("420869030411139")).as("invalid crc").isZero();
        assertThat(callCheckOgrn("1000000000000000")).as("too long").isZero();
    }

    private BigDecimal callCheckOgrn(String ogrn) {
        return DbUtil.callStoredProcedure(
                jdbcTemplate,
                "{? = call shops_web.check_ogrn(?)}",
                cs -> {
                    cs.registerOutParameter(1, Types.NUMERIC);
                    cs.setString(2, ogrn);
                    cs.execute();
                    return cs.getBigDecimal(1);
                }
        );
    }

    @Test
    void checkUnp() {
        assertThat(callCheckUnp(null)).isZero();
        assertThat(callCheckUnp("490868012")).isOne();
        assertThat(callCheckUnp("490868010")).as("invalid crc").isZero();
        assertThat(callCheckUnp("short")).as("less than 9 symbols").isZero();
        assertThat(callCheckUnp("looooooong")).as("more than 9 symbols").isZero();
        assertThat(callCheckUnp("9-symbols")).as("9 non-digit symbols").isZero();
    }

    private BigDecimal callCheckUnp(String ogrn) {
        return DbUtil.callStoredProcedure(
                jdbcTemplate,
                "{? = call shops_web.check_unp(?)}",
                cs -> {
                    cs.registerOutParameter(1, Types.NUMERIC);
                    cs.setString(2, ogrn);
                    cs.execute();
                    return cs.getBigDecimal(1);
                }
        );
    }
}
