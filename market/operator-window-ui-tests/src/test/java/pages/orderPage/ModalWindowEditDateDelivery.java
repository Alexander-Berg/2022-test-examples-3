package ui_tests.src.test.java.pages.orderPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class ModalWindowEditDateDelivery {
    private final WebDriver webDriver;

    public ModalWindowEditDateDelivery(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Нажать на кнопку Подтвердить
     */
    public void clickConfirmDeliveryDateButton() {
        Entity.modalWindow(webDriver).controls().clickButton("Подтвердить");
    }
}
