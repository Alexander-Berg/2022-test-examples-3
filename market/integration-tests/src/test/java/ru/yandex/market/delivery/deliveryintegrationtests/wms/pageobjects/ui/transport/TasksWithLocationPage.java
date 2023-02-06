package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport;

import io.qameta.allure.Step;

import static com.codeborne.selenide.Selectors.byXpath;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class TasksWithLocationPage extends AbstractPage {

    @FindBy(xpath = "//option[contains(text(), 'Любое задание')]")
    private SelenideElement anyTask;

    @FindBy(xpath = "//option[contains(text(), 'Размещение аномалий')]")
    private SelenideElement anomalyPlacement;

    @FindBy(xpath = "//option[contains(text(), 'Консолидация аномалий')]")
    private SelenideElement anomalyConsolidation;

    @FindBy(xpath = "//option[@value='REPLENISHMENT_MOVE']")
    private SelenideElement optReplenishmentTurnoverMove;
    @FindBy(xpath = "//option[@value='REPLENISHMENT_PICK']")
    private SelenideElement optReplenishmentTurnoverPick;
    @FindBy(xpath = "//option[@value='REPLENISHMENT_ORDER_MOVE']")
    private SelenideElement optReplenishmentOrderMove;

    @FindBy(xpath = "//option[@value='REPLENISHMENT_ORDER_PICK']")
    private SelenideElement optReplenishmentOrderPick;
    @FindBy(xpath = "//option[@value='REPLENISHMENT_WITHDRAWAL_MOVE']")
    private SelenideElement optReplenishmentWithdrawalMove;

    @FindBy(xpath = "//option[@value='REPLENISHMENT_WITHDRAWAL_PICK']")
    private SelenideElement optReplenishmentWithdrawalPick;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    @FindBy(xpath = "(//select[@data-e2e='duration-selector'])[1]")
    private SelenideElement areaSelect;

    // TODO: убрать, как только добавят свойство e2e для moveContainerContentPage
    private final String consolidationContainer;

    public TasksWithLocationPage(WebDriver driver, String consolidationContainer) {
        super(driver);
        this.consolidationContainer = consolidationContainer;
        wait.until(urlMatches("tasksWithLocationPage"));
    }

    @Step("Получаем задание на размещение аномалий")
    public RelocationMultiSourcePage getAnomalyPlacementTask(String areaKey) {
        chooseAreaForTask(areaKey);
        anyTask.click();
        anomalyPlacement.click();
        forward.click();
        return new RelocationMultiSourcePage(driver);
    }

    @Step("Выбираем участок, на котором хотим взть задание")
    public TasksWithLocationPage chooseAreaForTask(String areaKey) {
        By areaOption = byXpath(String.format("//option[contains(text(), '%s')]", areaKey));
        areaSelect.click();
        $(areaOption).click();
        return this;
    }

    @Step("Получаем задание на консолидацию аномалий")
    public CurrentContainerPage getAnomalyConsolidationTask(String areaKey) {
        chooseAreaForTask(areaKey);
        anyTask.click();
        anomalyConsolidation.click();
        forward.click();
        return new CurrentContainerPage(driver, consolidationContainer);
    }

    @Step("Размещаем аномальную тару")
    public TasksWithLocationPage placeAnomaly(String cartId, String anomalyPlacementLoc, String areaKey) {
        return getAnomalyPlacementTask(areaKey)
                .inputContainer(cartId)
                .placeContainer(cartId, anomalyPlacementLoc);
    }

    @Step("Консолидируем аномальную тару")
    public TasksWithLocationPage consolidateAnomaly(String cartId,
                                                    String containerId,
                                                    String areaKey) {
        return getAnomalyConsolidationTask(areaKey)
                .inputAnomalyContainer(containerId)
                .moveContainerContentToLoc(cartId)
                .consolidateIntoBuffZone();
    }

    @Step("Берём задание на перемещение для пополнения (спуска паллеты) на участке {atAreaCode}")
    public ReplenishmentStartLocInputPage getReplenishmentMoveTask(String atAreaCode) {
        clickAreaOptionByValue(atAreaCode);
        optReplenishmentTurnoverMove.click();
        forward.click();
        return new ReplenishmentStartLocInputPage(driver);
    }

    @Step("Берём задание на перемещение для пополнения под изъятие (спуска паллеты) на участке {atAreaCode}")
    public ReplenishmentStartLocInputPage getReplenishmentWithdrawalMoveTask(String atAreaCode) {
        clickAreaOptionByValue(atAreaCode);
        optReplenishmentWithdrawalMove.click();
        forward.click();
        return new ReplenishmentStartLocInputPage(driver);
    }


    @Step("Берём задание на отбор для пополнения (из паллеты) на участке {atAreaCode}")
    public ReplenishmentStartLocInputPage getReplenishmentPickTask(String atAreaCode) {
        clickAreaOptionByValue(atAreaCode);
        optReplenishmentTurnoverPick.click();
        forward.click();
        return new ReplenishmentStartLocInputPage(driver);
    }

    @Step("Берём задание на отбор для пополнения под изъятие (из паллеты) на участке {atAreaCode}")
    public ReplenishmentStartLocInputPage getReplenishmentWithdrawalPickTask(String atAreaCode) {
        clickAreaOptionByValue(atAreaCode);
        optReplenishmentWithdrawalPick.click();
        forward.click();
        return new ReplenishmentStartLocInputPage(driver);
    }

    @Step("Проверяем что не осталось заданий на перемещение")
    public void validateNoMoveWithdrawalTask(String area) {
        clickAreaOptionByValue(area);
        optReplenishmentWithdrawalMove.click();
        forward.click();
        notificationDialog.isPresentWithTitle("Заданий выбранного типа нет");
    }

    /**
     * @param area участок, в котором будут искаться задания
     */
    private void clickAreaOptionByValue(String area) {
        By by = byXpath("//option[@value='" + area + "']");
        $(by).click();
    }

    public void validateNoMoveTurnoverTask(String area) {
        clickAreaOptionByValue(area);
        optReplenishmentTurnoverMove.click();
        forward.click();
        notificationDialog.isPresentWithTitle("Заданий выбранного типа нет");

    }
}
