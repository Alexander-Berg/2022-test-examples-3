package ru.yandex.market.abo.core.checkorder.model;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod.PI;

/**
 * @author artemmz
 * @date 27/11/2020.
 */
class ScenarioPayloadTest {
    private static final String JSON = "{\"orderProcessMethod\":\"API\"}";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void makeCopy() {
        ScenarioPayload initPayload = new ScenarioPayload().setOrderProcessMethod(PI);
        ScenarioPayload clonedPayload = initPayload.makeCopy();
        assertEquals(clonedPayload, initPayload);

        initPayload.addFailedAttemptTrace("failed traceId");
        assertNotEquals(clonedPayload, initPayload);
    }

    @Test
    void testParse() throws IOException {
        ScenarioPayload payload = MAPPER.readValue(JSON, ScenarioPayload.class);
        assertNotNull(payload.getFailedAttemptsTraceIds());
    }
}