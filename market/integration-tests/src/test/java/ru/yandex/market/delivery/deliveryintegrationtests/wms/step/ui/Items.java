package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;

import java.util.List;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Items {
    private WebDriver driver;

    public Items(WebDriver driver) {
        this.driver = driver;
    }

    @Step("Сравниваем полученные статусы партии с ожидаемыми: {expectedStatuses}")
    public void verifyLotStatuses(List<String> expectedStatuses, List<String> actualStatuses) {
        assertEquals(expectedStatuses.size(), actualStatuses.size());
        assertTrue(actualStatuses.containsAll(expectedStatuses));
    }
}
