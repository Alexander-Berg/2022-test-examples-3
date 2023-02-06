package ru.yandex.market.cpa;

import java.io.PrintWriter;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PushDataToCheckoutCommandTest extends FunctionalTest {

    @Autowired
    private PushDataToCheckoutCommand tested;
    @Autowired
    private CheckouterAPI checkouterClient;
    private CheckouterShopApi shopApi;

    @BeforeEach
    void init() {
        shopApi = mock(CheckouterShopApi.class);
        when(checkouterClient.shops()).thenReturn(shopApi);
    }

    @Test
    @DbUnitDataSet(before = "PushDataToCheckoutCommandTest.dsbs.before.csv")
    void testExportDsbsToCheckouter() {
        executeCommand("shops");

        // Проверяем отправку обновлений в чекаутер
        verify(shopApi, times(2)).updateShopData(anyLong(), any());
    }

    private void executeCommand(final String... commandArguments) {
        final CommandInvocation commandInvocation = commandInvocation(commandArguments);
        final Terminal terminal = createTerminal();

        tested.executeCommand(commandInvocation, terminal);
    }

    private Terminal createTerminal() {
        final Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        return terminal;
    }

    private CommandInvocation commandInvocation(String... args) {
        return new CommandInvocation("checkouter-settings", args, Collections.emptyMap());
    }


}
