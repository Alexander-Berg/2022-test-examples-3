package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class SkuWarehouseDamagePage extends AbstractPage {

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement notDamaged;

    @FindBy(xpath = "//button[@data-e2e='yes-button']")
    private SelenideElement damaged;

    public SkuWarehouseDamagePage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("skuWarehouseDamage$"));
    }

    @Step("Выбираем, что на товаре нет повреждений")
    public void selectNotDamaged() {
        notDamaged.click();
    }

    @Step("Выбираем, что на товаре есть повреждения")
    public void selectDamaged() {
        damaged.click();
    }
}
