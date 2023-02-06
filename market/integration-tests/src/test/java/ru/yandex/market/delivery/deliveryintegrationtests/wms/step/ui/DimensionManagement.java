package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;

public class DimensionManagement {

    private WebDriver driver;
    private MenuPage menuPage;
    private final NotificationDialog notificationDialog;

    public DimensionManagement(WebDriver driver) {
        this.driver = driver;
        this.menuPage = new MenuPage(driver);
        this.notificationDialog = new NotificationDialog(driver);
    }

    @Step("Обмер одноместного товара на мобильной станции")
    public void measureOneBoxItemInMobileStation(String mobileTable, String uit, String length, String width, String height, String weight) {
        menuPage.inputMeasurePath()
                .enterMobileStation(mobileTable)
                .enterUit(uit)
                .enterVGH(length, width, height, weight)
                .finishMobileMeasure();

    }

    @Step("Обмер многоместного товара на мобильной станции")
    public void measureTwoBoxItemInMobileStation(String mobileTable, String uit1, String uit2,
                                                 String length1, String width1, String height1, String weight1,
                                                 String length2, String width2, String height2, String weight2) {
        menuPage.inputMeasurePath()
                .enterMobileStation(mobileTable)
                .enterUit(uit1)
                .enterVGH(length1, width1, height1, weight1)
                .enterUit(uit2)
                .enterVGH(length2, width2, height2, weight2)
                .finishMobileMeasure();
    }

}
