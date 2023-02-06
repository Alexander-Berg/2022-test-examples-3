package ui_tests.src.test.java.pages.ticketCommentTemplatePage;

import org.openqa.selenium.WebDriver;

// По аналогии с pages/ticketPage/TicketPage.java
public class TicketCommentTemplatePage {
    private WebDriver webDriver;

    public TicketCommentTemplatePage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Блок с кнопками шаблона
     *
     * @return
     */
    public Header header() {
        return new Header(webDriver);
    }

    /**
     * Раздел со свойствами шаблона
     *
     * @return
     */
    public Properties properties() {
        return new Properties(webDriver);
    }


}
