package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.offSystemActivity;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class StartOffSystemActivityPage extends AbstractPage {

    public StartOffSystemActivityPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("startOffsystemActivity"));
        notificationDialog = new NotificationDialog(driver);
    }

    @FindBy(xpath = "//button[@data-e2e='LUNCH']")
    private SelenideElement dinnerButton;

    @Step("Начинаем активность Обед")
    public StartOffSystemActivityPage chooseDinnerActivity() {
        dinnerButton.click();
        return this;
    }

    @Step("Нажимаем кнопку подтверждения")
    public StartOffSystemActivityPage confirmOfSystemActivity() {
        ModalWindow modalWindow = new ModalWindow(driver);
        modalWindow.clickForward();
        Assertions.assertTrue(notificationDialog.isPresentWithTitleCustomTimeout("Внесистемная активность успешно запущена", 10), "Не появился диалог с заголовком \"Внесистемная активность успешно запущена\"");
        notificationDialog.waitUntilHidden();
        return this;
    }


}
