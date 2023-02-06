package ru.yandex.market.apt;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static ru.yandex.market.apt.utils.ConfigurationFactory.defaultConfiguration;

class ArgumentsParserTest extends AbstractTest {
    private final ArgumentsParser argumentParser = new ArgumentsParser();

    @ParameterizedTest
    @MethodSource("validArguments")
    void parseAllArguments(String[] args, Configuration expectedConfiguration) {
        softly.assertThat(argumentParser.parseArguments(args)).isEqualToComparingFieldByField(expectedConfiguration);
    }

    @Nonnull
    private static Stream<Arguments> validArguments() {
        return Stream.of(
            Arguments.of(new String[]{
                "-processor", "fully.qualified.ProcessorName",
                "-output_directory", "path/to/output/directory",
                "input/dir/1",
                "input/dir/2",
                "input/dir/3"
            }, defaultConfiguration()),
            Arguments.of(new String[]{
                "-processor", "fully.qualified.ProcessorName",
                "-output_directory", "path/to/output/directory",
                "input/dir/1",
                "input/dir/2",
                "input/dir/3"
            }, defaultConfiguration()),
            Arguments.of(new String[]{
                "-output_directory", "path/to/output/directory",
                "input/dir/1",
                "input/dir/2",
                "input/dir/3"
            }, defaultConfiguration().setProcessorClassName(null)),
            Arguments.of(new String[]{
                "-processor", "fully.qualified.ProcessorName",
                "input/dir/1",
                "input/dir/2",
                "input/dir/3"
            }, defaultConfiguration().setOutputDirectory(null)),
            Arguments.of(new String[]{
                "-processor", "fully.qualified.ProcessorName",
                "-output_directory", "path/to/output/directory"
            }, defaultConfiguration().setInputDirectories(null))
        );
    }
}
