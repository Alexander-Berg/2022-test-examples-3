package ui_tests.src.test.java.pages.yaDeliveryOrderPage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import tools.Tools;

public class MainProperties {
    private WebDriver webDriver;

    public MainProperties(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Получить номер заказа
     */
    public String getOrderNumber() {
        return Tools.findElement(webDriver).findElement(By.xpath("//span[contains(text(),'№')]")).getText();
    }
}
