package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.consolidation.order;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class ContainerPage extends AbstractInputPage {

    public ContainerPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "containerPutwallFlow";
    }

    @Step("Сканируем тару отбора")
    public UitPage enterContainer(String container) {
        super.performInput(container);
        return new UitPage(driver);
    }
}
