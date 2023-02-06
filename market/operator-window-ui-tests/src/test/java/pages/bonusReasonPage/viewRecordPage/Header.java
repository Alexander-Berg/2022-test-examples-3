package ui_tests.src.test.java.pages.bonusReasonPage.viewRecordPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;
import tools.Tools;

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
            Tools.waitElement(webDriver).waitTime(3000);
            Entity.header(webDriver).clickButtonEditForm("изменить");
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
