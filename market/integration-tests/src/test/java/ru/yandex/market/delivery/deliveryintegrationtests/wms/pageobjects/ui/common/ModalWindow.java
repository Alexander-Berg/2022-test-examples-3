package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common;

import com.codeborne.selenide.SelenideElement;
import static com.codeborne.selenide.Selectors.byXpath;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.HtmlElementsCommon;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;
import ru.yandex.qatools.htmlelements.annotations.Name;

import java.util.concurrent.TimeUnit;

import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.page;

public class ModalWindow extends HtmlElementsCommon {

    @FindBy(xpath = "//div[@role='dialog']")
    @CacheLookup
    private SelenideElement modal;
    @FindBy(xpath = "//div[@role='dialog']//button[@data-e2e='button_forward']")
    private SelenideElement forwardButton;
    @FindBy(xpath = "//div[@role='dialog']//button[@data-e2e='button_back']")
    private SelenideElement backButton;
    @FindBy(xpath = "//div[@role='dialog']//button[@data-e2e='table_modal_submit']")
    private SelenideElement submitButton;
    @FindBy(xpath = "//div[@role='dialog']//button[@data-e2e='table_modal_cancel']")
    private SelenideElement cancelButton;
    @FindBy(xpath = "//input[@type='text']")
    private SelenideElement input;
    @Name("Чекбокс первого в результатах поиска")
    @FindBy(xpath = "//div[@role='dialog']//input[@data-e2e='checkbox_row_0']")
    private SelenideElement firstCheckbox;

    public ModalWindow(WebDriver driver) {
        super(driver);
        page(this);
    }

    public void clickForward() {
        forwardButton.click();
    }

    @Step("Выбираем первый элемент в таблице")
    public ModalWindow selectFirstResult() {
        firstCheckbox.click();
        return this;
    }

    public void clickBack() {
        backButton.click();
    }

    public void clickSubmit() {
        submitButton.click();
    }

    public void clickCancel() {
        cancelButton.click();
    }

    public void waitModalVisible() {
        modal.shouldBe(visible);
    }

    public void waitModalHidden() {
        modal.shouldBe(hidden);
    }

    public void waitModalHiddenWithText(String text) {
        final By by = byXpath(String.format("//div[@role='dialog']//*[contains(text(), '%s')]", text));
        waitElementHidden(by, true);
    }

    public boolean isPresent(String message) {
        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.MEDIUM_WAIT_TIMEOUT, TimeUnit.SECONDS);

        String searchString = "//*[text()[contains(.,'message_text_placeholder')]]"
                .replaceFirst("message_text_placeholder", message);
        boolean result = $$(byXpath(searchString))
                .size() != 0;

        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);
        return result;
    }

    public void performInputInActiveElement(String text) {
        input.sendKeys(text);
        clickForward();
    }
}
