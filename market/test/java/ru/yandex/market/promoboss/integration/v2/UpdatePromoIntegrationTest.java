package ru.yandex.market.promoboss.integration.v2;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.integration.AbstractInstantIntegrationTest;
import ru.yandex.mj.generated.server.model.Error;
import ru.yandex.mj.generated.server.model.PromoRequestV2;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DbUnitDataSet(before = "UpdatePromoIntegrationTest.csv")
class UpdatePromoIntegrationTest extends AbstractInstantIntegrationTest {

    private static final long NOW_EPOCH_SECONDS = 1654617851L;

    public static final String url = "/api/v2/promos/update";

    @Override
    protected long getNowEpochSecond() {
        return NOW_EPOCH_SECONDS;
    }

    @Test
    @DbUnitDataSet(after = "UpdatePromoIntegrationTest.updatePromoOk.after.csv")
    void updatePromoOk() throws Exception {
        PromoRequestV2 promoRequest = IntegrationPromoV2Utils.buildUpdatePromoRequest();

        mockMvc.perform(
                        MockMvcRequestBuilders.post(url)
                                .contentType("application/json")
                                .content(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(promoRequest))
                )
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void updatePromoBadRequest() throws Exception {
        PromoRequestV2 promoRequest = IntegrationPromoV2Utils.buildUpdatePromoRequest();
        promoRequest.getSuppliersConstraints().setExclude(null);

        Error error = new Error()
                .message("Validation type=ConstraintListsValidationException, Suppliers.exclude is empty");

        mockMvc.perform(
                        MockMvcRequestBuilders.post(url)
                                .contentType("application/json")
                                .content(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(promoRequest))
                )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(
                        MockMvcResultMatchers.content()
                                .json(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(error))
                );
    }

    @Test
    void updatePromoNotFound() throws Exception {
        PromoRequestV2 promoRequest = IntegrationPromoV2Utils.buildUpdatePromoRequest()
                .promoId("cf_999999");

        mockMvc.perform(
                        MockMvcRequestBuilders.post(url)
                                .contentType("application/json")
                                .content(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(promoRequest))
                )
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string("")
                );
    }

    @Test
    void updatePromoInternalServerError() throws Exception {
        PromoRequestV2 promoRequest = IntegrationPromoV2Utils.buildUpdatePromoRequest();

        promoRequest.getSrc().getCiface().getPromotions().get(0).id("-123");

        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.post(url)
                                .contentType("application/json")
                                .content(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(promoRequest))
                )
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().startsWith("Failed to execute DbAction.UpdateRoot(entity=CifacePromotion"));
    }
}
