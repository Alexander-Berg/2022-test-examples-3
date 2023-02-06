package ru.yandex.market.checkout.checkouterpumpkin.controllers;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import com.yandex.ydb.table.values.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.checkouterpumpkin.BasePumpkinTest;
import ru.yandex.market.ydb.integration.model.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PumpkinCheckoutControllerTest extends BasePumpkinTest {

    @BeforeEach
    public void setup() {
        testableClock.setFixed(Instant.parse("2021-09-16T12:00:00Z"), ZoneId.of("Europe/Moscow"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1_initial"
    })
    public void pumpkinCheckoutTest(String name) throws Exception {
        var checkoutRq = loadResourceAsString(String.format("checkout/%s_req.json", name));
        var expectedCheckoutRs = loadResourceAsString(String.format("checkout/%s_resp.json", name));

        var checkoutRs = mockMvc.perform(post("/checkout")
                        .header("X-Market-Rearrfactors", "111")
                        .param("uid", "222")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutRq))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JSONAssert.assertEquals(expectedCheckoutRs, checkoutRs, JSONCompareMode.NON_EXTENSIBLE);

        List<Map<Field<?, ?>, Value<?>>> queueData = readTableData(queueTable);
        assertEquals(1, queueData.size());
        Map<Field<?, ?>, Value<?>> queueRow = queueData.get(0);
        assertNotNull(queueRow.get(queueTable.getRequestId()));
        assertEquals(testableClock.instant(), queueRow.get(queueTable.getCreatedAt()).asData().getTimestamp());
        assertNull(queueRow.get(queueTable.getProcessingBy()));
        assertNull(queueRow.get(queueTable.getProcessingStartedAt()));

        List<Map<Field<?, ?>, Value<?>>> requestData = readTableData(requestTable);
        assertEquals(1, requestData.size());
        Map<Field<?, ?>, Value<?>> requestRow = requestData.get(0);
        assertEquals(queueRow.get(queueTable.getRequestId()).asData().getUtf8(),
                requestRow.get(requestTable.getId()).asData().getUtf8());
        assertEquals(testableClock.instant(),
                requestRow.get(requestTable.getCreatedAt()).asData().getTimestamp());
        assertEquals("in_queue",
                requestRow.get(requestTable.getStatus()).asData().getUtf8());
        assertEquals("{\"X-Market-Rearrfactors\":[\"111\"]}",
                requestRow.get(requestTable.getHeaders()).asData().getJson());
        assertEquals("{\"uid\":[\"222\"]}",
                requestRow.get(requestTable.getParameters()).asData().getJson());
        JSONAssert.assertEquals(checkoutRq, requestRow.get(requestTable.getBody()).asData().getUtf8(),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void pumpkinCheckoutWrongRequestTest() throws Exception {
        var invalidCheckoutRq = loadResourceAsString("checkout/wrong_req.json");

        mockMvc.perform(post("/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidCheckoutRq))
                .andExpect(status().isBadRequest());
    }
}
