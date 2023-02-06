package ru.yandex.common.util.io;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import ru.yandex.common.util.IOUtils;
import static ru.yandex.common.util.collections.CollectionFactory.array;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

/**
 * Created on 17:08:29 16.12.2008
 *
 * @author jkff
 */
public class IOUtilsTest {
    @Test
    public void testReadInputStreamToBytesBounded() throws Exception {
        byte[] bs = new byte[65536*4 + 123];
        for(int n : array(123,65535,65536,65537,69537,65536*4-1,65536*4,65536*4+1,65536*4+122,65536*4+123)) {
            assertEquals(n, IOUtils.readInputStreamToBytes(new ByteArrayInputStream(bs), n).length);
        }
        assertEquals(bs.length, IOUtils.readInputStreamToBytes(new ByteArrayInputStream(bs), bs.length).length);
        assertEquals(bs.length, IOUtils.readInputStreamToBytes(new ByteArrayInputStream(bs)).length);
        assertEquals(bs.length, IOUtils.readInputStreamToBytes(new ByteArrayInputStream(bs), bs.length+10).length);
        assertEquals(bs.length, IOUtils.readInputStreamToBytes(new ByteArrayInputStream(bs), bs.length*3).length);
    }

    public void testCreateTempDir() throws IOException {
        File dir = IOUtils.createTempDir("temp_dir");
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        assertTrue(dir.canWrite());
    }
}
