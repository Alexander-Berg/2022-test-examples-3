package ru.yandex.market.wms.packing.controller;

import java.util.Collections;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PackingNotificationControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/controller/packing-notification/before/before.xml")
    public void listPackingNotification() throws Exception {
        assertHttpCall(
                get("/packing-notification"),
                status().isOk(),
                Collections.emptyMap(),
                "controller/packing-notification/response/list.json"
        );
    }

    @Test
    @DatabaseSetup("/controller/packing-notification/before/before.xml")
    public void listPackingNotificationWithFilter() throws Exception {
        assertHttpCall(
                get("/packing-notification"),
                status().isOk(),
                Map.of(
                        "filter", "CARRIERCODE==3331 or CARRIERCODE==3332 or CARRIERCODE==3333 or CARRIERCODE==3334",
                        "limit", "2",
                        "offset", "1",
                        "sort", "CARRIERCODE",
                        "order", "ASC"
                ),
                "controller/packing-notification/response/list-filtered.json"
        );
    }

    @Test
    @DatabaseSetup("/controller/packing-notification/before/before.xml")
    public void getPackingNotification() throws Exception {
        assertHttpCall(
                get("/packing-notification/102"),
                status().isOk(),
                Collections.emptyMap(),
                "controller/packing-notification/response/get-one.json"
        );
    }

    @Test
    @DatabaseSetup("/controller/packing-notification/before/before.xml")
    @ExpectedDatabase(
            value = "/controller/packing-notification/after/created.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void createPackingNotification() throws Exception {
        assertHttpCall(
                post("/packing-notification"),
                status().isCreated(),
                "controller/packing-notification/request/create.json",
                "controller/packing-notification/response/create.json"
        );
    }

    @Test
    @DatabaseSetup("/controller/packing-notification/before/before.xml")
    @ExpectedDatabase(
            value = "/controller/packing-notification/after/created-with-with-dimensions.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void createPackingNotificationWithDimensions() throws Exception {
        assertHttpCall(
                post("/packing-notification"),
                status().isCreated(),
                "controller/packing-notification/request/create-with-dimensions.json",
                "controller/packing-notification/response/create-with-dimensions.json"
        );
    }

    @Test
    @DatabaseSetup("/controller/packing-notification/before/before.xml")
    @ExpectedDatabase(
            value = "/controller/packing-notification/after/updated.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void updatePackingNotification() throws Exception {
        assertHttpCall(
                put("/packing-notification/104"),
                status().isOk(),
                "controller/packing-notification/request/update.json",
                "controller/packing-notification/response/update.json"
        );
    }

    @Test
    @DatabaseSetup("/controller/packing-notification/before/before.xml")
    @ExpectedDatabase(
            value = "/controller/packing-notification/after/deleted.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void deletePackingNotification() throws Exception {
        assertHttpCall(
                delete("/packing-notification/104"),
                status().isNoContent()
        );
    }

}
