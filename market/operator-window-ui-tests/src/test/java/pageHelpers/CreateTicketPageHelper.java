package ui_tests.src.test.java.pageHelpers;

import Classes.Comment;
import org.openqa.selenium.WebDriver;
import pages.Pages;

public class CreateTicketPageHelper {
    private WebDriver webDriver;

    public CreateTicketPageHelper(WebDriver webDriver){
        this.webDriver=webDriver;
    }
    /**
     * Заполнить поле Комментарий
     * @param comment
     * @return
     */
    public CreateTicketPageHelper setComment(Comment comment) {
         if (comment.getType().equals("public")) {
            Pages.ticketPage(webDriver).createTicketPage().commentsCreation().clickOutputMailTab();
        } else if (comment.getType().equals("contact")) {
            Pages.ticketPage(webDriver).createTicketPage().commentsCreation().clickPartnerMailTab();
        } else {
            Pages.ticketPage(webDriver).createTicketPage().commentsCreation().clickInternalMailTab();
        }
        Pages.ticketPage(webDriver).createTicketPage().commentsCreation().setTextComment(comment.getText());
        return this;
    }
}
