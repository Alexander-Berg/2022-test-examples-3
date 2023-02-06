package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.outbound.recreateTask;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

public class TargetConveyorZonePage extends AbstractPage {

    @FindBy(xpath = "//button[@data-e2e='button_EXPENSIVE']")
    private SelenideElement expensiveButton;

    @FindBy(xpath = "//button[@data-e2e='button_FIRST_FLOOR']")
    private SelenideElement firstFloorButton;

    @FindBy(xpath = "//button[@data-e2e='button_OTHER']")
    private SelenideElement otherButton;

    @FindBy(xpath = "//button[@data-e2e='button_SECOND_FLOOR']")
    private SelenideElement secondFloorButton;

    @FindBy(xpath = "//button[@data-e2e='button_THIRD_FLOOR']")
    private SelenideElement thirdFloorButton;

    @FindBy(xpath = "//button[@data-e2e='button_FOURTH_FLOOR']")
    private SelenideElement fourthFloorButton;

    @FindBy(xpath = "//button[@data-e2e='button_FIFTH_FLOOR']")
    private SelenideElement fifthFloorButton;

    public TargetConveyorZonePage(WebDriver driver) {
        super(driver);
    }

    @Step("Выбираем первый этаж")
    public ChooseTask chooseFirstFloorZone() {
        firstFloorButton.click();
        return new ChooseTask(driver);
    }

    @Step("Выбираем второй этаж")
    public ChooseTask chooseSecondFloorZone() {
        secondFloorButton.click();
        return new ChooseTask(driver);
    }

}
