package ru.yandex.market.ff.controller.api;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("classpath:service/shop-request/requests_xdoc_inbound_to_ff.xml")
class XDocInboundControllerTest extends MvcIntegrationTest {
    @DisplayName("Попытка установить дату для реквеста, у которого уже есть дата")
    @Test
    @ExpectedDatabase(
        value = "classpath:service/shop-request/requests_xdoc_inbound_to_ff.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void xDocInboundFinalDateAlreadySet() throws Exception {
        MvcResult mvcResult = mockMvc
            .perform(
                post("/xdoc-inbound/final-date")
                    .content(
                        "{"
                            + "\"date\":\"2021-05-25T10:11:12+00:03\","
                            + "\"shopRequestId\":1"
                            + "}"
                    )
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().is(400))
            .andReturn();

        JSONAssert.assertEquals(
            "{\"message\":\"Shop request with id 1 already was commited with date 2017-03-01T09:09:09\"}",
            mvcResult.getResponse().getContentAsString(),
            JSONCompareMode.NON_EXTENSIBLE
        );
    }

    @DisplayName("Установка даты приёмки для xDoc")
    @Test
    @ExpectedDatabase(
        value = "classpath:service/shop-request/requests_xdoc_inbound_to_ff_after_date_set.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void xDocInboundFinalDate() throws Exception {
        mockMvc
            .perform(
                post("/xdoc-inbound/final-date")
                    .content(
                        "{"
                            + "\"date\":\"2021-05-25T10:11:12+00:03\","
                            + "\"shopRequestId\":2"
                            + "}"
                    )
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().is(200));
    }

    @DisplayName("Попытка установить дату неподдерживаемому типу")
    @Test
    @ExpectedDatabase(
        value = "classpath:service/shop-request/requests_xdoc_inbound_to_ff.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void xDocInboundFinalDateIncorrectType() throws Exception {
        MvcResult mvcResult = mockMvc
            .perform(
                post("/xdoc-inbound/final-date")
                    .content(
                        "{"
                            + "\"date\":\"2021-05-25T10:11:12+00:03\","
                            + "\"shopRequestId\":3"
                            + "}"
                    )
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().is(400))
            .andReturn();

        JSONAssert.assertEquals(
            "{\"message\":\"Shop request with id 3 has type WITHDRAW. " +
                    "Supported types are [X_DOC_PARTNER_SUPPLY_TO_FF]\"}",
            mvcResult.getResponse().getContentAsString(),
            JSONCompareMode.NON_EXTENSIBLE
        );
    }

    @DisplayName("Попытка установить дату для реквеста в некорректном статусе")
    @Test
    @ExpectedDatabase(
        value = "classpath:service/shop-request/requests_xdoc_inbound_to_ff.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void xDocInboundFinalDateIncorrectStatus() throws Exception {
        MvcResult mvcResult = mockMvc
            .perform(
                post("/xdoc-inbound/final-date")
                    .content(
                        "{"
                            + "\"date\":\"2021-05-25T10:11:12+00:03\","
                            + "\"shopRequestId\":4"
                            + "}"
                    )
                    .contentType(MediaType.APPLICATION_JSON)
            )
            .andExpect(status().is(400))
            .andReturn();

        Assertions.assertTrue(mvcResult.getResponse().getContentAsString()
                .contains("Shop request with id 4 has status SENT_TO_SERVICE. Supported statuses are"));
        Assertions.assertTrue(mvcResult.getResponse().getContentAsString()
                .contains("VALIDATED"));
        Assertions.assertTrue(mvcResult.getResponse().getContentAsString()
                .contains("WAITING_FOR_CONFIRMATION"));
    }
}
