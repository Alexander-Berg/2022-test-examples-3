package ru.yandex.market.common.excel.out.impl.template;

import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.excel.MarketTemplate;
import ru.yandex.market.common.excel.XlsConfig;
import ru.yandex.market.common.excel.template.impl.HeadersConverter;

/**
 * Тесты на логику работы {@link HeadersConverter}.
 *
 * @author fbokovikov
 */
@SuppressWarnings("unused")
class TemplateConversionTest extends AbstractTemplateTest {

    @Nonnull
    private static Stream<Arguments> getArguments() {
        return Stream.of(
                Arguments.of("xls/book_template.xls", MarketTemplate.BOOKS),
                Arguments.of("xls/music_template.xls", MarketTemplate.MUSIC),
                Arguments.of("xls/common_template.xls", MarketTemplate.COMMON),
                Arguments.of("xls/common_template_line_wrapping.xls", MarketTemplate.COMMON),
                Arguments.of("xls/supplier_feed.xls", MarketTemplate.SUPPLIER),
                Arguments.of("xls/problem_feed.xlsx", MarketTemplate.COMMON),
                Arguments.of("xls/book_template.xlsm", MarketTemplate.BOOKS),
                Arguments.of("xls/music_template.xlsm", MarketTemplate.MUSIC),
                Arguments.of("xls/common_template.xlsm", MarketTemplate.COMMON),
                Arguments.of("xls/common_template_line_wrapping.xlsm", MarketTemplate.COMMON),
                Arguments.of("xls/supplier_feed.xlsm", MarketTemplate.SUPPLIER),
                Arguments.of("xls/stock_template.xlsx", MarketTemplate.STOCK),
                Arguments.of("xls/price_template.xlsm", MarketTemplate.PRICE),
                Arguments.of("xls/marketplace-auction-list.xlsm", MarketTemplate.ADV_SHOP_BID)
        );
    }

    @DisplayName("Проверяем, что все русские колонки нормально конвертнулись")
    @MethodSource("getArguments")
    @ParameterizedTest(name = "file_name = {0}")
    void testColumnNamesConverting(String fileName, MarketTemplate marketTemplate) {
        assertNamesConverted(fileName);
    }

    @DisplayName("Тест проверяет, что шаблон распознается как маркетный")
    @MethodSource("getArguments")
    @ParameterizedTest(name = "file_name = {0}")
    void testIsMarketTemplate(String fileName, MarketTemplate marketTemplate) {
        XlsConfig xlsConfig = XlsConfig.readConfig(createWorkbook(fileName));

        Assertions.assertThat(xlsConfig.getMarketTemplate())
                .isEqualTo(marketTemplate);
    }

    @DisplayName("Тест на метод 'MarketTemplate#getXlsConfig(Workbook)', негативный сценарий.")
    @Test
    void getXlsConfig_workbook_negative() throws Exception {
        URL res = getClass().getClassLoader().getResource("xls/non_market_template.xls");
        URI uri = Objects.requireNonNull(res).toURI();

        Workbook workbook = WorkbookFactory.create(Paths.get(uri).toFile(), null, true);
        XlsConfig xlsConfig = XlsConfig.readConfig(workbook);

        Assertions.assertThat(xlsConfig.getMarketTemplate())
                .isEqualTo(MarketTemplate.NONE);
    }

    @DisplayName("Тест на метод 'MarketTemplate#getXlsConfig(XSSFReader)', негативный сценарий.")
    @Test
    void getXlsConfig_xssfReader_negative() throws Exception {
        URL res = getClass().getClassLoader().getResource("xls/non_market_template.xlsm");
        URI uri = Objects.requireNonNull(res).toURI();

        try (OPCPackage p = OPCPackage.open(Paths.get(uri).toFile(), PackageAccess.READ)) {
            XSSFReader xssfReader = new XSSFReader(p);
            XlsConfig xlsConfig = XlsConfig.readConfig(xssfReader);

            Assertions.assertThat(xlsConfig.getMarketTemplate())
                    .isEqualTo(MarketTemplate.NONE);
        }
    }

    @DisplayName("Проверка конвертации файла из excel в csv")
    @CsvSource({
            "supplier_feed.xls,Файл без настроек xls",
            "supplier_feed.xlsm,Файл без настроек xlsm",
            "supplier_feed_with_settings.xls,Файл c настройками xls",
            "supplier_feed_with_settings.xlsm,Файл c настройками xlsm",
    })
    @ParameterizedTest(name = "{1}")
    void convert_file_correctLines(String fileName, String name) {
        String[] fileLines = getConvertedFileResourceAsString("xls/" + fileName)
                .split("\n");

        Assertions.assertThat(fileLines)
                .hasSize(6);
        Assertions.assertThat(fileLines[0])
                .isEqualTo("shop-sku;name;category;barcode;price;vat;market-sku;disabled");
        Assertions.assertThat(fileLines[1])
                .isEqualTo("12222;\"Дрель Makita 6413 суперполезная в быту, лёгкая и отлично сверлит\";" +
                        "Дрели и миксеры;4607004650642;0;0;;");
    }
}
