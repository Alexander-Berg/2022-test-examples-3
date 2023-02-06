package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.outgoing.packing;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;

class TableSelectDlg extends AbstractWsPage {

    public TableSelectDlg(WebDriver driver) {
        super(driver);
    }

    public void openSortTable(String sortTable) {
        String sortTableXpath = String.format("//div[@class = 'select-table-dialog']//label[text() = '%s']",
                sortTable
        );
        driver.findElement(By.xpath(sortTableXpath)).click();
        overlayBusy.waitUntilHidden();
    }
}
