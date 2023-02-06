package ru.yandex.market.core.feed.validation.result;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.excel.MarketTemplate;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.supplier.model.IndexerErrorInfo;
import ru.yandex.market.core.supplier.model.OfferInfo;

import static ru.yandex.market.core.feed.validation.result.ValidationResultTestUtil.getValidationResultOnlyErrorTestCollection;
import static ru.yandex.market.core.feed.validation.result.ValidationResultTestUtil.getValidationResultTestCollection;
import static ru.yandex.market.core.feed.validation.result.ValidationResultTestUtil.getValidationResultWithoutErrorTestCollection;
import static ru.yandex.market.core.feed.validation.result.XlsTestUtils.buildExpectedMap;

/**
 * Date: 29.12.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class FeedTemplateXlsServiceTest extends FunctionalTest {

    @Qualifier("unitedFeedTemplateXlsService")
    @Autowired
    private FeedXlsService<OfferInfo> feedXlsService;

    @DisplayName("Проверка расширения файла")
    @ParameterizedTest(name = "MarketTemplate.{0} - {1}")
    @CsvSource({
            "DBS,.xlsx",
            "SUPPLIER,.xlsx",
            "PRICE,.xlsm",
            "STOCK,.xlsx"
    })
    void getXlsFileExtension_marketTemplate_extension(MarketTemplate template, String extension) {
        Assertions.assertThat(feedXlsService.getXlsFileExtension(template))
                .isEqualTo(extension);
    }

    @DisplayName("Проверка базового наименования файла")
    @ParameterizedTest(name = "MarketTemplate.{0} - {1}")
    @CsvSource({
            "DBS,shop_feed",
            "SUPPLIER,supplier_feed",
            "PRICE,price_feed",
            "STOCK,stock_feed"
    })
    void getDefaultFileName_marketTemplate_defaultName(MarketTemplate template, String extension) {
        Assertions.assertThat(feedXlsService.getDefaultFileName(template))
                .isEqualTo(extension);
    }

    @DisplayName("Проверка корректности заполнения шаблона фида. Не передали данные для заполенния.")
    @ParameterizedTest(name = "MarketTemplate.{0}")
    @CsvSource({
            "DBS,4,Ассортимент",
            "SUPPLIER,4,Ассортимент",
            "PRICE,3,Цены",
            "STOCK,2,Остатки"
    })
    void fillTemplate_emptyData_empty(MarketTemplate template, int rowCount, String sheetName) {
        feedXlsService.fillTemplate(
                template,
                Stream.of(),
                getPathConsumer(rowCount, sheetName, Collections.emptyMap())
        );
    }

    @SuppressWarnings("unused")
    @DisplayName("Проверка корректности заполнения шаблона фида. Только ошибки.")
    @ParameterizedTest(name = "MarketTemplate.{0}")
    @MethodSource("getOnlyErrorArguments")
    void fillTemplate_onlyError_successful(String name, MarketTemplate template, Consumer<Path> consumer) {
        var offers = getValidationResultOnlyErrorTestCollection();
        feedXlsService.fillTemplate(
                template,
                offers.stream(),
                consumer
        );
    }

    @SuppressWarnings("unused")
    @DisplayName("Проверка корректности заполнения шаблона фида. Только корректные фиды.")
    @ParameterizedTest(name = "MarketTemplate.{0}")
    @MethodSource("getOnlyCorrectOfferArguments")
    void fillTemplate_onlyCorrectOffer_successful(String name, MarketTemplate template, Consumer<Path> consumer) {
        var offers = getValidationResultWithoutErrorTestCollection(false);
        feedXlsService.fillTemplate(
                template,
                offers.stream(),
                consumer
        );
    }

    @SuppressWarnings("unused")
    @DisplayName("Проверка корректности заполнения шаблона фида.")
    @ParameterizedTest(name = "MarketTemplate.{0}")
    @MethodSource("getAllOffersArguments")
    void fillTemplate_allOffers_successful(String name, MarketTemplate template, Consumer<Path> consumer) {
        var offers = getValidationResultTestCollection(true, false);
        feedXlsService.fillTemplate(
                template,
                offers.stream(),
                consumer
        );
    }

    @DisplayName("Проверка корректности заполнения шаблона фида. Текст ошибки null.")
    @ParameterizedTest(name = "MarketTemplate.{0}")
    @CsvSource({
            "DBS,5,Ассортимент",
            "SUPPLIER,5,Ассортимент",
            "PRICE,4,Цены",
            "STOCK,3,Остатки"
    })
    void fillTemplate_errorWithNullDescription_empty(MarketTemplate template, int rowCount, String sheetName) {
        feedXlsService.fillTemplate(
                template,
                Stream.of(OfferInfo.builder()
                        .withIndexerErrorInfo(IndexerErrorInfo.builder()
                                .build())
                        .build()),
                getPathConsumer(rowCount, sheetName,
                        ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                .putAll(buildExpectedMap(rowCount - 1,
                                        "", "", "", "", "",
                                        "", "", "", "", "",
                                        "", "", "", "", "",
                                        "", "", "", "", "",
                                        "", "", "", "", "",
                                        "", "", "", "", "",
                                        "", "", "", "", "",
                                        "", "", "", "", "",
                                        "", "", "", "", "",
                                        ""
                                ))
                                .build()
                )
        );
    }

    @Nonnull
    private static Stream<Arguments> getOnlyErrorArguments() {
        return Stream.of(
                Arguments.of("DBS",
                        MarketTemplate.DBS,
                        getPathConsumer(8, "Ассортимент",
                                ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                        .putAll(buildExpectedMap(4,
                                                "", "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param." +
                                                        System.lineSeparator() + System.lineSeparator() +
                                                        "Не указана характеристика товара price:" +
                                                        "Укажите эту характеристику с помощью элемента param.",
                                                "300", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(5,
                                                "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param.",
                                                "", "301", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(6,
                                                "", "Не все предложения удастся опубликовать по модели CPA:" +
                                                        "Поправьте предложения которые нельзя опубликовать.",
                                                "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(7,
                                                "", "Есть одинаковые предложения:" +
                                                        "Дубликаты нужно удалить.",
                                                "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", ""
                                        ))
                                        .build())),
                Arguments.of("SUPPLIER",
                        MarketTemplate.SUPPLIER,
                        getPathConsumer(8, "Ассортимент",
                                ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                        .putAll(buildExpectedMap(4,
                                                "", "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param." +
                                                        System.lineSeparator() + System.lineSeparator() +
                                                        "Не указана характеристика товара price:" +
                                                        "Укажите эту характеристику с помощью элемента param.",
                                                "300", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                ""
                                        ))
                                        .putAll(buildExpectedMap(5,
                                                "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param.",
                                                "", "301", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(6,
                                                "", "Не все предложения удастся опубликовать по модели CPA:" +
                                                        "Поправьте предложения которые нельзя опубликовать.",
                                                "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(7,
                                                "", "Есть одинаковые предложения:" +
                                                        "Дубликаты нужно удалить.",
                                                "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", ""
                                        ))
                                        .build())),
                Arguments.of("PRICE",
                        MarketTemplate.PRICE,
                        getPathConsumer(7, "Цены",
                                ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                        .putAll(buildExpectedMap(3,
                                                "", "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param." +
                                                        System.lineSeparator() + System.lineSeparator() +
                                                        "Не указана характеристика товара price:" +
                                                        "Укажите эту характеристику с помощью элемента param.",
                                                "300", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(4,
                                                "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param.",
                                                "", "301", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(5,
                                                "", "Не все предложения удастся опубликовать по модели CPA:" +
                                                        "Поправьте предложения которые нельзя опубликовать.",
                                                "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(6,
                                                "", "Есть одинаковые предложения:" +
                                                        "Дубликаты нужно удалить.",
                                                "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", ""
                                        ))
                                        .build())),
                Arguments.of("STOCK",
                        MarketTemplate.STOCK,
                        getPathConsumer(6, "Остатки",
                                ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                        .putAll(buildExpectedMap(2,
                                                "", "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param." +
                                                        System.lineSeparator() + System.lineSeparator() +
                                                        "Не указана характеристика товара price:" +
                                                        "Укажите эту характеристику с помощью элемента param.",
                                                "300", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(3,
                                                "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param.",
                                                "", "301", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(4,
                                                "", "Не все предложения удастся опубликовать по модели CPA:" +
                                                        "Поправьте предложения которые нельзя опубликовать.",
                                                "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(5,
                                                "", "Есть одинаковые предложения:" +
                                                        "Дубликаты нужно удалить.",
                                                "", "", ""
                                        ))
                                        .build()))
        );
    }

    @Nonnull
    private static Stream<Arguments> getOnlyCorrectOfferArguments() {
        return Stream.of(
                Arguments.of("DBS",
                        MarketTemplate.DBS,
                        getPathConsumer(7, "Ассортимент",
                                ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                        .putAll(buildExpectedMap(4,
                                                "", "", "offer1", "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                                "https://image.com/ad,https://avatars.mds.yandex.net/get-marketpic/" +
                                                        "1662891/market_AVwbJCUZUxNIXcqb5luPyA_mbo/orig," +
                                                        "https://avatars.mds.yandex.net/get-marketpic/1041839/" +
                                                        "market_3tgf4RglGTwniQNavB4giA_upload/orig",
                                                "Offer description", "Батарейки и аккумуляторы", "PKCELL",
                                                "4985058793639", "5/1/1", "0.05", "Китай, Вьетнам", "", "Есть",
                                                "Диагональ|27|дюймов",
                                                "https://boomaa.nethouse.ru/products/pkcell-ag3-10b", "389", "506",
                                                "RUR", "NO_VAT", "Предоплата 42%", "Нельзя", "80", "да", "100", "Есть",
                                                "10", "3-5", "Есть", "0", "4"
                                        ))
                                        .putAll(buildExpectedMap(5,
                                                "", "", "offer4", "Ban", "https://2020.nu,https://2021.su," +
                                                        "https://avatars.mds.yandex.net/tu," +
                                                        "https://avatars.mds.yandex.net/ha", "", "Телефон", "", "", "",
                                                "", "", "", "", "", "https://bez-granic.jp/ban", "412", "", "USD"
                                        ))
                                        .putAll(buildExpectedMap(6,
                                                "", "", "offer5", "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                                "https://image.com/ad,https://avatars.mds.yandex.net/get-marketpic/" +
                                                        "1662891/market_AVwbJCUZUxNIXcqb5luPyA_mbo/orig," +
                                                        "https://avatars.mds.yandex.net/get-marketpic/1041839/" +
                                                        "market_3tgf4RglGTwniQNavB4giA_upload/orig",
                                                "Offer description", "Батарейки и аккумуляторы", "PKCELL",
                                                "4985058793639", "5/1/1", "0.05", "Китай, Вьетнам", "", "Есть",
                                                "Диагональ|27|дюймов",
                                                "https://boomaa.nethouse.ru/products/pkcell-ag3-10b", "389", "506",
                                                "RUR", "NO_VAT", "Предоплата 42%", "Нельзя", "80", "да", "100", "Есть",
                                                "10", "3-5", "Есть", "0", "4"
                                        ))
                                        .build())),
                Arguments.of("SUPPLIER",
                        MarketTemplate.SUPPLIER,
                        getPathConsumer(7, "Ассортимент",
                                ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                        .putAll(buildExpectedMap(4,
                                                "", "", "offer1",
                                                "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                                "https://image.com/ad," +
                                                        "https://avatars.mds.yandex.net/get-marketpic/1662891/" +
                                                        "market_AVwbJCUZUxNIXcqb5luPyA_mbo/orig," +
                                                        "https://avatars.mds.yandex.net/get-marketpic/1041839/" +
                                                        "market_3tgf4RglGTwniQNavB4giA_upload/orig", "Offer description",
                                                "Батарейки и аккумуляторы", "PKCELL",
                                                "4985058793639", "5/1/1", "0.05",
                                                "Китай, Вьетнам", "CODE 228",
                                                "6 дней, 10 часов", "Shelf life comment from partner spec",
                                                "6 месяцев", "Life time comment from partner spec",
                                                "2 месяца, 3 дня, 4 часа", "Guarantee period comment from partner spec",
                                                "584723957169", "8506101100,3216101100", "10", "100687839874",
                                                "https://boomaa.nethouse.ru/products/pkcell-ag3-10b",
                                                "389", "506", "NO_VAT", "Да", "100",
                                                "Поставки будут", "15", "5000", "1000", "пн,ср,пт,сб", "4"
                                        ))
                                        .putAll(buildExpectedMap(5,
                                                "", "", "offer4",
                                                "Ban",
                                                "https://2020.nu," +
                                                        "https://2021.su," +
                                                        "https://avatars.mds.yandex.net/tu," +
                                                        "https://avatars.mds.yandex.net/ha",
                                                "", "Телефон", "", "", "", "", "", "", "", "", "", "", "", "",
                                                "", "", "", "", "https://bez-granic.jp/ban", "412", "", "", "", "", "",
                                                "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(6,
                                                "", "", "offer5",
                                                "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                                "https://image.com/ad," +
                                                        "https://avatars.mds.yandex.net/get-marketpic/1662891/" +
                                                        "market_AVwbJCUZUxNIXcqb5luPyA_mbo/orig," +
                                                        "https://avatars.mds.yandex.net/get-marketpic/1041839/" +
                                                        "market_3tgf4RglGTwniQNavB4giA_upload/orig", "Offer description",
                                                "Батарейки и аккумуляторы", "PKCELL",
                                                "4985058793639", "5/1/1", "0.05",
                                                "Китай, Вьетнам", "CODE 228",
                                                "6 дней, 10 часов", "Shelf life comment from partner spec",
                                                "6 месяцев", "Life time comment from partner spec",
                                                "2 месяца, 3 дня, 4 часа", "Guarantee period comment from partner spec",
                                                "584723957169", "8506101100,3216101100", "10", "100687839874",
                                                "https://boomaa.nethouse.ru/products/pkcell-ag3-10b",
                                                "389", "506", "NO_VAT", "Да", "100",
                                                "Поставки будут", "15", "5000", "1000", "пн,ср,пт,сб", "4"
                                        ))
                                        .build())),
                Arguments.of("STOCK",
                        MarketTemplate.STOCK,
                        getPathConsumer(5, "Остатки",
                                ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                        .putAll(buildExpectedMap(2,
                                                "", "", "offer1", "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                                "100", "", ""
                                        ))
                                        .putAll(buildExpectedMap(3,
                                                "", "", "offer4", "Ban",
                                                "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(4,
                                                "", "", "offer5", "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                                "100", "", ""
                                        ))
                                        .build())),
                Arguments.of("PRICE",
                        MarketTemplate.PRICE,
                        getPathConsumer(6, "Цены",
                                ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                        .putAll(buildExpectedMap(3,
                                                "", "", "offer1", "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                                "389", "506", "NO_VAT", "Да",
                                                "Рекомендованные цены", "100", "руб.",
                                                "", "", "", "", "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(4,
                                                "", "", "offer4", "Ban",
                                                "412", "", "", "",
                                                "", "", "",
                                                "", "", "", "", "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(5,
                                                "", "", "offer5", "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                                "389", "506", "NO_VAT", "Да",
                                                "Рекомендованные цены", "100", "руб.",
                                                "", "", "", "", "", "", "", "", ""
                                        ))
                                        .build()))
        );
    }

    @Nonnull
    private static Stream<Arguments> getAllOffersArguments() {
        return Stream.of(
                Arguments.of("DBS",
                        MarketTemplate.DBS,
                        getPathConsumer(9, "Ассортимент",
                                ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                        .putAll(buildExpectedMap(4,
                                                "", "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param.\n\n" +
                                                        "Не указана характеристика товара price:" +
                                                        "Укажите эту характеристику с помощью элемента param.", "300",
                                                "Батарейка AG3 щелочная PKCELL AG3-10B 10шт", "https://image.com/ad," +
                                                        "https://avatars.mds.yandex.net/get-marketpic/1662891/" +
                                                        "market_AVwbJCUZUxNIXcqb5luPyA_mbo/orig," +
                                                        "https://avatars.mds.yandex.net/get-marketpic/1041839/" +
                                                        "market_3tgf4RglGTwniQNavB4giA_upload/orig",
                                                "Offer description", "Батарейки и аккумуляторы", "PKCELL",
                                                "4985058793639", "5/1/1", "0.05", "Китай, Вьетнам", "", "Есть",
                                                "Диагональ|27|дюймов",
                                                "https://boomaa.nethouse.ru/products/pkcell-ag3-10b", "389", "506",
                                                "RUR", "NO_VAT", "Предоплата 42%", "Нельзя", "80", "да", "100", "Есть",
                                                "10", "3-5", "Есть", "0", "4"
                                        ))
                                        .putAll(buildExpectedMap(5,
                                                "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param.", "",
                                                "301", "Ban", "https://2020.nu,https://2021.su," +
                                                        "https://avatars.mds.yandex.net/tu," +
                                                        "https://avatars.mds.yandex.net/ha", "", "Телефон", "", "", "",
                                                "", "", "", "", "", "https://bez-granic.jp/ban", "412", "", "USD"
                                        ))
                                        .putAll(buildExpectedMap(6,
                                                "", "", "303", "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                                "https://image.com/ad,https://avatars.mds.yandex.net/get-marketpic/" +
                                                        "1662891/market_AVwbJCUZUxNIXcqb5luPyA_mbo/orig," +
                                                        "https://avatars.mds.yandex.net/get-marketpic/1041839/" +
                                                        "market_3tgf4RglGTwniQNavB4giA_upload/orig",
                                                "Offer description", "Батарейки и аккумуляторы", "PKCELL",
                                                "4985058793639", "5/1/1", "0.05", "Китай, Вьетнам", "", "Есть",
                                                "Диагональ|27|дюймов",
                                                "https://boomaa.nethouse.ru/products/pkcell-ag3-10b", "389", "506",
                                                "RUR", "NO_VAT", "Предоплата 42%", "Нельзя", "80", "да", "100", "Есть",
                                                "10", "3-5", "Есть", "0", "4"
                                        ))
                                        .putAll(buildExpectedMap(7,
                                                "", "Не все предложения удастся опубликовать по модели CPA:" +
                                                        "Поправьте предложения которые нельзя опубликовать.",
                                                "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                ""
                                        ))
                                        .putAll(buildExpectedMap(8,
                                                "", "Есть одинаковые предложения:" +
                                                        "Дубликаты нужно удалить.",
                                                "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                "", "", "", "", "",
                                                ""
                                        ))
                                        .build())),
                Arguments.of("SUPPLIER",
                        MarketTemplate.SUPPLIER,
                        getPathConsumer(9, "Ассортимент",
                                ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                        .putAll(buildExpectedMap(4,
                                                "", "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param." +
                                                        System.lineSeparator() + System.lineSeparator() +
                                                        "Не указана характеристика товара price:" +
                                                        "Укажите эту характеристику с помощью элемента param.",
                                                "300", "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                                "https://image.com/ad," +
                                                        "https://avatars.mds.yandex.net/get-marketpic/1662891/" +
                                                        "market_AVwbJCUZUxNIXcqb5luPyA_mbo/orig," +
                                                        "https://avatars.mds.yandex.net/get-marketpic/1041839/" +
                                                        "market_3tgf4RglGTwniQNavB4giA_upload/orig", "Offer description",
                                                "Батарейки и аккумуляторы", "PKCELL",
                                                "4985058793639", "5/1/1", "0.05",
                                                "Китай, Вьетнам", "CODE 228",
                                                "6 дней, 10 часов", "Shelf life comment from partner spec",
                                                "6 месяцев", "Life time comment from partner spec",
                                                "2 месяца, 3 дня, 4 часа", "Guarantee period comment from partner spec",
                                                "584723957169", "8506101100,3216101100", "10", "100687839874",
                                                "https://boomaa.nethouse.ru/products/pkcell-ag3-10b",
                                                "389", "506", "NO_VAT", "Да", "100",
                                                "Поставки будут", "15", "5000", "1000", "пн,ср,пт,сб", "4"
                                        ))
                                        .putAll(buildExpectedMap(5,
                                                "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param.",
                                                "", "301", "Ban",
                                                "https://2020.nu," +
                                                        "https://2021.su," +
                                                        "https://avatars.mds.yandex.net/tu," +
                                                        "https://avatars.mds.yandex.net/ha",
                                                "", "Телефон", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                                                "", "", "https://bez-granic.jp/ban", "412", "", "", "", "", "", "", "",
                                                "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(6,
                                                "", "", "303", "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                                "https://image.com/ad," +
                                                        "https://avatars.mds.yandex.net/get-marketpic/1662891/" +
                                                        "market_AVwbJCUZUxNIXcqb5luPyA_mbo/orig," +
                                                        "https://avatars.mds.yandex.net/get-marketpic/1041839/" +
                                                        "market_3tgf4RglGTwniQNavB4giA_upload/orig", "Offer description",
                                                "Батарейки и аккумуляторы", "PKCELL",
                                                "4985058793639", "5/1/1", "0.05",
                                                "Китай, Вьетнам", "CODE 228",
                                                "6 дней, 10 часов", "Shelf life comment from partner spec",
                                                "6 месяцев", "Life time comment from partner spec",
                                                "2 месяца, 3 дня, 4 часа", "Guarantee period comment from partner spec",
                                                "584723957169", "8506101100,3216101100", "10", "100687839874",
                                                "https://boomaa.nethouse.ru/products/pkcell-ag3-10b",
                                                "389", "506", "NO_VAT", "Да", "100",
                                                "Поставки будут", "15", "5000", "1000", "пн,ср,пт,сб", "4"
                                        ))
                                        .putAll(buildExpectedMap(7,
                                                "","Не все предложения удастся опубликовать по модели CPA:" +
                                                        "Поправьте предложения которые нельзя опубликовать.", "",
                                                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                                                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                                                "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(8,
                                                "","Есть одинаковые предложения:" +
                                                        "Дубликаты нужно удалить.", "",
                                                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                                                "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                                                "", "", "", "", ""
                                        ))
                                        .build())),
                Arguments.of("STOCK",
                        MarketTemplate.STOCK,
                        getPathConsumer(7, "Остатки",
                                ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                        .putAll(buildExpectedMap(2,
                                                "", "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param." +
                                                        System.lineSeparator() + System.lineSeparator() +
                                                        "Не указана характеристика товара price:" +
                                                        "Укажите эту характеристику с помощью элемента param.",
                                                "300", "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                                "100", "", ""
                                        ))
                                        .putAll(buildExpectedMap(3,
                                                "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param.",
                                                "", "301", "Ban",
                                                "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(4,
                                                "", "", "303", "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                                "100", "", ""
                                        ))
                                        .putAll(buildExpectedMap(5,
                                                "","Не все предложения удастся опубликовать по модели CPA:" +
                                                        "Поправьте предложения которые нельзя опубликовать.",
                                                "", "", "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(6,
                                                "", "Есть одинаковые предложения:" +
                                                        "Дубликаты нужно удалить.",
                                                "", "", "", "", "", "", ""
                                        ))
                                        .build())),
                Arguments.of("PRICE",
                        MarketTemplate.PRICE,
                        getPathConsumer(8, "Цены",
                                ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                        .putAll(buildExpectedMap(3,
                                                "", "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param." +
                                                        System.lineSeparator() + System.lineSeparator() +
                                                        "Не указана характеристика товара price:" +
                                                        "Укажите эту характеристику с помощью элемента param.",
                                                "300", "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                                "389", "506", "NO_VAT", "Да",
                                                "Рекомендованные цены", "100", "руб.",
                                                "", "", "",
                                                "", "", "", "", "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(4,
                                                "Не указана характеристика товара age:" +
                                                        "Укажите эту характеристику с помощью элемента param.",
                                                "", "301", "Ban",
                                                "412", "", "", "",
                                                "", "", "",
                                                "", "", "", "", "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(5,
                                                "", "", "303", "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                                "389", "506", "NO_VAT", "Да",
                                                "Рекомендованные цены", "100", "руб.",
                                                "", "", "", "", "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(6,
                                                "", "Не все предложения удастся опубликовать по модели CPA:" +
                                                        "Поправьте предложения которые нельзя опубликовать.",
                                                "", "", "", "", "", "", "", "",
                                                "", "", "",
                                                "", "", "", "", "", "", ""
                                        ))
                                        .putAll(buildExpectedMap(7,
                                                "", "Есть одинаковые предложения:" +
                                                        "Дубликаты нужно удалить.",
                                                "", "", "", "", "", "", "", "",
                                                "", "", "", "", "", "", ""
                                        ))
                                        .build()))
        );
    }

    @Nonnull
    private static Consumer<Path> getPathConsumer(int rowCount,
                                                  String sheetName,
                                                  Map<XlsTestUtils.CellInfo, String> expected) {
        return XlsTestUtils.getPathConsumer(expected, new XlsTestUtils.SheetInfo(rowCount, 2, sheetName));
    }
}
