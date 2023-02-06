package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.complectation;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;

public class ComplectationCompletePage extends AbstractTsdPage {

    public ComplectationCompletePage(WebDriver driver) {
        super(driver);
    }

    @Step("Проверяем, что мы на Отборы заверш.")
    public boolean isDisplayed() {
        return driver.findElements(By.xpath("//label[@id = 'PCM_hdr_A' and text() = 'Отборы заверш.']")).size() != 0;
    }
}
