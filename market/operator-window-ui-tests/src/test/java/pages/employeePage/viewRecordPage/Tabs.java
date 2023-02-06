package ui_tests.src.test.java.pages.employeePage.viewRecordPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class Tabs {
    private WebDriver webDriver;

    public Tabs(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Открыть вкладку Основные параметры
     */
    public void openMainPropertiesTab() {
        try {
            Entity.tabs(webDriver).openTab("Основные парамерты");
        } catch (Throwable throwable) {
            throw new Error("Открыть вкладку Основные параметры\n" + throwable);
        }
    }

    /**
     * Открыть вкладку Обращения
     */
    public void openTicketsTab() {
        try {
            Entity.tabs(webDriver).openTab("Обращения");
        } catch (Throwable throwable) {
            throw new Error("Открыть вкладку Обращения\n" + throwable);
        }
    }
}
