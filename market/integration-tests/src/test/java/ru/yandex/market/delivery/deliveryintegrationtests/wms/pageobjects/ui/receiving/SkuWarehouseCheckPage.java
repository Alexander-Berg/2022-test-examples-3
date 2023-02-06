package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class SkuWarehouseCheckPage extends AbstractPage {

    @FindBy(xpath = "//span[starts-with(.,'Упаковка со следами вскрытия')]")
    private SelenideElement damagedPack;

    @FindBy(xpath = "//span[starts-with(.,'Механические повреждения')]")
    private SelenideElement physicalDamage;

    @FindBy(xpath = "//span[starts-with(.,'Не включается')]")
    private SelenideElement notTurningOn;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    public SkuWarehouseCheckPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("skuWarehouseCheck$"));
    }

    @Step("Выбираем атрибуты несоответствия качества товара")
    public void selectDamageAttributes() {
        damagedPack.click();
        physicalDamage.click();
        notTurningOn.click();
        forward.click();
    }
}
