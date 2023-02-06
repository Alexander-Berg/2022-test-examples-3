package ru.yandex.market.api.util.cache;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.concurrent.Futures;

import java.nio.file.Paths;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 *
 * Created by apershukov on 11.01.17.
 */
@SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
public class AsyncKeyValueFileCacheTest extends UnitTestBase {

    private static final String FILE_DIR = "/var/market-api/cache/dir";

    private Storage storage;
    private boolean loadCache;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        storage = Mockito.spy(new Storage());
        storage.value.put(FILE_DIR + "/key1.req", "cached-value");
        loadCache = false;

        cache = AsyncKeyValueFileCache.<String> builder()
                .setLoader(storage)
                .setDumper(storage)
                .setFileMover(storage)
                .setPath(Paths.get(FILE_DIR))
                .setDefaultValue("default")
                .setLoadCacheStrategy(() -> loadCache)
                .setEnsureDirs(false)
                .build();
    }

    private AsyncKeyValueCache<String> cache;

    @Test
    public void testForceLoadFromCache() {
        loadCache = true;

        Supplier<Future<String>> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn(Futures.newSucceededFuture("value"));
        Future<String> future = cache.get("key1.req", supplier);
        String value = Futures.waitAndGet(future);

        assertEquals("cached-value", value);
        verifyZeroInteractions(supplier);
    }

    @Test
    public void testSaveLoadedInCache() {
        String value = invoke("key1.req", () -> Futures.newSucceededFuture("new-value"));

        assertEquals("new-value", value);

        String tempFilePath = path("key1.req") + ".temp";
        assertEquals("new-value", storage.value.get(tempFilePath));

        String filePath = path("key1.req");
        assertEquals("new-value", storage.value.get(filePath));

        verify(storage).moveFile(Paths.get(tempFilePath), Paths.get(filePath));
    }

    @Test
    public void testSaveLoadedDataWithNewKey() {
        String value = invoke("key2.req", () -> Futures.newSucceededFuture("new-value"));

        assertEquals("new-value", value);

        String tempFilePath = path("key2.req") + ".temp";
        assertEquals("new-value", storage.value.get(tempFilePath));

        String filePath = path("key2.req");
        assertEquals("new-value", storage.value.get(filePath));

        verify(storage).moveFile(Paths.get(tempFilePath), Paths.get(filePath));

        assertEquals("cached-value", storage.value.get(path("key1.req")));
    }

    @Test
    public void testLoadFromCahceOnFail() {
        String value = invoke("key1.req", () -> Futures.newFailedFuture(new RuntimeException()));

        assertEquals("cached-value", value);
        assertEquals("cached-value", storage.value.get(path("key1.req")));

        verify(storage, never()).moveFile(any(), any());
    }

    @Test
    public void testLoadFromCacheOnExceptionInSupplier() {
        String value = invoke("key1.req", () -> {
            throw new RuntimeException();
        });

        assertEquals("cached-value", value);
        assertEquals("cached-value", storage.value.get(path("key1.req")));

        verify(storage, never()).moveFile(any(), any());
    }

    private String path(String key) {
        return FILE_DIR + '/' + key;
    }

    private String invoke(String key, Supplier<Future<String>> supplier) {
        return Futures.waitAndGet(cache.get(key, supplier));
    }
}
