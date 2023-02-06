package ru.yandex.market.supplier.command;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.api.cpa.checkout.AsyncCheckouterService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Проверяем работу {@link CreateSupplierContractsCommand}.
 */
class CreateSupplierContractsCommandTest extends FunctionalTest {

    @Autowired
    private CreateSupplierContractsCommand createSupplierContractsCommand;
    @Autowired
    private Terminal terminal;
    @Autowired
    private PrintWriter printWriter;

    @Autowired
    private AsyncCheckouterService asyncCheckouterService;
    @Autowired
    @Qualifier("patientBalanceService")
    private BalanceService balanceService;

    @BeforeEach
    private void init() {
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @DisplayName("Создание контракта для DSBS(DBS) партнера")
    @DbUnitDataSet(before = "CreateSupplierContractsCommandTest.before.csv")
    @ParameterizedTest(name = "partnerId = {0}")
    @CsvSource({"1000, true", "1001, true", "1002,false"})
    void executeCommandCreateSupplierContractsTest(String partnerId, boolean syncWithCheckout) {
        CommandInvocation commandInvocation = new CommandInvocation("create-supplier-contracts",
                new String[]{partnerId},
                Collections.emptyMap());

        createSupplierContractsCommand.executeCommand(commandInvocation, terminal);

        if (syncWithCheckout) {
            verify(asyncCheckouterService, times(1)).pushPartnerSettingsToCheckout(anySet());
            verify(balanceService, times(2)).createOrUpdatePerson(any(), anyLong());
            verify(balanceService, times(1)).createOffer(any(), anyLong());
        } else {
            verify(asyncCheckouterService, never()).pushPartnerSettingsToCheckout(anySet());
        }
    }
}

