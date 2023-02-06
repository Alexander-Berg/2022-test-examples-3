package ru.yandex.direct.mysql;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;

import org.junit.Test;

import ru.yandex.direct.utils.DateTimeUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestMySQLColumnType {
    @Test
    public void simpleDate() {
        assertThat(new MySQLColumnType("date").toString(),
                is("MySQLColumnType{dataType=DATE, width=0, precision=0, values=[], isUnsigned=false}"));
    }

    @Test
    public void simpleInteger() {
        assertThat(new MySQLColumnType("int(11)").toString(),
                is("MySQLColumnType{dataType=INT, width=11, precision=0, values=[], isUnsigned=false}"));
    }

    @Test
    public void unsignedInteger() {
        assertThat(new MySQLColumnType("int(11) unsigned").toString(),
                is("MySQLColumnType{dataType=INT, width=11, precision=0, values=[], isUnsigned=true}"));
    }

    @Test
    public void unsignedIntegerWithoutWidth() {
        assertThat(new MySQLColumnType("int unsigned").toString(),
                is("MySQLColumnType{dataType=INT, width=0, precision=0, values=[], isUnsigned=true}"));
    }

    @Test
    public void floatPrecision() {
        assertThat(new MySQLColumnType("float(10,2)").toString(),
                is("MySQLColumnType{dataType=FLOAT, width=10, precision=2, values=[], isUnsigned=false}"));
    }

    @Test
    public void unsupportedTypes() {
        assertThat(new MySQLColumnType("foobarbaz(11,3) whatever character set utf-8 something unsigned").toString(),
                is("MySQLColumnType{dataType=UNKNOWN, width=11, precision=3, values=[], isUnsigned=true}"));
    }

    @Test
    public void simpleEnum() {
        assertThat(new MySQLColumnType("enum('a','b','c')").toString(),
                is("MySQLColumnType{dataType=ENUM, width=0, precision=0, values=[a, b, c], isUnsigned=false}"));
    }

    @Test
    public void trickyEnum() {
        assertThat(new MySQLColumnType("enum('it''s','a\\nva\\'value','foo\\Zbar')").toString(),
                is("MySQLColumnType{dataType=ENUM, width=0, precision=0, values=[it's, a\nva'value, foo\u001Abar], isUnsigned=false}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingCommas() {
        new MySQLColumnType("enum('a' 'b')");
    }

    @Test(expected = IllegalArgumentException.class)
    public void extraCommas() {
        new MySQLColumnType("enum('a',,'b')");
    }

    @Test
    public void enumDecodingNormal() {
        assertThat(new MySQLColumnType("enum('a','b','c')").extractString(2), is("b"));
    }

    @Test
    public void enumDecodingEmptyValue() {
        assertThat(new MySQLColumnType("enum('a','b','c')").extractString(0), is(""));
    }

    @Test
    public void setDecodingNormal() {
        assertThat(new MySQLColumnType("set('a','b','c')").extractString(5), is("a,c"));
    }

    @Test
    public void setDecodingEmptyValue() {
        assertThat(new MySQLColumnType("set('a','b','c')").extractString(0), is(""));
    }

    @Test
    public void cachedValuesAreSame() {
        assertThat(MySQLColumnType.getCached("int(11)") == MySQLColumnType.getCached("int(11)"), is(true));
    }

    @Test
    public void signedTinyInt() {
        assertThat(MySQLColumnType.getCached("tinyint").extractLong(-1), is(-1L));
    }

    @Test
    public void signedTinyIntString() {
        assertThat(MySQLColumnType.getCached("tinyint").extractString(-1), is("-1"));
    }

    @Test
    public void unsignedTinyInt() {
        assertThat(MySQLColumnType.getCached("tinyint unsigned").extractLong(-1), is(255L));
    }

    @Test
    public void unsignedTinyIntString() {
        assertThat(MySQLColumnType.getCached("tinyint unsigned").extractString(-1), is("255"));
    }

    @Test
    public void signedSmallInt() {
        assertThat(MySQLColumnType.getCached("smallint").extractLong(-1), is(-1L));
    }

    @Test
    public void signedSmallIntString() {
        assertThat(MySQLColumnType.getCached("smallint").extractString(-1), is("-1"));
    }

    @Test
    public void unsignedSmallInt() {
        assertThat(MySQLColumnType.getCached("smallint unsigned").extractLong(-1), is(65535L));
    }

    @Test
    public void unsignedSmallIntString() {
        assertThat(MySQLColumnType.getCached("smallint unsigned").extractString(-1), is("65535"));
    }


    @Test
    public void signedMediumInt() {
        assertThat(MySQLColumnType.getCached("mediumint").extractLong(-1), is(-1L));
    }

    @Test
    public void signedMediumIntString() {
        assertThat(MySQLColumnType.getCached("mediumint").extractString(-1), is("-1"));
    }

    @Test
    public void unsignedMediumInt() {
        assertThat(MySQLColumnType.getCached("mediumint unsigned").extractLong(-1), is(16777215L));
    }

    @Test
    public void unsignedMediumIntString() {
        assertThat(MySQLColumnType.getCached("mediumint unsigned").extractString(-1), is("16777215"));
    }

    @Test
    public void signedInt() {
        assertThat(MySQLColumnType.getCached("int").extractLong(-1), is(-1L));
    }

    @Test
    public void signedIntString() {
        assertThat(MySQLColumnType.getCached("int").extractString(-1), is("-1"));
    }

    @Test
    public void unsignedInt() {
        assertThat(MySQLColumnType.getCached("int unsigned").extractLong(-1), is(4294967295L));
    }

    @Test
    public void unsignedIntString() {
        assertThat(MySQLColumnType.getCached("int unsigned").extractString(-1), is("4294967295"));
    }

    @Test
    public void signedBigInt() {
        assertThat(MySQLColumnType.getCached("bigint").extractLong(-1), is(-1L));
    }

    @Test
    public void signedBigIntString() {
        assertThat(MySQLColumnType.getCached("bigint").extractString(-1), is("-1"));
    }

    @Test
    public void unsignedBigInt() {
        // Java does not have unsigned longs, so a signed long is returned anyway
        assertThat(MySQLColumnType.getCached("bigint unsigned").extractLong(-1), is(-1L));
    }

    @Test
    public void unsignedBigIntString() {
        assertThat(MySQLColumnType.getCached("bigint unsigned").extractString(-1), is("18446744073709551615"));
    }

    @Test
    public void extractLocalDateTimeByTimestamp() {
        Timestamp timestampToSend = Timestamp.valueOf("2021-02-26 12:32:42.0");

        LocalDateTime expectTime = timestampToSend.toInstant().atZone(DateTimeUtils.MSK).toLocalDateTime();

        assertThat(MySQLColumnType.getCached("timestamp").extractLocalDateTime(timestampToSend), is(expectTime));
    }

    @Test
    public void extractLocalDateTimeByDateTime() {
        Calendar valueToSend = Calendar.getInstance();
        valueToSend.set(2021, Calendar.FEBRUARY, 26, 12, 32, 42);

        LocalDateTime expectTime = LocalDateTime.ofInstant(valueToSend.toInstant(), DateTimeUtils.MSK);

        assertThat(MySQLColumnType.getCached("datetime").extractLocalDateTime(valueToSend.getTime()), is(expectTime));
    }

    @Test
    public void extractLocalDateByTimestamp() {
        Timestamp timestampToSend = Timestamp.valueOf("2021-02-26 00:00:00.0");

        LocalDate expectTime = timestampToSend.toInstant().atZone(DateTimeUtils.MSK).toLocalDateTime().toLocalDate();

        assertThat(MySQLColumnType.getCached("date").extractLocalDate(timestampToSend), is(expectTime));
    }

    @Test
    public void extractLocalDateByDateTime() {
        Calendar valueToSend = Calendar.getInstance();
        valueToSend.set(2021, Calendar.FEBRUARY, 26);

        LocalDate expectTime = LocalDate.ofInstant(valueToSend.toInstant(), DateTimeUtils.MSK);

        assertThat(MySQLColumnType.getCached("datetime").extractLocalDate(valueToSend.getTime()), is(expectTime));
    }
}
