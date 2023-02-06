package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramService;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramStatus;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.shop.FunctionalTest;

@DbUnitDataSet(before = "ManagePartnerProgramCommand.before.csv")
public class ManagePartnerProgramCommandTest extends FunctionalTest {

    @Autowired
    private ManagePartnerProgramCommand managePartnerProgramCommand;

    @Autowired
    private PartnerPlacementProgramService partnerPlacementProgramService;

    private static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(
                        new CommandInvocation("manage-partner-program",
                                new String[]{"set", "10", "DROPSHIP", "DISABLED"},
                                Collections.emptyMap())
                ),
                Arguments.of(
                        new CommandInvocation("manage-partner-program",
                                new String[]{"set", "10", "CROSSDOCK", "SUCCESS"},
                                Collections.emptyMap())
                ),
                Arguments.of(
                        new CommandInvocation("manage-partner-program",
                                new String[]{"set", "11", "FULFILLMENT", "SUCCESS"},
                                Collections.emptyMap())
                )
        );
    }

    private static Stream<Arguments> wrongData() {
        return Stream.of(
                Arguments.of(
                        new CommandInvocation("manage-partner-program",
                                new String[]{"set", "10"},
                                Collections.emptyMap())
                ),
                Arguments.of(
                        new CommandInvocation("manage-partner-program",
                                new String[]{"set", "10", "CROSSDOCK"},
                                Collections.emptyMap())
                ),
                Arguments.of(
                        new CommandInvocation("manage-partner-program",
                                new String[]{"ololo", "11", "FULFILLMENT", "SUCCESS"},
                                Collections.emptyMap())
                ),
                Arguments.of(
                        new CommandInvocation("manage-partner-program",
                                new String[]{},
                                Collections.emptyMap())
                ),
                Arguments.of(
                        new CommandInvocation("manage-partner-program",
                                new String[]{"1", "11", "1", "1", "1"},
                                Collections.emptyMap())
                ),
                Arguments.of(
                        new CommandInvocation("manage-partner-program",
                                new String[]{"set", "11", "1", "NO"},
                                Collections.emptyMap())
                )
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    @DisplayName("Проверка подключения и отключения программ")
    public void executeSetCommand(CommandInvocation commandInvocation) {
        Terminal terminal = createTerminal();
        long partnerId = Long.parseLong(commandInvocation.getArgument(1));
        PartnerPlacementProgramType expectedType =
                PartnerPlacementProgramType.valueOf(commandInvocation.getArgument(2));
        PartnerPlacementProgramStatus expectedStatus =
                PartnerPlacementProgramStatus.valueOf(commandInvocation.getArgument(3));
        managePartnerProgramCommand.executeCommand(commandInvocation, terminal);
        Assert.assertEquals(expectedType,
                partnerPlacementProgramService.getPartnerPlacementProgram(partnerId, expectedType)
                        .orElseThrow().getProgram());
        Assert.assertEquals(expectedStatus,
                partnerPlacementProgramService.getPartnerPlacementProgram(partnerId, expectedType)
                        .orElseThrow().getStatus());
    }

    @ParameterizedTest
    @MethodSource("wrongData")
    @DisplayName("Плохие данные")
    public void wrongArgs(CommandInvocation commandInvocation) {
        Terminal terminal = createTerminal();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> managePartnerProgramCommand.executeCommand(commandInvocation, terminal)
        );
    }

    @Test
    @DisplayName("Проверка удаления программы")
    public void executeDeleteCommand() {
        CommandInvocation commandInvocation = new CommandInvocation("manage-partner-program",
                new String[]{"delete", "10", "FULFILLMENT"},
                Collections.emptyMap());
        long partnerId = 10;
        PartnerPlacementProgramType programType = PartnerPlacementProgramType.FULFILLMENT;
        managePartnerProgramCommand.executeCommand(commandInvocation, createTerminal());
        Assert.assertFalse(partnerPlacementProgramService.getPartnerPlacementProgram(partnerId, programType)
                .isPresent());
    }

    private Terminal createTerminal() {
        final Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        Mockito.when(terminal.areYouSure()).thenReturn(true);
        return terminal;
    }
}
