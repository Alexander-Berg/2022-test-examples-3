package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.outgoing;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class WaveConsolidationPage extends AbstractTsdPage {

    @FindBy(xpath = "//input[@id = 'id_0']")
    private HtmlElement idInput;

    public WaveConsolidationPage(WebDriver driver) {
        super(driver);
    }

    @Step("Сканируйте ШК тары")
    public ConsolidationBoxPage submitCart(String cart) {
        idInput.sendKeys(cart);
        idInput.sendKeys(Keys.ENTER);
        return new ConsolidationBoxPage(driver);
    }

    public static class ConsolidationBoxPage extends AbstractTsdPage {

        @FindBy(xpath = "//input[@id = 'confirmtoloc_0']")
        private HtmlElement cellInput;

        public ConsolidationBoxPage(WebDriver driver) {
            super(driver);
        }

        @Step("Просканируйте ШК ячейки")
        public void submitCell(String cell) {
            cellInput.sendKeys(cell);
            cellInput.sendKeys(Keys.ENTER);

        }
    }
}
