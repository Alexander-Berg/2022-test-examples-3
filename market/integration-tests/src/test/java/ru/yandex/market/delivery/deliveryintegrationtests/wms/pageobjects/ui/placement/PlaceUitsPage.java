package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import com.codeborne.selenide.SelenideElement;

public class PlaceUitsPage extends AbstractInputPage {

    @FindBy(xpath = "//button[@data-e2e='place-uits']")
    private SelenideElement placeIds;

    public PlaceUitsPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим УИТ для размещения")
    public PlaceUitsPage enterId(String id) {
        super.performInput(id);
        return new PlaceUitsPage(driver);
    }

    @Step("Размещаем УИТы")
    public FinishOptimizationPlacement placeIds() {
        placeIds.click();
        return new FinishOptimizationPlacement(driver);
    }

    @Override
    protected String getUrl() {
        return "placeUitsPage";
    }
}
