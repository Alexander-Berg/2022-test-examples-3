package ru.yandex.market.common.excel.out.impl.template;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.excel.InternalColumnName;
import ru.yandex.market.common.excel.MarketTemplate;
import ru.yandex.market.common.excel.XlsColumnName;
import ru.yandex.market.common.excel.XlsConfig;

/**
 * @author fbokovikov
 */
@ParametersAreNonnullByDefault
class ColumnNameTest {

    @Nonnull
    private static Stream<Arguments> getArguments() {
        return Stream.of(
                Arguments.of("Валюта", MarketTemplate.BOOKS, ImmutableList.of("currencyId")),
                Arguments.of("Категория", MarketTemplate.MUSIC, ImmutableList.of("category")),
                Arguments.of("Валюта", MarketTemplate.BOOKS, ImmutableList.of("currencyId")),
                Arguments.of("Категория", MarketTemplate.MUSIC, ImmutableList.of("category")),
                Arguments.of("Статус товара", MarketTemplate.COMMON, ImmutableList.of("available")),
                Arguments.of("Название", MarketTemplate.MUSIC, ImmutableList.of("title")),
                Arguments.of("Название", MarketTemplate.COMMON, ImmutableList.of("name")),
                Arguments.of("Название", MarketTemplate.BOOKS, ImmutableList.of("name")),
                Arguments.of("Купить в магазине без заказа", MarketTemplate.COMMON, ImmutableList.of("store")),
                Arguments.of("Страна исполнителя", MarketTemplate.MUSIC, ImmutableList.of("country")),
                Arguments.of("Штрихкод", MarketTemplate.COMMON, ImmutableList.of("barcode")),
                Arguments.of("Штрихкод", MarketTemplate.MUSIC, ImmutableList.of("barcode")),
                Arguments.of("Срок доставки", MarketTemplate.MUSIC, ImmutableList.of("local_delivery_days")),
                Arguments.of("Срок доставки", MarketTemplate.COMMON, ImmutableList.of("local_delivery_days")),
                Arguments.of("Срок доставки", MarketTemplate.BOOKS, ImmutableList.of("local_delivery_days")),
                Arguments.of("Стоимость доставки", MarketTemplate.MUSIC, ImmutableList.of("local_delivery_cost")),
                Arguments.of("Стоимость доставки", MarketTemplate.BOOKS, ImmutableList.of("local_delivery_cost")),
                Arguments.of("Стоимость доставки", MarketTemplate.COMMON, ImmutableList.of("local_delivery_cost")),

                //шаблон фида поставщика
                Arguments.of("Штрихкод", MarketTemplate.SUPPLIER, ImmutableList.of("barcode")),
                Arguments.of("НДС", MarketTemplate.SUPPLIER, ImmutableList.of("vat")),
                Arguments.of("Категория", MarketTemplate.SUPPLIER, ImmutableList.of("category")),
                Arguments.of("SKU на Яндексе", MarketTemplate.SUPPLIER, ImmutableList.of("market-sku")),
                Arguments.of("Ваш SKU", MarketTemplate.SUPPLIER, ImmutableList.of("shop-sku")),
                Arguments.of("Название товара", MarketTemplate.SUPPLIER, ImmutableList.of("name")),
                Arguments.of("Убрать из продажи", MarketTemplate.SUPPLIER, ImmutableList.of("disabled")),

                //шаблон стокового фида
                Arguments.of("Ваш SKU", MarketTemplate.STOCK, ImmutableList.of("shop-sku")),
                Arguments.of("Название товара", MarketTemplate.STOCK, ImmutableList.of("name")),
                Arguments.of("Доступное количество товара", MarketTemplate.STOCK, ImmutableList.of("count")),

                //шаблон ценового фида
                Arguments.of("Ваша цена", MarketTemplate.PRICE, ImmutableList.of("price")),
                Arguments.of("Цена до скидки", MarketTemplate.PRICE, ImmutableList.of("oldprice")),
                Arguments.of("НДС", MarketTemplate.PRICE, ImmutableList.of("vat")),
                Arguments.of("Ваш SKU", MarketTemplate.PRICE, ImmutableList.of("shop-sku")),
                Arguments.of("Название товара", MarketTemplate.PRICE, ImmutableList.of("name")),
                Arguments.of("Убрать из продажи", MarketTemplate.PRICE, ImmutableList.of("disabled"))
        );
    }

    @DisplayName("Тест проверяет, что названия столбцов маркетных шаблонов " +
            "корректно конвертируются в фидчекерные значения.")
    @MethodSource("getArguments")
    @ParameterizedTest(name = "column_name = {0}; column_keys = {2}; template = {1}")
    void getInMapping_nameConversion_correct(String templateName, MarketTemplate marketTemplate,
                                             List<String> expectedCheckerNames) {
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Инструкция");
        workbook.createSheet();
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue(marketTemplate.getTemplateIds()
                .get(0)
                .getCode());
        XlsConfig xlsConfig = XlsConfig.readConfig(workbook);
        List<InternalColumnName> csvNames = xlsConfig.getInMapping(new XlsColumnName(templateName));
        Assertions.assertThat(csvNames.stream().map(InternalColumnName::getName).collect(Collectors.toList()))
                .isEqualTo(expectedCheckerNames);
    }
}
