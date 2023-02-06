package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport;

import java.util.concurrent.TimeUnit;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;

import static com.codeborne.selenide.Condition.visible;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class ReplenishmentPickingSerialNumberApieceInputPage extends AbstractPage {

    @FindBy(tagName = "input")
    private SelenideElement input;

    @FindBy(xpath = "//button/span[contains(text(), 'Отобрать всю')]/..")
    private SelenideElement buttonPickWholePallete;

    @FindBy(xpath = "//button[@data-e2e='button_back']")
    private SelenideElement stop;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement go;

    @FindBy(xpath = "//*[contains(text(),'Хотите получить следующее задание?')]")
    private SelenideElement proceedConfirmationDialog;

    @FindBy(xpath = "//*[contains(text(),'Вы уверены, что хотите отобрать палету целиком?')]")
    private SelenideElement pickedAllItemsConfirmationDialog;

    @FindBy(xpath = "//span[contains(text(), 'Отобрано')]")
    private SelenideElement pickedQtyBlock;


    public ReplenishmentPickingSerialNumberApieceInputPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("replenishmentPickingSerialNumberApieceInputPage"));
    }

    @Step("Указываем отбираемый УИТ {sn}")
    public ReplenishmentPickingSerialNumberApieceInputPage inputSerialNumber(String sn) {
        int pickedQtyBefore = getPickedQty();
        input.sendKeys(sn);
        input.pressEnter();
        int pickedQtyAfter = getPickedQty();
        Retrier.retry(() -> Assertions.assertEquals(pickedQtyAfter, pickedQtyBefore + 1),
                Retrier.RETRIES_SMALL, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);
        return new ReplenishmentPickingSerialNumberApieceInputPage(driver);
    }

    @Step("Проверяем, что появилось предложение взять следующее задание")
    public ReplenishmentPickingSerialNumberApieceInputPage verifyProceedConfirmShown() {
        proceedConfirmationDialog.shouldBe(visible);

        return this;
    }

    @Step("Проверяем, что появился запрос подтверждения отбора палеты целиком")
    public ReplenishmentPickingSerialNumberApieceInputPage verifyPickAllConfirmShown() {
        pickedAllItemsConfirmationDialog.shouldBe(visible);
        return this;
    }

    @Step("Отказываемся от предложения взять следующее задание")
    public TasksWithLocationPage refuseToFetchNextTask(){
        stop.click();
        return new TasksWithLocationPage(driver, "");
    }

    @Step("Подтверждаем отбор всей палеты")
    public TasksWithLocationPage confirmPickingAll(){
        go.click();
        return new TasksWithLocationPage(driver, "");
    }

    @Step("Жмём 'Отобрать всю паллету'")
    public ReplenishmentPickingSerialNumberApieceInputPage pickWholePallete(){
        buttonPickWholePallete.click();
        return this;
    }

    @Step("Получаем количество отобранных уитов")
    public int getPickedQty() {
        return Integer.parseInt(
                StringUtils.substringBetween(pickedQtyBlock.getText(), "Отобрано:", "/").trim());
    }
}
