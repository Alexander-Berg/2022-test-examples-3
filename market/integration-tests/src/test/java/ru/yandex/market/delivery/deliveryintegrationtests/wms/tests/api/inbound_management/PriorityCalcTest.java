package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.api.inbound_management;

import io.qameta.allure.Epic;
import io.restassured.response.ValidatableResponse;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.InboundManagementClient;

@DisplayName("Inbound Management API")
@Epic("API Tests")
@Slf4j
public class PriorityCalcTest {

    private final InboundManagementClient inboundManagementClient = new InboundManagementClient();

    @Test
    @DisplayName("Calculate priorities")
    public void getInboundStatusTest() {
        log.info("Testing calculate priorities");
        ValidatableResponse response = inboundManagementClient.calculatePriorities();
        response.statusCode(200);
        response.body("inserted", Matchers.any(Integer.class)).body("updated", Matchers.any(Integer.class));
    }

}
