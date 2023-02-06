package ru.yandex.market.logistics.tarifficator.service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.validation.Validation;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.tarifficator.AbstractUnitTest;
import ru.yandex.market.logistics.tarifficator.converter.ErrorConverter;
import ru.yandex.market.logistics.tarifficator.converter.SpreadsheetConverter;
import ru.yandex.market.logistics.tarifficator.exception.FileProcessingException;
import ru.yandex.market.logistics.tarifficator.model.entity.Tariff;
import ru.yandex.market.logistics.tarifficator.model.entity.embedded.PriceListRestrictions;
import ru.yandex.market.logistics.tarifficator.model.enums.ServiceType;
import ru.yandex.market.logistics.tarifficator.model.enums.TariffType;
import ru.yandex.market.logistics.tarifficator.model.pricelist.CellPosition;
import ru.yandex.market.logistics.tarifficator.model.pricelist.ReaderContext;
import ru.yandex.market.logistics.tarifficator.model.pricelist.raw.DeliveryPriceRaw;
import ru.yandex.market.logistics.tarifficator.model.pricelist.raw.DimensionsLimitRaw;
import ru.yandex.market.logistics.tarifficator.model.pricelist.raw.DirectionRaw;
import ru.yandex.market.logistics.tarifficator.model.pricelist.raw.LocationRaw;
import ru.yandex.market.logistics.tarifficator.model.pricelist.raw.PriceListRaw;
import ru.yandex.market.logistics.tarifficator.model.pricelist.raw.ServicePriceRaw;
import ru.yandex.market.logistics.tarifficator.model.pricelist.scheme.SheetType;
import ru.yandex.market.logistics.tarifficator.model.source.PriceListSource;
import ru.yandex.market.logistics.tarifficator.model.source.SpreadsheetPriceListSource;
import ru.yandex.market.logistics.tarifficator.service.pricelist.spreadsheet.PriceListParserFactory;
import ru.yandex.market.logistics.tarifficator.service.pricelist.spreadsheet.ServiceColumnPrefixProvider;
import ru.yandex.market.logistics.tarifficator.service.pricelist.spreadsheet.SpreadsheetMapper;
import ru.yandex.market.logistics.tarifficator.service.pricelist.spreadsheet.SpreadsheetPriceListReaderService;

import static java.lang.ClassLoader.getSystemResource;

@DisplayName("Unit-тест сервиса SpreadsheetPriceListReaderService")
class SpreadsheetPriceListReaderServiceTest extends AbstractUnitTest {
    private final ServiceColumnPrefixProvider serviceColumnPrefixProvider = new ServiceColumnPrefixProvider();
    private final SpreadsheetPriceListReaderService readerService = new SpreadsheetPriceListReaderService(
        new PriceListParserFactory(
            Clock.systemDefaultZone(),
            Validation.buildDefaultValidatorFactory().usingContext().getValidator(),
            new SpreadsheetMapper(serviceColumnPrefixProvider),
            new ErrorConverter(),
            new SpreadsheetConverter(),
            serviceColumnPrefixProvider
        )
    );

    private ReaderContext context;

    @BeforeEach
    void setUp() {
        context = new ReaderContext();
    }

    @Test
    @DisplayName("Проверка поддержки реализацией источника данных")
    void isSupported() throws IOException {
        PriceListSource goodSource = createPriceListSource("service/pricelist/xlsx/minimal-price-list.xlsx");
        PriceListSource wrongSource = new PriceListSource() {
            @Nonnull
            @Override
            public byte[] getContent() {
                return new byte[0];
            }
        };

        softly.assertThat(readerService.isSupported(goodSource)).isTrue();
        softly.assertThat(readerService.isSupported(wrongSource)).isFalse();
    }

    @Test
    @DisplayName("Успешное чтение минимального прайс-листа тарифа")
    void readMinimal() throws IOException {
        PriceListSource source = createPriceListSource("service/pricelist/xlsx/minimal-price-list.xlsx");
        Tariff tariff = new Tariff().setType(TariffType.GENERAL).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(source, context, tariff, null);
        PriceListRaw expectedPriceList = expectedMinimalGeneralPriceList();

        softly.assertThat(context.getCellToError()).hasSize(0);
        softly.assertThatObject(priceList).usingRecursiveComparison().isEqualTo(expectedPriceList);
    }

    @Test
    @DisplayName("Успешное чтение минимального прайс-листа тарифа с ограничением количества направлений")
    void readMinimalWithDirectionRestriction() throws IOException {
        PriceListSource source = createPriceListSource("service/pricelist/xlsx/minimal-price-list.xlsx");
        Tariff tariff = new Tariff().setType(TariffType.GENERAL).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(
            source,
            context,
            tariff,
            new PriceListRestrictions().setDirectionCount(1)
        );
        PriceListRaw expectedPriceList = expectedMinimalGeneralPriceList();

        softly.assertThat(context.getCellToError()).hasSize(0);
        softly.assertThatObject(priceList).usingRecursiveComparison().isEqualTo(expectedPriceList);
    }

    @Test
    @DisplayName("Успешное чтение минимального прайс-листа тарифа Курьерской платформы")
    void readMinimalMK() throws IOException {
        PriceListSource source = createPriceListSource("service/pricelist/xlsx/minimal-price-list-market-courier.xlsx");
        Tariff tariff = new Tariff().setType(TariffType.MARKET_COURIER).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(source, context, tariff, null);
        PriceListRaw expectedPriceList = expectedMinimalMarketCourierPriceList();

        softly.assertThat(context.getCellToError()).hasSize(0);
        softly.assertThatObject(priceList).usingRecursiveComparison().isEqualTo(expectedPriceList);
    }

