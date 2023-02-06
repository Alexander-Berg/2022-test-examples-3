package ru.yandex.market.logistics.nesu.base.partner;

import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.client.enums.PageSize;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentLabelRequest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractPartnerShipmentGenerateLabelsTest extends AbstractContextualTest {

    @Test
    @DisplayName("Успех")
    void success() throws Exception {
        PartnerShipmentLabelRequest request = PartnerShipmentLabelRequest.builder()
            .shipmentIds(List.of(300L, 310L))
            .orderIds(List.of(400L, 410L))
            .pageSize(PageSize.A4)
            .build();

        mockMvc.perform(
            request(HttpMethod.PUT, url(), request)
                .param("userId", "100")
                .param("shopId", "200")
        )
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(
                content()
                    .string("100|200|PartnerShipmentLabelRequest("
                        + "shipmentIds=[300, 310], "
                        + "orderIds=[400, 410], "
                        + "pageSize=A4)")
            );
    }

    @Nonnull
    protected abstract String url();

}
