package ru.yandex.market.wms.datacreator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import ru.yandex.market.wms.datacreator.config.DataCreatorIntegrationTest;
import ru.yandex.market.wms.datacreator.dto.ItemDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
public class SkuCreatorControllerTest extends DataCreatorIntegrationTest {

    private final ObjectMapper mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    @Test
    @DatabaseSetup(value = "/service/items/before.xml", connection = "wmwhse1Connection")
    public void getItemBySerialNumberTest() throws Exception {

        ItemDto requestedItem = ItemDto.builder()
                .storerKey("10264169")
                .manufacturerSku("000116.4251497")
                .lot("0000013572")
                .loc("4-02")
                .serialNumber("995720063486")
                .build();

        String body = mockMvc.perform(
                post("/sku/getItem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8")
                        .content("{\"serialNumber\":\"995720063486\"}")
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ItemDto returnedItem = mapper.readValue(body, ItemDto.class);

        Assertions.assertEquals(requestedItem, returnedItem);
    }

    @Test
    @DatabaseSetup(value = "/service/items/before.xml", connection = "wmwhse1Connection")
    public void getItemByAllParamsTest() throws Exception {

        ItemDto requestedItem = ItemDto.builder()
                .storerKey("10264169")
                .manufacturerSku("000116.4251497")
                .lot("0000013572")
                .loc("4-02")
                .serialNumber("995720063486")
                .build();

        String body = mockMvc.perform(
                post("/sku/getItem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8")
                        .content(mapper.writeValueAsString(requestedItem))
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ItemDto returnedItem = mapper.readValue(body, ItemDto.class);

        Assertions.assertEquals(requestedItem, returnedItem);
    }
}
