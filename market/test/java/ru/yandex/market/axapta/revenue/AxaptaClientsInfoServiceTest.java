package ru.yandex.market.axapta.revenue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.balance.xmlrpc.model.ContractType;
import ru.yandex.market.common.balance.xmlrpc.model.PersonStructure;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.axapta.revenue.dao.AxaptaClientDao;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientContractInfo;

@DbUnitDataSet(
        before = "AxaptaClientsInfoServiceTest.before.csv",
        after = "AxaptaClientsInfoServiceTest.after.csv"
)
class AxaptaClientsInfoServiceTest extends FunctionalTest {

    @Autowired
    private AxaptaClientDao axaptaClientDao;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private AxaptaClientsInfoService axaptaClientsInfoService;

    @BeforeEach
    void setup() {
        BalanceService balanceService = Mockito.mock(BalanceService.class);

        Mockito.doReturn(List.of(
                createPersonStructure(101, 1001),
                createPersonStructure(101, 1002),
                createPersonStructure(101, 1003)
        )).when(balanceService).getClientPersons(Mockito.eq(101L), Mockito.anyInt());

        Mockito.doReturn(List.of(
                createPersonStructure(102, 1004)
        )).when(balanceService).getClientPersons(Mockito.eq(102L), Mockito.anyInt());

        Mockito.doReturn(List.of(
                createClientContractInfo(10001, 1001, true, new int[]{701, 702}),
                createClientContractInfo(10002, 1001, false, new int[]{701}),
                createClientContractInfo(10003, 1001, true, new int[]{701}),
                createClientContractInfo(10004, 1002, true, new int[]{})
        )).when(balanceService).getClientContracts(Mockito.eq(101L), Mockito.any(ContractType.class));

        axaptaClientsInfoService = new AxaptaClientsInfoService(balanceService,
                axaptaClientDao, transactionTemplate
        );
    }

    private static PersonStructure createPersonStructure(long clientId, long personId) {
        PersonStructure personStructure = new PersonStructure();
        personStructure.setClientId(clientId);
        personStructure.setPersonId(personId);
        return personStructure;
    }

    private static ClientContractInfo createClientContractInfo(
            int contractId, int personId, boolean signed, int[] services
    ) {
        return new ClientContractInfo.ClientContractInfoBuilder()
                .withId(contractId)
                .withPersonId(personId)
                .isSigned(signed)
                .withServices(services)
                .build();
    }

    @Test
    void testUpdateClientsInfo() {
        axaptaClientsInfoService.updateClientsInfo();
    }
}
