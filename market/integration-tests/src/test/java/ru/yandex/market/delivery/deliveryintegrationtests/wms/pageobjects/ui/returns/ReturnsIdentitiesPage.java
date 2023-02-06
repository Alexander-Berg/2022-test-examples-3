package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.returns;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

public class ReturnsIdentitiesPage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='text-field']//input")
    private SelenideElement input;
    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forwardButton;
    @FindBy(xpath = "//span[text() = 'Нет Честного ЗНАКА']")
    private SelenideElement noCisButton;

    public ReturnsIdentitiesPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("returnsIdentitiesPage$"));
    }

    @Step("Вводим идентификатор")
    public void enterIdentity(String identity, boolean clickNoCisButtonAfterCisEntering) {
        if (identity == null) {
            clickNoCisButton();
        } else {
            input.sendKeys(identity);
            forwardButton.click();
        }
        if (clickNoCisButtonAfterCisEntering) {
            clickNoCisButton();
        }
    }

    @Step("Нажимаем на кнопку 'Нет Честного Знака'")
    private void clickNoCisButton() {
        noCisButton.click();
    }

}
