package ru.yandex.market.common.excel.out.impl.template;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.assertj.core.api.Assertions;

import ru.yandex.market.common.excel.MarketTemplate;
import ru.yandex.market.common.excel.XlsConfig;
import ru.yandex.market.common.excel.XlsSheet;
import ru.yandex.market.common.excel.converter.impl.apache.ApacheConverter;
import ru.yandex.market.common.excel.converter.impl.apache.OOXMLConverter;
import ru.yandex.market.common.excel.template.impl.HeadersConverter;

import static ru.yandex.market.common.excel.converter.Xls2CsvConverter.DELIMITER;

/**
 * @author fbokovikov
 */
@SuppressWarnings("SameParameterValue")
public class AbstractTemplateTest {

    private static final String QUOTES_TEXT_PATTERN = "\"([^\"]*)\"";

    protected String getConvertedFileResourceAsString(String fileName) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            URL res = getClass().getClassLoader()
                    .getResource(fileName);
            URI uri = Objects.requireNonNull(res)
                    .toURI();

            new ApacheConverter().convert(Paths.get(uri).toFile(), osw);

            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String getConvertedExternalTemplate(String fileName, MarketTemplate marketTemplate) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
            URL res = getClass().getClassLoader()
                    .getResource(fileName);

            OOXMLConverter.convert(Paths.get(Objects.requireNonNull(res).toURI()).toFile(), marketTemplate,
                    (csvRow) -> {
                        try {
                            osw.write(String.join(DELIMITER, csvRow) + "\n");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
            );
            osw.flush();

            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Collection<String> getColumnNames(Workbook workbook) {
        XlsConfig xlsConfig = XlsConfig.readConfig(workbook);
        Sheet offers = XlsSheet.getPoiSheetFromWorkbook(xlsConfig.getXlsSheet(), workbook);
        Row row = offers.getRow(1);

        return HeadersConverter.getColumnNames(row, xlsConfig, workbook.getCreationHelper().createFormulaEvaluator());
    }

    Workbook createWorkbook(String excelFileName) {
        try {
            URL res = getClass().getClassLoader()
                    .getResource(excelFileName);

            return WorkbookFactory.create(Paths.get(Objects.requireNonNull(res).toURI()).toFile(), null, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void assertNamesConverted(String excelFileName) {
        Workbook workbook = createWorkbook(excelFileName);
        Collection<String> columnNames = getColumnNames(workbook);

        columnNames.forEach(name -> Assertions.assertThat(name).isNotNull());
    }

    protected void validateContent(String excelFileName) {
        Workbook workbook = createWorkbook(excelFileName);
        Collection<String> columnNames = getColumnNames(workbook);
        String convertedFile = getConvertedFileResourceAsString(excelFileName);
        String[] delimitedLines = convertedFile.split("\n");

        int expectedSize = columnNames.size() - 1;
        Stream.of(delimitedLines)
                .forEach(line -> assertLineContent(expectedSize, line));
    }

    private void assertLineContent(int expectedSize, @Nonnull String line) {
        String quotes = line.replaceAll(QUOTES_TEXT_PATTERN, "_");
        Assertions.assertThat(StringUtils.countMatches(quotes, DELIMITER))
                .isEqualTo(expectedSize);
    }

    /**
     * Ищет файл по имени и вычитывает его в строку
     *
     * @param fileName имя файла
     * @return файл в виде строки
     * @throws IOException в случае ошибки чтения
     */
    protected String readFile(String fileName) throws IOException {
        InputStream inputStream = Objects.requireNonNull(getClass().getClassLoader()
                .getResourceAsStream(fileName));

        return IOUtils.toString(inputStream);
    }
}
