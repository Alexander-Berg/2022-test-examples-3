package ru.yandex.market.clickhouse.ddl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 18.07.17
 */
public class ColumnTypeTest {
    @Test
    public void parseValueArray() throws Exception {
        Object[] parsedValue = (Object[]) ColumnType.ArrayString.parseValue("first,second,third", null);
        assertEquals("first", parsedValue[0]);
        assertEquals("second", parsedValue[1]);
        assertEquals("third", parsedValue[2]);
    }

    @Test
    public void parseDate() {
        Date date = (Date) ColumnType.DateTime.parseValue("1500460425000", null);
        assertEquals(new Date(1500460425000L), date);
    }

    @Test
    public void parseDateTime64() {
        LocalDateTime dateTime = LocalDateTime.parse("2022-04-19T12:34:54.123456789");
        assertEquals(dateTime, ColumnType.DateTime64.parseValue("2022-04-19T12:34:54.123456789", null));
        assertEquals(dateTime, ColumnType.DateTime64_3.parseValue("2022-04-19T12:34:54.123456789", null));
        assertEquals(dateTime, ColumnType.DateTime64_6.parseValue("2022-04-19T12:34:54.123456789", null));
        assertEquals(dateTime, ColumnType.DateTime64_9.parseValue("2022-04-19T12:34:54.123456789", null));
    }

    @Test
    public void parseNullable() {
        Object value = ColumnType.NullableUInt16.parseValue(null, null);
        assertEquals(null, value);

        value = ColumnType.NullableUInt16.parseValue("10", null);
        assertEquals(10, value);
    }

    @Test
    public void parseLowCardinality() {
        Object value = ColumnType.LowCardinalityString.parseValue("str", null);
        assertEquals("str", value);
    }

    @Test
    public void parseArrayLowCardinality() {
        boolean validArray = ColumnType.ArrayLowCardinalityString.validate(new String[]{"some test value"});
        assertTrue(validArray);

        validArray = ColumnType.ArrayLowCardinalityString.validate(new String[]{"some test value", "another test " +
            "value"});

        assertTrue(validArray);
    }

    @Test
    public void checkValidArrayValidation() throws Exception {
        boolean validArray = ColumnType.ArrayString.validate(new String[]{"some test value"});
        assertTrue(validArray);

        validArray = ColumnType.ArrayString.validate(new String[]{"some test value", "another test value"});

        assertTrue(validArray);
    }

    @Test
    public void checkValidNullableValidation() throws Exception {
        boolean validNullable = ColumnType.NullableUInt8.validate(null);
        assertTrue(validNullable);

        validNullable = ColumnType.NullableUInt8.validate(10);

        assertTrue(validNullable);
    }

    @Test
    public void checkValidLowCardinalityValidation() throws Exception {
        boolean validNullable = ColumnType.LowCardinalityString.validate("str");
        assertTrue(validNullable);
    }

    @Test
    public void checkValidCollectionValidation() throws Exception {
        boolean validCollection = ColumnType.ArrayString.validate(Collections.singleton("some test value"));
        assertTrue(validCollection);

        validCollection = ColumnType.ArrayString.validate(Arrays.asList("some test value", "another test value"));

        assertTrue(validCollection);
    }

    @Test
    public void checkInvalidCollectionValidation() throws Exception {
        boolean validCollection = ColumnType.ArrayString.validate("some test value");
        assertFalse(validCollection);
    }

    @Test
    public void checkCollectionFormat() throws Exception {
        StringBuilder sb = new StringBuilder();

        ColumnType.ArrayString.format(Collections.singletonList("some test value"), sb);

        assertEquals(sb.toString(), "['some test value']");
    }

    @Test
    public void checkCollectionFormatSameAsArrayFormat() throws Exception {
        String testString = "test string";

        StringBuilder collectionFormat = new StringBuilder();
        StringBuilder arrayFormat = new StringBuilder();

        ColumnType.ArrayString.format(Collections.singletonList(testString), collectionFormat);
        ColumnType.ArrayString.format(new Object[]{testString}, arrayFormat);

        assertEquals(String.valueOf(collectionFormat), String.valueOf(arrayFormat));
    }
}
