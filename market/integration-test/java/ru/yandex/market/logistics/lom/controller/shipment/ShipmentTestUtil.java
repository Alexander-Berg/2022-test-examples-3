package ru.yandex.market.logistics.lom.controller.shipment;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@ParametersAreNonnullByDefault
@Slf4j
public final class ShipmentTestUtil {

    private ShipmentTestUtil() {
        throw new UnsupportedOperationException();
    }

    static void createShipmentByRawRequestResponse(
        MockMvc mockMvc,
        String request,
        String response,
        ResultMatcher statusMatcher
    ) throws Exception {
        mockMvc.perform(post("/shipments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        )
            .andExpect(statusMatcher)
            .andExpect(content().json(response));
    }

    static void createShipment(MockMvc mockMvc, String filename, ResultMatcher statusMatcher) throws Exception {
        createShipmentByRawRequestResponse(
            mockMvc,
            extractFileContent("controller/shipment/request/" + filename),
            extractFileContent("controller/shipment/response/" + filename),
            statusMatcher
        );
    }

    public static ResultActions searchShipmentByRawRequestResponse(
        MockMvc mockMvc,
        String request,
        String response,
        boolean strict,
        @Nullable Map<String, Set<String>> params,
        ResultMatcher statusMatcher
    ) throws Exception {
        return mockMvc.perform(
            put("/shipments/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
                .params(toParams(Optional.ofNullable(params).orElseGet(Map::of)))
        )
            .andExpect(statusMatcher)
            .andExpect(content().json(response, strict));
    }

    @Nonnull
    static ResultActions cancelShipment(
        MockMvc mockMvc,
        long applicationId,
        ResultMatcher status,
        long marketIdFrom
    ) throws Exception {
        return mockMvc.perform(
            delete("/shipments/" + applicationId)
                .param("marketIdFrom", String.valueOf(marketIdFrom))
        )
            .andExpect(status);
    }

    @Nonnull
    static ResultActions confirmShipment(
        MockMvc mockMvc,
        HttpStatus expectedStatus
    ) throws Exception {
        return confirmShipment(mockMvc, expectedStatus, 1L);
    }

    @Nonnull
    static ResultActions confirmShipment(
        MockMvc mockMvc,
        HttpStatus expectedStatus,
        long shipmentApplicationId
    ) throws Exception {
        return mockMvc.perform(
            put("/shipments/{id}/confirm", shipmentApplicationId)
        )
            .andDo(r -> log.info(r.getResponse().getContentAsString()))
            .andExpect(expectedStatus == HttpStatus.OK ? status().isOk() : status().isBadRequest());
    }
}
