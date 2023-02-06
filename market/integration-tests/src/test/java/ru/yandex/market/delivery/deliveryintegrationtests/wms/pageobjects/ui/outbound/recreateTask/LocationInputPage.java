package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.outbound.recreateTask;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class LocationInputPage extends AbstractInputPage {

    public LocationInputPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим название стола в НОК для пересоздания транспортного ордера")
    public ContainerInputPage inputTable(String inboundTable) {
        super.performInput(inboundTable);
        return new ContainerInputPage(driver);
    }

    @Step("Вводим название буферной ячейки мезонина для пересоздания транспортного ордера")
    public ContainerInputPage inputBufferLoc(String placementBufForFirstFloor) {
        super.performInput(placementBufForFirstFloor);
        return new ContainerInputPage(driver);
    }

    @Override
    protected String getUrl() {
        return "location$";
    }

}
