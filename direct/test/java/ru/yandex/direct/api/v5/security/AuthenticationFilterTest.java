package ru.yandex.direct.api.v5.security;

import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.net.InetAddresses;
import jdk.jfr.Description;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.security.exception.TokenAbsentOrHasInvalidFormatException;
import ru.yandex.direct.api.v5.security.internal.DirectApiInternalAuthRequest;
import ru.yandex.direct.api.v5.security.ticket.TvmUserTicketAuthProvider;
import ru.yandex.direct.api.v5.security.token.DirectApiTokenAuthProvider;
import ru.yandex.direct.api.v5.security.token.DirectApiTokenAuthRequest;
import ru.yandex.direct.api.v5.ws.exceptionresolver.ApiExceptionResolver;
import ru.yandex.direct.api.v5.ws.json.JsonMessage;
import ru.yandex.direct.api.v5.ws.json.JsonMessageFactory;
import ru.yandex.direct.api.v5.ws.soap.SoapMessage;
import ru.yandex.direct.api.v5.ws.soap.SoapMessageFactory;
import ru.yandex.direct.common.util.HttpUtil;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static ru.yandex.direct.api.v5.security.AuthenticationFilter.XML_CONTENT_TYPE;
import static ru.yandex.direct.api.v5.units.UseOperatorUnitsMode.AUTO;
import static ru.yandex.direct.common.util.HttpUtil.HEADER_X_PROXY_REAL_IP;
import static ru.yandex.direct.common.util.HttpUtil.HEADER_X_REAL_IP;

public class AuthenticationFilterTest {
    private static final String REMOTE_ADDR = "127.0.0.111";
    private static final String TOKEN = "xxxxxx";
    private static final String CLIENT_LOGIN = "login-login";
    private static final String CLIENT_FAKE_LOGIN = "fake-login-login";
    private static final String TVM_TICKET = "tvm_ticket";
    private static final String serviceTicket = "serviceTicket33";

    private AuthenticationFilter authenticationFilter;
    private TvmUserTicketAuthProvider tvmUserTicketAuthProvider;
    private DirectApiTokenAuthProvider tokenAuthProvider;
    private JsonMessageFactory jsonMessageFactory;
    private SoapMessageFactory soapMessageFactory;
    private ApiExceptionResolver apiExceptionResolver;
    private HttpServletRequest request;
    private TvmIntegration tvmIntegration;

    @Before
    public void setUp() {
        ApiContext apiContext = new ApiContext();
        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(apiContext);

        request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(REMOTE_ADDR);
        when(request.getHeaders(HttpUtil.HEADER_AUTHORIZATION)).thenReturn(
                Collections.enumeration(Collections.singletonList(
                        HttpUtil.BEARER_TOKEN_TYPE + " " + TOKEN)));
        when(request.getHeader(DirectApiCredentials.CLIENT_LOGIN_HEADER)).thenReturn(CLIENT_LOGIN);
        when(request.getHeader(DirectApiCredentials.FAKE_LOGIN_HEADER)).thenReturn(CLIENT_FAKE_LOGIN);
        when(request.getHeader(DirectApiCredentials.USE_OPERATOR_UNITS_HEADER))
                .thenReturn(AUTO.toString());
        when(request.getHeader(DirectApiCredentials.TOKEN_TYPE_HEADER))
                .thenReturn(DirectApiCredentials.TOKEN_TYPE_PERSISTENT_VALUE);

        tvmUserTicketAuthProvider = mock(TvmUserTicketAuthProvider.class);
        tokenAuthProvider = mock(DirectApiTokenAuthProvider.class);
        jsonMessageFactory = mock(JsonMessageFactory.class);
        soapMessageFactory = mock(SoapMessageFactory.class);
        apiExceptionResolver = mock(ApiExceptionResolver.class);
        tvmIntegration = mock(TvmIntegration.class);

        when(jsonMessageFactory.createWebServiceMessage()).thenReturn(mock(JsonMessage.class));
        when(soapMessageFactory.createWebServiceMessage()).thenReturn(mock(SoapMessage.class));
        when(tvmIntegration.getTicket(any())).thenReturn(TVM_TICKET);

        authenticationFilter = new AuthenticationFilter(apiContextHolder, tvmUserTicketAuthProvider,
                tokenAuthProvider, jsonMessageFactory, soapMessageFactory, apiExceptionResolver,
                EnvironmentType.TESTING, tvmIntegration);
    }

