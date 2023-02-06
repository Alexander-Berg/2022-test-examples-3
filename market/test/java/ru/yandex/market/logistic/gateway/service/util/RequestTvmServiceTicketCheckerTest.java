package ru.yandex.market.logistic.gateway.service.util;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;

import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.tvm.TvmAuthenticationToken;
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker;
import ru.yandex.market.logistics.util.client.tvm.exception.TvmAuthenticationException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

public class RequestTvmServiceTicketCheckerTest extends BaseTest {

    private static final String SERVICE_TICKET = "service ticket value";

    @Mock
    private TvmTicketChecker tvmTicketChecker;

    @InjectMocks
    private RequestTvmServiceTicketChecker requestTvmServiceTicketChecker;

    @Test
    public void testValid() {
        doNothing()
            .when(tvmTicketChecker)
            .checkServiceTicket(
                eq(new TvmAuthenticationToken(SERVICE_TICKET, null, null, null))
            );
        MockHttpServletRequest request = prepareRequest();

        assertions.assertThat(requestTvmServiceTicketChecker.isValid(request))
            .isTrue();
    }

    @Test
    public void testInvalid() {
        doThrow(TvmAuthenticationException.class)
            .when(tvmTicketChecker)
            .checkServiceTicket(
                eq(new TvmAuthenticationToken(SERVICE_TICKET, null, null, null))
            );
        MockHttpServletRequest request = prepareRequest();

        assertions.assertThat(requestTvmServiceTicketChecker.isValid(request))
            .isFalse();
    }

    @NotNull
    private MockHttpServletRequest prepareRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpTemplate.SERVICE_TICKET_HEADER, SERVICE_TICKET);
        return request;
    }
}
