package ru.yandex.market.mboc.common.utils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("checkstyle:MagicNumber")
public class MultiwatchTest {
    private static final Logger log = LoggerFactory.getLogger(MultiwatchTest.class);
    private Multiwatch multiwatch = new Multiwatch("TestExecutor");

    @Test
    public void testMultiwatch() throws InterruptedException {
        multiwatch.startGlobal();

        multiwatch.startLocal("Loading");
        Thread.sleep(10);
        multiwatch.stopLocal("Loading");

        Thread.sleep(10);
        multiwatch.startLocal("Heavy processing");
        Thread.sleep(100);
        multiwatch.startLocal("Some part of heavy processing");
        Thread.sleep(10);
        multiwatch.stopLocal("Some part of heavy processing");
        multiwatch.stopLocal("Heavy processing");
        Thread.sleep(10);

        multiwatch.startLocal("Loading"); // again, should continue
        Thread.sleep(10);
        multiwatch.stopLocal("Loading");

        multiwatch.stopGlobal();
        log.error(multiwatch.toString());

        Map<String, Stopwatch> stopwatches = multiwatch.getLocalStopwatches();
        assertThat(stopwatches).hasSize(3);
        stopwatches.values().forEach(sw -> assertThat(sw.isRunning()).isFalse());
        long loadingMillis = stopwatches.get("Loading").elapsed(TimeUnit.MILLISECONDS);
        long heavyMillis = stopwatches.get("Heavy processing").elapsed(TimeUnit.MILLISECONDS);
        long partialMillis = stopwatches.get("Some part of heavy processing").elapsed(TimeUnit.MILLISECONDS);

        assertThat(loadingMillis).isGreaterThanOrEqualTo(20);
        assertThat(heavyMillis).isGreaterThanOrEqualTo(110);
        assertThat(partialMillis).isGreaterThanOrEqualTo(10);
    }

    @Test
    public void testMicrometerWatchInitializations() {
        assertThatThrownBy(() -> new MicrometerWatch("", List.of("a", "b")))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Label")
            .hasMessageContaining("is invalid");

        assertThatThrownBy(() -> new MicrometerWatch("Wow, bad label!", List.of("a", "b")))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Label")
            .hasMessageContaining("is invalid");

        assertThatThrownBy(() -> new MicrometerWatch("much_better_label_but_too_long", List.of("a", "b")))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Label")
            .hasMessageContaining("is invalid");

        assertThatThrownBy(() -> new MicrometerWatch("nice_label", List.of()))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("No push-tags specified");

        assertThatThrownBy(() -> new MicrometerWatch("nice_label", List.of("фьюить", "ха")))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tag")
            .hasMessageContaining("is invalid");

        assertThatThrownBy(() -> new MicrometerWatch("nice_label", List.of("a", "b"), 0))
            .isExactlyInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Batch size cannot be < 1");

        assertThatCode(() -> new MicrometerWatch("nice_label", List.of("a", "b"), 2))
            .doesNotThrowAnyException();
    }

}
