package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.execution.productivity;

import java.util.List;
import java.util.concurrent.TimeUnit;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class TaskPage extends AbstractWsPage {

    @Name("Кнопка фильтрации")
    @FindBy(xpath = "//input[@id = '$c7l7z1_filterbutton']")
    private HtmlElement filterButton;

    @Name("Поле фильтрации по типу задачи")
    @FindBy(xpath = "//div[@id = 'multiselectparent_Jhd44p5']")
    private HtmlElement taskType;

    @Name("Поле фильтрации по статусу задачи")
    @FindBy(xpath = "//div[@id = 'multiselectparent_Jdi035o']")
    private HtmlElement taskStatus;

    @Name("Чекбокс тип задания - Размещение аномальных контейнеров")
    @FindBy(xpath = "//input[@value = 'APLCMNT']")
    private HtmlElement anomalyPlacementCheckbox;

    @Name("Чекбокс тип задания - Консолидация аномальных контейнеров")
    @FindBy(xpath = "//input[@value = 'ACNS']")
    private HtmlElement anomalyConsolidationCheckbox;

    @Name("Чекбокс статус задания - Ожидает")
    @FindBy(xpath = "//label[contains(text(), 'Ожидает')]/input")
    private HtmlElement pendingStatus;

    @Name("Кнопка Удалить")
    @FindBy(xpath = "//div[@id = 'Azc2tbs']")
    private HtmlElement deleteButton;

    public TaskPage(WebDriver driver) {
        super(driver);
    }

    @Step("Выбираем типы заданий: Размещение и Консолидация аномальных тар")
    public void selectAnomalyPlacementAndConsolidationTypes() {
        taskType.click();
        anomalyPlacementCheckbox.click();
        anomalyConsolidationCheckbox.click();
    }

    @Step("Выбираем статус задания: Ожидает")
    public void selectPendingStatus() {
        taskStatus.click();
        pendingStatus.click();
    }

    @Step("Запускаем фильтрацию")
    public void filterButtonClick() {
        filterButton.click();
        overlayBusy.waitUntilHidden();
    }

    @Step("Выбираем все отфильрованные задания")
    public void selectAllTasksIfAnyExists() {
        driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        List<WebElement> selectAllCheckboxCandidates =
                driver.findElements(By.xpath("//input[@id = '$c7l7z1_SelectAllChkBox']"));
        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);

        selectAllCheckboxCandidates.stream().findFirst().ifPresent(WebElement::click);
    }

    @Step("Удаляем выбранные задания")
    public void deleteSelectedTasks() {
        deleteButton.click();
    }
}
