package ru.yandex.market.abo.core.export;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.annotation.Nonnull;

import com.amazonaws.util.StringInputStream;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.common.util.IOUtils.gunzip;
import static ru.yandex.common.util.IOUtils.readInputStream;

/**
 * Created by antipov93@yndx-team.ru.
 */
public class GZipContentProviderTest {

    @Test
    public void testGZip() throws Exception {
        GZipContentProvider gZipContentProvider = new GZipContentProvider();
        String text = "text123\t\r\n";
        gZipContentProvider.setContentProvider(new ContentProvider() {
            @Nonnull
            @Override
            public InputStream getInputStream() {
                try {
                    return new StringInputStream(text);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        assertEquals(text, readInputStream(gunzip(gZipContentProvider.getInputStream())));
    }
}
