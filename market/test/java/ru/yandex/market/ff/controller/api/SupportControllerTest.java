package ru.yandex.market.ff.controller.api;

import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundStatus;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.InboundStatusHistory;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Status;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StatusCode;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class SupportControllerTest extends MvcIntegrationTest {

    private static final Long REQ_ID_1 = 1L;

    private static final String REQ_EXT_ID_1 = "11";
    private static final String X_DOC_REQ_EXT_ID = "22";

    private static final ResourceId RES_ID_1 = ResourceId.builder()
            .setYandexId(REQ_ID_1.toString())
            .setPartnerId(REQ_EXT_ID_1)
            .build();

    private static final ResourceId X_DOC_RES_ID = ResourceId.builder()
            .setYandexId(REQ_ID_1.toString())
            .setPartnerId(X_DOC_REQ_EXT_ID)
            .build();

    private static final long SERVICE_ID_1 = 555;
    private static final long X_DOC_SERVICE_ID = 666;

    private static final Partner PARTNER_1 = new Partner(SERVICE_ID_1);
    private static final Partner X_DOC_PARTNER = new Partner(X_DOC_SERVICE_ID);

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @BeforeEach
    void initMocks() {

        when(fulfillmentClient.getInboundsStatus(Collections.singletonList(RES_ID_1), PARTNER_1)).thenReturn(
                Collections.singletonList(inboundStatus(RES_ID_1, StatusCode.CANCELLED, "2000-01-05T00:00:00")));
        when(fulfillmentClient.getInboundsStatus(Collections.singletonList(X_DOC_RES_ID), X_DOC_PARTNER)).thenReturn(
                Collections.singletonList(inboundStatus(X_DOC_RES_ID, StatusCode.ARRIVED, "2000-01-05T00:00:00")));

        // История первой заявки.
        InboundStatusHistory inboundHistory = new InboundStatusHistory(
                Collections.singletonList(status(StatusCode.CANCELLED, "2000-01-05T00:00:00")),
                RES_ID_1
        );
        when(fulfillmentClient.getInboundHistory(RES_ID_1, PARTNER_1))
                .thenReturn(inboundHistory);

        InboundStatusHistory xDocInboundHistory = new InboundStatusHistory(
                Collections.singletonList(status(StatusCode.ARRIVED, "2000-01-05T00:00:00")),
                X_DOC_RES_ID
        );
        when(fulfillmentClient.getInboundHistory(X_DOC_RES_ID, X_DOC_PARTNER))
                .thenReturn(xDocInboundHistory);
    }

    @Test
    @DatabaseSetup("classpath:service/sync-statuses/before_command.xml")
    @ExpectedDatabase(value = "classpath:service/sync-statuses/after_command.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testSyncGoesWell() throws Exception {
        mockMvc.perform(
                post("/support/sync-request-statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestIds\":[1],\"xdoc\":false}")
        ).andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:service/sync-statuses/before_command_xdoc.xml")
    @ExpectedDatabase(value = "classpath:service/sync-statuses/after_command_xdoc.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testSyncXDocGoesWell() throws Exception {
        mockMvc.perform(
                post("/support/sync-request-statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestIds\":[1],\"xdoc\":true}")
        ).andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:service/sync-statuses/before_command.xml")
    @ExpectedDatabase(value = "classpath:service/sync-statuses/before_command.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testLgwStatusesNotFound() throws Exception {
        // заявка есть в ffwf но по ней ничего ни прилетает из lgw
        mockMvc.perform(
                post("/support/sync-request-statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestIds\":[2],\"xdoc\":false}")
        ).andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:service/sync-statuses/before_command.xml")
    @ExpectedDatabase(value = "classpath:service/sync-statuses/before_command.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testShopRequestNotFound() throws Exception {
        // заявка отсутствует в ffwf
        mockMvc.perform(
                post("/support/sync-request-statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestIds\":[3],\"xdoc\":false}")
        ).andExpect(MockMvcResultMatchers.status().is5xxServerError());
    }

    @Test
    @DatabaseSetup("classpath:controller/support/recalculate/before.xml")
    @ExpectedDatabase(value = "classpath:controller/support/recalculate/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void recalculateItemsUsingSubRequests() throws Exception {
        mockMvc.perform(
                put("/support/recalculate-items-using-sub-requests/1")
        ).andExpect(MockMvcResultMatchers.status().isOk());
    }

    private static InboundStatus inboundStatus(
            final ResourceId resourceId, final StatusCode statusCode, final String dateTime) {

        return new InboundStatus(resourceId, new Status(statusCode, new DateTime(dateTime)));
    }

    private static Status status(final StatusCode statusCode, final String dateTime) {
        return new Status(statusCode, new DateTime(dateTime));
    }
}
