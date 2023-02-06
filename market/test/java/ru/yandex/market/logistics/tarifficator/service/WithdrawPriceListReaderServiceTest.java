package ru.yandex.market.logistics.tarifficator.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.validation.Validation;

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
import ru.yandex.market.logistics.tarifficator.model.pricelist.CellPosition;
import ru.yandex.market.logistics.tarifficator.model.pricelist.ReaderContext;
import ru.yandex.market.logistics.tarifficator.model.pricelist.scheme.SheetType;
import ru.yandex.market.logistics.tarifficator.model.source.SpreadsheetWithdrawPriceListSource;
import ru.yandex.market.logistics.tarifficator.model.withdraw.WithdrawPriceListItemRaw;
import ru.yandex.market.logistics.tarifficator.model.withdraw.WithdrawPriceListRaw;
import ru.yandex.market.logistics.tarifficator.service.pricelist.spreadsheet.PriceListParserFactory;
import ru.yandex.market.logistics.tarifficator.service.pricelist.spreadsheet.ServiceColumnPrefixProvider;
import ru.yandex.market.logistics.tarifficator.service.pricelist.spreadsheet.SpreadsheetMapper;
import ru.yandex.market.logistics.tarifficator.service.withdraw.pricelist.spreadsheet.WithdrawPriceListReaderService;
import ru.yandex.market.logistics.tarifficator.service.withdraw.pricelist.spreadsheet.WithdrawPriceListReaderServiceImpl;

import static java.lang.ClassLoader.getSystemResource;

