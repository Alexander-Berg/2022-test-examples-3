package ru.yandex.market.stat.dicts.utils;

import com.google.common.collect.Maps;
import org.junit.Test;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.stat.dicts.common.ConversionStrategy;
import ru.yandex.market.stat.dicts.loaders.jdbc.FieldMetadata;
import ru.yandex.market.stat.dicts.loaders.jdbc.RowMetadata;
import ru.yandex.market.stat.dicts.loaders.jdbc.SchemelessDictionaryRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class YtServiceUtilTest {
    @Test
    public void testBigDecimalFormat() {
        assertThat(YtServiceUtils.bigDecimalToString(new BigDecimal("0.00")),
            is("0"));
        assertThat(YtServiceUtils.bigDecimalToString(new BigDecimal("1234.32")),
            is("1234.32"));
        assertThat(YtServiceUtils.bigDecimalToString(new BigDecimal("-0.0000000001")),
            is("-0.0000000001"));
        assertThat(YtServiceUtils.bigDecimalToString(new BigDecimal(Long.MAX_VALUE).multiply(new BigDecimal(2))),
            is("18446744073709551614"));
        assertThat(YtServiceUtils.bigDecimalToString(new BigDecimal(
            new BigDecimal(Long.MAX_VALUE).multiply(new BigDecimal(2)).toString() +
                ".0000000001")),
            is("18446744073709551614.0000000001"));
    }

    @Test
    public void testLocale() {
        Locale defaultLocale = Locale.getDefault();
        // лягушатнички запятой отделяюч
        Locale.setDefault(Locale.FRANCE);
        try {
            assertThat(YtServiceUtils.bigDecimalToString(new BigDecimal("1234.32")),
                is("1234.32"));
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void testLegacyNullConversion() {
        SchemelessDictionaryRecord record = schemelessDictionaryRecord();

        YTreeMapNode ysonNode = YtServiceUtils.convertToYsonNode(record, LocalDate.now(), ConversionStrategy.LEGACY);
        assertThat(ysonNode.getString("string_field"), is (""));
        assertThat(ysonNode.getBool("boolean_field"), is (false));
        assertThat(ysonNode.getInt("int_field"), is (0));
        assertThat(ysonNode.getListO("collection_field"), is (Optional.empty()));
//      Нет, я не знаю, почему null в YTreeEntityNodeImpl конвертируется в решетку,
//      На нулловой ноде ни один родной метод не работает (getString getDouble и т.д.), так что выкручиваюсь вот так
//      У кого есть дока - поделитесь?)
        assertThat(ysonNode.getListO("collection_field_double").get().get(0).toString(), is ("#"));
        assertThat(ysonNode.getListO("collection_field_double").get().getDouble(1), is (50.332D));

    }


    @Test
    public void testStandardNullConversion() {
        SchemelessDictionaryRecord record = schemelessDictionaryRecord();

        YTreeMapNode ysonNode = YtServiceUtils.convertToYsonNode(record, LocalDate.now(), ConversionStrategy.STANDARD);

        assertThat(ysonNode.getStringO("string_field"), is (Optional.empty()));
        assertThat(ysonNode.getBoolO("boolean_field"), is (Optional.empty()));
        assertThat(ysonNode.getIntO("int_field"), is (Optional.empty()));
        assertThat(ysonNode.getListO("collection_field"), is (Optional.empty()));
//      Нет, я не знаю, почему null в YTreeEntityNodeImpl конвертируется в решетку,
//      На нулловой ноде ни один родной метод не работает (getString getDouble и т.д.), так что выкручиваюсь вот так
//      У кого есть дока - поделитесь?)
        assertThat(ysonNode.getListO("collection_field_double").get().get(0).toString(), is ("#"));
        assertThat(ysonNode.getListO("collection_field_double").get().getDouble(1), is (50.332D));

    }

    private SchemelessDictionaryRecord schemelessDictionaryRecord() {
        List<FieldMetadata> fieldMetadata = Arrays.asList(
                FieldMetadata.builder().name("string_field").columnName("string_field").javaClass(String.class).build(),
                FieldMetadata.builder().name("boolean_field").columnName("boolean_field").javaClass(Boolean.class).build(),
                FieldMetadata.builder().name("int_field").columnName("int_field").javaClass(Integer.class).build(),
                FieldMetadata.builder().name("collection_field").columnName("collection_field").javaClass(List.class).build(),
                FieldMetadata.builder().name("collection_field_double").columnName("collection_field_double").javaClass(List.class).build(),
                FieldMetadata.builder().name("collection_field_nulls").columnName("collection_field_nulls").javaClass(List.class).build()
        );
        RowMetadata rowMetadata = new RowMetadata("test", fieldMetadata);
        Map<String, Object> values = Maps.newHashMap();
        values.put("string_field", null);
        values.put("boolean_field", null);
        values.put("int_field", null);
        values.put("collection_field", null);
        values.put("collection_field_double", Arrays.asList(new Double[] {null, 50.332D}));
        values.put("collection_field_nulls", Arrays.asList(new Double[] {null, null}));
        return new SchemelessDictionaryRecord(rowMetadata, values);
    }
}
