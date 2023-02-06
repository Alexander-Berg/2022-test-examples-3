package ui_tests.src.test.java.pages.dataImportPage.beruOutgoingCallTicketByOrderIdPage;

import org.openqa.selenium.WebDriver;
import pages.dataImportPage.Header;
import pages.dataImportPage.Tabs;

public class BeruOutgoingCallTicketByOrderIdPage {
    private WebDriver webDriver;

    public BeruOutgoingCallTicketByOrderIdPage(WebDriver webDriver) {
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
