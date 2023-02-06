package ru.yandex.market.logistics.nesu.base.partner;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractPartnerShipmentStatusesTest extends AbstractContextualTest {

    @Autowired
    private FeatureProperties featureProperties;

    @Test
    @DisplayName("Получение списка статусов отгрузок")
    void getPartnerShipmentStatuses() throws Exception {
        mockMvc.perform(get(url()))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/statuses.json"));
    }

    @Nonnull
    protected abstract String url();
}
