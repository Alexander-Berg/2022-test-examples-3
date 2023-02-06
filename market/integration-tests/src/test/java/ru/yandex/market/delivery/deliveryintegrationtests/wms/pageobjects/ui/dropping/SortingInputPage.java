package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.dropping;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;

public class SortingInputPage extends AbstractInputPage {

    public SortingInputPage(WebDriver driver) {
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
