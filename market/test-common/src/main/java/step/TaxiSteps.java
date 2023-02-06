package step;

import client.TaxiClient;
import io.qameta.allure.Step;

public class TaxiSteps {

    private static final TaxiClient TAXI_CLIENT = new TaxiClient();

    @Step("Вызов курьера")
    public void transferActivate(String transferId, String requestId) {
        TAXI_CLIENT.transferActivate(transferId, requestId);
    }
}
