package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.HtmlElementsCommon;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.common.OverlayBusy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;
import ru.yandex.qatools.htmlelements.loader.decorator.HtmlElementDecorator;
import ru.yandex.qatools.htmlelements.loader.decorator.HtmlElementLocatorFactory;

public abstract class AbstractWsPage extends HtmlElementsCommon {
    protected OverlayBusy overlayBusy;
    protected JavascriptExecutor jsDriver;

    @Name("Счетчик результатов")
    @FindBy(xpath = "//table//td[contains(text(), ' из ')]")
    private HtmlElement resulstCounter;

    public AbstractWsPage(WebDriver driver) {
        super(driver);
        this.overlayBusy = new OverlayBusy(driver);
        this.jsDriver = (JavascriptExecutor)driver;

        PageFactory.initElements(new HtmlElementDecorator(new HtmlElementLocatorFactory(driver)), this);
    }

    public WebElement scrollLeft(WebElement scrollArea) {
        jsDriver.executeScript("arguments[0].scrollLeft = 1000", scrollArea);
        return scrollArea;
    }

    protected void inputFilterField(HtmlElement field, String text) {

        for (int i = 0; i < 5; i++) {
            field.click();
            overlayBusy.waitUntilHidden();
            if (field.equals(driver.switchTo().activeElement())) break;
        }

        field.sendKeys(text);
        overlayBusy.waitUntilHidden();

        Assertions.assertTrue(StringUtils
                .containsIgnoreCase(field.getAttribute("value"), text),
                field.getAttribute("value") + " != " + text);
    }

    public int resultsOnPage() {
        String sep = "-";
        String counterText = resulstCounter.getText()
                // NOTE: There are two different separators: "-" and "–".
                .replace("–", sep);
        int firstIndex = Integer.valueOf(StringUtils.substringBefore(counterText, sep));
        int lastIndex = Integer.valueOf(StringUtils.substringBetween(counterText, sep, " "));
        if (lastIndex == 0) {
            return 0;
        }
        return lastIndex - firstIndex + 1;
    }

    private boolean isLastPage() {
        String counterText = resulstCounter.getText();

        int lastIndex = Integer.valueOf(StringUtils.substringBetween(counterText, "–", " "));

        int totalIndex = Integer.valueOf(
                StringUtils.substringAfter(StringUtils.deleteWhitespace(counterText), "из")
        );

        return totalIndex == lastIndex;
    }
}
