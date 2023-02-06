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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.excel.MarketTemplate;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.supplier.model.IndexerErrorInfo;

import static ru.yandex.market.core.feed.validation.result.ValidationResultTestUtil.getValidationResultOnlyErrorTestCollection;
import static ru.yandex.market.core.feed.validation.result.ValidationResultTestUtil.getValidationResultTestCollection;
import static ru.yandex.market.core.feed.validation.result.ValidationResultTestUtil.getValidationResultWithoutErrorTestCollection;
import static ru.yandex.market.core.feed.validation.result.XlsTestUtils.buildExpectedMap;

/**
 * Date: 19.11.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class FeedErrorInfoXlsServiceTest extends FunctionalTest {

    private static final Map<XlsTestUtils.CellInfo, String> EXPECTED_RESULT =
            ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                    .putAll(buildExpectedMap(1,
                            "301",
                            "66",
                            "Ошибка",
                            "Не указана характеристика товара age",
                            "Укажите эту характеристику с помощью элемента param.",
                            ""
                    ))
                    .putAll(buildExpectedMap(2,
                            "300",
                            "55",
                            "Предупреждение",
                            "Не указана характеристика товара age",
                            "Укажите эту характеристику с помощью элемента param.",
                            ""
                    ))
                    .putAll(buildExpectedMap(3,
                            "300",
                            "66",
                            "Предупреждение",
                            "Не указана характеристика товара price",
                            "Укажите эту характеристику с помощью элемента param.",
                            ""
                    ))
                    .putAll(buildExpectedMap(4,
                            "",
                            "0",
                            "Ошибка",
                            "Не все предложения удастся опубликовать по модели CPA",
                            "Поправьте предложения которые нельзя опубликовать.",
                            ""
                    ))
                    .putAll(buildExpectedMap(5,
                            "",
                            "77",
                            "Критическая ошибка",
                            "Есть одинаковые предложения",
                            "Дубликаты нужно удалить.",
                            "age"
                    ))
                    .build();

    @Qualifier("feedErrorInfoXlsService")
    @Autowired
    private FeedXlsService<IndexerErrorInfo> feedXlsService;

    @DisplayName("Проверка расширения файла для MarketTemplate.COMMON")
    @Test
    void getXlsFileExtension_commonMarketTemplate_xls() {
        Assertions.assertThat(feedXlsService.getXlsFileExtension(MarketTemplate.COMMON))
                .isEqualTo(".xlsx");
    }

    @DisplayName("Проверка базового наименования файла")
    @ParameterizedTest(name = "MarketTemplate.{0} - {1}")
    @CsvSource({
            "COMMON,shop_feed",
            "SUPPLIER,supplier_feed",
            "PRICE,price_feed",
            "STOCK,stock_feed"
    })
    void getDefaultFileName_marketTemplate_defaultName(MarketTemplate template, String extension) {
        Assertions.assertThat(feedXlsService.getDefaultFileName(template))
                .isEqualTo(extension);
    }

    @DisplayName("Проверка корректности заполнения информации об ошибках по фиду. Только ошибки.")
    @Test
    void fillTemplate_errorOnly_successful() {
        var offers = getValidationResultOnlyErrorTestCollection();
        feedXlsService.fillTemplate(
                MarketTemplate.COMMON,
                Stream.concat(offers.stream().flatMap(o -> o.getIndexerErrorInfos().stream()),
                        offers.stream().flatMap(o -> o.getIndexerWarningInfos().stream())),
                getPathConsumer(EXPECTED_RESULT, 6)
        );
    }

    @DisplayName("Проверка корректности заполнения информации об ошибках по фиду. Только корректные фиды.")
    @Test
    void fillTemplate_onlyCorrectOffer_successful() {
        var offers = getValidationResultWithoutErrorTestCollection(false);
        feedXlsService.fillTemplate(
                MarketTemplate.COMMON,
                Stream.concat(offers.stream().flatMap(o -> o.getIndexerErrorInfos().stream()),
                        offers.stream().flatMap(o -> o.getIndexerWarningInfos().stream())),
                getPathConsumer(Collections.emptyMap(), 1)
        );
    }

    @DisplayName("Проверка корректности заполнения информации об ошибках по фиду.")
    @Test
    void fillTemplate_allOffers_successful() {
        var offers = getValidationResultTestCollection(true, true);
        feedXlsService.fillTemplate(
                MarketTemplate.COMMON,
                Stream.concat(offers.stream().flatMap(o -> o.getIndexerErrorInfos().stream()),
                        offers.stream().flatMap(o -> o.getIndexerWarningInfos().stream())),
                getPathConsumer(EXPECTED_RESULT, 6)
        );
    }

    @Nonnull
    private Consumer<Path> getPathConsumer(Map<XlsTestUtils.CellInfo, String> expected, int rowCount) {
        return XlsTestUtils.getPathConsumer(expected, new XlsTestUtils.SheetInfo(rowCount, 0, null));
    }
}
