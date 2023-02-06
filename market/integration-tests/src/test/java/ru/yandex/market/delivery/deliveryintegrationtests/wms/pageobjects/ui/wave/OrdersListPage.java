package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.wave;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OrderStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.selenium.WebDriverTimeout;
import ru.yandex.qatools.htmlelements.annotations.Name;

import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;

public class OrdersListPage extends AbstractPage {

    @Name("Кнопка Создать волну")
    @FindBy(xpath = "//button[@data-e2e='header_create_wave_button_row_0']")
    private SelenideElement createWaveButton;

    @Name("Кнопка Сбросить фильтры")
    @FindBy(xpath = "//button[@data-e2e='reset_table_filters']")
    private SelenideElement resetFiltersButton;

    @Name("Кнопка Выбрать период")
    @FindBy(xpath = "//button[@data-e2e='scheduledShipDate_filter']")
    private SelenideElement scheduledShipDateFilterButton;

    @Name("Кнопка фильтр Статус")
    @FindBy(xpath = "//button[@data-e2e='status_filter']")
    private SelenideElement statusFilterButton;

    @Name("Поле фильтрации по номеру заказа")
    @FindBy(xpath = orderIdFieldXpath)
    private SelenideElement orderIdField;

    final String orderIdFieldXpath = "//div[@data-e2e='orderId_filter']//input";

    String filterCheckboxXpath = "//div[@data-e2e='checkbox_filter_sort']//input";

    String waveIdFieldXpath = "//a[@data-e2e='waveKey_row_link_select']";

    String firstResultOrderStatusXpath = "//td[@data-e2e='status_cell_row_0']//span/span";
    String firstResultOrderTotalQtyXpath = "//td[@data-e2e='totalQty_cell_row_0']//div/span";

    String externalOrderIdInputXpath = "//div[@data-e2e='externalOrderId_filter']//input";

    String rowOrderResultXpath = "//tr[contains(@data-e2e, 'row_row')]";

    @Name("Поле Корпус")
    @FindBy(xpath = "//td[@data-e2e='building_cell_row_0']/div/span")
    private SelenideElement buildingField;

    public OrdersListPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("ordersList"));
    }

    @Step("Вводим {0} в поле фильтрации по номеру заказа")
    public OrdersListPage inputOrderId(String orderId) {
        orderIdField.shouldBe(enabled, Duration.ofSeconds(20));
        orderIdField.sendKeys(orderId);
        orderIdField.pressEnter();

        wait.until(ExpectedConditions.elementToBeClickable(orderIdField));

        return this;
    }

    @Step("Выбираем все элементы в таблице")
    public OrdersListPage selectAllResults() {
        $(byXpath(filterCheckboxXpath)).click();
        return this;
    }

    @Step("Проверяем, что фильтр сбросился")
    private void checkFilterIsReset() {
        scheduledShipDateFilterButton.shouldHave(text("Выбрать период"));
        statusFilterButton.shouldHave(text("Выбрано 0"));
    }

    @Step("Жмем кнопку Сбросить фильтры")
    public OrdersListPage resetFiltersClick() {
        waitTablePreloader();
        resetFiltersButton.shouldBe(enabled).click();
        checkFilterIsReset();
        return this;
    }

    @Step("Жмем кнопку Создать волну")
    public OrdersListPage createWaveClick() {
        createWaveButton.click();

        return this;
    }

    @Step("Нажимаем кнопку Создать в модальном окне")
    public WavesListPage clickCreateButton() {
        ModalWindow window = new ModalWindow(driver);
        window.waitModalVisible();
        window.clickSubmit();

        return new WavesListPage(driver);
    }

    //TODO Доделать кейс
    @Step("Проверяем что поле Корпус не null")
    public void checkBuilding() {
        if (buildingField.getText() == null) {
            throw new RuntimeException("Order without building!");
        }
    }

    @Step("Считываем ID волны из поля фильтрации")
    public String getWaveId() {
        checkResultsNumber(1,
                "Неверное количество результатов в списке заказов при получении id волны");
        return $(byXpath(waveIdFieldXpath)).getText();
    }

    @Step("Считываем статус заказа из результатов")
    public OrderStatus getOrderStatus() {
        checkResultsNumber(1,
                "Неверное количество результатов в списке заказов при получении статуса заказа");
        SelenideElement orderStatus = $(byXpath(firstResultOrderStatusXpath));
        return OrderStatus.get(orderStatus.getText());
    }

    @Step("Считываем totalQty заказа из результатов")
    public int getOrderTotalQty() {
        checkResultsNumber(1,
                "Неверное количество результатов в списке заказов при получении totalQty заказа");
        SelenideElement cell = $(byXpath(firstResultOrderTotalQtyXpath));
        return Integer.valueOf(cell.getText());
    }

    @Step("Вводим номер внешнего заказа - {externalOrderId}")
    public OrdersListPage inputExternalOrderId(long externalOrderId) {
        SelenideElement orderIdField = $(byXpath(externalOrderIdInputXpath));
        orderIdField.sendKeys(String.valueOf(externalOrderId));
        orderIdField.pressEnter();

        wait.until(ExpectedConditions.elementToBeClickable(byXpath(externalOrderIdInputXpath)));

        return this;
    }

    @Step("Получаем fulfillmentId заказа из строки с индексом {rowIndex}")
    public String getOrderFulfillmentIdByRowIndex(int rowIndex) {
        checkResultsNumber(2,
                "Неверное количество результатов в списке заказов при разделении заказов");
        final By orderFulfillmentId = byXpath(String.format("//td[@data-e2e='orderId_cell_row_%d']", rowIndex));
        return $(orderFulfillmentId).getText();
    }

    @Step("Проверяем количество результатов в списке заказов")
    private void checkResultsNumber(int expectedResultsNumber, String errorMessage) {
        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.MEDIUM_WAIT_TIMEOUT, TimeUnit.SECONDS);
        int actualResultsNumber = $$(byXpath(rowOrderResultXpath)).size();
        driver.manage().timeouts().implicitlyWait(WebDriverTimeout.LONG_WAIT_TIMEOUT, TimeUnit.SECONDS);

        Assertions.assertEquals(expectedResultsNumber, actualResultsNumber, errorMessage);
    }
}
