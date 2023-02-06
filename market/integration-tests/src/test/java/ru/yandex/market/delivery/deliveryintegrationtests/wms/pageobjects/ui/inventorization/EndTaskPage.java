package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.inventorization;

import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import com.codeborne.selenide.SelenideElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class EndTaskPage extends AbstractPage {

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

    public EndTaskPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches(getUrl()));
    }

    protected String getUrl() {
        return "endTask";
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

    @Step("Нажимаем кнопку Завершить")
    public EndTaskPage clickFinish() {
        forward.click();
        return this;
    }

    @Step("На вопрос есть ли еще НЗН в ячейке нажимаем нет")
    public EndTaskPage clickNo() {
        ModalWindow modalWindow = new ModalWindow(driver);
        modalWindow.waitModalVisible();
        modalWindow.clickBack();
        return this;
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

