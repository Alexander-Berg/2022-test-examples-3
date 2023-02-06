package ru.yandex.mail.tvmlocal;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yandex.mail.tvmlocal.options.ConfigLocation;
import ru.yandex.mail.tvmlocal.options.Mode;
import ru.yandex.mail.tvmlocal.options.ResourceConfigLocation;
import ru.yandex.mail.tvmlocal.options.TvmToolOptions;

import java.util.OptionalInt;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalTvmTest {
    private static final BinarySource TVMTOOL_BINARY_SOURCE = new BinarySandboxSource();
    private static final ConfigLocation CONFIG_LOCATION = new ResourceConfigLocation("tvmtool.conf");
    private static final TvmToolOptions OPTIONS = new TvmToolOptions(OptionalInt.empty(), CONFIG_LOCATION, Mode.UNITTEST,
        emptyMap(), TvmToolOptions.generateAuthToken());

    @Test
    @DisplayName("Verify that tvmtool could be started and stopped using config from classpath resources")
    void testRunWithConfigFromResources() {
        val tool = TvmTool.start(TVMTOOL_BINARY_SOURCE, OPTIONS);
        assertTrue(tool.ping());
        tool.stop();
    }
}
