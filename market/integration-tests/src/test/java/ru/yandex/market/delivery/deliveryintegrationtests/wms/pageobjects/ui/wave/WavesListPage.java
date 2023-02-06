package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.wave;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.WaveStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import ru.yandex.qatools.htmlelements.annotations.Name;

import java.time.Duration;

import static com.codeborne.selenide.Condition.enabled;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

@Slf4j
public class WavesListPage extends AbstractPage {
    @Name("Кнопка Показать действия первого элемента таблицы")
    @FindBy(xpath = "//button[@data-e2e='more_actions_button_row_0']")
    private SelenideElement firstResultMoreActionsButton;

    @Name("Кнопка Резервирование волны первого элемента таблицы")
    @FindBy(xpath = "//button[@data-e2e='reserve_button_row_0']")
    private SelenideElement firstResultReserveButton;

    @Name("Кнопка Запуск первого элемента таблицы")
    @FindBy(xpath = "//button[@data-e2e='start_wave_button_row_0']")
    private SelenideElement firstResultStartButton;

    @Name("Кнопка Принудительный запуск первого элемента таблицы")
    @FindBy(xpath = "//button[@data-e2e='force_start_wave_button_row_0']")
    private SelenideElement firstResultForceStartButton;

    @Name("Кнопка Разрезервировать волну первого элемента таблицы")
    @FindBy(xpath = "//button[@data-e2e='reset_reserve_button_row_0']")
    private SelenideElement firstResultUnreserveWave;

    @Name("Сбросить фильтры")
    @FindBy(xpath = "//button[@data-e2e='reset_table_filters']")
    private SelenideElement resetTableFilters;

    @Name("Статус волны первого элемента таблицы")
    @FindBy(xpath = "//td[@data-e2e='status_cell_row_0']")
    private SelenideElement firstResultWaveStatus;

    @Name("Поле фильтрации по ID волны")
    @FindBy(xpath = waveIdFieldXpath)
    private SelenideElement waveIdField;

    @Name("Первый чекбокс в результатах поиска")
    @FindBy(xpath = "//input[@data-e2e='checkbox_checkbox_0']")
    private SelenideElement firstCheckbox;

    final String waveIdFieldXpath = "//div[@data-e2e='waveId_filter']//input";

    public WavesListPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("wavesList"));
    }

    @Step("Вводим {0} в поле фильтрации по ID волны")
    public WavesListPage inputWaveId(String waveId) {
        waveIdField.shouldBe(enabled, Duration.ofSeconds(40));
        waveIdField.sendKeys(waveId);
        waveIdField.pressEnter();

        wait.until(ExpectedConditions.elementToBeClickable(waveIdField));

        return this;
    }

    @Step("Считываем ID волны из поля фильтрации")
    public String getWaveId() {
        return waveIdField.getAttribute("value");
    }

    @Step("Сбрасываем фильтры")
    public WavesListPage resetTableFilters() {
        resetTableFilters.click();

        return this;
    }

    @Step("Жмем кнопку Показать действия у первого элемента в таблице")
    public WavesListPage firstResultMoreActionsClick() {
        firstResultMoreActionsButton.click();

        return this;
    }

    @Step("Жмем кнопку резервирование волны у первого элемента таблицы")
    public WavesListPage firstResultReserveClick() {
        firstResultReserveButton.click();

        return this;
    }

    @Step("Жмем кнопку разрезервирование волны у первого элемента таблицы")
    public WavesListPage firstResultUnreserveClick() {
        firstResultUnreserveWave.click();

        return this;
    }

    @Step("Жмем кнопку Запуск у первого элемента в таблице")
    public WavesListPage firstResultStartClick() {
        firstResultStartButton.click();
        ModalWindow window = new ModalWindow(driver);
        window.clickSubmit();
        return this;
    }

    @Step("Жмем кнопку Принудительный запуск у первого элемента в таблице")
    public WavesListPage firstResultForceStartClick() {
        firstResultForceStartButton.click();

        return this;
    }

    @Step("Нажимаем кнопку ДА в модальном окне резервирования волны")
    public WavesListPage clickReserveButton() {
        ModalWindow window = new ModalWindow(driver);
        window.clickSubmit();

        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Волна зарезервирована"),
                "Не появилось сообщение о резервировании волны");

        return this;
    }

    @Step("Нажимаем кнопку ДА в модальном окне резервирования волны, ожидаем частичный резерв")
    public PartReservedListModal clickReserveButtonFail() {
        ModalWindow window = new ModalWindow(driver);
        window.clickSubmit();

        var partReservedModal = new PartReservedListModal(driver);
        partReservedModal.waitModalVisible();
        partReservedModal.checkModalCorrect();
        log.info("Волна ожидаемо зарезервировалась частично");
        return partReservedModal;
    }


    @Step("Вводим {0} в поле выбора сортировочной станции и нажимаем кнопку Запустить в модальном окне")
    public WavesListPage inputSortingStationAndClickStartButton(String sortingStation) {
        ForceStartWaveModalWindow window = new ForceStartWaveModalWindow(driver);
        window.inputSortingStation(sortingStation);
        window.clickSubmit();

        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Волна запущена"),
                "Не появилось сообщение о запуске волны");

        return this;
    }

    @Step("Вводим {0} в поле выбора линии консолидации и нажимаем кнопку Запустить в модальном окне")
    public WavesListPage inputConsolidationLineAndClickStartButton(String consolidationLine) {
        ForceStartWaveModalWindow window = new ForceStartWaveModalWindow(driver);
        window.inputConsolidationLine(consolidationLine);
        window.clickSubmit();

        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Волна запущена"),
                "Не появилось сообщение о запуске волны");

        return this;
    }

    @Step("Проверяем статус волны")
    public String checkWaveStatus() {
        return firstResultWaveStatus.getText();

    }

    @Step("Проверяем статус волны")
    public WaveStatus getWaveStatus() {
        return WaveStatus.get(firstResultWaveStatus.getText());
    }

    @Step("Нажимаем кнопку Разрезервировать в модальном окне разрезервирования волны")
    public WavesListPage clickUneserveButton() {
        ModalWindow window = new ModalWindow(driver);
        window.clickSubmit();

        Assertions.assertTrue(notificationDialog.isPresentWithTitle("Волна разрезервирована"),
                "Не появилось сообщение о разрезервировании волны");
        return this;
    }
}
