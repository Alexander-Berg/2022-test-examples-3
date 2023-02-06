package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.common.OverlayBusy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.common.PopupAlert;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.LeftMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.topcontextmenu.TopContextMenu;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.topmenu.TopMenu;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.PropertyLoader;

public class AbstractWSSteps {
    protected static final Logger log = LoggerFactory.getLogger(Inbound.class);

    protected WebDriver driver;
    protected TopMenu topMenu;
    protected LeftMenu leftMenu;
    protected TopContextMenu topContextMenu;
    protected PopupAlert popupAlert;
    protected OverlayBusy overlayBusy;

    public AbstractWSSteps(WebDriver drvr) {
        PropertyLoader.newInstance().populate(this);

        this.driver = drvr;
        topMenu = new TopMenu(driver);
        leftMenu = new LeftMenu(driver);
        topContextMenu = new TopContextMenu(driver);
        popupAlert = new PopupAlert(driver);
        overlayBusy = new OverlayBusy(driver);
    }
}
