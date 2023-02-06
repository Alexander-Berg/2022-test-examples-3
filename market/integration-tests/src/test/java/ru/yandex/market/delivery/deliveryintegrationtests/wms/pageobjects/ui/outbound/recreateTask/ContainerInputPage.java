package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.outbound.recreateTask;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class ContainerInputPage extends AbstractInputPage {

    public ContainerInputPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим название контейнера")
    public RecreateTransportOrderPage inputContainer(String totId) {
        super.performInput(totId);
        return new RecreateTransportOrderPage(driver);
    }

    @Override
    protected String getUrl() {
        return "containerInputPage$";
    }

}
