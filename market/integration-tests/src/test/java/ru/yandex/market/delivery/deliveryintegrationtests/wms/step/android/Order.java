package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.android;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.Android.picking.AreaInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;

import java.util.ArrayList;
import java.util.List;

import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

public class Order {

    private MenuPage menuPage = new MenuPage(getWebDriver());

    public List<String> pickMultiAssignment(String areaKey, String cart) {
        List<String> serialNumbers = new ArrayList<>();
        menuPage.inputAndroidMultipickingPath()
                .inputArea(areaKey)
                .addCart(cart)
                .verifyCartWasAdded(cart)
                .clickForwardButton()
                .enterUit(serialNumbers)
                .enterCart(cart)
                .refuseNextTask();

        return serialNumbers;
    }
}
