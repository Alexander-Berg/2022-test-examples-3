package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import com.codeborne.selenide.SelenideElement;

public class PlaceIdPage extends AbstractInputPage {

    @FindBy(xpath = "//button[@data-e2e='place-by-id']")
    private SelenideElement placeById;

    public PlaceIdPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим НЗН для размещения")
    public FinishOptimizationPlacement enterCart(String containerLabel) {
        super.performInput(containerLabel);
        return new FinishOptimizationPlacement(driver);
    }

    @Step("Выбираем размещение УИТами")
    public PlaceUitsPage choosePlaceByUIT() {
        placeById.click();
        return new PlaceUitsPage(driver);
    }

    @Step("Вводим УИТ для размещения")
    public PlacementUitsPage enterId(String id) {
        super.performInput(id);
        return new PlacementUitsPage(driver);
    }

    @Override
    protected String getUrl() {
        return "placeIdPage$";
    }
}
