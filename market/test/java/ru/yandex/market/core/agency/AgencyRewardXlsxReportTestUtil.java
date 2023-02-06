package ru.yandex.market.core.agency;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Assertions;

/**
 * Утилиты для тестирования xlsx-отчета агентства.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public final class AgencyRewardXlsxReportTestUtil {

    private AgencyRewardXlsxReportTestUtil() {
        throw new UnsupportedOperationException();
    }

    /**
     * Проверить отчет. Путь проверочного файла: "xlsx/" + expectedFile
     * Можно раскомментировать вызов метода saveFile(os, expectedFile);. Тогда xlsx сохранится в ~/agency_reward/expectedFile.xslx
     */
    public static void checkXlsx(final ByteArrayOutputStream actualDataStream, final String expectedFile, final Class<?> clazz) throws IOException {
        final ByteArrayInputStream is = new ByteArrayInputStream(actualDataStream.toByteArray());
        checkXlsx(is, expectedFile, clazz);
    }

    public static void checkXlsx(final ByteArrayInputStream actualDataStream, final String expectedFile, final Class<?> clazz) throws IOException {
        final String expectedData = getExpectedData(expectedFile, clazz);

        // Может быть полезным для тестирования. Не удаляй :)
//        saveFile(actualDataStream, expectedFile);

        final String actualData = getActualData(actualDataStream);
        Assertions.assertEquals(expectedData, actualData);
    }

    /**
     * Преобразует xlsx в формат для проверки.
     * <p>
     * ---sheet: Название листа---
     * col1;col2;
     * val1;val2;
     * ---sheet: Второй лист---
     * ;;;
     * val1;val2;val3
     */
    public static String getActualData(final InputStream is) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            final StringBuilder builder = new StringBuilder();
            workbook.sheetIterator().forEachRemaining(sheet -> {
                builder.append("---sheet: ").append(sheet.getSheetName()).append("---\n");
                sheet.rowIterator().forEachRemaining(row -> {
                    row.cellIterator().forEachRemaining(cell -> {
                        final String cellValue = getCellValue(cell);
                        builder.append(cellValue).append(";");
                    });
                    builder.append("\n");
                });
            });
            return builder.toString();
        }
    }

    private static String getExpectedData(final String expectedFile, final Class<?> clazz) throws IOException {
        try (InputStream is = clazz.getResourceAsStream("xlsx/" + expectedFile)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    private static String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case FORMULA:
                return cell.getCellFormula();

            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());

            default:
                return cell.getStringCellValue();
        }
    }

    private static void saveFile(final ByteArrayInputStream is, final String name) throws IOException {
        final File tmpDir = SystemUtils.getUserHome();
        final File dir = new File(tmpDir, "agency_reward");
        FileUtils.forceMkdir(dir);

        final File file = new File(dir, name + ".xlsx");
        Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        is.reset();
    }
}
