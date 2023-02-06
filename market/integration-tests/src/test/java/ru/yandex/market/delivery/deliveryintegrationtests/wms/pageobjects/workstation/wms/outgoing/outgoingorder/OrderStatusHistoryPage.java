package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.outgoing.outgoingorder;

import java.util.Collections;
import java.util.List;

import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;

public class OrderStatusHistoryPage extends AbstractWsPage {

    @Name("История статусов")
    private List<WebElement> statusHistory = Collections.emptyList();

    public OrderStatusHistoryPage(WebDriver driver) {
        super(driver);
    }

    @Step("Получаем последний статус заказа")
    public OrderStatus getLastStatus() {
        return getStatusByIndex(0);
    }

    @Step("Получаем предпоследний статус заказа")
    public OrderStatus getSecondLastStatus() {
        return getStatusByIndex(1);
    }

    @Step("Проверяем, что статус {status} присутствует в истории статусов")
    public void checkStatusInHistory(OrderStatus status) {
        updateStatusHistory();
        for (WebElement row : statusHistory) {
            if (row.getText().equalsIgnoreCase(status.getState())) {
                return;
            }
        }
        Assertions.fail("Order was not in status " + status.getState());
    }

    private OrderStatus getStatusByIndex(int index) {
        updateStatusHistory();
        if (index >= statusHistory.size()) {
            Assertions.fail("There is no row in status history with index " + index);
        }
        return OrderStatus.get(statusHistory.get(index).getText());
    }

    private void updateStatusHistory() {
        statusHistory = driver.findElements(By.xpath("//table[@id = '$xq4t38_table_data']/tbody/tr/td[3]/span"));
    }
}
