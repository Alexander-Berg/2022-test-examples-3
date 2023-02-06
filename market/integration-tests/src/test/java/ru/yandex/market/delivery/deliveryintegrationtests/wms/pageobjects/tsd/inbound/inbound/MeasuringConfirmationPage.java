package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;

public class MeasuringConfirmationPage extends AbstractTsdPage {

    public MeasuringConfirmationPage(WebDriver driver) {
        super(driver);
    }

    @Step("Подтверждаем переобмер")
    public void MeasureConfirm() {
        warningDialog.clickOk();
    }
}
