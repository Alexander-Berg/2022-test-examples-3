package ui_tests.src.test.java.pages.ticketPage.createTicketPage;

import entity.Header;
import entity.comments.CommentsCreation;
import org.openqa.selenium.WebDriver;

public class CreateTicketPage {

    private WebDriver webDriver;

    public CreateTicketPage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public Properties properties() {
        return new Properties(webDriver);
    }

    public CommentsCreation commentsCreation() {
        return new CommentsCreation(webDriver);
    }

    public Header header() {
        return new Header(webDriver);
    }
}
