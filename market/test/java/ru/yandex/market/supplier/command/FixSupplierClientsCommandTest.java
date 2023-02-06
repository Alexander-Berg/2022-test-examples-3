package ru.yandex.market.supplier.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.balance.model.ClientType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class FixSupplierClientsCommandTest extends FunctionalTest {

    @Autowired
    private FixSupplierClientsCommand clientsCommand;

    @Autowired
    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        Mockito.reset(balanceService);
    }


    @DbUnitDataSet(
            before = "FixSupplierClientsCommandTest.single.before.csv",
            after = "FixSupplierClientsCommandTest.single.before.csv")
    @Test
    void testIfExists() {
        when(balanceService.getClient(101L)).thenReturn(new ClientInfo(101L, ClientType.OOO));
        clientsCommand.createClientIfNotExist(101L);
        verify(balanceService, never()).createClient(any(), anyLong(), anyLong());
    }

    @DbUnitDataSet(
            before = "FixSupplierClientsCommandTest.single.before.csv",
            after = "FixSupplierClientsCommandTest.ifNotExist.after.csv")
    @Test
    void testIfNotExist() {
        when(balanceService.getClient(101L)).thenReturn(null);
        when(balanceService.createClient(any(), anyLong(), anyLong())).thenReturn(102L);
        clientsCommand.createClientIfNotExist(101L);
        verify(balanceService).createClient(any(), anyLong(), anyLong());
    }

    @DbUnitDataSet(
            before = "FixSupplierClientsCommandTest.clone.before.csv",
            after = "FixSupplierClientsCommandTest.clone.after.csv")
    @Test
    public void testIfNotExistWithClones() {
        when(balanceService.getClient(101L)).thenReturn(null);
        when(balanceService.createClient(any(), anyLong(), anyLong())).thenReturn(102L);
        clientsCommand.createClientIfNotExist(101L);
        verify(balanceService).createClient(any(), anyLong(), anyLong());
    }
}
