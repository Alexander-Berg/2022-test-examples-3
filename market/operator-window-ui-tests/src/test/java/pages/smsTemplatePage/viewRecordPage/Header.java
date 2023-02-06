package ui_tests.src.test.java.pages.smsTemplatePage.viewRecordPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class Header {
    private WebDriver webDriver;

    public Header(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Нажать на кнопку изменить
     */
    public void clickEditPageButton() {
        try {
            Entity.header(webDriver).clickButtonEditForm("Изменить");
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажвать на кнопку Изменения записи\n" + throwable);
        }
    }
}
