package ru.yandex.market.logshatter;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import ru.yandex.market.clickhouse.ddl.Column;
import ru.yandex.market.clickhouse.ddl.ColumnType;
import ru.yandex.market.health.configs.logshatter.LogBatch;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 28.05.2019
 */
public class LogBatchValidationTest {
    @Test
    public void rowDate() {
        checkRowDateValid(
            new Date(),
            new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(19 * 365)),
            new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(364))
        );
        checkRowDateInvalid(
            null,
            new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(21 * 365)),
            new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2 * 365))
        );
    }

    @Test
    public void string() {
        checkValid(ColumnType.String, "", "qwerty", 1);
        checkInvalid(ColumnType.String, (Object) null);
    }

    @Test
    public void date() {
        checkValid(ColumnType.Date, new Date(), new Date(0), new Date(2177355600636L));
        checkInvalid(ColumnType.Date, null, 1, new Date(-1), new Date(3000000000000L));
    }

    @Test
    public void dateTime() {
        checkValid(ColumnType.DateTime, new Date(), new Date(0), new Date(2177355600636L));
        checkInvalid(ColumnType.DateTime, null, 1, new Date(-1), new Date(3000000000000L));
    }

    @Test
    public void int8() {
        checkValid(ColumnType.Int8, 0, Byte.MIN_VALUE, Byte.MAX_VALUE, (byte) 1, (short) 1, (int) 1, (long) 1, true);
        checkInvalid(ColumnType.Int8, null, "", Byte.MIN_VALUE - 1, Byte.MAX_VALUE + 1);
    }

    @Test
    public void int16() {
        checkValid(ColumnType.Int16, 0, Short.MIN_VALUE, Short.MAX_VALUE, (byte) 1, (short) 1, (int) 1, (long) 1, true);
        checkInvalid(ColumnType.Int16, null, "", Short.MIN_VALUE - 1, Short.MAX_VALUE + 1);
    }

    @Test
    public void int32() {
        checkValid(ColumnType.Int32, 0, Integer.MIN_VALUE, Integer.MAX_VALUE, (byte) 1, (short) 1,
            (int) 1, (long) 1, true);
        checkInvalid(ColumnType.Int32, null, "", Integer.MIN_VALUE - 1L, Integer.MAX_VALUE + 1L);
    }

    @Test
    public void int64() {
        checkValid(ColumnType.Int64, 0, Long.MIN_VALUE, Long.MAX_VALUE, (byte) 1, (short) 1, (int) 1, (long) 1, true);
        checkInvalid(ColumnType.Int64, null, "");
    }

    @Test
    public void uint8() {
        checkValid(ColumnType.UInt8, 0, 255, (byte) 1, (short) 1, (int) 1, (long) 1, true);
        checkInvalid(ColumnType.UInt8, null, "", -1, 256);
    }

    @Test
    public void uint16() {
        checkValid(ColumnType.UInt16, 0, 65535, (byte) 1, (short) 1, (int) 1, (long) 1, true);
        checkInvalid(ColumnType.UInt16, null, "", -1, 65536);
    }

    @Test
    public void uint32() {
        checkValid(ColumnType.UInt32, 0, 4294967295L, (byte) 1, (short) 1, (int) 1, (long) 1, true);
        checkInvalid(ColumnType.UInt32, null, "", -1, 4294967296L);
    }

    @Test
    public void uint64() {
        checkValid(ColumnType.UInt64, 0, Long.MAX_VALUE, UnsignedLong.MAX_VALUE, (byte) 1, (short) 1, (int) 1,
            (long) 1, true);
        checkInvalid(ColumnType.UInt64, null, "", -1);
    }

    @Test
    public void float32() {
        checkValid(ColumnType.Float32, 0, Float.MIN_VALUE, Float.MAX_VALUE, (byte) 1, (short) 1, (int) 1,
            (long) 1, true);
        checkInvalid(ColumnType.Float32, null, "", Double.MIN_VALUE, Double.MAX_VALUE);
    }

    @Test
    public void float64() {
        checkValid(ColumnType.Float64, 0, Float.MIN_VALUE, Float.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE,
            (byte) 1, (short) 1, (int) 1, (long) 1, true);
        checkInvalid(ColumnType.Float64, null, "");
    }

    @Test
    public void array() {
        checkValid(ColumnType.ArrayString, new Object[]{}, new String[]{}, Collections.emptyList(),
            new String[]{"qwe"}, Collections.singletonList("qwe"));
        checkInvalid(ColumnType.ArrayString, null, "", new String[]{null}, Collections.singletonList(null));
    }

    @Test
    public void nullable() {
        checkValid(ColumnType.NullableUInt8, null, 0, 255, (byte) 1, (short) 1, (int) 1, (long) 1, true);
        checkInvalid(ColumnType.NullableUInt8, "", -1, 256);
    }

    private static void checkRowDateValid(Date... values) {
        for (Date date : values) {
            LogBatch logBatch = createLogBatch();
            logBatch.write(date);
            assertLogBatchIsNotEmpty(logBatch);
        }
    }

    private static void checkRowDateInvalid(Date... values) {
        for (Date date : values) {
            LogBatch logBatch = createLogBatch(ColumnType.Int32);
            assertThatThrownBy(() -> logBatch.write(date, 1)).isInstanceOf(IllegalArgumentException.class);
            assertLogBatchIsEmpty(logBatch);
        }
    }


    private static void checkValid(ColumnType columnType, Object... values) {
        for (Object value : values) {
            // Проверяем что это значение можно добавить в LogBatch.
            LogBatch logBatch = createLogBatch(columnType);
            logBatch.write(new Date(), value);
            assertLogBatchIsNotEmpty(logBatch);
        }
    }

    private static void checkInvalid(ColumnType columnType, Object... values) {
        for (Object value : values) {
            // Проверяем что это значение нельзя добавить в LogBatch.
            // Добавляем Int32-колонку перед проверяемой чтобы убедиться что перед падением данные не добавляются в
            // parsedDates и parsingInProgressColumns.
            LogBatch logBatch = createLogBatch(ColumnType.Int32, columnType);
            assertThatThrownBy(() -> logBatch.write(new Date(), 1, value)).isInstanceOf(IllegalArgumentException.class);
            assertLogBatchIsEmpty(logBatch);
        }
    }

    private static LogBatch createLogBatch(ColumnType... columnTypes) {
        return new LogBatch(
            Stream.empty(), 0, 0, 0, Duration.ofMillis(0),
            Stream.concat(
                Stream.of(
                    new Column("date", ColumnType.Date),
                    new Column("timestamp", ColumnType.UInt32)
                ),
                Stream.of(columnTypes)
                    .map(columnType -> new Column("column", columnType))
            )
                .collect(Collectors.toList()),
            "sourceName",
            "sourceHost",
            null
        );
    }

    /**
     * Проверяем что в LogBatch не добавилась часть колонок
     */
    private static void assertLogBatchIsEmpty(LogBatch logBatch) {
        assertEquals(0, logBatch.getOutputSize());
        for (LogBatch.ParsingInProgressColumn parsingInProgressColumn : logBatch.parsingInProgressColumns) {
            assertEquals(0, parsingInProgressColumn.getParsedColumn().length);
        }
    }

    private static void assertLogBatchIsNotEmpty(LogBatch logBatch) {
        assertEquals(1, logBatch.getOutputSize());
        for (LogBatch.ParsingInProgressColumn parsingInProgressColumn : logBatch.parsingInProgressColumns) {
            assertTrue(parsingInProgressColumn.getParsedColumn().length > 0);
        }
    }
}
