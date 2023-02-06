package ru.yandex.market.checkout.helpers;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.actual.ActualItem;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.ActualizeParameters;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.pushApi.PushApiConfigurer;
import ru.yandex.market.checkout.util.report.ColoredReportConfigurer;
import ru.yandex.market.checkout.util.report.ReportConfigurer;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.UID;

@WebTestHelper
public class ActualizeHelper extends MockMvcAware {

    private final ColoredReportConfigurer coloredReportConfigurer;
    private final PushApiConfigurer pushApiConfigurer;


    public ActualizeHelper(WebApplicationContext webApplicationContext,
                           TestSerializationService testSerializationService,
                           ColoredReportConfigurer coloredReportConfigurer,
                           PushApiConfigurer pushApiConfigurer) {
        super(webApplicationContext, testSerializationService);
        this.coloredReportConfigurer = coloredReportConfigurer;
        this.pushApiConfigurer = pushApiConfigurer;
    }

    public ActualItem actualizeItem(ActualizeParameters parameters) throws Exception {
        ResultActions resultActions = actualizeItemForActions(parameters);
        MvcResult result = resultActions
                .andExpect(status().isOk())
                .andReturn();

        return testSerializationService.deserializeCheckouterObject(
                result.getResponse().getContentAsString(), ActualItem.class
        );
    }

    public ResultActions actualizeItemForActions(ActualizeParameters parameters) throws Exception {
        Color color = parameters.getActualItem().getRgb();
        ReportConfigurer reportConfigurer = coloredReportConfigurer.getBy(color);

        reportConfigurer.mockReportPlace(MarketReportPlace.OFFER_INFO, parameters.getReportParameters());
        reportConfigurer.mockReportPlace(MarketReportPlace.SHOP_INFO, parameters.getReportParameters());
        reportConfigurer.mockReportPlace(MarketReportPlace.SHOP_INFO, parameters.getReportParameters());
        reportConfigurer.mockReportPlace(MarketReportPlace.MODEL_INFO, parameters.getReportParameters());
        reportConfigurer.mockReportPlace(MarketReportPlace.ACTUAL_DELIVERY, parameters.getReportParameters());
        reportConfigurer.mockOutlets(parameters.getReportParameters());
        if (parameters.isMockPushApi()) {
            pushApiConfigurer.mockCart(parameters.getOrder(),
                    parameters.getPushApiDeliveryResponses(), false
            );
        }

        return mockMvc.perform(post("/actualize")
                .param(UID, Long.toString(parameters.getUid()))
                .param(CheckouterClientParams.PERK_PROMO_ID, parameters.getPerkPromoId())
                .param(CheckouterClientParams.RGB, color == null ? null : color.name())
                .content(testSerializationService.serializeCheckouterObject(parameters.getActualItem()))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andDo(log());
    }

    public MockHttpServletRequestBuilder createRequestBuilder() {
        return post("/actualize?uid={uid}", 123)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" +
                        "  \"buyerRegionId\": 2," +
                        "  \"shopId\": 4545," +
                        "  \"feedId\": 200305173," +
                        "  \"offerId\": \"4\"" +
                        "}");
    }

}
