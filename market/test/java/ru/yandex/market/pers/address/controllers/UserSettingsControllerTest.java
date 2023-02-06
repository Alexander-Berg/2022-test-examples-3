package ru.yandex.market.pers.address.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.market.pers.address.config.PersAddress;
import ru.yandex.market.pers.address.config.TestClient;
import ru.yandex.market.pers.address.controllers.model.AdultDtoResponse;
import ru.yandex.market.pers.address.util.BaseWebTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserSettingsControllerTest extends BaseWebTest {
    @PersAddress
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TestClient testClient;

    @Test
    void testPostAdult() throws Exception {
        testClient.setAdult("uid", "0", true);
    }


    @Test
    void testGetAdultForEmptyData() throws Exception {
        String resultString = testClient.getAdult("uid", "0");

        AdultDtoResponse response = objectMapper.readValue(resultString, AdultDtoResponse.class);
        assertNull(response.getAdult());
        assertNull(response.getConfirmationDate());
    }

    @Test
    void testConfirmationDateIsISO() throws Exception {
        testClient.setAdult("uid", "0", true);

        String resultString = testClient.getAdult("uid", "0");

        JsonNode jsonNode = objectMapper.readTree(resultString);
        assertThat(jsonNode.get("confirmationDate").asText(), containsString("T"));
    }


    @Test
    void testGetAdultForSavedTrueData() throws Exception {
        testClient.setAdult("uid", "0", true);

        String resultString = testClient.getAdult("uid", "0");

        AdultDtoResponse response = objectMapper.readValue(resultString, AdultDtoResponse.class);
        assertTrue(response.getAdult());
        assertNotNull(response.getConfirmationDate());
    }

    @Test
    void testGetAdultForSavedFalseData() throws Exception {
        testClient.setAdult("uid", "0", false);

        String resultString = testClient.getAdult("uid", "0");

        AdultDtoResponse response = objectMapper.readValue(resultString, AdultDtoResponse.class);
        assertFalse(response.getAdult());
        assertNotNull(response.getConfirmationDate());
    }

}
