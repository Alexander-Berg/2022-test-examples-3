package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wave.execution.wavemanagement;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class WaveDetailPage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(WaveDetailPage.class);

    @Name("Номер заказа")
    @FindBy(xpath = "//input[@id='Ic889oa']")
    private HtmlElement OrderNumberField;

    @Name("Первое значение колонки Ключ волны")
    @FindBy(xpath = "//span[@id = '$lxfoah_cell_0_1_span']")
    private HtmlElement waveNumberField;

    @Name("Кнопка фильтрации")
    @FindBy(xpath = "//input[@id = '$lxfoah_filterbutton']")
    private HtmlElement filterButton;

    public WaveDetailPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим номер заказа")
    public WaveDetailPage inputOrderId(String orderId) {
        OrderNumberField.sendKeys(orderId);
        overlayBusy.waitUntilHidden();
        return this;
    }

    @Step("Жмем кнопку фильтрации")
    public WaveDetailPage filterButtonClick() {
        filterButton.click();
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Проверяем, что на странице есть результаты фильтрации")
    public boolean resultsShown() {
        return driver.findElements(By.xpath("//input[@id = '$lxfoah_row_0']")).size() > 0;
    }

    @Step("Получаем номер волны")
    public String getWaveNumber() {
        return waveNumberField.getText();
    }
}
