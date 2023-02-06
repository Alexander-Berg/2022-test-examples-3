package ui_tests.src.test.java.pages.orderPage.orderPage.generalInformationTab;

import org.openqa.selenium.WebDriver;

public class GeneralInformationTab {
    private final WebDriver webDriver;

    public GeneralInformationTab(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Блок комментариев
     *
     * @return
     */
    public Comments comments() {
        return new Comments(webDriver);
    }

    /**
     * Блок с товарами заказа
     *
     * @return
     */
    public OrderItems orderItems() {
        return new OrderItems(webDriver);
    }

    public PaymentInfo paymentInfo(){return new PaymentInfo(webDriver);}
}
