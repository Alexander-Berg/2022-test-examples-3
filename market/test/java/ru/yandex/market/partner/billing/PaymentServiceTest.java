package ru.yandex.market.partner.billing;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.balance.xmlrpc.model.ResponseChoicesStructure;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest extends FunctionalTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    @Qualifier("patientBalanceService")
    private BalanceService balanceService;

    @Test
    @DbUnitDataSet(before = "csv/testSubclientPayment.before.csv")
    void testGetPaymentMethodsSubclient() {
        final var clientId = 325076L;
        final var agencyId = 222222L;
        final var campaignId = 10774L;
        final var clientInfo = new ClientInfo(clientId, ClientType.PHYSICAL, false, agencyId);
        final var uid = 12345678L;

        when(balanceService.getClients(any())).thenReturn(Map.of(clientId, clientInfo));
        when(balanceService.getRequestChoices(eq(uid), eq(-1L), eq(agencyId), any(), any()))
                .thenReturn(new ResponseChoicesStructure(Map.of()));

        final var paymentRequests = Collections.singletonList(campaignId);
        paymentService.getPaymentMethods(uid, paymentRequests, "UE");

        verify(balanceService).getRequestChoices(eq(uid), eq(-1L), eq(agencyId), any(), any());
    }
}
