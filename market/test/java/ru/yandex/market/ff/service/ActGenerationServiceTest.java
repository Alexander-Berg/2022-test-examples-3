package ru.yandex.market.ff.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.ShopRequest;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Базовый класс функциональных тестов для {@link ActGenerationService}.
 */
public abstract class ActGenerationServiceTest extends IntegrationTest {

    @Autowired
    protected ShopRequestFetchingService requestService;

    void assertPdfActGeneration(long requestId,
                                String fileName,
                                Function<ShopRequest, InputStream> generator)
        throws IOException {
        assertPdf(getGeneratedActInputStream(requestId, generator), fileName);
    }

    void assertXlsxActGeneration(long requestId,
                                 String fileName,
                                 Function<ShopRequest, InputStream> generator)
        throws IOException {
        assertXlsx(getGeneratedActInputStream(requestId, generator), fileName);
    }

    private InputStream getGeneratedActInputStream(long requestId, Function<ShopRequest, InputStream> generator) {
        final ShopRequest request = requestService.getRequest(requestId).get();
        return generator.apply(request);
    }

    private void assertPdf(final InputStream is, String fileName) throws IOException {
        // IOUtils.copy(is, new FileOutputStream(new File("file.pdf")));

        final PDFParser parser = new PDFParser(new RandomAccessBuffer(is));
        parser.parse();

        final PDDocument document = parser.getPDDocument();

        final PDFTextStripper stripper = new PDFTextStripper();
        final String text = stripper.getText(document);
//        FileUtils.writeStringToFile(new File(""), text, StandardCharsets.UTF_8);

        final InputStream expectedIS = getSystemResourceAsStream("service/pdf-report/" + fileName);
        final String expectedText = IOUtils.toString(expectedIS, StandardCharsets.UTF_8);

        System.out.println(text);
        assertions.assertThat(text).isEqualToIgnoringWhitespace(expectedText);
    }

    private void assertXlsx(final InputStream is, String fileName) throws IOException {
        Workbook actualWorkbook = new XSSFWorkbook(is);
        Workbook expectedWorkbook = new XSSFWorkbook(getSystemResourceAsStream("service/xlsx-report/" + fileName));

        assertThat("Asserting that the number of sheets in workbook is valid",
            actualWorkbook.getNumberOfSheets(), equalTo(expectedWorkbook.getNumberOfSheets()));

        Sheet actualSheet = actualWorkbook.getSheetAt(0);
        Sheet expectedSheet = expectedWorkbook.getSheetAt(0);

        assertThat("Asserting that the number of the last row in sheet is valid",
            actualSheet.getLastRowNum(), equalTo(expectedSheet.getLastRowNum()));

        for (int i = 0; i < actualSheet.getLastRowNum(); i++) {
            Row actualRow = actualSheet.getRow(i);
            Row expectedRow = expectedSheet.getRow(i);

            assertRowSoftly(actualRow, expectedRow);
        }
    }

    private void assertRowSoftly(Row actualRow, Row expectedRow) {
        short actualLastCellNum = actualRow.getLastCellNum();
        short expectedLastCellNum = expectedRow.getLastCellNum();

        assertions.assertThat(actualLastCellNum)
            .as("Asserting that the number of the last cell in row is valid for row: " + actualRow.getRowNum())
            .isEqualTo(expectedLastCellNum);

        if (actualLastCellNum != expectedLastCellNum) {
            return;
        }

        for (int i = 0; i < actualLastCellNum; i++) {
            Cell actualCell = actualRow.getCell(i);
            Cell expectedCell = expectedRow.getCell(i);

            assertCellSoftly(actualCell, expectedCell);
        }
    }

    private void assertCellSoftly(Cell actualCell, Cell expectedCell) {
        if (actualCell == null || expectedCell == null) {
            assertions.assertThat(actualCell == null && expectedCell == null)
                .as("Asserting that both cells are null")
                .isTrue();
            return;
        }

        CellType actualCellType = actualCell.getCellType();
        CellType expectedCellType = expectedCell.getCellType();

        assertions.assertThat(actualCellType)
            .as("Asserting that the type of the cell is valid for cell: " + actualCell.getAddress())
            .isEqualTo(expectedCellType);
        assertions.assertThat(actualCell.getCellStyle())
            .as("Asserting that the style of the cell is valid for cell: " + actualCell.getAddress())
            .isEqualTo(expectedCell.getCellStyle());

        if (actualCellType != expectedCellType) {
            return;
        }

        switch (actualCellType) {
            case NUMERIC:
                assertions.assertThat(actualCell.getNumericCellValue())
                    .as("Asserting that the numeric value of the cell is valid for cell: " + actualCell.getAddress())
                    .isEqualTo(expectedCell.getNumericCellValue());
                break;
            case STRING:
                assertions.assertThat(actualCell.getStringCellValue())
                    .as("Asserting that the string value of the cell is valid for cell: " + actualCell.getAddress())
                    .isEqualTo(expectedCell.getStringCellValue());
                break;
            case BOOLEAN:
                assertions.assertThat(actualCell.getBooleanCellValue())
                    .as("Asserting that the boolean value of the cell is valid for cell: " + actualCell.getAddress())
                    .isEqualTo(expectedCell.getBooleanCellValue());
                break;
            case ERROR:
                assertions.assertThat(actualCell.getErrorCellValue())
                    .as("Asserting that the error value of the cell is valid for cell: " + actualCell.getAddress())
                    .isEqualTo(expectedCell.getErrorCellValue());
                break;
            default:
        }
    }
}
