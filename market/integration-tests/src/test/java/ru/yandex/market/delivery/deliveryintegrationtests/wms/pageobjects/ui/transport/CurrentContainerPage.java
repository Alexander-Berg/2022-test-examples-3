package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class CurrentContainerPage extends AbstractPage {

    @FindBy(tagName = "input")
    private SelenideElement input;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    private final String consolidationContainer;

    public CurrentContainerPage(WebDriver driver, String consolidationContainer) {
        super(driver);
        this.consolidationContainer = consolidationContainer;
        wait.until(urlMatches("currentContainerPage"));
    }

    @Step("Вводим контейнер для аномалий")
    public MoveContainerContentPage inputAnomalyContainer(String container) {
        input.sendKeys(container);
        forward.click();
        return new MoveContainerContentPage(driver, consolidationContainer);
    }
}
