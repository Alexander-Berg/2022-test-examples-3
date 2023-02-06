package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;

import static com.codeborne.selenide.Condition.visible;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public abstract class AbstractInputPage extends AbstractPage {

    @FindBy(xpath = "//input[@name='text-field']")
    private SelenideElement input;

    public AbstractInputPage(WebDriver driver) {
        super(driver);
        wait.until(ExpectedConditions.and(NotificationDialog.getRemoteErrorBreakerCondition(), urlMatches(getUrl())));
        input.shouldBe(visible);
    }

    protected abstract String getUrl();

    protected void performInput(String text) {
        performInputInActiveElement(input, text);
    }
}
