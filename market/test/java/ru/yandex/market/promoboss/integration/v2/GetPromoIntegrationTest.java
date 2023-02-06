package ru.yandex.market.promoboss.integration.v2;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.integration.AbstractIntegrationTest;
import ru.yandex.market.promoboss.integration.IntegrationPromoUtils;
import ru.yandex.mj.generated.server.model.PromoResponseV2;

@DbUnitDataSet(before = "GetPromoIntegrationTest.csv")
class GetPromoIntegrationTest extends AbstractIntegrationTest {
    public static final String url = "/api/v2/promos";

    @Test
    void getPromoSuccess() throws Exception {
        PromoResponseV2 expectedResponseDto = IntegrationPromoV2Utils.buildPromoResponse();

        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(url)
                                .param("id", IntegrationPromoUtils.PROMO_ID)
                )
                .andExpect(MockMvcResultMatchers.status().is2xxSuccessful())
                .andExpect(
                        MockMvcResultMatchers.content()
                                .json(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(expectedResponseDto))
                );
    }

    @Test
    void getPromoNotFound() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(url)
                                .param("id", "NotFoundPromoId")
                )
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void getPromoBadRequest() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(url)
                                .param("id", "")
                )
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    @DbUnitDataSet(before = {"GetPromoIntegrationTest.InternalServerError.csv"})
    void getPromoInternalServerError() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .get(url)
                                .param("id", IntegrationPromoUtils.PROMO_ID)
                )
                .andExpect(MockMvcResultMatchers.status().isInternalServerError());
    }
}

