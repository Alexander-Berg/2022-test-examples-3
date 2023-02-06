package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.topmenu;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.SeleniumUtil;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class WHSelector extends AbstractWsPage {

    @FindBy(xpath = "//div[text() = 'INFOR_ENTERPRISE']")
    public HtmlElement inforEnterprise;

    @FindBy(xpath = "//div[@id = '$eakrcq']")
    public HtmlElement warehouse;

    public WHSelector(WebDriver driver) {
        super(driver);
    }

    public void openEnterprise() {
        SeleniumUtil.jsClick(inforEnterprise, driver);
        overlayBusy.waitUntilHidden();
    }

    public void openWarehouse() {
        SeleniumUtil.jsClick(warehouse, driver);
        overlayBusy.waitUntilHidden();
    }
}
