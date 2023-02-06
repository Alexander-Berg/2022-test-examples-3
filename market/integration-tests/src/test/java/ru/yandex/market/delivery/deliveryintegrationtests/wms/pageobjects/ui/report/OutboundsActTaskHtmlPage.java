package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.report;

import static com.codeborne.selenide.Selectors.byXpath;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlContains;

public class OutboundsActTaskHtmlPage extends AbstractPage {

    public OutboundsActTaskHtmlPage(WebDriver driver) {
        super(driver);
        wait.until(urlContains(getUrl()));
    }

    protected String getUrl() {
        return "/download/HTML";
    }

    public void verifyApplicationNumber(String applicationNumber) {
        final By applicationNumberTitle = byXpath(String.format("//span[contains(text(), '%s')]", applicationNumber));
        $(applicationNumberTitle).shouldBe(visible);
    }
}
