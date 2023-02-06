package step;

import java.time.LocalDate;

import client.WmsClient;
import io.qameta.allure.Step;

public class WmsSteps {
    private static final WmsClient WMS_CLIENT = new WmsClient();

    @Step("Поменять дату отгрузки на скаде")
    public void changeShipmentDate(String orderKey, LocalDate shipDate) {
        WMS_CLIENT.changeShipmentDate(orderKey, shipDate);
    }

    @Step("Запустить шедулер смены даты на складе")
    public void calculateAndUpdateOrdersStatus() {
        WMS_CLIENT.calculateAndUpdateOrdersStatus();
    }
}
