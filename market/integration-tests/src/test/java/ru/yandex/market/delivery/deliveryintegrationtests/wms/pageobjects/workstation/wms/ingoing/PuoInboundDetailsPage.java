package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.ingoing;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

import static ru.yandex.common.util.ValidationUtils.assertNotEmpty;

public class PuoInboundDetailsPage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(PuoInboundDetailsPage.class);

    @Name("Кнопка фильтрации")
    @FindBy(xpath = "//input[@id = '$s27dog_filterbutton']")
    private HtmlElement filterButton;

    @Name("Поле фильтрации по номеру поставки")
    @FindBy(xpath = "//input[@id = 'Ixjdosv']")
    private HtmlElement inboundIdField;

    @Name("Поле фильтрации по товару")
    @FindBy(xpath = "//input[@id = 'I6wc4s3']")
    private HtmlElement skuField;

    public PuoInboundDetailsPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим {0} в поле фильтрации по номеру поставки")
    public void inputInboundId(String inboundId) {
        inboundIdField.sendKeys(inboundId);
        overlayBusy.waitUntilHidden();
    }

    @Step("Вводим {0} в поле фильтрации по товару")
    public void inputSku(String sku) {
        skuField.sendKeys(sku);
        overlayBusy.waitUntilHidden();
    }

    @Step("Жмем кнопку фильтрации")
    public void filterButtonClick() {
        filterButton.click();
        overlayBusy.waitUntilHidden();
    }

    @Step("Проверяем, что есть результаты фильтрации")
    public boolean firstResultIsPresent() {
        String firstResultCheckboxXpath = "//input[@type = 'checkbox' and @id = '$s27dog_rowChkBox_0']";
        return driver.findElements(By.xpath(firstResultCheckboxXpath)).size() > 0;
    }

    @Step("Получаем айди паллеты")
    public String selectPallet() {
        String palletXpath = "//span[@id = '$s27dog_cell_0_9_span']";
        final String pallet = driver.findElement(By.xpath(palletXpath)).getText();

        assertNotEmpty(pallet, "pallet");

        log.info("Pallet {}", pallet);
        return pallet;
    }
}
