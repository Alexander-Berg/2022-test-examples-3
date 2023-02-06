package ru.yandex.market.billing.distribution.command;

import java.io.PrintWriter;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.distribution.share.DistributionShareCalculationService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Тесты команды для перерасчета вознаграждений для партнеров дистрибуции по id заказов.
 * {@link DistributionShareCalculationByOrderIdsCommand}
 */
@ExtendWith(MockitoExtension.class)
class DistributionShareCalculationByOrderIdsCommandTest {

    private static final String COMMAND_NAME = "recalculate-distribution-share-by-orders";
    private static final Long ORDER_1 = 1L;
    private static final Long ORDER_2 = 2L;
    private static final String[] ORDERS = {String.valueOf(ORDER_1), String.valueOf(ORDER_2)};

    @Mock
    private DistributionShareCalculationService distributionShareCalculationService;

    @Mock
    private Terminal terminal;

    private DistributionShareCalculationByOrderIdsCommand distributionShareCalculationByOrderIdsCommand;
    private CommandInvocation commandInvocation;

    private static Stream<Arguments> getValidCases() {
        return Stream.of(
                Arguments.of(
                        "Перерасчёт для 2 заказов",
                        ORDERS
                )
        );
    }

    private static Stream<Arguments> getNotValidCases() {
        return Stream.of(
                Arguments.of(
                        "Expected at least 1 argument with order",
                        "Перерасчет для пустого списка заказов",
                        new String[]{}
                )
        );
    }

    private void init(String[] orders) {
        distributionShareCalculationByOrderIdsCommand = new DistributionShareCalculationByOrderIdsCommand(
                distributionShareCalculationService, Clock.systemDefaultZone()
        );
        Map<String, String> options = new HashMap<>();
        commandInvocation = new CommandInvocation(COMMAND_NAME, orders, options);
        when(terminal.getWriter()).thenReturn(new PrintWriter(System.out));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Успешные кейсы выполнения команды")
    @MethodSource("getValidCases")
    void testSuccessCommand(
            String description,
            String[] orders
    ) {
        init(orders);
        distributionShareCalculationByOrderIdsCommand.executeCommand(commandInvocation, terminal);
        verify(distributionShareCalculationService).recalculateShareForOrders(
                eq(Set.of(ORDER_1, ORDER_2)), any());
        verifyNoMoreInteractions(distributionShareCalculationService);
    }

    @ParameterizedTest(name = "{1}")
    @DisplayName("Неуспешные кейсы выполнения команды")
    @MethodSource("getNotValidCases")
    void testNowSuccessCommand(
            String exceptionMessage,
            String description,
            String[] orders
    ) {
        init(orders);
        Exception exception = Assertions.assertThrows(
                Exception.class,
                () -> distributionShareCalculationByOrderIdsCommand.executeCommand(commandInvocation, terminal)
        );
        assertThat(exception.getMessage(), Matchers.startsWith(exceptionMessage));

        verifyZeroInteractions(distributionShareCalculationService);
    }

}