    @Test
    @DisplayName(
        "Успешное чтение минимального прайс-листа тарифа с ограничением количества направлений " +
            "тарифа Курьерской платформы"
    )
    void readMinimalWithDirectionRestrictionMK() throws IOException {
        PriceListSource source = createPriceListSource("service/pricelist/xlsx/minimal-price-list-market-courier.xlsx");
        Tariff tariff = new Tariff().setType(TariffType.MARKET_COURIER).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(
            source,
            context,
            tariff,
            new PriceListRestrictions().setDirectionCount(1)
        );
        PriceListRaw expectedPriceList = expectedMinimalMarketCourierPriceList();

        softly.assertThat(context.getCellToError()).hasSize(0);
        softly.assertThatObject(priceList).usingRecursiveComparison().isEqualTo(expectedPriceList);
    }

    @Test
    @DisplayName(
        "Успешное автоматическое проставление значения НЕТ для поля \"Только для населённых пунктов\" " +
            "у тарифа Курьерской платформы"
    )
    void autoSetLocalityOnlyFalseForMK() throws IOException {
        // given:
        PriceListSource source = createPriceListSource("service/pricelist/xlsx/minimal-price-list-market-courier.xlsx");

        // when:
        Tariff tariff = new Tariff().setType(TariffType.MARKET_COURIER).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(source, context, tariff, null);

        // then:
        softly.assertThat(context.getCellToError()).hasSize(0);
        softly.assertThat(priceList.isLocalityOnly()).isFalse();
    }

    @Test
    @DisplayName(
        "Успешное явное проставление значения НЕТ для поля \"Только для населённых пунктов\" " +
            "у тарифа Курьерской платформы"
    )
    void setLocalityOnlyFalseForMK() throws IOException {
        // given:
        PriceListSource source =
            createPriceListSource("service/pricelist/xlsx/market-courier_with-explicit-locality-only-false.xlsx");

        // when:
        Tariff tariff = new Tariff().setType(TariffType.MARKET_COURIER).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(source, context, tariff, null);

        // then:
        softly.assertThat(context.getCellToError()).hasSize(0);
        softly.assertThat(priceList.isLocalityOnly()).isFalse();
    }

    @Test
    @DisplayName(
        "Успешное явное проставление значения ДА для поля \"Только для населённых пунктов\" " +
            "у тарифа Курьерской платформы"
    )
    void setLocalityOnlyTrueForMK() throws IOException {
        // given:
        PriceListSource source =
            createPriceListSource("service/pricelist/xlsx/market-courier_with-explicit-locality-only-true.xlsx");

        // when:
        Tariff tariff = new Tariff().setType(TariffType.MARKET_COURIER).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(source, context, tariff, null);

        // then:
        softly.assertThat(context.getCellToError()).hasSize(0);
        softly.assertThat(priceList.isLocalityOnly()).isTrue();
    }

    @Test
    @DisplayName(
        "Успешное автоматическое проставление значения Да для поля \"Только для населённых пунктов\" " +
            "у тарифов, отличных от Курьерской платформы"
    )
    void autoSetLocalityOnlyTrueForNonMK() throws IOException {
        // given:
        PriceListSource source = createPriceListSource("service/pricelist/xlsx/minimal-price-list-own-delivery.xlsx");

        // when:
        Tariff tariff = new Tariff().setType(TariffType.OWN_DELIVERY).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(source, context, tariff, null);

        // then:
        softly.assertThat(context.getCellToError()).hasSize(0);
        softly.assertThat(priceList.isLocalityOnly()).isTrue();
    }

    @Test
    @DisplayName("Успешное чтение прайс-листа с указанным scale")
    void readWithScale() throws IOException {
        PriceListSource source = createPriceListSource("service/pricelist/xlsx/price-list-with-custom-scale.xlsx");
        Tariff tariff = new Tariff().setType(TariffType.GENERAL).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(source, context, tariff, null);
        PriceListRaw expectedPriceList = expectedMinimalGeneralPriceList();
        expectedPriceList.setScale(BigDecimal.valueOf(0.5));
        DeliveryPriceRaw deliveryPrice = expectedPriceList.getDeliveryPrices().values().iterator().next().get(0);
        deliveryPrice.setMinWeight(0.33);
        deliveryPrice.setMaxWeight(1.79);

        softly.assertThat(context.getCellToError()).hasSize(0);
        softly.assertThatObject(priceList).usingRecursiveComparison().isEqualTo(expectedPriceList);
    }

    @Test
    @DisplayName("Успешное чтение прайс-листа с указанным scale курьерки")
    void readWithScaleMK() throws IOException {
        PriceListSource source = createPriceListSource("service/pricelist/xlsx/price-list-with-custom-scale-mk.xlsx");
        Tariff tariff = new Tariff().setType(TariffType.MARKET_COURIER).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(source, context, tariff, null);
        PriceListRaw expectedPriceList = expectedMinimalMarketCourierPriceList();
        expectedPriceList.setScale(BigDecimal.valueOf(0.5));
        DeliveryPriceRaw deliveryPrice = expectedPriceList.getDeliveryPrices().values().iterator().next().get(0);
        deliveryPrice.setMinWeight(0.33);
        deliveryPrice.setMaxWeight(1.79);

        softly.assertThat(context.getCellToError()).hasSize(0);
        softly.assertThatObject(priceList).usingRecursiveComparison().isEqualTo(expectedPriceList);
    }

