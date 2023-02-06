package ru.yandex.market.promoboss.integration.v2;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.integration.AbstractInstantIntegrationTest;
import ru.yandex.market.request.trace.RequestTraceUtil;
import ru.yandex.mj.generated.server.model.Error;
import ru.yandex.mj.generated.server.model.PromoRequestV2;

class CreatePromoIntegrationTest extends AbstractInstantIntegrationTest {
    private static final String url = "/api/v2/promos/create";

    private static final long NOW_EPOCH_SECONDS = 1654517851L;

    @Override
    protected long getNowEpochSecond() {
        return NOW_EPOCH_SECONDS;
    }

    @Test
    @DbUnitDataSet(after = "CreatePromoIntegrationTest.createPromoCreated.after.csv")
    void createPromoCreated() throws Exception {
        PromoRequestV2 promoRequest = IntegrationPromoV2Utils.buildCreatePromoRequest();

        mockMvc.perform(
                        MockMvcRequestBuilders.post(url)
                                .contentType("application/json")
                                .content(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(promoRequest))
                                .header(RequestTraceUtil.REQUEST_ID_HEADER, "requestId")
                )
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    void createPromoBadRequest() throws Exception {
        PromoRequestV2 promoRequest = IntegrationPromoV2Utils.buildCreatePromoRequest();
        promoRequest.getMskusConstraints().setExclude(null);

        Error error = new Error()
                .message("Validation type=ConstraintListsValidationException, Mskus.exclude is empty");

        mockMvc.perform(
                        MockMvcRequestBuilders.post(url)
                                .contentType("application/json")
                                .content(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(promoRequest))
                                .header(RequestTraceUtil.REQUEST_ID_HEADER, "requestId")
                )
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(
                        MockMvcResultMatchers.content()
                                .json(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(error))
                );
    }

    @Test
    void createPromoInternalServerError() throws Exception {
        PromoRequestV2 promoRequest = IntegrationPromoV2Utils.buildCreatePromoRequest();
        promoRequest.getSrc().getCiface().getPromotions().get(0).setId("123");

        String error = "CifacePromotion.id is not null!";

        mockMvc.perform(
                        MockMvcRequestBuilders.post(url)
                                .contentType("application/json")
                                .content(objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(promoRequest))
                                .header(RequestTraceUtil.REQUEST_ID_HEADER, "requestId")
                )
                .andExpect(MockMvcResultMatchers.status().isInternalServerError())
                .andExpect(
                        MockMvcResultMatchers.content().string(error)
                );
    }
}
