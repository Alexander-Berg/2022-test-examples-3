package ru.yandex.market.sc.internal.controller.partner;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;
import static org.assertj.core.api.Assertions.assertThat;

@ScIntControllerTest
public class PartnerOrderHistoryControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    @SneakyThrows
    public void getOrderItemsWeightAndDimensionsTest() {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.XDOC_ENABLED, "false");
        var orderParamsBuilder = order(sortingCenter)
                .externalId("1")
                .places("1", "2");
        var order = testFactory.create(orderParamsBuilder.build()).get();
        MvcResult r = mockMvc.perform(
                MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                        + "/orders/" + order.getExternalId())
        ).andExpect(status().isOk()).andReturn();
        String jsonString = r.getResponse().getContentAsString();
        ObjectMapper om = new ObjectMapper();
        JsonNode tree = om.readTree(jsonString);

        assertThat(tree.findValues("weightAndDimensions").toString())
                .isEqualTo("[{\"width\":121,\"height\":122,\"length\":123,\"weight\":121.12}]");
        assertThat(tree.findValues("places").toString())
                .isEqualTo("[[{\"mainPartnerCode\":\"1\",\"status\":\"CREATED\"," +
                        "\"placeWeightAndDimensions\":{\"width\":121,\"height\":122," +
                        "\"length\":123,\"weight\":121.12}}" +
                        ",{\"mainPartnerCode\":\"2\",\"status\":\"CREATED\"," +
                        "\"placeWeightAndDimensions\":{\"width\":121,\"height\":122," +
                        "\"length\":123,\"weight\":121.12}}]]");
    }

}
