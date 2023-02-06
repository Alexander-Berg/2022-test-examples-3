package ru.yandex.market.api.partner.limits;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.common.cache.memcached.MemCachedAgent;
import ru.yandex.market.api.partner.request.PartnerServletRequest;
import ru.yandex.market.api.partner.request.PartnerServletResponse;
import ru.yandex.market.api.resource.ApiLimit;
import ru.yandex.market.api.resource.ApiLimitType;
import ru.yandex.market.api.resource.ApiResource;
import ru.yandex.market.api.resource.ApiResourceAccessLevel;
import ru.yandex.market.api.resource.ApiResourceService;
import ru.yandex.market.api.resource.CalculatedApiLimit;
import ru.yandex.market.api.resource.MemCachedApiLimitsAgentService;
import ru.yandex.market.core.client.remove.RemoveClientEnvironmentService;
import ru.yandex.market.tags.Components;
import ru.yandex.market.tags.Tests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Tags({
        @Tag(Components.MBI_PARTNER_API),
        @Tag(Tests.COMPONENT)
})
class ResourceRateLimitsInterceptorTest {

    private static final String REQUESTS_LIMIT = "2";
    private static final String ZERO_REQUESTS_RATE = "0";
    private static final String ONE_REQUEST_RATE = "0";
    private static final int TEST_GROUP_ID = 868768678;
    private static final long TEST_CAMPAIGN_ID = 1502351357543L;
    private static final long TEST_CLIENT_ID = 1502351383162L;
    private static final ApiResource TEST_API_RESOURCE = new ApiResource(1, TEST_GROUP_ID, "/test", "/test", null,
            ApiResourceAccessLevel.READ_WRITE);
    private static final ApiLimit TEST_API_LIMIT = new ApiLimit(Integer.valueOf(REQUESTS_LIMIT), 2, TimeUnit.DAYS);
    private static final ApiLimit TEST_API_LIMIT_MINUTES =
            new ApiLimit(Integer.valueOf(REQUESTS_LIMIT), 2, TimeUnit.MINUTES);

    private static final long NOW = 1575048223112L;

    private static final String ALL_CLIENTS_RATE_KEY = buildCacheKey(ApiLimitType.DEFAULT);

    private ApiResourceService apiResourceService;
    private ResourceRateLimitsInterceptor interceptor;
    private MemCachedApiLimitsAgentService memCachedApiLimitsAgentService;
    private MemCachedAgent memCachedAgent;

    private PartnerServletRequest request;
    private PartnerServletResponse response;

    private static String buildCacheKey(ApiLimitType type) {
        return buildCacheKey(type, TEST_API_LIMIT, false);
    }

    private static String buildCacheKey(ApiLimitType type, ApiLimit apiLimit, boolean sharedLimit) {
        final long currentPeriod = NOW / apiLimit.getTimePeriodInMillis();
        return ApiLimitType.DEFAULT.getMemcachedPrefix()
                + (type != ApiLimitType.DEFAULT ? type.getMemcachedPrefix() : "")
                + "_" + TEST_GROUP_ID
                + (sharedLimit ? "" : "_" + TEST_CAMPAIGN_ID)
                + "_" + currentPeriod;
    }

    private static Object[] apiLimitTypesWithoutDefault() {
        return Stream.of(ApiLimitType.values())
                .filter(type -> type != ApiLimitType.DEFAULT && type != ApiLimitType.PARALLEL)
                .toArray(Object[]::new);
    }

    private static Object[][] memcachedKeys() {
        return Stream.of(ApiLimitType.values())
                .filter(type -> type != ApiLimitType.PARALLEL)
                .map(type -> new Object[]{type, key(type)})
                .toArray(Object[][]::new);
    }

    private static String key(ApiLimitType type) {
        final String prefix = type == ApiLimitType.DEFAULT ? "" : type.getMemcachedPrefix();
        return "RPL" + prefix + "_868768678_1502351357543_9114";
    }

