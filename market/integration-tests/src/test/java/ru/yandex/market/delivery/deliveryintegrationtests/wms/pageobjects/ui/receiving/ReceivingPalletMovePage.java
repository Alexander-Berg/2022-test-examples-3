package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;

import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;

public class ReceivingPalletMovePage extends AbstractPage {

    public ReceivingPalletMovePage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("newBomPage$"));
    }

    @Step("Вводим айди паллеты приемки")
    public ReceivingPalletMovePage enterPalletId(String palletId) {
        return new ReceivingPalletMovePage(driver);
    }

    @Step("Вводим локацию")
    public ReceivingPalletMovePage enterLoc(String loc) {
        return new ReceivingPalletMovePage(driver);
    }

}
