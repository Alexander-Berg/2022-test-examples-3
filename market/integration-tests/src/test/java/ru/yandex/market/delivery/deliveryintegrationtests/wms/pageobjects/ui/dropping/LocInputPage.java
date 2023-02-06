package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.dropping;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class LocInputPage extends AbstractInputPage {

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    @FindBy(xpath = "//div[@data-e2e='text-field']//input")
    private SelenideElement input;

    public LocInputPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches(getUrl()));
    }

    @Override
    protected String getUrl() {
        return "locInputPage$";
    }

    @Step("Сканируем локацию, нажимаем далее и ждем переход в меню сканирования стола")
    public void enterCell(String loc) {
        input.sendKeys(loc);
        forward.click();
        wait.until(urlMatches("tableInput"));
    }
}