    @Test
    public void correctTokenTest() throws Exception {
        ArgumentCaptor<DirectApiTokenAuthRequest> authRequestCaptor =
                ArgumentCaptor.forClass(DirectApiTokenAuthRequest.class);

        DirectApiInternalAuthRequest firstStepPreAuth = mock(DirectApiInternalAuthRequest.class);
        when(tokenAuthProvider.authenticate(any())).thenReturn(firstStepPreAuth);

        FilterChain chain = mock(FilterChain.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        authenticationFilter.doFilter(request, response, chain);

        verifyZeroInteractions(response);
        verify(chain, atLeastOnce()).doFilter(any(), any());

        verify(tokenAuthProvider).authenticate(authRequestCaptor.capture());
        DirectApiCredentials credentials = authRequestCaptor.getValue().getCredentials();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(credentials.getOauthToken()).isEqualTo(TOKEN);
            softly.assertThat(credentials.getUserIp()).isEqualTo(InetAddresses.forString(REMOTE_ADDR));
            softly.assertThat(credentials.getClientLogin()).isEqualTo(CLIENT_LOGIN);
            softly.assertThat(credentials.getFakeLogin()).isEqualTo(CLIENT_FAKE_LOGIN);
            softly.assertThat(credentials.getUseOperatorUnitsMode()).isEqualTo(AUTO);
            softly.assertThat(credentials.isTokenPersistent()).isEqualTo(true);
        });
    }

    @Test
    public void invalidTokenJsonTest() throws Exception {
        when(tokenAuthProvider.authenticate(any())).thenThrow(TokenAbsentOrHasInvalidFormatException.class);
        when(request.getRequestURI()).thenReturn("/json/v5/test");

        FilterChain chain = mock(FilterChain.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        authenticationFilter.doFilter(request, response, chain);

        verifyZeroInteractions(chain);
        verifyZeroInteractions(soapMessageFactory);
        verify(jsonMessageFactory).createWebServiceMessage();
        verify(apiExceptionResolver).resolveException(any(), any());
        verify(response).addHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE);
    }

    @Test
    public void invalidTokenSoapTest() throws Exception {
        when(tokenAuthProvider.authenticate(any())).thenThrow(TokenAbsentOrHasInvalidFormatException.class);
        when(request.getRequestURI()).thenReturn("/v5/test");

        FilterChain chain = mock(FilterChain.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        authenticationFilter.doFilter(request, response, chain);

        verifyZeroInteractions(chain);
        verifyZeroInteractions(jsonMessageFactory);
        verify(soapMessageFactory).createWebServiceMessage();
        verify(apiExceptionResolver).resolveException(any(), any());
        verify(response).addHeader(CONTENT_TYPE, XML_CONTENT_TYPE);
    }

    @Test
    @Description("Если установлены оба хедера HEADER_X_REAL_IP и HEADER_X_PROXY_REAL_IP и по tvm мы идём из сервиса " +
            "которому разрешено проставлять HEADER_X_PROXY_REAL_IP, то используем HEADER_X_PROXY_REAL_IP")
    public void xProxyRealIpHeaderTest() throws Exception {
        ipHeadersTest(TvmService.DIRECT_API_TEST, true);
    }

    @Test
    @Description("Если установлены оба хедера HEADER_X_REAL_IP и HEADER_X_PROXY_REAL_IP и по tvm мы идём из сервиса " +
            "которому НЕ разрешено проставлять HEADER_X_PROXY_REAL_IP, то используем HEADER_X_REAL_IP")
    public void xProxyIpHeaderTest() throws Exception {
        ipHeadersTest(TvmService.DIRECT_ESS_PROD, false);
    }

    private void ipHeadersTest(TvmService tvmService, boolean xProxyRealIpAllowed) throws Exception {
        when(request.getHeader(TvmIntegration.SERVICE_TICKET_HEADER)).thenReturn(serviceTicket);
        when(tvmIntegration.getTvmService(serviceTicket)).thenReturn(tvmService);
        String xProxyRealIpValue = "1.2.3.4";
        when(request.getHeader(HEADER_X_PROXY_REAL_IP)).thenReturn(xProxyRealIpValue);
        String xRealIpValue = "100.2.3.4";
        when(request.getHeader(HEADER_X_REAL_IP)).thenReturn(xRealIpValue);

        ArgumentCaptor<DirectApiTokenAuthRequest> authRequestCaptor =
                ArgumentCaptor.forClass(DirectApiTokenAuthRequest.class);

        DirectApiInternalAuthRequest firstStepPreAuth = mock(DirectApiInternalAuthRequest.class);
        when(tokenAuthProvider.authenticate(any())).thenReturn(firstStepPreAuth);

        FilterChain chain = mock(FilterChain.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        authenticationFilter.doFilter(request, response, chain);

        verify(tokenAuthProvider).authenticate(authRequestCaptor.capture());
        DirectApiCredentials credentials = authRequestCaptor.getValue().getCredentials();

        assertThat(credentials.getUserIp()).isEqualTo(InetAddresses.forString(xProxyRealIpAllowed ?
                xProxyRealIpValue : xRealIpValue));
    }

}
