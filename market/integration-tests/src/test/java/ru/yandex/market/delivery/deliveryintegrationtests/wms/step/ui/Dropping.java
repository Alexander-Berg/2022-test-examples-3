package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;


import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;

public class Dropping {
    private final WebDriver driver;
    private final MenuPage menuPage;

    public Dropping(WebDriver driver) {
        this.driver = driver;
        this.menuPage = new MenuPage(driver);
    }

    @Step("Привязываем дропку {dropId} к ячейке {loc}")
    public void assignDropWithLoc(String dropId, String loc) {
        menuPage
                .sortingOrderPath()
                .assignDrop()
                .enterDropId(dropId)
                .enterCell(loc);
    }
}
