package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

public class ReplenishmentPickingMoveContainerInputPage extends AbstractPage {

    @FindBy(tagName = "input")
    private SelenideElement input;

    public ReplenishmentPickingMoveContainerInputPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("replenishmentPickingMoveContainerInputPage"));
    }

    @Step("Сканируем Контейнер/НЗН/ID {id}, в который начнём отбирать")
    public ReplenishmentPickingSerialNumberApieceInputPage inputTargetContainerId(String id) {
        input.sendKeys(id);
        input.pressEnter();
        return new ReplenishmentPickingSerialNumberApieceInputPage(driver);
    }

}
