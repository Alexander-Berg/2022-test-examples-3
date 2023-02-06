package ru.yandex.market.deepmind.common.services.tracker_approver.excel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Assert;
import org.junit.Test;

public class AutoOkRuleExcelCreatorTest {
    public static final List<Error> ERRORS = List.of(
        new Error(3, 20, "error2"),
        new Error(4, 0, "error3"),
        new Error(4, 0, "error4")
    );

    /**
     * Method to apply AutoOkRule locally.
     * It can be useful while debugging
     *  or to regenerate test sources after making changes in {@link AutoOkRuleExcelCreator}.
     */
    public void applyAutoOkRuleLocally() throws IOException {
        var sourcePath = "excel_files/autoOkRuleExcelCreatorFileAfter.xlsx";
        var targetPath = "absolute-path/file-name.xlsx";

        var source = getWorkBookFromFile(sourcePath);
        var result = AutoOkRuleExcelCreator.populateWithErrors(source, List.of());

        try (var fileOutputStream = new FileOutputStream(targetPath)) {
            result.write(fileOutputStream);
        }
    }

    @Test
    public void testAutoOkRuleExcelCreatorTest() throws IOException {
        Workbook workbook = getWorkBookFromFile("excel_files/autoOkRuleExcelCreatorFileBefore.xlsx");
        Workbook result = AutoOkRuleExcelCreator.populateWithErrors(workbook, ERRORS);

        Workbook correctBook = getWorkBookFromFile("excel_files/autoOkRuleExcelCreatorFileAfter.xlsx");
        compareBooks(correctBook, result);
    }

    @Test
    public void testAutoOkRuleExcelCreatorWithColoredExcelTest() throws IOException {
        Workbook workbook = getWorkBookFromFile("excel_files/autoOkRuleExcelCreatorFileAfter.xlsx");
        Workbook result = AutoOkRuleExcelCreator.populateWithErrors(workbook, List.of());

        Workbook correctBook = getWorkBookFromFile("excel_files/autoOkRuleExcelCreatorFileOk.xlsx");
        compareBooks(correctBook, result);
    }

    private void compareBooks(Workbook correctBook, Workbook resultBook) {
        Sheet correctSheet = correctBook.getSheetAt(0);
        Sheet resultSheet = resultBook.getSheetAt(0);

        int rowCounts = correctSheet.getPhysicalNumberOfRows();
        for (int j = 0; j < rowCounts; j++) {
            var row = correctSheet.getRow(j);
            if (row != null) {
                int cellCounts = row.getPhysicalNumberOfCells();
                for (int k = 0; k < cellCounts; k++) {
                    Cell correctCell = correctSheet.getRow(j).getCell(k);
                    Cell resultCell = resultSheet.getRow(j).getCell(k);
                    if (correctCell != null) {
                        Assert.assertEquals(
                            correctCell.getCellStyle().getFillForegroundColorColor(),
                            resultCell.getCellStyle().getFillForegroundColorColor()
                        );
                        if (correctCell.getCellComment() != null) {
                            String correctComment = String.valueOf(correctCell.getCellComment().getString());
                            String resultComment = String.valueOf(resultCell.getCellComment().getString());
                            Assert.assertEquals(correctComment, resultComment);
                        }
                    }
                }
            }
        }
    }

    private Workbook getWorkBookFromFile(String path) throws IOException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(path);
        return WorkbookFactory.create(is);
    }
}
