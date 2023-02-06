package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.precons;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

public class ConsLineInputPage extends AbstractInputPage {

    @FindBy(xpath = "//span[@data-e2e='destination']")
    private SelenideElement destination;

    public ConsLineInputPage(WebDriver driver) {super (driver);}

    @Step("Вводим линию консолидации")
    public ContainerConsInputPage enterConsLine(String consolidationCell) {
        super.performInput(consolidationCell);
        return new ContainerConsInputPage(driver);
    }

    @Step("Вводим предлагаемую линию консолидации")
    public ContainerConsInputPage enterProposedConsLine() {
        String consLine = destination.getText();
        super.performInput(consLine);
        return new ContainerConsInputPage(driver);
    }

    @Override
    protected String getUrl() {
        return "moveContainerInputPage($|\\?)";
    }
}
