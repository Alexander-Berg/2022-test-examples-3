package ru.yandex.direct.api.v5.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ws.server.endpoint.MethodEndpoint;

import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.security.utils.ApiUserMockBuilder;
import ru.yandex.direct.api.v5.ws.annotation.ApiMethod;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.common.lettuce.LettuceConnectionProvider;
import ru.yandex.direct.common.lettuce.LettuceExecuteException;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.rbac.RbacRole;

import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@ParametersAreNonnullByDefault
public class RateLimitInterceptorTest {
    private static final ApiUser CLIENT = new ApiUserMockBuilder("client", 345, 22, RbacRole.CLIENT).build();

    private final Map<String, Long> mockLettuceStorage = new HashMap<>();
    private RedisAdvancedClusterCommands<String, String> commandsLettuce;
    private RateLimitInterceptor rateLimitInterceptor;
    private Service service;
    private LettuceConnectionProvider lettuce;
    private PpcProperty<Integer> mockedProperty;

    @Before
    public void setUp() {
        service = new Service();

        lettuce = mock(LettuceConnectionProvider.class);

        commandsLettuce = mock(RedisAdvancedClusterCommands.class);

        when(lettuce.call(anyString(), any()))
                .then(invocation -> {
                    Function cmd = (Function) invocation.getArgument(1);
                    return cmd.apply(commandsLettuce);
                });

        mockLettuceStorage.clear();
        when(commandsLettuce.set(anyString(), anyString(), any()))
                .then(invocation -> {
                    Object[] arguments = invocation.getArguments();
                    String key = (String) arguments[0];
                    long value = Long.parseLong((String) arguments[1], 10);

                    if (mockLettuceStorage.containsKey(key)) {
                        return null;
                    }

                    mockLettuceStorage.put(key, value);

                    return "OK";
                });

        when(commandsLettuce.incr(anyString())).then(
                invocation -> {
                    String key = (String) invocation.getArguments()[0];
                    if (!mockLettuceStorage.containsKey(key)) {
                        mockLettuceStorage.put(key, (long) 0);
                    }

                    long value = mockLettuceStorage.get(key) + 1;
                    mockLettuceStorage.remove(key);
                    mockLettuceStorage.put(key, value);
                    return value;
                });

        ApiAuthenticationSource apiAuthenticationSource = mock(ApiAuthenticationSource.class);
        when(apiAuthenticationSource.getSubclient()).thenReturn(CLIENT);
        when(apiAuthenticationSource.getApplicationId()).thenReturn("any_app_id");

        PpcPropertiesSupport ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        mockedProperty = mock(PpcProperty.class);
        when(ppcPropertiesSupport.get(any(PpcPropertyName.class), any(Duration.class))).thenReturn(mockedProperty);
        rateLimitInterceptor = spy(new RateLimitInterceptor(lettuce, apiAuthenticationSource,
                ppcPropertiesSupport, Clock.fixed(Instant.now(), ZoneId.systemDefault())));
    }

    @Test
    public void handleRequest_noRestriction() {
        MethodEndpoint methodEndpoint = getMethodEndpoint("methodWithoutRestriction");
        assertTrue(wrappedHandleRequest(methodEndpoint));
        verifyZeroInteractions(commandsLettuce);
        assertThat(mockLettuceStorage.entrySet(), empty());
    }

    @Test
    public void handleRequest_withRestriction_oneRequest() {
        MethodEndpoint methodEndpoint = getMethodEndpoint("methodWithRestriction");
        assertTrue(wrappedHandleRequest(methodEndpoint));
        verify(commandsLettuce).set(anyString(), anyString(), any());
        verify(commandsLettuce).incr(anyString());
        assertThat(mockLettuceStorage.entrySet(), hasSize(1));
    }

    @Test(expected = RateLimitExceededException.class)
    public void handleRequest_withRestriction_twoRequests() {
        MethodEndpoint methodEndpoint = getMethodEndpoint("methodWithRestriction");
        try {
            assertTrue(wrappedHandleRequest(methodEndpoint));
        } catch (RateLimitExceededException e) {
            fail("got RateLimitExceededException too early");
        }
        assumeThat(mockLettuceStorage.entrySet(), hasSize(1));

        // здесь должно быть RateLimitExceededException
        wrappedHandleRequest(methodEndpoint);
    }

    @Test
    public void handleRequest_withMultipleRestrictions_oneRequest() {
        MethodEndpoint methodEndpoint = getMethodEndpoint("methodWithMultipleRestrictions");
        assertTrue(wrappedHandleRequest(methodEndpoint));
        verify(commandsLettuce, times(2)).set(anyString(), anyString(), any());
        verify(commandsLettuce, times(2)).incr(anyString());
        assertThat(mockLettuceStorage.entrySet(), hasSize(2));
    }

