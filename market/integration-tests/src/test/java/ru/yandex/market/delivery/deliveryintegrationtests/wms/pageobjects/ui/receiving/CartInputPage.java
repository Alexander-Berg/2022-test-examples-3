package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.visible;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

@Slf4j
public class CartInputPage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='text-field']//input")
    private SelenideElement input;

    @FindBy(xpath = "//span[@data-e2e='uit']")
    private SelenideElement instanceElement;

    @FindBy(xpath = "//*[contains(text(),'АНОМАЛИЯ')]")
    private SelenideElement anomalyText;

    public CartInputPage(WebDriver driver) {
        super(driver);
        wait.until(ExpectedConditions
                .and(urlMatches("cartInput"), NotificationDialog.getRemoteErrorBreakerCondition()));
    }

    @Step("Вводим тару: {cart}")
    public BarcodeInputPage enterCart(String cart) {
        input.sendKeys(cart);
        input.pressEnter();
        return new BarcodeInputPage(driver);
    }

    @Step("Вводим флипбокс: {flipboxId}")
    public CartParentInputPage enterFlipBox(String flipboxId) {
        input.sendKeys(flipboxId);
        input.pressEnter();
        return new CartParentInputPage(driver);
    }

    public String getInstance() {
        final String instance = instanceElement.getText().substring(0, 12);
        assertTrue(StringUtils.isNotBlank(instance), "Instance id should not be blank");
        assertTrue(StringUtils.isNumeric(instance), "Instance id should be numeric");
        log.info("Instance id {}", instance);
        return instance;
    }

    public CartInputPage verifyAnomaly() {
        anomalyText.shouldBe(visible);

        return this;
    }
}
