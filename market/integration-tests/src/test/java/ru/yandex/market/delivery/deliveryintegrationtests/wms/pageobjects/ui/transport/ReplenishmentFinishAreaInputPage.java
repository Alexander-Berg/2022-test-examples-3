package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport;

import io.qameta.allure.Step;
import static com.codeborne.selenide.Selectors.byXpath;
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

public class ReplenishmentFinishAreaInputPage extends AbstractPage {

    @FindBy(tagName = "input")
    private SelenideElement input;

    @FindBy(xpath = "//button[@data-e2e='button_back']")
    private SelenideElement stop;

    @FindBy(xpath = "//*[contains(text(),'Хотите получить следующее задание?')]")
    private SelenideElement proceedConfirmationDialog;

    public ReplenishmentFinishAreaInputPage(WebDriver driver) {
        super(driver);
        wait.until(ExpectedConditions.and(urlMatches("replenishmentFinishAreaInputPage"),
                        NotificationDialog.getRemoteErrorBreakerCondition()));
    }

    @Step("Проверяем, что показано предложение взять следующее задание")
    public ReplenishmentFinishAreaInputPage verifyProceedConfirmShown() {
        proceedConfirmationDialog.shouldBe(visible);

        return this;
    }

    @Step("Отказываемся от предложения взять следующее задание")
    public TasksWithLocationPage refuseToFetchNextTask(){
        stop.click();
        return new TasksWithLocationPage(driver, "");
    }

    @Step("Подтверждаем ячейку паллетного хранения {loc}")
    public ReplenishmentFinishAreaInputPage inputTargetLocation(String loc) {
        input.sendKeys(loc);
        input.pressEnter();
        return new ReplenishmentFinishAreaInputPage(driver);
    }
}
