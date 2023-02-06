package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.HtmlElementsCommon;
import ru.yandex.qatools.htmlelements.loader.decorator.HtmlElementDecorator;
import ru.yandex.qatools.htmlelements.loader.decorator.HtmlElementLocatorFactory;

public abstract class AbstractTsdElement extends HtmlElementsCommon {

    private final String overlayXpath = "//div[@class = 'overlay busy is-hidden' or @class = 'overlay busy']";
    private final String spinnerXpath = "//*[@id = 'inforLoadingOverlay' " +
            "or @id = 'inforOverlay' or @class = 'loadingText']";

    public AbstractTsdElement(WebDriver driver) {
        super(driver);

        PageFactory.initElements(new HtmlElementDecorator(new HtmlElementLocatorFactory(driver)), this);
    }

    public void waitOverlayHidden() {
        waitElementHidden(By.xpath(overlayXpath), false);
    }

    public void waitOverlayHiddenIfPresent() {
        waitElementHidden(By.xpath(overlayXpath), true);
    }

    public void waitSpinner() {
        waitElementHidden(By.xpath(spinnerXpath), false);
    }

    public void waitSpinnerIfPresent() {
        waitElementHidden(By.xpath(spinnerXpath), true);
    }
}
