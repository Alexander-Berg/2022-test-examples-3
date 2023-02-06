package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wave.execution.wavemanagement.wavepage;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class OrderListSubpage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(OrderListSubpage.class);

    @Name("Кнопка рефреша списка заказов")
    @FindBy(xpath = "//img[@id = '${id}_refresh']")
    private HtmlElement refreshButton;

    @Name("Поле Количество в пакетном заказе")
    @FindBy(xpath = "//span[text()[starts-with(., 'B')]]/parent::td/parent::tr/td/span[contains(@id, '_9_span')]")
    private HtmlElement totalGoods;

    @Name("Поле Зарезервировано в пакетном заказе")
    @FindBy(xpath = "//span[text()[starts-with(., 'B')]]/parent::td/parent::tr/td/span[contains(@id, '_10_span')]")
    private HtmlElement reservedGoods;

    public OrderListSubpage(WebDriver driver) {
        super(driver);
    }

    @Step("Рефрешим список заказов")
    public void refreshOrderList() {
        safeClick(refreshButton);
        overlayBusy.waitUntilHidden();
    }

    @Step("Получаем кол-во товаров в пакетном заказе")
    public int getTotalGoods() {
        log.info("Total goods: {}", totalGoods.getText());
        return Integer.valueOf(totalGoods.getText());
    }

    @Step("Получаем кол-во зарезервированных товаров")
    public int getReservedGoods() {
        log.info("Reserved goods: {}", reservedGoods.getText());
        return Integer.valueOf(reservedGoods.getText());
    }

}
