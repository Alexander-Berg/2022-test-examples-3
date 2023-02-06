package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wave.execution;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class OrderWithoutWavePage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(OrderWithoutWavePage.class);

    @Name("Кнопка Создать волну")
    @FindBy(xpath = "//div[@id = 'A264d2v']")
    private HtmlElement createWaveButton;

    @Name("Кнопка фильтрации")
    @FindBy(xpath = "//input[@id = '$47yeyb_filterbutton']")
    private HtmlElement filterButton;

    @Name("Поле фильтрации по номеру заказа")
    @FindBy(xpath = "//input[@attribute = 'ORDERKEY']")
    private HtmlElement orderIdField;

    @Name("Первый чекбокс в результатах поиска")
    @FindBy(xpath = "//input[@id = '$47yeyb_rowChkBox_0']")
    private HtmlElement firstCheckbox;

    public OrderWithoutWavePage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим {0} в поле фильтрации по номеру заказа")
    public OrderWithoutWavePage inputOrderId(String orderId) {
        orderIdField.sendKeys(orderId);
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Жмем кнопку фильтрации")
    public OrderWithoutWavePage filterButtonClick() {
        filterButton.click();
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Выбираем первый элемент в таблице")
    public OrderWithoutWavePage selectFirstResult() {
        firstCheckbox.click();

        return this;
    }

    @Step("Жмем кнопку Создать волну")
    public void createWaveClick() {
        createWaveButton.click();
        overlayBusy.waitUntilHidden();
    }

}
