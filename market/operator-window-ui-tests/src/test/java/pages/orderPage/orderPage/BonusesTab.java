package ui_tests.src.test.java.pages.orderPage.orderPage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import tools.Tools;

import java.util.List;

public class BonusesTab {
    private final WebDriver webDriver;

    public BonusesTab(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Получить значение из ячейки таблицы
     *
     * @param tableName  - название таблицы
     * @param columnName - название колонки
     * @param rowNumber  - номер строки
     * @return значение из указанной ячейки
     */
    public String getValueFromTable(String tableName, String columnName, int rowNumber) {
        // Получить набор колонок в таблице
        List<WebElement> columnsList;
        try {
            columnsList = Tools.findElement(webDriver).findElements(By.xpath(String.format
                    ("//div[text()='%s']/ancestor::div[@class='jmf-simple-table']//div[@class='WFhuSnaf']/div[@class='_1GMGsRC7 _1rF67My8']",
                            tableName)));
        } catch (Throwable e) {
            throw new Error("Не удалось получить список колонок из таблицы " + tableName + ": \n" + e);
        }

        // Определить номер искомой колонки
        int columnNumber = 0;
        for (WebElement column : columnsList) {
            if (columnNumber > columnsList.size() + 1) throw new Error("В таблице нет колонки " + columnName);
            if (column.getText().equals(columnName)) break;
            columnNumber++;
        }

        // Вытащить значение из ячейки
        String cellValue;
        try {
            cellValue = Tools.findElement(webDriver).findElement(By.xpath(String.format("//div[text()='%s']/ancestor::div[@class='jmf-simple-table']//div[@class='WFhuSnaf']/div[@data-row='%d' and @data-column='%d']", tableName, rowNumber - 1, columnNumber))).getText();
        } catch (Throwable e) {
            throw new Error("Не удалось получить значение из ячейки: \n" + e);
        }
        return cellValue;
    }

    /**
     * Вернуть количество строк в таблице
     */
    public int getNumberOfRows(String tableName) {
        int res = 0;
        List<WebElement> rows = Tools.findElement(webDriver).findElements(
                By.xpath(String.format("//div[text()='%s']/ancestor::div[@class='jmf-simple-table']//div[@class='WFhuSnaf']/div[@data-row]",
                        tableName)));
        for (WebElement row : rows) {
            int rowNumber = Integer.parseInt(row.getAttribute("data-row")) + 1;
            if (rowNumber > res) {
                res = rowNumber;
            }
        }
        return res;
    }
}
