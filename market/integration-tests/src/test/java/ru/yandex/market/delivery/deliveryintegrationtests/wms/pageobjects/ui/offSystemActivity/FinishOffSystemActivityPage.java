package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.offSystemActivity;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.NotificationDialog;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class FinishOffSystemActivityPage extends AbstractPage {

    public FinishOffSystemActivityPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("finishOffsystemActivity"));
        notificationDialog = new NotificationDialog(driver);
    }

    @FindBy(xpath = "//button[@data-e2e='LUNCH']")
    private SelenideElement dinnerButton;

    @Step("Завершаем активность Обед")
    public FinishOffSystemActivityPage finishOfDinnerActivity() {
        dinnerButton.click();
        Assertions.assertTrue(notificationDialog.isPresentWithTitleCustomTimeout("Внесистемная активность успешно завершена", 10), "Не появился диалог с заголовком \"Внесистемная активность успешно завершена\"");
        notificationDialog.waitUntilHidden();
        return this;
    }
}
