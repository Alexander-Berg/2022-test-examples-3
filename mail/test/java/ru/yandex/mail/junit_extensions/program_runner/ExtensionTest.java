package ru.yandex.mail.junit_extensions.program_runner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.mail.junit_extensions.program_runner.TestProgramRegistry.SIMPLE_JAVA;

@RunProgram(name = SIMPLE_JAVA)
@RegisterProgramsRegistry(TestProgramRegistry.class)
class ExtensionTest {
    @Test
    @DisplayName("Verify that java program is running before test")
    void testJavaProgramRun(@BindProcess(SIMPLE_JAVA) Process process) {
        assertThat(process.isAlive())
            .isTrue();
    }
}
