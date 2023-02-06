package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import com.codeborne.selenide.SelenideElement;

public class PlacementIdPage extends AbstractInputPage {

    @FindBy(xpath = "//button[@data-e2e='place-by-id']")
    private SelenideElement placeById;

    public PlacementIdPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим НЗН для размещения")
    public FinishPlacementPage enterCart(String containerLabel) {
        super.performInput(containerLabel);
        return new FinishPlacementPage(driver);
    }

    @Step("Выбираем размещение УИТами")
    public PlacementIdPage choosePlaceById() {
        placeById.click();
        return this;
    }

    @Step("Вводим УИТ для размещения")
    public PlacementUitsPage enterId(String id) {
        super.performInput(id);
        return new PlacementUitsPage(driver);
    }

    @Override
    protected String getUrl() {
        return "placementIdPage$";
    }
}
