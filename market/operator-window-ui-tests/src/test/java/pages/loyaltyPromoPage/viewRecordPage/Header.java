package ui_tests.src.test.java.pages.loyaltyPromoPage.viewRecordPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class Header {

    private WebDriver webDriver;

    public Header(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Нажать на кнопку Изменить
     */
    public void clickEditRecordButton() {
        try {
            Entity.header(webDriver).clickButtonEditForm("Изменить");
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажать на кнопку Изменить\n" + throwable);
        }
    }

    /**
     * Нажать на кнопку Архивировать
     */
    public void clickArchiveRecordButton() {
        try {
            Entity.header(webDriver).clickButtonEditStatus("Архивировать");
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажать на кнопку Архивировать\n" + throwable);
        }
    }
}
