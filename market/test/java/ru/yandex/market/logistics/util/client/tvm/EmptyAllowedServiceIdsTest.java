package ru.yandex.market.logistics.util.client.tvm;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import ru.yandex.market.logistics.util.client.tvm.client.TvmServiceTicket;
import ru.yandex.market.logistics.util.client.tvm.client.TvmTicketStatus;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.util.client.tvm.AbstractTvmServiceTicketTest.REQUEST_URI;

@TestPropertySource("/empty-allowed-service-ids.properties")
class EmptyAllowedServiceIdsTest extends AbstractContextualTest {

    @Test
    public void emptyAllowedServiceIdsTest() {
        TvmServiceTicket serviceTicket = mock(TvmServiceTicket.class);
        when(tvmClient.checkServiceTicket("tbody"))
            .thenAnswer(i -> serviceTicket);
        when(serviceTicket.getStatus())
            .thenReturn(TvmTicketStatus.OK);
        when(serviceTicket.getServiceId())
            .thenReturn(42);

        TvmAuthenticationToken authentication = new TvmAuthenticationToken("tbody", "", "", REQUEST_URI);
        authenticationManager.authenticate(authentication);
    }
}
