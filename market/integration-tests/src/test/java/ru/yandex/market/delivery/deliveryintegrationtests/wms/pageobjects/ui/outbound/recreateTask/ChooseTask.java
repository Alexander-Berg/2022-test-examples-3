package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.outbound.recreateTask;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport.NestedIdsMovementsScanParentIdPage;
import com.codeborne.selenide.SelenideElement;

public class ChooseTask extends AbstractPage {

    @FindBy(xpath = "//button[@data-e2e='dialog_button_sorter']")
    private SelenideElement sortingButton;

    @FindBy(xpath = "//button[@data-e2e='dialog_button_container']")
    private SelenideElement recreateTaskButton;

    @FindBy(xpath = "//div[@data-e2e='additionalMenu']")
    private SelenideElement additionalMenuButton;

    @FindBy(xpath = "//button[@data-e2e='Context_change_parent_id']")
    private SelenideElement replaceBetweenParentButton;

    public ChooseTask(WebDriver driver) {
        super(driver);
    }

    @Step("Выбираем тип задачи - Пересоздать задание для ТОТа")
    public LocationInputPage recreateTask() {
        recreateTaskButton.click();
        return new LocationInputPage(driver);
    }

    @Step("Открываем через три точки - Сменить род. тару")
    public NestedIdsMovementsScanParentIdPage changeParent() {
        additionalMenuButton.click();
        replaceBetweenParentButton.click();
        return new NestedIdsMovementsScanParentIdPage(driver);
    }

}
