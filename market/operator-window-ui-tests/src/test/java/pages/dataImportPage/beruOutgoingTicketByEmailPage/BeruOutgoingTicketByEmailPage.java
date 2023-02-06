package ui_tests.src.test.java.pages.dataImportPage.beruOutgoingTicketByEmailPage;

import org.openqa.selenium.WebDriver;
import pages.dataImportPage.Header;
import pages.dataImportPage.Tabs;

public class BeruOutgoingTicketByEmailPage {
    private WebDriver webDriver;

    public BeruOutgoingTicketByEmailPage(WebDriver webDriver) {
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
