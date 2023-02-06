package ui_tests.src.test.java.pages.dataImportPage.beruOutgoingCallTicketByClientPhonePage;

import org.openqa.selenium.WebDriver;
import pages.dataImportPage.Header;
import pages.dataImportPage.Tabs;
import pages.dataImportPage.beruOutgoingTicketByOrderIdPage.TabForm;

public class BeruOutgoingCallTicketByClientPhonePage {

    private WebDriver webDriver;

    public BeruOutgoingCallTicketByClientPhonePage(WebDriver webDriver) {
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
    public pages.dataImportPage.beruOutgoingTicketByOrderIdPage.TabForm tabForm() {
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
