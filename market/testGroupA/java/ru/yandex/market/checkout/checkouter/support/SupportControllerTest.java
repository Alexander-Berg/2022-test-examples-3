package ru.yandex.market.checkout.checkouter.support;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolver;
import ru.yandex.market.checkout.checkouter.feature.PutFeatureRequest;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentBooleanFeatureType;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SupportControllerTest extends AbstractWebTestBase {

    @Autowired
    private TestSerializationService testSerializationService;
    @Autowired
    private CheckouterFeatureResolver checkouterFeatureResolver;

    @AfterEach
    public void cleanup() {
        client.internalSupport().putExpirationSwitchedOff(false);
    }

    @DisplayName("Ручка мониторинга должна возвращать 200, если протухание включено.")
    @Test
    public void shouldReturnOkIfExpirationEnabled() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.EXPIRATION_SWITCHED_OFF, false);

        mockMvc.perform(get("/monitor/expiration-switched-off"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(false));
    }

    @DisplayName("Ручка мониторинга должна возвращать 500, если протухание выключено.")
    @Test
    public void shouldReturn500IfExpirationDisabled() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.EXPIRATION_SWITCHED_OFF, true);

        mockMvc.perform(get("/monitor/expiration-switched-off"))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.value").value(true));
    }

    @DisplayName("Проверяем, что редактирование перманентных свойств работает")
    @Test
    public void shouldEditPermanentFeatures() throws Exception {
        checkouterFeatureResolver.writeValue(PermanentBooleanFeatureType.SKIP_DISCOUNT_CALCULATION_ENABLED, false);
        mockMvc.perform(get("/permanent-features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.featureName=='skipDiscountCalculationEnabled')].payload").value("false"));

        mockMvc.perform(put("/permanent-features/skipDiscountCalculationEnabled")
                .content(testSerializationService.serializeCheckouterObject(new PutFeatureRequest("true", "-", "-")))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[?(@.featureName=='skipDiscountCalculationEnabled')].payload").value("true"));
    }

}
