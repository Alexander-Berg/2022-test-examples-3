package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation.Inbound;

public class Inventorization {
    private WebDriver driver;
    private static final Logger log = LoggerFactory.getLogger(Inbound.class);
    private MenuPage menuPage;

    public Inventorization(WebDriver driver) {
        this.driver = driver;
        this.menuPage = new MenuPage(driver);
    }

    @Step("Инвентаризируем нетронутую палету")
    public void palletInventorization(String palleteStorageCell, String pickingCart) {
        log.info("Инвентаризируем нетронутую палету");
        menuPage
                .inputInventorizationPath()
                .enterCell(palleteStorageCell)
                .enterPallet(pickingCart)
                .clickNo();
    }

    @Step("Инвентаризируем тронутую палету")
    public void touchedPalletInventorization(String palleteStorageCell, String pickingCart, int qty, int countToMove) {
        log.info("Инвентаризируем тронутую палету");
        menuPage
                .inputInventorizationPath()
                .enterCell(palleteStorageCell)
                .enterTouchedPallet(pickingCart)
                .enterQty(qty - countToMove)
                .clickFinish()
                .clickNo();
    }
}
