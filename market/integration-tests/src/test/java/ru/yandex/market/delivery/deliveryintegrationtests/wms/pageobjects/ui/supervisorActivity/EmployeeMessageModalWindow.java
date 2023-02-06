package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.supervisorActivity;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.User;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;
import ru.yandex.qatools.htmlelements.annotations.Name;

import java.util.Locale;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;

public class EmployeeMessageModalWindow extends ModalWindow {

    protected NotificationDialog notificationDialog;

    public EmployeeMessageModalWindow(WebDriver driver) {
        super(driver);
        notificationDialog = new NotificationDialog(driver);
    }

    @Name("Модальное окно")
    @FindBy(xpath = "//div[@role='dialog']")
    private SelenideElement modal;

    @Name("Ссылка на Похвалить")
    @FindBy(xpath = "//a[@data-e2e='grats-link']")
    private SelenideElement linkToPraise;

    @Name("Текст в модальном окне")
    @FindBy(xpath = "//div[@data-e2e='message-text-field']//textarea")
    private SelenideElement textInModalWindow;

    @Name("Текст с именем пользователя в окне сотрудника")
    @FindBy(xpath = "//span[@data-e2e='user-prop']")
    private SelenideElement textCurrentUser;

    @Name("Кнопка Отправить")
    @FindBy(xpath = "//button[@data-e2e='send-button']")
    private SelenideElement submitButton;

    @Name("Кнопка Прочитано")
    @FindBy(xpath = "//button[@data-e2e='push-message-button']")
    private SelenideElement readButton;

    @Step("Нажимаем на ссылку Похвалить")
    public String clicklinkToPraise() {
        linkToPraise.click();
        String actualValue = textInModalWindow.getText();
        return actualValue;
    }

    @Step("Проверяем сообщение")
    public EmployeeMessageModalWindow checkMessageModal(String actualValue) {
        final By by = byXpath(String.format("//div[@role='dialog']//span[text()='%s']", actualValue));
        Assertions.assertTrue(isElementPresent(by), "Отправленное сообщение не совпало с полученным");;
        return this;
    }

    @Step("Проверяем имя пользователя")
    public EmployeeMessageModalWindow checkUserName(User defaultUser) {
        String actualValue = textCurrentUser.getText();
        Assertions.assertEquals(defaultUser.getLogin().toLowerCase(Locale.ROOT), actualValue, "Ожидаемый логин автора сообщения не совпал с фактическим");
        return this;
    }

    @Step("Нажимаем на кнопку Отправить")
    public EmployeeMessageModalWindow clicktoSubmitButton() {
        submitButton.click();
        return this;
    }

    @Step("Нажимаем на кнопку Прочитано")
    public EmployeeMessageModalWindow clicktoReadButton() {
        readButton.click();
        return this;
    }

    @Step("Ждем появления окна с сообщением сотруднику")
    public void waitReceivedMessageWindow() {
        final By by = byXpath("//button[@data-e2e='push-message-button']");
        $(by).shouldBe(visible);
    }
}
