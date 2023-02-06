package ru.yandex.direct.api.v5.security;

import org.junit.Test;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.MethodEndpoint;

import ru.yandex.direct.api.v5.ws.annotation.ApiMethod;
import ru.yandex.direct.api.v5.ws.annotation.ApiRequest;
import ru.yandex.direct.api.v5.ws.annotation.ApiResponse;
import ru.yandex.direct.api.v5.ws.annotation.ApiServiceEndpoint;
import ru.yandex.direct.api.v5.ws.annotation.ApiServiceType;
import ru.yandex.direct.api.v5.ws.annotation.ServiceType;
import ru.yandex.direct.core.security.authorization.PreAuthorizeRead;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author egorovmv
 */
public final class ServiceAuthInterceptorTest {
    private static final String SOME_OPERATION = "someOperation";

    @Test
    public void testAuthorizationHelperInvocation() throws Exception {
        DirectApiAuthentication authenticationMock = mock(DirectApiAuthentication.class);

        ApiAuthenticationSource authenticationSourceMock = mock(ApiAuthenticationSource.class);
        when(authenticationSourceMock.getAuthentication()).thenReturn(authenticationMock);

        DirectApiAuthorizationHelper authorizationHelperMock = mock(DirectApiAuthorizationHelper.class);

        ServiceAuthInterceptor serviceAuthInterceptor = new ServiceAuthInterceptor(
                authenticationSourceMock, authorizationHelperMock);

        serviceAuthInterceptor.handleRequest(
                mock(MessageContext.class),
                new MethodEndpoint(
                        new SomeEndpoint(),
                        SomeEndpoint.class.getMethod(
                                SOME_OPERATION, String.class)));

        verify(authorizationHelperMock)
                .authorize(
                        eq(authenticationMock),
                        eq(ServiceType.CLIENT),
                        eq(SOME_OPERATION));
    }

    @ApiServiceEndpoint
    @ApiServiceType(type = ServiceType.CLIENT)
    public static class SomeEndpoint {
        @PreAuthorizeRead
        @ApiMethod(service = "SomeService", operation = SOME_OPERATION)
        @ApiResponse
        public String someOperation(@ApiRequest String p) {
            return "";
        }
    }
}
