package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.ingoing;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class PuoInboundPage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(PuoInboundPage.class);

    @Name("Кнопка фильтрации")
    @FindBy(xpath = "//input[@id = '$ij2psx_filterbutton']")
    private HtmlElement filterButton;

    @Name("Поле фильтрации по номеру поставки")
    @FindBy(xpath = "//input[@id = 'Ixjdosv']")
    private HtmlElement inboundIdField;

    @Name("Первый элемент в результатах поиска")
    @FindBy(xpath = "//input[@id = '$ij2psx_rowChkBox_0']")
    private HtmlElement firstCheckbox;

    @Name("Первый элемент в результатах поиска")
    @FindBy(xpath = "//input[@id = '$ij2psx_cell_0_0_Img']")
    private HtmlElement firstFilteredElement;

    @Name("Первая ячейка из колонки Товар")
    @FindBy(xpath = "//span[@id = '$i79iaj_cell_0_4_span']")
    private HtmlElement firstProductItemNumber;

    public PuoInboundPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим {0} в поле фильтрации по номеру поставки")
    public void inputInboundId(String inboundId) {
        inboundIdField.sendKeys(inboundId);
        overlayBusy.waitUntilHidden();
    }

    @Step("Жмем кнопку фильтрации")
    public void filterButtonClick() {
        filterButton.click();
        overlayBusy.waitUntilHidden();
    }

    @Step("Выбираем первый элемент в таблице")
    public void selectFirstResult() {
        firstCheckbox.click();
    }

    @Step("Нажать кнопку фильтрации для первой строки")
    public void filterFirstResult() {
        firstFilteredElement.click();
    }

    @Step("Скопировать текст из первой ячейки Товар")
    public String copyProductNumber() {
        String rov = firstProductItemNumber.getText();
        log.info("Product item number {}...", rov);
        return rov;
    }

}
