package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.api.health;

import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ServicesPing;

@DisplayName("API: Ping")
@Epic("Ping")
@Slf4j
public class ServicesAvailabilityTest {
    private final ServicesPing multitestingPing = new ServicesPing();

    @Test
    @DisplayName("Multitesting API availability")
    public void apiPingTest() {
        log.info("Testing API availability");

        String response = multitestingPing.apiPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting Auth availability")
    public void authPingTest() {
        log.info("Testing Auth availability");

        String response = multitestingPing.authPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting yt-util availability")
    @Tag("notForMultitesting")
    public void ytUtilPingTest() {
        log.info("Testing yt-util availability");

        String response = multitestingPing.ytUtilPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting WcsEmulator availability")
    public void wcsEmulatorPingTest() {
        log.info("Testing WcsEmulator availability");

        String response = multitestingPing.wcsEmulatorPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting DataCreator availability")
    public void dataCreatorPingTest() {
        log.info("Testing DataCreator availability");

        String response = multitestingPing.datacreatorPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting TaskRouter availability")
    @Tag("notForMultitesting")
    public void taskRouterPingTest() {
        log.info("Testing TaskRouter availability");

        String response = multitestingPing.taskrouterPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting Consolidation availability")
    public void consolidationPingTest() {
        log.info("Testing Consolidation availability");

        String response = multitestingPing.consolidationPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting Dropping availability")
    public void droppingPingTest() {
        log.info("Testing Dropping availability");

        String response = multitestingPing.droppingPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting Placement availability")
    public void placementPingTest() {
        log.info("Testing Placement availability");

        String response = multitestingPing.placementPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting Packing availability")
    public void packingPingTest() {
        log.info("Testing Packing availability");

        String response = multitestingPing.packingPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting Reporter availability")
    public void reporterPingTest() {
        log.info("Testing Reporter availability");

        String response = multitestingPing.reporterPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting Picking availability")
    public void pickingPingTest() {
        log.info("Testing Picking availability");

        String response = multitestingPing.pickingPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting Receiving availability")
    public void receivingPingTest() {
        log.info("Testing Receiving availability");

        String response = multitestingPing.receivingPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting Shipping availability")
    public void shippingPingTest() {
        log.info("Testing Shipping availability");

        String response = multitestingPing.shippingPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting Inventorization availability")
    public void inventorizationPingTest() {
        log.info("Testing Inventorization availability");

        String response = multitestingPing.inventorizationPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting Autostart availability")
    public void autostartPingTest() {
        log.info("Testing Autostart availability");

        String response = multitestingPing.autostartPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting Replenishment availability")
    public void replenishmentPingTest() {
        log.info("Testing Replenishment availability");

        String response = multitestingPing.replenishmentPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting Core availability")
    public void corePingTest() {
        log.info("Testing Core availability");

        String response = multitestingPing.corePing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting ShippingSorter availability")
    public void shippingSorterPingTest() {
        log.info("Testing ShippingSorter availability");

        String response = multitestingPing.shippingsorterPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting ServiceBus availability")
    public void serviceBusPingTest() {
        log.info("Testing ServiceBus availability");

        String response = multitestingPing.servicebusPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting SchedulerV2 availability")
    public void schedulerv2PingTest() {
        log.info("Testing SchedulerV2 availability");

        String response = multitestingPing.schedulerv2Ping().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting OrderManagement availability")
    public void orderManagementPingTest() {
        log.info("Testing OrderManagement availability");

        String response = multitestingPing.ordermanagementPing().extract().asString();

        log.info("Response: {}", response);
    }

    @Test
    @DisplayName("Multitesting Transportation availability")
    public void transportationPingTest() {
        log.info("Testing Transportation availability");

        String response = multitestingPing.transportationPing().extract().asString();

        log.info("Response: {}", response);
    }
}
