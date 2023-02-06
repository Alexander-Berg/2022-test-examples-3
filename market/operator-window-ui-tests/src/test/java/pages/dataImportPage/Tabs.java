package ui_tests.src.test.java.pages.dataImportPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class Tabs {
    private WebDriver webDriver;

    public Tabs(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Открыть вкладку Форма
     */
    public void openFormTab() {
        Entity.tabs(webDriver).openTab("Форма");
    }
}
