package ru.yandex.market.common.mds.s3.client.content.compress;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.content.factory.ContentCompressorFactory;
import ru.yandex.market.common.mds.s3.client.content.factory.ContentProviderFactory;
import ru.yandex.market.common.mds.s3.client.content.provider.TextContentProvider;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Тесты для {@link GZipContentCompressor}.
 */
public class GZipContentCompressorTest {

    @Test
    public void testCompressContentProvider() {
        String str = "String for compression";
        TextContentProvider inputContentProvider = ContentProviderFactory.text(str);
        ContentCompressorFactory.gzip()
                .compress(inputContentProvider,
                        compressed -> assertThat(new String(gunzip(compressed.getInputStream()), UTF_8), is(str)));
    }

    public static byte[] gunzip(InputStream inputStream) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream)) {
            IOUtils.copy(gzipInputStream, result);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return result.toByteArray();
    }
}
