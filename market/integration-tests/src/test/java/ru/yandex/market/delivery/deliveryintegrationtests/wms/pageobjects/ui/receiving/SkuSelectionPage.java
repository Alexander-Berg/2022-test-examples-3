package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import io.qameta.allure.Step;
import static com.codeborne.selenide.Selectors.byXpath;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class SkuSelectionPage extends AbstractPage {

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    public SkuSelectionPage(WebDriver driver) {
        super(driver);
    }

    @Step("Выбираем SKU")
    public void select(String name) {
        final By by = byXpath(String.format("//span[text()='%s']", name));
        $(by).click();
        forward.click();
    }
}
