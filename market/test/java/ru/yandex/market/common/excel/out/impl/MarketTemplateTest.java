package ru.yandex.market.common.excel.out.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.excel.InternalColumnName;
import ru.yandex.market.common.excel.MarketTemplate;
import ru.yandex.market.common.excel.XlsColumnName;
import ru.yandex.market.common.excel.XlsConfig;
import ru.yandex.market.common.excel.wrapper.PoiWorkbook;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
class MarketTemplateTest {

    @Nonnull
    private static Stream<Arguments> provideMarketTemplatePOIWorkbookArguments() {
        return Stream.concat(
                Stream.of(
                        Arguments.of(MarketTemplate.SUPPLIER, "mbi_26584.xls"),
                        Arguments.of(MarketTemplate.SUPPLIER, "mbi_26584-1.xls"),
                        Arguments.of(MarketTemplate.BOOKS, "book_template.xls"),
                        Arguments.of(MarketTemplate.MUSIC, "music_template.xls"),
                        Arguments.of(MarketTemplate.NONE, "non_market_template.xls"),
                        Arguments.of(MarketTemplate.COMMON, "common_template.xls"),
                        Arguments.of(MarketTemplate.COMMON, "problem_feed-2018-29-05.xls"),
                        Arguments.of(MarketTemplate.COMMON, "mbi_31803.xls"),
                        Arguments.of(MarketTemplate.COMMON, "mbi_36598.xls"),
                        Arguments.of(MarketTemplate.ADV_SHOP_BID, "marketplace-auction-list.xlsm")
                ), provideMarketTemplateArguments());
    }

    @Nonnull
    private static Stream<Arguments> provideMarketTemplateOPCPackageArguments() {
        return Stream.concat(
                Stream.of(
                        Arguments.of(MarketTemplate.SUPPLIER, "mbi_26584.xlsm"),
                        Arguments.of(MarketTemplate.SUPPLIER, "mbi_26584-1.xlsm"),
                        Arguments.of(MarketTemplate.BOOKS, "book_template.xlsm"),
                        Arguments.of(MarketTemplate.MUSIC, "music_template.xlsm"),
                        Arguments.of(MarketTemplate.NONE, "non_market_template.xlsm"),
                        Arguments.of(MarketTemplate.COMMON, "common_template.xlsm"),
                        Arguments.of(MarketTemplate.COMMON, "problem_feed-2018-29-05.xlsm"),
                        Arguments.of(MarketTemplate.COMMON, "mbi_31803.xlsm"),
                        Arguments.of(MarketTemplate.COMMON, "mbi_36598.xlsm"),
                        Arguments.of(MarketTemplate.ADV_SHOP_BID, "marketplace-auction-list.xlsm")
                ), provideMarketTemplateArguments());
    }

    @Nonnull
    private static Stream<Arguments> provideMarketTemplateArguments() {
        return Stream.of(
                Arguments.of(MarketTemplate.ALCOHOL, "alcohol_template.xlsm"),
                Arguments.of(MarketTemplate.COMMON, "standard_configurable_template.xlsm"),
                Arguments.of(MarketTemplate.MUSIC, "music_configurable_template.xlsm"),
                Arguments.of(MarketTemplate.BOOKS, "books_configurable_template.xlsm"),
                Arguments.of(MarketTemplate.SUPPLIER, "catalog-tovarov-dlya-beru.xlsm"),
                Arguments.of(MarketTemplate.NONE, "ozon_feed.xlsx"),
                Arguments.of(MarketTemplate.NONE, "wlb_feed.xlsx"),
                Arguments.of(MarketTemplate.NONE, "goods_feed.xlsx"),
                Arguments.of(MarketTemplate.NONE, "kupivip_feed.xlsm"),
                Arguments.of(MarketTemplate.STOCK, "stock_template.xlsx"),
                Arguments.of(MarketTemplate.PRICE, "price_template.xlsm"),
                Arguments.of(MarketTemplate.ADV_SHOP_BID, "marketplace-auction-list.xlsm")
        );
    }

    @DisplayName("Уникальные шаблоны по названию категории")
    @Test
    void checkUniqueness_byCategory_successful() {
        checkUniqueness(MarketTemplate::getCategory);
    }

    @DisplayName("Уникальные шаблоны по коду категории")
    @Test
    void checkUniqueness_byCategoryCode_successful() {
        checkUniqueness(MarketTemplate::getCategoryCode);
    }

