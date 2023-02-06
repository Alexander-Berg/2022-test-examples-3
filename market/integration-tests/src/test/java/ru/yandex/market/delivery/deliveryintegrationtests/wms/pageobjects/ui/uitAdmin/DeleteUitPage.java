package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.uitAdmin;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class DeleteUitPage extends AbstractPage {

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    public DeleteUitPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("deleteUit"));
    }

    @Step("Нажимаем Удалить")
    public UitInputDeletePage deleteUit() {
        forward.click();
        return new UitInputDeletePage(driver);
    }
}
