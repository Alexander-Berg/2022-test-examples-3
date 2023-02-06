package ui_tests.src.test.java.pages.orderPage.orderPage.generalInformationTab;

import entity.Entity;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;

public class PaymentInfo {
    private WebDriver webDriver;

    public PaymentInfo(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    /**
     * Проверить что в поле "Подтверждения покупки" ничего нет
     *
     * @return
     */
    public String getPurchaseConfirmation() {
        try {
            return Entity.properties(webDriver).getValueField("warrantyUrl");
        } catch (Throwable t) {
            throw new Error("Не удалось получить поле 'Подтверждения покупки':\n" + t);
        }
    }
}
