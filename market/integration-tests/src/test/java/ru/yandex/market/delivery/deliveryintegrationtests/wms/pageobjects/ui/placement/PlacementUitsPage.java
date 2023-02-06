package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.placement;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import com.codeborne.selenide.SelenideElement;

public class PlacementUitsPage extends AbstractInputPage {

    @FindBy(xpath = "//button[@data-e2e='place-uits']")
    private SelenideElement placeIds;

    public PlacementUitsPage(WebDriver driver) {
        super(driver);
    }

    @Step("Выбираем размещение УИТами")
    public FinishPlacementPage placeIds() {
        placeIds.click();
        return new FinishPlacementPage(driver);
    }

    @Override
    protected String getUrl() {
        return "placementUitsPage";
    }
}
