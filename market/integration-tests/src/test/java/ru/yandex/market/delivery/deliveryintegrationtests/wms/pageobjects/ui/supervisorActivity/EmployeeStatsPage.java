package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.supervisorActivity;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.User;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.qatools.htmlelements.annotations.Name;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class EmployeeStatsPage extends AbstractPage {

    public EmployeeStatsPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("employeeStats"));
    }

    @Name("Поле фильтрации по сотруднику")
    @FindBy(xpath = "//div[@data-e2e='userName_filter']/div/input")
    private SelenideElement employeeFilterInput;

    @Name("Чекбокс первого в результатах поиска")
    @FindBy(xpath = "//input[@data-e2e='selectedUser_checkbox_0']")
    private SelenideElement firstCheckbox;

    @Name("Кнопка отправить сообщение")
    @FindBy(xpath = "//div[4]/button[@data-e2e='button_row_0_header']")
    private SelenideElement buttonMessage;

    @Name("Значение поля Статус")
    @FindBy(xpath = "//td[@data-e2e='location_cell_row_0']/span/span")
    private SelenideElement statusResult;

    @Step("Вводим имя сотрудника {defaultUser} в поле фильтрации")
    public EmployeeStatsPage filterByEmployee(User defaultUser) {
        employeeFilterInput.sendKeys(defaultUser.getLogin());
        employeeFilterInput.pressEnter();
        return this;
    }

    @Step("Выбираем первый элемент в таблице")
    public EmployeeStatsPage selectFirstResult() {
        firstCheckbox.click();
        return this;
    }

    @Step("Нажимаем кнопку Отправить сообщение")
    public EmployeeStatsPage clickButtonMessage() {
        buttonMessage.click();
        return this;
    }

    @Step("Проверяем текст Статуса")
    public EmployeeStatsPage checkStatusText(String statusText) {
        String actualValue = statusResult.getText();
        Assertions.assertEquals(statusText, actualValue, "Ожидаемый статус не совпал с фактическим");
        return this;
    }
}
