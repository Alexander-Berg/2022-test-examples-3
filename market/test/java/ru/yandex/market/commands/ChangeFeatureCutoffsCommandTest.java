package ru.yandex.market.commands;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.EqualsBuilder;
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
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.shop.FunctionalTest;

public class ChangeFeatureCutoffsCommandTest extends FunctionalTest {
    static Stream<Arguments> commandNotParsedArgs() {
        return Stream.of(
                Arguments.of(commandInvocation("1")),
                Arguments.of(commandInvocation("open", "11", "DROPSHIP", "DONT_WANT")),
                Arguments.of(commandInvocation("open", "11t", "DROPSHIP", "HIDDEN")),
                Arguments.of(commandInvocation("close", "11", "dropship", "HIDDEN"))
        );
    }

    private static CommandInvocation commandInvocation(String... args) {
        return new CommandInvocation("change-feature-cutoffs", args, Collections.emptyMap());
    }

    @ParameterizedTest
    @DisplayName("Не удалось распарсить аргументы команды")
    @MethodSource("commandNotParsedArgs")
    void commandNotParsed(CommandInvocation commandInvocation) {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ChangeFeatureCutoffsCommand.ChangeFeatureCutoffsArguments.fromArgs(commandInvocation.getArguments())
        );
    }

    @Test
    @DisplayName("Корректный вызов команды")
    void commandParsed() {
        CommandInvocation commandInvocation = commandInvocation("open", "10", "DROPSHIP", "HIDDEN");
        ChangeFeatureCutoffsCommand.ChangeFeatureCutoffsArguments arguments =
                ChangeFeatureCutoffsCommand.ChangeFeatureCutoffsArguments.fromArgs(commandInvocation.getArguments());
        Assertions.assertTrue(
                EqualsBuilder.reflectionEquals(
                        arguments,
                        new ChangeFeatureCutoffsCommand.ChangeFeatureCutoffsArguments(
                                true,
                                10L,
                                FeatureType.DROPSHIP,
                                FeatureCutoffType.HIDDEN
                        )
                )
        );
    }

    @Autowired
    private ChangeFeatureCutoffsCommand changeFeatureCutoffsCommand;

    @Test
    @DbUnitDataSet(
            before = "ChangeFeatureCutoffsCommand.before.csv",
            after = "ChangeFeatureCutoffsCommand.after.csv"
    )
    void executeCommand() {
        executeCommand("open", "10", "DROPSHIP", "HIDDEN");
    }


    private void executeCommand(final String... commandArguments) {
        final CommandInvocation commandInvocation = commandInvocation(commandArguments);
        final Terminal terminal = createTerminal();

        changeFeatureCutoffsCommand.executeCommand(commandInvocation, terminal);
    }

    private Terminal createTerminal() {
        final Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.when(terminal.getWriter()).thenReturn(Mockito.mock(PrintWriter.class));
        Mockito.when(terminal.areYouSure()).thenReturn(true);
        return terminal;
    }

}
