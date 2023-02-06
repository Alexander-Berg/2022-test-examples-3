package ru.yandex.market.core.feed.validation.result;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.common.excel.wrapper.PoiWorkbook;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Класс утилита для работы с excel-файлами
 * Date: 19.11.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
public class XlsTestUtils {
    private static final Logger LOG = LoggerFactory.getLogger(XlsTestUtils.class);

    private XlsTestUtils() {
        throw new UnsupportedOperationException("Must not be called!");
    }

    /**
     * Проверяем excel-лист на то, что он содержит ожидаемые данные
     *
     * @param expected  ожидаемые данные
     * @param workbook  excel-файл
     * @param sheetInfo информация о листе
     */
    public static void assertSheet(Map<CellInfo, String> expected,
                                   Workbook workbook,
                                   SheetInfo sheetInfo) {
        DataFormatter formatter = new DataFormatter();
        Sheet sheet = StringUtils.isEmpty(sheetInfo.sheetName)
                ? workbook.getSheetAt(sheetInfo.sheetNumber)
                : workbook.getSheet(sheetInfo.sheetName);

        assertNotNull(sheet);
        Assertions.assertThat(sheet.getPhysicalNumberOfRows())
                .isEqualTo(sheetInfo.rowCount);

        int columnsCount = expected.keySet().stream().mapToInt(value -> value.col).max().orElse(0);
        var sortedExpected = expected.entrySet().stream()
                .sorted(Comparator.comparingInt(a -> a.getKey().row * columnsCount + a.getKey().col))
                .collect(Collectors.toList());
        for (Map.Entry<CellInfo, String> entry : sortedExpected) {
            CellInfo cellInfo = entry.getKey();

            String actual = Optional.of(sheet)
                    .map(s -> s.getRow(cellInfo.row))
                    .map(r -> r.getCell(cellInfo.col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK))
                    .map(formatter::formatCellValue)
                    .orElse(null);

            Assertions.assertThat(actual)
                    .withFailMessage("\nRow: %s, Col: %s\nExpected: %s\nActual: %s\n",
                            cellInfo.row, cellInfo.col, entry.getValue(), actual)
                    .isEqualTo(entry.getValue());
        }
    }

    @Nonnull
    public static Consumer<Path> getPathConsumer(Map<XlsTestUtils.CellInfo, String> expected,
                                                 XlsTestUtils.SheetInfo sheetInfo) {
        return path -> {
            try {
                PoiWorkbook workbook = PoiWorkbook.load(path.toFile());
                assertSheet(expected, workbook.getWorkbook(), sheetInfo);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Nonnull
    public static Map<XlsTestUtils.CellInfo, String> buildExpectedMap(int row, @Nonnull String... values) {
        int col = 0;
        HashMap<CellInfo, String> map = new HashMap<>();

        for (String value : values) {
            map.put(new XlsTestUtils.CellInfo(row, col++), value);
        }

        return map;
    }

    /**
     * Записывает контент {@link ByteArrayOutputStream}'а во временный excel файл.
     * Не убирайте, пожалуйста, этот метод, он нужен чтобы глазами можно было посмотреть на эксельки из тестов.
     *
     * @param output - {@link ByteArrayOutputStream} c контентом для записи в excel файл.
     * @throws IOException
     */
    public static void createTempXlsFrom(ByteArrayOutputStream output) throws IOException {
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()));
        Path tmp = Files.createTempFile("tmp", ".xls");
        try (FileOutputStream fos = new FileOutputStream(tmp.toFile())) {
            workbook.write(fos);
        }
        LOG.info("Temporary excel report: " + tmp.toAbsolutePath().toString());
    }

    /**
     * Информация о листе, который будем проверять
     */
    public static class SheetInfo {
        /**
         * Количество строк
         */
        private final int rowCount;
        /**
         * Номер листа
         */
        private final int sheetNumber;
        /**
         * Название листа
         */
        private final String sheetName;

        public SheetInfo(int rowCount, int sheetNumber, @Nullable String sheetName) {
            this.rowCount = rowCount;
            this.sheetNumber = sheetNumber;
            this.sheetName = sheetName;
        }
    }

    /**
     * Информация о ячейке
     */
    public static class CellInfo {
        /**
         * Строка
         */
        private final int row;
        /**
         * Колонка
         */
        private final int col;

        public CellInfo(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }
}
