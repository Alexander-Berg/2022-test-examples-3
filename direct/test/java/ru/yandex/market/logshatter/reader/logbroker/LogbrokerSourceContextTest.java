package ru.yandex.market.logshatter.reader.logbroker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.logbroker.pull.LogBrokerSourceKey;
import ru.yandex.market.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.logging.BatchErrorLoggerFactory;
import ru.yandex.market.logshatter.reader.ReadSemaphore;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 11.10.17
 */
public class LogbrokerSourceContextTest {
    private LogbrokerSourceContext sut;

    @Before
    public void setUp() throws Exception {
        LogBrokerSourceKey sourceKey = Mockito.mock(LogBrokerSourceKey.class);
        Mockito.when(sourceKey.getPath()).thenReturn("/dev/null");
        sut = new LogbrokerSourceContext(
            Paths.get(sourceKey.getPath()),
            Mockito.mock(LogShatterConfig.class), sourceKey,
            Mockito.mock(BatchErrorLoggerFactory.class),
            0, new ReadSemaphore().getEmptyQueuesCounter()
        );
    }

    @Test
    public void shouldFinishImmediatelyIfNoSaving() throws Exception {
        CompletableFuture<Void> future = sut.finish();
        assertTrue(future.isDone());

        boolean canSave = sut.beginSave();
        assertFalse(canSave);
    }

    @Test
    public void shouldWaitUntilSaved() throws Exception {
        boolean canSave = sut.beginSave();
        assertTrue(canSave);
        CompletableFuture<Void> future = sut.finish();
        assertFalse(future.isDone());
        sut.completeSave();
        assertTrue(future.isDone());
    }
}