    @DisplayName("Уникальные шаблоны по списку идентификаторов")
    @Test
    void checkUniqueness_byTemplateIds_successful() {
        checkUniqueness(MarketTemplate::getTemplateIds);
    }

    @DisplayName("Связка шаблон - код категория 1 к 1")
    @Test
    void byCategoryCode_categoryCode_successful() {
        for (MarketTemplate marketTemplate : MarketTemplate.values()) {
            assertThat(MarketTemplate.byCategoryCode(marketTemplate.getCategoryCode()))
                    .isSameAs(marketTemplate);
        }
    }

    @DisplayName("Связка шаблон - категория 1 к 1")
    @Test
    void byCategory_categoryCode_successful() {
        for (MarketTemplate marketTemplate : MarketTemplate.values()) {
            assertThat(MarketTemplate.byCategory(marketTemplate.getCategory()))
                    .isSameAs(marketTemplate);
        }
    }

    @Test
    void byId() {
        assertThat(MarketTemplate.byId("NONE")).isEqualTo(MarketTemplate.NONE);
        assertThat(MarketTemplate.byId("ЯМ102017О")).isEqualTo(MarketTemplate.COMMON);
        assertThat(MarketTemplate.byId("ЯМ012019О")).isEqualTo(MarketTemplate.COMMON);
        assertThat(MarketTemplate.byId("ЧВЗ100С")).isEqualTo(MarketTemplate.SUPPLIER);
    }

    @Test
    void byNullsShouldReturnNONE() {
        assertThat(MarketTemplate.byId(null)).isSameAs(MarketTemplate.NONE);
        assertThat(MarketTemplate.byCategory(null)).isSameAs(MarketTemplate.NONE);
        assertThat(MarketTemplate.byCategoryCode(-1)).isSameAs(MarketTemplate.NONE);
    }

    @DisplayName("Открытие файла и получения типа шаблона через OPCPackage")
    @MethodSource("provideMarketTemplatePOIWorkbookArguments")
    @ParameterizedTest(name = "file = {1}")
    void open_opcPackage_correctTemplate(MarketTemplate marketTemplate, String fileName) throws Exception {
        assertTemplateResolvedTo(marketTemplate, "xls/" + fileName);
    }

    @DisplayName("Открытие файла и получения типа шаблона через PoiWorkbook")
    @MethodSource("provideMarketTemplateOPCPackageArguments")
    @ParameterizedTest(name = "file_name = {1}")
    void load_poiWorkbook_correctTemplate(MarketTemplate marketTemplate, String fileName) throws Exception {
        assertTemplateStreamResolvedTo(marketTemplate, "xls/" + fileName);
    }

    @DisplayName("Извлечение из конфигурации шаблона маппинга колонок 1 to Many")
    @Test
    void multipleColumnMapping() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("xls/mbi-74325.xlsx")) {
            try (PoiWorkbook poiWorkbook = PoiWorkbook.load(Objects.requireNonNull(inputStream))) {
                XlsConfig xlsConfig = XlsConfig.readConfig(poiWorkbook);
                Collection<InternalColumnName> mapping =
                        xlsConfig.getColumns().get(new XlsColumnName("С какого возраста пользоваться"));
                assertThat(mapping).containsExactly(new InternalColumnName("age"), new InternalColumnName("age_unit"));
            }
        }
    }

    private void assertTemplateResolvedTo(MarketTemplate expectedTemplate,
                                          String resourceName) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            try (PoiWorkbook poiWorkbook = PoiWorkbook.load(Objects.requireNonNull(inputStream))) {
                XlsConfig xlsConfig = XlsConfig.readConfig(poiWorkbook);
                assertThat(xlsConfig.getMarketTemplate())
                        .isEqualTo(expectedTemplate);
            }
        }
    }

    private void assertTemplateStreamResolvedTo(MarketTemplate expectedTemplate,
                                                String resourceName) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            try (OPCPackage opcPackage = OPCPackage.open(Objects.requireNonNull(inputStream))) {
                XSSFReader xssfReader = new XSSFReader(opcPackage);

                XlsConfig xlsConfig = XlsConfig.readConfig(xssfReader);
                assertThat(xlsConfig.getMarketTemplate())
                        .isEqualTo(expectedTemplate);
            }
        }
    }

    private void checkUniqueness(Function<MarketTemplate, Object> function) {
        MarketTemplate[] values = MarketTemplate.values();
        Set<Object> unique = Arrays.stream(values)
                .map(function)
                .collect(Collectors.toSet());

        assertThat(unique)
                .hasSameSizeAs(values);
    }
}
