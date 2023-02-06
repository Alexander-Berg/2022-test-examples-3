package ui_tests.src.test.java.pages.ticketCommentTemplatePage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

// По аналогии с pages/ticketPage/ToolBar.java
public class Header {
    private WebDriver webDriver;

    public Header(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Получить тему обращения
     *
     * @return
     */
    public String getSubject() {
        return Entity.header(webDriver).getSubject("default");
    }
}
