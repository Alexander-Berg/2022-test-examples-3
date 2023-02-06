package ui_tests.src.test.java.entity;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import tools.Tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class SimpleTable {

    private WebDriver webDriver;

    public SimpleTable(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Получить количество строк в таблице
     */
    public int getNumberOfRows(String block) {
        // Посчитать количество колонок
        int numberOfColumns = Tools.findElement(webDriver).findElements(By.xpath(block + "//div[contains(@class,'Nwgtzgpv K8dboubh')]")).size();
        // Посчитать, сколько ячеек в табличке, и разделить на количество колонок
        return Tools.findElement(webDriver).findElements(By.xpath(block + "//div[@class='Nwgtzgpv']")).size() / numberOfColumns;
    }

    /**
     * Получить значение ячейки с заданным data-ow-test-attribute-container в указанной строке
     */
    public String getValueFromCellByAttributeAndRow(String attributeCode, int rowNumber) {
        try {
            return Tools.findElement(webDriver).findElement(By.xpath(String.format(
                    "(//*[@*[starts-with(name(.),'data-ow-test')]='%s'])[%s]",
                    attributeCode, rowNumber))).getText();
        } catch (Throwable e) {
            throw new Error(String.format("Не удалось получить значение из ячейки %s и строки %s: \n", attributeCode, rowNumber) + e);
        }
    }

    /**
     * Получаем все данные из таблицы
     *
     * @param block
     * @return данные выдаются в формате:
     * - один HashMap - одна строка в таблице
     * - HashMap.key - код элемента из атрибута "data-ow-test-attribute-container"
     * - HashMap.value - значение ячейки
     */
    public List<HashMap<String, String>> getDateFromTable(String block) {
        List<HashMap<String, String>> tables = new ArrayList<>();
        List<WebElement> columnsElement = Tools.findElement(webDriver).findElementsWithAnExpectationToAppearInDOM(By.xpath(block + "//div[contains(@class,'Nwgtzgpv K8dboubh')]"));
        int countColumns = columnsElement.size();
        int index = 0;
        List<WebElement> elementsTable = Tools.findElement(webDriver).findElementsWithAnExpectationToAppearInDOM(By.xpath(block + "//*[@*[starts-with(name(.),'data-ow-test')]]"));
        int countRow = getNumberOfRows(block);
        for (int i = 0; i < countRow; i++) {
            HashMap<String, String> row = new HashMap<>();
            for (int x = 0; x < countColumns; x++) {
                WebElement element = elementsTable.get(index++);
                String key = "";
                String attribute = Tools.other().getSubString("(data-ow-test-[^(hidden)].*?=)",element.getAttribute("outerHTML"));
                attribute = attribute.substring(0,attribute.length()-1);
                key = element.getAttribute(attribute);
                 if (element.getAttribute("data-ow-test-hidden") != null) {
                    if (element.getAttribute("data-ow-test-hidden").equals("true")) {
                        Entity.buttons(webDriver).clickCustomButton(element, "********");
                    }
                }
                String value = element.getText();
                row.put(key, value);
            }
            tables.add(row);
        }
        return tables;
    }
}
