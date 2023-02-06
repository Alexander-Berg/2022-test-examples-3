package ru.yandex.personal.mail.search.metrics.scraper.services.account;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.personal.mail.search.metrics.scraper.metrics.CachedMetricsCompatibleSystemManager;
import ru.yandex.personal.mail.search.metrics.scraper.metrics.MetricsCompatibleMailSystem;
import ru.yandex.personal.mail.search.metrics.scraper.metrics.MetricsCompatibleSystemManager;
import ru.yandex.personal.mail.search.metrics.scraper.metrics.MetricsSystemLoaderRegistry;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CachedMetricsCompatibleSystemManagerTest {
    private static final String SYS = "sys";
    private static final String ACC = "acc";
    private static final AccountConfiguration CONFIG =
            AccountConfiguration.fromInfo(new AccountInfo(SYS, ACC), Paths.get(""));

    @Test
    void accountDisconnectTest() throws InterruptedException {
        MetricsSystemLoaderRegistry registry = mock(MetricsSystemLoaderRegistry.class, Mockito.RETURNS_DEEP_STUBS);
        when(registry.getForInfo(any()).load(any(Path.class))).thenAnswer(
                (Answer<MetricsCompatibleMailSystem>) invocation -> mock(MetricsCompatibleMailSystem.class));

        int timeoutSeconds = 1;

        MetricsCompatibleSystemManager manager = new CachedMetricsCompatibleSystemManager(registry, timeoutSeconds);

        MetricsCompatibleMailSystem first = manager.getSystem(CONFIG);
        TimeUnit.SECONDS.sleep(timeoutSeconds);
        // Cache relies on millis, need extra delay to invalidate
        TimeUnit.MILLISECONDS.sleep(1);
        MetricsCompatibleMailSystem second = manager.getSystem(CONFIG);

        assertNotEquals(first, second);
        verify(registry.getForInfo(any()), times(2)).load(any());
    }
}
