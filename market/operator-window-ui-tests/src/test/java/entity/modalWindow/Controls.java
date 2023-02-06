package ui_tests.src.test.java.entity.modalWindow;

import entity.Entity;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public class Controls extends Entity {
    private String block = "//span[@data-ow-test-modal-controls]";
    private WebDriver webDriver;

    public Controls(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
//        Tools.waitElement(webDriver).waitVisibilityElementTheTime(By.xpath(block), Config.DEF_TIME_WAIT_LOAD_PAGE);
    }

    /**
     * Нажать на кнопку с назвнием {buttonName}
     *
     * @param buttonName название кнопки
     */
    public void clickButton(String buttonName) {
        try {
            buttons(webDriver).clickButton(block, buttonName);
        } catch (Throwable t) {
            throw new Error("Не удалось нажать на кнопку " + buttonName + " \n" + t);
        }
    }
}
