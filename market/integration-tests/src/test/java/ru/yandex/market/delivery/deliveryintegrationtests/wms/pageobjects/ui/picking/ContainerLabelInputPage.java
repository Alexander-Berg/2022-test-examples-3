package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.picking;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class ContainerLabelInputPage extends AbstractInputPage {

    public ContainerLabelInputPage(WebDriver driver) {
        super(driver);
    }

    @FindBy(xpath = "//span[contains(text(), 'Участок:')]")
    private SelenideElement putawayZoneLabel;

    @Step("Вводим тару отбора")
    public CheckLocationPage enterCart(String containerLabel) {
        super.performInput(containerLabel);
        return new CheckLocationPage(driver);
    }

    @Override
    protected String getUrl() {
        return "containerLabelInputPage($|\\?)";
    }

    @Step("Проверяем, что назначение выдалось из зоны {putawayZoneKey}")
    public ContainerLabelInputPage checkArea(String putawayZoneKey) {
        Assertions.assertEquals(putawayZoneKey,
                StringUtils.substringAfter(putawayZoneLabel.getText(), ":").trim(),
                "Фактический участок назначения не совпал с ожидаемым");

        return this;
    }
}