class WithdrawPriceListReaderServiceTest extends AbstractUnitTest {
    private final ServiceColumnPrefixProvider serviceColumnPrefixProvider = new ServiceColumnPrefixProvider();
    private final WithdrawPriceListReaderService readerService = new WithdrawPriceListReaderServiceImpl(
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
    @DisplayName("Успешное чтение файла заборного прайс-листа")
    void readWithdrawPriceListFileSuccess() throws IOException {
        SpreadsheetWithdrawPriceListSource source =
            createPriceListSource("service/withdraw/pricelist/xlsx/price-list-file.xlsx");
        WithdrawPriceListRaw priceList = readerService.read(source, context);
        WithdrawPriceListRaw expectedPriceList = expectedWithdrawPriceList();

        softly.assertThat(context.getCellToError()).hasSize(0);
        softly.assertThatObject(priceList)
            .isEqualToComparingFieldByFieldRecursively(expectedPriceList);
    }

    @Test
    @DisplayName("Чтение файла заборного прайс-листа без необходимых колонок")
    void readWithdrawPriceListFileWithoutRequiredColumns() throws IOException {
        SpreadsheetWithdrawPriceListSource source =
            createPriceListSource("service/withdraw/pricelist/xlsx/price-list-file-without-required-columns.xlsx");
        FileProcessingException e = Assertions.assertThrows(
            FileProcessingException.class,
            () -> readerService.read(source, context)
        );

        softly.assertThat(e).hasMessage("Cannot find columns: 'Объем От (м3)'");
    }

    @Test
    @DisplayName("Чтение файла заборного прайс-листа с ошибками в строках")
    void readWithdrawPriceListFileWithErrors() throws IOException {
        SpreadsheetWithdrawPriceListSource source =
            createPriceListSource("service/withdraw/pricelist/xlsx/price-list-file-with-errors.xlsx");
        readerService.read(source, context);

        softly.assertThat(context.getCellToError()).isEqualToComparingFieldByFieldRecursively(expectedErrors());
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("getPriceListWeightsValidationArguments")
    @DisplayName("Проверка валидации диапазонов объёма в заборном прайс-листе")
    void priceListVolumesValidationTests(String filename, String errorMessage, String displayName) throws IOException {
        SpreadsheetWithdrawPriceListSource source = createPriceListSource(filename);
        readerService.read(source, context);

        softly.assertThatObject(context.getCellToError())
            .isEqualToComparingFieldByFieldRecursively(
                Map.of(
                    new CellPosition(SheetType.WITHDRAW, "A", 2),
                    errorMessage
                )
            );
    }

    @Test
    @DisplayName("Проверка валидации дублирующихся строк")
    void readPriceListWithDuplicatingLines() throws IOException {
        SpreadsheetWithdrawPriceListSource source =
            createPriceListSource("service/withdraw/pricelist/xlsx/price-list-with-dublicates.xlsx");
        readerService.read(source, context);

        softly.assertThatObject(context.getCellToError())
            .isEqualToComparingFieldByFieldRecursively(
                Map.of(
                    new CellPosition(SheetType.WITHDRAW, "A", 10),
                    "[Цена для объёма в диапазоне между 3.0 м3 и 10.0 м3 указана более одного раза]"
                )
            );
    }

    @Test
    @DisplayName("Проверка валидации minVolume < maxVolume")
    void priceListWithMinVolumeGreaterThanMaxVolume() throws IOException {
        SpreadsheetWithdrawPriceListSource source =
            createPriceListSource("service/withdraw/pricelist/xlsx/price-list-file-with-min-greater-than-max.xlsx");
        readerService.read(source, context);

        softly.assertThatObject(context.getCellToError())
            .isEqualToComparingFieldByFieldRecursively(
                Map.of(
                    new CellPosition(SheetType.WITHDRAW, "B", 2),
                    "Объем От (м3) больше чем Объем До (м3) [min: 0.3, max: 0.0]"
                )
            );
    }

    @Nonnull
    private static Stream<Arguments> getPriceListWeightsValidationArguments() {
        return Stream.of(
            Arguments.of(
                "service/withdraw/pricelist/xlsx/price-list-file-with-non-covered-volume-range.xlsx",
                "[Цена для объёма в диапазоне между 0.3 м3 и 0.4 м3 не указана]",
                "Непокрытые диапазоны объёма"
            ),
            Arguments.of(
                "service/withdraw/pricelist/xlsx/price-list-file-with-volume-range-inteception.xlsx",
                "[Цена для объёма в диапазоне между 0.3 м3 и 0.4 м3 указана более одного раза]",
                "Пересечение диапазонов объёма"
            )
        );
    }

    @Nonnull
    private WithdrawPriceListRaw expectedWithdrawPriceList() {
        return WithdrawPriceListRaw.builder()
            .withdrawPriceListItemsRaw(List.of(
                WithdrawPriceListItemRaw.builder()
                    .cost(220D)
                    .locationZoneName("внутри мкад")
                    .minVolume(0D)
                    .maxVolume(0.3D)
                    .build(),
                WithdrawPriceListItemRaw.builder()
                    .cost(260D)
                    .locationZoneName("внутри мкад")
                    .minVolume(0.3D)
                    .maxVolume(1D)
                    .build(),
                WithdrawPriceListItemRaw.builder()
                    .cost(450D)
                    .locationZoneName("внутри мкад")
                    .minVolume(1D)
                    .maxVolume(3D)
                    .build(),
                WithdrawPriceListItemRaw.builder()
                    .cost(900D)
                    .locationZoneName("внутри мкад")
                    .minVolume(3D)
                    .maxVolume(10D)
                    .build(),
                WithdrawPriceListItemRaw.builder()
                    .cost(300D)
                    .locationZoneName("от 0 до 10 км от мкад")
                    .minVolume(0D)
                    .maxVolume(3D)
                    .build(),
                WithdrawPriceListItemRaw.builder()
                    .cost(1050D)
                    .locationZoneName("от 0 до 10 км от мкад")
                    .minVolume(3D)
                    .maxVolume(10D)
                    .build(),
                WithdrawPriceListItemRaw.builder()
                    .cost(650D)
                    .locationZoneName("от 10 до 30 км от мкад")
                    .minVolume(0D)
                    .maxVolume(3D)
                    .build(),
                WithdrawPriceListItemRaw.builder()
                    .cost(2450D)
                    .locationZoneName("от 10 до 30 км от мкад")
                    .minVolume(3D)
                    .maxVolume(10D)
                    .build(),
                WithdrawPriceListItemRaw.builder()
                    .cost(2150D)
                    .locationZoneName("от 30 км от мкад до границ мо")
                    .minVolume(0D)
                    .maxVolume(3D)
                    .build(),
                WithdrawPriceListItemRaw.builder()
                    .cost(5200D)
                    .locationZoneName("от 30 км от мкад до границ мо")
                    .minVolume(3D)
                    .maxVolume(10D)
                    .build()
            ))
            .build();
    }

    @Nonnull
    private SpreadsheetWithdrawPriceListSource createPriceListSource(String path) throws IOException {
        Path xlsxPath = getFile(path).toPath();
        return new SpreadsheetWithdrawPriceListSource(Files.readAllBytes(xlsxPath));
    }

    @Nonnull
    private static File getFile(String relativeFilePath) {
        return new File(getSystemResource(relativeFilePath).getFile());
    }

    @Nonnull
    private Map<CellPosition, String> expectedErrors() {
        return Map.of(
            new CellPosition(SheetType.WITHDRAW, "D", 9), "Значение ячейки не должно быть отрицательным",
            new CellPosition(SheetType.WITHDRAW, "A", 2), "Не заполнено\nЗона локации не указана",
            new CellPosition(SheetType.WITHDRAW, "B", 7), "Значение ячейки не должно быть отрицательным",
            new CellPosition(SheetType.WITHDRAW, "B", 3), "Не заполнено",
            new CellPosition(SheetType.WITHDRAW, "D", 5), "Не заполнено",
            new CellPosition(SheetType.WITHDRAW, "C", 8), "Значение ячейки не должно быть отрицательным",
            new CellPosition(SheetType.WITHDRAW, "C", 4), "Не заполнено"
        );
    }
}
