package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.User;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.offSystemActivity.FinishOffSystemActivityPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.supervisorActivity.EmployeeMessageModalWindow;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.supervisorActivity.EmployeeStatsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.offSystemActivity.StartOffSystemActivityPage;

@Slf4j
public class SupervisorActivity {
    private WebDriver driver;
    private MenuPage menuPage;
    private final User defaultUser;

    public SupervisorActivity(WebDriver driver, User defaultUser) {
        this.driver = driver;
        this.menuPage = new MenuPage(driver);
        this.defaultUser = defaultUser;
    }

    @Step("Начинаем активность Обед")
    public void startDinnerActivity() {
        menuPage.inputSystemActivityPath();
        new StartOffSystemActivityPage(driver)
                .chooseDinnerActivity()
                .confirmOfSystemActivity();
    }

    public void checkUserStatus(String statusText) {
        checkUserStatus(defaultUser, statusText);
    }

    @Step("Выбираем пользователя и проверяем статус")
    public void checkUserStatus(User user, String statusText) {
        menuPage.inputEmployeeStatsPath();
        new EmployeeStatsPage(driver)
                .filterByEmployee(user)
                .checkStatusText(statusText);
    }

    @Step("Отправляем сообщение о Похвале пользователю")
    public String sendPraisingMessageToFirstUser() {
        EmployeeMessageModalWindow employeeMessageModalWindow = new EmployeeMessageModalWindow(driver);
        menuPage.inputEmployeeStatsPath();
        new EmployeeStatsPage(driver)
                .selectFirstResult()
                .clickButtonMessage();
        String modalText = employeeMessageModalWindow
                .clicklinkToPraise();
        employeeMessageModalWindow.clicktoSubmitButton()
                .waitModalHiddenWithText("Отправить уведомление сотрудникам");
        return modalText;
    }

    @Step("Проверяем сообщение в модальном окне и закрываем его")
    public void receiveMessage(String actualValue) {
        EmployeeMessageModalWindow employeeMessageModalWindow = new EmployeeMessageModalWindow(driver);
        employeeMessageModalWindow.waitReceivedMessageWindow();
        employeeMessageModalWindow
                .checkMessageModal(actualValue)
                .checkUserName(defaultUser)
                .clicktoReadButton();
    }

    @Step("Завершаем активность Обед")
    public void endDinnerActivity() {
        menuPage.inputEndOfSystemActivityPath();
        new FinishOffSystemActivityPage(driver)
                .finishOfDinnerActivity();
    }
}


