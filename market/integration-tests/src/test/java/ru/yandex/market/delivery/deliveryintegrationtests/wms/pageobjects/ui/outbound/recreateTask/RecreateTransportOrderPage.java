package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.outbound.recreateTask;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

public class RecreateTransportOrderPage extends AbstractPage {

    @FindBy(xpath = "//span[text() = 'Отбор']")
    private SelenideElement selectionButton;

    @FindBy(xpath = "//span[text() = 'Приёмка']")
    private SelenideElement receivingButton;

    @FindBy(xpath = "//span[text() = 'Пересоздать ТО']")
    private SelenideElement recreateToButton;

    public RecreateTransportOrderPage(WebDriver driver) {
        super(driver);
    }

    @Step("Пересоздаем задание для приемки")
    public TargetConveyorZonePage recreateToForReceiving() {
        receivingButton.click();
        recreateToButton.click();
        return new TargetConveyorZonePage(driver);
    }

}
