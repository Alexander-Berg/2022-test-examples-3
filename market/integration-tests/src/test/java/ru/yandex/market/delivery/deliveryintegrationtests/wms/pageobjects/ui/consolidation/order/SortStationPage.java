package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.consolidation.order;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class SortStationPage extends AbstractInputPage {

    public SortStationPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "sortStation";
    }

    @Step("Сканируем сортировочную станцию")
    public ContainerPage enterSortStation(String sortStation) {
        super.performInput(sortStation);
        return new ContainerPage(driver);
    }
}
