package ui_tests.src.test.java.pages.servicePage.createServicePage;

import entity.Entity;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public class Header {
    private WebDriver webDriver;
    private String block = "//div[@class='_2JLvZL31']";

    public Header(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    /**
     * Нажать кнопку Сохранить
     */
    public void saveButtonClick() {
        Entity.header(webDriver).clickButtonSaveForm("Сохранить");
    }

}
