package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.transport;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import com.codeborne.selenide.SelenideElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

@Resource.Classpath("wms/wms.properties")
public class ConsolidationDestinationPage extends AbstractPage {

    @Property("wms.ui.tasks.anomaly.consolidation.buffer")
    private String anomalyConsolidationBuffZone;

    @FindBy(tagName = "input")
    private SelenideElement input;

    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement forward;

    public ConsolidationDestinationPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("consolidationDestinationPage"));
    }

    @Step("Консолидируем аномальные тары в буфферную зону")
    public TasksWithLocationPage consolidateIntoBuffZone() {
        input.sendKeys(anomalyConsolidationBuffZone);
        forward.click();
        return new TasksWithLocationPage(driver, "");
    }
}
