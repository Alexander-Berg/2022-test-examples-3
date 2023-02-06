package ru.yandex.market.common.excel.out.impl.template;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.excel.InternalColumnName;
import ru.yandex.market.common.excel.MarketTemplate;
import ru.yandex.market.common.excel.template.TemplateConverterRules;

class CommonTemplateTest {

    @Nonnull
    private static Stream<Arguments> getArguments() {
        return Stream.of(
                Arguments.of(MarketTemplate.COMMON, "available", "Да", "1"),
                Arguments.of(MarketTemplate.COMMON, "available", "Нет", "0"),
                Arguments.of(MarketTemplate.MUSIC, "available", "Завтра", "Завтра"),
                Arguments.of(MarketTemplate.COMMON, "available", null, ""),
                Arguments.of(MarketTemplate.BOOKS, "barcode", "1646514", "1646514"),
                Arguments.of(MarketTemplate.COMMON, "category", "Спорт-товары", "Спорт-товары"),
                Arguments.of(MarketTemplate.BOOKS, "cpa", "Да", "1"),
                Arguments.of(MarketTemplate.COMMON, "cpa", "Нет", "0"),
                Arguments.of(MarketTemplate.COMMON, "cpa", "YES", "YES"),
                Arguments.of(MarketTemplate.COMMON, "cpa", null, ""),
                Arguments.of(MarketTemplate.COMMON, "cbid", "31543151", "31543151"),
                Arguments.of(MarketTemplate.BOOKS, "description", "Описание оффера", "Описание оффера"),
                Arguments.of(MarketTemplate.COMMON, "delivery", "Есть", "1"),
                Arguments.of(MarketTemplate.COMMON, "delivery", "Нет", "0"),
                Arguments.of(MarketTemplate.BOOKS, "delivery", "True", "True"),
                Arguments.of(MarketTemplate.MUSIC, "delivery", null, ""),
                Arguments.of(MarketTemplate.COMMON, "store", "Можно", "1"),
                Arguments.of(MarketTemplate.COMMON, "store", "Нельзя", "0"),
                Arguments.of(MarketTemplate.MUSIC, "store", "Можно купить", "Можно купить"),
                Arguments.of(MarketTemplate.BOOKS, "store", null, ""),
                Arguments.of(MarketTemplate.COMMON, "currencyId", "RUR", "RUR"),
                Arguments.of(MarketTemplate.COMMON, "author", "Сарамаго", "Сарамаго"),
                Arguments.of(MarketTemplate.COMMON, "language", "rus", "rus"),
                Arguments.of(MarketTemplate.COMMON, "manufacturer_warranty", "Есть", "1"),
                Arguments.of(MarketTemplate.MUSIC, "manufacturer_warranty", "Нет", "0"),
                Arguments.of(MarketTemplate.BOOKS, "manufacturer_warranty", null, ""),
                Arguments.of(MarketTemplate.COMMON, "manufacturer_warranty", "Hello world", "Hello world"),

                //фид поставщика
                Arguments.of(MarketTemplate.SUPPLIER, "shop-sku", "qwerty", "qwerty"),
                Arguments.of(MarketTemplate.SUPPLIER, "market-sku", "100400", "100400"),
                Arguments.of(MarketTemplate.SUPPLIER, "name", "value", "value"),
                Arguments.of(MarketTemplate.SUPPLIER, "category", "cat_name", "cat_name"),
                Arguments.of(MarketTemplate.SUPPLIER, "price", "1000", "1000"),
                Arguments.of(MarketTemplate.SUPPLIER, "vat", "VAT_18", "VAT_18"),
                Arguments.of(MarketTemplate.SUPPLIER, "disabled", "Да", "1"),
                Arguments.of(MarketTemplate.SUPPLIER, "disabled", "Нет", "0"),
                Arguments.of(MarketTemplate.SUPPLIER, "price", "123", "123"),
                Arguments.of(MarketTemplate.SUPPLIER, "price", "123.43", "123.43"),
                Arguments.of(MarketTemplate.SUPPLIER, "price", "123,43", "123.43"),
                Arguments.of(MarketTemplate.SUPPLIER, "market-sku", "100400  ", "100400"),
                Arguments.of(MarketTemplate.SUPPLIER, "market-sku", "  100400", "100400"),
                Arguments.of(MarketTemplate.SUPPLIER, "market-sku", "  100400  ", "100400"),
                Arguments.of(MarketTemplate.SUPPLIER, "shop-sku", "100400  ", "100400"),
                Arguments.of(MarketTemplate.SUPPLIER, "shop-sku", "  100400", "100400"),
                Arguments.of(MarketTemplate.SUPPLIER, "shop-sku", "  100400  ", "100400"),
                Arguments.of(MarketTemplate.SUPPLIER, "shop-sku", " \n  100400 \r  ", "100400"),
                Arguments.of(MarketTemplate.SUPPLIER, "shop-sku", "100400\u00A0\u00A0", "100400"),

                //Игнорирование регистра
                Arguments.of(MarketTemplate.SUPPLIER, "shop-sku", "MeIsNotIgnoringCase", "MeIsNotIgnoringCase"),
                Arguments.of(MarketTemplate.SUPPLIER, "market-sku", "100400", "100400"),
                Arguments.of(MarketTemplate.SUPPLIER, "name", "UndefinedValue", "UndefinedValue"),
                Arguments.of(MarketTemplate.SUPPLIER, "price", "1000", "1000"),
                Arguments.of(MarketTemplate.SUPPLIER, "vat", "VAT_18", "VAT_18"),
                Arguments.of(MarketTemplate.SUPPLIER, "disabled", "Да", "1"),
                Arguments.of(MarketTemplate.SUPPLIER, "disabled", "да", "1"),
                Arguments.of(MarketTemplate.SUPPLIER, "disabled", "ДА", "1"),
                Arguments.of(MarketTemplate.SUPPLIER, "disabled", "НеТ", "0"),
                Arguments.of(MarketTemplate.COMMON, "delivery", "Есть", "1"),
                Arguments.of(MarketTemplate.COMMON, "delivery", "ЕстЬ", "1"),
                Arguments.of(MarketTemplate.COMMON, "delivery", "Нет ", "Нет "),
                Arguments.of(MarketTemplate.MUSIC, "delivery", null, ""),
                Arguments.of(MarketTemplate.COMMON, "store", "МожНО", "1"),
                Arguments.of(MarketTemplate.COMMON, "store", "нельзя", "0"),

                //шаблон стокового фида
                Arguments.of(MarketTemplate.STOCK, "shop-sku", " MeIsNotIgnoringCase", "MeIsNotIgnoringCase"),
                Arguments.of(MarketTemplate.STOCK, "name", "UndefinedValue", "UndefinedValue"),
                Arguments.of(MarketTemplate.STOCK, "count", "300 ", "300 "),

                //шаблон ценового фида
                Arguments.of(MarketTemplate.PRICE, "price", "123,43", "123.43"),
                Arguments.of(MarketTemplate.PRICE, "price", "123,43", "123.43"),
                Arguments.of(MarketTemplate.PRICE, "price", "123.43", "123.43"),
                Arguments.of(MarketTemplate.PRICE, "oldprice", "123", "123"),
                Arguments.of(MarketTemplate.PRICE, "oldprice", "123.43", "123.43"),
                Arguments.of(MarketTemplate.PRICE, "oldprice", "123,43", "123.43"),
                Arguments.of(MarketTemplate.PRICE, "vat", "VAT_18", "VAT_18"),
                Arguments.of(MarketTemplate.PRICE, "vat", "5", "5"),
                Arguments.of(MarketTemplate.PRICE, "shop-sku", "100400  ", "100400"),
                Arguments.of(MarketTemplate.PRICE, "name", "UndefinedValue", "UndefinedValue"),
                Arguments.of(MarketTemplate.PRICE, "disabled", "Да", "1"),
                Arguments.of(MarketTemplate.PRICE, "disabled", "да", "1"),
                Arguments.of(MarketTemplate.PRICE, "disabled", "ДА", "1"),
                Arguments.of(MarketTemplate.PRICE, "disabled", "НеТ", "0")
        );
    }

    @DisplayName("Проверяем корректность конвертации значений в столбцах")
    @MethodSource("getArguments")
    @ParameterizedTest(name = "column_name = {1}; template_value = ({2}); expected_value = ({3})")
    void convertValue_conversion_correct(MarketTemplate marketTemplate,
                                         String columnName,
                                         String templateValue,
                                         String expectedValue) {
        InternalColumnName internalColumnName = new InternalColumnName(columnName);
        String actualValue = TemplateConverterRules.convertValue(marketTemplate, internalColumnName, templateValue);

        Assertions.assertThat(actualValue)
                .isEqualTo(expectedValue);
    }
}
