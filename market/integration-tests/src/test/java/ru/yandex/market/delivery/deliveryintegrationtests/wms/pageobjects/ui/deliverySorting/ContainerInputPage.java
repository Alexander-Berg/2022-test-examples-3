package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.deliverySorting;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class ContainerInputPage extends AbstractInputPage {

    public ContainerInputPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches(getUrl()));
    }

    @Override
    protected String getUrl() {
        return "containerInputPage";
    }

    @Step("Сканируем дропку")
    public LocInputPage enterContainer(String container) {
        super.performInput(container);
        return new LocInputPage(driver);
    }
}
