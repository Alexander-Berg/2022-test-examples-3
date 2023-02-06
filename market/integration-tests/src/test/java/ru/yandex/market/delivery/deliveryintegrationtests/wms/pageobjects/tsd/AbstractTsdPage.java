package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common.Header;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.common.WarningDialog;

public abstract class AbstractTsdPage extends AbstractTsdElement {

    private final String screenId;

    protected final Header header;
    protected final WarningDialog warningDialog;

    public AbstractTsdPage(WebDriver driver) {
        this(driver, null);
    }

    public AbstractTsdPage(WebDriver driver, String screenId) {
        super(driver);
        this.header = new Header(driver);
        this.warningDialog = new WarningDialog(driver);
        this.screenId = screenId;
    }

    protected void assertScreenIsOpen() {
        String screenIdXpath = String.format("//div[@id = 'screenCodeDIV' and text() = '%s']", screenId);
        driver.findElement(By.xpath(screenIdXpath));
    }
}
