package ru.yandex.market.jmf.utils.log;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import ru.yandex.market.jmf.utils.log.trace.CallContext;
import ru.yandex.market.request.trace.RequestContextHolder;

public class CallContextTest {

    private static final String REQUEST_ID_KEY = RequestContextHolder.REQUEST_ID_MDC_KEY;

    @BeforeEach
    public void setUp() {
        RequestContextHolder.clearContext();
    }

    @Test
    public void byRequestId() {
        String initialRequestId = getRequestId();

        assertExpectedRequestId(initialRequestId);

        final String fixedRequestId = "123";
        try (CallContext ignored = CallContext.byRequestId(fixedRequestId)) {
            assertExpectedRequestId(fixedRequestId);
        }

        assertExpectedRequestId(initialRequestId);
    }

    @Test
    public void createNewContext() {
        String initialRequestId = getRequestId();

        try (CallContext newCallContext = CallContext.createNewIfEmpty()) {
            final String newRequestId = getRequestId();
            assertExpectedRequestId(newRequestId);

            Assertions.assertNotEquals(initialRequestId, newRequestId);

            try (CallContext sameCallContext = CallContext.createNewIfEmpty()) {
                assertExpectedRequestId(newRequestId);
            }
        }
        assertExpectedRequestId(initialRequestId);
    }

    @Test
    public void createChildContext() {
        final String initialRequestId = getRequestId();

        try (CallContext parent = CallContext.createNewIfEmpty()) {
            String parentRequestId = getRequestId();

            try (CallContext child = CallContext.createChild()) {
                String childRequestId = getRequestId();
                Assertions.assertEquals(childRequestId, getMDCRequestIdValue());
            }

            assertExpectedRequestId(parentRequestId);
        }

        assertExpectedRequestId(initialRequestId);
    }

    private String getRequestId() {
        return RequestContextHolder.getContext().getRequestId();
    }

    private String getMDCRequestIdValue() {
        return MDC.get(REQUEST_ID_KEY);
    }

    private void assertExpectedRequestId(String expectedRequestId) {
        Assertions.assertEquals(expectedRequestId, getRequestId());
        Assertions.assertEquals(expectedRequestId, getMDCRequestIdValue());
    }
}
