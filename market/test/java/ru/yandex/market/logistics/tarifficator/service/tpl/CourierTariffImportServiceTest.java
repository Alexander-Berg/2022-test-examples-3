package ru.yandex.market.logistics.tarifficator.service.tpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.validation.Validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.logistics.tarifficator.AbstractUnitTest;
import ru.yandex.market.logistics.tarifficator.converter.ErrorConverter;
import ru.yandex.market.logistics.tarifficator.converter.SpreadsheetConverter;
import ru.yandex.market.logistics.tarifficator.model.pricelist.CellPosition;
import ru.yandex.market.logistics.tarifficator.model.pricelist.ReaderContext;
import ru.yandex.market.logistics.tarifficator.model.pricelist.scheme.SheetType;
import ru.yandex.market.logistics.tarifficator.model.source.TplCourierTariffSource;
import ru.yandex.market.logistics.tarifficator.service.pricelist.spreadsheet.PriceListParserFactory;
import ru.yandex.market.logistics.tarifficator.service.pricelist.spreadsheet.ServiceColumnPrefixProvider;
import ru.yandex.market.logistics.tarifficator.service.pricelist.spreadsheet.SpreadsheetMapper;

import static java.lang.ClassLoader.getSystemResource;

class CourierTariffImportServiceTest extends AbstractUnitTest {
    private final ServiceColumnPrefixProvider serviceColumnPrefixProvider = new ServiceColumnPrefixProvider();
    private final TestableClock clock = new TestableClock();
    private final CourierTariffReaderServiceImpl courierTariffReaderService = new CourierTariffReaderServiceImpl(
        new PriceListParserFactory(
            clock,
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
        clock.setFixed(Instant.parse("2021-04-15T10:15:30Z"), ZoneId.systemDefault());
        context = new ReaderContext();
    }

    @Test
    @DisplayName("Успешное чтение минимального курьерского тарифа")
    void readMinimal() throws IOException {
        TplCourierTariffSource source = createPriceListSource("service/tpl/courier/tariff/xlsx/minimal.xlsx");
        courierTariffReaderService.read(source, context);

        softly.assertThat(context.getCellToError()).hasSize(0);
    }

    @Test
    @DisplayName("Чтение курьерского тарифа без указания компаний")
    void readTariffWithoutCompanies() throws IOException {
        TplCourierTariffSource source = createPriceListSource(
            "service/tpl/courier/tariff/xlsx/without-companies.xlsx"
        );
        courierTariffReaderService.read(source, context);

        softly.assertThat(context.getCellToError()).isEqualTo(Map.of(
            new CellPosition(SheetType.TPL_COURIER_TARIFF, null, 2),
            "Необходимо заполнить хотя бы одно значение из [Курьерская компания, Сортировочный центр, Тарифная зона]"
        ));
    }

    @Test
    @DisplayName("Чтение курьерского тарифа без пробегов")
    void readTariffWithoutDistances() throws IOException {
        TplCourierTariffSource source = createPriceListSource(
            "service/tpl/courier/tariff/xlsx/without-distances.xlsx"
        );
        courierTariffReaderService.read(source, context);

        softly.assertThat(context.getCellToError()).isEqualTo(Map.of(
            new CellPosition(SheetType.TPL_COURIER_TARIFF, "D", 2),
            "Не заполнено",
            new CellPosition(SheetType.TPL_COURIER_TARIFF, "E", 3),
            "Не заполнено"
        ));
    }

    @Test
    @DisplayName("Чтение курьерского тарифа - некорректный диапазон пробега")
    void readTariffToDistanceLessThanFromDistance() throws IOException {
        TplCourierTariffSource source = createPriceListSource(
            "service/tpl/courier/tariff/xlsx/to-distance-less-than-from-distance.xlsx"
        );
        courierTariffReaderService.read(source, context);

        softly.assertThat(context.getCellToError()).isEqualTo(Map.of(
            new CellPosition(SheetType.TPL_COURIER_TARIFF, "D", 2),
            "Минимальный пробег больше чем Максимальный пробег [min: 130, max: 0]",
            new CellPosition(SheetType.TPL_COURIER_TARIFF, "A", 3),
            "Пробеги должны начинаться с нулевого значения"
        ));
    }

    @Test
    @DisplayName("Чтение курьерского тарифа без дат начала и окончания действия")
    void readTariffWithoutDates() throws IOException {
        TplCourierTariffSource source = createPriceListSource(
            "service/tpl/courier/tariff/xlsx/without-dates.xlsx"
        );
        courierTariffReaderService.read(source, context);

        softly.assertThat(context.getCellToError()).isEqualTo(Map.of(
            new CellPosition(SheetType.TPL_COURIER_TARIFF, "B", 2),
            "Не заполнено",
            new CellPosition(SheetType.TPL_COURIER_TARIFF, "C", 3),
            "Не заполнено"
        ));
    }

    @Test
    @DisplayName("Чтение курьерского тарифа с некорректным диапазоном дат")
    void readTariffToDateLessBeforeFromDate() throws IOException {
        TplCourierTariffSource source = createPriceListSource(
            "service/tpl/courier/tariff/xlsx/to-date-before-from-date.xlsx"
        );
        courierTariffReaderService.read(source, context);

        softly.assertThat(context.getCellToError()).isEqualTo(Map.of(
            new CellPosition(SheetType.TPL_COURIER_TARIFF, "B", 2),
            "Дата начала больше чем Дата окончания [min: 2021-05-01, max: 2021-04-30]",
            new CellPosition(SheetType.TPL_COURIER_TARIFF, "B", 3),
            "Дата начала больше чем Дата окончания [min: 2021-05-01, max: 2021-04-30]"
        ));
    }

    @Test
    @DisplayName("Обновление тарифа в конце месяца")
    void readTariffAtTheEndOfMonth() throws IOException {
        clock.setFixed(Instant.parse("2021-04-28T10:15:30Z"), ZoneId.systemDefault());
        TplCourierTariffSource source = createPriceListSource(
            "service/tpl/courier/tariff/xlsx/minimal.xlsx"
        );
        courierTariffReaderService.read(source, context);

        softly.assertThat(context.getCellToError()).isEqualTo(Map.of(
            new CellPosition(SheetType.TPL_COURIER_TARIFF, "B", 2),
            "Можно менять тарифы задним числом, " +
                "но только за текущий месяц, " +
                "и не позднее последнего дня месяца минуc 2 дня."
        ));
    }

    @Test
    @DisplayName("Чтение курьерского тарифа - дата начала действия тарифа не уникальна")
    void readTariffFromDateIsNotUnique() throws IOException {
        TplCourierTariffSource source = createPriceListSource(
            "service/tpl/courier/tariff/xlsx/from-date-is-not-unique.xlsx"
        );
        courierTariffReaderService.read(source, context);

        softly.assertThat(context.getCellToError()).isEqualTo(Map.of(
            new CellPosition(SheetType.TPL_COURIER_TARIFF, "B", 3),
            "Дата начала действия тарифа должна быть одинаковой на всех строках"
        ));
    }

    @Test
    @DisplayName("Чтение курьерского тарифа - дата окончания действия тарифа не уникальна")
    void readTariffToDateIsNotUnique() throws IOException {
        TplCourierTariffSource source = createPriceListSource(
            "service/tpl/courier/tariff/xlsx/to-date-is-not-unique.xlsx"
        );
        courierTariffReaderService.read(source, context);

        softly.assertThat(context.getCellToError()).isEqualTo(Map.of(
            new CellPosition(SheetType.TPL_COURIER_TARIFF, "C", 3),
            "Дата окончания действия тарифа должна быть одинаковой на всех строках"
        ));
    }

    @Test
    @DisplayName("Чтение курьерского тарифа - расценки не указаны")
    void readTariffPricesAreNotDefined() throws IOException {
        TplCourierTariffSource source = createPriceListSource(
            "service/tpl/courier/tariff/xlsx/prices-are-not-defined.xlsx"
        );
        courierTariffReaderService.read(source, context);

        softly.assertThat(context.getCellToError()).isEqualTo(Map.of(
            new CellPosition(SheetType.TPL_COURIER_TARIFF, null, 2),
            "Необходимо заполнить хотя бы одно значение из [" +
                "Тариф доставки до клиента, " +
                "Тариф доставки до нескольких клиентов на одном адресе, " +
                "Тариф доставки в постамат, Тариф доставки в ПВЗ, " +
                "Тариф за коробку в постамат, " +
                "Тариф за коробку в ПВЗ" +
                "]"
        ));
    }

    @Test
    @DisplayName("Чтение курьерского тарифа - диапазоны пробегов пересекаются")
    void readTariffDistancesAreIntercepted() throws IOException {
        TplCourierTariffSource source = createPriceListSource(
            "service/tpl/courier/tariff/xlsx/distances-are-intercepted.xlsx"
        );
        courierTariffReaderService.read(source, context);

        softly.assertThat(context.getCellToError()).isEqualTo(Map.of(
            new CellPosition(SheetType.TPL_COURIER_TARIFF, "A", 3),
            "Цены в диапазоне между 120 и 131 указаны более одного раза"
        ));
    }

    @Test
    @DisplayName("Чтение курьерского тарифа - диапазоны пробегов не покрыты полностью")
    void readTariffDistancesAreNotCovered() throws IOException {
        TplCourierTariffSource source = createPriceListSource(
            "service/tpl/courier/tariff/xlsx/distances-are-not-covered.xlsx"
        );
        courierTariffReaderService.read(source, context);

        softly.assertThat(context.getCellToError()).isEqualTo(Map.of(
            new CellPosition(SheetType.TPL_COURIER_TARIFF, "A", 3),
            "Цены в диапазоне между 131 и 140 не указаны"
        ));
    }

    @Test
    @DisplayName("Чтение курьерского тарифа - пробеги начинаются не с 0")
    void readTariffDistancesStartFromNotZeroValue() throws IOException {
        TplCourierTariffSource source = createPriceListSource(
            "service/tpl/courier/tariff/xlsx/distances-start-from-not-zero-value.xlsx"
        );
        courierTariffReaderService.read(source, context);

        softly.assertThat(context.getCellToError()).isEqualTo(Map.of(
            new CellPosition(SheetType.TPL_COURIER_TARIFF, "A", 3),
            "Пробеги должны начинаться с нулевого значения"
        ));
    }

    @Test
    @DisplayName("Чтение курьерского тарифа - неизвестный вид опции")
    void readTariffUnknownOptionType() throws IOException {
        TplCourierTariffSource source = createPriceListSource(
            "service/tpl/courier/tariff/xlsx/unknown-option-type.xlsx"
        );
        courierTariffReaderService.read(source, context);

        softly.assertThat(context.getCellToError()).isEqualTo(Map.of(
            new CellPosition(SheetType.TPL_COURIER_TARIFF, "H", 3),
            "Не удалось прочитать значение"
        ));
    }

    @Nonnull
    private TplCourierTariffSource createPriceListSource(String path) throws IOException {
        Path xlsxPath = getFile(path).toPath();
        return new TplCourierTariffSource(Files.readAllBytes(xlsxPath));
    }

    @Nonnull
    private File getFile(String relativeFilePath) {
        return new File(getSystemResource(relativeFilePath).getFile());
    }
}
