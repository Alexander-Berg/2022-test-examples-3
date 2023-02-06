package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.dimensionControl;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class StationPage extends AbstractInputPage {

    public StationPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим название мобильной станции обмера ВГХ - {mobileTable}")
    public UitScanPage enterMobileStation(String mobileTable) {
        super.performInput(mobileTable);
        return new UitScanPage(driver);
    }

    @Override
    protected String getUrl() {
        return "stationPage$";
    }
}
