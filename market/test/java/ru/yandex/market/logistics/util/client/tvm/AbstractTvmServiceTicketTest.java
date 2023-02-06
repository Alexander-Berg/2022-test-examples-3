package ru.yandex.market.logistics.util.client.tvm;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.util.client.tvm.client.TvmServiceTicket;
import ru.yandex.market.logistics.util.client.tvm.client.TvmTicketStatus;
import ru.yandex.market.logistics.util.client.tvm.client.TvmUserTicket;
import ru.yandex.market.logistics.util.client.tvm.exception.TvmAuthenticationException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.util.client.tvm.TvmErrorType.BAD_SERVICE_TICKET;
import static ru.yandex.market.logistics.util.client.tvm.TvmErrorType.NOT_ALLOWED_SERVICE;
import static ru.yandex.market.logistics.util.client.tvm.TvmErrorType.SERVICE_TICKET_NOT_PRESENT;
import static ru.yandex.market.logistics.util.client.tvm.TvmErrorType.USER_TICKET_NOT_PRESENT;

abstract class AbstractTvmServiceTicketTest extends AbstractContextualTest {

    private static final String SERVICE_TICKET = "sticket1";
    private static final String REMOTE_ADDRESS = "http://remote.address";
    protected static final String REQUEST_URI = "/test";
    private static final String USER_TICKET = "uticket";

    @Test
    public void ticketNotPresent() {
        checkAuth(SERVICE_TICKET_NOT_PRESENT, "");
    }

    @Test
    public void invalidTicket() {
        TvmServiceTicket serviceTicket = mock(TvmServiceTicket.class);
        when(tvmClient.checkServiceTicket("tbody"))
            .thenAnswer(i -> serviceTicket);
        when(serviceTicket.getStatus())
            .thenReturn(TvmTicketStatus.SIGN_BROKEN);

        checkAuth(BAD_SERVICE_TICKET, "tbody");
    }

    @Test
    public void notAllowedTicket() {
        TvmServiceTicket serviceTicket = mock(TvmServiceTicket.class);
        when(tvmClient.checkServiceTicket("tbody"))
            .thenAnswer(i -> serviceTicket);
        when(serviceTicket.getStatus())
            .thenReturn(TvmTicketStatus.OK);
        when(serviceTicket.getServiceId())
            .thenReturn(42);

        checkAuth(NOT_ALLOWED_SERVICE, "tbody");
    }

    @Test
    public void noUserTicket() {
        TvmServiceTicket serviceTicket = mock(TvmServiceTicket.class);
        when(tvmClient.checkServiceTicket("tbody"))
            .thenAnswer(i -> serviceTicket);
        when(serviceTicket.getStatus())
            .thenReturn(TvmTicketStatus.OK);
        when(serviceTicket.getServiceId())
            .thenReturn(1);

        checkAuth(USER_TICKET_NOT_PRESENT, "tbody");
    }

    @Test
    public void okTicket() {
        TvmServiceTicket serviceTicket = mock(TvmServiceTicket.class);
        when(tvmClient.checkServiceTicket(SERVICE_TICKET)).thenAnswer(i -> serviceTicket);
        when(serviceTicket.getStatus()).thenReturn(TvmTicketStatus.OK);
        when(serviceTicket.getServiceId()).thenReturn(1);

        TvmUserTicket userTicket = mock(TvmUserTicket.class);
        when(tvmClient.checkUserTicket(USER_TICKET)).thenAnswer(i -> userTicket);
        when(userTicket.getStatus()).thenReturn(TvmTicketStatus.OK);

        TvmAuthenticationToken authentication = new TvmAuthenticationToken(
            SERVICE_TICKET,
            USER_TICKET,
            REMOTE_ADDRESS,
            REQUEST_URI
        );
        authenticationManager.authenticate(authentication);
    }

    public void checkAuth(TvmErrorType error, String ticket) {
        TvmAuthenticationToken authentication = new TvmAuthenticationToken(ticket, "", REMOTE_ADDRESS, REQUEST_URI);
        softly.assertThatThrownBy(() -> authenticationManager.authenticate(authentication))
            .isInstanceOf(TvmAuthenticationException.class)
            .hasMessage(
                "Auth fail " + error + ". "
                    + "Token: TvmAuthenticationToken("
                    + "serviceTicket=" + ticket + ", "
                    + "userTicket=, "
                    + "remoteAddress=" + REMOTE_ADDRESS + ", "
                    + "requestUri=" + REQUEST_URI + ", "
                    + "tvmServiceId=" + authentication.getTvmServiceId()
                    + ")."
            );
    }
}
