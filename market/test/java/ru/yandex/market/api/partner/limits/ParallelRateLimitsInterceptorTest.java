package ru.yandex.market.api.partner.limits;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.market.api.partner.request.PartnerServletRequest;
import ru.yandex.market.api.partner.request.PartnerServletResponse;
import ru.yandex.market.api.resource.ApiLimit;
import ru.yandex.market.api.resource.ApiLimitType;
import ru.yandex.market.api.resource.ApiResource;
import ru.yandex.market.api.resource.ApiResourceAccessLevel;
import ru.yandex.market.api.resource.CalculatedApiLimit;
import ru.yandex.market.api.resource.MemCachedApiLimitsAgentService;
import ru.yandex.market.api.resource.MemCachedApiResourceService;
import ru.yandex.market.core.client.remove.RemoveClientEnvironmentService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParallelRateLimitsInterceptorTest {

    private ParallelRateLimitsInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new ParallelRateLimitsInterceptor();

        RemoveClientEnvironmentService environmentService = mock(RemoveClientEnvironmentService.class);
        Mockito.when(environmentService.papiLimitsByUid()).thenReturn(true);
        interceptor.setRemoveClientEnvironmentService(environmentService);
    }

    @Test
    void testDifferentLimits() {
        for (int limit : Arrays.asList(1, 2, 5, 20, 100)) {
            testLimit("PUT", "ep", "ep", createCalculatedApiLimit(limit, ApiLimitType.PARALLEL));
            testLimit("GET", "ep", "ep", createCalculatedApiLimit(limit, ApiLimitType.PARALLEL));
        }
    }

    @Test
    void testEndPointPattern() {
        CalculatedApiLimit calculatedApiLimit = createCalculatedApiLimit(100, ApiLimitType.PARALLEL);
        testLimit("GET", "/endpoint/*/endpoint/*", "/endpoint/123/endpoint/abc", calculatedApiLimit);
        testLimit("DELETE", "/path/*", "/path/parameter", calculatedApiLimit);
        testLimit("POST", "/check/*/*", "/check/123/abc", calculatedApiLimit);
    }

    @Test
    void testDefaultLimit() {
        testLimit("GET", "/endpoint/*/*", "/endpoint/1/1",
                createCalculatedApiLimit(interceptor.getLimit(), ApiLimitType.DEFAULT));
        testLimit("GET", "/endpoint/*/*", "/endpoint/1/1",
                createCalculatedApiLimit(null, ApiLimitType.PARALLEL));
    }

    private void testLimit(String method, String endpointPattern, String endpointReal,
                           CalculatedApiLimit calculatedApiLimit) {
        PartnerServletRequest request = createRequest(method, endpointPattern, endpointReal);
        PartnerServletResponse response = createResponse(request);

        setUpMocksInInterceptor(calculatedApiLimit);
        int limit = interceptor.getLimit();
        if (calculatedApiLimit.getApiLimit().getLimit() != null &&
                calculatedApiLimit.getCalculatedApiLimitType().equals(ApiLimitType.PARALLEL)) {
            limit = calculatedApiLimit.getApiLimit().getLimit();
        }
        for (int i = 0; i < limit; i++) {
            assertTrue(interceptor.preHandle(request, response, null));
        }
        assertThrows(RateLimitHitException.class, () -> interceptor.preHandle(request, response, null));
    }

    private CalculatedApiLimit createCalculatedApiLimit(Integer limit, ApiLimitType apiLimitType) {
        return new CalculatedApiLimit(new ApiLimit(limit, 10, TimeUnit.SECONDS), apiLimitType);
    }

    private void setUpMocksInInterceptor(CalculatedApiLimit calculatedApiLimit) {
        MemCachedApiLimitsAgentService memCachedApiLimitsAgentService =
                mockMemCachedApiLimitsAgentService(calculatedApiLimit.getApiLimit().getLimit());
        interceptor.setMemCachedApiLimitsAgentService(memCachedApiLimitsAgentService);

        MemCachedApiResourceService memCachedApiResourceService = mockMemCachedApiResourceService(calculatedApiLimit);
        interceptor.setMemCachedApiResourceService(memCachedApiResourceService);
    }

    private PartnerServletRequest createRequest(String method, String pattern, String real) {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        PartnerServletRequest req = new PartnerServletRequest(httpServletRequest, 1000);
        req.initResource(real);
        req.setApiResource(new ApiResource(1, 1, pattern, method, null, ApiResourceAccessLevel.READ_WRITE));
        return req;
    }

    private PartnerServletResponse createResponse(PartnerServletRequest request) {
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        return new PartnerServletResponse((HttpServletRequest) request.getRequest(), httpServletResponse);
    }

    private MemCachedApiResourceService mockMemCachedApiResourceService(CalculatedApiLimit calculatedApiLimit) {
        MemCachedApiResourceService memCachedApiResourceService = mock(MemCachedApiResourceService.class);
        when(memCachedApiResourceService.getResourceLimit(eq(ApiLimitType.PARALLEL), anyInt(), anyLong()))
                .thenReturn(calculatedApiLimit);
        return memCachedApiResourceService;
    }

    private MemCachedApiLimitsAgentService mockMemCachedApiLimitsAgentService(Integer limit) {
        if (limit == null) {
            limit = interceptor.getLimit();
        }
        MemCachedApiLimitsAgentService mock = mock(MemCachedApiLimitsAgentService.class);
        boolean[] slots = new boolean[limit];
        Arrays.fill(slots, false);
        when(mock.addInCache(anyString(), anyObject(), anyObject())).thenAnswer((Answer<Boolean>) invocation -> {
            String key = invocation.getArgument(0);
            String indexStr = key.substring(key.lastIndexOf("_") + 1);
            assertDoesNotThrow(() -> Integer.parseInt(indexStr));
            int index = Integer.parseInt(indexStr) - 1;
            return !slots[index] && (slots[index] = true);
        });
        return mock;
    }

}
