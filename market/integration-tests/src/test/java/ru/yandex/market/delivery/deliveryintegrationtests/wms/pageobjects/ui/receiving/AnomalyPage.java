package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.logistic.api.model.common.NonconformityType;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

public class AnomalyPage extends AbstractPage {

    private static final String url = "/ui/inbound/receiving/mismatchItem";

    @FindBy(xpath = "//button[@data-e2e='defective_button']")
    private SelenideElement defectButton;

    @FindBy(xpath = "//button[@data-e2e='incorrect_description_button']")
    private SelenideElement wrongDescriptionButton;

    @FindBy(xpath = "//button[@data-e2e='missing_sticker_button']")
    private SelenideElement absentRussianDescriptionButton;

    @FindBy(xpath = "//button[@data-e2e='missing_part_button']")
    private SelenideElement incompleteSetButton;

    @FindBy(xpath = "//button[@data-e2e='temperature_regime_button']")
    private SelenideElement noStorageConditionsButton;

    @FindBy(xpath = "//button[@data-e2e='missing_product_date_button']")
    private SelenideElement noExpirationDateButton;

    public AnomalyPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("mismatchItem$"));
    }

    public CartInputPage selectAnomalyType(NonconformityType anomalyType) {
        return switch (anomalyType) {
            case MISMATCHING_DESCRIPTION -> clickOnWrongDescriptionButton();
            case NO_RUSSIAN_INFO -> clickOnAbsentRussianDescriptionButton();
            case PART_MISSING -> clickOnIncompleteSetButton();
            case NO_TEMPERATURE_REGIME -> clickOnNoStorageConditionsButton();
            case NO_LIFE_TIME -> clickOnNoExpirationDateButton();
            default -> clickOnDefect();
        };
    }

    public CartInputPage clickOnDefect() {
        defectButton.click();
        return new CartInputPage(driver);
    }

    public CartInputPage clickOnWrongDescriptionButton() {
        wrongDescriptionButton.click();
        return new CartInputPage(driver);
    }

    public CartInputPage clickOnAbsentRussianDescriptionButton() {
        absentRussianDescriptionButton.click();
        return new CartInputPage(driver);
    }

    public CartInputPage clickOnIncompleteSetButton() {
        incompleteSetButton.click();
        return new CartInputPage(driver);
    }

    public CartInputPage clickOnNoStorageConditionsButton() {
        noStorageConditionsButton.click();
        return new CartInputPage(driver);
    }

    public CartInputPage clickOnNoExpirationDateButton() {
        noExpirationDateButton.click();
        return new CartInputPage(driver);
    }
}
