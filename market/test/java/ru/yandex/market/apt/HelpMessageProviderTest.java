package ru.yandex.market.apt;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class HelpMessageProviderTest extends AbstractTest {
    private final HelpMessageProvider helpMessageProvider = new HelpMessageProvider();

    @ParameterizedTest
    @MethodSource("arguments")
    void getHelpMessage(String[] args, boolean isMessageProvided) {
        Optional<String> helpMessage = helpMessageProvider.getHelpMessage(args);
        if (isMessageProvided) {
            softly.assertThat(helpMessage).isPresent();
        } else {
            softly.assertThat(helpMessage).isEmpty();
        }
    }

    private static Stream<Arguments> arguments() {
        return Stream.of(
            Arguments.of(new String[]{"-h"}, true),
            Arguments.of(new String[]{"-help"}, true),
            Arguments.of(new String[]{"--help"}, true),
            Arguments.of(new String[]{"-other_parameter", "-h"}, true),
            Arguments.of(new String[]{"-other_parameter", "-help"}, true),
            Arguments.of(new String[]{"-other_parameter", "--help"}, true),
            Arguments.of(new String[]{"-no_help_parameter"}, false),
            Arguments.of(new String[]{"-no_help_parameter1", "-no_help_parameter2"}, false)
        );
    }
}
