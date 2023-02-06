package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.uitAdmin;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractInputPage;

@Slf4j
public class UitInputDeletePage extends AbstractInputPage {

    public UitInputDeletePage(WebDriver driver) {
        super(driver);
    }

    @Override
    protected String getUrl() {
        return "uitInputDelete";
    }

    @Step("Вводим УИТ для отмены приемки: {uit}")
    public DeleteUitPage enterUitToCancel(String uit) {
        super.performInput(uit);
        return new DeleteUitPage(driver);
    }
}
