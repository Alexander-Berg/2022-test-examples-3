package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

public class ChooseConveyorZonePage extends AbstractPage {

    @FindBy(xpath = "//span[text() = 'Спецхран']")
    private SelenideElement expensiveButton;

    @FindBy(xpath = "//span[text() = '1 этаж Мезонина']")
    private SelenideElement firstFloorButton;

    @FindBy(xpath = "//span[text() = 'Иное']")
    private SelenideElement otherButton;

    public ChooseConveyorZonePage(WebDriver driver) {
        super(driver);
    }

    public void clickExpensiveZone() {
        expensiveButton.click();
    }
    public void clickFirstFloorZone() {
        firstFloorButton.click();
    }
    public void clickOtherZone() {
        otherButton.click();
    }
}
