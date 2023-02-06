package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.gridmenu;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;

public class GridMenu extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(GridMenu.class);

    public GridMenu(WebDriver driver) {
        super(driver);
    }
}
