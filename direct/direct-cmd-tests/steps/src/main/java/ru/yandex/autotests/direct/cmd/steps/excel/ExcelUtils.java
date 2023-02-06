package ru.yandex.autotests.direct.cmd.steps.excel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import ru.yandex.autotests.direct.cmd.data.excel.ExcelColumnsEnum;
import ru.yandex.autotests.direct.cmd.steps.base.DirectCmdStepsException;
import ru.yandex.qatools.allure.annotations.Step;

public class ExcelUtils {

    private final static int FIRST_DATA_ROW = 11;
    // В тестовых данных координаты ячейки с номером кампании такие. Этот код не претендует на общность
    private final static int CAMPAIGN_ID_CELL_ROW = 7;
    private final static int CAMPAIGN_ID_CELL_COL = 4;

    public static void modifyLabel(File source, File dest,
            ExcelColumnsEnum column, int row, String content)
    {
        setCellValue(source, dest, column, row, content);
    }

    public static void removeRow(File source, File dest, ExcelColumnsEnum column, String value) {
        try {
            Workbook wb = WorkbookFactory.create(source);
            Sheet sheet = wb.getSheetAt(0);
            Row rowToDelete = getRowByCellText(sheet, column, value);
            if (rowToDelete.getRowNum() == sheet.getLastRowNum()) {
                sheet.removeRow(rowToDelete);
            } else if (sheet.getLastRowNum() > rowToDelete.getRowNum()) {
                sheet.shiftRows(rowToDelete.getRowNum(), sheet.getLastRowNum(), -1);
            }
            FileOutputStream fileOut = new FileOutputStream(dest, false);
            wb.write(fileOut);
            fileOut.close();
        } catch (Exception e) {
            throw new IllegalStateException("ошибка изменения excel-файла", e);
        }
    }

    public static String getCellValue(File source, ExcelColumnsEnum column, int row) {
        try {
            Workbook wb = WorkbookFactory.create(source);
            Sheet sheet = wb.getSheetAt(0);
            return getValueFrom(sheet, column, FIRST_DATA_ROW + row);
        } catch (Exception e) {
            throw new IllegalStateException("ошибка чтения excel-файла", e);
        }
    }

    @Step("Excel: Запись в строку {2} в колонку \"{1}\" данных: {3}")
    public static void setCellValue(File file,
            ExcelColumnsEnum column, int row, String content)
    {
        setCellValue(file, file, column, row, content);
    }

    @Step("Excel: Запись в строку {3} в колонку \"{2}\" данных: {4}")
    public static void setCellValue(File source, File dest,
            ExcelColumnsEnum column, int row, String content)
    {
        try (FileInputStream inputStream = new FileInputStream(source);
             Workbook wb = WorkbookFactory.create(inputStream))
        {
            Sheet sheet = wb.getSheetAt(0);
            setValueTo(sheet, column, FIRST_DATA_ROW + row, content);
            FileOutputStream fileOut = new FileOutputStream(dest);
            wb.write(fileOut);
            fileOut.close();
        } catch (Exception e) {
            throw new IllegalStateException("ошибка изменения excel-файла", e);
        }
    }

    @Step("Excel: Установка № заказа: {1}")
    public static void setCampaignId(File file, long cid) {
        setCampaignId(file, file, cid);
    }

    @Step("Excel: Установка № заказа: {2}")
    public static void setCampaignId(File source, File dest, long cid) {
        try (FileInputStream inputStream = new FileInputStream(source);
             Workbook wb = WorkbookFactory.create(inputStream);
             FileOutputStream outputStream = new FileOutputStream(dest))
        {
            Sheet sheet = wb.getSheetAt(0);
            Cell cell = sheet.getRow(CAMPAIGN_ID_CELL_ROW)
                    .getCell(CAMPAIGN_ID_CELL_COL, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
            cell.setCellValue(cid);
            wb.write(outputStream);
        } catch (InvalidFormatException | IOException e) {
            throw new IllegalStateException("ошибка изменения excel-файла", e);
        }
    }

    public static void changeHyperLink(File source, File dest, int row,
            ExcelColumnsEnum textColumn, String newLink)
    {
        try (Workbook wb = WorkbookFactory.create(source)) {
            Sheet sheet = wb.getSheetAt(0);
            int colNum = getColNumByText(textColumn.getCaption(), sheet);
            sheet.getHyperlinkList().stream()
                    .filter(
                            link -> link.getFirstColumn() == colNum && link.getFirstRow() == FIRST_DATA_ROW + row
                    )
                    .findFirst().orElseThrow(() -> new IllegalStateException("искомая ячейка excel-файла не найдена"))
                    .setAddress(newLink);
            setValueTo(sheet, textColumn, FIRST_DATA_ROW + row, newLink);
            FileOutputStream fileOut = new FileOutputStream(dest);
            wb.write(fileOut);
            fileOut.close();
        } catch (Exception e) {
            throw new IllegalStateException("ошибка изменения excel-файла", e);
        }
    }

    private static void setValueTo(Sheet sheet, ExcelColumnsEnum column, int row, String value) {
        int colNum = getColNumByText(column.getCaption(), sheet);
        Cell cell = sheet.getRow(row).getCell(colNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellValue(value);
    }

    private static String getValueFrom(Sheet sheet, ExcelColumnsEnum column, int row) {
        int colNum = getColNumByText(column.getCaption(), sheet);
        Cell cell = sheet.getRow(row).getCell(colNum);
        return cell.getStringCellValue();
    }

    private static int getColNumByText(String colText, Sheet sheet) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                if (colText.equals(cell.toString())) {
                    return cell.getColumnIndex();
                }
            }
        }
        throw new DirectCmdStepsException("Column with text " + colText + " not found");
    }

    public static Row getRowByCellText(Sheet sheet, ExcelColumnsEnum column, String rowText) {
        int colNum = getColNumByText(column.getCaption(), sheet);
        for (int rowIndex = FIRST_DATA_ROW; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Cell currCell = sheet.getRow(rowIndex).getCell(colNum);
            switch (currCell.getCellType()) {
                case 0:
                    if (Double.valueOf(currCell.getNumericCellValue()).equals(Double.valueOf(rowText))) {
                        return sheet.getRow(rowIndex);
                    }
                    break;
                default:
                    if (currCell.getStringCellValue().equals(rowText)) {
                        return sheet.getRow(rowIndex);
                    }
            }
        }
        throw new DirectCmdStepsException("Row with text " + rowText + " not found by column " + column.getCaption());
    }

    /**
     * Установить в строку {@code row} ID группы, баннера и фразы.
     * Необходимо в случае работы с новой тестовой кампанией
     */
    public static void updateRow(
            File newCampFile, File excelToUpload, int row,
            Long campaignId, Long adGroupId, Long bannerId, Long phraseId) {
        setCampaignId(newCampFile, excelToUpload, campaignId);
        setCellValue(excelToUpload, ExcelColumnsEnum.GROUP_ID, row, String.valueOf(adGroupId));
        setCellValue(excelToUpload, ExcelColumnsEnum.BANNER_ID, row, String.valueOf(bannerId));
        setCellValue(excelToUpload, ExcelColumnsEnum.PHRASE_ID, row, String.valueOf(phraseId));
    }
}
