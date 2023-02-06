package ru.yandex.market.fulfillment.stockstorage.security.tvm;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import ru.yandex.market.logistics.util.client.tvm.TvmAuthenticationFilter;
import ru.yandex.market.logistics.util.client.tvm.TvmAuthenticationToken;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TvmAuthenticationFilterTest {

    private static final String SERVICE_TICKET_HEADER = "X-Ya-Service-Ticket";
    private static final String USER_SERVICE_TICKET_HEADER = "X-Ya-User-Ticket";

    private static final String SERVICE_TICKET_VALUE = "ServiceTicket";
    private static final String USER_TICKET_VALUE = "UserTicket";
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private AuthenticationFailureHandler authenticationFailureHandler;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void attemptAuthenticationWithValidTicketsSucceeded() {
        TvmAuthenticationFilter tvmAuthenticationFilter =
                new TvmAuthenticationFilter(authenticationManager, authenticationFailureHandler);
        Authentication authentication =
                new TvmAuthenticationToken(SERVICE_TICKET_VALUE, USER_TICKET_VALUE, "127.0.0.1", "");
        when(authenticationManager.authenticate(authentication)).thenReturn(authentication);

        HttpServletRequest request = getRequest(SERVICE_TICKET_VALUE, USER_TICKET_VALUE);
        tvmAuthenticationFilter.attemptAuthentication(request, null);

        verify(authenticationManager).authenticate(authentication);
    }

    private HttpServletRequest getRequest(String serviceTicketValue, String userTicketValue) {
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.addHeader(SERVICE_TICKET_HEADER, serviceTicketValue);
        httpServletRequest.addHeader(USER_SERVICE_TICKET_HEADER, userTicketValue);
        return httpServletRequest;
    }
}
