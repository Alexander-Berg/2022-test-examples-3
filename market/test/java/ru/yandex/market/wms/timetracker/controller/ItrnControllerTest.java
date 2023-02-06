package ru.yandex.market.wms.timetracker.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.wms.timetracker.config.NoAuthTest;
import ru.yandex.market.wms.timetracker.model.enums.EnumerationOrder;
import ru.yandex.market.wms.timetracker.response.DefaultResponse;
import ru.yandex.market.wms.timetracker.response.ItrnResponse;
import ru.yandex.market.wms.timetracker.service.ItrnService;
import ru.yandex.market.wms.timetracker.specification.rsql.ApiField;

@WebMvcTest(ItrnController.class)
@ActiveProfiles("test")
@Import(NoAuthTest.class)
class ItrnControllerTest {

    @MockBean
    private ItrnService itrnService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getItrn() throws Exception {

        final List<ItrnResponse> contentExpected = List.of(
                ItrnResponse.builder()
                        .serialKey(1110299646)
                        .whseId("wmwhse1")
                        .itrnKey("0563442549")
                        .storerKey("472311")
                        .sku("ROV0000000000004498186")
                        .lot("0004512925")
                        .toId("P201920267")
                        .toLoc("UPACKL5_33")
                        .qty(new BigDecimal("1.00000"))
                        .tranType("MV")
                        .addWho("sof-test")
                        .editWho("sof-test")
                        .addDate(LocalDateTime.parse("2021-12-13T12:01:48"))
                        .editDate(LocalDateTime.parse("2021-12-13T12:31:48"))
                        .build()
        );

        final DefaultResponse<List<ItrnResponse>> requestExpected =
                DefaultResponse.<List<ItrnResponse>>builder()
                        .limit(20)
                        .offset(0)
                        .content(contentExpected)
                        .build();

        Mockito.when(itrnService.findAll(ArgumentMatchers.eq("SOF"),
                        ArgumentMatchers.any(ApiField.class),
                        ArgumentMatchers.any(EnumerationOrder.class),
                        ArgumentMatchers.any(String.class),
                        ArgumentMatchers.any(Integer.class),
                        ArgumentMatchers.any(Integer.class)))
                .thenReturn(contentExpected);

        final String jsonModel = mapper.writeValueAsString(requestExpected);

        mockMvc
                .perform(
                        MockMvcRequestBuilders.get("/itrn/SOF")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(jsonModel));
    }

    @Test
    public void getItrnSerialWithRsql() throws Exception {

        final List<ItrnResponse> contentExpected = List.of(
                ItrnResponse.builder()
                        .serialKey(1110299646)
                        .whseId("wmwhse1")
                        .itrnKey("0563442549")
                        .storerKey("472311")
                        .sku("ROV0000000000004498186")
                        .lot("0004512925")
                        .toId("P201920267")
                        .toLoc("UPACKL5_33")
                        .qty(new BigDecimal("1.00000"))
                        .tranType("MV")
                        .addWho("sof-test")
                        .editWho("sof-test")
                        .addDate(LocalDateTime.parse("2021-12-13T12:01:48"))
                        .editDate(LocalDateTime.parse("2021-12-13T12:31:48"))
                        .build()
        );

        final DefaultResponse<List<ItrnResponse>> requestExpected =
                DefaultResponse.<List<ItrnResponse>>builder()
                        .limit(1)
                        .offset(10)
                        .content(contentExpected)
                        .build();

        Mockito.when(itrnService.findAll(ArgumentMatchers.eq("SOF"),
                        ArgumentMatchers.eq(ApiField.of("addDate")),
                        ArgumentMatchers.eq(EnumerationOrder.DESC),
                        ArgumentMatchers.any(String.class),
                        ArgumentMatchers.eq(1),
                        ArgumentMatchers.any(Integer.class)))
                .thenReturn(contentExpected);

        final String jsonModel = mapper.writeValueAsString(requestExpected);

        mockMvc
                .perform(
                        MockMvcRequestBuilders.get("/itrn/SOF?" +
                                        "sort=addDate" +
                                        "&limit=1" +
                                        "&offset=10" +
                                        "&sort=addDate" +
                                        "&order=desc"
                                )
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(jsonModel));
    }
}
