package ui_tests.src.test.java.pages.dataImportPage.calcelOrdersPage;

import org.openqa.selenium.WebDriver;
import pages.dataImportPage.Header;
import pages.dataImportPage.Tabs;

public class CancelOrdersPage {
    private WebDriver webDriver;

    public CancelOrdersPage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Блок с вкладками
     *
     * @return
     */
    public Tabs tabs() {
        return new Tabs(webDriver);
    }

    /**
     * Вкладка "Форма"
     *
     * @return
     */
    public TabForm tabForm() {
        return new TabForm(webDriver);
    }

    /**
     * Шапка вкладки "Форма"
     *
     * @return
     */
    public Header header() {
        return new Header(webDriver);
    }

}
