package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;

import static com.codeborne.selenide.Selectors.byXpath;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class MoveContainerContentPage extends AbstractPage {

    @FindBy(tagName = "input")
    private SelenideElement input;

    @FindBy(xpath = "//span[contains(text(), 'В ячейке остались тары')]/../..")
    private SelenideElement containerLeftSection;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    private final String consolidationContainer;

    public MoveContainerContentPage(WebDriver driver, String consolidationContainer) {
        super(driver);
        this.consolidationContainer = consolidationContainer;
        wait.until(urlMatches("moveContainerContentPage"));
    }

    @Step("Перемещаем содержимое контейнера в ячейку")
    public ConsolidationDestinationPage moveContainerContentToLoc(String cartId) {
        Integer cartIdMatches = containerLeftSection
                .$$(byXpath(".//div/span[contains(text(), '" + cartId + "')]"))
                .size();
        Assertions.assertEquals(cartIdMatches, 1,
                String.format("%s НЗН для консолидации не совпадает с размещённой", cartId));
        input.sendKeys(consolidationContainer);
        forward.click();
        return new ConsolidationDestinationPage(driver);
    }
}
