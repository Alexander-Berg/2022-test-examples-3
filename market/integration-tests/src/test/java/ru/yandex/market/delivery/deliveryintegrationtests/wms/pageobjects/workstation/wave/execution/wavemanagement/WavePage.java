package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wave.execution.wavemanagement;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wave.execution.wavemanagement.wavepage.OrderListSubpage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class WavePage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(WavePage.class);

    private OrderListSubpage orderListSubpage = new OrderListSubpage(driver);

    @Name("Поле Номер волны")
    @FindBy(xpath = "//input[@id = 'I9i19c4']")
    private HtmlElement waveNumberField;

    @Name("Поле Статус")
    @FindBy(xpath = "//select[@id = 'I4ybyho']")
    private HtmlElement waveStatusFiels;

    @Name("Поле Номер пакетного заказа")
    @FindBy(xpath = "//input[@id = 'Iigc4mx']")
    private HtmlElement batchNumberField;

    public WavePage(WebDriver driver) {
        super(driver);
    }

    public OrderListSubpage OrderList() {
        return orderListSubpage;
    }

    @Step("Считываем номер волны")
    public String getWaveId() {
        return waveNumberField.getAttribute("value");
    }

    @Step("Получаем статус волны")
    public WaveStatus getWaveStatus() {
        return WaveStatus.get(Integer.valueOf(waveStatusFiels.getAttribute("oldvalue")));
    }

    @Step("Считываем номер пакетного заказа")
    public String getBatchOrderId() {
        log.info("BatchOrder: {}", batchNumberField.getAttribute("value"));
        return batchNumberField.getAttribute("value");
    }

}
