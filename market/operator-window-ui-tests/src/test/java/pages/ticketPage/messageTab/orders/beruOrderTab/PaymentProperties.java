package ui_tests.src.test.java.pages.ticketPage.messageTab.orders.beruOrderTab;

import entity.Entity;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;

import java.util.HashMap;
import java.util.List;

public class PaymentProperties {
    private final WebDriver webDriver;

    public PaymentProperties(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    /**
     * Получить со страницы плательщика заказа
     *
     * @return
     */
    public String getPayer() {
        try {
            return Entity.properties(webDriver).getValueField("buyerFullName");
        } catch (Throwable t) {
            throw new Error("Не удалось получить плательщика заказа\n" + t);
        }
    }

    /**
     * Получить со страницы стоимость за товары по заказу
     *
     * @return
     */
    public String getOrderAmount() {
        try {
            return Entity.properties(webDriver).getValueField("buyerItemsTotal");
        } catch (Throwable t) {
            throw new Error("Не удалось получить общую стоимость за товары по заказу:\n" + t);
        }
    }

    /**
     * Получить со старницы стоимость заказа доставки заказа
     *
     * @return
     */
    public String getCostDelivery() {
        try {
            return Entity.properties(webDriver).getValueField("deliveryPrice");
        } catch (Throwable t) {
            throw new Error("Не удалось получить стоимость доставки заказа:\n" + t);
        }
    }

    /**
     * Получить со страницы итоговую стоимость по заказу
     *
     * @return
     */
    public String getTotalCostOrder() {
        try {
            return Entity.properties(webDriver).getValueField("buyerTotal");
        } catch (Throwable t) {
            throw new Error("Не удалось получить итоговую стоимость по заказу:\n" + t);
        }
    }

    /**
     * Получить со страницы способ оплаты заказа
     *
     * @return
     */
    public String getTypePayment() {
        try {
            return Entity.properties(webDriver).getValueField("paymentMethod");
        } catch (Throwable t) {
            throw new Error("Не удалось получить способ оплаты заказа:\n" + t);
        }
    }

    /**
     * Получить со страницы Способы платежа и сумму платежа
     *
     * @return
     */
    public HashMap<String, String> getTypePaymentAndPaymentAmount() {
        HashMap<String, String> map = new HashMap<>();
        try {
            Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//*[@*[starts-with(name(.),'data-ow-test')]=\"paymentKind\"]"));
            List<WebElement> typePayment = Tools.findElement(webDriver).findElements(By.xpath("//*[@*[starts-with(name(.),'data-ow-test')]=\"paymentKind\"]"));
            List<WebElement> paymentAmount = Tools.findElement(webDriver).findElements(By.xpath("//*[@*[starts-with(name(.),'data-ow-test')]=\"amount\"]"));
            int i = 0;
            for (WebElement webElement : typePayment) {
                map.put(webElement.getText(), paymentAmount.get(i++).getText());
            }

        } finally {
            return map;
        }
    }

    /**
     * Проверить, что блок оплаты отображается на странице 1 раз (не дулбируется)
     */
    public void checkRenderingOfPaymentPropertiesBlock() {
        By paymentPropertiesBlock = By.xpath("//*[text()='Оплата']");
        Tools.waitElement(webDriver).waitVisibilityElement(paymentPropertiesBlock);
        Assert.assertEquals("Блок 'Оплата' отображается на превью больше одного раза",
                1, Tools.other().getNumberOfElementOccurrences(webDriver, paymentPropertiesBlock));
    }

    /**
     * Получить со страницы значение поля "Накопленный кэшбэк"
     *
     * @return
     */
    public String getAccruedCashBack() {
        return Entity.properties(webDriver).getValueField("accumulatedCashback");
    }

}
