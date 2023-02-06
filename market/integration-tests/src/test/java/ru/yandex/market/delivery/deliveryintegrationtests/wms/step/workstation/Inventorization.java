package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.topcontextmenu.TopContextMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.execution.cyclicinventorization.initinventorization.InitCyclicInventorizationPage;

public class Inventorization extends AbstractWSSteps {

    private InitCyclicInventorizationPage initCyclicInventorizationPage;
    private TopContextMenu topContextMenu;

    public Inventorization(WebDriver driver) {
        super(driver);
        initCyclicInventorizationPage = new InitCyclicInventorizationPage(driver);
        topContextMenu = new TopContextMenu(driver);
    }

    @Deprecated
    @Step("Запускаем задание на инвентаризацию ячейки {cell}")
    public void startCellInventorization(String cell) {
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().execution().cyclicInventorization().startCellInventorization();
        initCyclicInventorizationPage.createNewInventorizationTask()
                .inputCellFrom(cell)
                .inputCellTo(cell)
                .save()
                .selectFirstTask();
        topContextMenu.Actions().start();
    }
}
