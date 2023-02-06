package ru.yandex.market.b2b.clients.impl;

import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.b2b.balance.model.BalancePassportInfo;
import ru.yandex.market.b2b.balance.service.BalanceService;
import ru.yandex.market.b2b.clients.AbstractFunctionalTest;
import ru.yandex.market.b2b.clients.CustomersStorageService;
import ru.yandex.market.b2b.clients.EdoRoamingService;
import ru.yandex.market.b2b.clients.Randoms;
import ru.yandex.market.b2b.clients.mock.ExecuteCallMock;
import ru.yandex.mj.generated.client.b2boffice.api.B2bofficeApiClient;
import ru.yandex.mj.generated.client.b2boffice.model.EntityResponseDto;
import ru.yandex.mj.generated.server.model.CustomerWithContractDto;
import ru.yandex.mj.generated.server.model.EdoProvider;

public class EdoRoamingServiceImplTest extends AbstractFunctionalTest {
    @Autowired
    private EdoRoamingServiceImpl edoRoamingService;
    @MockBean
    private B2bofficeApiClient b2bofficeApiClient;
    @MockBean
    private CustomersStorageService storage;
    @MockBean
    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        Mockito.when(b2bofficeApiClient.createEntity(Mockito.anyString(), Mockito.anyMap()))
                .thenReturn(new ExecuteCallMock<>(new EntityResponseDto()));
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(b2bofficeApiClient);
    }

    @Test
    void testByUid_withEdo() {
        EdoProvider edoProvider = EdoProvider.DIADOC;
        String uid = Randoms.string();

        edoRoamingService.asyncSendEdoConnectionIfNeedBy(edoProvider, uid);

        Mockito.verify(b2bofficeApiClient, Mockito.times(0))
                .createEntity(Mockito.any(), Mockito.any());
    }

    @Test
    void testByInnKpp_withEdo() throws XmlRpcException {
        EdoProvider edoProvider = EdoProvider.DIADOC;
        String login = Randoms.string();
        String name = Randoms.string();
        String inn = Randoms.stringNumber();
        String kpp = Randoms.stringNumber();

        EdoRoamingService.CustomerDto customerDto = new EdoRoamingService.CustomerDto(null, login, name, inn, kpp);
        edoRoamingService.asyncSendEdoConnectionIfNeedBy(edoProvider, customerDto);

        Mockito.verify(b2bofficeApiClient, Mockito.times(0))
                .createEntity(Mockito.any(), Mockito.any());
    }

    @Test
    void testByUid_withEdoRoaming() {
        EdoProvider edoProvider = EdoProvider.EVATORPLATFORM;
        String uid = Randoms.string();
        String name = Randoms.string();
        String inn = Randoms.stringNumber();
        String kpp = Randoms.stringNumber();

        Mockito.when(storage.getUserCustomers(Mockito.eq(uid)))
                .thenReturn(List.of(new CustomerWithContractDto().name(name).inn(inn).kpp(kpp)));

        edoRoamingService.asyncSendEdoConnectionIfNeedBy(edoProvider, uid);

        Mockito.verify(b2bofficeApiClient, Mockito.times(1))
                .createEntity(Mockito.eq("edoConnection"), Mockito.eq(Map.of(
                        "title", name + " ("+ EdoProvider.EVATORPLATFORM + ")",
                        "uid", uid,
                        "inn", inn,
                        "kpp", kpp,
                        "edoProvider", edoProvider.getValue()
                )));
    }

    @Test
    void testByInnKpp_withEdoRoaming() throws XmlRpcException {
        EdoProvider edoProvider = EdoProvider.EVATORPLATFORM;
        long uid = Randoms.longValue();
        String login = Randoms.string();
        String name = Randoms.string();
        String inn = Randoms.stringNumber();
        String kpp = Randoms.stringNumber();

        Mockito.when(balanceService.getPassportByLogin(Mockito.eq(login)))
                .thenReturn(BalancePassportInfo.builder().setUid(uid).build());

        var customerDto = new EdoRoamingService.CustomerDto(null, login, name, inn, kpp);
        edoRoamingService.asyncSendEdoConnectionIfNeedBy(edoProvider, customerDto);

        Mockito.verify(b2bofficeApiClient, Mockito.times(1))
                .createEntity(Mockito.eq("edoConnection"), Mockito.eq(Map.of(
                        "title", name + " ("+ EdoProvider.EVATORPLATFORM + ")",
                        "uid", String.valueOf(uid),
                        "inn", inn,
                        "kpp", kpp,
                        "edoProvider", edoProvider.getValue()
                )));
    }
}
