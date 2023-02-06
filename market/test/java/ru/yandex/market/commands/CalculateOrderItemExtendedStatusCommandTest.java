package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.orders.resupply.ExtendedOrderStatusRecalculateService;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CalculateOrderItemExtendedStatusCommandTest extends FunctionalTest {

    @Autowired
    @Qualifier("namedParameterJdbcTemplate")
    private NamedParameterJdbcTemplate jdbcTemplate;

    private CommandInvocation commandInvocationMock;
    private Terminal terminalMock;

    private ExtendedOrderStatusRecalculateService orderStatusRecalculateService;

    private CalculateOrderItemExtendedStatusCommand calculateStatusCommand;

    @BeforeEach
    void setUp() {
        orderStatusRecalculateService = mock(ExtendedOrderStatusRecalculateService.class);
        commandInvocationMock = mock(CommandInvocation.class);
        terminalMock = mock(Terminal.class);
        when(terminalMock.getWriter()).thenReturn(mock(PrintWriter.class));

        calculateStatusCommand = new CalculateOrderItemExtendedStatusCommand(
                jdbcTemplate,
                orderStatusRecalculateService
        );
    }

    @Test
    @DbUnitDataSet(before = "CalculateOrderItemExtendedStatusCommandTest/calculate.before.csv")
    void testExtendedStatusRecalculation() {
        when(commandInvocationMock.getOptionValue("orderFrom")).thenReturn("92");
        when(commandInvocationMock.getOptionValue("orderTo")).thenReturn("100");

        calculateStatusCommand.executeCommand(commandInvocationMock, terminalMock);

        verify(orderStatusRecalculateService).recalculateOrdersExtendedStatus(eq(List.of(92L)));
    }
}
