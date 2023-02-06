package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shipping;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import static com.codeborne.selenide.Selectors.byXpath;
import org.openqa.selenium.WebDriver;
import ru.yandex.common.util.date.TimerUtils;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;

import static com.codeborne.selenide.Selenide.$;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class DoorToDeliveryPage extends AbstractPage {

    public DoorToDeliveryPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("deliveryConnections"));
    }

    @Step("Привязываем СД к воротам")
    public void appendDeliveryIdForDoor(String door, String carrier) {
        $(byXpath(String.format("//td[@data-e2e='carrierCodes_cell_%s']", door))).click();
        SelenideElement carrierCheckBox =
                $(byXpath(String.format("//div[contains(@data-e2e,'%s')]//input", carrier)));
        if (carrierCheckBox.getAttribute("checked") != null) {
            return;
        }
        carrierCheckBox.click();
        // ждем пока привязка вступит в силу
        TimerUtils.sleep(3000);
    }
}
