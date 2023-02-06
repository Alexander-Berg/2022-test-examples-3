package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wms.configuration;

import io.qameta.allure.Step;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class ProductPage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(ProductPage.class);

    @Name("Счетчик результатов")
    @FindBy(xpath = "//div[starts-with(@id, 'slot_')]//td[contains(text(), 'из')]")
    private HtmlElement resulstCounter;


    @Name("Кнопка следующая страница")
    @FindBy(xpath = "//div[starts-with(@id, 'slot')]//img[@alt = 'Далее' or @title = 'Далее']")
    private HtmlElement nextButton;

    @Name("Кнопка фильтрации")
    @FindBy(xpath = "//input[@id = '$asvr2k_filterbutton']")
    private HtmlElement filterButton;

    @Name("Поле фильтрации: Артикул поставщика")
    @FindBy(xpath = "//input[@attribute='MANUFACTURERSKU']")
    private HtmlElement supplierSkuField;

    @Name("Поле фильтрации: Айди поставщика")
    @FindBy(xpath = "//input[@attribute='STORERKEY']")
    private HtmlElement storerkeyField;

    @Name("Результат фильтрации: ROV-ка")
    @FindBy(xpath = "//span[starts-with(@id, '$asvr2k_cell_0_') and starts-with(text(), 'ROV')]")
    private HtmlElement rovResult;

    @Name("Поле фильтрации: Товар")
    @FindBy(xpath = "//input[@attribute='SKU']")
    private HtmlElement rovField;

    @Name("Кнопка открытия деталей ROV-ки")
    @FindBy(xpath = "//input[@type='image' and @id='$asvr2k_cell_0_0_Img']")
    private HtmlElement rovDetails;

    @Name("Вкладка \"Входящие\" в деталях ROV-ки")
    @FindBy(xpath = "//div[@id='SectionContent2']//div[text() = 'Входящие']")
    private HtmlElement incomingTab;

    @Name("Чекбокс \"Требуется ручная настройка\"")
    @FindBy(xpath = "//input[@attribute='MANUALSETUPREQUIRED']")
    private HtmlElement manualSetUpRequiredCheckbox;

    @Name("Кнопка Сохранить")
    @FindBy(xpath = "//div[@id = 'Aq8kvq2']")
    private HtmlElement saveButton;

    public ProductPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим Артикул поставщика")
    public ProductPage inputSupplierSku(String supplierSku) {
        inputFilterField(supplierSkuField, supplierSku);

        return this;
    }

    @Step("Вводим айди поставщика")
    public ProductPage inputSupplierId(long supplierId) {
        inputFilterField(storerkeyField, String.valueOf(supplierId));

        return this;
    }

    @Step("Запускаем фильтрацию")
    public ProductPage filterButtonClick() {
        overlayBusy.waitUntilHidden();
        filterButton.click();
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Получаем ROV-ку из результатов фильтра")
    public String getRovFromFilterResults() {

        Assertions.assertTrue(StringUtils
                .containsIgnoreCase(rovResult.getText(), "ROV000")
        );

        return rovResult.getText();
    }

    @Step("Вводим Товар")
    public ProductPage inputRov(String rov) {
        inputFilterField(rovField, rov);

        return this;
    }

    @Step("Открываем детали найденной ROV-ки")
    public ProductPage openRovDetails() {
        rovDetails.click();
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Открываем вкладку \"Входящие\" в деталях ROV-ки")
    public ProductPage openIncomingTab() {
        incomingTab.click();
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Кликаем по чекбоксу \"Требуется ручная настройка\"")
    public ProductPage clickManualSetUpRequiredCheckbox() {
        manualSetUpRequiredCheckbox.click();

        return this;
    }

    @Step("Сохранить изменения")
    public ProductPage saveChanges() {
        saveButton.click();
        overlayBusy.waitUntilHidden();

        return this;
    }
}
