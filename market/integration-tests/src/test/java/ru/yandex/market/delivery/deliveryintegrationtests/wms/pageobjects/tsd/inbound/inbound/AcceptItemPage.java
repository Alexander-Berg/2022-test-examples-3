package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.inbound.inbound;

import io.qameta.allure.Step;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.tsd.AbstractTsdPage;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;

public class AcceptItemPage extends AbstractTsdPage {

    private static final Logger log = LoggerFactory.getLogger(AcceptItemPage.class);

    @FindBy(id="STORER_0")
    private HtmlElement storerField;

    @FindBy(id="SKU_0")
    private HtmlElement skuField;

    @FindBy(id="DESC_0")
    private HtmlElement descField;

    public AcceptItemPage(WebDriver driver) {
        super(driver);
    }

    @Step("Проверяем, что на странице появились данные товара")
    public void verifyItem() {
        wait.until(visibilityOf(storerField));
        wait.until(visibilityOf(skuField));
        wait.until(visibilityOf(descField));

        log.info("Storer: {}, sku: {}, desc: {}",
                storerField.getAttribute("value"),
                skuField.getAttribute("value"),
                descField.getText()
        );
    }

    @Step("Принимаем единицу товара")
    public void acceptItem() {
        driver.switchTo().activeElement().sendKeys(Keys.ENTER);
    }
}
