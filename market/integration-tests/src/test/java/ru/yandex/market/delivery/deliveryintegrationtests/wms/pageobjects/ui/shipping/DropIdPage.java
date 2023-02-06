package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shipping;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

public class DropIdPage extends AbstractInputPage {
    private static final String CLOSE_SHIPMENT_BUTTON = "//button[@data-e2e='button_forward']";
    private static final String SHIPPING_PAGE_URL = "shippingPage";
    private static final Pattern SHIPPING_PAGE_PATTERN = Pattern.compile(".+?" + SHIPPING_PAGE_URL + ".*");

    @FindBy(xpath = CLOSE_SHIPMENT_BUTTON)
    SelenideElement closeShipmentButton;
    @FindBy(xpath = "//div[@data-e2e='additionalMenu']")
    SelenideElement openMenuButton;
    @FindBy(xpath = "//button[@data-e2e='Context_finish_shipping_manualy']")
    SelenideElement closeShipmentFromMenuButton;

    public DropIdPage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return SHIPPING_PAGE_URL;
    }

    @Step("Сканируем дропку")
    public FinishGatePage enterDropId(String dropId) {
        return enterDropIds(List.of(dropId));
    }

    @Step("Сканируем дропку ")
    public FinishGatePage enterDropIds(Collection<String> dropIds) {
        for (String dropId : dropIds) {
            super.performInput(dropId);
            Assertions.assertTrue(
                    notificationDialog.IsPresentWithMessageWithCustomTimeout("Дропка загружена", 10),
                    "Не появилось сообщение об успешной загрузке дропки"
            );
            notificationDialog.waitUntilHidden();
        }

        if (!SHIPPING_PAGE_PATTERN.matcher(driver.getCurrentUrl()).matches()) {
            closeShipmentButton.click();
        } else {
            openMenuButton.click();
            closeShipmentFromMenuButton.click();
        }
        return new FinishGatePage(driver);
    }
}
