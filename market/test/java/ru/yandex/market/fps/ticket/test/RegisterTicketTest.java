package ru.yandex.market.fps.ticket.test;

import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.xmlrpc.XmlRpcException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.fps.balance.BalanceService;
import ru.yandex.market.fps.balance.model.BalancePassportInfo;
import ru.yandex.market.fps.ticket.RegisterTicket;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.bcp.exceptions.UniqueAttributeValidationException;

@SpringJUnitConfig(InternalModuleFpsTicketTestConfiguration.class)
@Transactional
public class RegisterTicketTest {
    @Inject
    private BcpService bcpService;
    @Inject
    private BalanceService balanceService;

    @BeforeEach
    public void setUp() throws XmlRpcException {
        Mockito.when(balanceService.getPassportByLogin(Mockito.anyString())).thenReturn(
                BalancePassportInfo.builder()
                        .setUid(321)
                        .build()
        );
    }

    @AfterEach
    public void tearDown() {
        Mockito.reset(balanceService);
    }

    @Test
    public void uniqueLoginValidationTest() {
        bcpService.create(RegisterTicket.FQN, Map.of(
                RegisterTicket.LOGIN, "asd",
                RegisterTicket.CLIENT_EMAIL, Randoms.email()
        ));
        Assertions.assertThrows(UniqueAttributeValidationException.class, () -> bcpService.create(RegisterTicket.FQN,
                Map.of(
                        RegisterTicket.LOGIN, "asd"
                )));
    }
}
