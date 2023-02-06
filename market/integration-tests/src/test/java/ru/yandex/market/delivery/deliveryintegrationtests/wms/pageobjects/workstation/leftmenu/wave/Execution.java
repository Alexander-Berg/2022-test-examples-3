package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wave;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.leftmenu.wms.execution.Stocks;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class Execution extends AbstractWsPage {

    @Name("Заказ вне волны")
    @FindBy(xpath = "//div[@id = '$wbaiqi']")
    private HtmlElement orderWithoutWave;

    @Name("Обслуживание волн")
    @FindBy(xpath = "//div[@id = '$z6e2hb']")
    private HtmlElement waveManagement;

    @Name("Детали волны")
    @FindBy(xpath = "//div[@id = '$97in1a']")
    private HtmlElement waveDetail;

    private Stocks stocks;

    public Execution(WebDriver driver) {
        super(driver);
    }

    @Step("Выбираем меню: Заказ вне волны")
    public void orderWithoutWave() {
        safeClick(orderWithoutWave);
        overlayBusy.waitUntilHidden();
    }

    @Step("Выбираем меню: Обслуживание волн")
    public void waveManagement() {
        safeClick(waveManagement);
        overlayBusy.waitUntilHidden();
    }

    @Step("Выбираем меню: Детали волны")
    public void waveDetail() {
        safeClick(waveDetail);
        overlayBusy.waitUntilHidden();
    }
}