    @Test
    @DisplayName("Чтение минимального прайс-листа тарифа с ограничением направлений")
    void readMinimalWithExceededDirectionRestriction() throws IOException {
        PriceListSource source =
            createPriceListSource("service/pricelist/xlsx/minimal-price-list.xlsx");
        Tariff tariff = new Tariff().setType(TariffType.GENERAL).setEqualPublicAndNonpublicPrices(false);
        softly.assertThatThrownBy(() -> readerService.read(
            source,
            context,
            tariff,
            new PriceListRestrictions().setDirectionCount(0)
        ))
            .isInstanceOf(FileProcessingException.class)
            .hasMessage("Exceeded number of directions: 0");
    }

    @Test
    @DisplayName("Успешное чтение минимального прайс-листа тарифа с одной указанной стоимостью (OWN_DELIVERY)")
    void readOnePriceDeliveryMinimalOwnDelivery() throws IOException {
        PriceListSource source =
            createPriceListSource("service/pricelist/xlsx/minimal-price-list-own-delivery.xlsx");

        Tariff tariff = new Tariff().setType(TariffType.OWN_DELIVERY).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(source, context, tariff, null);
        PriceListRaw expectedPriceList = expectedMinimalPriceList();

        softly.assertThat(context.getCellToError()).hasSize(0);
        softly.assertThatObject(priceList).usingRecursiveComparison().isEqualTo(expectedPriceList);
    }

    @Test
    @DisplayName("Успешное чтение минимального прайс-листа тарифа с одной указанной стоимостью (не OWN_DELIVERY)")
    void readOnePriceDeliveryMinimalNonOwnDelivery() throws IOException {
        PriceListSource source =
            createPriceListSource("service/pricelist/xlsx/minimal-price-list-own-delivery.xlsx");

        Tariff tariff = new Tariff().setType(TariffType.GENERAL).setEqualPublicAndNonpublicPrices(true);
        PriceListRaw priceList = readerService.read(source, context, tariff, null);
        PriceListRaw expectedPriceList = expectedMinimalPriceList();

        softly.assertThat(context.getCellToError()).hasSize(0);
        softly.assertThatObject(priceList).usingRecursiveComparison().isEqualTo(expectedPriceList);
    }

    @Test
    @DisplayName(
        "Успешное чтение минимального прайс-листа тарифа с одной указанной стоимостью с ограничением направлений"
    )
    void readOnePriceDeliveryMinimalWithDirectionRestriction() throws IOException {
        PriceListSource source =
            createPriceListSource("service/pricelist/xlsx/minimal-price-list-own-delivery.xlsx");

        Tariff tariff = new Tariff().setType(TariffType.OWN_DELIVERY).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(
            source,
            context,
            tariff,
            new PriceListRestrictions().setDirectionCount(1)
        );
        PriceListRaw expectedPriceList = expectedMinimalPriceList();

        softly.assertThat(context.getCellToError()).hasSize(0);
        softly.assertThatObject(priceList).usingRecursiveComparison().isEqualTo(expectedPriceList);
    }

    @Test
    @DisplayName("Чтение минимального прайс-листа тарифа с одной указанной стоимостью с ограничением направлений")
    void readOnePriceDeliveryMinimalWithExceededDirectionRestriction() throws IOException {
        PriceListSource source =
            createPriceListSource("service/pricelist/xlsx/minimal-price-list-own-delivery.xlsx");

        Tariff tariff = new Tariff().setType(TariffType.OWN_DELIVERY).setEqualPublicAndNonpublicPrices(false);
        softly.assertThatThrownBy(() -> readerService.read(
            source,
            context,
            tariff,
            new PriceListRestrictions().setDirectionCount(0)
        ))
            .isInstanceOf(FileProcessingException.class)
            .hasMessage("Exceeded number of directions: 0");
    }

    @Test
    @DisplayName("Успешное чтение прайс-листа тарифа с одной указанной стоимостью с ограничением весовых брейков")
    void readOnePriceDeliveryMinimalWithWeightBreaksRestriction() throws IOException {
        PriceListSource source =
            createPriceListSource("service/pricelist/xlsx/price-list-own-delivery-weight-breaks.xlsx");

        Tariff tariff = new Tariff().setType(TariffType.OWN_DELIVERY).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(
            source,
            context,
            tariff,
            new PriceListRestrictions().setWeightBreaksCount(19)
        );

        softly.assertThat(context.getCellToError()).hasSize(0);
        softly.assertThat(priceList).usingRecursiveComparison().isEqualTo(expectedPriceListWithWeightBreaks());
    }

    @Test
    @DisplayName("Чтение прайс-листа тарифа с одной указанной стоимостью с ограничением весовых брейков")
    void readOnePriceDeliveryWithWeightBreaksRestriction() throws IOException {
        PriceListSource source =
            createPriceListSource("service/pricelist/xlsx/price-list-own-delivery-weight-breaks.xlsx");
        Tariff tariff = new Tariff().setType(TariffType.OWN_DELIVERY).setEqualPublicAndNonpublicPrices(false);
        softly.assertThatThrownBy(() -> readerService.read(
            source,
            context,
            tariff,
            new PriceListRestrictions().setWeightBreaksCount(18)
        ))
            .isInstanceOf(FileProcessingException.class)
            .hasMessage("Exceeded number of weight breaks: 18");
    }

