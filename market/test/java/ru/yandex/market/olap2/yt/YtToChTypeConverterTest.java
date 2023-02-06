package ru.yandex.market.olap2.yt;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class YtToChTypeConverterTest {

    private final List<String> attributesNullable = Arrays.asList("Nullable");

    // Decimal
    @Test
    public void testGetChTypeFromYtDecimalFabrikNotNullable() {
        String expected = "Decimal(18, 8)";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment...",
                "string",
                "Decimal_18_8",
                null,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtDecimalAutoNotNullable() {
        String expected = "Decimal(15, 8)";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment_numeric",
                "string",
                null,
                null,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtDecimalFabrikNullable() {
        String expected = "Nullable(Decimal(38, 16))";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment...",
                "string",
                "Decimal_38_16",
                attributesNullable,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtDecimalAutoNullable() {
        String expected = "Nullable(Decimal(15, 8))";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment_numeric",
                "string",
                null,
                attributesNullable,
                false
        );
        assertEquals(expected, actual);
    }

    // Date
    @Test
    public void testGetChTypeFromYtDateFabrikNotNullable() {
        String expected = "Date";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment...",
                "string",
                "Date",
                null,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtDateFabrikNullable() {
        String expected = "Nullable(Date)";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment...",
                "string",
                "Date",
                attributesNullable,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtDateAutoNotNullable() {
        String expected = "Date";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment_date",
                "string",
                null,
                null,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtDateAutoNullable() {
        String expected = "Nullable(Date)";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment_date",
                "string",
                null,
                attributesNullable,
                false
        );
        assertEquals(expected, actual);
    }

    // DateTime
    @Test
    public void testGetChTypeFromYtDateTimeFabrikNotNullable() {
        String expected = "DateTime";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment...",
                "string",
                "DateTime",
                null,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtDateTimeFabrikNullable() {
        String expected = "Nullable(DateTime)";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment...",
                "string",
                "DateTime",
                attributesNullable,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtDateTimeAutoNotNullable() {
        String expected = "DateTime";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment_datetime",
                "string",
                null,
                null,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtDateTimeAutoNullable() {
        String expected = "Nullable(DateTime)";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment_datetime",
                "string",
                null,
                attributesNullable,
                false
        );
        assertEquals(expected, actual);
    }

    // Int
    @Test
    public void testGetChTypeFromYtIntFabrikNotNullable() {
        String expected = "Int32";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment...",
                "int32",
                "Int32",
                null,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtIntFabrikNullable() {
        String expected = "Nullable(Int64)";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment...",
                "int64",
                "Int64",
                attributesNullable,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtIntAutoNotNullable() {
        String expected = "Int8";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment",
                "int8",
                null,
                null,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtIntAutoNullable() {
        String expected = "Nullable(Int16)";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment",
                "int16",
                null,
                attributesNullable,
                false
        );
        assertEquals(expected, actual);
    }

    // UInt
    @Test
    public void testGetChTypeFromYtUIntFabrikNotNullable() {
        String expected = "UInt32";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment...",
                "uint32",
                "UInt32",
                null,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtUIntFabrikNullable() {
        String expected = "Nullable(UInt64)";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment...",
                "uint64",
                "UInt64",
                attributesNullable,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtUIntAutoNotNullable() {
        String expected = "UInt8";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment",
                "uint8",
                null,
                null,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtUIntAutoNullable() {
        String expected = "Nullable(UInt16)";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment",
                "uint16",
                null,
                attributesNullable,
                false
        );
        assertEquals(expected, actual);
    }

    // Float
    @Test
    public void testGetChTypeFromYtFloatFabrikNotNullable() {
        String expected = "Float32";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment...",
                "double",
                "Float32",
                null,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtFloatFabrikNullable() {
        String expected = "Nullable(Float64)";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment...",
                "double",
                "Float64",
                attributesNullable,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtFloatAutoNotNullable() {
        String expected = "Float64";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment",
                "double",
                null,
                null,
                false
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChTypeFromYtFloatAutoNullable() {
        String expected = "Nullable(Float64)";
        String actual = YtToChTypeConverter.getChTypeFromYt(
                "moment",
                "double",
                null,
                attributesNullable,
                false
        );
        assertEquals(expected, actual);
    }

    // Tests for getChDefaultString FabrikClickhouseType
    @Test
    public void testGetChDefaultStringDecimalFabrik() {
        String expected = " DEFAULT 0.0";
        String actual = YtToChTypeConverter.getChDefaultString(
                "moment",
                "Decimal(18, 8)",
                "Decimal_18_8"
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChDefaultStringDateFabrik() {
        String expected = " DEFAULT '0000-00-00'";
        String actual = YtToChTypeConverter.getChDefaultString(
                "moment",
                "Date",
                "Date"
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChDefaultStringDateTimeFabrik() {
        String expected = " DEFAULT '0000-00-00 00:00:00'";
        String actual = YtToChTypeConverter.getChDefaultString(
                "moment",
                "DateTime",
                "DateTime"
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChDefaultStringFloatFabrik() {
        String expected = " DEFAULT 0.0";
        String actual = YtToChTypeConverter.getChDefaultString(
                "moment",
                "Float32",
                "Float32"
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChDefaultStringIntFabrik() {
        String expected = " DEFAULT 0";
        String actual = YtToChTypeConverter.getChDefaultString(
                "moment",
                "Int64",
                "Int64"
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChDefaultStringUIntFabrik() {
        String expected = " DEFAULT 0";
        String actual = YtToChTypeConverter.getChDefaultString(
                "moment",
                "UInt32",
                "UInt32"
        );
        assertEquals(expected, actual);
    }

    // Tests for getChDefaultString Auto
    @Test
    public void testGetChDefaultStringDecimalAuto() {
        String expected = " DEFAULT 0.0";
        String actual = YtToChTypeConverter.getChDefaultString(
                "moment_numeric",
                "Decimal(15, 8)",
                null
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChDefaultStringDateAuto() {
        String expected = " DEFAULT '0000-00-00'";
        String actual = YtToChTypeConverter.getChDefaultString(
                "moment_date",
                "Date",
                null
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChDefaultStringDateTimeAuto() {
        String expected = " DEFAULT '0000-00-00 00:00:00'";
        String actual = YtToChTypeConverter.getChDefaultString(
                "moment_datetime",
                "DateTime",
                null
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChDefaultStringFloatAuto() {
        String expected = " DEFAULT 0.0";
        String actual = YtToChTypeConverter.getChDefaultString(
                "moment",
                "Float32",
                null
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChDefaultStringIntAuto() {
        String expected = " DEFAULT 0";
        String actual = YtToChTypeConverter.getChDefaultString(
                "moment",
                "Int64",
                null
        );
        assertEquals(expected, actual);
    }

    @Test
    public void testGetChDefaultStringUIntAuto() {
        String expected = " DEFAULT 0";
        String actual = YtToChTypeConverter.getChDefaultString(
                "moment",
                "UInt32",
                null
        );
        assertEquals(expected, actual);
    }

}
