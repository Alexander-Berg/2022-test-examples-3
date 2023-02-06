package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.precons;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class ContainerConsInputPage extends AbstractInputPage {

    public ContainerConsInputPage(WebDriver driver) {
        super(driver);
    }
    @Step("Вводим тару консолидации")
    public ConsLineInputPage enterConsCart(String containerLabel) {
        super.performInput(containerLabel);
        return new ConsLineInputPage(driver);
    }

    @Override
    protected String getUrl() {
        return "consolidation/containerLabelInputPage($|\\?)";
    }
}
