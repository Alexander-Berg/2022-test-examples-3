package ru.yandex.market.common.ping;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.common.util.HttpClientWrapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MarketReportPingCheckerWithFallbackTest {
    private final static String MAIN_URL = "URL1";
    private final static String FALLBACK_URL = "URL2";

    private final static String MAIN_PING_URL = MAIN_URL + "/ping";
    private final static String FALLBACK_PING_URL = FALLBACK_URL + "/ping";

    private MarketReportPingCheckerWithFallback checker;
    private HttpClientWrapper httpClientWrapper;

    @Before
    public void setUp() throws Exception {
        checker = new MarketReportPingCheckerWithFallback();
        httpClientWrapper = Mockito.mock(HttpClientWrapper.class);
        checker.setHttpClientWrapper(httpClientWrapper);
        checker.setReportUrl(MAIN_URL);
        checker.setFallbackReportUrl(FALLBACK_URL);
        checker.setLevel(CheckResult.Level.CRITICAL);
    }

    @Test
    public void returnsOkWhenServiceIsAvailable() throws Exception {
        Mockito.when(httpClientWrapper.executeRequest(MAIN_PING_URL)).thenReturn("0;OK".getBytes());
        Mockito.when(httpClientWrapper.executeRequest(FALLBACK_PING_URL)).thenReturn("0;ok".getBytes());

        CheckResult result = checker.makeChecks();

        assertEquals(CheckResult.Level.OK, result.getLevel());
        assertEquals("OK", result.getMessage());
    }

    @Test
    public void returnsOkWhenOnlyFallbackIsAvailable() throws Exception {
        Mockito.when(httpClientWrapper.executeRequest(MAIN_PING_URL)).thenThrow(new RuntimeException());
        Mockito.when(httpClientWrapper.executeRequest(FALLBACK_PING_URL)).thenReturn("0;ok".getBytes());

        CheckResult result = checker.makeChecks();

        assertEquals(CheckResult.Level.OK, result.getLevel());
        assertEquals("OK", result.getMessage());
    }

    @Test
    public void failsWhenNoServiceIsAvailable() throws Exception {
        Mockito.when(httpClientWrapper.executeRequest(MAIN_PING_URL)).thenThrow(new RuntimeException());
        Mockito.when(httpClientWrapper.executeRequest(FALLBACK_PING_URL)).thenThrow(new RuntimeException());

        CheckResult result = checker.makeChecks();

        assertEquals(CheckResult.Level.CRITICAL, result.getLevel());
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().startsWith("market-report"));
    }

    @Test
    public void failsWhenPingReturnsBadResponse() throws Exception {
        Mockito.when(httpClientWrapper.executeRequest(MAIN_PING_URL)).thenReturn("invalid-response-1".getBytes());
        Mockito.when(httpClientWrapper.executeRequest(FALLBACK_PING_URL)).thenReturn("invalid-response-1".getBytes());

        CheckResult result = checker.makeChecks();

        assertEquals(CheckResult.Level.CRITICAL, result.getLevel());
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().startsWith("market-report"));
    }

    @Test
    public void failsWhenPingReturnsNotOk() throws Exception {
        Mockito.when(httpClientWrapper.executeRequest(MAIN_PING_URL)).thenThrow(new RuntimeException());
        Mockito.when(httpClientWrapper.executeRequest(FALLBACK_PING_URL)).thenReturn("2;Some error".getBytes());

        CheckResult result = checker.makeChecks();

        assertEquals(CheckResult.Level.CRITICAL, result.getLevel());
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().startsWith("market-report"));
    }
}
