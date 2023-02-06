package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.dropping;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class BbxdConfirmPage extends AbstractInputPage {

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    public BbxdConfirmPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches(getUrl()));
    }

    @Override
    protected String getUrl() {
        return "confirmPage$";
    }

    @Step("Вводим количество и нажимаем далее")
    public BbxdDropPage confirmAmount(int amount) {
        //в случае если сортируется один товар, количество в поле вводится автоматически
        if (amount != 1) {
            super.performInput(Integer.valueOf(amount).toString());
        }
        forward.click();
        return new BbxdDropPage(driver);
    }
}
