package ui_tests.src.test.java.pages.ticketPage.clientTicketsTab;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class RelatedTicketsTable {
    private WebDriver webDriver;

    public RelatedTicketsTable(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    private String block = "//div[text()='Связанные обращения']/..";

    /**
     * Получить количество строк в таблице
     */
    public int getNumberOfRows() {
        try {
            return Entity.simpleTable(webDriver).getNumberOfRows(block);
        } catch (Throwable e) {
            throw new Error("Не удалось посчитать количество строк в таблице 'Связанные обращения': \n" + e);
        }
    }

    /**
     * Получить Тип связи у указанной строки
     */
    public String getRelationType(int rowNumber) {
        try {
            return Entity.simpleTable(webDriver).getValueFromCellByAttributeAndRow("title", rowNumber);
        } catch (Throwable e) {
            throw new Error("Не удалось получить значение из столбца 'Тип связи' из строки " + rowNumber + " : \n" + e);
        }
    }

    /**
     * Получить Ссылку у указанной строки
     */
    public String getLink(int rowNumber) {
        try {
            return Entity.simpleTable(webDriver).getValueFromCellByAttributeAndRow("externalUrl", rowNumber);
        } catch (Throwable e) {
            throw new Error("Не удалось получить значение из столбца 'Ссылка' из строки " + rowNumber + " : \n" + e);
        }
    }

    /**
     * Получить Связанный объект у указанной строки
     */
    public String getRelatedObject(int rowNumber) {
        try {
            return Entity.simpleTable(webDriver).getValueFromCellByAttributeAndRow("relatedObject", rowNumber);
        } catch (Throwable e) {
            throw new Error("Не удалось получить значение из столбца 'Связанный объект' из строки " + rowNumber + " : \n" + e);
        }
    }

    /**
     * Получить Очередь у указанной строки
     */
    public String getService(int rowNumber) {
        try {
            return Entity.simpleTable(webDriver).getValueFromCellByAttributeAndRow("ticketService", rowNumber);
        } catch (Throwable e) {
            throw new Error("Не удалось получить значение из столбца 'Очередь' из строки " + rowNumber + " : \n" + e);
        }
    }

    /**
     * Получить Статус у указанной строки
     */
    public String getStatus(int rowNumber) {
        try {
            return Entity.simpleTable(webDriver).getValueFromCellByAttributeAndRow("ticketStatus", rowNumber);
        } catch (Throwable e) {
            throw new Error("Не удалось получить значение из столбца 'Статус' из строки " + rowNumber + " : \n" + e);
        }
    }
}
