package ui_tests.src.test.java.pages.dataImportPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class Header {
    private WebDriver webDriver;

    public Header(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public void clickSaveRecordButton() {
        Entity.header(webDriver).clickButtonSaveForm("Добавить");
    }
}
