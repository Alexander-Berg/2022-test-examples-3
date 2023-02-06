package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.wave.execution.wavemanagement;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveProgress;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.workstation.AbstractWsPage;
import ru.yandex.qatools.htmlelements.annotations.Name;
import ru.yandex.qatools.htmlelements.element.HtmlElement;

public class WaveManagementPage extends AbstractWsPage {

    private static final Logger log = LoggerFactory.getLogger(WaveManagementPage.class);

    @Name("Счетчик результатов")
    @FindBy(xpath = "//td[contains(text(), 'из')]")
    private HtmlElement resulstCounter;

    @Name("Кнопка фильтрации")
    @FindBy(xpath = "//input[@id = '$u7zkc9_filterbutton']")
    private HtmlElement filterButton;

    @Name("Поле Ключ волны")
    @FindBy(xpath = "//input[@id = 'Ifyqtt8']")
    private HtmlElement waveNumberField;

    @Name("Поле Кем создано")
    @FindBy(xpath = "//input[@id = 'Ihrniup']")
    private HtmlElement createdByField;

    @Name("Поле Зарезервировано")
    @FindBy(xpath = "//input[@id = 'Iyvd7dp']")
    private HtmlElement reservedField;

    @Name("Первый чекбокс в результатах поиска")
    @FindBy(xpath = "//input[@id = '$u7zkc9_rowChkBox_0']")
    private HtmlElement firstCheckbox;

    @Name("Первое значение в колонке В процессе")
    @FindBy(xpath = "//span[@id = '$u7zkc9_cell_0_5_span']")
    private HtmlElement firstInprogressField;

    @Name("Кнопка Открыть первую волну в списке")
    @FindBy(xpath = "//input[@id = '$u7zkc9_cell_0_0_Img']")
    private HtmlElement openFirstWave;

    public WaveManagementPage(WebDriver driver) {
        super(driver);
    }

    @Step("Вводим номер волны")
    public WaveManagementPage inputWaveId(String waveId) {
        waveNumberField.sendKeys(waveId);

        return this;
    }

    @Step("Вводим Кем создано")
    public WaveManagementPage inputCreatedBy(String createdBy) {
        createdByField.sendKeys(createdBy);

        return this;
    }

    @Step("Вводим количество зарезервированного")
    public WaveManagementPage inputReserved(String reserved) {
        reservedField.sendKeys(reserved);

        return this;
    }

    @Step("Жмем кнопку фильтрации")
    public WaveManagementPage filterButtonClick() {
        filterButton.click();
        overlayBusy.waitUntilHidden();

        return this;
    }

    @Step("Проверяем, что на странице есть результаты фильтрации")
    public boolean resultsShown() {
        return driver.findElements(By.xpath("//input[@id = '$u7zkc9_rowChkBox_0']")).size() > 0;
    }

    @Step("Выбираем первую волну в результатах поиска")
    public void selectFirstWave() {
        firstCheckbox.click();
    }

    @Step("Открываем первую волну в результатах поиска")
    public void openFirstWave() {
        openFirstWave.click();
        overlayBusy.waitUntilHidden();
    }

    @Step("Получаем состояние волны")
    public WaveProgress getWaveProgressState() {
        return WaveProgress.get(firstInprogressField.getText());
    }
}