    @Test(expected = RateLimitExceededException.class)
    public void handleRequest_withMultipleRestrictions_twoRequests() {
        MethodEndpoint methodEndpoint = getMethodEndpoint("methodWithMultipleRestrictions");
        try {
            assertTrue(wrappedHandleRequest(methodEndpoint));
        } catch (RateLimitExceededException e) {
            fail("got RateLimitExceededException too early");
        }

        assumeThat(mockLettuceStorage.entrySet(), hasSize(2));

        // здесь должно быть RateLimitExceededException
        wrappedHandleRequest(methodEndpoint);
    }

    @Test
    public void handleRequest_withRestrictionAllowingFiveRequests_notTooManyRequests() {
        MethodEndpoint methodEndpoint = getMethodEndpoint("methodWithRestrictionAllowingUpToFiveRequests");

        for (int i = 0; i < 5; i++) {
            assertTrue(wrappedHandleRequest(methodEndpoint));
        }

        verify(commandsLettuce, times(5)).set(anyString(), anyString(), any());
        verify(commandsLettuce, times(5)).incr(anyString());

        assertThat(mockLettuceStorage.entrySet(), hasSize(1));
    }

    @Test(expected = RateLimitExceededException.class)
    public void handleRequest_withRestrictionAllowingFiveRequests_tooManyRequests() {
        MethodEndpoint methodEndpoint = getMethodEndpoint("methodWithRestrictionAllowingUpToFiveRequests");
        try {
            for (int i = 0; i < 5; i++) {
                assertTrue(wrappedHandleRequest(methodEndpoint));
            }
        } catch (RateLimitExceededException e) {
            fail("got RateLimitExceededException too early");
        }

        assumeThat(mockLettuceStorage.entrySet(), hasSize(1));

        // здесь должно быть RateLimitExceededException
        wrappedHandleRequest(methodEndpoint);
    }

    @Test
    public void handleRequest_withUnavailableRedis() {
        MethodEndpoint methodEndpoint = getMethodEndpoint("methodWithRestriction");

        doThrow(new LettuceExecuteException())
                .when(lettuce)
                .call(anyString(), any());

        assertTrue(wrappedHandleRequest(methodEndpoint));
    }

    @Test
    public void handleRequest_withApplicationRestriction_oneRequest() {
        when(mockedProperty.get()).thenReturn(1);

        MethodEndpoint methodEndpoint = getMethodEndpoint("methodWithApplicationRestriction");
        assertTrue(wrappedHandleRequest(methodEndpoint));
        verify(commandsLettuce).set(anyString(), anyString(), any());
        verify(commandsLettuce).incr(anyString());
        assertThat(mockLettuceStorage.entrySet(), hasSize(1));
    }

    @Test
    public void handleRequest_withClientAndApplicationRestriction_oneRequest() {
        when(mockedProperty.get()).thenReturn(1);

        MethodEndpoint methodEndpoint = getMethodEndpoint("methodWithClientAndApplicationRestriction");
        assertTrue(wrappedHandleRequest(methodEndpoint));
        verify(commandsLettuce, times(2)).set(anyString(), anyString(), any());
        verify(commandsLettuce, times(2)).incr(anyString());
        assertThat(mockLettuceStorage.entrySet(), hasSize(2));
    }

    protected MethodEndpoint getMethodEndpoint(String methodName) {
        try {
            return new MethodEndpoint(service, Service.class.getMethod(methodName));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean wrappedHandleRequest(MethodEndpoint methodEndpoint) {
        try {
            return rateLimitInterceptor.handleRequest(null, methodEndpoint);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class Service {
        @ApiMethod(service = "service", operation = "methodWithoutRestriction")
        public void methodWithoutRestriction() {
        }

        @RateLimitByClient(periodInSeconds = 3600, maxRequests = 1)
        @ApiMethod(service = "service", operation = "methodWithRestriction")
        public void methodWithRestriction() {
        }

        @RateLimitByClient(periodInSeconds = 1800, maxRequests = 1)
        @RateLimitByClient(periodInSeconds = 3600, maxRequests = 1)
        @ApiMethod(service = "service", operation = "methodWithMultipleRestrictions")
        public void methodWithMultipleRestrictions() {
        }

        @RateLimitByClient(periodInSeconds = 3600, maxRequests = 5)
        @ApiMethod(service = "service", operation = "methodWithRestrictionAllowingUpToFiveRequests")
        public void methodWithRestrictionAllowingUpToFiveRequests() {
        }

        @RateLimitByApplication(periodInSeconds = 3600)
        @ApiMethod(service = "keywordsresearch", operation = "hasSearchVolume")
        public void methodWithApplicationRestriction() {
        }

        @RateLimitByClient(periodInSeconds = 3600, maxRequests = 1)
        @RateLimitByApplication(periodInSeconds = 3600)
        @ApiMethod(service = "keywordsresearch", operation = "hasSearchVolume")
        public void methodWithClientAndApplicationRestriction() {
        }
    }
}
