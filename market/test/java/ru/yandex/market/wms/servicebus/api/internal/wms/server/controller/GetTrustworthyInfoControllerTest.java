package ru.yandex.market.wms.servicebus.api.internal.wms.server.controller;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;

import ru.yandex.market.logistics.iris.client.api.TrustworthyInfoClient;
import ru.yandex.market.logistics.iris.client.model.entity.Dimensions;
import ru.yandex.market.logistics.iris.client.model.entity.TrustworthyItem;
import ru.yandex.market.logistics.iris.client.model.response.TrustworthyInfoResponse;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;
import ru.yandex.market.wms.servicebus.IntegrationTest;

import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.TestConstructor.AutowireMode.ALL;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

@TestConstructor(autowireMode = ALL)
class GetTrustworthyInfoControllerTest extends IntegrationTest {

    private final TrustworthyInfoClient trustworthyInfoClient;

    GetTrustworthyInfoControllerTest(TrustworthyInfoClient trustworthyInfoClient) {
        this.trustworthyInfoClient = trustworthyInfoClient;
    }

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    void getTrustworthyInfo() throws Exception {
        when(trustworthyInfoClient.getTrustworthyInfo(any())).thenReturn(
                new TrustworthyInfoResponse(singletonMap(
                        ItemIdentifier.of("111111", "exists.111111"),
                        TrustworthyItem.builder()
                                .setName("Test Item")
                                .setWeightGross(BigDecimal.valueOf(501.1))
                                .setDimensions(new Dimensions.DimensionsBuilder()
                                        .setHeight(BigDecimal.valueOf(1.01))
                                        .setLength(BigDecimal.valueOf(20))
                                        .setWidth(null)
                                        .build())
                                .setLifetime(365)
                                .setLifetimeTimestamp(0L)
                                .build()
                ))
        );

        mockMvc.perform(post("/wms/getTrustworthyInfo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getFileContent("api/internal/wms/getTrustworthyInfo/request.json")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(getFileContent("api/internal/wms/getTrustworthyInfo/response.json"), true));
    }
}
