package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.returns;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;

import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class SkuCheckPage extends AbstractPage {

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    private static final String QUALITY_ATTRIBUTE_XPATH_TEMPLATE = "//button[@data-e2e='%s']";

    public SkuCheckPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("skuCheckPage$"));
    }

    @Step("Выбираем: Упаковка со следами вскрытия")
    public void clickSevereDamage() {
        // Упаковка со следами вскрытия
        $(byXpath(String.format(QUALITY_ATTRIBUTE_XPATH_TEMPLATE, "71")))
                .click();
    }

    @Step("Выбираем: Для теста")
    public void clickDeformation() {
        //Для теста (Утилизация)
        $(byXpath(String.format(QUALITY_ATTRIBUTE_XPATH_TEMPLATE, "154")))
                .click();
    }

    @Step("Подтверждаем")
    public void confirm() {
        forward.click();
    }
}
