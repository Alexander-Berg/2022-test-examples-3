package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.outgoing.packing.PackingPage;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Resource;

@Resource.Classpath({"wms/infor.properties"})
public class Packing extends AbstractWSSteps {
    private static final Logger log = LoggerFactory.getLogger(Packing.class);

    private final PackingPage packingPage;


    public Packing(WebDriver drvr) {
        super(drvr);

        packingPage = new PackingPage(driver);
    }

    @Step("Пакуем товары")
    public void pack(String itemSerial, String packingTable) {
        log.info("Packing");

        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().outgoing().packing();

        packingPage.openSortTable(packingTable);
        packingPage.enterSerial(itemSerial);
        packingPage.enterSuggestedPackType();
    }

}