    @Test
    @DisplayName("Успешное чтение прайс-листа тарифа")
    void read() throws IOException {
        PriceListSource source = createPriceListSource("service/pricelist/xlsx/price-list.xlsx");
        Tariff tariff = new Tariff().setType(TariffType.OWN_DELIVERY).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(source, context, tariff, null);
        PriceListRaw expectedPriceList = expectedPriceList();

        softly.assertThatObject(priceList).usingRecursiveComparison().isEqualTo(expectedPriceList);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("getPriceListWeightsValidationArguments")
    @DisplayName("Проверка валидации набора весов в прайс-листе")
    void priceListWeightsValidationTests(String filename, String errorMessage, String displayName) throws IOException {
        PriceListRaw expectedPriceList = expectedPriceListWithValidationErrors();
        PriceListSource source = createPriceListSource(filename);
        Tariff tariff = new Tariff().setType(TariffType.OWN_DELIVERY).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(source, context, tariff, null);

        softly.assertThatObject(context.getCellToError())
            .usingRecursiveComparison()
            .isEqualTo(
                Map.of(
                    new CellPosition(SheetType.DELIVERY, "A", 14),
                    errorMessage
                )
            );

        softly.assertThatObject(priceList).usingRecursiveComparison().isEqualTo(expectedPriceList);

    }

    private static Stream<Arguments> getPriceListWeightsValidationArguments() {
        return Stream.of(
            Arguments.of(
                "service/pricelist/xlsx/price-list-with-non-covered-weight-range.xlsx",
                "[Цена для веса в диапазоне между 15.0 кг и 20.0 кг не указана]",
                "Непокрытые диапазоны"
            ),
            Arguments.of(
                "service/pricelist/xlsx/price-list-with-intercepting-weight-ranges.xlsx",
                "[Цена для веса в диапазоне между 15.0 кг и 20.0 кг указана более одного раза]",
                "Пересечение весовых диапазонов"
            ),
            Arguments.of(
                "service/pricelist/xlsx/price-list-with-multiple-weight-set-validation-errors.xlsx",
                "[Цена для веса в диапазоне между 3.0 кг и 4.0 кг указана более одного раза," +
                    " Цена для веса в диапазоне между 12.0 кг и 15.0 кг не указана]",
                "Множественные ошибки валидации"
            )
        );
    }

    @Test
    @DisplayName("Чтение прайс-листа с дубликатами")
    void readWithDuplicates() throws IOException {
        PriceListSource source = createPriceListSource("service/pricelist/xlsx/price-list-with-duplicates.xlsx");
        Tariff tariff = new Tariff().setType(TariffType.OWN_DELIVERY).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(source, context, tariff, null);
        PriceListRaw expectedPriceList = expectedMinimalPriceList();

        softly.assertThatObject(context.getCellToError())
            .usingRecursiveComparison()
            .isEqualTo(
                Map.of(
                    new CellPosition(SheetType.SERVICES, "A", 3),
                    "Услуги по данному направлению уже определены выше"
                )
            );

        softly.assertThatObject(priceList)
            .usingRecursiveComparison().isEqualTo(expectedPriceList);
    }

    @Test
    @DisplayName("Чтение прайс-листа содержащего ошибки в строках")
    void readWithInvalidLocations() throws IOException {
        PriceListSource source = createPriceListSource(
            "service/pricelist/xlsx/price-list-with-errors-in-rows.xlsx");
        Tariff tariff = new Tariff().setType(TariffType.OWN_DELIVERY).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(source, context, tariff, null);
        ReaderContext expectedContext = new ReaderContext();
        PriceListRaw expectedPrice = expectedPriceListWithErrors(expectedContext);

        softly.assertThatObject(context.getCellToError())
            .usingRecursiveComparison().isEqualTo(expectedContext.getCellToError());

        softly.assertThatObject(priceList).usingRecursiveComparison().isEqualTo(expectedPrice);
    }

    @Test
    @DisplayName("Чтение прайс-листа общего тарифа, содержащего ошибки в строках")
    void readGeneralWithInvalidLocations() throws IOException {
        PriceListSource source = createPriceListSource(
            "service/pricelist/xlsx/price-list-with-errors-in-rows-general.xlsx");
        Tariff tariff = new Tariff().setType(TariffType.GENERAL).setEqualPublicAndNonpublicPrices(false);
        PriceListRaw priceList = readerService.read(source, context, tariff, null);
        ReaderContext expectedContext = new ReaderContext();
        PriceListRaw expectedPrice = expectedGeneralPriceListWithErrors(expectedContext);

        softly.assertThatObject(context.getCellToError())
            .usingRecursiveComparison().isEqualTo(expectedContext.getCellToError());

        softly.assertThatObject(priceList).usingRecursiveComparison().isEqualTo(expectedPrice);
    }

    @Test
    @DisplayName("Ошибка чтения прайс-листа тарифа без обязательных колонок")
    void readError() {
        FileProcessingException e = Assertions.assertThrows(FileProcessingException.class, () -> {
            PriceListSource source = createPriceListSource("service/pricelist/xlsx/error-price-list.xlsx");
            Tariff tariff = new Tariff().setType(TariffType.GENERAL).setEqualPublicAndNonpublicPrices(false);
            readerService.read(source, context, tariff, null);
        });

        softly.assertThat(e.getMessage())
            .isEqualTo(
                "Cannot find columns: " +
                    "'Максимальный срок'," +
                    "'ИМ/Фиксированная стоимость'," +
                    "'ИМ/Дельта стоимость'," +
                    "'ЯД/Фиксированная стоимость'," +
                    "'ЯД/Дельта стоимость'"
            );
    }

    @Test
    @DisplayName("Ошибка чтения прайс-листа тарифа с одной указанной стоимостью без обязательных колонок")
    void readOnePriceDeliveryError() {
        FileProcessingException e = Assertions.assertThrows(FileProcessingException.class, () -> {
            PriceListSource source = createPriceListSource("service/pricelist/xlsx/error-price-list.xlsx");
            Tariff tariff = new Tariff().setType(TariffType.OWN_DELIVERY).setEqualPublicAndNonpublicPrices(false);
            readerService.read(source, context, tariff, null);
        });

        softly.assertThat(e.getMessage())
            .isEqualTo("Cannot find columns: 'Фиксированная стоимость','Максимальный срок'");
    }

    @Test
    @DisplayName("Ошибка чтения прайс-листа: указан нулевой scale")
    void zeroScaleError() throws IOException {
        PriceListSource source = createPriceListSource("service/pricelist/xlsx/price-list-with-zero-scale.xlsx");
        Tariff tariff = new Tariff().setType(TariffType.GENERAL).setEqualPublicAndNonpublicPrices(false);
        FileProcessingException e = Assertions.assertThrows(
            FileProcessingException.class,
            () -> readerService.read(source, context, tariff, null)
        );

        softly.assertThat(e.getMessage())
            .isEqualTo("[Лист: 'Тариф', Ячейка: J2] Значение ячейки не должно принимать нулевое значение");
    }

    @Test
    @DisplayName("Ошибка чтения прайс-листа: указан отрицательный scale")
    void invalidScaleError() throws IOException {
        PriceListSource source = createPriceListSource("service/pricelist/xlsx/price-list-with-negative-scale.xlsx");
        Tariff tariff = new Tariff().setType(TariffType.GENERAL).setEqualPublicAndNonpublicPrices(false);
        FileProcessingException e = Assertions.assertThrows(
            FileProcessingException.class,
            () -> readerService.read(source, context, tariff, null)
        );

        softly.assertThat(e.getMessage())
            .isEqualTo("[Лист: 'Тариф', Ячейка: J2] Значение ячейки не должно быть отрицательным");
    }

    private PriceListRaw.Builder defaultPriceList() {
        return PriceListRaw.builder()
            .scale(BigDecimal.ONE);
    }

    private PriceListRaw expectedPriceList() {
        return defaultPriceList()
            .dimensionsLimit(DimensionsLimitRaw.builder()
                .length(250)
                .width(200)
                .height(200)
                .weight(500)
                .dimensionalWeight(250)
                .dimensionsSum(400)
                .build())
            .servicePrices(createServicePrices())
            .deliveryPrices(createDeliveryPrices())
            .build();
    }

    @Nonnull
    private Map<DirectionRaw, List<ServicePriceRaw>> createServicePrices() {
        return Map.of(
            createDirection("Москва", "Россия, Алтайский край, Барнаул"),
            List.of(
                createService(ServiceType.CASH_SERVICE, true, 30d, 0.022d, 6600d),
                createService(ServiceType.WAIT_20, false, null, null, null),
                createService(ServiceType.INSURANCE, true, 0d, 0.005d, 1500d),
                createService(ServiceType.RETURN, true, 0d, 0.75d, 999999d)
            ),
            createDirection("Москва", 42),
            List.of(
                createService(ServiceType.CASH_SERVICE, true, 30d, 0.022d, 6600d),
                createService(ServiceType.WAIT_20, false, null, null, null),
                createService(ServiceType.INSURANCE, true, 0d, 0.005d, 1500d),
                createService(ServiceType.RETURN, true, 0d, 0.75d, 999999d)
            ),
            createDirection("Москва", "Россия, Амурская область, Благовещенск"),
            List.of(
                createService(ServiceType.CASH_SERVICE, true, 30d, 0.022d, 6600d),
                createService(ServiceType.WAIT_20, false, null, null, null),
                createService(ServiceType.INSURANCE, true, 0d, 0.005d, 1500d),
                createService(ServiceType.RETURN, true, 0d, 0.75d, 999999d)
            )
        );
    }

    @Nonnull
    private Map<DirectionRaw, List<DeliveryPriceRaw>> createDeliveryPrices() {
        return Map.of(
            createDirection("Москва", "Россия, Алтайский край, Барнаул"),
            List.of(
                createDeliveryPrice(0.0, 1.0, 143d, 0d, 5, 7),
                createDeliveryPrice(1.0, 3.0, 178d, 0d, 5, 7),
                createDeliveryPrice(3.0, 5.0, 265d, 0d, 5, 7),
                createDeliveryPrice(5.0, 10.0, 367d, 0d, 5, 7),
                createDeliveryPrice(10.0, 15.0, 489d, 0d, 5, 7),
                createDeliveryPrice(15.0, 500.0, 515d, 27d, 5, 7)
            ),
            createDirection("Москва", 42),
            List.of(
                createDeliveryPrice(0.0, 1.0, 143d, 0d, 8, 9),
                createDeliveryPrice(1.0, 3.0, 178d, 0d, 8, 9),
                createDeliveryPrice(3.0, 5.0, 265d, 0d, 8, 9),
                createDeliveryPrice(5.0, 10.0, 367d, 0d, 8, 9),
                createDeliveryPrice(10.0, 15.0, 489d, 0d, 8, 9),
                createDeliveryPrice(15.0, 500.0, 515d, 27d, 8, 9)
            ),
            createDirection("Москва", "Россия, Амурская область, Благовещенск"),
            List.of(
                createDeliveryPrice(0.0, 1.0, 148d, 0d, 11, 12),
                createDeliveryPrice(1.0, 3.0, 194d, 0d, 11, 12),
                createDeliveryPrice(3.0, 5.0, 295d, 0d, 11, 12),
                createDeliveryPrice(5.0, 10.0, 417d, 0d, 11, 12),
                createDeliveryPrice(10.0, 15.0, 580d, 0d, 11, 12),
                createDeliveryPrice(15.0, 500.0, 614d, 34d, 11, 12)
            )
        );
    }

    private DeliveryPriceRaw createDeliveryPrice(
        Double minWeight,
        Double maxWeight,
        Double fixedCost,
        Double deltaCost,
        Integer minDays,
        Integer maxDays
    ) {
        return DeliveryPriceRaw.builder()
            .minWeight(minWeight)
            .maxWeight(maxWeight)
            .publicFixedCost(fixedCost)
            .publicDeltaCost(deltaCost)
            .minDays(minDays)
            .maxDays(maxDays)
            .build();
    }

    private PriceListRaw expectedMinimalMarketCourierPriceList() {
        final PriceListRaw priceListRaw = expectedMinimalGeneralPriceList();
        priceListRaw.setLocalityOnly(false);
        return priceListRaw;
    }

    private PriceListRaw expectedMinimalGeneralPriceList() {
        return defaultPriceList()
            .dimensionsLimit(DimensionsLimitRaw.builder()
                .length(250)
                .width(200)
                .height(200)
                .weight(500)
                .dimensionalWeight(250)
                .dimensionsSum(400)
                .build())
            .servicePrices(
                Map.of(
                    createDirection("Москва", "Россия, Алтайский край, Барнаул"),
                    List.of(
                        ServicePriceRaw.builder()
                            .serviceCode(ServiceType.CASH_SERVICE)
                            .enabled(true)
                            .nonPublicMinCost(0d).publicMinCost(0d)
                            .nonPublicPriceValue(0.01d).publicPriceValue(0.01d)
                            .nonPublicMaxCost(10000.0d).publicMaxCost(10000.0d)
                            .build(),
                        ServicePriceRaw.builder()
                            .serviceCode(ServiceType.WAIT_20)
                            .enabled(true)
                            .nonPublicMinCost(0d).publicMinCost(0d)
                            .nonPublicPriceValue(0d).publicPriceValue(0d)
                            .nonPublicMaxCost(0d).publicMaxCost(0d)
                            .build(),
                        ServicePriceRaw.builder()
                            .serviceCode(ServiceType.INSURANCE)
                            .enabled(true)
                            .nonPublicMinCost(0d).publicMinCost(0d)
                            .nonPublicPriceValue(0.003d).publicPriceValue(0.003d)
                            .nonPublicMaxCost(3000.0d).publicMaxCost(3000.0d)
                            .build(),
                        ServicePriceRaw.builder()
                            .serviceCode(ServiceType.RETURN)
                            .enabled(true)
                            .nonPublicMinCost(0d).publicMinCost(0d)
                            .nonPublicPriceValue(0.7d).publicPriceValue(0.75d)
                            .nonPublicMaxCost(99999d).publicMaxCost(999999d)
                            .build()
                    )
                )
            )
            .deliveryPrices(Map.of(
                createDirection("Москва", "Россия, Алтайский край, Барнаул"),
                List.of(
                    DeliveryPriceRaw.builder()
                        .minWeight(0.0)
                        .maxWeight(1.0)
                        .nonPublicFixedCost(135d).publicFixedCost(143d)
                        .nonPublicDeltaCost(0d).publicDeltaCost(0d)
                        .minDays(5)
                        .maxDays(7)
                        .build()
                )
            ))
            .build();
    }

    private Map<DirectionRaw, List<ServicePriceRaw>> minimalServicePrices() {
        return Map.of(
            createDirection("Москва", "Россия, Алтайский край, Барнаул"),
            List.of(
                createService(ServiceType.CASH_SERVICE, false, null, null, null),
                createService(ServiceType.WAIT_20, false, null, null, null),
                createService(ServiceType.INSURANCE, false, null, null, null),
                createService(ServiceType.RETURN, true, 0d, 0.75d, 999999d)
            )
        );
    }

    private PriceListRaw expectedMinimalPriceList() {
        return defaultPriceList()
            .dimensionsLimit(DimensionsLimitRaw.builder()
                .length(250)
                .width(200)
                .height(200)
                .weight(500)
                .dimensionalWeight(250)
                .dimensionsSum(400)
                .build())
            .servicePrices(minimalServicePrices())
            .deliveryPrices(Map.of(
                createDirection("Москва", "Россия, Алтайский край, Барнаул"),
                List.of(
                    createDeliveryPrice(0.0, 1.0, 143d, 0d, 5, 7)
                )
            ))
            .build();
    }

    private PriceListRaw expectedPriceListWithErrors(ReaderContext context) {
        PriceListRaw priceList = defaultPriceList()
            .dimensionsLimit(DimensionsLimitRaw.builder()
                .length(250)
                .width(200)
                .height(200)
                .weight(500)
                .dimensionalWeight(250)
                .dimensionsSum(400)
                .build())
            .servicePrices(minimalServicePrices())
            .deliveryPrices(Map.of(
                createDirection("Москва", "Россия, Алтайский край, Бийск"),
                List.of(),
                createDirection("Москва", "Россия, Алтайский край, Барнаул"),
                List.of(createDeliveryPrice(0.0, 1.0, 143d, 0d, 5, 7)),
                createDirection("Москва", "Россия, Новосибирск"),
                List.of(createDeliveryPrice(0.0, 1.0, 143d, 0d, 5, 7))
            ))
            .build();

        context.getCellToError()
            .putAll(
                new ImmutableMap.Builder<CellPosition, String>()
                    .put(new CellPosition(SheetType.SERVICES, "G", 4), "Услуга 'Возврат' проигнорирована")
                    .put(new CellPosition(SheetType.SERVICES, "G", 5), "Услуга 'Возврат' проигнорирована")
                    .put(
                        new CellPosition(SheetType.SERVICES, "J", 4),
                        "Значение ячейки не должно быть отрицательным\nНе заполнено"
                    )
                    .put(new CellPosition(SheetType.SERVICES, "A", 3), "Локация отправления не заполнена")
                    .put(
                        new CellPosition(SheetType.SERVICES, "B", 4),
                        "Диапазон доступных значений широты от -90.0 до 90.0"
                    )
                    .put(
                        new CellPosition(SheetType.SERVICES, "C", 4),
                        "Диапазон доступных значений долготы от -180.0 до 180.0"
                    )
                    .put(new CellPosition(SheetType.DELIVERY, "D", 3), "Локация назначения не заполнена")
                    .put(
                        new CellPosition(SheetType.SERVICES, "H", 4),
                        "Не удалось прочитать числовое значение\nНе заполнено"
                    )
                    .put(
                        new CellPosition(SheetType.SERVICES, "A", 4),
                        "Направление доставки проигнорировано, так как следующие обязательные услуги не найдены " +
                            "или содержат ошибки: [Возврат]"
                    )
                    .put(
                        new CellPosition(SheetType.SERVICES, "A", 5),
                        "Направление доставки проигнорировано, так как следующие обязательные услуги не найдены " +
                            "или содержат ошибки: [Возврат]"
                    )
                    .put(
                        new CellPosition(SheetType.DELIVERY, "G", 4),
                        "Минимальный вес больше чем Максимальный вес [min: 5.0, max: 3.0]"
                    )
                    .put(
                        new CellPosition(SheetType.DELIVERY, "K", 4),
                        "Минимальный срок больше чем Максимальный срок [min: 5, max: 3]"
                    )
                    .put(
                        new CellPosition(SheetType.DELIVERY, "I", 4),
                        "Значение ячейки не должно быть отрицательным"
                    )
                    .put(new CellPosition(SheetType.DELIVERY, "G", 5), "Не заполнено")
                    .put(new CellPosition(SheetType.DELIVERY, "H", 6), "Не заполнено")
                    .put(new CellPosition(SheetType.DELIVERY, "I", 7), "Не заполнено")
                    .put(new CellPosition(SheetType.DELIVERY, "J", 8), "Не заполнено")
                    .put(new CellPosition(SheetType.DELIVERY, "K", 9), "Не заполнено")
                    .put(new CellPosition(SheetType.DELIVERY, "L", 10), "Не заполнено")
                    .build()
            );
        return priceList;
    }

    private PriceListRaw expectedGeneralPriceListWithErrors(ReaderContext context) {
        PriceListRaw priceList = defaultPriceList()
            .dimensionsLimit(DimensionsLimitRaw.builder()
                .length(250)
                .width(200)
                .height(200)
                .weight(500)
                .dimensionalWeight(250)
                .dimensionsSum(400)
                .build())
            .servicePrices(
                Map.of()
            )
            .deliveryPrices(Map.of(
                createDirection("Москва", "Россия, Алтайский край, Бийск"),
                List.of(),
                createDirection("Москва", "Россия, Алтайский край, Барнаул"),
                List.of(
                    DeliveryPriceRaw.builder()
                        .minWeight(0.0)
                        .maxWeight(1.0)
                        .nonPublicFixedCost(143d).publicFixedCost(143d)
                        .nonPublicDeltaCost(0d).publicDeltaCost(0d)
                        .minDays(5)
                        .maxDays(7)
                        .build()
                )
            ))
            .build();

        context.getCellToError()
            .putAll(
                new ImmutableMap.Builder<CellPosition, String>()
                    .put(new CellPosition(SheetType.SERVICES, "A", 3), "Локация отправления не заполнена")
                    .put(
                        new CellPosition(SheetType.SERVICES, "B", 4),
                        "Диапазон доступных значений широты от -90.0 до 90.0"
                    )
                    .put(
                        new CellPosition(SheetType.SERVICES, "C", 4),
                        "Диапазон доступных значений долготы от -180.0 до 180.0"
                    )
                    .put(
                        new CellPosition(SheetType.SERVICES, "A", 2),
                        "Направление доставки проигнорировано, так как следующие обязательные услуги не найдены или " +
                            "содержат ошибки: [Возврат, Кассовое обслуживание, Ожидание курьера, Оценочная стоимость]"
                    )
                    .put(
                        new CellPosition(SheetType.SERVICES, "A", 4),
                        "Направление доставки проигнорировано, так как следующие обязательные услуги не найдены или " +
                            "содержат ошибки: [Возврат, Кассовое обслуживание, Ожидание курьера, Оценочная стоимость]"
                    )
                    .put(new CellPosition(SheetType.DELIVERY, "D", 3), "Локация назначения не заполнена")
                    .put(
                        new CellPosition(SheetType.DELIVERY, "G", 4),
                        "Минимальный вес больше чем Максимальный вес [min: 5.0, max: 3.0]"
                    )
                    .put(new CellPosition(SheetType.DELIVERY, "G", 5), "Не заполнено")
                    .put(new CellPosition(SheetType.DELIVERY, "H", 6), "Не заполнено")
                    .put(new CellPosition(SheetType.DELIVERY, "I", 7), "Не заполнено")
                    .put(new CellPosition(SheetType.DELIVERY, "J", 8), "Не заполнено")
                    .put(new CellPosition(SheetType.DELIVERY, "K", 9), "Не заполнено")
                    .put(new CellPosition(SheetType.DELIVERY, "L", 10), "Не заполнено")
                    .put(new CellPosition(SheetType.DELIVERY, "M", 11), "Не заполнено")
                    .put(new CellPosition(SheetType.DELIVERY, "N", 12), "Не заполнено")
                    .put(new CellPosition(SheetType.DELIVERY, "L", 4), "Значение ячейки не должно быть отрицательным")
                    .put(new CellPosition(SheetType.DELIVERY, "K", 4), "Значение ячейки не должно быть отрицательным")
                    .put(
                        new CellPosition(SheetType.DELIVERY, "I", 4),
                        "Минимальный срок больше чем Максимальный срок [min: 5, max: 3]"
                    )
                    .build()
            );
        return priceList;
    }

    private PriceListRaw expectedPriceListWithValidationErrors() {
        return defaultPriceList()
            .dimensionsLimit(DimensionsLimitRaw.builder()
                .length(250)
                .width(200)
                .height(200)
                .weight(500)
                .dimensionalWeight(250)
                .dimensionsSum(400)
                .build())
            .servicePrices(createServicePrices())
            .deliveryPrices(Map.of(
                createDirection("Москва", "Россия, Алтайский край, Барнаул"),
                List.of(
                    createDeliveryPrice(0.0, 1.0, 143d, 0d, 5, 7),
                    createDeliveryPrice(1.0, 3.0, 178d, 0d, 5, 7),
                    createDeliveryPrice(3.0, 5.0, 265d, 0d, 5, 7),
                    createDeliveryPrice(5.0, 10.0, 367d, 0d, 5, 7),
                    createDeliveryPrice(10.0, 15.0, 489d, 0d, 5, 7),
                    createDeliveryPrice(15.0, 500.0, 515d, 27d, 5, 7)
                ),
                createDirection("Москва", 42),
                List.of(
                    createDeliveryPrice(0.0, 1.0, 143d, 0d, 8, 9),
                    createDeliveryPrice(1.0, 3.0, 178d, 0d, 8, 9),
                    createDeliveryPrice(3.0, 5.0, 265d, 0d, 8, 9),
                    createDeliveryPrice(5.0, 10.0, 367d, 0d, 8, 9),
                    createDeliveryPrice(10.0, 15.0, 489d, 0d, 8, 9),
                    createDeliveryPrice(15.0, 500.0, 515d, 27d, 8, 9)
                )
            ))
            .build();
    }

    private PriceListRaw expectedPriceListWithWeightBreaks() {
        return defaultPriceList()
            .scale(new BigDecimal("0.5"))
            .dimensionsLimit(DimensionsLimitRaw.builder()
                .length(250)
                .width(200)
                .height(200)
                .weight(500)
                .dimensionalWeight(250)
                .dimensionsSum(400)
                .build())
            .servicePrices(minimalServicePrices())
            .deliveryPrices(Map.of(
                createDirection("Москва", "Россия, Алтайский край, Барнаул"),
                List.of(
                    createDeliveryPrice(0.0, 1.0, 143d, 0d, 5, 7),
                    createDeliveryPrice(1.0, 10.0, 143d, 10d, 5, 7)
                )
            ))
            .build();
    }

    private ServicePriceRaw createService(
        ServiceType serviceType,
        Boolean enabled,
        Double minCost,
        Double priceValue,
        Double maxCost
    ) {
        return ServicePriceRaw.builder()
            .serviceCode(serviceType)
            .enabled(enabled)
            .publicMinCost(minCost)
            .publicPriceValue(priceValue)
            .publicMaxCost(maxCost)
            .build();
    }

    private DirectionRaw createDirection(String from, String to) {
        return DirectionRaw.builder()
            .from(LocationRaw.builder().address(from).build())
            .to(LocationRaw.builder().address(to).build())
            .build();
    }

    private DirectionRaw createDirection(String from, Integer geoId) {
        return DirectionRaw.builder()
            .from(LocationRaw.builder().address(from).build())
            .to(LocationRaw.builder().geoId(geoId).build())
            .build();
    }

    private DirectionRaw createDirection(
        String from,
        Double llFrom,
        Double ltFrom,
        String to,
        Double llTo,
        Double ltTo
    ) {
        return DirectionRaw.builder()
            .from(LocationRaw.builder()
                .address(from)
                .latitude(llFrom)
                .longitude(ltFrom)
                .build())
            .to(LocationRaw.builder()
                .address(to)
                .latitude(llTo)
                .longitude(ltTo)
                .build())
            .build();
    }

    private PriceListSource createPriceListSource(String path) throws IOException {
        Path xlsxPath = getFile(path).toPath();
        return new SpreadsheetPriceListSource(Files.readAllBytes(xlsxPath));
    }

    private static File getFile(String relativeFilePath) {
        return new File(getSystemResource(relativeFilePath).getFile());
    }
}
