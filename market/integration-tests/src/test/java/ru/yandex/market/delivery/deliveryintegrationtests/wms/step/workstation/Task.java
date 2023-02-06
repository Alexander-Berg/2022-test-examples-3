package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.workstation;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.execution.productivity.TaskPage;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
public class Task extends AbstractWSSteps {

    private final TaskPage taskPage;

    public Task(WebDriver driver) {
        super(driver);

        taskPage = new TaskPage(driver);
    }

    @Step("Удаляем все ожидающие задания на размещение и консолидацию аномалий")
    public void deletePendingAnomalyLocationTasks() {
        topMenu.whSelectorClick().openWarehouse();
        leftMenu.WMS().execution().productivity().task();

        taskPage.selectAnomalyPlacementAndConsolidationTypes();
        taskPage.selectPendingStatus();
        taskPage.filterButtonClick();

        taskPage.selectAllTasksIfAnyExists();
        taskPage.deleteSelectedTasks();
    }
}
