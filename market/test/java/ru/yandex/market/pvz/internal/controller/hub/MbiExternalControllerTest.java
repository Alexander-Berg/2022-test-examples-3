package ru.yandex.market.pvz.internal.controller.hub;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class MbiExternalControllerTest extends BaseShallowTest {

    private final TestPickupPointFactory pickupPointFactory;

    @Test
    void createLegalPartner() throws Exception {
        var pickupPoint = pickupPointFactory.createPickupPoint();

        mockMvc.perform(
                get("/v1/pi/hub?campaignId=" + pickupPoint.getPvzMarketId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(String.format(
                        getFileContent("hub/response_get_hub_by_campaign_id.json"),
                        pickupPoint.getId(),
                        pickupPoint.getPvzMarketId())));
    }
}
