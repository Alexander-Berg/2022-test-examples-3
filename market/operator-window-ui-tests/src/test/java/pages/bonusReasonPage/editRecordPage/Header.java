package ui_tests.src.test.java.pages.bonusReasonPage.editRecordPage;


import entity.Entity;
import org.openqa.selenium.WebDriver;

public class Header {
    private WebDriver webDriver;

    public Header(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Нажать на кнопку Сохранить
     */
    public void clickSaveRecordButton() {

        try {
            Entity.header(webDriver).clickButtonSaveForm("Сохранить");
        } catch (Throwable throwable) {
            throw new Error("Не удалось нажать на кнопку Сохранить\n" + throwable);
        }

    }
}
