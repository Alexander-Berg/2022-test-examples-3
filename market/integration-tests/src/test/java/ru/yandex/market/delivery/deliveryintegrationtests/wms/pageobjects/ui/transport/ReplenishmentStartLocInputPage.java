package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

public class ReplenishmentStartLocInputPage extends AbstractPage {

    @FindBy(tagName = "input")
    private SelenideElement input;

    public ReplenishmentStartLocInputPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("replenishmentStartLocInputPage"));
    }

    @Step("Подтверждаем ячейку {loc}")
    public ReplenishmentContainerIdInputPage inputLocation(String loc) {
        input.sendKeys(loc);
        input.pressEnter();
        return new ReplenishmentContainerIdInputPage(driver);
    }
}
