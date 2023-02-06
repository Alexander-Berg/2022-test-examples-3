package ui_tests.src.test.java.pages.customerPage;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import tools.Tools;

public class Header {

    private WebDriver webDriver;

    public Header(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Выбрать заказ для привязки
     *
     * @param orderId
     */
    public void selectOrder(String orderId) {
        webDriver.findElement(By.xpath(".//*[@*[starts-with(name(.),'data-ow-test')]='autocomplete']/span/div/div/input"))
                .sendKeys(orderId);
        Tools.clickerElement(webDriver).clickElement(By.xpath("//*[@id='ow-popper-portal']//*[ text()='" + orderId + "']"));
    }

    /**
     * Нажать на кнопку привязки заказа
     */
    public void clickBindingOrderButton() {
        Tools.clickerElement(webDriver).clickElement(By.xpath(".//button[@title='Привязать заказ']"));
    }
}
