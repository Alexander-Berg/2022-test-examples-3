package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.packing;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class ContainerPackingPage extends AbstractInputPage {

    public ContainerPackingPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводит НЗН {container}")
    public PackingPage enterContainer(String container) {
        super.performInput(container);
        return new PackingPage(driver);
    }

    @Override
    protected String getUrl() {
        return "containerInputPage";
    }
}
