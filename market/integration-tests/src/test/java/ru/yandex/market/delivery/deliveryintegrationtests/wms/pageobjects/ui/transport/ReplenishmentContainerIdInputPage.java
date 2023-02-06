package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

public class ReplenishmentContainerIdInputPage extends AbstractPage {

    @FindBy(tagName = "input")
    private SelenideElement input;

    public ReplenishmentContainerIdInputPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("replenishmentContainerIdInputPage"));
    }

    @Step("Подтверждаем Контейнер/НЗН/ID {id}")
    public ReplenishmentFinishAreaInputPage inputContainerIdForMove(String id) {
        input.sendKeys(id);
        input.pressEnter();
        return new ReplenishmentFinishAreaInputPage(driver);
    }


    @Step("Подтверждаем Контейнер/НЗН/ID {id}")
    public ReplenishmentPickingMoveContainerInputPage inputContainerIdForPick(String id) {
        input.sendKeys(id);
        input.pressEnter();
        return new ReplenishmentPickingMoveContainerInputPage(driver);
    }
}
