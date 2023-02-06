package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class MismatchItemPage extends AbstractPage {

    @FindBy(xpath = "//button[@data-e2e='incorrect_description_button']")
    private SelenideElement incorrectDescriptionButton;

    @FindBy(xpath = "//button[@data-e2e='missing_item_button']")
    private SelenideElement missingItem_buttonButton;

    @FindBy(xpath = "//button[@data-e2e='missing_part_button']")
    private SelenideElement missingPartButton;

    @FindBy(xpath = "//button[@data-e2e='substitution_button']")
    private SelenideElement substitutionButton;

    public MismatchItemPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("mismatchItem$"));
    }

    @Step("Нажимаем кнопку: Несоответствующие описание")
    public CartInputPage clickIncorrectDescription(){
        incorrectDescriptionButton.click();
        return new CartInputPage(driver);
    }

}
