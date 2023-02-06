package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;
public class WorkingAreaInputPage extends AbstractInputPage {

    public WorkingAreaInputPage(WebDriver driver) {
        super(driver);
    }
    @Step("Вводим участок")
    public void enterWorkingArea(String workingArea) {
        super.performInput(workingArea);
    }

    @Override
    protected String getUrl() {
        return "workingAreaInputPage($|\\?)";
    }

    @Step("Проверка: Появился ли диалог 'Нет действий для выполнения'")
    public boolean noComplectationStartedNotificationPresent() {
        return driver.getCurrentUrl().matches(".*" + getUrl() + ".*") //Если есть отборы, то нас перекинет на новую страницу
                && notificationDialog.IsPresentWithMessage("Отсутствуют назначения на этом участке");
    }
}
