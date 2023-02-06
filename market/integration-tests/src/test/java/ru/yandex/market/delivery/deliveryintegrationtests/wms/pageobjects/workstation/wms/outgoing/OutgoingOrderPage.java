package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.outgoing;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.common.OverlayBusy;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;
import ru.yandex.qatools.htmlelements.loader.decorator.HtmlElementDecorator;
import ru.yandex.qatools.htmlelements.loader.decorator.HtmlElementLocatorFactory;

public class OutgoingOrderPage {
    private WebDriver driver;
    private WebDriverWait wait;
    private OverlayBusy overlayBusy;

    private static final Logger log = LoggerFactory.getLogger(OutgoingOrderPage.class);

    @Name("Кнопка фильтрации")
    @FindBy(xpath = "//input[@id = '$v2ewwt_filterbutton']")
    private HtmlElement filterButton;

    @Name("Поле фильтрации по номеру заказа")
    @FindBy(xpath = "//input[@id = 'It4g6o8']")
    private HtmlElement orderIdField;

    @Name("Иконка перехода к первому заказу в списке")
    @FindBy(xpath = "//input[@id = '$v2ewwt_cell_0_0_Img']")
    private HtmlElement firstOrderIcon;

    public OutgoingOrderPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(this.driver, WebDriverTimeout.LONG_WAIT_TIMEOUT);
        this.overlayBusy = new OverlayBusy(this.driver);

        PageFactory.initElements(new HtmlElementDecorator(new HtmlElementLocatorFactory(driver)), this);
    }

    @Step("Вводим {0} в поле фильтрации по номеру заказа")
    public void inputOrderId(String inboundId) {
        orderIdField.sendKeys(inboundId);
        overlayBusy.waitUntilHidden();
    }

    @Step("Жмем кнопку фильтрации")
    public void filterButtonClick() {
        filterButton.click();
        overlayBusy.waitUntilHidden();
    }

    @Step("Выбираем первый элемент в таблице")
    public void openFirstOrder() {
        firstOrderIcon.click();
        overlayBusy.waitUntilHidden();
    }

}
