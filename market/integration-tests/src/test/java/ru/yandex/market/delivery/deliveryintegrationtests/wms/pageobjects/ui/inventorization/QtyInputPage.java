package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.inventorization;

import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
import com.codeborne.selenide.SelenideElement;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QtyInputPage extends AbstractInputPage {

    @FindBy(xpath = "//span[@data-e2e='loc']")
    private SelenideElement locElement;
    @FindBy(xpath = "//span[@data-e2e='id']")
    private SelenideElement idElement;
    @FindBy(xpath = "//span[@data-e2e='lot']")
    private SelenideElement lotElement;
    @FindBy(xpath = "//span[@data-e2e='description']")
    private SelenideElement descriptionElement;
    @FindBy(xpath = "//span[@data-e2e='sku-counter']")
    private SelenideElement skuCounterElement;
    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    public QtyInputPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "qtyInput";
    }

    @Step("Вводим количество")
    public EndTaskPage enterQty(int qty) {
        super.performInput(String.valueOf(qty));
        return new EndTaskPage(driver);
    }

    @Step("Проверяем локацию")
    public void checkLoc(String loc) {
        assertEquals(locElement.getText(), loc);
    }

    @Step("Проверяем тару")
    public void checkContainer(String container) {
        assertEquals(idElement.getText(), container);
    }

    @Step("Проверяем партию")
    public void checkLot(String lot) {
        assertEquals(lotElement.getText(), lot);
    }

    @Step("Проверяем описание")
    public void checkDescription(String description) {
        assertEquals(descriptionElement.getText(), description);
    }

    @Step("Проверяем количество уже инвентаризированных")
    public void checkCurrentQty(int currentQty) {
        assertEquals(getCurrentItemsCount(), currentQty);
    }

    @Step("Проверяем количество")
    public void checkRemainQty(int remainQty) {
        assertEquals(getInitialItemsCount(), remainQty);
    }


    private int getInitialItemsCount() {
        return Integer.parseInt(StringUtils.substringAfter(skuCounterElement.getText(), "из").trim());
    }

    private int getCurrentItemsCount() {
        return Integer.parseInt(
                StringUtils.substringBefore(StringUtils.substringAfter(skuCounterElement.getText(), "SKU").trim(), "из")
                        .trim());
    }
}

