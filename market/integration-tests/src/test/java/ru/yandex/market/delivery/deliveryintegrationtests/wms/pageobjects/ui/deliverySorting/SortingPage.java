package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.deliverySorting;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class SortingPage extends AbstractInputPage {

    public SortingPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "sortingPage";
    }

    @Step("Сканируем посылку")
    public SortingPage enterParcel(String parcel) {
        super.performInput(parcel);
        return new SortingPage(driver);
    }
}
