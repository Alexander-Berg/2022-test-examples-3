package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shippingsorter;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

@Slf4j
public class SorterOrderManagementPage extends AbstractPage {

    @FindBy(xpath = "//div[@data-e2e='boxId_filter']//input")
    private SelenideElement parcelInput;
    @FindBy(xpath = "//td[@data-e2e='currentLoc_cell_row_0']")
    private SelenideElement currentLocElement;

    public SorterOrderManagementPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches(getUrl()));
    }

    @SneakyThrows
    @Step("Получение текущей локации сортер ордера")
    public void checkSorterOrderLocation(String parcelId, String targetLocation) {
        parcelInput.sendKeys(parcelId);
        parcelInput.pressEnter();
        String currentLoc = currentLocElement.getText();
        log.info("verify Sorter Order {} location: {}", parcelId, currentLoc);
        Assertions.assertEquals(targetLocation, currentLoc,
                "Transport order is moving on conveyor:");
    }

    protected String getUrl() {
        return "sorterOrderManagementPage";
    }
}
