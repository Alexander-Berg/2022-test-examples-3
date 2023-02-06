package ru.yandex.direct.binlog.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class BinlogEventTypesTest {
    @SuppressWarnings("squid:S3878")
    private static final ImmutableList<Object[]> ASSETS = ImmutableList.<Object[]>builder()
            // null
            .add(new Object[]{"null", null})

            // ColumnType.BYTES
            .add(new Object[]{"bytes", "some bytes".getBytes()})
            .add(new Object[]{"bytes_empty", "".getBytes()})

            // ColumnType.DATE
            .add(new Object[]{"date", LocalDate.of(2018, 6, 5)})
            .add(new Object[]{"date_zero", LocalDate.of(1970, 1, 1)})

            // ColumnType.FIXED_POINT
            .add(new Object[]{"fixed_point_decimal", new BigDecimal("123.456")})
            .add(new Object[]{"fixed_point_integral", new BigDecimal("123")})
            .add(new Object[]{"fixed_point_zero", new BigDecimal("0")})

            // ColumnType.FLOATING_POINT
            .add(new Object[]{"floating_point_from_double_decimal", Double.valueOf("3.14159")})
            .add(new Object[]{"floating_point_from_double_integral", Double.valueOf("123")})
            .add(new Object[]{"floating_point_from_double_zero", Double.valueOf("0")})
            .add(new Object[]{"floating_point_from_float_decimal", Float.valueOf("3.14159")})
            .add(new Object[]{"floating_point_from_float_integral", Float.valueOf("123")})
            .add(new Object[]{"floating_point_from_float_zero", Float.valueOf("0")})

            // ColumnType.INTEGER
            .add(new Object[]{"integer_byte_const", Byte.valueOf("123")})
            .add(new Object[]{"integer_byte_max", Byte.MAX_VALUE})
            .add(new Object[]{"integer_byte_min", Byte.MIN_VALUE})
            .add(new Object[]{"integer_byte_zero", Byte.valueOf("0")})
            .add(new Object[]{"integer_int_const", Integer.valueOf("123")})
            .add(new Object[]{"integer_int_max", Integer.MAX_VALUE})
            .add(new Object[]{"integer_int_min", Integer.MIN_VALUE})
            .add(new Object[]{"integer_int_zero", Integer.valueOf("0")})
            .add(new Object[]{"integer_long_const", Long.valueOf("123")})
            .add(new Object[]{"integer_long_max", Long.MAX_VALUE})
            .add(new Object[]{"integer_long_min", Long.MIN_VALUE})
            .add(new Object[]{"integer_long_zero", Long.valueOf("0")})
            .add(new Object[]{"integer_short_const", Short.valueOf("123")})
            .add(new Object[]{"integer_short_max", Short.MAX_VALUE})
            .add(new Object[]{"integer_short_min", Short.MIN_VALUE})
            .add(new Object[]{"integer_short_zero", Short.valueOf("0")})
            .add(new Object[]{"integer_boolean_false", Boolean.FALSE})
            .add(new Object[]{"integer_boolean_true", Boolean.TRUE})

            // ColumnType.STRING
            .add(new Object[]{"string", "some string"})
            .add(new Object[]{"string_empty", ""})

            // ColumnType.TIMESTAMP
            .add(new Object[]{"timestamp", LocalDateTime.of(2018, 6, 5, 14, 22, 33)})
            .add(new Object[]{"timestamp_with_nanoseconds", LocalDateTime.of(2018, 6, 5, 14, 22, 33, 123456)})
            .add(new Object[]{"timestamp_zero", LocalDateTime.of(1970, 1, 1, 0, 0)})

            // ColumnType.UNSIGNED_BIGINT
            .add(new Object[]{"unsigned_bigint", new BigInteger("1234")})
            .add(new Object[]{"unsigned_bigint_max", new BigInteger("1").shiftLeft(64).subtract(BigInteger.valueOf(1))})
            .add(new Object[]{"unsigned_bigint_zero", new BigInteger("0")})
            .build();

    private final String columnName;
    private final Object value;

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    public BinlogEventTypesTest(String columnName, Object value) {
        this.columnName = columnName;
        if (columnName.equals("null")) {
            this.value = null;
        } else {
            this.value = value;
        }
    }

    @Parameterized.Parameters(name = "typeConversions({0})")
    public static Iterable<Object[]> data() {
        return ASSETS;
    }

    /**
     * Проверка работоспособности всех возможных типов.
     * Нормализованные значения до и после должны совпадать.
     */
    @Test
    public void typeConversions() {
        Map<String, Object> nameAndValue = mapOf(columnName, value);
        BinlogEvent.Row oldRow = new BinlogEvent.Row()
                .withRowIndex(1)
                .withPrimaryKey(nameAndValue)
                .withBefore(nameAndValue)
                .withAfter(nameAndValue)
                .validate();
        BinlogEvent.Row newRow = BinlogEvent.Row.fromProtobuf(oldRow.toProtobuf()).validate();

        Map<String, Supplier<Object>> listNamesAndGetters = Map.of(
                "primaryKey", () -> newRow.getPrimaryKey().values().iterator().next(),
                "before", () -> newRow.getBefore().values().iterator().next(),
                "after", () -> newRow.getAfter().values().iterator().next());

        for (Map.Entry<String, Supplier<Object>> listNameAndGetter : listNamesAndGetters
                .entrySet()) {
            String description = "Checking " + listNameAndGetter.getKey() + " value for " + columnName;
            Object checkingValue = listNameAndGetter.getValue().get();
            ColumnType.Normalized realValue = ColumnType.normalize(value);
            if (realValue == null) {
                softly.assertThat(checkingValue)
                        .describedAs(description)
                        .isNull();
            } else {
                switch (realValue.getType()) {
                    case BYTES:
                        softly.assertThat((byte[]) checkingValue)
                                .describedAs(description)
                                .isEqualTo(value);
                        break;
                    case FLOATING_POINT:
                        softly.assertThat((double) checkingValue)
                                .describedAs(description)
                                .isEqualTo((double) realValue.getObject());
                        break;
                    case INTEGER:
                        softly.assertThat((long) checkingValue)
                                .describedAs(description)
                                .isEqualTo((long) realValue.getObject());
                        break;
                    default:
                        softly.assertThat(checkingValue)
                                .describedAs(description)
                                .isEqualTo(value);
                }
            }
        }
    }

    private Map<String, Object> mapOf(String key, Object value) {
        var map = new HashMap<String, Object>();
        map.put(key, value);
        return map;
    }
}