    @BeforeEach
    void setUp() {
        interceptor = new ResourceRateLimitsInterceptor();
        interceptor.setClock(Clock.fixed(Instant.ofEpochMilli(NOW), ZoneId.systemDefault()));
        interceptor.init();

        ExecutorService executorService = mock(ExecutorService.class);
        doAnswer(invocation -> {
            final Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(executorService).execute(any(Runnable.class));

        apiResourceService = mock(ApiResourceService.class);
        for (ApiLimitType type : ApiLimitType.values()) {
            when(apiResourceService.getResourceLimit(type, TEST_GROUP_ID, TEST_CAMPAIGN_ID)).thenReturn(
                    new CalculatedApiLimit(TEST_API_LIMIT, type));
        }

        memCachedAgent = mock(MemCachedAgent.class);

        memCachedApiLimitsAgentService = new MemCachedApiLimitsAgentService();
        memCachedApiLimitsAgentService.setExecutorService(executorService);
        memCachedApiLimitsAgentService.setMemCachedAgent(memCachedAgent);

        interceptor.setMemCachedApiLimitsAgentService(memCachedApiLimitsAgentService);
        interceptor.setApiResourceService(apiResourceService);

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        request = createDefaultRequest(httpRequest);
        response = createDefaultResponse(httpRequest, httpResponse);

        RemoveClientEnvironmentService environmentService = mock(RemoveClientEnvironmentService.class);
        Mockito.when(environmentService.papiLimitsByUid()).thenReturn(true);
        interceptor.setRemoveClientEnvironmentService(environmentService);
    }

    @Test
    @DisplayName("Проверка ожидаемой ошибки в случае превышения лимитов для всех клиентов кроме Price Labs")
    void testAllClientsRateLimitFailure() {
        setUpCurrentRates(REQUESTS_LIMIT, ZERO_REQUESTS_RATE);

        Assertions.assertThrows(RateLimitHitException.class, () -> interceptor.preHandle(request, response, null));
    }

    @ParameterizedTest
    @MethodSource("apiLimitTypesWithoutDefault")
    @DisplayName("Проверка ожидаемой ошибки в случае превышения типизированных лимитов (Price Labs)")
    void testPriceLabsRateLimitFailure(ApiLimitType apiLimitType) {
        setUpCurrentRates(ZERO_REQUESTS_RATE, REQUESTS_LIMIT);
        request.setApiLimitType(apiLimitType);

        Assertions.assertThrows(RateLimitHitException.class, () -> interceptor.preHandle(request, response, null));
    }

    @ParameterizedTest
    @MethodSource("apiLimitTypesWithoutDefault")
    @DisplayName("Проверка инкремента текущего значения лимита при запросе типизированного лимита (Price Labs), " +
            "когда в кеше не было начального значения для рейта")
    void testPriceLabsSeparateLimitWithoutCachedRate(ApiLimitType apiLimitType) {
        request.setApiLimitType(apiLimitType);

        interceptor.preHandle(request, response, null);
        interceptor.postHandle(request, response, null, null);

        final String key = buildCacheKey(apiLimitType);
        verify(memCachedAgent).incrementInCache(eq(key), eq(1L), any());
        verify(memCachedAgent).getFromCache(eq(key));
        verifyNoMoreInteractions(memCachedAgent);
    }

    @ParameterizedTest
    @MethodSource("apiLimitTypesWithoutDefault")
    @DisplayName("Проверка инкремента текущего значения лимита при запросе типизированного лимита (Price Labs), " +
            "когда в кеше не было начального значения для рейта и у нас лимиты поминутные")
    void testPriceLabsSeparateLimitWithoutCachedRatePerMinute(ApiLimitType apiLimitType) {
        when(apiResourceService.getResourceLimit(apiLimitType, TEST_GROUP_ID, TEST_CAMPAIGN_ID)).thenReturn(
                new CalculatedApiLimit(TEST_API_LIMIT_MINUTES, apiLimitType));

        request.setApiLimitType(apiLimitType);

        interceptor.preHandle(request, response, null);
        interceptor.postHandle(request, response, null, null);

        final String key = buildCacheKey(apiLimitType, TEST_API_LIMIT_MINUTES, true);
        Assertions.assertEquals("RPL" + apiLimitType.getMemcachedPrefix() + "_868768678_13125401", key);

        verify(memCachedAgent).incrementInCache(eq(key), eq(1L), any());
        verify(memCachedAgent).getFromCache(eq(key));
        verifyNoMoreInteractions(memCachedAgent);
    }

    @ParameterizedTest
    @MethodSource("apiLimitTypesWithoutDefault")
    @DisplayName("Проверка инкремента текущего значения лимита при запросе от всех клиентов кроме Price Labs, " +
            "когда в кеше хранится закешированное значение рейта")
    void testPriceLabsSeparateLimitWithCachedRate(ApiLimitType apiLimitType) {
        setUpCurrentRates(ONE_REQUEST_RATE, ONE_REQUEST_RATE);
        request.setApiLimitType(apiLimitType);

        interceptor.preHandle(request, response, null);
        interceptor.postHandle(request, response, null, null);

        final String key = buildCacheKey(apiLimitType);

        verify(memCachedAgent).incrementInCache(eq(key), eq(1L), any());
        verify(memCachedAgent).getFromCache(eq(key));
        verifyNoMoreInteractions(memCachedAgent);
    }

    @Test
    @DisplayName("Проверка инкремента текущего значения лимита при запросе от всех клиентов кроме Price Labs, " +
            "когда в кеше не было начального значения для рейта")
    void testOtherClientsSeparateLimitWithoutCachedRate() {
        interceptor.preHandle(request, response, null);
        interceptor.postHandle(request, response, null, null);

        verify(memCachedAgent).incrementInCache(eq(ALL_CLIENTS_RATE_KEY), eq(1L), any());
        verify(memCachedAgent).getFromCache(eq(ALL_CLIENTS_RATE_KEY));
        verifyNoMoreInteractions(memCachedAgent);
    }

    @Test
    @DisplayName("Проверка инкремента текущего значения лимита при включенном unlimit-е")
    void testOtherClientUnlimited() {
        setUpCurrentRates(ZERO_REQUESTS_RATE, REQUESTS_LIMIT); // Это будет проигнорировано

        interceptor.setUnlimited(true);

        interceptor.preHandle(request, response, null);
        interceptor.postHandle(request, response, null, null);

        verify(memCachedAgent).incrementInCache(eq(ALL_CLIENTS_RATE_KEY), eq(1L), any());
        verify(memCachedAgent).getFromCache(eq(ALL_CLIENTS_RATE_KEY));
        verifyNoMoreInteractions(memCachedAgent);
    }

    @Test
    @DisplayName("Проверка инкремента текущего значения лимита при запросе от Price Labs, когда в кеше " +
            "хранится закешированное значение рейта")
    void testOtherClientsSeparateLimitWithCachedRate() {
        setUpCurrentRates(ONE_REQUEST_RATE, ONE_REQUEST_RATE);

        interceptor.preHandle(request, response, null);
        interceptor.postHandle(request, response, null, null);

        verify(memCachedAgent).incrementInCache(eq(ALL_CLIENTS_RATE_KEY), eq(1L), any());
        verify(memCachedAgent).getFromCache(eq(ALL_CLIENTS_RATE_KEY));
        verifyNoMoreInteractions(memCachedAgent);
    }

    @ParameterizedTest
    @MethodSource("memcachedKeys")
    @DisplayName("Проверка того, как собирается ключ в Memcached для разных типов клиентов")
    void testCacheKeys(ApiLimitType limitType, String expectedKey) {
        Assertions.assertEquals(expectedKey, buildCacheKey(limitType));
    }

    private PartnerServletRequest createDefaultRequest(HttpServletRequest httpRequest) {
        PartnerServletRequest req = new PartnerServletRequest(httpRequest, 1000);
        req.setApiResource(TEST_API_RESOURCE);
        req.initCampaignId(TEST_CAMPAIGN_ID);
        req.initClientId(TEST_CLIENT_ID);

        return req;
    }

    private PartnerServletResponse createDefaultResponse(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return new PartnerServletResponse(httpRequest, httpResponse);
    }

    private void setUpCurrentRates(String allClientsRates, String typedRate) {
        when(memCachedAgent.getFromCache(eq(ALL_CLIENTS_RATE_KEY))).thenReturn(allClientsRates);
        for (Object type : apiLimitTypesWithoutDefault()) {
            when(memCachedAgent.getFromCache(eq(buildCacheKey((ApiLimitType) type)))).thenReturn(typedRate);
        }
    }

}
