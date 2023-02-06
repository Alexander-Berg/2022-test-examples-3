package ru.yandex.mail.tvmlocal.junit_jupiter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.tvmlocal.TvmTool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithLocalTvm(TestTvmToolOptionsProvider.class)
class ExtensionTest {
    @Test
    @DisplayName("Verify that tvmtool is already running before the test execution")
    void testTvmToolIsRunning(TvmTool tool) {
        assertTrue(tool.ping());
        assertEquals(tool.getPort(), TestTvmToolOptionsProvider.PORT);
    }
}
