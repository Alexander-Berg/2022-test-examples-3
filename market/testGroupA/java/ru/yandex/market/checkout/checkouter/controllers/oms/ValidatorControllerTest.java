package ru.yandex.market.checkout.checkouter.controllers.oms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.viewmodel.CisValidationResultResponse;
import ru.yandex.market.checkout.checkouter.viewmodel.CisesValidationResponse;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ValidatorControllerTest extends AbstractWebTestBase {

    @Autowired
    private TestSerializationService testSerializationService;

    @Test
    @DisplayName("Передача в запросе двух валидных КИЗ-ов: с криптохвостом и без")
    public void shouldValidateCises() throws Exception {
        String response = mockMvc.perform(post("/validators/cisValidator")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cises\": [\"0104601662000016215RNef*\\u001d93B0Ik\", \"0104601662000016215RNef*\"]}")
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        CisesValidationResponse cisesValidationResponse =
                testSerializationService.deserializeCheckouterObject(response, CisesValidationResponse.class);
        for (CisValidationResultResponse resultResponse : cisesValidationResponse.getCisValidationResultResponse()) {
            assertTrue(resultResponse.isValid());
        }
    }

}
