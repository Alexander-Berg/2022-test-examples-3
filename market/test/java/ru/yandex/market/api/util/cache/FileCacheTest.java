package ru.yandex.market.api.util.cache;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.Paths;
import java.util.function.Supplier;

import ru.yandex.market.api.integration.UnitTestBase;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 *
 * Created by apershukov on 30.12.16.
 */
@SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
public class FileCacheTest extends UnitTestBase {

    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    private static final String FILE_PATH = "/var/market-api/cache/test.csv";

    private FileCache<String> cache;
    private Storage storage;
    private boolean loadCache;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        storage = Mockito.spy(new Storage());
        storage.value.put(FILE_PATH, "cached-value");
        loadCache = false;

        cache = FileCache.<String> builder()
                .setLoader(storage)
                .setDumper(storage)
                .setFileMover(storage)
                .setPath(Paths.get(FILE_PATH))
                .setDefaultValue("default")
                .setLoadCacheStrategy(() -> loadCache)
                .build();
    }

    @Test
    public void testStartFromCache() {
        loadCache = true;

        Supplier<String> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn("new-value");

        String value = cache.get(supplier);
        assertEquals("cached-value", value);

        verifyZeroInteractions(supplier);
    }

    @Test
    public void testGetValueAndPutToCache() {
        String value = cache.get(() -> "new-value");
        assertEquals("new-value", value);
        assertEquals("new-value", storage.value.get(FILE_PATH + ".temp"));
        assertEquals("new-value", storage.value.get(FILE_PATH));

        verify(storage).moveFile(Paths.get("/var/market-api/cache/test.csv.temp"),
                                 Paths.get("/var/market-api/cache/test.csv"));
    }

    @Test
    public void testLoadFromCacheIfFailed() {
        Supplier<String> supplier = mock(Supplier.class);
        when(supplier.get()).thenThrow(RuntimeException.class);

        String value = cache.get(supplier);
        assertEquals("cached-value", value);
    }
}
