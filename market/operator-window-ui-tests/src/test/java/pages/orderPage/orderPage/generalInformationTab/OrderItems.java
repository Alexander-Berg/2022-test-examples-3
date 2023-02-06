package ui_tests.src.test.java.pages.orderPage.orderPage.generalInformationTab;

import Classes.OrderItem;
import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import tools.Tools;

import java.util.ArrayList;
import java.util.List;

public class OrderItems {
    private final WebDriver webDriver;

    public OrderItems(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Получить данные по всем товарам на карточке заказа
     *
     * @return
     */
    public List<OrderItem> getOrderItems() {
        List<OrderItem> orderItems = new ArrayList<>();
        String accruedCashBack = null;
        String cashBackSpent = null;
        String plannedCashback = null;
        String orderMarkerTitle = null;

        Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//*[contains(@class,'qFovWjjp')]"));
        List<WebElement> webElements = Tools.findElement(webDriver).findElements(By.xpath("//*[contains(@class,'qFovWjjp')]"));

        for (WebElement webElement : webElements) {
            OrderItem orderItem = new OrderItem();

            try {
                accruedCashBack = Entity.properties(webDriver).getValueField(webElement, "creditedCashback");
            } catch (Throwable noSuchElementException) {
                if (!noSuchElementException.getMessage().contains("NoSuchElementException")) {
                    throw new Error(noSuchElementException);
                }
            }
            try {
                cashBackSpent = Entity.properties(webDriver).getValueField(webElement, "spentCashback");
            } catch (Throwable noSuchElementException) {
                if (!noSuchElementException.getMessage().contains("NoSuchElementException")) {
                    throw new Error(noSuchElementException);
                }
            }

            try {
                plannedCashback = Entity.properties(webDriver).getValueField(webElement, "plannedCashback");
            } catch (Throwable noSuchElementException) {
                if (!noSuchElementException.getMessage().contains("NoSuchElementException")) {
                    throw new Error(noSuchElementException);
                }
            }
            try {
                orderMarkerTitle = Entity.properties(webDriver).getValueField(webElement, "orderMarkerTitle");
            } catch (Throwable noSuchElementException) {
                if (!noSuchElementException.getMessage().contains("NoSuchElementException")) {
                    throw new Error(noSuchElementException);
                }
            }
            orderItem.setAccruedCashBack(accruedCashBack);
            orderItem.setCashBackSpent(cashBackSpent);
            orderItem.setPlannedCashback(plannedCashback);
            orderItem.setOrderMarketTitle(orderMarkerTitle);

            orderItems.add(orderItem);
        }

        return orderItems;
    }

}
