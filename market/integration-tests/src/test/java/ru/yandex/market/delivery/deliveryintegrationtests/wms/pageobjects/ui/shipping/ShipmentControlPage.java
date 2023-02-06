package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.shipping;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Assertions;
import static com.codeborne.selenide.Selectors.byXpath;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ShipmentType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.ModalWindow;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.common.TablePreloader;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class ShipmentControlPage extends AbstractPage {
    @FindBy(xpath = "//button[@data-e2e='button_row_0_header']")
    private SelenideElement addShipmentButton;
    @FindBy(xpath = "//div[@data-e2e='shippingVehicle']//input")
    private SelenideElement vehicleInput;
    @FindBy(xpath = "//div[@data-e2e='outbound']//input")
    private SelenideElement vehicleSelectorInput;
    @FindBy(xpath = "//div[@data-e2e='shippingType']")
    private SelenideElement typeSelector;
    @FindBy(xpath = "//div[@data-e2e='shippingCarrierCode']")
    private SelenideElement carrierSelector;
    @FindBy(xpath = "//div[@data-e2e='shippingWithdrawalIds']//input")
    private SelenideElement withdrawalIdsInput;
    @FindBy(xpath = "//div[@data-e2e='shippingDoor']")
    private SelenideElement doorSelector;
    @FindBy(xpath = "//div[@data-e2e='time']//input")
    private SelenideElement scheduledDepartureTimeInput;
    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement createShipmentForwardButton;
    @FindBy(xpath = "//div[@data-e2e='vehicle_filter']//input")
    private SelenideElement vehicleFilterInput;
    @FindBy(xpath = "//button[@data-e2e='carrierCodes_filter']")
    private SelenideElement carrierFilterSelector;
    @FindBy(xpath = "//button[@data-e2e='carrierCodes_filter_select-submit-button']")
    private SelenideElement carrierFilterSubmitButton;
    @FindBy(xpath = "//div[@data-e2e='door_filter']//input")
    private SelenideElement doorFilterInput;
    @FindBy(xpath = "//button[@data-e2e='status_filter']")
    private SelenideElement statusFilterSelector;
    @FindBy(xpath = "//div[@data-e2e='status_filter-selectAll_Новый']")
    private SelenideElement statusFilterNew;
    @FindBy(xpath = "//button[@data-e2e='status_filter_select-submit-button']")
    private SelenideElement statusFilterSubmitButton;
    @FindBy(xpath = "//div[@data-e2e='withdrawalIds_filter']//input")
    private SelenideElement withdrawalIdsFilterInput;
    @FindBy(xpath = "//div[@data-e2e='shipmentId_filter']//input")
    private SelenideElement shipmentIdFilterInput;
    @FindBy(xpath = "//button[@data-e2e='button_forward']")
    private SelenideElement closeShipmentConfirmationButton;
    @FindBy(xpath = "//td[starts-with(@data-e2e,'shipmentId_cell')]//span")
    private SelenideElement shipmentId;

    private final TablePreloader tablePreloader;

    public ShipmentControlPage(WebDriver driver) {
        super(driver);
        wait.until(ExpectedConditions.urlMatches("mainPage"));
        this.tablePreloader = new TablePreloader(driver);
    }

    @Step("Создаем отгрузку (стандартная)")
    public Long createStandardShipment(String vehicle, String carrier, String door, String scheduledDepartureTime) {
        selectShipmentType(ShipmentType.STANDARD);
        //TODO ask UI to add data-e2e to close menu button
        inputValueIntoFieldByPath(vehicleSelectorInput, vehicle);
        selectValueFromMultiSelector(carrierSelector, "shippingCarrierCode-selectAll_" + carrier, "Служба доставки");
        selectValueFromSelector(doorSelector, "shippingDoor-selectAll_" + door);
        inputValueIntoFieldByPath(scheduledDepartureTimeInput, scheduledDepartureTime);
        new ModalWindow(driver).waitModalHidden();
        filterTableBy(vehicle, carrier, door, String.join(", ", Collections.emptyList()));
        return getShipmentId();
    }

    @Step("Создаем отгрузку (изъятие)")
    public Long createWithdrawalShipment(String vehicle, String withdrawalId, String door) {
        selectShipmentType(ShipmentType.WITHDRAWAL);
        inputValueIntoFieldByPath(withdrawalIdsInput, withdrawalId);
        return createShipmentCommon(vehicle, null, door, withdrawalId);
    }

    @Step("Создаем отгрузку (изъятие)")
    public Long createWithdrawalShipment(String vehicle, Collection<String> withdrawalIds, String door) {
        selectShipmentType(ShipmentType.WITHDRAWAL);
        withdrawalIds.forEach(withdrawalId -> inputValueIntoFieldByPath(withdrawalIdsInput, withdrawalId));
        return createShipmentCommon(vehicle, null, door, withdrawalIds);
    }

    @Step("Выбираем тип отгрузки")
    private void selectShipmentType(ShipmentType type) {
        Retrier.retryInteractionWithElement(() -> {
            addShipmentButton.click();
            new ModalWindow(driver).waitModalVisible();
        });
        selectValueFromSelector(typeSelector, "shippingType-selectAll_" + type.getValue());
    }

    @Step("Создаем отгрузку")
    private Long createShipmentCommon(String vehicle, String carrier, String door, String withdrawalId) {
        return createShipmentCommon(vehicle, carrier, door, List.of(withdrawalId));
    }

    @Step("Создаем отгрузку")
    private Long createShipmentCommon(String vehicle, String carrier, String door, Collection<String> withdrawalIds) {
        inputValueIntoFieldByPath(vehicleInput, vehicle);
        selectValueFromSelector(doorSelector, "shippingDoor-selectAll_" + door);
        createShipmentForwardButton.click();
        filterTableBy(vehicle, carrier, door, String.join(", ", withdrawalIds));
        return getShipmentId();
    }

    @Step("Завершаем отгрузку")
    public void closeShipment(Long shipmentId) {
        final By finishShipmentButton = byXpath(String.format(
                "//td[@data-e2e='custom_cell_%s']//button[@data-e2e='finishShippment']", shipmentId
        ));
        inputValueIntoFieldByPath(shipmentIdFilterInput, shipmentId.toString());
        waitForOnlyOneRowInTable();
        Assertions.assertEquals(shipmentId, getShipmentId());
        $(finishShipmentButton).click();
        closeShipmentConfirmationButton.click();
        notificationDialog.isPresentWithTitle("Отгрузка завершена");
    }

    @Step("Вводим значение: {value}")
    private void inputValueIntoFieldByPath(SelenideElement input, String value) {
        input.click();
        performInputInActiveElement(input, value);
        tablePreloader.waitUntilHidden();
    }

    private void selectValueFromSelector(SelenideElement selector, String value) {
        if (!value.equals(selector.getAttribute("value"))) {
            selector.click();
            $(byXpath(String.format("//button[@data-e2e='%s']", value))).click();
        }
    }

    private void selectValueFromMultiSelector(SelenideElement selector, String value, String label) {
        selector.click();
        SelenideElement carrierCheckBox = $(byXpath(String.format("//div[@data-e2e='%s']//input", value)));
        if (carrierCheckBox.getAttribute("checked") != null) {
            return;
        }
        carrierCheckBox.click();
        $(byXpath(String.format("//label//div[text()='%s']", label))).click();
    }

    private void filterTableBy(String vehicle, String carrier, String door, String withdrawalId) {
        final By carrierInDropDown = byXpath(String.format("//div[@data-e2e='carrierCodes_filter-selectAll_%s']", carrier));
        inputValueIntoFieldByPath(vehicleFilterInput, vehicle);
        if (carrier != null) {
            carrierFilterSelector.click();
            $(carrierInDropDown).click();
            carrierFilterSubmitButton.click();
        }
        inputValueIntoFieldByPath(doorFilterInput, door);
        statusFilterSelector.click();
        statusFilterNew.click();
        statusFilterSubmitButton.click();
        if (withdrawalId != null) {
            inputValueIntoFieldByPath(withdrawalIdsFilterInput, withdrawalId);
        }
    }

    private Long getShipmentId() {
        return Long.valueOf(shipmentId.getText());
    }

    private void waitForOnlyOneRowInTable() {
        Retrier.retry(() -> {
                    int rowCount = $$(byXpath("//tr[contains(@data-e2e, 'row')]")).size();
                    Assertions.assertEquals(rowCount, 1, "Количество строк в таблице больше 1");
                }, Retrier.RETRIES_SMALL, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS
        );
    }
}
