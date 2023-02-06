package ru.yandex.market.billing.factoring.command;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import ru.yandex.common.util.terminal.CommandExecutor;
import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.core.factoring.PayoutFrequency;
import ru.yandex.market.billing.factoring.dao.ClientFactorDao;
import ru.yandex.market.billing.factoring.model.ClientFactor;
import ru.yandex.market.core.payment.PaymentOrderFactoring;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.billing.factoring.command.FactoringCommand.ACTION_REMOVE;
import static ru.yandex.market.billing.factoring.command.FactoringCommand.ACTION_SET;
import static ru.yandex.market.billing.factoring.command.FactoringCommand.COMMAND_NAME;
import static ru.yandex.market.billing.factoring.command.FactoringCommand.FACTOR_OPTION;
import static ru.yandex.market.billing.factoring.command.FactoringCommand.FREQUENCY_OPTION;

@ExtendWith(MockitoExtension.class)
public class FactoringCommandTest {
    private static final long FIRST_CONTRACT_ID = 123;
    private static final long SECOND_CONTRACT_ID = 45;
    private static final String[] CONTRACT_IDS =
            {String.valueOf(FIRST_CONTRACT_ID), String.valueOf(SECOND_CONTRACT_ID)};

    @Mock
    private ClientFactorDao clientFactorDao;

    @Mock
    private Terminal terminal;

    @Mock
    private CommandExecutor commandExecutor;

    private FactoringCommand testing;

    private void init() {
        testing = new FactoringCommand(clientFactorDao, commandExecutor);

        when(terminal.getWriter()).thenReturn(new PrintWriter(System.out));
        when(terminal.confirm(anyString())).thenReturn(true);
    }

    @DisplayName("ВЫполнение команды со всеми опциями")
    @Test
    public void executeSetCommandWithAllOptions() {
        init();

        CommandInvocation commandInvocation = new CommandInvocation(
                COMMAND_NAME,
                ArrayUtils.addAll(new String[]{ACTION_SET}, CONTRACT_IDS),
                Map.of(FACTOR_OPTION, PaymentOrderFactoring.MARKET.getId(),
                        FREQUENCY_OPTION, PayoutFrequency.DAILY.getId())
        );

        testing.executeCommand(commandInvocation, terminal);

        verify(clientFactorDao).persistClientFactors(
                eq(Set.of(
                        new ClientFactor(FIRST_CONTRACT_ID, PayoutFrequency.DAILY, PaymentOrderFactoring.MARKET),
                        new ClientFactor(SECOND_CONTRACT_ID, PayoutFrequency.DAILY, PaymentOrderFactoring.MARKET)
                ))
        );

        verifyNoMoreInteractions(clientFactorDao);
    }

    @DisplayName("Выполнение команды без указания контрактов")
    @Test
    public void executeSetCommandWoContracts() {
        init();

        CommandInvocation commandInvocation = new CommandInvocation(
                COMMAND_NAME,
                new String[]{ACTION_SET},
                Map.of(FACTOR_OPTION, PaymentOrderFactoring.RAIFFEISEN.getId(),
                        FREQUENCY_OPTION, PayoutFrequency.DAILY.getId())
        );

        testing.executeCommand(commandInvocation, terminal);

        verify(clientFactorDao).persistClientFactors(
                eq(Set.of(
                        new ClientFactor(null, PayoutFrequency.DAILY, PaymentOrderFactoring.RAIFFEISEN)
                ))
        );

        verifyNoMoreInteractions(clientFactorDao);
    }

    @DisplayName("Выполнение команды без опции с частотой выплат")
    @Test
    public void executeSetCommandWoFrequency() {
        init();

        CommandInvocation commandInvocation = new CommandInvocation(
                COMMAND_NAME,
                ArrayUtils.addAll(new String[]{ACTION_SET}, CONTRACT_IDS),
                Map.of(FACTOR_OPTION, PaymentOrderFactoring.RAIFFEISEN.getId())
        );

        testing.executeCommand(commandInvocation, terminal);

        verify(clientFactorDao).persistClientFactors(
                eq(Set.of(
                        new ClientFactor(FIRST_CONTRACT_ID, PayoutFrequency.DAILY, PaymentOrderFactoring.RAIFFEISEN),
                        new ClientFactor(FIRST_CONTRACT_ID, PayoutFrequency.WEEKLY, PaymentOrderFactoring.RAIFFEISEN),
                        new ClientFactor(FIRST_CONTRACT_ID, PayoutFrequency.BI_WEEKLY,
                                PaymentOrderFactoring.RAIFFEISEN),
                        new ClientFactor(FIRST_CONTRACT_ID, PayoutFrequency.MONTHLY, PaymentOrderFactoring.RAIFFEISEN),
                        new ClientFactor(FIRST_CONTRACT_ID, PayoutFrequency.COURIER, PaymentOrderFactoring.RAIFFEISEN),
                        new ClientFactor(FIRST_CONTRACT_ID, PayoutFrequency.SELF_EMPLOYED_COURIER,
                                PaymentOrderFactoring.RAIFFEISEN),
                        new ClientFactor(SECOND_CONTRACT_ID, PayoutFrequency.DAILY, PaymentOrderFactoring.RAIFFEISEN),
                        new ClientFactor(SECOND_CONTRACT_ID, PayoutFrequency.WEEKLY, PaymentOrderFactoring.RAIFFEISEN),
                        new ClientFactor(SECOND_CONTRACT_ID, PayoutFrequency.BI_WEEKLY,
                                PaymentOrderFactoring.RAIFFEISEN),
                        new ClientFactor(SECOND_CONTRACT_ID, PayoutFrequency.MONTHLY, PaymentOrderFactoring.RAIFFEISEN),
                        new ClientFactor(SECOND_CONTRACT_ID, PayoutFrequency.COURIER, PaymentOrderFactoring.RAIFFEISEN),
                        new ClientFactor(SECOND_CONTRACT_ID, PayoutFrequency.SELF_EMPLOYED_COURIER,
                                PaymentOrderFactoring.RAIFFEISEN)
                ))
        );

        verifyNoMoreInteractions(clientFactorDao);
    }

    @DisplayName("Удаление настроек для контрактов")
    @Test
    public void executeRemoveCommand() {
        init();

        CommandInvocation commandInvocation = new CommandInvocation(
                COMMAND_NAME,
                ArrayUtils.addAll(new String[]{ACTION_REMOVE}, CONTRACT_IDS),
                Map.of()
        );

        testing.executeCommand(commandInvocation, terminal);

        verify(clientFactorDao).removeClientFactors(
                argThat(arg -> {
                    MatcherAssert.assertThat(
                            arg,
                            containsInAnyOrder(FIRST_CONTRACT_ID, SECOND_CONTRACT_ID));
                    return true;
                }));

        verifyNoMoreInteractions(clientFactorDao);
    }
}
