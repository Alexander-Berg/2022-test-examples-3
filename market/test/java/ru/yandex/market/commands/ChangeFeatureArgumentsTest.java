package ru.yandex.market.commands;

import java.util.Collections;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.param.model.ParamCheckStatus;

/**
 * Тесты на парсинг аргументов команды {@link ChangeFeatureCommand}.
 */
class ChangeFeatureArgumentsTest {

    static Stream<Arguments> commandNotParsedArgs() {
        return Stream.of(
                Arguments.of(commandInvocation("1")),
                Arguments.of(commandInvocation("qwerty", "DROPSHIP", "DONT_WANT")),
                Arguments.of(commandInvocation("1", "Funny feature type", "DONT WANT")),
                Arguments.of(commandInvocation("1", "DROPSHIP", "do-not-want"))
        );
    }

    private static CommandInvocation commandInvocation(String... args) {
        return new CommandInvocation("change-feature-feature", args, Collections.emptyMap());
    }

    @ParameterizedTest
    @DisplayName("Не удалось распарсить аргументы команды")
    @MethodSource("commandNotParsedArgs")
    void commandNotParsed(CommandInvocation commandInvocation) {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> ChangeFeatureCommand.ChangeFeatureArguments.fromArgs(commandInvocation.getArguments())
        );
    }

    @Test
    @DisplayName("Корректный вызов команды")
    void commandParsed() {
        CommandInvocation commandInvocation = commandInvocation("10", "DROPSHIP", "DONT_WANT");
        ChangeFeatureCommand.ChangeFeatureArguments arguments =
                ChangeFeatureCommand.ChangeFeatureArguments.fromArgs(commandInvocation.getArguments());
        Assertions.assertTrue(
                EqualsBuilder.reflectionEquals(
                        arguments,
                        new ChangeFeatureCommand.ChangeFeatureArguments(
                                10L,
                                FeatureType.DROPSHIP,
                                ParamCheckStatus.DONT_WANT
                        )
                )
        );
    }
}
