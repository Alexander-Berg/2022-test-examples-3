package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport;

import io.qameta.allure.Step;
import static com.codeborne.selenide.Selectors.byXpath;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NestedIdsMovementsPickingItemsPage extends AbstractInputPage {

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forwardButton;

    public NestedIdsMovementsPickingItemsPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "pickingItemsPage$";
    }

    @Step("Вводим флипбокс для перемещения в пустую тару")
    public NestedIdsMovementsScanParentIdPage enterFlip(String flipboxId) {
        super.performInput(flipboxId);
        checkFlip(flipboxId);
        forwardButton.click();
        return new NestedIdsMovementsScanParentIdPage(driver);
    }

    @Step("Проверяем наличие в списке отсканированного флипбокса")
    public NestedIdsMovementsPickingItemsPage checkFlip(String flipboxId) {
        final By flipBox = byXpath(String.format("//span[text() = '%s']", flipboxId));
        $(flipBox).shouldBe(visible);

        return this;
    }

}
