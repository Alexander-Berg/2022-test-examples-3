package ru.yandex.mail.junit_extensions.program_runner;

import ru.yandex.mail.arcadia_test_support.TestPaths;
import ru.yandex.mail.junit_extensions.program_runner.ProgramOptions.JavaProgramOptions;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class TestProgramRegistry implements ProgramRegistry {
    public static final String SIMPLE_JAVA = "simple_java";
    private static final Path SIMPLE_JAVA_PATH = TestPaths.getBuildPath(
        "mail", "java", "junit_extensions", "program_runner", "ut", "simple_java_daemon", "ut-simple_java_daemon.jar");

    private static final JavaProgramOptions OPTIONS = new JavaProgramOptions(SIMPLE_JAVA_PATH, Optional.empty(), emptyList(),
        "ru.yandex.mail.Main", ProgramOptions.NO_HEALTH_CHECK, Optional.empty());

    private static final Map<String, ProgramOptions> PROGRAMS = Map.of(
        SIMPLE_JAVA, OPTIONS
    );

    @Override
    public Optional<ProgramOptions> findProgramOptions(String id) {
        return Optional.ofNullable(PROGRAMS.get(id));
    }
}
