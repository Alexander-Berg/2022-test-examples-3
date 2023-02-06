package ru.yandex.market.wms.api.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class DeliveryServiceControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/delivery-service/happy-path/init_db.xml")
    @ExpectedDatabase(value = "/delivery-service/happy-path/init_db.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getDeliveryServicesHappyPath() throws Exception {
        mockMvc.perform(get("/delivery-services")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("delivery-service/happy-path/response.json"), false));
    }

    @Test
    @DatabaseSetup("/delivery-service/empty/init_db.xml")
    @ExpectedDatabase(value = "/delivery-service/empty/init_db.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getDeliveryServicesEmpty() throws Exception {
        mockMvc.perform(get("/delivery-services")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("delivery-service/empty/response.json")));
    }
}
